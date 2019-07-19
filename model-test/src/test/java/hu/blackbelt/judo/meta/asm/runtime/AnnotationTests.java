package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.loadArgumentsBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class AnnotationTests {

    static Logger log = LoggerFactory.getLogger(AnnotationTests.class);

    ResourceSet resourceSet;
    AsmUtils asmUtils;

    @Before
    public void setUp() throws Exception {
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());

        asmUtils = new AsmUtils(resourceSet);
        
        AsmModel.loadAsmModel(loadArgumentsBuilder()
                .resourceSet(Optional.of(resourceSet))
                .uri(URI.createFileURI(new File("src/test/model/asm.model").getAbsolutePath()))
                .name("test")
                .build());
    }

    @After
    public void tearDown() {
        resourceSet = null;
    }

    @Test
    public void testGetExtensionAnnotationExisting() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");

        final Optional<EAnnotation> existing = AsmUtils.getExtensionAnnotationByName(order, "entity", false);

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
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");
        Optional<String> value = AsmUtils.getExtensionAnnotationValue(orderInfo, "mappedEntityType", false);

        assertTrue(value.isPresent());
        assertThat(value.get(), equalTo("demo.entities.Order"));

    }

    @Test
    public void testPackageFQName() {
        final EPackage ePackage = (EPackage) resourceSet.getResources().get(0).getEObject("//service");

        assertThat(AsmUtils.getPackageFQName(ePackage), equalTo("demo.service"));
    }


    @Test
    public void testClassFQName() {
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");

        assertThat(AsmUtils.getClassifierFQName(orderInfo), equalTo("demo.service.OrderInfo"));
    }


    @Test
    public void testAttributeFQName() {
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");
        final EAttribute orderDate = (EAttribute) orderInfo.getEStructuralFeature("orderDate");

        assertThat(AsmUtils.getAttributeFQName(orderDate), equalTo("demo.service.OrderInfo#orderDate"));
    }



    @Test
    public void testGetClassByFQName() {
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");
        Optional<EClass> founded = asmUtils.getClassByFQName("demo.service.OrderInfo");

        assertTrue(founded.isPresent());
        assertThat(founded.get(), equalTo(orderInfo));
    }


    @Test
    public void testGetMappedEntity() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");

        Optional<EClass> mappedType = asmUtils.getMappedEntityType(orderInfo);
        assertTrue(mappedType.isPresent());
        assertThat(mappedType.get(), equalTo(order));
    }

    @Test
    public void testGetMappedAttribute() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");

        final EAttribute orderDate = (EAttribute) order.getEStructuralFeature("orderDate");
        final EAttribute orderInfoDate = (EAttribute) orderInfo.getEStructuralFeature("orderDate");

        Optional<EAttribute> mappedAttribute = asmUtils.getMappedAttribute(orderInfoDate);
        assertTrue(mappedAttribute.isPresent());
        assertThat(mappedAttribute.get(), equalTo(orderDate));
    }


    @Test
    public void testGetMappedReference() {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");

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
