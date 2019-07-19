package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.NioFilesystemnRelativePathURIHandlerImpl;
import hu.blackbelt.epsilon.runtime.execution.impl.Slf4jLog;
import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.loadArgumentsBuilder;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Optional;

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
        ResourceSet resourceSet = AsmModelResourceSupport.createAsmResourceSet();

        AsmModel.loadAsmModel(loadArgumentsBuilder()
                .resourceSet(Optional.of(resourceSet))
                .uriHandler(Optional.of(uriHandler))
                .uri(URI.createURI("urn:asm.model"))
                .name("test")
                .build());

        
        TreeIterator<Notifier> iter = resourceSet.getAllContents();
        while (iter.hasNext()) {
            final Notifier obj = iter.next();
            log.debug(obj.toString());
        }
    }
}