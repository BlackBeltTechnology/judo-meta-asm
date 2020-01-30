package hu.blackbelt.judo.meta.asm.runtime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class InheritanceTest {

    private static final Logger log = LoggerFactory.getLogger(InheritanceTest.class);

    @Test
    public void testInheritedAttributes() throws IOException {
        final EcorePackage ecore = EcorePackage.eINSTANCE;

        final EClass personClass = newEClassBuilder()
                .withName("Person")
                .withEStructuralFeatures(
                        ImmutableList.of(newEAttributeBuilder()
                                        .withName("firstName")
                                        .withEType(ecore.getEString())
                                        .build(),
                                newEAttributeBuilder()
                                        .withName("lastName")
                                        .withEType(ecore.getEString())
                                        .build()))
                .build();

        final EClass employeeClass = newEClassBuilder()
                .withName("Employee")
                .withEStructuralFeatures(
                        newEAttributeBuilder()
                                .withName("room")
                                .withEType(ecore.getEIntegerObject())
                                .build())
                .withESuperTypes(personClass)
                .build();

        final EPackage ePackage = newEPackageBuilder()
                .withName("test")
                .withNsPrefix("test")
                .withNsURI("http:///com.example.test.ecore")
                .withEClassifiers(employeeClass)
                .withEClassifiers(personClass)
                .build();

        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
        final File inheritanceEcoreFile = new File(targetDir(), "inheritance.ecore");
        try {
            final Resource resource = resourceSet.createResource(URI.createFileURI(inheritanceEcoreFile.getAbsolutePath()));
            resource.getContents().add(ePackage);
            resource.save(Collections.emptyMap());

            if (!resource.getErrors().isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                for (final Resource.Diagnostic diagnostic : resource.getErrors()) {
                    sb.append(diagnostic.getMessage()).append("\n");
                }
                fail(sb.toString());
            }
        } finally {
            // inheritanceEcoreFile.delete();
        }

        assertEquals(3, employeeClass.getEAllAttributes().size());
    }

    @Test
    void testOperationInMultipleInheritance() {
        final AsmModelResourceSupport asmModelResourceSupport = AsmModelResourceSupport.asmModelResourceSupportBuilder()
                .uri(URI.createURI("asm:test"))
                .build();

        final String OP1_NAME = "op1";

        final EClass a1 = newEClassBuilder()
                .withName("A1")
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();

        final EClass a2 = newEClassBuilder()
                .withName("A2")
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();

        final EOperation a3Op = newEOperationBuilder()
                .withName(OP1_NAME)
                .build();
        AsmUtils.addExtensionAnnotation(a3Op, "abstract", "true");
        final EClass a3 = newEClassBuilder()
                .withName("A3")
                .withAbstract_(true)
                .withEOperations(a3Op)
                .build();

        final EOperation a4Op = newEOperationBuilder()
                .withName(OP1_NAME)
                .build();
        AsmUtils.addExtensionAnnotation(a4Op, "abstract", "true");
        final EClass a4 = newEClassBuilder()
                .withName("A4")
                .withAbstract_(true)
                .withEOperations(a4Op)
                .build();

        final EClass b1 = newEClassBuilder()
                .withName("B1")
                .withESuperTypes(Arrays.asList(a1, a2))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();

        final EClass b2 = newEClassBuilder()
                .withName("B2")
                .withESuperTypes(Arrays.asList(a1, a2))
                .build();

        final EClass b3 = newEClassBuilder()
                .withName("B3")
                .withESuperTypes(Arrays.asList(a2, a3))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();

        final EClass b4 = newEClassBuilder()
                .withName("B4")
                .withESuperTypes(Arrays.asList(a2, a3))
                .build();

        final EClass b5 = newEClassBuilder()
                .withName("B5")
                .withESuperTypes(Arrays.asList(a3, a4))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();

        final EClass b6 = newEClassBuilder()
                .withName("B6")
                .withESuperTypes(Arrays.asList(a3, a4))
                .build();

        final EPackage model = newEPackageBuilder()
                .withName("Model")
                .withNsPrefix("test")
                .withNsURI("http:///com.example.test.ecore")
                .withEClassifiers(Arrays.asList(a1, a2, a3, a4, b1, b2, b3, b4, b5, b6))
                .build();

        asmModelResourceSupport.addContent(model);

        assertOperations(a1, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a1) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a1) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(a2, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(a3, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a3) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(a4, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a4) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(b1, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(b1) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(b1) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(b2, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a1) + "#" + OP1_NAME, AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(b3, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(b3) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(b3) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(b4, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME, AsmUtils.getClassifierFQName(a3) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(b5, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(b5) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(b5) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(b6, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a3) + "#" + OP1_NAME, AsmUtils.getClassifierFQName(a4) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
    }

    private void assertOperations(final EClass clazz, final Set<String> expectedAllOperationNames, final Set<String> expectedAllOperationDeclarations, final Set<String> expectedAllOperationImplementations, final Set<String> expectedAllAbstractOperationNames) {
        final Set<String> allOperationNames = AsmUtils.getAllOperationNames(clazz);
        final Set<String> allOperationDeclarations = AsmUtils.getAllOperationDeclarations(clazz).stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.toSet());
        final Set<String> allOperationImplementations = AsmUtils.getAllOperationImplementations(clazz).stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.toSet());
        final Set<String> allAbstractOperationNames = AsmUtils.getAllAbstractOperationNames(clazz);

        log.debug("Operations of {}: {}", clazz.getName(), clazz.getEOperations().stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.joining(", ")));
        log.debug("  - all operations of {}: {}", clazz.getName(), clazz.getEAllOperations().stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.joining(", ")));
        log.debug("  - all operation names: {}", allOperationNames);
        log.debug("  - all operation declarations: {}", allOperationDeclarations);
        log.debug("  - all operation implementations: {}", allOperationImplementations);
        log.debug("  - all abstract operation names: {}", allAbstractOperationNames);

        assertThat(expectedAllOperationNames, equalTo(expectedAllOperationNames));
        assertThat(allOperationDeclarations, equalTo(expectedAllOperationDeclarations));
        assertThat(allOperationImplementations, equalTo(expectedAllOperationImplementations));
        assertThat(allAbstractOperationNames, equalTo(expectedAllAbstractOperationNames));
    }

    public File targetDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath);
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }
}
