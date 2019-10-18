package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.asmLoadArgumentsBuilder;
import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.loadAsmModel;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsmUtilsTest {

    AsmUtils asmUtils;
    AsmModel asmModel;

    @BeforeEach
    public void setUp () throws Exception {
        asmModel = loadAsmModel(asmLoadArgumentsBuilder()
                .uri(URI.createFileURI(new File("target/test-classes/model/northwind-asm.model").getAbsolutePath()))
                .name("test"));
        asmUtils = new AsmUtils(asmModel.getResourceSet());
    }

    @Test
    public void testGetPackageFQName () {
        Optional<EPackage> ePackage = asmUtils.all(EPackage.class).filter(pkg -> "services".equals(pkg.getName())).findAny();

        assertTrue(ePackage.isPresent());
        assertThat(asmUtils.getPackageFQName(ePackage.get()), is("demo.services"));
    }

    @Test
    public void testGetClassifierFQName () {
        Optional<EClassifier> eClassifier = asmUtils.all(EClassifier.class).filter(clsf -> "Countries".equals(clsf.getName())).findAny();

        assertTrue(eClassifier.isPresent());
        assertThat(asmUtils.getClassifierFQName(eClassifier.get()), is("demo.types.Countries"));
    }

    @Test
    public void testGetAttributeFQName () {
        Optional<EAttribute> eAttribute = asmUtils.all(EAttribute.class).filter(attr -> "totalNumberOfOrders".equals(attr.getName())).findAny();

        assertTrue(eAttribute.isPresent());
        assertThat(asmUtils.getAttributeFQName(eAttribute.get()), is("demo.services.__Static#totalNumberOfOrders"));
    }

    @Test
    public void testGetReferenceFQName () {
        Optional<EReference> eReference = asmUtils.all(EReference.class).filter(ref -> "owner".equals(ref.getName())).findAny();

        assertTrue(eReference.isPresent());
        assertThat(asmUtils.getReferenceFQName(eReference.get()), is("demo.entities.Category#owner"));
    }

    @Test
    public void testGetOperationFQName () {
        Optional<EOperation> eOperation = asmUtils.all(EOperation.class).filter(op -> "getAllOrders".equals(op.getName())).findAny();

        assertTrue(eOperation.isPresent());
        assertThat(asmUtils.getOperationFQName(eOperation.get()), is("demo.services.__UnboundServices#getAllOrders"));
    }

    @Test
    public void testResolve () {
        Optional<EClassifier> countries = asmUtils.all(EClassifier.class).filter(classifier -> "Countries".equals(classifier.getName())).findAny();
        assertThat(asmUtils.resolve("Countries"), is(countries));

        assertThat(asmUtils.resolve("demo.types.Countries"), is(countries));

        assertFalse(asmUtils.resolve("MissingClass").isPresent());
    }

    @Test
    public void testGetClassByFQName () {
        Optional<EClassifier> productInfo = asmUtils.all(EClassifier.class).filter(classifier -> "ProductInfo".equals(classifier.getName())).findAny();
        assertTrue(productInfo.isPresent());
        assertThat(asmUtils.getClassByFQName("ProductInfo").get(), is(productInfo.get()));

        assertFalse(asmUtils.getClassByFQName("MissingEClass").isPresent());
    }

    @Test
    public void testGetNestedClasses () {
        Optional<EClass> nestedClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery__items".equals(c.getName())).findAny();
        assertTrue(nestedClass.isPresent());
        Optional<EClass> containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery".equals(c.getName())).findAny();
        assertTrue(containerClass.isPresent());

        assertThat(asmUtils.getNestedClasses(containerClass.get()), hasItem(nestedClass.get()));

        Optional<EClass> classWithoutNestedClass = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(classWithoutNestedClass.isPresent());

        assertTrue(asmUtils.getNestedClasses(classWithoutNestedClass.get()).isEmpty());
    }

    @Test
    public void testGetContainerClass () {
        Optional<EClass> containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery__items".equals(c.getName())).findAny();
        assertTrue(containerClass.isPresent());
        Optional<EClass> nestedClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery__items__Reference".equals(c.getName())).findAny();
        assertTrue(nestedClass.isPresent());

        assertThat(asmUtils.getContainerClass(nestedClass.get()).get(), is(containerClass.get()));

        Optional<EClass> classWithoutContainer = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery".equals(c.getName())).findAny();
        assertTrue(classWithoutContainer.isPresent());

        assertFalse(asmUtils.getContainerClass(classWithoutContainer.get()).isPresent());
    }
    
    @Test
    public void testEnrichMethod()
    {
    	Optional<EClass> orderClass = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
    	assertTrue(orderClass.isPresent());
    	
    	asmUtils.createMappedTransferObjectTypeByEntityType(orderClass.get());
    	
    	assertTrue(asmUtils.getExtensionAnnotationByName(orderClass.get(), "mappedEntityType", false).isPresent());
    	
    	for(EStructuralFeature eStructuralFeature : orderClass.get().getEAllStructuralFeatures())
    	{
    		assertTrue(asmUtils.getExtensionAnnotationByName(eStructuralFeature, "binding", false).isPresent());
    	}
    }
    
    @Test
    public void testEnrichMethodAlreadyPresent()
    {
    	Optional<EClass> orderClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoQuery".equals(c.getName())).findAny();
    	assertTrue(orderClass.isPresent());
    	
    	asmUtils.createMappedTransferObjectTypeByEntityType(orderClass.get());
    	
    	assertTrue(asmUtils.getExtensionAnnotationByName(orderClass.get(), "mappedEntityType", false).isPresent());
    	
    	for(EStructuralFeature eStructuralFeature : orderClass.get().getEAllStructuralFeatures())
    	{
    		assertTrue(asmUtils.getExtensionAnnotationByName(eStructuralFeature, "binding", false).isPresent());
    	}
    }
}
