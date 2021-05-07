package dk.kb.facesearch;

import dk.kb.facesearch.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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
class WolframFacesTest {
    // Use the same config for unit testing as when running as a webservice
    @BeforeAll
    static void init() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        ServiceConfig.initialize(projectRoot + File.separator + "conf" + File.separator + "daner-face-search*.yaml");
    }

    @Test
    void basicTest() throws MalformedURLException, FileNotFoundException {
        // Fails with cannot open PNG!?
        //String image = Resolver.resolveURL("thispersondoesnotexist.com.jpg").toString();
        //assertNotNull(WolframFaces.getSimilarFaces(image, image.endsWith("jpg") ? "JPEG" : "PNG", 2));

        // Two calls to test if the state is restored after a call
        assertNotNull(WolframFaces.getSimilarFaces("http://17053.dk/pmd.png", "PNG", 2));
        assertNotNull(WolframFaces.getSimilarFaces("http://17053.dk/pmd.png", "PNG", 2));
    }
}