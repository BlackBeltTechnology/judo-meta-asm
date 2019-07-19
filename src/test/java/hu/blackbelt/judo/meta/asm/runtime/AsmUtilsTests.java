package hu.blackbelt.judo.meta.asm.runtime;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;

@Slf4j
public class AsmUtilsTests {

    ResourceSet resourceSet;
    AsmUtils asmUtils;
    AsmModel asmModel;

    @BeforeEach
    public void setUp() throws Exception {
        asmModel = AsmModelLoader.loadAsmModel(
                AsmModelLoader.createAsmResourceSet(),
                URI.createURI(new File(srcDir(), "test/resources/asm.model").getAbsolutePath()),
                "test",
                "1.0.0");

        asmUtils = new AsmUtils(asmModel.getResourceSet());
    }

    @AfterEach
    public void tearDown() {
        resourceSet = null;
    }

    private File srcDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "../../src");
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }


    @Test
    public void testGetPackageFQName() {
        Optional<EPackage> ePackage = asmUtils.all(EPackage.class).filter(pkg -> "service".equals(pkg.getName())).findAny();

        Assert.assertTrue(ePackage.isPresent());
        Assert.assertThat(asmUtils.getPackageFQName(ePackage.get()), is("demo.service"));
    }

    @Test
    public void testGetClassifierFQName() {
        Optional<EClassifier> eClassifier = asmUtils.all(EClassifier.class).filter(clsf -> "Countries".equals(clsf.getName())).findAny();

        Assert.assertTrue(eClassifier.isPresent());
        Assert.assertThat(asmUtils.getClassifierFQName(eClassifier.get()), is("demo.types.Countries"));
    }

    @Test
    public void testGetAttributeFQName() {
        Optional<EAttribute> eAttribute = asmUtils.all(EAttribute.class).filter(attr -> "totalNumberOfOrders".equals(attr.getName())).findAny();

        Assert.assertTrue(eAttribute.isPresent());
        Assert.assertThat(asmUtils.getAttributeFQName(eAttribute.get()), is("demo.entities.ʘStatic#totalNumberOfOrders"));
    }

    @Test
    public void testGetReferenceFQName() {
        Optional<EReference> eReference = asmUtils.all(EReference.class).filter(ref -> "owner".equals(ref.getName())).findAny();

        Assert.assertTrue(eReference.isPresent());
        Assert.assertThat(asmUtils.getReferenceFQName(eReference.get()), is("demo.entities.Category#owner"));
    }

    @Test
    public void testGetOperationFQName() {
        Optional<EOperation> eOperation = asmUtils.all(EOperation.class).filter(op -> "getAllOrders".equals(op.getName())).findAny();

        Assert.assertTrue(eOperation.isPresent());
        Assert.assertThat(asmUtils.getOperationFQName(eOperation.get()), is("demo.service.ʘUnboundServices#getAllOrders"));
    }

    @Test
    public void testResolve() {
        Optional<EClassifier> eClassifier = asmUtils.all(EClassifier.class).filter(clsf -> "Countries".equals(clsf.getName())).findAny();

        Assert.assertThat(asmUtils.resolve(eClassifier.get().getName()), is(eClassifier));
        //Assert.assertThat(asmUtils.resolve(""), is()); //TODO: negtest: by FQname not found, but nonFQname found
        Assert.assertThat(asmUtils.resolve("negtest"), is(Optional.empty()));
    }

    @Test
    public void testGetClassByFQName() {
        //isPresent & instanceof EClass
        Assert.assertThat(asmUtils.getClassByFQName("ProductInfo"), is(asmUtils.resolve("ProductInfo")));
        //isPresent
        Assert.assertTrue(asmUtils.resolve("Float").isPresent());
        Assert.assertThat(asmUtils.getClassByFQName("Float"), is(Optional.empty()));
        //not found
        Assert.assertFalse(asmUtils.resolve("negtest").isPresent());
    }

    @Test
    public void testGetNestedClasses() {
        //nested
        Optional<EClass> nestedClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘitemsʘReference".equals(c.getName())).findAny();
        Assert.assertTrue(nestedClass.isPresent());
        //container
        Optional<EClass> containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘitems".equals(c.getName())).findAny();
        Assert.assertTrue(containerClass.isPresent());

        Assert.assertTrue(asmUtils.getNestedClasses(containerClass.get()).contains(nestedClass.get()));

        //negtest: trying to get indirectly nested classes
        Optional<EClass> negtest_containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        Assert.assertTrue(negtest_containerClass.isPresent());

        Assert.assertFalse(asmUtils.getNestedClasses(negtest_containerClass.get()).contains(nestedClass.get()));
    }

    @Test
    public void testGetContainerClass() {
        //container
        Optional<EClass> containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘitems".equals(c.getName())).findAny();
        Assert.assertTrue(containerClass.isPresent());
        //nested
        Optional<EClass> nestedClass = asmUtils.all(EClass.class).filter(c -> "OrderInfoʘitemsʘReference".equals(c.getName())).findAny();
        Assert.assertTrue(nestedClass.isPresent());

        Assert.assertThat(asmUtils.getContainerClass(nestedClass.get()).get(), is(containerClass.get()));

        //negtest: trying to get indirectly containing classes
        Optional<EClass> negtest_containerClass = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        Assert.assertTrue(negtest_containerClass.isPresent());

        Assert.assertThat(asmUtils.getContainerClass(negtest_containerClass.get()), is(Optional.empty()));
    }

    //@Test
    //public void


}
