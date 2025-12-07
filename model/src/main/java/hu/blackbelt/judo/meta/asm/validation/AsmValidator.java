package hu.blackbelt.judo.meta.asm.validation;

/*-
 * #%L
 * Judo :: Asm :: Model
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

import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import hu.blackbelt.judo.meta.asm.validation.core.*;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;

/**
 * Entry point for Java-based ASM model validation.
 *
 * <p>This validator provides a native Java alternative to EVL (Epsilon Validation Language)
 * validation with better IDE integration, debugging support, and performance.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * AsmValidator.validateAsm(log, asmModel);
 * }
 * </pre>
 */
public class AsmValidator {

    /**
     * Validate ASM model using Java validation rules.
     *
     * @param log the logger
     * @param asmModel the model to validate
     * @throws AsmModel.AsmValidationException if validation fails
     */
    public static void validateAsm(Logger log, AsmModel asmModel)
        throws AsmModel.AsmValidationException {
        validateAsm(log, asmModel, null, null, false);
    }

    /**
     * Validate ASM model with expected errors and warnings (for testing).
     *
     * @param log the logger
     * @param asmModel the model to validate
     * @param expectedErrors expected error constraint names
     * @param expectedWarnings expected warning constraint names
     * @throws AsmModel.AsmValidationException if validation fails
     */
    public static void validateAsm(
        Logger log,
        AsmModel asmModel,
        Collection<String> expectedErrors,
        Collection<String> expectedWarnings
    ) throws AsmModel.AsmValidationException {
        validateAsm(log, asmModel, expectedErrors, expectedWarnings, false);
    }

    /**
     * Validate ASM model with all options.
     *
     * @param log the logger
     * @param asmModel the model to validate
     * @param expectedErrors expected error constraint names
     * @param expectedWarnings expected warning constraint names
     * @param parallel use parallel execution
     * @throws AsmModel.AsmValidationException if validation fails
     */
    public static void validateAsm(
        Logger log,
        AsmModel asmModel,
        Collection<String> expectedErrors,
        Collection<String> expectedWarnings,
        boolean parallel
    ) throws AsmModel.AsmValidationException {
        log.info("Starting Java-based ASM validation...");

        // Create validation infrastructure
        ValidationRegistry registry = new ValidationRegistry();
        ExtensionMethodRegistry extensionRegistry = new ExtensionMethodRegistry();

        // Register extension methods
        try {
            // Currently no extension methods registered - add as needed
            // extensionRegistry.register(AsmUtilsExtensions.class);
            log.debug("Extension method registry initialized");
        } catch (Exception e) {
            log.error("Failed to register extension methods", e);
            throw new RuntimeException(e);
        }

        // Create validation context
        ValidationContext context = new ValidationContext(
            new AsmUtils(asmModel.getResourceSet()),
            asmModel.getResourceSet(),
            extensionRegistry
        );

        // Register validation rule classes
        // Currently no validation rules - the asm.evl is empty
        // Add validation rule classes here as needed:
        // registry.register(SomeValidations.class);
        log.debug("Validation registry initialized (no rules registered - asm.evl is empty)");

        // Set registry in context for satisfies() support
        context.setValidationRegistry(registry);

        // Collect all elements to validate
        AsmUtils asmUtils = new AsmUtils(asmModel.getResourceSet());
        List<EObject> allElements = asmUtils.all(EObject.class)
            .collect(Collectors.toList());

        log.info("Validating {} elements...", allElements.size());

        // Create and run executor
        ValidationExecutor executor = new ValidationExecutor(
            registry,
            context,
            parallel
        );
        List<ValidationResult> failures = executor.validate(allElements);

        // Separate errors and warnings
        List<ValidationResult> errors = failures.stream()
            .filter(r -> r.getSeverity() == Severity.ERROR)
            .collect(Collectors.toList());

        List<ValidationResult> warnings = failures.stream()
            .filter(r -> r.getSeverity() == Severity.WARNING)
            .collect(Collectors.toList());

        // Extract constraint names
        Set<String> actualErrors = errors.stream()
            .map(ValidationResult::getConstraintName)
            .collect(Collectors.toSet());

        Set<String> actualWarnings = warnings.stream()
            .map(ValidationResult::getConstraintName)
            .collect(Collectors.toSet());

        // Compare with expected (if provided)
        Set<String> expectedErrorSet = expectedErrors != null
            ? new HashSet<>(expectedErrors)
            : Collections.emptySet();

        Set<String> expectedWarningSet = expectedWarnings != null
            ? new HashSet<>(expectedWarnings)
            : null; // null means "don't care about warnings"

        // Check errors
        Set<String> unexpectedErrors = new HashSet<>(actualErrors);
        unexpectedErrors.removeAll(expectedErrorSet);

        Set<String> missingErrors = new HashSet<>(expectedErrorSet);
        missingErrors.removeAll(actualErrors);

        // Check warnings only if expectedWarnings was explicitly provided
        Set<String> unexpectedWarnings = Collections.emptySet();
        Set<String> missingWarnings = Collections.emptySet();
        if (expectedWarningSet != null) {
            unexpectedWarnings = new HashSet<>(actualWarnings);
            unexpectedWarnings.removeAll(expectedWarningSet);
            missingWarnings = new HashSet<>(expectedWarningSet);
            missingWarnings.removeAll(actualWarnings);
        }

        boolean hasIssues = !unexpectedErrors.isEmpty() ||
            !missingErrors.isEmpty() ||
            !unexpectedWarnings.isEmpty() ||
            !missingWarnings.isEmpty();

        if (hasIssues) {
            log.error("Java validation result mismatch:");
            log.error("  Actual errors: {}", actualErrors);
            log.error("  Expected errors: {}", expectedErrorSet);
            log.error("  Actual warnings: {}", actualWarnings);
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
            "Java validation passed: {} errors, {} warnings",
            actualErrors.size(),
            actualWarnings.size()
        );
    }
}
