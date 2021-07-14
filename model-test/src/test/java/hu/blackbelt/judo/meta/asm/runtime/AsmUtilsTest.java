package hu.blackbelt.judo.meta.asm.runtime;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmUtils.setId;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class AsmUtilsTest extends ExecutionContextOnAsmTest {

    @BeforeEach
    public void setUp() throws Exception {
    	super.setUp();
    }

    @Test
    public void testGetPackageFQName() {
    	Optional<EPackage> ePackage = asmUtils.all(EPackage.class).filter(pkg -> "services".equals(pkg.getName()))
                .findAny();

        assertTrue(ePackage.isPresent());
        assertThat(asmUtils.getPackageFQName(ePackage.get()), is("demo.services"));
    }
    
    @Test
    public void testGetClassifierFQName() {
        Optional<EClassifier> eClassifier = asmUtils.all(EClassifier.class)
                .filter(clsf -> "Countries".equals(clsf.getName())).findAny();

        assertTrue(eClassifier.isPresent());
        assertThat(asmUtils.getClassifierFQName(eClassifier.get()), is("demo.types.Countries"));
    }

    @Test
    public void testGetAttributeFQName() {
        Optional<EAttribute> eAttribute = asmUtils.all(EAttribute.class)
                .filter(attr -> "totalNumberOfOrders".equals(attr.getName())).findAny();

        assertTrue(eAttribute.isPresent());
        assertThat(asmUtils.getAttributeFQName(eAttribute.get()), is("demo.services.__Static#totalNumberOfOrders"));
    }

    @Test
    public void testGetReferenceFQName() {
        Optional<EReference> eReference = asmUtils.all(EReference.class).filter(ref -> "owner".equals(ref.getName()))
                .findAny();

        assertTrue(eReference.isPresent());
        assertThat(asmUtils.getReferenceFQName(eReference.get()), is("demo.entities.Category#owner"));
    }

    @Test
    public void testGetOperationFQName() {
        Optional<EOperation> eOperation = asmUtils.all(EOperation.class)
                .filter(op -> "getAllOrders".equals(op.getName())).findAny();

        assertTrue(eOperation.isPresent());
        assertThat(asmUtils.getOperationFQName(eOperation.get()), is("demo.services.__UnboundServices#getAllOrders"));
    }

    @Test
    public void testResolve() {
        Optional<EClassifier> countries = asmUtils.all(EClassifier.class)
                .filter(classifier -> "Countries".equals(classifier.getName())).findAny();
        assertThat(asmUtils.resolve("Countries"), is(countries));

        assertThat(asmUtils.resolve("demo.types.Countries"), is(countries));

        assertFalse(asmUtils.resolve("MissingClass").isPresent());
    }

    @Test
    public void testGetClassByFQName() {
        Optional<EClassifier> productInfo = asmUtils.all(EClassifier.class)
                .filter(classifier -> "ProductInfo".equals(classifier.getName())).findAny();
        assertTrue(productInfo.isPresent());
        assertThat(asmUtils.getClassByFQName("ProductInfo").get(), is(productInfo.get()));

        assertFalse(asmUtils.getClassByFQName("MissingEClass").isPresent());
    }

    @Test
    public void testMappedTransferObjectTypeByEntityTypeMethod() {
        final EcorePackage ecore = EcorePackage.eINSTANCE;

        EAnnotation eannotation = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity")
                .build();

        eannotation.getDetails().put("value", "true");

        final EClass personClass = newEClassBuilder()
                .withName("Person")
                .withEAnnotations(eannotation)
                .withEStructuralFeatures(
                        ImmutableList.of(newEAttributeBuilder()
                                        .withName("firstName")
                                        .withEType(ecore.getEString())
                                        .build(),
                                newEAttributeBuilder()
                                        .withName("lastName")
                                        .withEType(ecore.getEString())
                                        .build()))
                .build();

        final EPackage epackage = newEPackageBuilder()
                .withName("test")
                .withNsPrefix("test")
                .withNsURI("http://com.example.test.ecore")
                .withEClassifiers(personClass)
                .build();

        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
        final File testEcoreFile = new File(targetDir(), "test.ecore");

        final Resource resource = resourceSet.createResource(URI.createFileURI(testEcoreFile.getAbsolutePath()));
        resource.getContents().add(epackage);

        AsmUtils asmUtils = new AsmUtils(resourceSet);

        asmUtils.createMappedTransferObjectTypeByEntityType(personClass);

        assertTrue(asmUtils.getExtensionAnnotationByName(personClass, "mappedEntityType", false).isPresent());

        for (EStructuralFeature estructuralfeature : personClass.getEAllStructuralFeatures()) {
            assertTrue(asmUtils.getExtensionAnnotationByName(estructuralfeature, "binding", false).isPresent());
        }
    }

    @Test
    public void testMappedTransferObjectTypeByEntityTypeMethodNonEntity() {
        final EcorePackage ecore = EcorePackage.eINSTANCE;

        final EClass personClass = newEClassBuilder().withName("Person")
                .withEStructuralFeatures(ImmutableList.of(
                        newEAttributeBuilder().withName("firstName").withEType(ecore.getEString()).build(),
                        newEAttributeBuilder().withName("lastName").withEType(ecore.getEString()).build()))
                .build();

        final EPackage epackage = newEPackageBuilder().withName("test").withNsPrefix("test")
                .withNsURI("http://com.example.test.ecore").withEClassifiers(personClass).build();

        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
        final File testEcoreFile = new File(targetDir(), "test2.ecore");

        final Resource resource = resourceSet.createResource(URI.createFileURI(testEcoreFile.getAbsolutePath()));
        resource.getContents().add(epackage);

        AsmUtils asmUtils = new AsmUtils(resourceSet);

        asmUtils.createMappedTransferObjectTypeByEntityType(personClass);

        assertFalse(asmUtils.getExtensionAnnotationByName(personClass, "mappedEntityType", false).isPresent());
    }

    @Test
    public void testMappedTransferObjectTypeByEntityTypeMethodAlreadyPresent() {
        final EcorePackage ecore = EcorePackage.eINSTANCE;

        EAnnotation eannotation = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity")
                .build();

        eannotation.getDetails().put("value", "true");

        EAnnotation eannotation2 = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/mappedEntityType")
                .build();

        eannotation2.getDetails().put("value", "demo.entities.Person");
        eannotation2.getDetails().put("entityIdPresence", "OPTIONAL");

        EAnnotation bindingAnnotation1 = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/binding")
                .build();

        bindingAnnotation1.getDetails().put("value", "firstName");

        EAnnotation bindingAnnotation2 = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/binding")
                .build();

        bindingAnnotation2.getDetails().put("value", "lastName");

        final EClass personClass = newEClassBuilder().withName("Person")
                .withEAnnotations(ImmutableList.of(eannotation, eannotation2))
                .withEStructuralFeatures(ImmutableList.of(
                        newEAttributeBuilder().withName("firstName").withEAnnotations(bindingAnnotation1)
                                .withEType(ecore.getEString()).build(),
                        newEAttributeBuilder().withName("lastName").withEAnnotations(bindingAnnotation2)
                                .withEType(ecore.getEString()).build()))
                .build();

        final EPackage epackage = newEPackageBuilder().withName("test").withNsPrefix("test")
                .withNsURI("http://com.example.test.ecore").withEClassifiers(personClass).build();

        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
        final File testEcoreFile = new File(targetDir(), "test3.ecore");

        final Resource resource = resourceSet.createResource(URI.createFileURI(testEcoreFile.getAbsolutePath()));
        resource.getContents().add(epackage);

        AsmUtils asmUtils = new AsmUtils(resourceSet);

        asmUtils.createMappedTransferObjectTypeByEntityType(personClass);

        assertTrue(asmUtils.getExtensionAnnotationByName(personClass, "mappedEntityType", false).isPresent());
    }

    @Test
    public void testMappedTransferObjectTypeByEntityTypeMethodInheritance() {
        final EcorePackage ecore = EcorePackage.eINSTANCE;

        EAnnotation eannotation = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity").build();

        EAnnotation eannotation2 = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity").build();

        eannotation.getDetails().put("value", "true");
        eannotation2.getDetails().put("value", "true");

        final EClass customerClass = newEClassBuilder().withName("Customer")
                .withEAnnotations(ImmutableList.of(eannotation))
                .withEStructuralFeatures(ImmutableList.of(
                        newEAttributeBuilder().withName("address").withEType(ecore.getEString()).build()))
                .build();

        final EClass companyClass = newEClassBuilder().withName("Company")
                .withEAnnotations(ImmutableList.of(eannotation2))
                .withESuperTypes(customerClass)
                .withEStructuralFeatures(ImmutableList.of(
                        newEAttributeBuilder().withName("companyName").withEType(ecore.getEString()).build()))
                .build();

        final EPackage epackage = newEPackageBuilder().withName("test").withNsPrefix("test")
                .withNsURI("http://com.example.test.ecore")
                .withEClassifiers(customerClass)
                .withEClassifiers(companyClass)
                .build();

        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
        final File testEcoreFile = new File(targetDir(), "test4.ecore");

        final Resource resource = resourceSet.createResource(URI.createFileURI(testEcoreFile.getAbsolutePath()));
        resource.getContents().add(epackage);

        AsmUtils asmUtils = new AsmUtils(resourceSet);

        asmUtils.createMappedTransferObjectTypeByEntityType(companyClass);

        assertTrue(asmUtils.getExtensionAnnotationByName(companyClass, "mappedEntityType", false).isPresent());

        for (EStructuralFeature estructuralfeature : companyClass.getEAllStructuralFeatures()) {
            assertTrue(asmUtils.getExtensionAnnotationByName(estructuralfeature, "binding", false).isPresent());
        }

        assertTrue(asmUtils.getExtensionAnnotationByName(customerClass, "mappedEntityType", false).isPresent());

        for (EStructuralFeature estructuralfeature : customerClass.getEAllStructuralFeatures()) {
            assertTrue(asmUtils.getExtensionAnnotationByName(estructuralfeature, "binding", false).isPresent());
        }
    }

    @Test
    public void testMappedTransferObjectTypeByEntityTypeMethodReferenceType() {
        final EcorePackage ecore = EcorePackage.eINSTANCE;

        EAnnotation eannotation = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity")
                .build();

        EAnnotation eannotation2 = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity")
                .build();

        eannotation.getDetails().put("value", "true");
        eannotation2.getDetails().put("value", "true");

        final EClass orderClass = newEClassBuilder().withName("Order")
                .withEAnnotations(ImmutableList.of(eannotation))
                .withEStructuralFeatures(ImmutableList.of(
                        newEAttributeBuilder()
                                .withName("shipName")
                                .withEType(ecore.getEString())
                                .build()))
                .build();

        final EClass customerClass = newEClassBuilder().withName("Customer")
                .withEAnnotations(ImmutableList.of(eannotation2))
                .withEStructuralFeatures(ImmutableList.of(
                        newEReferenceBuilder().withName("orders")
                                .withEType(orderClass)
                                .build()))
                .build();

        final EPackage epackage = newEPackageBuilder()
                .withName("test")
                .withNsPrefix("test")
                .withNsURI("http://com.example.test.ecore")
                .withEClassifiers(orderClass)
                .withEClassifiers(customerClass)
                .build();

        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
        final File testEcoreFile = new File(targetDir(), "test5.ecore");

        final Resource resource = resourceSet.createResource(URI.createFileURI(testEcoreFile.getAbsolutePath()));
        resource.getContents().add(epackage);

        AsmUtils asmUtils = new AsmUtils(resourceSet);

        asmUtils.createMappedTransferObjectTypeByEntityType(customerClass);

        assertTrue(asmUtils.getExtensionAnnotationByName(customerClass, "mappedEntityType", false).isPresent());

        for (EStructuralFeature estructuralfeature : customerClass.getEAllStructuralFeatures()) {
            assertTrue(asmUtils.getExtensionAnnotationByName(estructuralfeature, "binding", false).isPresent());
        }

        assertTrue(asmUtils.getExtensionAnnotationByName(orderClass, "mappedEntityType", false).isPresent());

        for (EStructuralFeature estructuralfeature : orderClass.getEAllStructuralFeatures()) {
            assertTrue(asmUtils.getExtensionAnnotationByName(estructuralfeature, "binding", false).isPresent());
        }
    }

    @Test
    public void testMappedTransferObjectTypeByEntityTypeMethodCyclic() {
        final EcorePackage ecore = EcorePackage.eINSTANCE;

        EAnnotation eannotation = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity").build();

        EAnnotation eannotation2 = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity").build();

        eannotation.getDetails().put("value", "true");
        eannotation2.getDetails().put("value", "true");

        final EClass productClass = newEClassBuilder().withName("Product")
                .withEAnnotations(ImmutableList.of(eannotation))
                .build();

        final EClass categoryClass = newEClassBuilder().withName("Category")
                .withEAnnotations(ImmutableList.of(eannotation2))
                .withEStructuralFeatures(ImmutableList.of(
                        newEReferenceBuilder().withName("products")
                                .withEType(productClass)
                                .build()))
                .build();

        productClass.getEStructuralFeatures().add(newEReferenceBuilder().withName("category")
                .withEType(categoryClass)
                .build());

        final EPackage epackage = newEPackageBuilder()
                .withName("test")
                .withNsPrefix("test")
                .withNsURI("http://com.example.test.ecore")
                .withEClassifiers(categoryClass)
                .withEClassifiers(productClass)
                .build();

        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
        final File testEcoreFile = new File(targetDir(), "test6.ecore");

        final Resource resource = resourceSet.createResource(URI.createFileURI(testEcoreFile.getAbsolutePath()));
        resource.getContents().add(epackage);

        AsmUtils asmUtils = new AsmUtils(resourceSet);

        asmUtils.createMappedTransferObjectTypeByEntityType(productClass);

        assertTrue(asmUtils.getExtensionAnnotationByName(productClass, "mappedEntityType", false).isPresent());

        for (EStructuralFeature estructuralfeature : productClass.getEAllStructuralFeatures()) {
            assertTrue(asmUtils.getExtensionAnnotationByName(estructuralfeature, "binding", false).isPresent());
        }

        assertTrue(asmUtils.getExtensionAnnotationByName(categoryClass, "mappedEntityType", false).isPresent());

        for (EStructuralFeature estructuralfeature : categoryClass.getEAllStructuralFeatures()) {
            assertTrue(asmUtils.getExtensionAnnotationByName(estructuralfeature, "binding", false).isPresent());
        }
    }

    @Test
    public void testValidateUniqueXmiids() {
        final EcorePackage ecore = EcorePackage.eINSTANCE;

        final EPackage epackage = newEPackageBuilder()
                .withName("test")
                .withNsPrefix("test")
                .withNsURI("http://com.example.test.ecore")
                .build();

        EAnnotation eannotation = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity").build();

        EAnnotation eannotation2 = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/entity").build();

        eannotation.getDetails().put("value", "true");
        eannotation2.getDetails().put("value", "true");

        final EClass eClass1 = newEClassBuilder().withName("C1")
                .withEAnnotations(ImmutableList.of(eannotation))
                .build();
        epackage.getEClassifiers().add(eClass1);

        final EClass eClass2 = newEClassBuilder().withName("C2")
                .withEAnnotations(ImmutableList.of(eannotation2))
                .build();
        epackage.getEClassifiers().add(eClass2);

        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
        final File testEcoreFile = new File(targetDir(), "test7.ecore");

        final Resource resource = resourceSet.createResource(URI.createFileURI(testEcoreFile.getAbsolutePath()));
        resource.getContents().add(epackage);

        AsmUtils asmUtils = new AsmUtils(resourceSet);

        // #1 - unique xmiid
        setId(eClass1, "EClass1Xmiid");
        setId(eClass2, "EClass2Xmiid");

        asmUtils.validateUniqueXmiids();

        // #2 - non-unique xmiid
        setId(eClass2, "EClass1Xmiid");

        assertThrows(IllegalStateException.class, asmUtils::validateUniqueXmiids);
    }

    public File targetDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath);
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }
}
