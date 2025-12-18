package hu.blackbelt.judo.meta.asm.cli;

/*-
 * #%L
 * Judo :: Asm :: Model
 * %%
 * Copyright (C) 2018 - 2024 BlackBelt Technology
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

import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.slf4j.Logger;

import hu.blackbelt.judo.cli.api.ModelSchema;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;

/**
 * ASM model schema implementation.
 * <p>
 * Implements {@link ModelSchema} interface for cross-model GraphQL querying.
 * <p>
 * This is a MANUAL implementation because ASM extends EMF Ecore (basePackage="org.eclipse.emf")
 * rather than being a JUDO-specific metamodel. The generator skips non-JUDO packages,
 * so this schema must be manually maintained.
 * <p>
 * Unlike generated ModelSchemas, this implementation directly iterates over the ResourceSet
 * contents rather than using generated Operations classes, because ASM uses AsmModel while
 * the generated Operations expect EcoreModel.
 */
public class AsmModelSchema implements ModelSchema {

    private static final AsmModelSchema INSTANCE = new AsmModelSchema();
    private static final String MODEL_TYPE = "asm";

    // Resolver and validator instances
    private AsmFqnResolverImpl fqnResolver;
    private AsmValidatorImpl validator;

    // Reference to the loaded model
    private static AsmModel sharedModel;

    // Bound resource set
    private ResourceSet boundResourceSet;

    private AsmModelSchema() {}

    public static AsmModelSchema getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the shared model instance.
     */
    public static void setSharedModel(AsmModel model) {
        sharedModel = model;
    }

    /**
     * Gets the shared model instance.
     */
    public static AsmModel getSharedModel() {
        return sharedModel;
    }

    // =========================================================================
    // ModelSchema interface implementation
    // =========================================================================

    @Override
    public String getModelType() {
        return MODEL_TYPE;
    }

    @Override
    public boolean isAvailable() {
        return sharedModel != null;
    }

    @Override
    public boolean supportsMutations() {
        return false;
    }

    @Override
    public void bind(ResourceSet resourceSet) {
        this.boundResourceSet = resourceSet;
        if (fqnResolver == null) {
            fqnResolver = new AsmFqnResolverImpl();
        }
        fqnResolver.bind(resourceSet);
    }

    @Override
    public void unbind() {
        this.boundResourceSet = null;
        if (fqnResolver != null) {
            fqnResolver.unbind();
        }
    }

    @Override
    public boolean isBound() {
        return fqnResolver != null && fqnResolver.isBound();
    }

    @Override
    public Optional<EObject> resolve(String fqn) {
        if (fqnResolver == null) {
            return Optional.empty();
        }
        return fqnResolver.resolve(fqn);
    }

    @Override
    public Optional<String> getFqn(EObject eObject) {
        if (fqnResolver == null) {
            return Optional.empty();
        }
        return fqnResolver.getFqn(eObject);
    }

    @Override
    public Stream<String> getFqnCollection() {
        if (fqnResolver == null) {
            return Stream.empty();
        }
        return fqnResolver.getFqnCollection();
    }

    @Override
    public ValidationResult validate(Logger logger) {
        if (validator == null) {
            validator = new AsmValidatorImpl();
        }
        try {
            validator.validateModel(logger, sharedModel);
            return ValidationResult.success(MODEL_TYPE);
        } catch (Exception e) {
            return ValidationResult.failure(MODEL_TYPE, e.getMessage(), e);
        }
    }

    @Override
    public ResourceSet getResourceSet() {
        return boundResourceSet;
    }

    @Override
    public Resource getResource() {
        if (sharedModel != null && sharedModel.getResourceSet() != null) {
            ResourceSet rs = sharedModel.getResourceSet();
            if (!rs.getResources().isEmpty()) {
                return rs.getResources().get(0);
            }
        }
        return ModelSchema.super.getResource();
    }

    @Override
    public Stream<EPackage> getEPackages() {
        // ASM extends EMF Ecore, so we provide the Ecore package
        return Stream.of(EcorePackage.eINSTANCE);
    }

    // =========================================================================
    // Legacy methods for backwards compatibility
    // =========================================================================

    /**
     * Returns the FQN resolver instance.
     * @deprecated Use {@link #resolve(String)} instead
     */
    public Object getFqnResolverInstance() {
        if (fqnResolver == null) {
            fqnResolver = new AsmFqnResolverImpl();
        }
        return fqnResolver;
    }

    /**
     * Returns the validator instance.
     * @deprecated Use {@link #validate(Logger)} instead
     */
    public Object getValidatorInstance() {
        if (validator == null) {
            validator = new AsmValidatorImpl();
        }
        return validator;
    }

}
