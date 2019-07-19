package hu.blackbelt.judo.meta.asm.runtime;

import com.google.common.collect.ImmutableList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAttributeBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEPackageBuilder;

public class InheritanceTest {

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
            final Resource resource = resourceSet.createResource(URI.createFileURI(inheritanceEcoreFile .getAbsolutePath()));
            resource.getContents().add(ePackage);
            resource.save(Collections.emptyMap());

            if (!resource.getErrors().isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                for (final Resource.Diagnostic diagnostic : resource.getErrors()) {
                    sb.append(diagnostic.getMessage()).append("\n");
                }
                Assert.fail(sb.toString());
            }
        } finally {
            // inheritanceEcoreFile.delete();
        }

        Assert.assertEquals(3, employeeClass.getEAllAttributes().size());
    }

    public File targetDir(){
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath);
        if(!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }
}
