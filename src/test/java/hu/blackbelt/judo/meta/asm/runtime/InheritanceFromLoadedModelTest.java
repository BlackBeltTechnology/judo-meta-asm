package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.epsilon.runtime.execution.impl.NioFilesystemnRelativePathURIHandlerImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.FileSystems;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModelLoader.*;

@Slf4j
public class InheritanceFromLoadedModelTest {

    ResourceSet resourceSet;
    URIHandler uriHandler;

    @Before
    public void setUp() {
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
    }

    @After
    public void tearDown() {
        resourceSet = null;
    }

    @Test
    public void testInheritedAttributesInXMI() throws Exception {
        log.info("Testing XMI ...");
        final File modelFile = new File(srcDir(), "test/resources/inheritance.model");
        loadAsmModel(resourceSet, URI.createURI(modelFile.getAbsolutePath()),"test","1.0.0");
        test(2 + 1);
    }

    @Test
    public void testInheritedAttributesInXML() throws Exception {
        log.info("Testing XML ...");
        final File modelFile = new File(srcDir(), "test/resources/asm.model");
        loadAsmModel(resourceSet, URI.createURI(modelFile.getAbsolutePath()),"test","1.0.0");
        test(3 + 8);
    }

    @Test
    public void testInheritedAttributesInXMIWithNIOLoader() throws Exception {
        log.info("Testing XMI (custom uri handler) ...");
        final File modelFile = new File(srcDir(), "test/resources/inheritance.model");

        uriHandler = new NioFilesystemnRelativePathURIHandlerImpl("urn", FileSystems.getDefault(),
                modelFile.getParentFile().getAbsolutePath());

        resourceSet = createAsmResourceSet(uriHandler);
        loadAsmModel(resourceSet, URI.createURI("urn:inheritance.model"),"test","1.0.0");
        test(2 + 1);
    }

    @Test
    public void testInheritedAttributesInXMLWithNIOLoader() throws Exception {
        log.info("Testing XML (custom uri handler) ...");
        final File modelFile = new File(srcDir(), "test/resources/asm.model");

        uriHandler = new NioFilesystemnRelativePathURIHandlerImpl("urn", FileSystems.getDefault(),
                modelFile.getParentFile().getAbsolutePath());

        resourceSet = createAsmResourceSet(uriHandler);

        loadAsmModel(resourceSet, URI.createURI("urn:asm.model"),"test","1.0.0");
        test(3 + 8);
    }


    @Test
    public void testInheritedAttributesInXMIWithStandardLoader() {
        log.info("Testing XMI (standard loader) ...");
        final File modelFile = new File(srcDir(), "test/resources/inheritance.model");
        resourceSet.getResource(URI.createURI(modelFile.getAbsolutePath()), true);
        setupRelativeUriRoot(resourceSet, URI.createURI(modelFile.getAbsolutePath()));
        test(2 + 1);
    }

    @Test
    public void testInheritedAttributesInXMLWithStandardLoader() {
        log.info("Testing XML (standard loader) ...");
        final File modelFile = new File(srcDir(), "test/resources/asm.model");
        resourceSet.getResource(URI.createURI(modelFile.getAbsolutePath()), true);
        setupRelativeUriRoot(resourceSet, URI.createURI(modelFile.getAbsolutePath()));
        test(3 + 8);
    }

    private void test(int expectedAttributes) {
        final EClass employeeClass = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Employee");

        employeeClass.getEAllAttributes().forEach(a -> log.debug(" - attribute: {}", a.getName()));

        Assert.assertEquals(expectedAttributes, employeeClass.getEAllAttributes().size());
    }

    public File srcDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "../../src");
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }
}
