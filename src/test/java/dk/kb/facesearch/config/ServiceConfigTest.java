package dk.kb.facesearch.config;

import dk.kb.util.Resolver;
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
class ServiceConfigTest {

    /*
     * This unit-test probably fails when the template is applied and a proper project is taking form.
     * That is okay. It is only here to serve as a temporary demonstration of unit-testing and configuration.
     */
    @Test
    void sampleConfigTest() throws IOException {
        // Pretty hacky, but it is only a sample unit test
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();

        Path sampleEnvironmentSetup = Path.of(projectRoot, "conf/daner-face-search-environment.yaml");
        assertTrue(Files.exists(sampleEnvironmentSetup),
                   "The sample setup is expected to be present at '" + sampleEnvironmentSetup + "'");

        ServiceConfig.initialize(projectRoot + File.separator + "conf" + File.separator + "daner-face-search*.yaml");

        // Defined in behaviour
//        assertEquals(10, ServiceConfig.getConfig().getInteger("config.limits.min"));

        // Real value in environment
//        assertEquals("real_dbpassword", ServiceConfig.getConfig().getString("config.backend.password"));
    }
}