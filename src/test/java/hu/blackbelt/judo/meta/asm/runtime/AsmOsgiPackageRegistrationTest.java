package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.NioFilesystemnRelativePathURIHandlerImpl;
import hu.blackbelt.epsilon.runtime.execution.impl.Slf4jLog;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.FileSystems;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModelLoader.*;

@Slf4j
public class AsmOsgiPackageRegistrationTest {

    URIHandler uriHandler;
    Log slf4jlog;

    @Before
    public void setUp() throws Exception {
        // Set our custom handler
        uriHandler = new NioFilesystemnRelativePathURIHandlerImpl("urn", FileSystems.getDefault(),
                getTargetFile("").getAbsolutePath());

        // Default logger
        slf4jlog = new Slf4jLog(log);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoadToReourceSet() throws Exception {
        ResourceSet resourceSet = createAsmResourceSet(uriHandler);
        loadAsmModel(resourceSet, URI.createURI("urn:asm.model"), "northwind", "1.0.0");

        TreeIterator<Notifier> iter = resourceSet.getAllContents();
        while (iter.hasNext()) {
            final Notifier obj = iter.next();
            log.debug(obj.toString());
        }
    }


    private File getBundleFile(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(new File(classLoader.getResource("").getFile()).getParentFile().getParentFile().getAbsoluteFile() + File.separator + "model");

        return new File(file.getAbsolutePath() + File.separator + name);
    }

    private File getTargetFile(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("").getFile()).getAbsoluteFile();

        return new File(file.getAbsolutePath() + File.separator + name);
    }

}