package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.NioFilesystemnRelativePathURIHandlerImpl;
import hu.blackbelt.epsilon.runtime.execution.impl.Slf4jLog;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileSystems;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.asmLoadArgumentsBuilder;

public class AsmOsgiPackageRegistrationTest {

    static Logger log = LoggerFactory.getLogger(AsmOsgiPackageRegistrationTest.class);

    URIHandler uriHandler;

    Log slf4jlog;

    @BeforeEach
    public void setUp() throws Exception {
        // Set our custom handler
        final File modelFile = new File("src/test/model/asm.model");

    	
        uriHandler = new NioFilesystemnRelativePathURIHandlerImpl("urn", FileSystems.getDefault(),
                modelFile.getParentFile().getAbsolutePath());

        // Default logger
        slf4jlog = new Slf4jLog(log);
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoadToReourceSet() throws Exception {
        AsmModel asmModel = AsmModel.loadAsmModel(asmLoadArgumentsBuilder()
                .uriHandler(uriHandler)
                .uri(URI.createURI("urn:asm.model"))
                .name("test"));

        
        TreeIterator<Notifier> iter = asmModel.getResourceSet().getAllContents();
        while (iter.hasNext()) {
            final Notifier obj = iter.next();
            log.debug(obj.toString());
        }
    }
}