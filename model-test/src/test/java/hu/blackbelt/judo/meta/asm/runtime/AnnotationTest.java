package hu.blackbelt.judo.meta.asm.runtime;

import com.google.common.collect.ImmutableMap;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.asmLoadArgumentsBuilder;
import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.loadAsmModel;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAnnotationBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnnotationTest {

    AsmModel asmModel;
    AsmUtils asmUtils;

    @BeforeEach
    public void setUp () throws Exception {
        asmModel = loadAsmModel(asmLoadArgumentsBuilder()
                .uri(URI.createFileURI(new File("target/test-classes/model/northwind-asm.model").getAbsolutePath()))
                .name("test"));
        asmUtils = new AsmUtils(asmModel.getResourceSet());
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
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "internalAP".equals(a.getName())).findAny();

        assertTrue(exposedByAnnotation.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(exposedByAnnotation.get()), is(internalAP));

        //negtest: annotation ("value") key not found
        EAnnotation annotationWithoutValueKey = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy").build();
        assertThat(asmUtils.getResolvedExposedBy(annotationWithoutValueKey), is(Optional.empty()));

        //negtest: access point not found
        EAnnotation annotationWithInvalidValue = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy").build();
        annotationWithInvalidValue.getDetails().put("value", "demo.service.internalAP");
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

        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "internalAP".equals(a.getName())).findAny();
        assertTrue(internalAP.isPresent());
        assertThat(asmUtils.getAccessPointsOfOperation(getAllOrders.get()), hasItems(internalAP.get()));
    }

    @Test
    public void testGetExposedServicesOfAccessPoint () {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        assertTrue(internalAP.isPresent());

        //EOperations exposed by internalAP
        Optional<EOperation> getAllOrders = asmUtils.all(EOperation.class).filter(o -> "getAllOrders".equals(o.getName())).findAny();
        assertTrue(getAllOrders.isPresent());
        Optional<EOperation> createOrder = asmUtils.all(EOperation.class).filter(o -> "createOrder".equals(o.getName())).findAny();
        assertTrue(createOrder.isPresent());

        assertThat(asmUtils.getExposedServicesOfAccessPoint(internalAP.get()), hasItems(getAllOrders.get(), createOrder.get()));
    }

    //@Test
    public void testGetResolvedRoot () {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        Optional<EClass> orderInfoQuery = asmUtils.all(EClass.class).filter(a -> "OrderInfoQuery".equals(a.getName())).findAny();
        Optional<EAnnotation> graph = internalAP.get().getEAnnotations().stream().filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/graph".equals(a.getSource())).findAny();
        assertTrue(graph.isPresent());
        //assertThat(asmUtils.getResolvedRoot(graph.get()), is(orderInfoQuery));

        //negtest: no root key
        EAnnotation annotationWithoutRoot = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/graph").build();
        //assertThat(asmUtils.getResolvedRoot(annotationWithoutRoot), is(Optional.empty()));

        //negtest: root not found
        EAnnotation annotationWithNotExistingRoot = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/graph").build();
        annotationWithNotExistingRoot.getDetails().put("root", "demo.entities.OrderInfoQuery");
        //assertThat(asmUtils.getResolvedRoot(annotationWithNotExistingRoot), is(Optional.empty()));

        //negtest: root not a mapped transfer object
        EAnnotation annotationWithInvalidRoot = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/graph").build();
        annotationWithInvalidRoot.getDetails().put("root", "demo.entities.Order");
        //assertThat(asmUtils.getResolvedRoot(annotationWithInvalidRoot), is(Optional.empty()));
    }

    //@Test
    public void testGetExposedGraphByFqName () {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        assertTrue(internalAP.isPresent());
        Optional<EAnnotation> internalAPGraphAnnotation = internalAP.get().getEAnnotations().stream().filter(annotation -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/graph".equals(annotation.getSource())).findAny();
        assertThat(asmUtils.getExposedGraphByFqName("demo.internalAP/ordersAssignedToEmployee"), is(internalAPGraphAnnotation));

        //negtest: invalid exposed graph name (not matching exposed graph pattern)
        assertThat(asmUtils.getExposedGraphByFqName("ordersAssignedToEmployee"), is(Optional.empty()));

        //negtest: invalid exposed graph name (access point not found)
        assertThat(asmUtils.getExposedGraphByFqName("demo.AP/ordersAssignedToEmployee"), is(Optional.empty()));
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
        Optional<EClass> internationalOrderInfoQueryItems = asmUtils.all(EClass.class).filter(c -> "InternationalOrderInfoQuery__items".equals(c.getName())).findAny();
        assertTrue(internationalOrderInfoQueryItems.isPresent());

        Optional<EOperation> get = internationalOrderInfoQueryItems.get().getEAllOperations().stream().filter(operation -> "get".equals(operation.getName())).findAny();
        assertTrue(get.isPresent());

        assertTrue(asmUtils.annotatedAsFalse(get.get(), "stateful"));
        assertFalse(asmUtils.annotatedAsFalse(get.get(), "missingAnnotation"));

        Optional<EOperation> set = internationalOrderInfoQueryItems.get().getEAllOperations().stream().filter(operation -> "set".equals(operation.getName())).findAny();
        assertTrue(set.isPresent());

        assertTrue(asmUtils.annotatedAsTrue(set.get(), "stateful"));
        assertFalse(asmUtils.annotatedAsTrue(set.get(), "missingAnnotation"));

    }

}
