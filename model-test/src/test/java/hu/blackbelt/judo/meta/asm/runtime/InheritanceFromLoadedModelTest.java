package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.epsilon.runtime.execution.impl.NioFilesystemnRelativePathURIHandlerImpl;
import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.support.EcoreModelResourceSupport;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.loadArgumentsBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Optional;


public class InheritanceFromLoadedModelTest {

    static Logger log = LoggerFactory.getLogger(InheritanceFromLoadedModelTest.class);

    ResourceSet resourceSet;
    URIHandler uriHandler;

    @BeforeEach
    public void setUp() {
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
    }

    @AfterEach
    public void tearDown() {
        resourceSet = null;
    }

    @Test
    public void testInheritedAttributesInXMI() throws Exception {
        log.info("Testing XMI ...");
        AsmModel.loadAsmModel(loadArgumentsBuilder()
                .resourceSet(Optional.of(resourceSet))
                .uri(URI.createFileURI("src/test/model/inheritance.model"))
                .name("test")
                .build());
        test(2 + 1);
    }

    @Test
    public void testInheritedAttributesInXML() throws Exception {
        log.info("Testing XML ...");
        AsmModel.loadAsmModel(loadArgumentsBuilder()
                .resourceSet(Optional.of(resourceSet))
                .uri(URI.createFileURI("src/test/model/asm.model"))
                .name("test")
                .build());
        test(3 + 8);
    }

    @Test
    public void testInheritedAttributesInXMIWithNIOLoader() throws Exception {
        log.info("Testing XMI (custom uri handler) ...");
        final File modelFile = new File("src/test/model/inheritance.model");

        uriHandler = new NioFilesystemnRelativePathURIHandlerImpl("urn", FileSystems.getDefault(),
                modelFile.getParentFile().getAbsolutePath());

        AsmModel.loadAsmModel(loadArgumentsBuilder()
                .resourceSet(Optional.of(resourceSet))
                .uriHandler(Optional.of(uriHandler))
                .uri(URI.createURI("urn:inheritance.model"))
                .name("test")
                .build());

        test(2 + 1);
    }

    @Test
    public void testInheritedAttributesInXMLWithNIOLoader() throws Exception {
        log.info("Testing XML (custom uri handler) ...");
        final File modelFile = new File("src/test/model/asm.model");

        uriHandler = new NioFilesystemnRelativePathURIHandlerImpl("urn", FileSystems.getDefault(),
                modelFile.getParentFile().getAbsolutePath());

        resourceSet = AsmModelResourceSupport.createAsmResourceSet();

        AsmModel.loadAsmModel(loadArgumentsBuilder()
                .resourceSet(Optional.of(resourceSet))
                .uriHandler(Optional.of(uriHandler))
                .uri(URI.createURI("urn:asm.model"))
                .name("test")
                .build());

        test(3 + 8);
    }


    @Test
    public void testInheritedAttributesInXMIWithStandardLoader() {
        log.info("Testing XMI (standard loader) ...");
        
        final File modelFile = new File("src/test/model/inheritance.model");
        URI fileURI  = URI.createFileURI(modelFile.getAbsolutePath());
        
        resourceSet.getResource(fileURI, true);
        EcoreModelResourceSupport.setupRelativeUriRoot(resourceSet, fileURI);
        
        test(2 + 1);
    }

    @Test
    public void testInheritedAttributesInXMLWithStandardLoader() {
        log.info("Testing XML (standard loader) ...");
        final File modelFile = new File("src/test/model/asm.model");
        URI fileURI  = URI.createFileURI(modelFile.getAbsolutePath());
        
        resourceSet.getResource(fileURI, true);
        EcoreModelResourceSupport.setupRelativeUriRoot(resourceSet, fileURI);
        test(3 + 8);
    }

    private void test(int expectedAttributes) {
        final EClass employeeClass = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Employee");

        employeeClass.getEAllAttributes().forEach(a -> log.debug(" - attribute: {}", a.getName()));

        assertEquals(expectedAttributes, employeeClass.getEAllAttributes().size());
    }
}
