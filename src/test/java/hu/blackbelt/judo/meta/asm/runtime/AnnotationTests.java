package hu.blackbelt.judo.meta.asm.runtime;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
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

import static hu.blackbelt.judo.meta.asm.runtime.AsmModelLoader.loadAsmModel;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

@Slf4j
public class AnnotationTests {

    ResourceSet resourceSet;
    AsmUtils asmUtils;

    @Before
    public void setUp() throws Exception {
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());

        asmUtils = new AsmUtils(resourceSet);
        loadAsmModel(resourceSet,
                URI.createURI(new File(srcDir(), "test/resources/asm.model").getAbsolutePath()),
                "test",
                "1.0.0");
    }

    @After
    public void tearDown() {
        resourceSet = null;
    }

    @Test
    public void testGetExtensionAnnotationExisting() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");

        final Optional<EAnnotation> existing = asmUtils.getExtensionAnnotationByName(order, "entity", false);

        assertTrue(existing.isPresent());
        assertTrue(Boolean.valueOf(existing.get().getDetails().get(AsmUtils.EXTENDED_METADATA_DETAILS_VALUE_KEY)));
    }

    public void testGetExtensionAnnotationNotExistingButCreated() {
        // TODO - check annotation that is not existing yet but created
    }

    public void testGetExtensionAnnotationNotExisting() {
        // TODO - check annotation that is not existing nor created
    }

    @Test
    public void testGetExtensionAnnotationValue() {
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//services/OrderInfo");
        Optional<String> value = asmUtils.getExtensionAnnotationValue(orderInfo, "mappedEntityType", false);

        assertTrue(value.isPresent());
        assertThat(value.get(), equalTo("northwind.entities.Order"));

    }

    @Test
    public void testPackageFQName() {
        final EPackage ePackage = (EPackage) resourceSet.getResources().get(0).getEObject("//services");

        assertThat(asmUtils.getPackageFQName(ePackage), equalTo("northwind.services"));
    }


    @Test
    public void testClassFQName() {
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//services/OrderInfo");

        assertThat(asmUtils.getClassifierFQName(orderInfo), equalTo("northwind.services.OrderInfo"));
    }


    @Test
    public void testAttributeFQName() {
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//services/OrderInfo");
        final EAttribute orderDate = (EAttribute) orderInfo.getEStructuralFeature("orderDate");

        assertThat(asmUtils.getAttributeFQName(orderDate), equalTo("northwind.services.OrderInfo#orderDate"));
    }



    @Test
    public void testGetClassByFQName() {
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//services/OrderInfo");
        Optional<EClass> founded = asmUtils.getClassByFQName("northwind.services.OrderInfo");

        assertTrue(founded.isPresent());
        assertThat(founded.get(), equalTo(orderInfo));
    }


    @Test
    public void testGetMappedEntity() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//services/OrderInfo");

        Optional<EClass> mappedType = asmUtils.getMappedEntityType(orderInfo);
        assertTrue(mappedType.isPresent());
        assertThat(mappedType.get(), equalTo(order));
    }

    @Test
    public void testGetMappedAttribute() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//services/OrderInfo");

        final EAttribute orderDate = (EAttribute) order.getEStructuralFeature("orderDate");
        final EAttribute orderInfoDate = (EAttribute) orderInfo.getEStructuralFeature("orderDate");

        Optional<EAttribute> mappedAttribute = asmUtils.getMappedAttribute(orderInfoDate);
        assertTrue(mappedAttribute.isPresent());
        assertThat(mappedAttribute.get(), equalTo(orderDate));
    }


    @Test
    public void testGetMappedReference() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//services/OrderInfo");

        final EReference orderDetails = (EReference) order.getEStructuralFeature("orderDetails");
        final EReference orderInfoItems = (EReference) orderInfo.getEStructuralFeature("items");

        Optional<EReference> mappedReference = asmUtils.getMappedReference(orderInfoItems);
        assertTrue(mappedReference.isPresent());
        assertThat(mappedReference.get(), equalTo(orderDetails));
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
