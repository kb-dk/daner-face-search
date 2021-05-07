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
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.MathLinkFactory;
import dk.kb.facesearch.config.ServiceConfig;
import dk.kb.facesearch.model.v1.FaceDto;
import dk.kb.facesearch.model.v1.SimilarDto;
import dk.kb.facesearch.model.v1.SimilarResponseDto;
import dk.kb.facesearch.webservice.exception.InternalServiceException;
import dk.kb.util.Resolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

    public static final String FEATURES_KEY = "config.wolfram.features";
    public static final String KERNEL_KEY = "config.wolfram.mathKernel";
    public static final String SCRIPT_KEY = "config.wolfram.danerScript";

    private static WolframFaces instance;
    private KernelLink ml;

    /**
     * Initialized the Wolfram engine, loads the DANER-script and performs a warmup query.
     */
    public WolframFaces() {
        String featuresStr = ServiceConfig.getConfig().getString(FEATURES_KEY);
        log.info("Initializing WolframFaces with features from '" + featuresStr + "'");

        List<Path> featureCandidates = Resolver.resolveGlob(featuresStr);
        if (featureCandidates.isEmpty()) {
            String message = "Unable to initialize WolframFaces as no file matches feature glob '" + featuresStr + "'";
            log.error(message); // We use the anti-pattern log-throw as this halts the full application and we want all possible signals for that
            throw new IllegalStateException(
                    "Unable to initialize WolframFaces as no file matches feature glob '" + featuresStr + "'");
        }

        String kernel = ServiceConfig.getConfig().getString(KERNEL_KEY);
        String script;
        try {
            script = Resolver.resolveURL(ServiceConfig.getConfig().getString(SCRIPT_KEY)).getFile();
        } catch (Exception e) {
            String message = "Error: unable to resolve DANER script";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }
        Path featureFile = featureCandidates.get(featureCandidates.size()-1);
        log.debug("Resolved concrete feature file '" + featureFile + "'");

        try {
            initEngine(kernel, script, featureFile);
        } catch (Exception e) {
            String message = "Error: initEngine failed";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }
        // TODO: Init the engine, load the script, warm it with a test search
    }

    public static WolframFaces getInstance() {
        if (instance == null) {
            instance = new WolframFaces();
        }
        return instance;
    }

    private void initEngine(String kernel, String script, Path featureFile) {
        String[] argv = new String[] {
                "-linkmode",
                "launch",
                "-linkname",
                kernel + " -mathlink"
                //"/Applications/Mathematica.app/Contents/MacOS/MathKernel -mathlink"
        };

        try {
            ml = MathLinkFactory.createKernelLink(argv);
        } catch (MathLinkException e) {
            throw new RuntimeException("Fatal error opening kernel link with " + Arrays.toString(argv), e);
        }

        try {
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
            ml.discardAnswer();
            ml.evaluate("<<" + script);
            ml.discardAnswer();
            //System.out.println("Now for the real test - takes less that a second:");
            //System.out.println(ml.evaluateToOutputForm("findSimilarFaces[\"http://17053.dk/pmd.png\",2]", 0));
            //System.out.println("******");
            // DONE INITIALUZATION
            // ********************************************************************

            // Warmup and test
            // TODO: Why does this fail!?
//            getSimilarFaces("http://localhost:8234/daner-face-search/thispersondoesnotexist.com.jpg", "auto", 1);
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
            String message = "Expected JSON from DANER script " +
            (jsonStr.length() > 400 ? jsonStr.substring(0, 397) + "..." : jsonStr);
            log.warn(message, e);
            throw new InternalServiceException(message);
        }
    }

    String getSimilarFacesJSONString(String imageURL, String imageType, int maxMatches) {
        String wCall = String.format(
                Locale.ROOT, "findSimilarFaces[\"%s\", \"%s\", %d]", imageURL, imageType, maxMatches);
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

}
