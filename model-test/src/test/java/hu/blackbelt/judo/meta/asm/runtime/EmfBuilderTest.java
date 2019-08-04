package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.asmLoadArgumentsBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EmfBuilderTest {

    @Test
    @DisplayName("Build valid company metamodel")
    public void buildValidCompanyMetamodel() throws IOException, AsmModel.AsmValidationException {

        AsmModel asmModel = AsmModel.buildAsmModel()
                .uri(URI.createFileURI("target/test-classes/Company.ecore"))
                .name("company")
                .build();
        asmModel.addContent(createCopmanyPackage());

        asmModel.saveAsmModel();

        assertTrue(asmModel.isValid());
    }

    @Test
    @DisplayName("Build valid company metamodel")
    public void buildInvalidCompanyMetamodel() throws IOException, AsmModel.AsmValidationException {

        AsmModel asmModel = AsmModel.buildAsmModel()
                .uri(URI.createFileURI("target/test-classes/CompanyInvalid.ecore"))
                .name("company")
                .build();
        asmModel.addContent(createCopmanyPackage());

        assertTrue(asmModel.isValid());

        final EPackage ePackageWrong = newEPackageBuilder()
                .withNsPrefix("wrong")
                .withNsURI("http:///com.example.company.ecore")
                .build();

        asmModel.addContent(ePackageWrong);

        assertFalse(asmModel.isValid());
        assertEquals(1, asmModel.getDiagnostics().size());
        assertEquals("The name 'null' is not well formed", asmModel.getDiagnostics().iterator().next().getMessage());


        // Test save invalid model thrown validation exception
        AsmModel.AsmValidationException thrown = assertThrows(AsmModel.AsmValidationException.class,
                () -> asmModel.saveAsmModel(), "Expected AsmValidationException, but not thrown");

        // Test save invalid model disable validation
        asmModel.saveAsmModel(
                AsmModel.SaveArguments
                        .asmSaveArgumentsBuilder().validateModel(false).build());

    }

    private EPackage createCopmanyPackage() {
        final EcorePackage ecore = EcorePackage.eINSTANCE;
        // @formatter:off
        final EClass employeeClass = newEClassBuilder()
                .withName("Employee")
                .withEStructuralFeatures(
                        newEAttributeBuilder()
                                .withName("name")
                                .withEType(ecore.getEString())
                                .build()
                )
                .build();

        final EClass departmentClass = newEClassBuilder()
                .withName("Department")
                .withEStructuralFeatures(
                        newEAttributeBuilder()
                                .withName("number")
                                .withEType(ecore.getEInt())
                                .build()
                )
                .withEStructuralFeatures(
                        newEReferenceBuilder()
                                .withName("employees")
                                .withEType(employeeClass)
                                .withUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY)
                                .withContainment(true)
                                .build()
                )
                .build();

        final EClass companyClass = newEClassBuilder()
                .withName("Company")
                .withEStructuralFeatures(
                        newEAttributeBuilder()
                                .withName("name")
                                .withEType(ecore.getEString()).build()
                )
                .withEStructuralFeatures(
                        newEReferenceBuilder()
                                .withName("department")
                                .withEType(departmentClass)
                                .withUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY)
                                .withContainment(true)
                                .build()
                )
                .build();

        final EPackage ePackage = newEPackageBuilder()
                .withName("company")
                .withNsPrefix("company")
                .withNsURI("http:///com.example.company.ecore")
                .withEClassifiers(employeeClass)
                .withEClassifiers(companyClass)
                .withEClassifiers(departmentClass)
                .build();
        // @formatter:on
        return ePackage;
    }

}
