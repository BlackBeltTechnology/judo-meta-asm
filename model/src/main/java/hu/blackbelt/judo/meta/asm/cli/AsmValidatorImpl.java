package hu.blackbelt.judo.meta.asm.cli;

import org.slf4j.Logger;

import hu.blackbelt.judo.cli.api.CliValidationException;
import hu.blackbelt.judo.cli.api.ModelValidator;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.asm.runtime.AsmEpsilonValidator;

/**
 * ASM validator implementation.
 * <p>
 * Implements {@link ModelValidator} from judo-cli-api for type-safe validation.
 * Uses Epsilon (EVL) validation.
 * <p>
 * ASM models are Ecore-based and generated from PSM.
 * <p>
 * Note: Zeta validation support was removed - see JNG-6354.
 */
public class AsmValidatorImpl implements ModelValidator {

    @Override
    public String getModelType() {
        return "asm";
    }

    @Override
    public void validate(Logger logger, Object model) throws CliValidationException {
        if (!(model instanceof AsmModel)) {
            throw new IllegalArgumentException(
                "Expected AsmModel but got: " + (model == null ? "null" : model.getClass().getName()));
        }
        AsmModel asmModel = (AsmModel) model;

        try {
            AsmEpsilonValidator.validateAsm(logger, asmModel,
                    AsmEpsilonValidator.calculateAsmValidationScriptURI());
        } catch (Exception e) {
            throw new CliValidationException("asm",
                "EVL validation failed: " + e.getMessage(), e);
        }
    }
}
