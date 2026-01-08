package org.eclipse.emf.ecore.cli;

import org.slf4j.Logger;
import hu.blackbelt.judo.cli.api.CliValidationException;
import hu.blackbelt.judo.cli.api.ModelValidator;

/**
 * Ecore validator implementation.
 * <p>
 * Implements {@link ModelValidator} from model-cli-api for type-safe validation.
 * <p>
 * Ecore models are the base metamodel and typically don't require validation
 * beyond what EMF provides inherently. This implementation provides a no-op
 * validator that always succeeds.
 */
public class EcoreValidatorImpl implements ModelValidator {

    @Override
    public String getModelType() {
        return "ecore";
    }

    @Override
    public void validate(Logger logger, Object model) throws CliValidationException {
        // Ecore models don't require custom validation - EMF handles basic validation
        logger.debug("Ecore model validation: no custom validation required");
    }
}
