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
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEDataTypeBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsmUtilsTest {

    static Logger log = LoggerFactory.getLogger(AsmUtilsTest.class);

    ResourceSet resourceSet;
    AsmUtils asmUtils;
    AsmModel asmModel;

    @BeforeEach
    public void setUp () throws Exception {
        asmModel = AsmModel.loadAsmModel(loadArgumentsBuilder()
                .uri(URI.createFileURI(new File("src/test/model/asm.model").getAbsolutePath()))
                .name("test")
                .build());

        asmUtils = new AsmUtils(asmModel.getResourceSet());
    }

    @AfterEach
    public void tearDown () {
        resourceSet = null;
    }

    @Test
    public void testGetPackageFQName () {
        Optional<EPackage> ePackage = asmUtils.all(EPackage.class).filter(pkg -> "service".equals(pkg.getName())).findAny();

        assertTrue(ePackage.isPresent());
        assertThat(AsmUtils.getPackageFQName(ePackage.get()), is("demo.service"));
    }

    @Test
    public void testGetClassifierFQName () {
        Optional<EClassifier> eClassifier = asmUtils.all(EClassifier.class).filter(clsf -> "Countries".equals(clsf.getName())).findAny();

        assertTrue(eClassifier.isPresent());
        assertThat(AsmUtils.getClassifierFQName(eClassifier.get()), is("demo.types.Countries"));
    }

    @Test
    public void testGetAttributeFQName () {
        Optional<EAttribute> eAttribute = asmUtils.all(EAttribute.class).filter(attr -> "totalNumberOfOrders".equals(attr.getName())).findAny();

        assertTrue(eAttribute.isPresent());
        assertThat(AsmUtils.getAttributeFQName(eAttribute.get()), is("demo.entities.ʘStatic#totalNumberOfOrders"));
    }

    @Test
    public void testGetReferenceFQName () {
        Optional<EReference> eReference = asmUtils.all(EReference.class).filter(ref -> "owner".equals(ref.getName())).findAny();

        assertTrue(eReference.isPresent());
        assertThat(AsmUtils.getReferenceFQName(eReference.get()), is("demo.entities.Category#owner"));
    }

    @Test
    public void testGetOperationFQName () {
        Optional<EOperation> eOperation = asmUtils.all(EOperation.class).filter(op -> "getAllOrders".equals(op.getName())).findAny();

        assertTrue(eOperation.isPresent());
        assertThat(AsmUtils.getOperationFQName(eOperation.get()), is("demo.service.ʘUnboundServices#getAllOrders"));
    }

    @Test
    public void testResolve () {
        Optional<EClassifier> eClassifier = asmUtils.all(EClassifier.class).filter(clsf -> "Countries".equals(clsf.getName())).findAny();

        assertThat(asmUtils.resolve(eClassifier.get().getName()), is(eClassifier));
        //Assert.assertThat(asmUtils.resolve(""), is()); //TODO: negtest: by FQname not found, but nonFQname found
        assertThat(asmUtils.resolve("negtest"), is(Optional.empty()));
    }

    @Test
    public void testGetClassByFQName () {
        //isPresent & instanceof EClass
        assertThat(asmUtils.getClassByFQName("ProductInfo"), is(asmUtils.resolve("ProductInfo")));
        //isPresent
        assertTrue(asmUtils.resolve("Float").isPresent());
        assertThat(asmUtils.getClassByFQName("Float"), is(Optional.empty()));
        //not found
        assertFalse(asmUtils.resolve("negtest").isPresent());
    }

    @Test
    public void testGetNestedClasses () {
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
    public void testGetContainerClass () {
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

    @Test
    public void testIsType() {
        Optional<EDataType> stringType = asmUtils.all(EDataType.class).filter(dt -> "String".equals(dt.getName())).findAny();
        Optional<EDataType> doubleType = asmUtils.all(EDataType.class).filter(dt -> "Double".equals(dt.getName())).findAny();
        Optional<EDataType> longType = asmUtils.all(EDataType.class).filter(dt -> "Long".equals(dt.getName())).findAny();
        Optional<EDataType> textType = asmUtils.all(EDataType.class).filter(dt -> "Text".equals(dt.getName())).findAny();
        Optional<EDataType> phoneType = asmUtils.all(EDataType.class).filter(dt -> "Phone".equals(dt.getName())).findAny();
        Optional<EDataType> urlType = asmUtils.all(EDataType.class).filter(dt -> "URL".equals(dt.getName())).findAny();
        Optional<EDataType> binaryType = asmUtils.all(EDataType.class).filter(dt -> "Binary".equals(dt.getName())).findAny();
        Optional<EDataType> integerType = asmUtils.all(EDataType.class).filter(dt -> "Integer".equals(dt.getName())).findAny();
        Optional<EDataType> booleanType = asmUtils.all(EDataType.class).filter(dt -> "Boolean".equals(dt.getName())).findAny();
        Optional<EDataType> timestampType = asmUtils.all(EDataType.class).filter(dt -> "Timestamp".equals(dt.getName())).findAny();
        Optional<EDataType> dateType = asmUtils.all(EDataType.class).filter(dt -> "Date".equals(dt.getName())).findAny();
        Optional<EDataType> floatType = asmUtils.all(EDataType.class).filter(dt -> "Float".equals(dt.getName())).findAny();
        Optional<EEnum> enumType = asmUtils.all(EEnum.class).filter(et -> "Countries".equals(et.getName())).findAny();
        EDataType utilDateType = newEDataTypeBuilder().withName("utilDate").withInstanceClassName("java.util.Date").build();
        EDataType byteArrayType = newEDataTypeBuilder().withName("byteArray").withInstanceClassName("byte[]").build();


        assertThat(asmUtils.isEnumeration(enumType.get()), equalTo(Boolean.TRUE));

        assertThat(asmUtils.isDecimal(floatType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isDecimal(doubleType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isNumeric(doubleType.get()), equalTo(Boolean.TRUE));

        assertThat(asmUtils.isInteger(integerType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isInteger(longType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isNumeric(longType.get()), equalTo(Boolean.TRUE));

        assertThat(asmUtils.isDate(dateType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isDate(utilDateType), equalTo(Boolean.TRUE));

        assertThat(asmUtils.isTimestamp(timestampType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isTimestamp(utilDateType), equalTo(Boolean.FALSE));

        assertThat(asmUtils.isBoolean(booleanType.get()), equalTo(Boolean.TRUE));

        assertThat(asmUtils.isString(stringType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isString(textType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isString(phoneType.get()), equalTo(Boolean.TRUE));
        assertThat(asmUtils.isString(urlType.get()), equalTo(Boolean.TRUE));

        assertThat(asmUtils.isByteArray(byteArrayType), equalTo(Boolean.TRUE));


        //assertThat(asmUtils.isText(textType.get()), equalTo(Boolean.TRUE)); //TODO
        //negtests: notNull, notContains
        assertThat(asmUtils.isString(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isBoolean(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isNumeric(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isInteger(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isDecimal(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isTimestamp(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isDate(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isEnumeration(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isText(binaryType.get()), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isByteArray(binaryType.get()), equalTo(Boolean.FALSE));

        //negtests: null(=> notContains)
        EDataType negtestType = newEDataTypeBuilder().withInstanceClassName(null).withName("negtestType").build();
        assertThat(asmUtils.isString(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isBoolean(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isNumeric(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isInteger(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isDecimal(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isTimestamp(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isDate(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isEnumeration(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isText(negtestType), equalTo(Boolean.FALSE));
        assertThat(asmUtils.isByteArray(negtestType), equalTo(Boolean.FALSE));

    }
}
