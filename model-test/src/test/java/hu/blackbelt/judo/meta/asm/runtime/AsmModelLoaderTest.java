package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.LoadArguments.loadArgumentsBuilder;

public class AsmModelLoaderTest {

    static Logger log = LoggerFactory.getLogger(AsmModelLoaderTest.class);
	
    @Test
    @DisplayName("Load Asm Model")
    void loadAsmModel() throws IOException {
        AsmModel asmModel = AsmModel.loadAsmModel(loadArgumentsBuilder()
                .uri(URI.createFileURI(new File("src/test/model/test.asm").getAbsolutePath()))
                .name("test")
                .build());

        for (Iterator<EObject> i = asmModel.getResourceSet().getResource(asmModel.getUri(), false).getAllContents(); i.hasNext(); ) {
            log.info(i.next().toString());
        }
    }
}