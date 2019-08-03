package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.util.builder.EPackageBuilder;
import org.eclipse.emf.ecore.util.builder.EcoreBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEPackageBuilder;

class AsmExecutionContextTest {

    @Test
    @DisplayName("Create Asm model with builder pattern")
    void testAsmReflectiveCreated() throws Exception {


        AsmModel asmModel = AsmModel.buildAsmModel()
                .name("test")
                .uri(URI.createURI("asm.judo-meta-asm"))
                .build();
        asmModel.getResource().getContents().add(newEPackageBuilder().withName("test").withNsURI("http://test").build());
        // Build model here
    }
}