package hu.blackbelt.judo.meta.asm;

import hu.blackbelt.judo.meta.asm.runtime.AsmModelLoader;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class AnnotationTests {

    ResourceSet resourceSet;

    @Before
    public void setUp() throws Exception {
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());

        AsmModelLoader.loadAsmModel(resourceSet,
                URI.createURI(new File(srcDir(), "test/resources/asm.model").getAbsolutePath()),
                "test",
                "1.0.0");
    }

    @After
    public void tearDown() {
        resourceSet = null;
    }

    @Test
    public void testGetAnnotation() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");

        final Map.Entry<String, String> annotationDetailsEntry = order.getEAnnotations().iterator().next().getDetails().entrySet().iterator().next();
        final Optional<EAnnotation> eAnnotation = AsmUtils.getAnnotation(resourceSet, annotationDetailsEntry);

        Assert.assertTrue(eAnnotation.isPresent());
        Assert.assertEquals(AsmUtils.extendedMetadataUri, eAnnotation.get().getSource());
    }

    @Test
    public void testGetExtensionAnnotationExisting() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");

        final Optional<EAnnotation> existing = AsmUtils.getExtensionAnnotation(order, false);

        Assert.assertTrue(existing.isPresent());
        Assert.assertTrue(Boolean.valueOf(existing.get().getDetails().get("entity")));
    }

    public void testGetExtensionAnnotationNotExistingButCreated() {
        // TODO - check annotation that is not existing yet but created
    }

    public void testGetExtensionAnnotationNotExisting() {
        // TODO - check annotation that is not existing nor created
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
