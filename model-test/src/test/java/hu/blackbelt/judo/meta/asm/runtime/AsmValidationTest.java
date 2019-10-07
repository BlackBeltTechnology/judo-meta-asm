package hu.blackbelt.judo.meta.asm.runtime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hu.blackbelt.epsilon.runtime.execution.ExecutionContext;
import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.exceptions.EvlScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.Slf4jLog;
import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

import static hu.blackbelt.epsilon.runtime.execution.ExecutionContext.executionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.contexts.EvlExecutionContext.evlExecutionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.model.emf.WrappedEmfModelContext.wrappedEmfModelContextBuilder;
import static hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport.asmModelResourceSupportBuilder;


public class AsmValidationTest {

    private final String createdSourceModelName = "urn:Asm.model";
    private ExecutionContext executionContext;
    AsmModelResourceSupport asmModelSupport;

    private AsmModel asmModel;
    private AsmUtils asmUtils;
    
    private Log log = new Slf4jLog();
    
    Logger logger = LoggerFactory.getLogger(AsmValidationTest.class);

    @BeforeEach
    void setUp() {

        asmModelSupport = asmModelResourceSupportBuilder()
                .uri(URI.createFileURI(createdSourceModelName))
                .build();

        Log log = new Slf4jLog();

        asmUtils = new AsmUtils(asmModelSupport.getResourceSet(), false);
        
        asmModel = AsmModel.buildAsmModel()
        		.asmModelResourceSupport(asmModelSupport)
                .uri(URI.createURI(createdSourceModelName))
                .name("test")
                .build();

        // Execution context
        executionContext = executionContextBuilder()
                .log(log)
                .resourceSet(asmModelSupport.getResourceSet())
                .metaModels(ImmutableList.of())
                .modelContexts(ImmutableList.of(
                        wrappedEmfModelContextBuilder()
                                .log(log)
                                .name("ASM")
                                .resource(asmModelSupport.getResource())
                                .build()))
                .injectContexts(ImmutableMap.of("asmUtils", asmUtils))
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
