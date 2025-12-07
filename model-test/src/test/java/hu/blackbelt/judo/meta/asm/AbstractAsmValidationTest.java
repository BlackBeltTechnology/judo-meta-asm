package hu.blackbelt.judo.meta.asm;

/*-
 * #%L
 * Judo :: Asm :: Model :: Test
 * %%
 * Copyright (C) 2018 - 2022 BlackBelt Technology
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import static hu.blackbelt.epsilon.runtime.execution.ExecutionContext.executionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.contexts.EvlExecutionContext.evlExecutionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.model.emf.WrappedEmfModelContext.wrappedEmfModelContextBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

import hu.blackbelt.epsilon.runtime.execution.ExecutionContext;
import hu.blackbelt.epsilon.runtime.execution.exceptions.EvlScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.asm.runtime.AsmEpsilonValidator;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import hu.blackbelt.judo.meta.asm.validation.AsmValidator;
import java.util.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.epsilon.common.util.UriUtil;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for ASM validation tests.
 *
 * <p>Provides common infrastructure for testing both EVL and Java validators
 * with the same test cases using JUnit 5 parameterized tests.</p>
 *
 * <p>The key challenge is that EVL and Java validators use different expected error formats:
 * <ul>
 *   <li>EVL uses: {@code "ConstraintName|Full error message with context"}</li>
 *   <li>Java uses: {@code "ConstraintName"} (just the constraint name)</li>
 * </ul>
 *
 * <p>This base class handles this difference by running EVL validation with empty
 * expected collections to force it to throw an exception with all results, then
 * extracting constraint names for comparison.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class AsmValidationTest extends AbstractAsmValidationTest {
 *
 *     @ParameterizedTest(name = "testSomeConstraint [{0}]")
 *     @EnumSource(ValidatorType.class)
 *     void testSomeConstraint(ValidatorType type) throws Exception {
 *         this.validatorType = type;
 *         initModel();
 *
 *         // ... build model ...
 *
 *         runValidation(
 *             ImmutableList.of("SomeExpectedError"),
 *             ImmutableList.of()
 *         );
 *     }
 * }
 * }</pre>
 *
 * @see ValidatorType
 * @see AsmValidator
 * @see AsmEpsilonValidator
 */
public abstract class AbstractAsmValidationTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final String createdSourceModelName = "urn:asm.judo-meta-asm";

    protected AsmModel asmModel;
    protected ValidatorType validatorType;

    /**
     * Initialize the test with a fresh AsmModel.
     * Call this at the start of each parameterized test method.
     */
    protected void initModel() {
        asmModel = AsmModel.buildAsmModel()
            .uri(URI.createURI(createdSourceModelName))
            .build();
    }

    /**
     * Run validation using the selected validator type.
     *
     * <p>For EVL validation, this method handles the format difference by:
     * <ol>
     *   <li>Running EVL with empty expected collections to force exception</li>
     *   <li>Extracting constraint names from the exception</li>
     *   <li>Comparing against expected constraint names</li>
     * </ol>
     *
     * @param expectedErrors expected error constraint names (null means no errors expected)
     * @param expectedWarnings expected warning constraint names (null means warnings are ignored)
     * @throws Exception if validation fails unexpectedly
     */
    protected void runValidation(
        Collection<String> expectedErrors,
        Collection<String> expectedWarnings
    ) throws Exception {
        log.debug("ASM diagnostics: {}", asmModel.getDiagnosticsAsString());
        Assertions.assertTrue(
            asmModel.isValid(),
            "Model should be structurally valid"
        );

        try (BufferedSlf4jLogger bufferedLogger = new BufferedSlf4jLogger(log)) {
            switch (validatorType) {
                case EVL:
                    log.info("Running EVL validation...");
                    runEvlValidation(
                        bufferedLogger,
                        expectedErrors,
                        expectedWarnings
                    );
                    break;
                case JAVA:
                    log.info("Running Java validation...");
                    AsmValidator.validateAsm(
                        bufferedLogger,
                        asmModel,
                        expectedErrors,
                        expectedWarnings,
                        false // Sequential for deterministic results
                    );
                    break;
                default:
                    throw new IllegalStateException(
                        "Unknown validator type: " + validatorType
                    );
            }
        } catch (AsmModel.AsmValidationException ex) {
            log.error("{} validation failed", validatorType, ex);
            throw ex;
        }
    }

    /**
     * Run EVL validation with constraint name matching (not full key matching).
     *
     * <p>This method runs EVL validation with empty expected collections to force
     * it to throw an exception containing the actual results. We then extract
     * constraint names from the unsatisfied constraints for comparison.</p>
     */
    private void runEvlValidation(
        BufferedSlf4jLogger bufferedLogger,
        Collection<String> expectedErrors,
        Collection<String> expectedWarnings
    ) throws Exception {
        // Build and execute EVL with empty expected collections to force exception
        ExecutionContext executionContext = executionContextBuilder()
            .log(bufferedLogger)
            .resourceSet(asmModel.getResourceSet())
            .metaModels(emptyList())
            .modelContexts(
                asList(
                    wrappedEmfModelContextBuilder()
                        .log(bufferedLogger)
                        .name("ASM")
                        .resource(asmModel.getResource())
                        .validateModel(false)
                        .useCache(false)
                        .build()
                )
            )
            .injectContexts(singletonMap("asmUtils", new AsmUtils(asmModel.getResourceSet())))
            .build();

        Set<String> actualErrorNames = new HashSet<>();
        Set<String> actualWarningNames = new HashSet<>();

        try {
            // Load the model
            executionContext.load();

            // Execute EVL with empty expected collections
            executionContext.executeProgram(
                evlExecutionContextBuilder()
                    .source(
                        UriUtil.resolve(
                            "asm.evl",
                            AsmEpsilonValidator.calculateAsmValidationScriptURI()
                        )
                    )
                    .expectedErrors(Collections.emptyList())
                    .expectedWarnings(Collections.emptyList())
                    .parallel(false)
                    .build()
            );

            // If we get here, no errors or warnings were found
            log.debug("EVL validation completed with no errors or warnings");
        } catch (EvlScriptExecutionException ex) {
            // Extract constraint names from the exception
            if (ex.getUnsatisfiedErrors() != null) {
                for (UnsatisfiedConstraint uc : ex.getUnsatisfiedErrors()) {
                    actualErrorNames.add(uc.getConstraint().getName());
                    log.debug(
                        "EVL error: {} - {}",
                        uc.getConstraint().getName(),
                        uc.getMessage()
                    );
                }
            }
            if (ex.getUnsatisfiedWarnings() != null) {
                for (UnsatisfiedConstraint uc : ex.getUnsatisfiedWarnings()) {
                    actualWarningNames.add(uc.getConstraint().getName());
                    log.debug(
                        "EVL warning: {} - {}",
                        uc.getConstraint().getName(),
                        uc.getMessage()
                    );
                }
            }
        } finally {
            executionContext.commit();
            try {
                executionContext.close();
            } catch (Exception e) {
                log.warn("Unable to close EVL execution context", e);
            }
        }

        // Now compare with expected constraint names
        Set<String> expectedErrorSet = expectedErrors != null
            ? new HashSet<>(expectedErrors)
            : Collections.emptySet();

        Set<String> expectedWarningSet = expectedWarnings != null
            ? new HashSet<>(expectedWarnings)
            : null; // null means "don't care about warnings"

        // Check errors
        Set<String> unexpectedErrors = new HashSet<>(actualErrorNames);
        unexpectedErrors.removeAll(expectedErrorSet);

        Set<String> missingErrors = new HashSet<>(expectedErrorSet);
        missingErrors.removeAll(actualErrorNames);

        // Check warnings only if expectedWarnings was explicitly provided
        Set<String> unexpectedWarnings = Collections.emptySet();
        Set<String> missingWarnings = Collections.emptySet();
        if (expectedWarningSet != null) {
            unexpectedWarnings = new HashSet<>(actualWarningNames);
            unexpectedWarnings.removeAll(expectedWarningSet);
            missingWarnings = new HashSet<>(expectedWarningSet);
            missingWarnings.removeAll(actualWarningNames);
        }

        boolean hasIssues =
            !unexpectedErrors.isEmpty() ||
            !missingErrors.isEmpty() ||
            !unexpectedWarnings.isEmpty() ||
            !missingWarnings.isEmpty();

        if (hasIssues) {
            log.error("EVL validation result mismatch:");
            log.error("  Actual errors: {}", actualErrorNames);
            log.error("  Expected errors: {}", expectedErrorSet);
            log.error("  Actual warnings: {}", actualWarningNames);
            log.error("  Expected warnings: {}", expectedWarningSet);
            if (!unexpectedErrors.isEmpty()) {
                log.error("  Unexpected errors: {}", unexpectedErrors);
            }
            if (!missingErrors.isEmpty()) {
                log.error("  Missing errors: {}", missingErrors);
            }
            if (!unexpectedWarnings.isEmpty()) {
                log.error("  Unexpected warnings: {}", unexpectedWarnings);
            }
            if (!missingWarnings.isEmpty()) {
                log.error("  Missing warnings: {}", missingWarnings);
            }
            throw new AsmModel.AsmValidationException(asmModel);
        }

        // Log success
        log.info(
            "EVL validation passed: {} errors, {} warnings",
            actualErrorNames.size(),
            actualWarningNames.size()
        );
    }
}
