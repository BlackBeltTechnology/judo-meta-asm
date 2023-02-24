package hu.blackbelt.judo.meta.asm.runtime;

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

import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;

import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport.setupRelativeUriRoot;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;


/**
 * A wrapper class on a Ecore metamodel based model. This wrapper organizing the model a structure which can be used
 * in Tatami as a logical model.
 * The logical model have version and loaded resources. The logical model can handle load and save from / to different
 * type of input / output source.
 *
 * Examples:
 *
 * Load an model from file.
 * <pre>
 *    AsmModel asmModel = AsmModel.loadAsmModel(asmLoadArgumentsBuilder()
 *                 .uri(URI.createFileURI(new File("src/test/model/test.ecore").getAbsolutePath()))
 *                 .build());
 *
 * </pre>
 *
 * More complex example, where model is loaded over an {@link URIHandler} in OSGi environment.
 *
 * <pre>
 *
 *    BundleURIHandler bundleURIHandler = new BundleURIHandler("urn", "", bundleContext.getBundle());
 *
 *    AsmModel asmModel = AsmModel.buildAsmModel()
 *                 .uri(URI.createURI("urn:test.ecore"))
 *                 .uriHandler(bundleURIHandler)
 * </pre>
 *
 * Create an empty ecore model
 * <pre>
 *    AsmModel asmModel = AsmModel.buildAsmModel()
 *                 .uri(URI.createFileURI("test.model"))
 *                 .build()
 * </pre>
 *
 */
public class AsmModel {

    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String URI = "uri";
    public static final String RESOURCESET = "resourceset";

    private URI uri;
    private AsmModelResourceSupport asmModelResourceSupport;

    /**
     * Return the model's name
     * @return model's name
     */
    public String getName() {
        return new hu.blackbelt.judo.meta.asm.runtime.AsmUtils(asmModelResourceSupport.getResourceSet()).getModel().orElseThrow(() -> new IllegalStateException("Could not get ASM model ePackage")).getName();
    }

    /**
     * Return the model's version
     * @return model's version
     */
    public String getVersion() {
        return java.util.Optional.ofNullable(new hu.blackbelt.judo.meta.asm.runtime.AsmUtils(asmModelResourceSupport.getResourceSet()).getVersion()).orElse("1.0.0");
    }

    /**
     * Return all properties as a {@link Dictionary}
     * @return
     */
    public Dictionary<String, Object> toDictionary() {
        Dictionary<String, Object> ret = new Hashtable<>();
        ret.put(NAME, getName());
        ret.put(VERSION, getVersion());
        ret.put(URI, uri);
        ret.put(RESOURCESET, asmModelResourceSupport.getResourceSet());
        return ret;
    }

    /**
     * Get the model's {@link AsmModelResourceSupport} resource helper itself.
     * @return instance of {@link AsmModelResourceSupport}
     */
    public AsmModelResourceSupport getAsmModelResourceSupport() {
        return asmModelResourceSupport;
    }

    /**
     * Get the model's isolated {@link ResourceSet}
     * @return instance of {@link ResourceSet}
     */
    public ResourceSet getResourceSet() {
        return asmModelResourceSupport.getResourceSet();
    }


    /**
     * Get the model's root resource which represents the mdoel's uri {@link URI} itself.
     * If the given resource does not exists new one is created.
     * @return instance of {@link Resource}
     */
    public Resource getResource() {
        if (getResourceSet().getResource(uri, false) == null) {
            getResourceSet().createResource(uri);
        }
        return getResourceSet().getResource(uri, false);
    }

    /**
     * Add content to the given model's root.
     * @param object Object to add to resource.
     * @return return this instance
     */
    @SuppressWarnings("UnusedReturnValue")
    public AsmModel addContent(EObject object) {
        getResource().getContents().add(object);
        return this;
    }

    /**
     * Load an model into {@link AsmModel} default {@link Resource}.
     * The {@link URI}, {@link URIHandler} and {@link ResourceSet} arguments are not used here, because it has
     * already set.
     * @param loadArgumentsBuilder {@link LoadArguments.LoadArgumentsBuilder} used for load.
     * @return this {@link AsmModelResourceSupport}
     * @throws IOException when IO error occured
     * @throws AsmValidationException when model validation is true and the model is invalid.
     */
    public AsmModel loadResource(LoadArguments.LoadArgumentsBuilder
                                           loadArgumentsBuilder)
            throws IOException, AsmValidationException {
        return loadResource(loadArgumentsBuilder.build());
    }

    /**
     * Load an model into {@link AsmModel} default {@link Resource}.
     * The {@link URI}, {@link URIHandler} and {@link ResourceSet} arguments are not used here, because it has
     * already set.
     * @param loadArguments {@link LoadArguments} used for load.
     * @return this {@link AsmModelResourceSupport}
     * @throws IOException when IO error occured
     * @throws AsmValidationException when model validation is true and the model is invalid.
     */
    @SuppressWarnings("WeakerAccess")
    public AsmModel loadResource(LoadArguments loadArguments)
            throws IOException, AsmValidationException {

        Resource resource = getResource();
        Map loadOptions = loadArguments.getLoadOptions()
                .orElseGet(AsmModelResourceSupport::getAsmModelDefaultLoadOptions);

        try {
            InputStream inputStream = loadArguments.getInputStream()
                    .orElseGet(() -> loadArguments.getFile().map(f -> {
                        try {
                            return new FileInputStream(f);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }).orElse(null));

            if (inputStream != null) {
                resource.load(inputStream, loadOptions);
            } else {
                resource.load(loadOptions);
            }

        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw e;
            }
        }

        if (loadArguments.isValidateModel() && !isValid()) {
            throw new AsmValidationException(this);
        }
        return this;
    }

    /**
     * Load an model. {@link LoadArguments.LoadArgumentsBuilder} contains all parameter
     * @param loadArgumentsBuilder {@link LoadArguments.LoadArgumentsBuilder} used for load
     * @return new {@link AsmModel} instance
     * @throws IOException when IO error occured
     * @throws AsmValidationException when model validation is true and the model is invalid.
     */
    public static AsmModel loadAsmModel(LoadArguments.LoadArgumentsBuilder loadArgumentsBuilder)
            throws IOException, AsmValidationException {
        return loadAsmModel(loadArgumentsBuilder.build());
    }

    /**
     * Load an model. {@link LoadArguments} contains all parameter
     * @param loadArguments {@link LoadArguments.LoadArgumentsBuilder} used for load
     * @return new {@link AsmModel} instance.
     * @throws IOException when IO error occured
     * @throws AsmValidationException when model validation is true and the model is invalid.
     */
    public static AsmModel loadAsmModel(LoadArguments loadArguments) throws IOException, AsmValidationException {
        try {
            AsmModelResourceSupport asmModelResourceSupport = AsmModelResourceSupport
                    .loadAsm(loadArguments.toAsmModelResourceSupportLoadArgumentsBuilder()
                            .validateModel(false));
            AsmModel asmModel = buildAsmModel()
                    .uri(loadArguments.getUri().orElseGet(() -> org.eclipse.emf.common.util.URI.createURI("asm.model")))
                    .asmModelResourceSupport(asmModelResourceSupport)
                    .build();

            setupRelativeUriRoot(asmModel.getResourceSet(), loadArguments.uri);

            if (loadArguments.validateModel && !asmModelResourceSupport.isValid()) {
                throw new AsmValidationException(asmModel);
            }
            return asmModel;

        } catch (AsmModelResourceSupport.AsmValidationException ignore) {
            throw new IllegalStateException("This exception generated because the code is broken");
        }
    }

    /**
     * Save the model to the given URI.
     * @throws IOException when IO error occurred
     * @throws AsmValidationException when model validation is true and the model is invalid.
     */
    public void saveAsmModel() throws IOException, AsmValidationException {
        saveAsmModel(SaveArguments.asmSaveArgumentsBuilder());
    }

    /**
     * Save the model as the given {@link SaveArguments.SaveArgumentsBuilder} defines
     * @param saveArgumentsBuilder the {@link SaveArguments.SaveArgumentsBuilder} used for save
     * @throws IOException when IO error occurred
     * @throws AsmValidationException when model validation is true and the model is invalid.
     */
    public void saveAsmModel(SaveArguments.SaveArgumentsBuilder saveArgumentsBuilder)
            throws IOException, AsmValidationException {
        saveAsmModel(saveArgumentsBuilder.build());
    }

    /**
     * Save the model as the given {@link SaveArguments} defines
     * @param saveArguments the {@link SaveArguments} used for save
     * @throws IOException when IO error occurred
     * @throws AsmValidationException when model validation is true and the model is invalid.
     */
    public void saveAsmModel(SaveArguments saveArguments) throws IOException, AsmValidationException {
        if (saveArguments.validateModel && !asmModelResourceSupport.isValid()) {
            throw new AsmValidationException(this);
        }
        try {
            asmModelResourceSupport.saveAsm(saveArguments.toAsmModelResourceSupportSaveArgumentsBuilder()
                    .validateModel(false));
        } catch (AsmModelResourceSupport.AsmValidationException e) {
            // Validation disaled, this exception cannot be thrown
        }
    }

    /**
     * Get distinct diagnostics for model. Only  {@link Diagnostic}.WARN and {@link Diagnostic}.ERROR are returns.
     * @return set of {@link Diagnostic}
     */
    public Set<Diagnostic> getDiagnostics() {
        return asmModelResourceSupport.getDiagnostics();
    }

    /**
     * Checks the model have any {@link Diagnostic}.ERROR diagnostics. When there is no any the model assumed as valid.
     * @return true when model is valid
     */
    public boolean isValid() {
        return asmModelResourceSupport.isValid();
    }

    /**
     * Print model as string
     * @return model as XML string
     */
    @SuppressWarnings("WeakerAccess")
    public String asString() {
        return asmModelResourceSupport.asString();
    }

    /**
     * Get diagnostics as a String
     * @return diagnostic list as string. Every line represents one diagnostic.
     */
    @SuppressWarnings("WeakerAccess")
    public String getDiagnosticsAsString() {
        return asmModelResourceSupport.getDiagnosticsAsString();
    }

    /**
     * This exception is thrown when validateModel is true on load or save and the model is not conform with its
     * defined metamodel.
     */
    @SuppressWarnings("WeakerAccess")
    public static class AsmValidationException extends Exception {
        AsmModel asmModel;

        public AsmValidationException(AsmModel asmModel) {
            super("Invalid model\n" + asmModel.getDiagnosticsAsString());
            this.asmModel = asmModel;
        }
    }

    /**
     * Arguments for {@link AsmModel#loadAsmModel(LoadArguments)}
     * It can handle variance of the presented arguments.
     */
    public static class LoadArguments {
        URI uri;
        URIHandler uriHandler;
        ResourceSet resourceSet;
        Map<Object, Object> loadOptions;
        boolean validateModel;
        InputStream inputStream;
        File file;

        private static URIHandler $default$uriHandler() {
            return null;
        }

        private static ResourceSet $default$resourceSet() {
            return null;
        }

        private static File $default$file() {
            return null;
        }

        private static InputStream $default$inputStream() {
            return null;
        }

        private static Map<Object, Object> $default$loadOptions() {
            return AsmModelResourceSupport.getAsmModelDefaultLoadOptions();
        }

        Optional<URI> getUri() {
            return ofNullable(uri);
        }

        Optional<URIHandler> getUriHandler() {
            return ofNullable(uriHandler);
        }

        Optional<ResourceSet> getResourceSet() {
            return ofNullable(resourceSet);
        }

        Optional<Map<Object, Object>> getLoadOptions() {
            return ofNullable(loadOptions);
        }

        boolean isValidateModel() {
            return validateModel;
        }

        Optional<File> getFile() {
            return ofNullable(file);
        }

        Optional<InputStream> getInputStream() {
            return ofNullable(inputStream);
        }

        /**
         * Builder for {@link AsmModel#loadAsmModel(LoadArguments)}.
         */
        public static class LoadArgumentsBuilder {
            private URI uri;

            private boolean uriHandler$set;
            private URIHandler uriHandler;

            private boolean resourceSet$set;
            private ResourceSet resourceSet;


            private boolean loadOptions$set;
            private Map<Object, Object> loadOptions;

            private boolean validateModel = true;

            private boolean file$set;
            private File file;


            private boolean inputStream$set;
            private InputStream inputStream;


            LoadArgumentsBuilder() {
            }

            /**
             * Defines the {@link URI} of the model.
             * This is mandatory.
             */
            public LoadArgumentsBuilder uri(final URI uri) {
                requireNonNull(uri);
                this.uri = uri;
                return this;
            }


            /**
             * Defines the {@link URIHandler} used for model IO. If not defined the default is EMF used.
             */
            public LoadArgumentsBuilder uriHandler(final URIHandler uriHandler) {
                requireNonNull(uriHandler);
                this.uriHandler = uriHandler;
                uriHandler$set = true;
                return this;
            }


            /**
             * Defines the default {@link ResourceSet}. If it is not defined the factory based resourceSet is used.
             */
            public LoadArgumentsBuilder resourceSet(final ResourceSet resourceSet) {
                requireNonNull(resourceSet);
                this.resourceSet = resourceSet;
                resourceSet$set = true;
                return this;
            }


            /**
             * Defines the load options for model. If not defined the
             * {@link AsmModelResourceSupport#getAsmModelDefaultLoadOptions()} us used.
             */
            public LoadArgumentsBuilder loadOptions(final Map<Object, Object> loadOptions) {
                requireNonNull(loadOptions);
                this.loadOptions = loadOptions;
                loadOptions$set = true;
                return this;
            }


            /**
             * Defines that model validation required or not on load. Default: true
             */
            public LoadArgumentsBuilder validateModel(boolean validateModel) {
                this.validateModel = validateModel;
                return this;
            }


            /**
             * Defines the file if it is not loaded from URI. If not defined, URI is used. If inputStream is defined
             * it is used.
             */
            public LoadArgumentsBuilder file(final File file) {
                requireNonNull(file);
                this.file = file;
                file$set = true;
                return this;
            }


            /**
             * Defines the file if it is not loaded from  File or URI. If not defined, File or URI is used.
             */
            public LoadArgumentsBuilder inputStream(final InputStream inputStream) {
                requireNonNull(inputStream);
                this.inputStream = inputStream;
                inputStream$set = true;
                return this;
            }

            public LoadArguments build() {
                URIHandler uriHandler = this.uriHandler;
                if (!uriHandler$set) uriHandler = LoadArguments.$default$uriHandler();
                ResourceSet resourceSet = this.resourceSet;
                if (!resourceSet$set) resourceSet = LoadArguments.$default$resourceSet();

                Map<Object, Object> loadOptions = this.loadOptions;
                if (!loadOptions$set) loadOptions = LoadArguments.$default$loadOptions();
                File file = this.file;
                if (!file$set) file = LoadArguments.$default$file();
                InputStream inputStream = this.inputStream;
                if (!inputStream$set) inputStream = LoadArguments.$default$inputStream();

                return new LoadArguments(
                        uri,
                        uriHandler,
                        resourceSet,
                        loadOptions,
                        validateModel,
                        file,
                        inputStream);
            }

            @java.lang.Override

            public java.lang.String toString() {
                return "AsmModel.LoadArguments.LoadArgumentsBuilder(uri=" + this.uri
                        + ", uriHandler=" + this.uriHandler
                        + ", resourceSet=" + this.resourceSet
                        + ", loadOptions=" + this.loadOptions
                        + ", validateModel=" + this.validateModel
                        + ", file=" + this.file
                        + ", inputStream=" + this.inputStream
                        + ")";
            }
        }


        public static LoadArgumentsBuilder asmLoadArgumentsBuilder() {
            return new LoadArgumentsBuilder();
        }


        private LoadArguments(final URI uri,
                              final URIHandler uriHandler,
                              final ResourceSet resourceSet,
                              final Map<Object, Object> loadOptions,
                              final boolean validateModel,
                              final File file,
                              final InputStream inputStream) {
            this.uri = uri;
            this.uriHandler = uriHandler;
            this.resourceSet = resourceSet;
            this.loadOptions = loadOptions;
            this.validateModel = validateModel;
            this.file = file;
            this.inputStream = inputStream;
        }


        AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder
        toAsmModelResourceSupportLoadArgumentsBuilder() {
            AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder argumentsBuilder =
                    AsmModelResourceSupport.LoadArguments.asmLoadArgumentsBuilder()
                            .uri(getUri()
                                    .orElseGet(() ->
                                            org.eclipse.emf.common.util.URI.createURI("asm.model")))
                            .validateModel(isValidateModel());

            getUriHandler().ifPresent(argumentsBuilder::uriHandler);
            getResourceSet().ifPresent(argumentsBuilder::resourceSet);
            getLoadOptions().ifPresent(argumentsBuilder::loadOptions);
            getFile().ifPresent(argumentsBuilder::file);
            getInputStream().ifPresent(argumentsBuilder::inputStream);

            return argumentsBuilder;
        }


    }


    /**
     * Arguments for {@link AsmModel#saveAsmModel(SaveArguments)}
     * It can handle variance of the presented arguments.
     */
    public static class SaveArguments {
        OutputStream outputStream;
        File file;
        Map<Object, Object> saveOptions;
        boolean validateModel;

        private static OutputStream $default$outputStream() {
            return null;
        }

        private static File $default$file() {
            return null;
        }

        private static Map<Object, Object> $default$saveOptions() {
            return null;
        }

        public Optional<OutputStream> getOutputStream() {
            return ofNullable(outputStream);
        }

        public Optional<File> getFile() {
            return ofNullable(file);
        }

        public Optional<Map<Object, Object>> getSaveOptions() {
            return ofNullable(saveOptions);
        }

        public boolean isValidateModel() {
            return validateModel;
        }

        /**
         * Builder for {@link AsmModel#saveAsmModel(SaveArguments)}.
         */
        public static class SaveArgumentsBuilder {

            private boolean outputStream$set;
            private OutputStream outputStream;

            private boolean file$set;
            private File file;

            private boolean saveOptions$set;
            private Map<Object, Object> saveOptions;

            private boolean validateModel = true;


            SaveArgumentsBuilder() {
            }


            public AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder
            toAsmModelResourceSupportSaveArgumentsBuilder() {
                AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder argumentsBuilder =
                        AsmModelResourceSupport.SaveArguments.asmSaveArgumentsBuilder().validateModel(validateModel);

                if (outputStream$set) argumentsBuilder.outputStream(outputStream);
                if (file$set) argumentsBuilder.file(file);
                if (saveOptions$set) argumentsBuilder.saveOptions(saveOptions);
                return argumentsBuilder;
            }


            /**
             * Defines {@link OutputStream} which is used by save. Whe it is not defined, file is used.
             */
            public SaveArgumentsBuilder outputStream(final OutputStream outputStream) {
                requireNonNull(outputStream);
                this.outputStream = outputStream;
                outputStream$set = true;
                return this;
            }


            /**
             * Defines {@link File} which is used by save. Whe it is not defined the model's
             * {@link AsmModel#uri is used}
             */
            public SaveArgumentsBuilder file(File file) {
                requireNonNull(file);
                this.file = file;
                file$set = true;
                return this;
            }


            /**
             * Defines save options. When it is not defined
             * {@link AsmModelResourceSupport#getAsmModelDefaultSaveOptions()} is used.
             */
            public SaveArgumentsBuilder saveOptions(final Map<Object, Object> saveOptions) {
                requireNonNull(saveOptions);
                this.saveOptions = saveOptions;
                saveOptions$set = true;
                return this;
            }

            /**
             * Defines that model validation required or not on save. Default: true
             */
            public SaveArgumentsBuilder validateModel(boolean validateModel) {
                this.validateModel = validateModel;
                return this;
            }


            public SaveArguments build() {
                OutputStream outputStream = this.outputStream;
                if (!outputStream$set) outputStream = SaveArguments.$default$outputStream();
                File file = this.file;
                if (!file$set) file = SaveArguments.$default$file();
                Map<Object, Object> saveOptions = this.saveOptions;
                if (!saveOptions$set) saveOptions = SaveArguments.$default$saveOptions();
                return new SaveArguments(outputStream, file, saveOptions, validateModel);
            }

            @java.lang.Override

            public java.lang.String toString() {
                return "AsmModel.SaveArguments.SaveArgumentsBuilder("
                        + "outputStream=" + this.outputStream
                        + ", file=" + this.file
                        + ", saveOptions=" + this.saveOptions
                        + ")";
            }
        }


        public static SaveArgumentsBuilder asmSaveArgumentsBuilder() {
            return new SaveArgumentsBuilder();
        }


        public AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder toAsmModelResourceSupportSaveArgumentsBuilder() {
            AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder argumentsBuilder =
                    AsmModelResourceSupport.SaveArguments.asmSaveArgumentsBuilder().validateModel(validateModel);

            getOutputStream().ifPresent(o -> argumentsBuilder.outputStream(o));
            getFile().ifPresent(o -> argumentsBuilder.file(o));
            getSaveOptions().ifPresent(o -> argumentsBuilder.saveOptions(o));
            return argumentsBuilder;
        }



        private SaveArguments(final OutputStream outputStream,
                              final File file,
                              final Map<Object, Object> saveOptions,
                              final boolean validateModel) {
            this.outputStream = outputStream;
            this.file = file;
            this.saveOptions = saveOptions;
            this.validateModel = validateModel;
        }
    }



    public static class AsmModelBuilder {

        private URI uri;


        private boolean asmModelResourceSupport$set;
        private AsmModelResourceSupport asmModelResourceSupport;

        private boolean resourceSet$set;
        private ResourceSet resourceSet;

        private URIHandler uriHandler;
        private boolean uriHandler$set;

        AsmModelBuilder() {
        }




        /**
         * Defines the uri {@link URI} of the model. Its mandatory.
         */
        public AsmModelBuilder uri(final URI uri) {
            this.uri = uri;
            return this;
        }


        public AsmModelBuilder asmModelResourceSupport(final AsmModelResourceSupport asmModelResourceSupport) {
            requireNonNull(asmModelResourceSupport);
            this.asmModelResourceSupport = asmModelResourceSupport;
            this.asmModelResourceSupport$set = true;
            return this;
        }

        public AsmModelBuilder resourceSet(final ResourceSet resourceSet) {
            requireNonNull(resourceSet);
            this.resourceSet = resourceSet;
            this.resourceSet$set = true;
            return this;
        }

        public AsmModelBuilder uriHandler(final URIHandler uriHandler) {
            requireNonNull(uriHandler);
            this.uriHandler = uriHandler;
            this.uriHandler$set = true;
            return this;
        }


        public AsmModel build() {
            org.eclipse.emf.common.util.URI uriPhysicalOrLogical = ofNullable(uri)
                    .orElseGet(() -> org.eclipse.emf.common.util.URI.createURI("asm.model"));


            AsmModelResourceSupport asmModelResourceSupport = this.asmModelResourceSupport;
            if (!asmModelResourceSupport$set) {
                AsmModelResourceSupport.AsmModelResourceSupportBuilder asmModelResourceSupportBuilder =
                        AsmModelResourceSupport.asmModelResourceSupportBuilder()
                                .uri(uriPhysicalOrLogical);

                if (resourceSet$set) asmModelResourceSupportBuilder.resourceSet(resourceSet);
                if (uriHandler$set) asmModelResourceSupportBuilder.uriHandler(uriHandler);

                asmModelResourceSupport = asmModelResourceSupportBuilder.build();
            } else {
                this.uri = asmModelResourceSupport.getResource().getURI();
            }


            return new AsmModel(
                    uriPhysicalOrLogical,
                    asmModelResourceSupport);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "AsmModel.AsmModelBuilder("
                    + "uri=" + this.uri
                    + ", asmModelResourceSupport=" + this.asmModelResourceSupport + ")";
        }
    }

    public static AsmModelBuilder buildAsmModel() {
        return new AsmModelBuilder();
    }

    private AsmModel(
            final URI uri,
            final AsmModelResourceSupport asmModelResourceSupport) {

        requireNonNull(uri, "URI is mandatory");
        this.uri = uri;
        this.asmModelResourceSupport = asmModelResourceSupport;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "AsmModel("
                + ", uri=" + this.getUri()
                + ", asmModelResourceSupport=" + this.asmModelResourceSupport + ")";
    }

    /**
     * Get the {@link URI} of the model.
     */
    public URI getUri() {
        return this.uri;
    }
}
