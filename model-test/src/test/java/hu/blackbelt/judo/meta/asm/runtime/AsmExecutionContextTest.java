package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;

import static  hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport.asmModelResourceSupportBuilder;

class ExecutionContextTest {

    @Test
    @DisplayName("Create Asm model with builder pattern")
    void testAsmReflectiveCreated() throws Exception {


        String createdSourceModelName = "urn:asm.judo-meta-asm";

        AsmModelResourceSupport asmModelSupport = asmModelResourceSupportBuilder().build();
        asmModelSupport.getResourceSet().createResource(
                URI.createFileURI(createdSourceModelName));

        // Build model here
    }
}