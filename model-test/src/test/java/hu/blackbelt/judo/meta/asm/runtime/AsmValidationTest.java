package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.exceptions.EvlScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.Slf4jLog;
import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;

import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport.asmModelResourceSupportBuilder;

public class AsmValidationTest {

    private final String createdSourceModelName = "urn:Asm.model";
    AsmModelResourceSupport asmModelSupport;

    private AsmModel asmModel;
    
    private Log log = new Slf4jLog();
    
    Logger logger = LoggerFactory.getLogger(AsmValidationTest.class);

    @BeforeEach
    void setUp() {

        asmModelSupport = asmModelResourceSupportBuilder()
                .uri(URI.createFileURI(createdSourceModelName))
                .build();
        
        asmModel = AsmModel.buildAsmModel()
        		.asmModelResourceSupport(asmModelSupport)
                .uri(URI.createURI(createdSourceModelName))
                .name("test")
                .build();
    }

    private void runEpsilon (Collection<String> expectedErrors, Collection<String> expectedWarnings) throws Exception {
        try {
            AsmEpsilonValidator.validateAsm(log,
                    asmModel,
                    AsmEpsilonValidator.calculateAsmValidationScriptURI(),
                    expectedErrors,
                    expectedWarnings);
        } catch (EvlScriptExecutionException ex) {
            logger.error("EVL failed", ex);
            logger.error("\u001B[31m - expected errors: {}\u001B[0m", expectedErrors);
            logger.error("\u001B[31m - unexpected errors: {}\u001B[0m", ex.getUnexpectedErrors());
            logger.error("\u001B[31m - errors not found: {}\u001B[0m", ex.getErrorsNotFound());
            logger.error("\u001B[33m - expected warnings: {}\u001B[0m", expectedWarnings);
            logger.error("\u001B[33m - unexpected warnings: {}\u001B[0m", ex.getUnexpectedWarnings());
            logger.error("\u001B[33m - warnings not found: {}\u001B[0m", ex.getWarningsNotFound());
            throw ex;
        }
    }
}
