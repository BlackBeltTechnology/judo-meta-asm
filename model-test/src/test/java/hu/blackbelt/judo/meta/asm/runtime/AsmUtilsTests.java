package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.loadArgumentsBuilder;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AsmUtilsTests {

    static Logger log = LoggerFactory.getLogger(AsmUtilsTests.class);

    ResourceSet resourceSet;
    AsmUtils asmUtils;
    AsmModel asmModel;

    @BeforeEach
    public void setUp() throws Exception {
        asmModel = AsmModel.loadAsmModel(loadArgumentsBuilder()
                .uri(URI.createFileURI(new File("src/test/model/asm.model").getAbsolutePath()))
                .name("test")
                .build());

        asmUtils = new AsmUtils(asmModel.getResourceSet());
    }

    @AfterEach
    public void tearDown() {
        resourceSet = null;
    }
    
    @Test
    public void testGetPackageFQName() {
        Optional<EPackage> ePackage = asmUtils.all(EPackage.class).filter(pkg -> "service".equals(pkg.getName())).findAny();

        assertTrue(ePackage.isPresent());
        assertThat(AsmUtils.getPackageFQName(ePackage.get()), is("demo.service"));
    }

    @Test
    public void testGetClassifierFQName() {
        Optional<EClassifier> eClassifier = asmUtils.all(EClassifier.class).filter(clsf -> "Countries".equals(clsf.getName())).findAny();

        assertTrue(eClassifier.isPresent());
        assertThat(AsmUtils.getClassifierFQName(eClassifier.get()), is("demo.types.Countries"));
    }

    @Test
    public void testGetAttributeFQName() {
        Optional<EAttribute> eAttribute = asmUtils.all(EAttribute.class).filter(attr -> "totalNumberOfOrders".equals(attr.getName())).findAny();

        assertTrue(eAttribute.isPresent());
        assertThat(AsmUtils.getAttributeFQName(eAttribute.get()), is("demo.entities.ʘStatic#totalNumberOfOrders"));
    }

    @Test
    public void testGetReferenceFQName() {
        Optional<EReference> eReference = asmUtils.all(EReference.class).filter(ref -> "owner".equals(ref.getName())).findAny();

        assertTrue(eReference.isPresent());
        assertThat(AsmUtils.getReferenceFQName(eReference.get()), is("demo.entities.Category#owner"));
    }

    @Test
    public void testGetOperationFQName() {
        Optional<EOperation> eOperation = asmUtils.all(EOperation.class).filter(op -> "getAllOrders".equals(op.getName())).findAny();

        assertTrue(eOperation.isPresent());
        assertThat(AsmUtils.getOperationFQName(eOperation.get()), is("demo.service.ʘUnboundServices#getAllOrders"));
    }

    @Test
    public void testResolve() {
        Optional<EClassifier> eClassifier = asmUtils.all(EClassifier.class).filter(clsf -> "Countries".equals(clsf.getName())).findAny();

        assertThat(asmUtils.resolve(eClassifier.get().getName()), is(eClassifier));
        //Assert.assertThat(asmUtils.resolve(""), is()); //TODO: negtest: by FQname not found, but nonFQname found
        assertThat(asmUtils.resolve("negtest"), is(Optional.empty()));
    }

    @Test
    public void testGetClassByFQName() {
        //isPresent & instanceof EClass
        assertThat(asmUtils.getClassByFQName("ProductInfo"), is(asmUtils.resolve("ProductInfo")));
        //isPresent
        assertTrue(asmUtils.resolve("Float").isPresent());
        assertThat(asmUtils.getClassByFQName("Float"), is(Optional.empty()));
        //not found
        assertFalse(asmUtils.resolve("negtest").isPresent());
    }

    @Test
    public void testGetNestedClasses() {
        //nested
        Optional<EClass> nestedClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘitemsʘReference".equals(c.getName())).findAny();
        assertTrue(nestedClass.isPresent());
        //container
        Optional<EClass> containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘitems".equals(c.getName())).findAny();
        assertTrue(containerClass.isPresent());

        assertTrue(asmUtils.getNestedClasses(containerClass.get()).contains(nestedClass.get()));

        //negtest: trying to get indirectly nested classes
        Optional<EClass> negtest_containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(negtest_containerClass.isPresent());

        assertFalse(asmUtils.getNestedClasses(negtest_containerClass.get()).contains(nestedClass.get()));
    }

    @Test
    public void testGetContainerClass() {
        //container
        Optional<EClass> containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘitems".equals(c.getName())).findAny();
        assertTrue(containerClass.isPresent());
        //nested
        Optional<EClass> nestedClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘitemsʘReference".equals(c.getName())).findAny();
        assertTrue(nestedClass.isPresent());

        assertThat(asmUtils.getContainerClass(nestedClass.get()).get(), is(containerClass.get()));

        //negtest: trying to get indirectly containing classes
        Optional<EClass> negtest_containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(negtest_containerClass.isPresent());

        assertThat(asmUtils.getContainerClass(negtest_containerClass.get()), is(Optional.empty()));
    }

    //@Test
    //public void
}
