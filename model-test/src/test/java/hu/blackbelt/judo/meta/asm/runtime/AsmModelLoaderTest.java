package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.asmLoadArgumentsBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsmModelLoaderTest {

    static Logger log = LoggerFactory.getLogger(AsmModelLoaderTest.class);
	
    @Test
    @DisplayName("Load Asm Model")
    void loadAsmModel() throws IOException, AsmModel.AsmValidationException {
        AsmModel asmModel = AsmModel.loadAsmModel(asmLoadArgumentsBuilder()
                .uri(URI.createFileURI(new File("src/test/model/test.asm").getAbsolutePath()))
                .name("test"));

        assertTrue(asmModel.isValid());

        for (Iterator<EObject> i = asmModel.getResourceSet().getResource(asmModel.getUri(), false).getAllContents(); i.hasNext(); ) {
            log.info(i.next().toString());
        }
    }

    @Test
    @DisplayName("Diagnose Asm Model - validate model disabled")
    void loadInvalidAsmModelWithDisabledValidation() throws IOException, AsmModel.AsmValidationException {
        AsmModel asmModel = AsmModel.loadAsmModel(asmLoadArgumentsBuilder()
                .validateModel(false)
                .uri(URI.createFileURI(new File("src/test/model/invalid-asm.model").getAbsolutePath()))
                .name("test"));

        assertFalse(asmModel.isValid());
        assertTrue(asmModel.getDiagnostics().size() == 1);
        assertEquals("There may not be two features named 'orders'", asmModel.getDiagnostics().iterator().next().getMessage());
    }

    @Test
    @DisplayName("Diagnose Asm Model - validate model enabled")
    void loadInvalidAsmModel() throws IOException, AsmModel.AsmValidationException {
        AsmModel.AsmValidationException thrown = assertThrows(AsmModel.AsmValidationException.class,
                () -> AsmModel.loadAsmModel(asmLoadArgumentsBuilder()
                        .uri(URI.createFileURI(new File("src/test/model/invalid-asm.model").getAbsolutePath()))
                        .name("test")), "Expected AsmValidationException, but not thrown");
    }

}