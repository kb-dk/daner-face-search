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

import dk.kb.facesearch.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.MathLinkFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class WolframTest {
    private static final Logger log = LoggerFactory.getLogger(WolframTest.class);

    // Use the same config for unit testing as when running as a webservice
    @BeforeAll
    static void init() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        Path sampleEnvironmentSetup = Path.of(projectRoot, "conf/daner-face-search-environment.yaml");
        assertTrue(Files.exists(sampleEnvironmentSetup),
                   "The sample setup is expected to be present at '" + sampleEnvironmentSetup + "'");

        ServiceConfig.initialize(projectRoot + File.separator + "conf" + File.separator + "daner-face-search*.yaml");
    }

    @Test
    public void testBasicWolframUse() throws MathLinkException {
        String kernel = ServiceConfig.getConfig().getString(WolframFaces.KERNEL_KEY);
        String[] argv = new String[] {
                "-linkmode",
                "launch",
                "-linkname",
                kernel + " -mathlink"
                //"/Applications/Mathematica.app/Contents/MacOS/MathKernel -mathlink"
        };
        KernelLink ml;

        try {
            ml = MathLinkFactory.createKernelLink(argv);
        } catch (MathLinkException e) {
            throw new RuntimeException("Fatal error opening kernel link", e);
        }

        try {
            /*
             * Get rid of the initial InputNamePacket the kernel will send
             * when it is launched.
             */
            ml.discardAnswer();

            // Generate som JSON
                String result = ml.evaluateToOutputForm(
                            "1+2",
                            0
                    );
            assertEquals("3", result, "Evaluating 1+2 with the Wolfram Engine should yield the correct result");
        } finally {
            ml.close();
        }

    }
}
