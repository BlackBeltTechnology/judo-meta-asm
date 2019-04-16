package hu.blackbelt.judo.meta.asm;

import hu.blackbelt.judo.meta.asm.runtime.AsmModelLoader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

@Slf4j
public class InheritanceFromLoadedModelTest {

    ResourceSet resourceSet;

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
        AsmModelLoader.loadAsmModel(resourceSet,
                URI.createURI(new File(srcDir(), "test/resources/inheritance.model").getAbsolutePath()),
                "test",
                "1.0.0");

        final EClass employeeClass = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Employee");

        employeeClass.getEAllAttributes().forEach(a -> log.debug("Attribute: {}", a.getName()));

        Assert.assertEquals(3, employeeClass.getEAttributes().size());
    }

    @Test
    public void testInheritedAttributesInXML() throws Exception {
        AsmModelLoader.loadAsmModel(resourceSet,
                URI.createURI(new File(srcDir(), "test/resources/asm.model").getAbsolutePath()),
                "test",
                "1.0.0");

        final EClass employeeClass = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Employee");

        employeeClass.getEAllAttributes().forEach(a -> log.debug("Attribute: {}", a.getName()));

        Assert.assertEquals(3, employeeClass.getEAttributes().size());
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
