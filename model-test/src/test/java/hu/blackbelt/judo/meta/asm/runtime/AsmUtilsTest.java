package hu.blackbelt.judo.meta.asm.runtime;

import com.google.common.collect.ImmutableList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.asmLoadArgumentsBuilder;
import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.loadAsmModel;
import static hu.blackbelt.judo.meta.asm.runtime.AsmUtils.getAnnotationUri;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsmUtilsTest {

    AsmUtils asmUtils;
    AsmModel asmModel;

    @BeforeEach
    public void setUp() throws Exception {
        asmModel = loadAsmModel(asmLoadArgumentsBuilder()
                .uri(URI.createFileURI(new File("target/test-classes/model/northwind-asm.model").getAbsolutePath()))
                .name("test"));
        asmUtils = new AsmUtils(asmModel.getResourceSet());
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
    public void testGetNestedClasses() {
        Optional<EClass> nestedClass = asmUtils.all(EClass.class)
                .filter(c -> "OrderInfoQuery__items".equals(c.getName())).findAny();
        assertTrue(nestedClass.isPresent());
        Optional<EClass> containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery".equals(c.getName()))
                .findAny();
        assertTrue(containerClass.isPresent());

        assertThat(asmUtils.getNestedClasses(containerClass.get()), hasItem(nestedClass.get()));

        Optional<EClass> classWithoutNestedClass = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName()))
                .findAny();
        assertTrue(classWithoutNestedClass.isPresent());

        assertTrue(asmUtils.getNestedClasses(classWithoutNestedClass.get()).isEmpty());
    }

    @Test
    public void testGetContainerClass() {
        Optional<EClass> containerClass = asmUtils.all(EClass.class)
                .filter(c -> "OrderInfoQuery__items".equals(c.getName())).findAny();
        assertTrue(containerClass.isPresent());
        Optional<EClass> nestedClass = asmUtils.all(EClass.class)
                .filter(c -> "OrderInfoQuery__items__Reference".equals(c.getName())).findAny();
        assertTrue(nestedClass.isPresent());

        assertThat(asmUtils.getContainerClass(nestedClass.get()).get(), is(containerClass.get()));

        Optional<EClass> classWithoutContainer = asmUtils.all(EClass.class)
                .filter(c -> "OrderInfoQuery".equals(c.getName())).findAny();
        assertTrue(classWithoutContainer.isPresent());

        assertFalse(asmUtils.getContainerClass(classWithoutContainer.get()).isPresent());
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
    public void testGetResolvedRoot() {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        Optional<EClass> orderInfoQuery = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery".equals(c.getName())).findAny();
        Optional<EReference> graphReference = internalAP.get().getEReferences().stream().filter(eReference -> "ordersAssignedToEmployee".equals(eReference.getName())).findAny();
        assertTrue(graphReference.isPresent());
        assertThat(asmUtils.getResolvedRoot(graphReference.get()), is(orderInfoQuery));

        //negtest: root not found
        final EAnnotation expressionAnnotationOfGraphReferenceWithNotExistingRoot = newEAnnotationBuilder().withSource(getAnnotationUri("expression")).build();
        expressionAnnotationOfGraphReferenceWithNotExistingRoot.getDetails().put("getter", "northwind::entities::Employee.orders");
        expressionAnnotationOfGraphReferenceWithNotExistingRoot.getDetails().put("getter.dialect", "JQL");
        final EReference graphReferenceWithNotExistingRoot = newEReferenceBuilder().withName("ordersAssignedToEmployee")
                .withEAnnotations(expressionAnnotationOfGraphReferenceWithNotExistingRoot)
                .withDerived(true)
                .withUpperBound(-1)
                .build();
        assertThat(asmUtils.getResolvedRoot(graphReferenceWithNotExistingRoot), is(Optional.empty()));

        //negtest: root not a mapped transfer object
        final EAnnotation expressionAnnotationOfGraphReferenceWithInvalidRoot = newEAnnotationBuilder().withSource(getAnnotationUri("expression")).build();
        expressionAnnotationOfGraphReferenceWithInvalidRoot.getDetails().put("getter", "northwind::entities::Employee.orders");
        expressionAnnotationOfGraphReferenceWithInvalidRoot.getDetails().put("getter.dialect", "JQL");

        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());

        final EReference graphReferenceWithInvalidRoot = newEReferenceBuilder().withName("ordersAssignedToEmployee")
                .withEAnnotations(expressionAnnotationOfGraphReferenceWithInvalidRoot)
                .withDerived(true)
                .withUpperBound(-1)
                .withEType(order.get())
                .build();
        assertThat(asmUtils.getResolvedRoot(graphReferenceWithInvalidRoot), is(Optional.empty()));
    }

    @Test
    @Disabled
    public void testGetExposedGraphByFqName() {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        assertTrue(internalAP.isPresent());
        Optional<EReference> graphReference = internalAP.get().getEReferences().stream().filter(eReference -> "ordersAssignedToEmployee".equals(eReference.getName())).findAny();
        assertTrue(graphReference.isPresent());
        assertThat(asmUtils.getExposedGraphByFqName("demo.internalAP#ordersAssignedToEmployee"), is(graphReference));

        //negtest: invalid exposed graph name (not matching exposed graph pattern)
        assertThat(asmUtils.getExposedGraphByFqName("ordersAssignedToEmployee"), is(Optional.empty()));

        //negtest: invalid exposed graph name (access point not found)
        assertThat(asmUtils.getExposedGraphByFqName("demo.AP#ordersAssignedToEmployee"), is(Optional.empty()));
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
