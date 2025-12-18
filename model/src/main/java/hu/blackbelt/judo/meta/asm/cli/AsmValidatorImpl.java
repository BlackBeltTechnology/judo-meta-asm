package hu.blackbelt.judo.meta.asm.cli;

import org.slf4j.Logger;
import hu.blackbelt.judo.cli.api.ModelValidator;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.asm.runtime.AsmEpsilonValidator;

/**
 * ASM validator implementation.
 * <p>
 * Implements {@link ModelValidator} from model-cli-api for type-safe validation.
 * <p>
 * ASM models are Ecore-based and generated from PSM. Full validation
 * uses Epsilon (EVL) scripts.
 */
public class AsmValidatorImpl implements ModelValidator {

    /**
     * Returns the model type identifier.
     *
     * @return "asm"
     */
    public String getModelType() {
        return "asm";
    }

    @Override
    public void validate(Logger logger, Object model) throws Exception {
        if (!(model instanceof AsmModel)) {
            throw new IllegalArgumentException(
                "Expected AsmModel but got: " + (model == null ? "null" : model.getClass().getName()));
        }
        validateModel(logger, (AsmModel) model);
    }

    /**
     * Validates the ASM model directly (type-safe convenience method).
     *
     * @param logger   the logger for validation messages
     * @param asmModel the ASM model to validate
     * @throws AsmModel.AsmValidationException if validation fails
     */
    public void validateModel(Logger logger, AsmModel asmModel) throws AsmModel.AsmValidationException {
        try {
            AsmEpsilonValidator.validateAsm(logger, asmModel,
                    AsmEpsilonValidator.calculateAsmValidationScriptURI());
        } catch (Exception e) {
            throw new AsmModel.AsmValidationException(asmModel);
        }
    }
}
