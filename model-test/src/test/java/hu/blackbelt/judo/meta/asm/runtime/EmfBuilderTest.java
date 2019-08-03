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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EmfBuilderTest {

    @Test
    public void buildsCompanyMetamodel() throws IOException {
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

        AsmModel asmModel = AsmModel.buildAsmModel()
                .uri(URI.createFileURI("Company.ecore"))
                .name("company")
                .build();
        asmModel.getResource().getContents().add(ePackage);

        asmModel.saveAsmModel();

        assertTrue(asmModel.isValid());

        final EPackage ePackageWrong = newEPackageBuilder()
                .withNsPrefix("wrong")
                .withNsURI("http:///com.example.company.ecore")
                .build();

        asmModel.getResource().getContents().add(ePackageWrong);

        assertFalse(asmModel.isValid());
        assertEquals(1, asmModel.getDiagnostics().size());
        assertEquals("The name 'null' is not well formed", asmModel.getDiagnostics().iterator().next().getMessage());

    }

}
