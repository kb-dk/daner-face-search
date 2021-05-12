/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.facesearch;

import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkFactory;
import dk.kb.facesearch.config.ServiceConfig;
import dk.kb.facesearch.model.v1.FaceDto;
import dk.kb.facesearch.model.v1.SimilarDto;
import dk.kb.facesearch.model.v1.SimilarResponseDto;
import dk.kb.facesearch.webservice.exception.InternalServiceException;
import dk.kb.facesearch.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Connects to the Wolfram engine to perform similarity search for faces.
 */
public class WolframFaces {
    private static final Logger log = LoggerFactory.getLogger(WolframFaces.class);

    public static final String FOLDER_KEY = "config.wolfram.folder";
    public static final String JLINK_KEY = "config.wolfram.jlinkLibDir";
    public static final String FEATURES_KEY = "config.wolfram.features";
    public static final String KERNEL_KEY = "config.wolfram.mathKernel";
    public static final String SCRIPT_KEY = "config.wolfram.danerScript";

    public static final String LIBDIR_PROP_KEY = "com.wolfram.jlink.libdir"; // Not YAML but system property
    public static final String LIBDIR_ENV_KEY = "JLINK_LIB_DIR"; // Not YAML but system property

    private static WolframFaces instance;
    private KernelLink ml;

    /**
     * Initialized the Wolfram engine, loads the DANER-script and performs a warmup query.
     */
    public WolframFaces() {
        initEngine(ServiceConfig.getConfig());
    }

    public static WolframFaces getInstance() {
        if (instance == null) {
            instance = new WolframFaces();
        }
        return instance;
    }

    void initEngine(YAML conf) {
        // Feature file
        if (!conf.containsKey(FEATURES_KEY)) {
            String message = "Cannot initialize Wolfram engine as config " + FEATURES_KEY + " is not defined";
            log.error(message);
            throw new IllegalStateException(message);
        }
        String featuresStr = conf.getString(FEATURES_KEY);
        List<Path> featureCandidates = Resolver.resolveGlob(featuresStr);
        if (featureCandidates.isEmpty()) {
            String message = "Unable to initialize WolframFaces as no file matches feature glob '" + featuresStr + "'";
            log.error(message); // We use the anti-pattern log-throw as this halts the full application and we want all possible signals for that
            throw new IllegalStateException(
                    "Unable to initialize WolframFaces as no file matches feature glob '" + featuresStr + "'");
        }
        Path featureFile = featureCandidates.get(featureCandidates.size()-1);
        log.debug("Resolved concrete feature file '" + featureFile + "'");

        // Native JLink libraries
        if (System.getProperty(LIBDIR_PROP_KEY) != null) {
            log.info("Keeping already defined system property {}='{}'",
                     LIBDIR_PROP_KEY, System.getProperty(LIBDIR_PROP_KEY));
        } else if (System.getenv(LIBDIR_ENV_KEY) != null) {
            log.info("Keeping already defined environment {}='{}'",
                     LIBDIR_ENV_KEY, System.getenv(LIBDIR_ENV_KEY));
        } else {
            if (conf.containsKey(JLINK_KEY)) {
                System.setProperty(LIBDIR_PROP_KEY, conf.getString(JLINK_KEY));
                log.info("Service config key {} converted to system property {}='{}'",
                         JLINK_KEY, LIBDIR_PROP_KEY, System.getProperty(JLINK_KEY));
            } else if (conf.containsKey(FOLDER_KEY)){
                System.setProperty(LIBDIR_PROP_KEY, conf.getString(FOLDER_KEY) + "/SystemFiles/Links/JLink/");
                log.info("Service config key {} converted to system property {}='{}'",
                         FOLDER_KEY, LIBDIR_PROP_KEY, System.getProperty(JLINK_KEY));
            } else {
                log.warn("Neither system property {}, nor service config keys {} or {} defined. " +
                         "JLink native library location will be derived from JLink.jar location",
                         LIBDIR_PROP_KEY, JLINK_KEY, FOLDER_KEY);
            }
        }

        // Kernel
        String kernel;
        if (conf.containsKey(KERNEL_KEY)) {
            kernel = conf.getString(KERNEL_KEY);
            if (!Path.of(kernel).toFile().canRead()) {
                String message = "No kernel at " + KERNEL_KEY + "='" + kernel;
                log.error(message);
                throw new IllegalStateException(new FileNotFoundException(message));
            }
        } else if (conf.containsKey(FOLDER_KEY)) {
            kernel = conf.getString(FOLDER_KEY) + "/Executables/MathKernel";
            if (!Path.of(kernel).toFile().canRead()) {
                String message = "No kernel at " + FOLDER_KEY + " + '/Executables/MathKernel' ='" + kernel;
                log.error(message);
                throw new IllegalStateException(new FileNotFoundException(message));
            }
        } else {
            String message = "Cannot initialize Wolfram engine as neither config " + KERNEL_KEY + " or " + FOLDER_KEY +
                             " are defined";
            log.error(message);
            throw new IllegalStateException(message);
        }

        // Script
        String script;
        if (!conf.containsKey(SCRIPT_KEY)) {
            String message = "Cannot initialize Wolfram engine as config " + SCRIPT_KEY + " is not defined";
            log.error(message);
            throw new IllegalStateException(message);
        }
        String scriptResource = conf.getString(SCRIPT_KEY);
        try {
            script = Resolver.resolveURL(scriptResource).getFile();
        } catch (Exception e) {
            String message = "Unable to resolve DANER script from '" + scriptResource + "'";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }

        try {
            initEngine(kernel, script, featureFile);
        } catch (Exception e) {
            String message = "Error: initEngine failed";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    void initEngine(String kernel, String script, Path featureFile) {
        log.info("initEngine(kernel='{}', script='{}', featureFile='{}') called", kernel, script, featureFile);

        String[] argv = new String[] {
                "-linkmode",
                "launch",
                "-linkname",
                kernel + " -mathlink"
                //"/Applications/Mathematica.app/Contents/MacOS/MathKernel -mathlink"
        };

        log.info("Creating MathLink Kernel link with " + Arrays.asList(argv));
        try {
            ml = MathLinkFactory.createKernelLink(argv);
        } catch (Exception e) {
            throw new RuntimeException("Fatal error opening kernel link with " + Arrays.toString(argv), e);
        }

        try {
            String result;
            // ********************************************************************
            // INITIALIZE THE KERNEL
            /*
             * Get rid of the initial InputNamePacket the kernel will send
             * when it is launched.
             * pmd: As I understand it, this forces the kernel to load before our
             * first evaluation.
             */
            ml.discardAnswer();

            // Okay, try to load some definitions from a Wolfram Language package
            ml.evaluate("featuresFile = \"" + featureFile + "\"");
            ml.waitForAnswer();
            result = ml.getString();
            log.info("Result from setting featureFile '{}': '{}'", featureFile, result);

            log.info("Loading the " + script + " file.");
            ml.evaluate("Get[\"" + script + "\"]");
            ml.waitForAnswer();
            result = ml.getString();
            log.info("Result from loading script '{}': '{}'", script, result);
            if (result.equals("$Failed") || result.equals("$Aborted")) {
                throw new IllegalStateException("Couldn't load '" + script + "': " + result);
            }

            // setting the log level:
            ml.evaluate("logLevel=1"); // 1: default
            ml.discardAnswer();

            //System.out.println("Now for the real test - takes less that a second:");
            //System.out.println(ml.evaluateToOutputForm("findSimilarFaces[\"http://17053.dk/pmd.png\",2]", 0));
            //System.out.println("******");
            // DONE INITIALUZATION
            // ********************************************************************

            // Warmup and test
            log.debug("Wolfram script loaded. Performing warmup");
            String testImage = "https://upload.wikimedia.org/wikipedia/commons/6/6f/Christopher_Meloni_croped_face.png";
            try {
                getSimilarFacesJSONString(testImage, "PNG", 1);
            } catch (Exception e) {
                log.warn("Unable to perform warmup of DANER script. Maybe the test PNG is not available? " + testImage);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize Wolfram Engine", e);
        //} finally {
//            ml.close();
        }
        log.info("Successfully initalized the Wolfram engine with DANER script '{}' and features '{}'",
                 script, featureFile);
    }

    /**
     * Request a list of detected faces from imageURL, each containing a list of IDs for similar faces from
     * the DANER collection.
     * @param imageURL   the URL to an image to use as basis for faciag detection and similarity search.
     * @param imageType
     * @param maxMatches the maximum number of similar face IDs to return for each detected face.
     * @return a strincture with similarity matches, ready for delivery to the caller.
     */
    public static SimilarResponseDto getSimilarFaces(String imageURL, String imageType, int maxMatches) {
        JSONArray json = getInstance().getSimilarFacesJSON(imageURL, imageType, maxMatches);
        AtomicInteger faceIndex = new AtomicInteger(0);

        return new SimilarResponseDto()
                .imageURL(imageURL)
                .technote("Wolfram script through Java")
                .faces(StreamSupport.stream(json.spliterator(), false)
                               .map(o -> jsonToFaces((JSONArray)o, faceIndex.getAndIncrement()))
                               .collect(Collectors.toList()));
    }

    private static FaceDto jsonToFaces(JSONArray jsonFaces, int index) {
        return new FaceDto()
                .index(index)
                .similars(StreamSupport.stream(jsonFaces.spliterator(), false)
                                  .map(o -> jsonToSimilarity((JSONObject)o))
                                  .collect(Collectors.toList()));
    }

    private static SimilarDto jsonToSimilarity(JSONObject jsonSimilarity) {
        return new SimilarDto()
                .id(jsonSimilarity.getString("id"))
                .distance(jsonSimilarity.getDouble("distance"));
    }

    JSONArray getSimilarFacesJSON(String imageURL, String imageType, int maxMatches) {
        String jsonStr = getSimilarFacesJSONString(imageURL, imageType, maxMatches);
        try {
            return new JSONArray(jsonStr);
        } catch (JSONException e) {
            String message = "Expected JSON from DANER script but got '" +
            (jsonStr.length() > 400 ? jsonStr.substring(0, 397) + "..." : jsonStr) + "'";
            log.warn(message, e);
            throw new InternalServiceException(message);
        }
    }

    String getSimilarFacesJSONString(String imageURL, String imageType, int maxMatches) {
        imageType = guessImageType(imageURL, imageType);
        String wCall = String.format(
                Locale.ROOT, "findSimilarFaces[\"%s\", %d]", imageURL, maxMatches);
//                Locale.ROOT, "findSimilarFaces[\"%s\", \"%s\", %d]", imageURL, imageType, maxMatches);
        log.debug("Invoking findSimilarFaces script with {}", wCall);
        String jsonStr = ml.evaluateToOutputForm(wCall, 0);

        // TODO: Remove this debug line
        System.err.println("JSON string from " + wCall +": " + jsonStr);

        if ("Null".equals(jsonStr)) {
            throw new NullPointerException("Got String 'Null' from " + wCall);
        }
        if (jsonStr == null) {
            throw new NullPointerException("Got Java null from " + wCall);
        }

        return jsonStr;
    }

    String guessImageType(String imageURL, String suggestedImageType) {
        if (suggestedImageType == null) {
            suggestedImageType = "auto";
        }
        String imageType;
        switch (suggestedImageType.toUpperCase(Locale.ROOT)) {
            case "PNG":
            case "JPEG":
                imageType = suggestedImageType.toUpperCase(Locale.ROOT);
                break;
            case "AUTO": {
                if (imageURL.toLowerCase(Locale.ROOT).endsWith("jpg") || imageURL.toLowerCase(Locale.ROOT).endsWith("jpeg")) {
                    imageType = "JPEG";
                } else {
                    imageType = "PNG";
                }
                log.debug("Auto-assigning imageType=" + imageType + " to image '" + imageURL + "'");
                break;
            }
            default: throw new InvalidArgumentServiceException(
                    "The imageType '" + suggestedImageType + "' is not supported. Valid values are 'JPEG', 'PNG' and 'auto'");
        }
        return imageType;
    }

}
