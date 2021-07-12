package hu.blackbelt.judo.meta.asm.runtime;

import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAttributeBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEOperationBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEPackageBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;

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

        final EPackage model = newEPackageBuilder()
                .withName("Model")
                .withNsPrefix("test")
                .withNsURI("http:///com.example.test.ecore")
                .build();

        asmModelResourceSupport.addContent(model);

        final EClass a1 = newEClassBuilder()
                .withName("A1")
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(a1);

        final EClass a2 = newEClassBuilder()
                .withName("A2")
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(a2);

        final EOperation a3Op = newEOperationBuilder()
                .withName(OP1_NAME)
                .build();
        final EClass a3 = newEClassBuilder()
                .withName("A3")
                .withAbstract_(true)
                .withEOperations(a3Op)
                .build();
        model.getEClassifiers().add(a3);
        AsmUtils.addExtensionAnnotation(a3Op, "abstract", "true");

        final EOperation a4Op = newEOperationBuilder()
                .withName(OP1_NAME)
                .build();
        final EClass a4 = newEClassBuilder()
                .withName("A4")
                .withAbstract_(true)
                .withEOperations(a4Op)
                .build();
        model.getEClassifiers().add(a4);
        AsmUtils.addExtensionAnnotation(a4Op, "abstract", "true");

        final EClass b1 = newEClassBuilder()
                .withName("B1")
                .withESuperTypes(Arrays.asList(a1, a2))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(b1);

        final EClass b2 = newEClassBuilder()
                .withName("B2")
                .withESuperTypes(Arrays.asList(a1, a2))
                .build();
        model.getEClassifiers().add(b2);

        final EClass b3 = newEClassBuilder()
                .withName("B3")
                .withESuperTypes(Arrays.asList(a2, a3))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(b3);

        final EClass b4 = newEClassBuilder()
                .withName("B4")
                .withESuperTypes(Arrays.asList(a2, a3))
                .build();
        model.getEClassifiers().add(b4);

        final EClass b5 = newEClassBuilder()
                .withName("B5")
                .withESuperTypes(Arrays.asList(a3, a4))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(b5);

        final EClass b6 = newEClassBuilder()
                .withName("B6")
                .withESuperTypes(Arrays.asList(a3, a4))
                .build();
        model.getEClassifiers().add(b6);

        final EClass x1 = newEClassBuilder()
                .withName("X1")
                .withESuperTypes(Arrays.asList(a2))
                .build();
        model.getEClassifiers().add(x1);

        final EClass x2 = newEClassBuilder()
                .withName("X2")
                .withESuperTypes(Arrays.asList(a2))
                .build();
        model.getEClassifiers().add(x2);

        final EClass x3 = newEClassBuilder()
                .withName("X3")
                .withESuperTypes(Arrays.asList(a2))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(x3);

        final EClass x4 = newEClassBuilder()
                .withName("X4")
                .withESuperTypes(Arrays.asList(a2))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(x4);

        final EOperation x5Op = newEOperationBuilder()
                .withName(OP1_NAME)
                .build();
        final EClass x5 = newEClassBuilder()
                .withName("X5")
                .withESuperTypes(Arrays.asList(a2))
                .withEOperations(x5Op)
                .build();
        model.getEClassifiers().add(x5);
        AsmUtils.addExtensionAnnotation(x5Op, "abstract", "true");

        final EOperation x6Op = newEOperationBuilder()
                .withName(OP1_NAME)
                .build();
        final EClass x6 = newEClassBuilder()
                .withName("X6")
                .withESuperTypes(Arrays.asList(a2))
                .withEOperations(x6Op)
                .build();
        model.getEClassifiers().add(x6);
        AsmUtils.addExtensionAnnotation(x6Op, "abstract", "true");

        final EClass x7 = newEClassBuilder()
                .withName("X7")
                .withESuperTypes(Arrays.asList(a3))
                .build();
        model.getEClassifiers().add(x7);

        final EClass x8 = newEClassBuilder()
                .withName("X8")
                .withESuperTypes(Arrays.asList(a3))
                .build();
        model.getEClassifiers().add(x8);

        final EClass x9 = newEClassBuilder()
                .withName("X9")
                .withESuperTypes(Arrays.asList(a3))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(x9);

        final EClass x10 = newEClassBuilder()
                .withName("X10")
                .withESuperTypes(Arrays.asList(a3))
                .withEOperations(newEOperationBuilder()
                        .withName(OP1_NAME)
                        .build())
                .build();
        model.getEClassifiers().add(x10);

        final EOperation x11Op = newEOperationBuilder()
                .withName(OP1_NAME)
                .build();
        final EClass x11 = newEClassBuilder()
                .withName("X11")
                .withESuperTypes(Arrays.asList(a3))
                .withEOperations(x11Op)
                .build();
        model.getEClassifiers().add(x11);
        AsmUtils.addExtensionAnnotation(x11Op, "abstract", "true");

        final EOperation x12Op = newEOperationBuilder()
                .withName(OP1_NAME)
                .build();
        final EClass x12 = newEClassBuilder()
                .withName("X12")
                .withESuperTypes(Arrays.asList(a3))
                .withEOperations(x12Op)
                .build();
        model.getEClassifiers().add(x12);
        AsmUtils.addExtensionAnnotation(x12Op, "abstract", "true");

        final EClass y1 = newEClassBuilder()
                .withName("Y1")
                .withESuperTypes(Arrays.asList(x1, x2))
                .build();
        model.getEClassifiers().add(y1);

        final EClass y2 = newEClassBuilder()
                .withName("Y2")
                .withESuperTypes(Arrays.asList(x2, x3))
                .build();
        model.getEClassifiers().add(y2);

        final EClass y3 = newEClassBuilder()
                .withName("Y3")
                .withESuperTypes(Arrays.asList(x3, x4))
                .build();
        model.getEClassifiers().add(y3);

        final EClass y4 = newEClassBuilder()
                .withName("Y4")
                .withESuperTypes(Arrays.asList(x1, x5))
                .build();
        model.getEClassifiers().add(y4);

        final EClass y5 = newEClassBuilder()
                .withName("Y5")
                .withESuperTypes(Arrays.asList(x5, x6))
                .build();
        model.getEClassifiers().add(y5);

        final EClass y6 = newEClassBuilder()
                .withName("Y6")
                .withESuperTypes(Arrays.asList(x3, x5))
                .build();
        model.getEClassifiers().add(y6);

        final EClass y7 = newEClassBuilder()
                .withName("Y7")
                .withESuperTypes(Arrays.asList(x7, x8))
                .build();
        model.getEClassifiers().add(y7);

        final EClass y8 = newEClassBuilder()
                .withName("Y8")
                .withESuperTypes(Arrays.asList(x8, x9))
                .build();
        model.getEClassifiers().add(y8);

        final EClass y9 = newEClassBuilder()
                .withName("Y9")
                .withESuperTypes(Arrays.asList(x9, x10))
                .build();
        model.getEClassifiers().add(y9);

        final EClass y10 = newEClassBuilder()
                .withName("Y10")
                .withESuperTypes(Arrays.asList(x7, x11))
                .build();
        model.getEClassifiers().add(y10);

        final EClass y11 = newEClassBuilder()
                .withName("Y11")
                .withESuperTypes(Arrays.asList(x11, x12))
                .build();
        model.getEClassifiers().add(y11);

        final EClass y12 = newEClassBuilder()
                .withName("Y12")
                .withESuperTypes(Arrays.asList(x9, x11))
                .build();
        model.getEClassifiers().add(y12);


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

        assertOperations(y1, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(y2, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(y3, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(y4, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(y5, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(y6, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(x3) + "#" + OP1_NAME, AsmUtils.getClassifierFQName(a2) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));

        assertOperations(y7, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a3) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(y8, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a3) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(x9) + "#" + OP1_NAME), Collections.emptySet());
        assertOperations(y9, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a3) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(y10, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a3) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(y11, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(a3) + "#" + OP1_NAME), Collections.emptySet(), ImmutableSet.of(OP1_NAME));
        assertOperations(y12, ImmutableSet.of(OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(x9) + "#" + OP1_NAME), ImmutableSet.of(AsmUtils.getClassifierFQName(x9) + "#" + OP1_NAME), Collections.emptySet());
    }

    private void assertOperations(final EClass clazz, final Set<String> expectedAllOperationNames, final Set<String> expectedAllOperationDeclarations, final Set<String> expectedAllOperationImplementations, final Set<String> expectedAllAbstractOperationNames) {
        final Set<String> allOperationNames = AsmUtils.getAllOperationNames(clazz);
        final Set<String> allOperationDeclarations = AsmUtils.getAllOperationDeclarations(clazz, false).stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.toSet());
        final Set<String> allOperationDeclarationsWithoutOverrides = AsmUtils.getAllOperationDeclarations(clazz, true).stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.toSet());
        final Set<String> allOperationImplementations = AsmUtils.getAllOperationImplementations(clazz).stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.toSet());
        final Set<String> allAbstractOperationNames = AsmUtils.getAllAbstractOperationNames(clazz);

        log.debug("Operations of {}: {}", clazz.getName(), clazz.getEOperations().stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.joining(", ")));
        log.debug("  - all operations of {}: {}", clazz.getName(), clazz.getEAllOperations().stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.joining(", ")));
        log.debug("  - all operation names: {}", allOperationNames);
        log.debug("  - all operation declarations: {}", allOperationDeclarations);
        log.debug("  - all operation declarations (without overrides): {}", allOperationDeclarationsWithoutOverrides);
        log.debug("  - all operation implementations: {}", allOperationImplementations);
        log.debug("  - all abstract operation names: {}", allAbstractOperationNames);

        assertThat(expectedAllOperationNames, equalTo(expectedAllOperationNames));
        assertThat(allOperationDeclarations, equalTo(allOperationDeclarationsWithoutOverrides));
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
