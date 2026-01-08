package hu.blackbelt.judo.meta.asm.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import hu.blackbelt.judo.cli.api.CliValidationException;
import hu.blackbelt.judo.cli.api.ModelValidator;
import hu.blackbelt.judo.cli.api.ValidationType;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.asm.runtime.AsmEpsilonValidator;
import hu.blackbelt.judo.meta.asm.validation.AsmValidator;

/**
 * ASM validator implementation.
 * <p>
 * Implements {@link ModelValidator} from model-cli-api for type-safe validation.
 * Supports both Epsilon (EVL) and Zeta (Java) validation engines.
 * <p>
 * ASM models are Ecore-based and generated from PSM.
 */
public class AsmValidatorImpl implements ModelValidator {

    @Override
    public String getModelType() {
        return "asm";
    }

    @Override
    public Set<ValidationType> getSupportedTypes() {
        return Set.of(ValidationType.EPSILON, ValidationType.ZETA, ValidationType.BOTH);
    }

    @Override
    public void validate(Logger logger, Object model) throws CliValidationException {
        // Default to EPSILON validation
        validate(logger, model, ValidationType.EPSILON);
    }

    @Override
    public void validate(Logger logger, Object model, ValidationType type) throws CliValidationException {
        if (!(model instanceof AsmModel)) {
            throw new IllegalArgumentException(
                "Expected AsmModel but got: " + (model == null ? "null" : model.getClass().getName()));
        }
        AsmModel asmModel = (AsmModel) model;
        List<CliValidationException> exceptions = new ArrayList<>();

        // Run EPSILON validation
        if (type == ValidationType.EPSILON || type == ValidationType.BOTH) {
            try {
                validateWithEpsilon(logger, asmModel);
            } catch (CliValidationException e) {
                exceptions.add(e);
            }
        }

        // Run ZETA validation
        if (type == ValidationType.ZETA || type == ValidationType.BOTH) {
            try {
                validateWithZeta(logger, asmModel);
            } catch (CliValidationException e) {
                exceptions.add(e);
            }
        }

        // If there were any exceptions, combine and throw
        if (!exceptions.isEmpty()) {
            if (exceptions.size() == 1) {
                throw exceptions.get(0);
            }
            // Combine all errors from multiple validators
            List<String> allErrors = new ArrayList<>();
            for (CliValidationException e : exceptions) {
                for (String error : e.getErrors()) {
                    String prefix = e.getValidationType() != null 
                        ? "[" + e.getValidationType().name() + "] " 
                        : "";
                    allErrors.add(prefix + error);
                }
            }
            throw new CliValidationException("asm", type, allErrors);
        }
    }

    private void validateWithEpsilon(Logger logger, AsmModel asmModel) throws CliValidationException {
        try {
            AsmEpsilonValidator.validateAsm(logger, asmModel,
                    AsmEpsilonValidator.calculateAsmValidationScriptURI());
        } catch (Exception e) {
            throw new CliValidationException("asm", ValidationType.EPSILON, 
                "Epsilon validation failed: " + e.getMessage(), e);
        }
    }

    private void validateWithZeta(Logger logger, AsmModel asmModel) throws CliValidationException {
        try {
            AsmValidator.validateAsm(logger, asmModel);
        } catch (AsmModel.AsmValidationException e) {
            throw new CliValidationException("asm", ValidationType.ZETA, e.getMessage(), e);
        }
    }
}
