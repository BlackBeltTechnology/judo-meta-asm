package hu.blackbelt.judo.meta.asm.runtime;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.buildAsmModel;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAttributeBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEDataTypeBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEEnumBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEEnumLiteralBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEOperationBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEPackageBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEReferenceBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.useEPackage;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.useEReference;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.exceptions.EvlScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.Slf4jLog;

class ExecutionContextOnAsmTest {
	Log log = new Slf4jLog();
    AsmModel asmModel;
	AsmUtils asmUtils;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionContextOnAsmTest.class);
    
    void setUp() throws Exception {
    	asmModel = buildAsmModel()
    			.uri(URI.createURI("urn:asm.judo-meta-asm"))
                .name("demo")
                .build();
    	asmUtils = new AsmUtils(asmModel.getResourceSet());
    	populateAsmModel();
        
  		log.info(asmModel.getDiagnosticsAsString());
    	assertTrue(asmModel.isValid());
    	
    	runEpsilon();
    }
    
    private void runEpsilon() throws Exception {
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
    
    private void populateAsmModel() {
    	//enum
    	EEnum countriesEnum = newEEnumBuilder().withName("Countries").withELiterals(newEEnumLiteralBuilder().withLiteral("HU").withName("HU").withValue(0).build(),
    			newEEnumLiteralBuilder().withLiteral("AT").withName("AT").withValue(1).build(),
    			newEEnumLiteralBuilder().withLiteral("RO").withName("RO").withValue(2).build(),
    			newEEnumLiteralBuilder().withLiteral("SK").withName("SK").withValue(3).build()).build();
    	
    	//types
    	EDataType timestamp = newEDataTypeBuilder().withName("Timestamp").withInstanceClassName("java.time.OffsetDateTime").build();
    	EDataType stringType = newEDataTypeBuilder().withName("String").withInstanceClassName("java.lang.String").build();
    	EDataType doubleType = newEDataTypeBuilder().withName("Double").withInstanceClassName("java.lang.Double").build();
    	EDataType integerType = newEDataTypeBuilder().withName("Integer").withInstanceClassName("java.lang.Integer").build();
    	EDataType binary = newEDataTypeBuilder().withName("Binary").withInstanceClassName("java.lang.Object").build();
    	EDataType timeStoredInMonths = newEDataTypeBuilder().withName("TimeStoredInMonths").withInstanceClassName("java.lang.Integer").build();
    	EDataType timeStoredInSeconds = newEDataTypeBuilder().withName("TimeStoredInSeconds").withInstanceClassName("java.lang.Double").build();
    	EDataType dateType = newEDataTypeBuilder().withName("Date").withInstanceClassName("java.time.LocalDate").build();
    	EDataType phoneType = newEDataTypeBuilder().withName("Phone").withInstanceClassName("java.lang.String").build();
    	EDataType booleanType = newEDataTypeBuilder().withName("Boolean").withInstanceClassName("java.lang.Boolean").build();
    	EDataType massStoredInKilograms = newEDataTypeBuilder().withName("MassStoredInKilograms").withInstanceClassName("java.lang.Double").build();
    	
    	//attributes
    	EAttribute orderDate = newEAttributeBuilder().withName("orderDate").withEType(timestamp).build();
    	EAttribute orderDateAnnotated = newEAttributeBuilder().withName("orderDate").withEType(timestamp).build();
    	EAttribute companyName = newEAttributeBuilder().withName("companyName").withEType(stringType).build();
    	EAttribute shipperName = newEAttributeBuilder().withName("shipperName").withEType(stringType).build();
    	EAttribute exciseTax = newEAttributeBuilder().withName("exciseTax").withEType(doubleType).build();
    	EAttribute customsDescription = newEAttributeBuilder().withName("customsDescription").withEType(stringType).build();
    	EAttribute productName = newEAttributeBuilder().withName("productName").withEType(stringType).build();
    	EAttribute unitPrice = newEAttributeBuilder().withName("unitPrice").withEType(doubleType).build();
    	EAttribute categoryName = newEAttributeBuilder().withName("categoryName").withEType(stringType).build();
    	EAttribute unitPriceOrderDetail = newEAttributeBuilder().withName("unitPrice").withEType(doubleType).build();
    	EAttribute quantity = newEAttributeBuilder().withName("quantity").withEType(integerType).build();
    	EAttribute discount = newEAttributeBuilder().withName("discount").withEType(doubleType).build();
    	EAttribute country = newEAttributeBuilder().withName("country").withEType(countriesEnum).build();
    	EAttribute picture = newEAttributeBuilder().withName("picture").withEType(binary).build();
    	EAttribute quantityPerUnit = newEAttributeBuilder().withName("quantityPerUnit").withEType(integerType).build();
    	EAttribute firstName = newEAttributeBuilder().withName("firstName").withEType(stringType).build();
    	EAttribute firstNameEmployee = newEAttributeBuilder().withName("firstName").withEType(stringType).build();
    	EAttribute lastNameEmployee = newEAttributeBuilder().withName("lastName").withEType(stringType).build();
    	EAttribute phone = newEAttributeBuilder().withName("phone").withEType(phoneType).build();
    	EAttribute discounted = newEAttributeBuilder().withName("discounted").withEType(booleanType).build();
    	EAttribute weight = newEAttributeBuilder().withName("weight").withEType(massStoredInKilograms).build();
    	EAttribute freight = newEAttributeBuilder().withName("freight").withEType(doubleType).build();
    	EAttribute price = newEAttributeBuilder().withName("price").withEType(doubleType).build();
    	EAttribute postalCode = newEAttributeBuilder().withName("postalCode").withEType(phoneType).build();
    	EAttribute shipperNameMapped = newEAttributeBuilder().withName("shipperName").withEType(stringType).build();
    	EAttribute totalNumberOfOrders = newEAttributeBuilder().withName("totalNumberOfOrders").withEType(integerType).build();
    	
    	EOperation getAllOrders = newEOperationBuilder().withName("getAllOrders").build();
    	
    	//relations
    	EReference orderDetails = newEReferenceBuilder().withName("orderDetails").withContainment(true).withLowerBound(0).withUpperBound(-1).build();
    	EReference productRef = newEReferenceBuilder().withName("product").withLowerBound(1).withUpperBound(1).build();
    	EReference categoryRef = newEReferenceBuilder().withName("category").withLowerBound(1).withUpperBound(1).build();
    	EReference productsRef = newEReferenceBuilder().withName("products").withLowerBound(0).withUpperBound(-1).build();
    	EReference categories = newEReferenceBuilder().withName("categories").withLowerBound(0).withUpperBound(-1).build();
    	EReference ordersRef = newEReferenceBuilder().withName("orders").withLowerBound(0).withUpperBound(-1).build();
    	EReference employeeRef = newEReferenceBuilder().withName("employee").withLowerBound(0).withUpperBound(1).build();
    	EReference shipperOrdersRef = newEReferenceBuilder().withName("shipperOrders").withLowerBound(0).withUpperBound(-1).build();
    	EReference shipperRef = newEReferenceBuilder().withName("shipper").withLowerBound(0).withUpperBound(1).build();
    	EReference ordersCustomer = newEReferenceBuilder().withName("orders").withLowerBound(0).withUpperBound(-1).build();
    	EReference addressesCustomer = newEReferenceBuilder().withName("addresses").withLowerBound(0).withUpperBound(-1)
    			.withContainment(true).build();
    	EReference customerOrder = newEReferenceBuilder().withName("customer").withLowerBound(0).withUpperBound(1).build();
    	EReference owner = newEReferenceBuilder().withName("owner").withLowerBound(0).withUpperBound(1).build();
    	EReference categoryEmployee = newEReferenceBuilder().withName("category").withLowerBound(0).withUpperBound(-1).build();
    	EReference territoryRef = newEReferenceBuilder().withName("territory").withLowerBound(0).withUpperBound(1).build();
    	EReference shipperTerritory = newEReferenceBuilder().withName("shipper").withLowerBound(0).withUpperBound(1).build();
    	EReference shipAddress = newEReferenceBuilder().withName("shipAddress").withLowerBound(0).withUpperBound(1).build();
    	EReference items = newEReferenceBuilder().withName("items").build();
    	EReference ordersAssignedToEmployee = newEReferenceBuilder().withName("ordersAssignedToEmployee").build();
    	
    	//classes - entities
    	EClass order = newEClassBuilder().withName("Order")
    			.withEStructuralFeatures(orderDate,orderDetails,categories,employeeRef,shipperRef,customerOrder,shipAddress,freight,shipperName).build();
    	EClass orderDetail = newEClassBuilder().withName("OrderDetail").withEStructuralFeatures(productRef,unitPriceOrderDetail,quantity,discount,price).build();
    	EClass product = newEClassBuilder().withName("Product").withEStructuralFeatures(categoryRef,productName,unitPrice,quantityPerUnit,discounted,weight).build();
    	EClass category = newEClassBuilder().withName("Category").withEStructuralFeatures(productsRef,categoryName,picture,owner).build();
    	EClass employee = newEClassBuilder().withName("Employee").withEStructuralFeatures(ordersRef,categoryEmployee,firstNameEmployee,lastNameEmployee).build();
    	EClass internationalOrder = newEClassBuilder().withName("InternationalOrder").withEStructuralFeatures(exciseTax,customsDescription)
    			.withESuperTypes(order).build();
    	EClass customer = newEClassBuilder().withName("Customer").withEStructuralFeatures(ordersCustomer,addressesCustomer).build();
    	EClass address = newEClassBuilder().withName("Address").withEStructuralFeatures(postalCode).build();
    	EClass internationalAddress = newEClassBuilder().withName("InternationalAddress")
    			.withESuperTypes(address).withEStructuralFeatures(country).build();
    	EClass company = newEClassBuilder().withName("Company").withESuperTypes(customer).build();
    	EClass shipper = newEClassBuilder().withName("Shipper").withEStructuralFeatures(companyName,shipperOrdersRef,phone,territoryRef)
    			.withESuperTypes(company).build();
    	EClass onlineOrder = newEClassBuilder().withName("OnlineOrder")
    			.withESuperTypes(order).build();
    	EClass individual = newEClassBuilder().withName("Individual").withEStructuralFeatures(firstName)
    			.withESuperTypes(customer).build();
    	EClass supplier = newEClassBuilder().withName("Supplier")
    			.withESuperTypes(company).build();
    	EClass territory = newEClassBuilder().withName("Territory").withEStructuralFeatures(shipperTerritory).build();
    	EClass internalAP = newEClassBuilder().withName("InternalAP").withEStructuralFeatures(ordersAssignedToEmployee).build();
    	
    	//classes - transfer object types
    	EClass orderInfo = newEClassBuilder().withName("OrderInfo").withEStructuralFeatures(orderDateAnnotated,items).build();
    	EClass orderItem = newEClassBuilder().withName("OrderItem").build();
    	EClass productInfo = newEClassBuilder().withName("ProductInfo").build();
    	EClass internationalOrderInfo = newEClassBuilder().withName("InternationalOrderInfo").withEStructuralFeatures(shipperNameMapped).build();
    	
    	EClass __static = newEClassBuilder().withName("__Static").withEStructuralFeatures(totalNumberOfOrders).build();
    	EClass unboundServices = newEClassBuilder().withName("__UnboundServices").withEOperations(getAllOrders).build();
    	
    	//relations again
    	useEReference(orderDetails).withEType(orderDetail).build();
    	useEReference(productRef).withEType(product).build();
    	useEReference(categoryRef).withEType(category).withEOpposite(productsRef).build();
    	useEReference(productsRef).withEType(product).withEOpposite(categoryRef).build();
    	useEReference(categories).withEType(category).build();
    	useEReference(ordersRef).withEType(order).withEOpposite(employeeRef).build();
    	useEReference(employeeRef).withEType(employee).withEOpposite(ordersRef).build();
    	useEReference(shipperOrdersRef).withEType(order).withEOpposite(shipperRef).build();
    	useEReference(shipperRef).withEType(shipper).withEOpposite(shipperOrdersRef).build();
    	useEReference(addressesCustomer).withEType(address).build();
    	useEReference(ordersCustomer).withEType(order).withEOpposite(customerOrder).build();
    	useEReference(customerOrder).withEType(customer).withEOpposite(ordersCustomer).build();
    	useEReference(owner).withEType(employee).withEOpposite(categoryEmployee).build();
    	useEReference(categoryEmployee).withEType(category).withEOpposite(owner).build();
    	useEReference(shipperTerritory).withEType(shipper).withEOpposite(territoryRef).build();
    	useEReference(territoryRef).withEType(territory).withEOpposite(shipperTerritory).build();
    	useEReference(shipAddress).withEType(address).build();
    	useEReference(items).withEType(orderItem).build();
    	useEReference(ordersAssignedToEmployee).withEType(order).build();
    	
    	//packages
    	EPackage demo = newEPackageBuilder().withName("demo").withNsURI("http://blackbelt.hu/judo/northwind/northwind/demo")
    			.withNsPrefix("runtimenorthwindNorthwindDemo").build();
    	EPackage services = newEPackageBuilder().withName("services").withNsURI("http://blackbelt.hu/judo/northwind/northwind/services")
    			.withNsPrefix("runtimenorthwindNorthwindServices")
    			.withEClassifiers(orderInfo,orderItem,internationalOrderInfo,__static,unboundServices,internalAP,productInfo).build();
    	EPackage entities = newEPackageBuilder().withName("entities")
    			.withEClassifiers(order,
						orderDetail,product,category,employee,
						shipper,internationalOrder,customer,address,
						internationalAddress,company,onlineOrder,individual,supplier,territory)
    			.withNsURI("http://blackbelt.hu/judo/northwind/northwind/entities")
    			.withNsPrefix("runtimenorthwindNorthwindEntities").build();
    	EPackage types = newEPackageBuilder().withName("types")
    			.withEClassifiers(timestamp,stringType,doubleType,integerType,binary,dateType,countriesEnum,phoneType,booleanType)
    			.withNsURI("http://blackbelt.hu/judo/northwind/northwind/types")
    			.withNsPrefix("runtimenorthwindNorthwindTypes").build();
    	EPackage measured = newEPackageBuilder().withName("measured").withEClassifiers(timeStoredInMonths,timeStoredInSeconds,massStoredInKilograms)
    			.withNsURI("http://blackbelt.hu/judo/northwind/demo/types/measured")
    			.withNsPrefix("runtimenorthwindDemoTypesMeasured").build();
    	EPackage measures = newEPackageBuilder().withName("measures")
    			.withNsURI("http://blackbelt.hu/judo/northwind/demo/measures")
    			.withNsPrefix("runtimenorthwindDemoMeasures").build();
    	
    	//packages again
    	useEPackage(demo).withESubpackages(services,entities,types,measures).build();
    	useEPackage(types).withESubpackages(measured).build();
    	
    	asmModel.addContent(demo);
        
        //annotations
        EAnnotation orderAnnotation = asmUtils.getExtensionAnnotationByName(order, "entity", true).get();
        orderAnnotation.getDetails().put("value", "true");
    	EAnnotation orderDetailAnnotation = asmUtils.getExtensionAnnotationByName(orderDetail, "entity", true).get();
    	orderDetailAnnotation.getDetails().put("value", "true");
    	EAnnotation productAnnotation = asmUtils.getExtensionAnnotationByName(product, "entity", true).get();
    	productAnnotation.getDetails().put("value", "true");
    	EAnnotation categoryAnnotation = asmUtils.getExtensionAnnotationByName(category, "entity", true).get();
    	categoryAnnotation.getDetails().put("value", "true");
    	EAnnotation employeeAnnotation = asmUtils.getExtensionAnnotationByName(employee, "entity", true).get();
    	employeeAnnotation.getDetails().put("value", "true");
    	EAnnotation shipperAnnotation = asmUtils.getExtensionAnnotationByName(shipper, "entity", true).get();
    	shipperAnnotation.getDetails().put("value", "true");
    	EAnnotation intOrderAnnotation = asmUtils.getExtensionAnnotationByName(internationalOrder, "entity", true).get();
    	intOrderAnnotation.getDetails().put("value", "true");
    	EAnnotation addressAnnotation = asmUtils.getExtensionAnnotationByName(address, "entity", true).get();
    	addressAnnotation.getDetails().put("value", "true");
    	EAnnotation customerAnnotation = asmUtils.getExtensionAnnotationByName(customer, "entity", true).get();
    	customerAnnotation.getDetails().put("value", "true");
    	EAnnotation intAddrAnnotation = asmUtils.getExtensionAnnotationByName(internationalAddress, "entity", true).get();
    	intAddrAnnotation.getDetails().put("value", "true");
    	EAnnotation companyAnnotation = asmUtils.getExtensionAnnotationByName(company, "entity", true).get();
    	companyAnnotation.getDetails().put("value", "true");
    	EAnnotation onlineOrderAnnotation = asmUtils.getExtensionAnnotationByName(onlineOrder, "entity", true).get();
    	onlineOrderAnnotation.getDetails().put("value", "true");
    	EAnnotation individaulAnnotation = asmUtils.getExtensionAnnotationByName(individual, "entity", true).get();
    	individaulAnnotation.getDetails().put("value", "true");
    	EAnnotation supplierAnnotation = asmUtils.getExtensionAnnotationByName(supplier, "entity", true).get();
    	supplierAnnotation.getDetails().put("value", "true");
    	EAnnotation territoryAnnotation = asmUtils.getExtensionAnnotationByName(territory, "entity", true).get();
    	territoryAnnotation.getDetails().put("value", "true");
    	EAnnotation weightAnnotation = asmUtils.getExtensionAnnotationByName(weight, "constraints", true).get();
    	weightAnnotation.getDetails().put("precision", "15");
    	weightAnnotation.getDetails().put("scale", "4");
    	weightAnnotation.getDetails().put("measure", "demo.measures.Mass");
    	weightAnnotation.getDetails().put("unit", "kilogram");
    	EAnnotation attributeAnnotation = asmUtils.getExtensionAnnotationByName(orderDateAnnotated, "binding", true).get();
    	attributeAnnotation.getDetails().put("value", orderDate.getName());
    	EAnnotation shipperNameAnnotation = asmUtils.getExtensionAnnotationByName(shipperNameMapped, "binding", true).get();
    	shipperNameAnnotation.getDetails().put("value", shipperName.getName());
    	EAnnotation itemsAnnotation = asmUtils.getExtensionAnnotationByName(items, "binding", true).get();
    	itemsAnnotation.getDetails().put("value", orderDetails.getName());
    	EAnnotation shipperNameConstraintAnnotation = asmUtils.getExtensionAnnotationByName(shipperName, "constraints", true).get();
    	shipperNameConstraintAnnotation.getDetails().put("maxLength", "255");
    	EAnnotation shipperNameMappedConstraintAnnotation = asmUtils.getExtensionAnnotationByName(shipperNameMapped, "constraints", true).get();
    	shipperNameMappedConstraintAnnotation.getDetails().put("maxLength", "255");
    	EAnnotation annotationOrderInfo = asmUtils.getExtensionAnnotationByName(orderInfo, "mappedEntityType", true).get();
    	annotationOrderInfo.getDetails().put("value", asmUtils.getClassifierFQName(order));
    	EAnnotation annotationOrderItem = asmUtils.getExtensionAnnotationByName(orderItem, "mappedEntityType", true).get();
    	annotationOrderItem.getDetails().put("value", asmUtils.getClassifierFQName(orderDetail));
    	EAnnotation annotationInternationalOrderInfo = asmUtils.getExtensionAnnotationByName(internationalOrderInfo, "mappedEntityType", true).get();
    	annotationInternationalOrderInfo.getDetails().put("value", asmUtils.getClassifierFQName(internationalOrder));
    	EAnnotation annotationProductInfo = asmUtils.getExtensionAnnotationByName(productInfo, "mappedEntityType", true).get();
    	annotationProductInfo.getDetails().put("value", asmUtils.getClassifierFQName(product));
    	EAnnotation apAnnotation = asmUtils.getExtensionAnnotationByName(internalAP, "accessPoint", true).get();
    	apAnnotation.getDetails().put("value", "true");
    	EAnnotation operationAnnotation = asmUtils.getExtensionAnnotationByName(getAllOrders, "exposedBy", true).get();
    	operationAnnotation.getDetails().put("value", asmUtils.getClassifierFQName(internalAP));
    }
}
