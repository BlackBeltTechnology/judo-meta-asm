package hu.blackbelt.judo.meta.asm.runtime;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.buildAsmModel;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAnnotationBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAttributeBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEDataTypeBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEOperationBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEPackageBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEReferenceBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.useEPackage;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.useEReference;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import hu.blackbelt.epsilon.runtime.execution.exceptions.EvlScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.Slf4jLog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnnotationTest {

    AsmModel asmModel;
    AsmUtils asmUtils;
    private final String createdSourceModelName = "urn:asm.judo-meta-asm";
    private static final Logger logger = LoggerFactory.getLogger(AnnotationTest.class);

    @BeforeEach
    public void setUp () throws Exception {
    	asmModel = buildAsmModel()
    			.uri(URI.createURI(createdSourceModelName))
                .name("test")
                .build();
        asmUtils = new AsmUtils(asmModel.getResourceSet());
        
        EDataType timestamp = newEDataTypeBuilder().withName("Timestamp").withInstanceClassName("java.time.OffsetDateTime").build();
        EDataType stringType = newEDataTypeBuilder().withName("String").withInstanceClassName("java.lang.String").build();
        EDataType integerType = newEDataTypeBuilder().withName("Integer").withInstanceClassName("java.lang.Integer").build();
    	
        EAttribute orderDate = newEAttributeBuilder().withName("orderDate").withEType(timestamp).build();
        EAttribute orderDateAnnotated = newEAttributeBuilder().withName("orderDate").withEType(timestamp).build();
        EAttribute shipperName = newEAttributeBuilder().withName("shipperName").withEType(stringType).build();
        EAttribute totalNumberOfOrders = newEAttributeBuilder().withName("totalNumberOfOrders").withEType(integerType).build();
        
    	EReference items = newEReferenceBuilder().withName("items").build();
    	EReference orderDetails = newEReferenceBuilder().withName("orderDetails").build();
    	EReference owner = newEReferenceBuilder().withName("owner").build();
    	EReference ordersAssignedToEmployee = newEReferenceBuilder().withName("ordersAssignedToEmployee").build();
    	
    	EOperation getAllOrders = newEOperationBuilder().withName("getAllOrders").build();
    	
    	EClass category = newEClassBuilder().withName("Category").withEStructuralFeatures(owner).build();
    	EClass orderInfo = newEClassBuilder().withName("OrderInfo").withEStructuralFeatures(orderDateAnnotated,items).build();
    	EClass productInfo = newEClassBuilder().withName("ProductInfo").build();
    	EClass order = newEClassBuilder().withName("Order").withEStructuralFeatures(orderDate,orderDetails).build();
    	EClass internationalOrderInfo = newEClassBuilder().withName("InternationalOrderInfo").withEStructuralFeatures(shipperName).build();
    	EClass countries = newEClassBuilder().withName("Countries").build();
    	EClass __static = newEClassBuilder().withName("__Static").withEStructuralFeatures(totalNumberOfOrders).build();
    	EClass unboundServices = newEClassBuilder().withName("__UnboundServices").withEOperations(getAllOrders).build();
    	EClass internalAP = newEClassBuilder().withName("InternalAP").withEStructuralFeatures(ordersAssignedToEmployee).build();
    	EClass employee = newEClassBuilder().withName("Employee").build();
    	EClass orderDetail = newEClassBuilder().withName("OrderDetail").build();
    	
    	useEReference(items).withEType(orderInfo).build();
    	useEReference(owner).withEType(employee).build();
    	useEReference(ordersAssignedToEmployee).withEType(order).build();
    	useEReference(orderDetails).withEType(orderDetail).build();
    	
    	//packages
    	EPackage demo = newEPackageBuilder().withName("demo").withNsURI("http://blackbelt.hu/judo/northwind/northwind/demo")
    			.withNsPrefix("runtimenorthwindNorthwindDemo").build();
    	EPackage services = newEPackageBuilder().withName("services").withNsURI("http://blackbelt.hu/judo/northwind/northwind/services")
    			.withNsPrefix("runtimenorthwindNorthwindServices").build();
    	EPackage entities = newEPackageBuilder().withName("entities").withNsURI("http://blackbelt.hu/judo/northwind/northwind/entities")
    			.withNsPrefix("runtimenorthwindNorthwindEntities").build();
    	EPackage types = newEPackageBuilder().withName("types")
    			.withNsURI("http://blackbelt.hu/judo/northwind/northwind/types")
    			.withNsPrefix("runtimenorthwindNorthwindTypes").build();
    	
    	useEPackage(services).withEClassifiers(productInfo,orderInfo,internationalOrderInfo,unboundServices,__static).build();
    	useEPackage(entities).withEClassifiers(order,category,employee,orderDetail).build();
    	useEPackage(types).withEClassifiers(countries,timestamp,stringType,integerType).build();
    	useEPackage(demo).withESubpackages(services,entities,types).withEClassifiers(internalAP).build();
    	
    	asmModel.addContent(demo);
    	
    	EAnnotation annotation = asmUtils.getExtensionAnnotationByName(orderInfo, "mappedEntityType", true).get();
    	annotation.getDetails().put("value", asmUtils.getClassifierFQName(order));
    	EAnnotation entityAnnotation = asmUtils.getExtensionAnnotationByName(order, "entity", true).get();
    	entityAnnotation.getDetails().put("value", "true");
    	EAnnotation attributeAnnotation = asmUtils.getExtensionAnnotationByName(orderDateAnnotated, "binding", true).get();
    	attributeAnnotation.getDetails().put("value", orderDate.getName());
    	EAnnotation referenceAnnotation = asmUtils.getExtensionAnnotationByName(items, "binding", true).get();
    	referenceAnnotation.getDetails().put("value", orderDetails.getName());
    	EAnnotation shipperNameAnnotation = asmUtils.getExtensionAnnotationByName(shipperName, "constraints", true).get();
    	shipperNameAnnotation.getDetails().put("maxLength", "255");
    	EAnnotation operationAnnotation = asmUtils.getExtensionAnnotationByName(getAllOrders, "exposedBy", true).get();
    	operationAnnotation.getDetails().put("value", asmUtils.getClassifierFQName(internalAP));
    	EAnnotation apAnnotation = asmUtils.getExtensionAnnotationByName(internalAP, "accessPoint", true).get();
    	apAnnotation.getDetails().put("value", "true");
    	EAnnotation employeeAnnotation = asmUtils.getExtensionAnnotationByName(employee, "entity", true).get();
    	employeeAnnotation.getDetails().put("value", "true");
    	EAnnotation orderDetailAnnotation = asmUtils.getExtensionAnnotationByName(orderDetail, "entity", true).get();
    	orderDetailAnnotation.getDetails().put("value", "true");
    	
    	log.info(asmModel.getDiagnosticsAsString());
    	assertTrue(asmModel.isValid());
    	runEpsilonOnAsm();
    }

    private void runEpsilonOnAsm() throws Exception {
        try {
            AsmEpsilonValidator.validateAsm(new Slf4jLog(),
            		asmModel,
            		AsmEpsilonValidator.calculateAsmValidationScriptURI(),
            		Collections.emptyList(),
            		Collections.emptyList());
        } catch (EvlScriptExecutionException ex) {
            logger.error("EVL failed", ex);
            logger.error("\u001B[31m - unexpected errors: {}\u001B[0m", ex.getUnexpectedErrors());
            logger.error("\u001B[33m - unexpected warnings: {}\u001B[0m", ex.getUnexpectedWarnings());
            throw ex;
        }
    }
    
    @Test
    public void testGetExtensionAnnotationExisting () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();

        final Optional<EAnnotation> existing = asmUtils.getExtensionAnnotationByName(order.get(), "entity", false);

        assertTrue(existing.isPresent());
        assertTrue(Boolean.valueOf(existing.get().getDetails().get(AsmUtils.EXTENDED_METADATA_DETAILS_VALUE_KEY)));
    }

    @Test
    public void testGetExtensionAnnotationNotExistingButCreated () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EAnnotation> originallyMissingButCreatedAnnotation = asmUtils.getExtensionAnnotationByName(order.get(), "toBeCreated", true);

        assertTrue(originallyMissingButCreatedAnnotation.isPresent());

        assertFalse(Boolean.parseBoolean(originallyMissingButCreatedAnnotation.get().getDetails().get(AsmUtils.EXTENDED_METADATA_DETAILS_VALUE_KEY)));
    }

    @Test
    public void testGetExtensionAnnotationNotExisting () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EAnnotation> missingAndNotCreatedAnnotation = asmUtils.getExtensionAnnotationByName(order.get(), "notToBeCreated", false);

        assertFalse(missingAndNotCreatedAnnotation.isPresent());
    }

    @Test
    public void testGetExtensionAnnotationListByName () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        EList<EAnnotation> annotationList = asmUtils.getExtensionAnnotationListByName(order.get(), "entity");

        assertFalse(annotationList.isEmpty());
        assertTrue(annotationList.contains(asmUtils.getExtensionAnnotationByName(order.get(), "entity", false).get()));
    }

    @Test
    public void testAddExtensionAnnotation () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        //annotation not already present
        assertTrue(asmUtils.addExtensionAnnotation(order.get(), "NewAnnotation", "value"));
        assertTrue(asmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).isPresent());
        //negtest: annotation already present
        assertFalse(asmUtils.addExtensionAnnotation(order.get(), "NewAnnotation", "value"));
    }

    @Test
    public void testAddExtensionAnnotationDetails () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());

        asmUtils.addExtensionAnnotationDetails(order.get(), "NewAnnotation", ImmutableMap.of("key1", "value1"));
        assertTrue(asmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).get().getDetails().containsKey("key1"));

        asmUtils.addExtensionAnnotationDetails(order.get(), "NewAnnotation", ImmutableMap.of("key2", "value2"));
        assertTrue(asmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).get().getDetails().containsKey("key2"));
    }

    @Test
    public void testGetExtensionAnnotationValue () {
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        Optional<String> mappedEntityTypeValue = asmUtils.getExtensionAnnotationValue(orderInfo.get(), "mappedEntityType", false);

        assertTrue(mappedEntityTypeValue.isPresent());
        assertThat(mappedEntityTypeValue.get(), equalTo("demo.entities.Order"));

        Optional<String> exposedGraphValue = asmUtils.getExtensionAnnotationValue(orderInfo.get(), "missingAnnotation", false);
        assertFalse(exposedGraphValue.isPresent());
    }

    @Test
    public void testGetExtensionAnnotationCustomValue () {
        Optional<EClass> internationalOrderInfo = asmUtils.all(EClass.class).filter(c -> "InternationalOrderInfo".equals(c.getName())).findAny();
        assertTrue(internationalOrderInfo.isPresent());
        Optional<EAttribute> shipperName = internationalOrderInfo.get().getEAttributes().stream().filter(attribute -> "shipperName".equals(attribute.getName())).findAny();
        assertTrue(shipperName.isPresent());

        assertThat(asmUtils.getExtensionAnnotationCustomValue(shipperName.get(), "constraints", "maxLength", false).get(), equalTo("255"));

        //negtest: annotation key not found
        assertFalse(asmUtils.getExtensionAnnotationCustomValue(shipperName.get(), "constraints", "missingKey", false).isPresent());

        //negtest: annotation not found
        assertFalse(asmUtils.getExtensionAnnotationCustomValue(shipperName.get(), "missingAnnotation", "maxLength", false).isPresent());
    }

    @Test
    public void testGetResolvedExposedBy () {
        Optional<EAnnotation> exposedByAnnotation = asmUtils.all(EAnnotation.class).filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy".equals(a.getSource())).findAny();
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "InternalAP".equals(a.getName())).findAny();

        assertTrue(exposedByAnnotation.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(exposedByAnnotation.get()), is(internalAP));

        //negtest: annotation ("value") key not found
        EAnnotation annotationWithoutValueKey = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy").build();
        assertThat(asmUtils.getResolvedExposedBy(annotationWithoutValueKey), is(Optional.empty()));

        //negtest: access point not found
        EAnnotation annotationWithInvalidValue = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy").build();
        annotationWithInvalidValue.getDetails().put("value", "demo.services.InternalAP");
        assertThat(asmUtils.getResolvedExposedBy(annotationWithInvalidValue), is(Optional.empty()));

        //negtest: annotation not pointing to an AccessPoint
        EAnnotation annotationExposingInvalidAccessPoint = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy").build();
        annotationExposingInvalidAccessPoint.getDetails().put("value", "demo.entities.Order");
        assertThat(asmUtils.getResolvedExposedBy(annotationExposingInvalidAccessPoint), is(Optional.empty()));
    }

    @Test
    public void testGetAccessPointsOfOperation () {
        Optional<EClass> unboundServices = asmUtils.all(EClass.class).filter(c -> "__UnboundServices".equals(c.getName())).findAny();
        assertTrue(unboundServices.isPresent());
        Optional<EOperation> getAllOrders = unboundServices.get().getEOperations().stream().filter(o -> "getAllOrders".equals(o.getName())).findAny();
        assertTrue(getAllOrders.isPresent());

        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "InternalAP".equals(a.getName())).findAny();
        assertTrue(internalAP.isPresent());
        assertThat(asmUtils.getAccessPointsOfOperation(getAllOrders.get()), hasItems(internalAP.get()));
    }

    @Test
    public void testGetMappedEntity () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        assertTrue(orderInfo.isPresent());

        Optional<EClass> mappedType = asmUtils.getMappedEntityType(orderInfo.get());
        assertTrue(mappedType.isPresent());
        assertThat(mappedType.get(), equalTo(order.get()));

        final EAnnotation mappedEntityTypeAnnotationWithMissingKey = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/mappedEntityType").build();
        final EClass classWithInvalidMappedEntityTypeAnnotation = newEClassBuilder().withName("ClassWithInvalidMappedEntityTypeAnnotation").withEAnnotations(mappedEntityTypeAnnotationWithMissingKey).build();
        assertFalse(asmUtils.getMappedEntityType(classWithInvalidMappedEntityTypeAnnotation).isPresent());
    }

    @Test
    public void testGetMappedAttribute () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        assertTrue(orderInfo.isPresent());

        Optional<EAttribute> orderDate = order.get().getEAllAttributes().stream().filter(attribute -> "orderDate".equals(attribute.getName())).findAny();
        Optional<EAttribute> orderInfoDate = orderInfo.get().getEAllAttributes().stream().filter(attribute -> "orderDate".equals(attribute.getName())).findAny();
        assertTrue(orderDate.isPresent());
        assertTrue(orderInfoDate.isPresent());

        Optional<EAttribute> mappedAttribute = asmUtils.getMappedAttribute(orderInfoDate.get());
        assertTrue(mappedAttribute.isPresent());
        assertThat(mappedAttribute.get(), equalTo(orderDate.get()));
    }

    @Test
    public void testGetMappedReference () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        assertTrue(orderInfo.isPresent());

        Optional<EReference> orderDetails = order.get().getEAllReferences().stream().filter(reference -> "orderDetails".equals(reference.getName())).findAny();
        Optional<EReference> orderInfoItems = orderInfo.get().getEAllReferences().stream().filter(reference -> "items".equals(reference.getName())).findAny();

        Optional<EReference> mappedReference = asmUtils.getMappedReference(orderInfoItems.get());
        assertTrue(mappedReference.isPresent());
        assertThat(mappedReference.get(), equalTo(orderDetails.get()));
    }

    @Test
    public void testAnnotatedAsFalseOrTrue () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());

        assertTrue(asmUtils.annotatedAsTrue(order.get(), "entity"));
        assertFalse(asmUtils.annotatedAsTrue(order.get(), "missingAnnotation"));

        assertFalse(asmUtils.annotatedAsFalse(order.get(), "entity"));
        assertFalse(asmUtils.annotatedAsFalse(order.get(), "missingAnnotation"));
    }
}
