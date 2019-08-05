package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.asmLoadArgumentsBuilder;
import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.loadAsmModel;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAnnotationBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnnotationTest {

    AsmModel asmModel;
    AsmUtils asmUtils;

    @BeforeEach
    public void setUp() throws Exception {
        asmModel = loadAsmModel(asmLoadArgumentsBuilder()
                .uri(URI.createFileURI(new File("src/test/model/asm.model").getAbsolutePath()))
                .name("test"));
        asmUtils = new AsmUtils(asmModel.getResourceSet());
    }

    @Test
    public void testGetExtensionAnnotationExisting() {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();

        final Optional<EAnnotation> existing = asmUtils.getExtensionAnnotationByName(order.get(), "entity", false);

        assertTrue(existing.isPresent());
        assertTrue(Boolean.valueOf(existing.get().getDetails().get(AsmUtils.EXTENDED_METADATA_DETAILS_VALUE_KEY)));
    }

    //TODO
    @Test
    public void testGetExtensionAnnotationNotExistingButCreated() {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EAnnotation> notExistingButCreated = asmUtils.getExtensionAnnotationByName(order.get(), "toBeCreated", true);

        assertTrue(notExistingButCreated.isPresent());
        //TODO: get what this is
        //assertTrue(Boolean.valueOf(notExistingButCreated.get().getDetails().get(AsmUtils.EXTENDED_METADATA_DETAILS_VALUE_KEY)));
    }

    @Test
    public void testGetExtensionAnnotationNotExisting() {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EAnnotation> notExistingNotCreated = asmUtils.getExtensionAnnotationByName(order.get(), "notToBeCreated", false);

        assertFalse(notExistingNotCreated.isPresent());
    }

    @Test
    public void testGetExtensionAnnotationListByName() {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        EList<EAnnotation> annotationList = asmUtils.getExtensionAnnotationListByName(order.get(), "entity");

        assertFalse(annotationList.isEmpty());
        assertTrue(annotationList.contains(asmUtils.getExtensionAnnotationByName(order.get(), "entity", false).get()));
    }

    @Test
    public void testAddExtensionAnnotation() {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        //annotation not already present
        assertTrue(asmUtils.addExtensionAnnotation(order.get(), "NewAnnotation", "value"));
        assertTrue(asmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).isPresent());
        //negtest: annotation already present
        assertFalse(asmUtils.addExtensionAnnotation(order.get(), "NewAnnotation", "value"));
    }

    @Test
    public void testAddExtensionAnnotationDetails() {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());

        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        asmUtils.addExtensionAnnotationDetails(order.get(), "NewAnnotation", map);

        assertTrue(asmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).get().getDetails().containsKey("key1"));
        assertTrue(asmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).get().getDetails().containsKey("key2"));
    }

    @Test
    public void testGetExtensionAnnotationValue() {
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        Optional<String> value = asmUtils.getExtensionAnnotationValue(orderInfo.get(), "mappedEntityType", false);

        assertTrue(value.isPresent());
        assertThat(value.get(), equalTo("demo.entities.Order"));
    }

    //TODO: ask
    //@Test
    public void testGetExtensionAnnotationCustomValue() {
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();

    }

    @Test
    public void testPackageFQName() {
        final EPackage ePackage = (EPackage) asmModel.getResource().getEObject("//service");

        assertThat(asmUtils.getPackageFQName(ePackage), equalTo("demo.service"));
    }


    @Test
    public void testClassFQName() {
        final EClass orderInfo = (EClass) asmModel.getResource().getEObject("//service/OrderInfo");

        assertThat(asmUtils.getClassifierFQName(orderInfo), equalTo("demo.service.OrderInfo"));
    }


    @Test
    public void testAttributeFQName() {
        final EClass orderInfo = (EClass) asmModel.getResource().getEObject("//service/OrderInfo");
        final EAttribute orderDate = (EAttribute) orderInfo.getEStructuralFeature("orderDate");

        assertThat(asmUtils.getAttributeFQName(orderDate), equalTo("demo.service.OrderInfo#orderDate"));
    }


    @Test
    public void testGetClassByFQName() {
        final EClass orderInfo = (EClass) asmModel.getResource().getEObject("//service/OrderInfo");
        Optional<EClass> founded = asmUtils.getClassByFQName("demo.service.OrderInfo");

        assertTrue(founded.isPresent());
        assertThat(founded.get(), equalTo(orderInfo));
    }

    //--------------------------- BIG FAT LINE OF PROGRESSSSSION -----------------------

    @Test
    public void testGetResolvedExposedBy() {
        Optional<EAnnotation> exposedBy = asmUtils.all(EAnnotation.class).filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy".equals(a.getSource())).findAny();
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "internalAP".equals(a.getName())).findAny();

        assertTrue(exposedBy.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(exposedBy.get()), is(internalAP));

        //negtest: not an exposedBy
        Optional<EAnnotation> entity = asmUtils.all(EAnnotation.class).filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/entity".equals(a.getSource())).findAny();
        assertTrue(entity.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(entity.get()), is(Optional.empty()));

        //negtest: key not value
        EAnnotation notValueKey = newEAnnotationBuilder()
                .withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy")
                .build();
        notValueKey.getDetails().put("notValue", "true");
        assertThat(asmUtils.getResolvedExposedBy(notValueKey), is(Optional.empty()));

        //negtest: not an EClass
        Optional<EAnnotation> accessPoint = asmUtils.all(EAnnotation.class).filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/accessPoint".equals(a.getSource())).findAny();
        assertTrue(accessPoint.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(accessPoint.get()), is(Optional.empty()));

        //negtest: not an accessPoint
        Optional<EAnnotation> binding = asmUtils.all(EAnnotation.class).filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/binding".equals(a.getSource())).findAny();
        assertTrue(binding.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(binding.get()), is(Optional.empty()));

    }

    @Test
    public void testGetAccessPointsOfUnboundOperation() {
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
        assertThat(asmUtils.getAccessPointsOfUnboundOperation(getOper.get()), CoreMatchers.is(ECollections.emptyEList()));


    }

    @Test
    public void testGetExposedServicesOfAccessPoint() {
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
        assertThat(asmUtils.getExposedServicesOfAccessPoint(orderInfo.get()), is(ECollections.emptyEList()));
    }

    @Test
    public void testGetResolvedRoot() {
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(c -> "internalAP".equals(c.getName())).findAny();
        Optional<EClass> productInfo = asmUtils.all(EClass.class).filter(a -> "OrderInfoQuery".equals(a.getName())).findAny();
        Optional<EAnnotation> graph = internalAP.get().getEAnnotations().stream().filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/graph".equals(a.getSource())).findAny();
        assertTrue(graph.isPresent());
        assertThat(asmUtils.getResolvedRoot(graph.get()), is(productInfo));

        //negtest: no root = no fun
        Optional<EAttribute> entity = asmUtils.all(EAttribute.class).filter(a -> "totalNumberOfOrders".equals(a.getName())).findAny();
        Optional<EAnnotation> expression = entity.get().getEAnnotations().stream().filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/expression".equals(a.getSource())).findAny();
        //asmUtils.all(EAnnotation.class).filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/expression".equals(a.getSource())).findAny();
        assertTrue(expression.isPresent());
        assertThat(asmUtils.getResolvedRoot(expression.get()), is(Optional.empty()));

        //TODO negtest: yeproot, notmapped riperoni

    }

    //@Test
    public void testGetExposedGraphByFqName() {
        assertThat(asmUtils.getExposedGraphByFqName("something inherently wrongful"), is(Optional.empty()));

        //Optional<EClass> ap =
        assertThat(asmUtils.getExposedGraphByFqName("demo.internalAP/ordersAssignedToEmployee"), is(Optional.empty()));
    }

    @Test
    public void testGetMappedEntity() {
        final EClass order = (EClass) asmModel.getResource().getEObject("//entities/Order");
        final EClass orderInfo = (EClass) asmModel.getResource().getEObject("//service/OrderInfo");

        Optional<EClass> mappedType = asmUtils.getMappedEntityType(orderInfo);
        assertTrue(mappedType.isPresent());
        assertThat(mappedType.get(), equalTo(order));
    }

    @Test
    public void testGetMappedAttribute() {
        final EClass order = (EClass) asmModel.getResource().getEObject("//entities/Order");
        final EClass orderInfo = (EClass) asmModel.getResource().getEObject("//service/OrderInfo");

        final EAttribute orderDate = (EAttribute) order.getEStructuralFeature("orderDate");
        final EAttribute orderInfoDate = (EAttribute) orderInfo.getEStructuralFeature("orderDate");

        Optional<EAttribute> mappedAttribute = asmUtils.getMappedAttribute(orderInfoDate);
        assertTrue(mappedAttribute.isPresent());
        assertThat(mappedAttribute.get(), equalTo(orderDate));
    }


    @Test
    public void testGetMappedReference() {
        final EClass order = (EClass) asmModel.getResource().getEObject("//entities/Order");
        final EClass orderInfo = (EClass) asmModel.getResource().getEObject("//service/OrderInfo");

        final EReference orderDetails = (EReference) order.getEStructuralFeature("orderDetails");
        final EReference orderInfoItems = (EReference) orderInfo.getEStructuralFeature("items");

        Optional<EReference> mappedReference = asmUtils.getMappedReference(orderInfoItems);
        assertTrue(mappedReference.isPresent());
        assertThat(mappedReference.get(), equalTo(orderDetails));
    }

}
