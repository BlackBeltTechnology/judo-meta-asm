package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.loadArgumentsBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAnnotationBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnnotationTest {

    static Logger log = LoggerFactory.getLogger(AnnotationTest.class);

    ResourceSet resourceSet;
    AsmUtils asmUtils;

    @BeforeEach
    public void setUp () throws Exception {
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());

        asmUtils = new AsmUtils(resourceSet);

        AsmModel.loadAsmModel(loadArgumentsBuilder()
                .resourceSet(Optional.of(resourceSet))
                .uri(URI.createFileURI(new File("src/test/model/asm.model").getAbsolutePath()))
                .name("test")
                .build());
    }

    @AfterEach
    public void tearDown () {
        resourceSet = null;
    }

    @Test
    public void testGetExtensionAnnotationExisting () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();

        final Optional<EAnnotation> existing = AsmUtils.getExtensionAnnotationByName(order.get(), "entity", false);

        assertTrue(existing.isPresent());
        assertTrue(Boolean.valueOf(existing.get().getDetails().get(AsmUtils.EXTENDED_METADATA_DETAILS_VALUE_KEY)));
    }

    @Test
    public void testGetExtensionAnnotationNotExistingButCreated () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EAnnotation> notExistingButCreated = AsmUtils.getExtensionAnnotationByName(order.get(), "toBeCreated", true);
        assertTrue(notExistingButCreated.isPresent());
    }

    @Test
    public void testGetExtensionAnnotationNotExisting () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EAnnotation> notExistingNotCreated = AsmUtils.getExtensionAnnotationByName(order.get(), "notToBeCreated", false);
        assertFalse(notExistingNotCreated.isPresent());
    }

    @Test
    public void testGetExtensionAnnotationListByName () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        EList<EAnnotation> annotationList = AsmUtils.getExtensionAnnotationListByName(order.get(), "entity");

        assertFalse(annotationList.isEmpty());
        assertTrue(annotationList.contains(AsmUtils.getExtensionAnnotationByName(order.get(), "entity", false).get()));
    }

    @Test
    public void testAddExtensionAnnotation () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        //annotation not already present
        assertTrue(AsmUtils.addExtensionAnnotation(order.get(), "NewAnnotation", "value"));
        assertTrue(AsmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).isPresent());
        //negtest: annotation already present
        assertFalse(AsmUtils.addExtensionAnnotation(order.get(), "NewAnnotation", "value"));
    }

    @Test
    @DisplayName("Add map of annotation details to annotation of a model element ☺")
    public void testAddExtensionAnnotationDetails () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());

        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        //annotation.isPresent()
        AsmUtils.addExtensionAnnotationDetails(order.get(), "entity", map);
        assertTrue(AsmUtils.getExtensionAnnotationByName(order.get(), "entity", false).get().getDetails().containsKey("key1"));
        assertTrue(AsmUtils.getExtensionAnnotationByName(order.get(), "entity", false).get().getDetails().containsKey("key2"));

        AsmUtils.addExtensionAnnotationDetails(order.get(), "NewAnnotation", map);
        assertTrue(AsmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).get().getDetails().containsKey("key1"));
        assertTrue(AsmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).get().getDetails().containsKey("key2"));
    }

    @Test
    public void testGetExtensionAnnotationValue () {
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        Optional<String> value = AsmUtils.getExtensionAnnotationValue(orderInfo.get(), "mappedEntityType", false);

        assertTrue(value.isPresent());
        assertThat(value.get(), equalTo("demo.entities.Order"));

        //negtest: annotation not present
        AsmUtils.getExtensionAnnotationValue(orderInfo.get(), "value", true);

        //negtest: value is null
        Map<String, String> map = new HashMap<>();
        map.put("value", null);
        asmUtils.addExtensionAnnotationDetails(orderInfo.get(), "negtest", map);
        //negtest: ...and logging
        AsmUtils.getExtensionAnnotationValue(orderInfo.get(), "negtest", true);
        //negtest: ...and not logging
        AsmUtils.getExtensionAnnotationValue(orderInfo.get(), "negtest", false);
    }

    @Test
    public void testGetExtensionAnnotationCustomValue () {
        Optional<EClass> internationalOrderInfo = asmUtils.all(EClass.class).filter(c -> "InternationalOrderInfo".equals(c.getName())).findAny();
        Optional<String> value = asmUtils.getExtensionAnnotationCustomValue(internationalOrderInfo.get(), "mappedEntityType", "entityIdPresence", false);

        assertTrue(internationalOrderInfo.isPresent());
        assertTrue(value.isPresent());
        assertThat(value.get(), equalTo("OPTIONAL"));

        //negtest: annotation not present

        Optional<String> negtest_value = asmUtils.getExtensionAnnotationCustomValue(internationalOrderInfo.get(), "negtest", "negtest", true);
        asmUtils.getExtensionAnnotationCustomValue(internationalOrderInfo.get(), "negtest", "negtest", false);
        assertFalse(negtest_value.isPresent());

        Map<String, String> map = new HashMap<>();
        map.put("negtest", null);
        asmUtils.addExtensionAnnotationDetails(internationalOrderInfo.get(), "negtest", map);
        //negtest: ...and logging
        AsmUtils.getExtensionAnnotationCustomValue(internationalOrderInfo.get(), "negtest", "negtest", true);
        //negtest: ...and not logging
        AsmUtils.getExtensionAnnotationCustomValue(internationalOrderInfo.get(), "negtest", "negtest", false);
    }

    @Test
    public void expectTheUnexpected () {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        Optional<EAnnotation> accessPoint = asmUtils.all(EAnnotation.class).filter(a -> asmUtils.getAnnotationUri("accessPoint").equals(a.getSource())).findAny();
        assertTrue(internalAP.isPresent());
        assertTrue(accessPoint.isPresent());
        assertFalse(asmUtils.annotatedAsFalse(internalAP.get(), "accessPoint"));
        assertFalse(asmUtils.annotatedAsFalse(internalAP.get(), "negtest"));
        assertTrue(asmUtils.annotatedAsTrue(internalAP.get(), "accessPoint"));

        Map<String, String> map = new HashMap<>();
        map.put("value", "false");
        EAnnotation negtestAnnot = newEAnnotationBuilder().withSource(asmUtils.getAnnotationUri("accessPoint")).build();
        EClass negtestAP = newEClassBuilder().withEAnnotations(negtestAnnot).build();
        asmUtils.addExtensionAnnotationDetails(negtestAP, "accessPoint", map);
        assertFalse(asmUtils.annotatedAsTrue(negtestAP, "accessPoint"));
    }

    @Test
    public void testGetClassByFQName () {
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");
        Optional<EClass> founded = asmUtils.getClassByFQName("demo.service.OrderInfo");

        assertTrue(founded.isPresent());
        assertThat(founded.get(), equalTo(orderInfo));
    }


    @Test
    public void testGetAccessPointsOfUnboundOperation () {
        Optional<EClass> unboundServices = asmUtils.all(EClass.class).filter(c -> "ʘUnboundServices".equals(c.getName())).findAny();
        assertTrue(unboundServices.isPresent());
        Optional<EOperation> getAllOrders = unboundServices.get().getEOperations().stream().filter(o -> "getAllOrders".equals(o.getName())).findAny();
        assertTrue(getAllOrders.isPresent());

        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "internalAP".equals(a.getName())).findAny();
        assertTrue(internalAP.isPresent());
        assertThat(asmUtils.getAccessPointsOfUnboundOperation(getAllOrders.get()), hasItem(internalAP.get()));

        //negtest: not an unbound operation
        Optional<EClass> orderinfo_shipper = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘshipper".equals(c.getName())).findAny();
        assertTrue(orderinfo_shipper.isPresent());
        Optional<EOperation> getOper = orderinfo_shipper.get().getEOperations().stream().filter(o -> "get".equals(o.getName())).findAny();
        assertTrue(getOper.isPresent());
        assertThat(asmUtils.getAccessPointsOfUnboundOperation(getOper.get()), equalTo(ECollections.emptyEList()));
    }

    @Test
    public void testGetExposedServicesOfAccessPoint () {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        assertTrue(internalAP.isPresent());

        //EOperations annotating to internalAP
        Optional<EOperation> getAllOrders = asmUtils.all(EOperation.class).filter(o -> "getAllOrders".equals(o.getName())).findAny();
        assertTrue(getAllOrders.isPresent());
        Optional<EOperation> createOrder = asmUtils.all(EOperation.class).filter(o -> "createOrder".equals(o.getName())).findAny();
        assertTrue(createOrder.isPresent());

        assertThat(asmUtils.getExposedServicesOfAccessPoint(internalAP.get()), hasItems(getAllOrders.get(), createOrder.get()));

        //negtest: not an access point
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(orderInfo.isPresent());
        assertThat(asmUtils.getExposedServicesOfAccessPoint(orderInfo.get()), equalTo(ECollections.emptyEList()));
    }

    @Test
    public void testGetResolvedRoot () {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        Optional<EClass> productInfo = asmUtils.all(EClass.class).filter(a -> "OrderInfoQuery".equals(a.getName())).findAny();
        Optional<EAnnotation> graph = internalAP.get().getEAnnotations().stream().filter(a -> asmUtils.getAnnotationUri("graph").equals(a.getSource())).findAny();
        assertTrue(graph.isPresent());
        assertThat(asmUtils.getResolvedRoot(graph.get()), equalTo(productInfo));

        //negtest: no root = no fun
        Optional<EAttribute> entity = asmUtils.all(EAttribute.class).filter(a -> "totalNumberOfOrders".equals(a.getName())).findAny();
        Optional<EAnnotation> expression = entity.get().getEAnnotations().stream().filter(a -> asmUtils.getAnnotationUri("expression").equals(a.getSource())).findAny();
        assertTrue(expression.isPresent());
        assertThat(asmUtils.getResolvedRoot(expression.get()), equalTo(Optional.empty()));

        //TODO negtest: yeproot, notmapped riperoni
    }

    @Test
    public void testGetResolvedExposedBy () {
        Optional<EAnnotation> exposedBy = asmUtils.all(EAnnotation.class).filter(a -> asmUtils.getAnnotationUri("exposedBy").equals(a.getSource())).findAny();
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "internalAP".equals(a.getName())).findAny();

        assertTrue(exposedBy.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(exposedBy.get()), equalTo(internalAP));

        //negtest: not an exposedBy
        Optional<EAnnotation> entity = asmUtils.all(EAnnotation.class).filter(a -> asmUtils.getAnnotationUri("entity").equals(a.getSource())).findAny();
        assertTrue(entity.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(entity.get()), equalTo(Optional.empty()));

        //negtest: key not value
        EAnnotation notValueKey = newEAnnotationBuilder()
                .withSource(asmUtils.getAnnotationUri("exposedBy"))
                .build();
        notValueKey.getDetails().put("notValue", "true");
        assertThat(asmUtils.getResolvedExposedBy(notValueKey), equalTo(Optional.empty()));


        //negtest: invalid resolved exposedBy
        Map<String, String> invalidExposedBy = new HashMap<>();
        invalidExposedBy.put("value", "invalidExposedByFQName");
        EAnnotation negtestInvalidAnnot = newEAnnotationBuilder().withSource(asmUtils.getAnnotationUri("exposedBy")).build();
        EClass negtestEClass = newEClassBuilder().withName("negtest").withEAnnotations(negtestInvalidAnnot).build();
        asmUtils.addExtensionAnnotationDetails(negtestEClass, "exposedBy", invalidExposedBy);
        assertThat(asmUtils.getResolvedExposedBy(negtestInvalidAnnot), equalTo(Optional.empty()));

        //negtest: not an accesspoint
        Map<String, String> notAnAccessPoint = new HashMap<>();
        notAnAccessPoint.put("value", "demo.entities.Order");
        EAnnotation negtestNotAP = newEAnnotationBuilder().withSource(asmUtils.getAnnotationUri("exposedBy")).build();
        EClass negtestClass = newEClassBuilder().withName("negtest").withEAnnotations(negtestNotAP).build();
        asmUtils.addExtensionAnnotationDetails(negtestClass, "exposedBy", notAnAccessPoint);
        assertThat(asmUtils.getResolvedExposedBy(negtestNotAP), equalTo(Optional.empty()));
    }

    @Test
    public void testGetMappedEntityType () {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");
        assertThat(asmUtils.getMappedEntityType(orderInfo).get(), equalTo(order));

        //negtest: mappedEntityType notPresent
        Map<String, String> map = new HashMap<>();
        map.put("value", "negtest");
        EClass negtestClass = newEClassBuilder().build();
        AsmUtils.addExtensionAnnotationDetails(negtestClass, "mappedEntityType", map);
        assertThat(asmUtils.getMappedEntityType(negtestClass), equalTo(Optional.empty()));

        //negtest: mappedEntityType isPresent, but not an EntityType
        Map<String, String> map2 = new HashMap<>();
        map2.put("value", "false");
        EClass negtestOrder = newEClassBuilder().withName("NegtestOrder").withEAnnotations(
                newEAnnotationBuilder().withSource(asmUtils.getAnnotationUri("entity")).build()
        ).build();
        AsmUtils.addExtensionAnnotationDetails(negtestOrder, "entity", map2);

        Optional<EPackage> pack = asmUtils.all(EPackage.class).filter(p -> "entities".equals(p.getName())).findAny();
        pack.get().getEClassifiers().add(negtestOrder);

        Map<String, String> map3 = new HashMap<>();
        map3.put("value", "demo.entities.NegtestOrder");
        EClass negtestOrderInfo = newEClassBuilder().withEAnnotations(
                newEAnnotationBuilder().withSource(asmUtils.getAnnotationUri("mappedEntityType")).build()
        ).build();
        AsmUtils.addExtensionAnnotationDetails(negtestOrderInfo, "mappedEntityType", map3);

        assertThat(asmUtils.getMappedEntityType(negtestOrderInfo), equalTo(Optional.empty()));
    }

    @Test
    public void testGetMappedAttribute () {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");

        final EAttribute orderDate = (EAttribute) order.getEStructuralFeature("orderDate");
        final EAttribute orderInfoDate = (EAttribute) orderInfo.getEStructuralFeature("orderDate");

        Optional<EAttribute> mappedAttribute = asmUtils.getMappedAttribute(orderInfoDate);
        assertTrue(mappedAttribute.isPresent());
        assertThat(mappedAttribute.get(), equalTo(orderDate));

        //TODO: negtest: mappedAttributeName notPresent

        //TODO: negtest: mappedEntityType notPresent

        //TODO: negtest: mappedEntityType is not Attribute
    }


    @Test
    public void testGetMappedReference () {
        final EClass order = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Order");
        final EClass orderInfo = (EClass) resourceSet.getResources().get(0).getEObject("//service/OrderInfo");

        final EReference orderDetails = (EReference) order.getEStructuralFeature("orderDetails");
        final EReference orderInfoItems = (EReference) orderInfo.getEStructuralFeature("items");

        Optional<EReference> mappedReference = asmUtils.getMappedReference(orderInfoItems);
        assertTrue(mappedReference.isPresent());
        assertThat(mappedReference.get(), equalTo(orderDetails));

        //TODO: negtest: mappedReferenceName notPresent

        //TODO: negtest: mappedEntityType notPresent

        //TODO: negtest: mappedEntityType is not Reference
    }

    @Test
    public void testGetExposedGraphByFqName () {
        Optional<EAnnotation> graph = asmUtils.all(EAnnotation.class).filter(a -> asmUtils.getAnnotationUri("graph").equals(a.getSource())).findAny();
        assertThat(asmUtils.getExposedGraphByFqName("demo.internalAP/ordersAssignedToEmployee").get(), equalTo(graph.get()));

        //negtest: invalid name (for real)
        assertThat(asmUtils.getExposedGraphByFqName("something so inherently wrongful you are not even allowed to call it by name"), equalTo(Optional.empty()));

        //negtest: accesspoint not present
        assertThat(asmUtils.getExposedGraphByFqName("demo.NEGTEST/ordersAssignedToEmployee"), equalTo(Optional.empty()));
    }

    @Test
    public void testGetResolvedExposedGraph () {
        //negtest: not an exposed graph
        Optional<EAnnotation> notExposedGraph = asmUtils.all(EAnnotation.class).filter(a -> !(asmUtils.getAnnotationUri("exposedGraph").equals(a.getSource()))).findAny();
        assertTrue(notExposedGraph.isPresent());
        assertThat(asmUtils.getResolvedExposedGraph(notExposedGraph.get()), equalTo(Optional.empty()));

        //negtest: not containing "value" key
        EAnnotation notContainingValueKey = newEAnnotationBuilder()
                .withSource(asmUtils.getAnnotationUri("exposedGraph"))
                .build();
        notContainingValueKey.getDetails().put("notValue", "NEGTEST");
        assertThat(asmUtils.getResolvedExposedGraph(notContainingValueKey), equalTo(Optional.empty()));

        //negtest: exposedGraph cannot be resolved
        EAnnotation noGraphWithValue = newEAnnotationBuilder()
                .withSource(asmUtils.getAnnotationUri("exposedGraph"))
                .build();
        noGraphWithValue.getDetails().put("value", "NEGTEST");
        assertThat(asmUtils.getResolvedExposedGraph(noGraphWithValue), equalTo(Optional.empty()));

        //TODO: negtest: exposed graph is not a graph (it was so obvious all along, right?)
        /*
        EAnnotation notGraph = newEAnnotationBuilder()
                .withSource(asmUtils.getAnnotationUri("exposedGraph"))
                .build();
        notGraph.getDetails().put("value", "demo.entities.Order");//bajos
        assertThat(asmUtils.getResolvedExposedGraph(notGraph), equalTo(Optional.empty()));
         */
    }

    @Test
    public void testGetMappedTransferObjectGraph () {
        final Optional<EClass> orderInfoQuery = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery".equals(c.getName())).findAny();
        final Optional<EClass> orderItemQuery = asmUtils.all(EClass.class).filter(c -> "OrderItemQuery".equals(c.getName())).findAny();
        final Optional<EClass> productInfoQuery = asmUtils.all(EClass.class).filter(c -> "ProductInfoQuery".equals(c.getName())).findAny();
        final Optional<EClass> categoryInfo = asmUtils.all(EClass.class).filter(c -> "CategoryInfo".equals(c.getName())).findAny();
        final Optional<EClass> productInfo = asmUtils.all(EClass.class).filter(c -> "ProductInfo".equals(c.getName())).findAny();

        assertTrue(orderInfoQuery.isPresent());
        assertTrue(orderItemQuery.isPresent());
        assertTrue(productInfoQuery.isPresent());
        assertTrue(categoryInfo.isPresent());
        assertTrue(productInfo.isPresent());
        assertThat(asmUtils.getMappedTransferObjectGraph(orderInfoQuery.get()), hasItems(orderInfoQuery.get(), orderItemQuery.get(), productInfo.get(), productInfoQuery.get(), categoryInfo.get()));

        final Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        assertTrue(internalAP.isPresent());
        assertThat(asmUtils.getMappedTransferObjectGraph(internalAP.get()), hasItems());
    }

    @Test
    public void testGetAllStuffs () {
        assertThat(asmUtils.getAllMappedTransferObjectTypes().isEmpty(), equalTo(Boolean.FALSE));
        assertThat(asmUtils.getAllStatelessOperations().isEmpty(), equalTo(Boolean.FALSE));
        assertThat(asmUtils.getAllAccessPoints().isEmpty(), equalTo(Boolean.FALSE));
        assertThat(asmUtils.getAllExposedServices().isEmpty(), equalTo(Boolean.FALSE));
        assertThat(asmUtils.getAllGraphs().isEmpty(), equalTo(Boolean.FALSE));
    }

    @Test
    public void testGetMappedTransferObjectTypesOfAccessPoint () {
        final Optional<EClass> externalAP = asmUtils.all(EClass.class).filter(c -> "externalAP".equals(c.getName())).findAny();
        assertTrue(externalAP.isPresent());
        final Optional<EClass> productInfo = asmUtils.all(EClass.class).filter(c -> "ProductInfo".equals(c.getName())).findAny();
        final Optional<EClass> categoryInfo = asmUtils.all(EClass.class).filter(c -> "CategoryInfo".equals(c.getName())).findAny();
        final Optional<EClass> prodInfoCatRef = asmUtils.all(EClass.class).filter(c -> "ProductInfoʘcategoryʘReference".equals(c.getName())).findAny();
        final Optional<EClass> catInfoProdRef = asmUtils.all(EClass.class).filter(c -> "CategoryInfoʘproductsʘReference".equals(c.getName())).findAny();

        assertThat(asmUtils.getMappedTransferObjectTypesOfAccessPoint(externalAP.get()), hasItems(productInfo.get(), categoryInfo.get(), prodInfoCatRef.get(), catInfoProdRef.get()));


        //negtest: not an access point
        final Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        assertThat(asmUtils.getMappedTransferObjectTypesOfAccessPoint(order.get()), equalTo(ECollections.emptyEList()));
    }

    @Test
    public void testGetMappedTransferObjectTypesOfGraph () {

        //TODO: postest: getResolvedExposedGraph needed
        //Optional<EAnnotation> exposedGraph = asmUtils.all(EAnnotation.class).filter(a -> asmUtils.getAnnotationUri("exposedGraph").equals(a.getSource())).findAny();
        //assertThat(exposedGraph.isPresent(), equalTo(Boolean.TRUE));
        //assertThat(asmUtils.getMappedTransferObjectTypesOfGraph(exposedGraph.get()), equalTo(ECollections.emptyEList()));


        //negtest: not a graph
        Optional<EAnnotation> externalAP = asmUtils.all(EAnnotation.class).filter(a -> asmUtils.getAnnotationUri("accessPoint").equals(a.getSource())).findAny();
        assertThat(externalAP.isPresent(), equalTo(Boolean.TRUE));
        assertThat(asmUtils.getMappedTransferObjectTypesOfGraph(externalAP.get()), equalTo(ECollections.emptyEList()));
    }

    //TODO?
    //@Test
    public void testAddExposedByAnnotationToTransferObjectType () {
    }
}


