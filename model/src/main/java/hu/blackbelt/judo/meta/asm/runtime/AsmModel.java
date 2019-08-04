package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.DelegatingResourceLocator;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;

import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EObjectValidator;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A wrapper class on a ASM metamodel based model. This wrapper organizing the model a structure which can be used
 * in Tatami as a logical model.
 * The logical model have version and loaded resources. The logical model can handle load and save from / to different
 * type of input / output source.
 *
 * Examples:
 *
 * Load an model from file.
 * <pre>
 *    AsmModel asmModel = AsmModel.loadAsmModel(asmLoadArgumentsBuilder()
 *                 .uri(URI.createFileURI(new File("src/test/model/test.asm").getAbsolutePath()))
 *                 .name("test")
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
 *                 .name("test")
 *                 .version("1.0.0")
 *                 .uri(URI.createURI("urn:test.asm"))
 *                 .asmModelResourceSupport(
 *                         asmModelResourceSupportBuilder()
 *                                 .uriHandler(bundleURIHandler)
 *                                 .build())
 *                 .metaVersionRange(bundleContext.getBundle().getHeaders().get("[1.0,2))).build();
 * </pre>
 *
 * Create an empty asm model
 * <pre>
 *    AsmModel asmModel = AsmModel.buildAsmModel()
 *                 .name("test")
 *                 .uri(URI.createFileURI("test.model"))
 *                 .build()
 * </pre>
 *
 */
public class AsmModel {

    public static Diagnostician diagnostician = new Diagnostician();

    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String CHECKSUM = "checksum";
    public static final String META_VERSION_RANGE = "meta-version-range";
    public static final String URI = "uri";
    public static final String RESOURCESET = "resourceset";
    String name;
    String version;
    URI uri;
    String checksum;
    String metaVersionRange;
    AsmModelResourceSupport asmModelResourceSupport;

    /**
     * Return all properties as a {@link Dictionary}
     * @return
     */
    public Dictionary<String, Object> toDictionary() {
        Dictionary<String, Object> ret = new Hashtable<>();
        ret.put(NAME, name);
        ret.put(VERSION, version);
        ret.put(URI, uri);
        ret.put(CHECKSUM, checksum);
        ret.put(META_VERSION_RANGE, metaVersionRange);
        ret.put(RESOURCESET, asmModelResourceSupport.getResourceSet());
        return ret;
    }

    /**
     * Get the model's isolated {@link ResourceSet}
     * @return
     */
    public ResourceSet getResourceSet() {
        return asmModelResourceSupport.getResourceSet();
    }


    /**
     * Get the model's root resource which represents the mdoel's uri {@link URI} itself.
     * If the given resource does not exists new one is created.
     * @return
     */
    public Resource getResource() {
        if (getResourceSet().getResource(uri, false) == null) {
            getResourceSet().createResource(uri);
        }
        return getResourceSet().getResource(uri, false);
    }

    /**
     * Add content to the given model's root.
     * @return
     */
    public AsmModel addContent(EObject object) {
        getResource().getContents().add(object);
        return this;
    }

    /**
     * Load an model. {@link LoadArguments.LoadArgumentsBuilder} contains all parameter
     * @param loadArgumentsBuilder
     * @return
     * @throws IOException
     * @throws AsmValidationException
     */
    public static AsmModel loadAsmModel(LoadArguments.LoadArgumentsBuilder loadArgumentsBuilder) throws IOException, AsmValidationException {
        return loadAsmModel(loadArgumentsBuilder.build());
    }

    /**
     * Load an model. {@link LoadArguments} contains all parameter
     * @param loadArguments
     * @return
     * @throws IOException
     * @throws AsmValidationException
     */
    public static AsmModel loadAsmModel(LoadArguments loadArguments) throws IOException, AsmValidationException {
        AsmModelResourceSupport asmModelResourceSupport = loadArguments.asmModelResourceSupport
                .orElseGet(() -> AsmModelResourceSupport.asmModelResourceSupportBuilder()
                        .resourceSet(loadArguments.resourceSet.orElse(null))
                        .rootUri(loadArguments.rootUri)
                        .uriHandler(loadArguments.uriHandler)
                        .build());

        AsmModel asmModel = AsmModel.buildAsmModel()
                .name(loadArguments.name)
                .version(loadArguments.version.orElse("1.0.0"))
                .uri(loadArguments.uri)
                .checksum(loadArguments.checksum.orElse("NON-DEFINED"))
                .asmModelResourceSupport(asmModelResourceSupport)
                .metaVersionRange(loadArguments.acceptedMetaVersionRange.orElse("[0,9999)"))
                .build();

        AsmModelResourceSupport.setupRelativeUriRoot(asmModel.getResourceSet(), loadArguments.uri);
        Resource resource = asmModel.getResourceSet().createResource(loadArguments.uri);
        Map loadOptions = loadArguments.loadOptions
                .orElseGet(() -> AsmModelResourceSupport.getAsmModelDefaultLoadOptions());

        try {
            InputStream inputStream = loadArguments.inputStream.orElseGet(() -> loadArguments.file.map(f -> {
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

        if (loadArguments.validateModel && !asmModel.isValid()) {
            throw new AsmValidationException(asmModel);
        }
        return asmModel;
    }

    /**
     * Save the model to the given URI.
     * @throws IOException
     * @throws AsmValidationException
     */
    public void saveAsmModel() throws IOException, AsmValidationException {
        saveAsmModel(SaveArguments.asmSaveArgumentsBuilder());
    }

    /**
     * Save the model as the given {@link SaveArguments.SaveArgumentsBuilder} defines
     * @param saveArgumentsBuilder
     * @throws IOException
     * @throws AsmValidationException
     */
    public void saveAsmModel(SaveArguments.SaveArgumentsBuilder saveArgumentsBuilder) throws IOException, AsmValidationException {
        saveAsmModel(saveArgumentsBuilder.build());
    }

    /**
     * Save the model as the given {@link SaveArguments} defines
     * @param saveArguments
     * @throws IOException
     * @throws AsmValidationException
     */
    public void saveAsmModel(SaveArguments saveArguments) throws IOException, AsmValidationException {
        if (saveArguments.validateModel && !isValid()) {
            throw new AsmValidationException(this);
        }
        Map saveOptions = saveArguments.saveOptions
                .orElseGet(() -> AsmModelResourceSupport.getAsmModelDefaultSaveOptions());
        try {
            OutputStream outputStream = saveArguments.outputStream
                    .orElseGet(() -> saveArguments.file.map(f -> {
                try {
                    return new FileOutputStream(f);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).orElse(null));
            if (outputStream != null) {
                getResource().save(outputStream, saveOptions);
            } else {
                getResource().save(saveOptions);
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    private Diagnostic getDiagnostic(EObject eObject) {
        // TODO: The hack is called here
        fixEcoreUri();
        BasicDiagnostic diagnostics = new BasicDiagnostic
                (EObjectValidator.DIAGNOSTIC_SOURCE,
                        0,
                        String.format("Diagnosis of %s\n", new Object[] { diagnostician.getObjectLabel(eObject) }),
                        new Object [] { eObject });

        diagnostician.validate(eObject, diagnostics, diagnostician.createDefaultContext());
        return diagnostics;
    }

    private static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /**
     * Get distinct diagnostics for model. Only  {@link Diagnostic}.WARN and {@link Diagnostic}.ERROR are returns.
     * @return
     */
    public Set<Diagnostic> getDiagnostics() {
        return getAsmModelResourceSupport().all()
                .filter(EObject.class :: isInstance)
                .map(EObject.class :: cast)
                .map(e -> getDiagnostic(e))
                .filter(d -> d.getSeverity() > Diagnostic.INFO)
                .filter(d -> d.getChildren().size() > 0)
                .flatMap(d -> d.getChildren().stream())
                .filter(distinctByKey(e -> e.toString()))
                .collect(Collectors.toSet());
    }

    /**
     * Checks the model have any {@link Diagnostic}.ERROR diagnostics. When there is no any the model assumed as valid.
     * @return
     */
    public boolean isValid() {
        Set<Diagnostic> diagnostics = getDiagnostics();
        return !diagnostics.stream().filter(e -> e.getSeverity() >= Diagnostic.ERROR).findAny().isPresent();
    }

    /**
     * Print model as string
     * @return
     */
    public String asString() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            // Do not call save on model to bypass the validation
            getResource().save(byteArrayOutputStream, Collections.EMPTY_MAP);
        } catch (IOException e) {
        }
        return new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset());
    }

    /**
     * Get diagnostics as a String
     * @return
     */
    public String getDiagnosticsAsString() {
        return getDiagnostics().stream().map(d -> d.toString()).collect(Collectors.joining("\n"));
    }

    /**
     * This exception is thrown when validateModel is true on load or save and the model is not conform with its
     * defined metamodel.
     */
    public static class AsmValidationException extends Exception {
        AsmModel asmModel;

        public AsmValidationException(AsmModel asmModel) {
            super("Invalid model\n" +
                    asmModel.getDiagnosticsAsString() + "\n" + asmModel.asString()
            );
            this.asmModel = asmModel;
        }
    }

    /**
     * Arguments for {@link AsmModel#loadAsmModel(LoadArguments)}
     * It can handle variance of the presented arguments.
     */
    public static class LoadArguments {
        URI uri;
        String name;
        Optional<AsmModelResourceSupport> asmModelResourceSupport;
        Optional<URI> rootUri;
        Optional<URIHandler> uriHandler;
        Optional<ResourceSet> resourceSet;
        Optional<String> version;
        Optional<String> checksum;
        Optional<String> acceptedMetaVersionRange;
        Optional<Map<Object, Object>> loadOptions;
        boolean validateModel = true;
        Optional<InputStream> inputStream;
        Optional<File> file;

        @java.lang.SuppressWarnings("all")
        private static Optional<AsmModelResourceSupport> $default$asmModelResourceSupport() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<URI> $default$rootUri() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<URIHandler> $default$uriHandler() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<ResourceSet> $default$resourceSet() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<String> $default$version() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<String> $default$checksum() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<String> $default$acceptedMetaVersionRange() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<File> $default$file() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<InputStream> $default$inputStream() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<Map<Object, Object>> $default$loadOptions() {
            return Optional.of(AsmModelResourceSupport.getAsmModelDefaultLoadOptions());
        }


        @java.lang.SuppressWarnings("all")
        /**
         * Builder for {@link AsmModel#loadAsmModel(LoadArguments)}.
         */
        public static class LoadArgumentsBuilder {
            @java.lang.SuppressWarnings("all")
            private URI uri;
            @java.lang.SuppressWarnings("all")
            private String name;
            @java.lang.SuppressWarnings("all")
            private boolean asmModelResourceSupport$set;
            @java.lang.SuppressWarnings("all")
            private Optional<AsmModelResourceSupport> asmModelResourceSupport;
            @java.lang.SuppressWarnings("all")
            private boolean rootUri$set;
            @java.lang.SuppressWarnings("all")
            private Optional<URI> rootUri;
            @java.lang.SuppressWarnings("all")
            private boolean uriHandler$set;
            @java.lang.SuppressWarnings("all")
            private Optional<URIHandler> uriHandler;
            @java.lang.SuppressWarnings("all")
            private boolean resourceSet$set;
            @java.lang.SuppressWarnings("all")
            private Optional<ResourceSet> resourceSet;
            @java.lang.SuppressWarnings("all")
            private boolean version$set;
            @java.lang.SuppressWarnings("all")
            private Optional<String> version;
            @java.lang.SuppressWarnings("all")
            private boolean checksum$set;
            @java.lang.SuppressWarnings("all")
            private Optional<String> checksum;
            @java.lang.SuppressWarnings("all")
            private boolean acceptedMetaVersionRange$set;
            @java.lang.SuppressWarnings("all")
            private Optional<String> acceptedMetaVersionRange;
            @java.lang.SuppressWarnings("all")
            private boolean loadOptions$set;
            @java.lang.SuppressWarnings("all")
            private Optional<Map<Object, Object>> loadOptions;

            private boolean validateModel = true;

            @java.lang.SuppressWarnings("all")
            private boolean file$set;
            @java.lang.SuppressWarnings("all")
            private Optional<File> file;

            @java.lang.SuppressWarnings("all")
            private boolean inputStream$set;
            @java.lang.SuppressWarnings("all")
            private Optional<InputStream> inputStream;

            @java.lang.SuppressWarnings("all")
            LoadArgumentsBuilder() {
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the {@link URI} of the model.
             * This is mandatory.
             */
            public LoadArgumentsBuilder uri(final URI uri) {
                this.uri = uri;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the name of the model.
             * This is mandatory.
             */
            public LoadArgumentsBuilder name(final String name) {
                this.name = name;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the {@link AsmModelResourceSupport} for model.
             * If its not defined a default one is created based on default {@link ResourceSet}
             */
            public LoadArgumentsBuilder asmModelResourceSupport(final AsmModelResourceSupport asmModelResourceSupport) {
                this.asmModelResourceSupport = Optional.of(asmModelResourceSupport);
                asmModelResourceSupport$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the root uri {@link URI} for model.
             * If its not defined a default one is created based on the main resource.
             */
            public LoadArgumentsBuilder rootUri(final URI rootUri) {
                this.rootUri = Optional.of(rootUri);
                rootUri$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the {@link URIHandler} used for model IO. If not defined the default is EMF used.
             */
            public LoadArgumentsBuilder uriHandler(final URIHandler uriHandler) {
                this.uriHandler = Optional.of(uriHandler);
                uriHandler$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the default {@link ResourceSet}. If it is not defined the factory based resourceSet is used.
             */
            public LoadArgumentsBuilder resourceSet(final ResourceSet resourceSet) {
                this.resourceSet = Optional.of(resourceSet);
                resourceSet$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the model version. If its not defined the version will be 1.0.0
             */
            public LoadArgumentsBuilder version(final String version) {
                this.version = Optional.of(version);
                version$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the model checksum. If its not defined 'notused' is defined.
             */
            public LoadArgumentsBuilder checksum(final String checksum) {
                this.checksum = Optional.of(checksum);
                checksum$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the accepted version range of the meta model. If its not defined [1.0,2) is used.
             */
            public LoadArgumentsBuilder acceptedMetaVersionRange(final String acceptedMetaVersionRange) {
                this.acceptedMetaVersionRange = Optional.of(acceptedMetaVersionRange);
                acceptedMetaVersionRange$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the load options for model. If not defined the {@link AsmModel#getAsmModelDefaultLoadOptions()} us used.
             */
            public LoadArgumentsBuilder loadOptions(final Map<Object, Object> loadOptions) {
                this.loadOptions = Optional.of(loadOptions);
                loadOptions$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines that model validation required or not on load. Default: true
             */
            public LoadArgumentsBuilder validateModel(boolean validateModel) {
                this.validateModel = validateModel;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the file if it is not loaded from URI. If not defined, URI is used. If inputStream is defined
             * it is used.
             */
            public LoadArgumentsBuilder file(final File file) {
                this.file = Optional.of(file);
                file$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines the file if it is not loaded from  File or URI. If not defined, File or URI is used.
             */
            public LoadArgumentsBuilder inputStream(final InputStream inputStream) {
                this.inputStream = Optional.of(inputStream);
                inputStream$set = true;
                return this;
            }


            @java.lang.SuppressWarnings("all")
            public LoadArguments build() {
                Optional<AsmModelResourceSupport> asmModelResourceSupport = this.asmModelResourceSupport;
                if (!asmModelResourceSupport$set) asmModelResourceSupport = LoadArguments.$default$asmModelResourceSupport();
                Optional<URI> rootUri = this.rootUri;
                if (!rootUri$set) rootUri = LoadArguments.$default$rootUri();
                Optional<URIHandler> uriHandler = this.uriHandler;
                if (!uriHandler$set) uriHandler = LoadArguments.$default$uriHandler();
                Optional<ResourceSet> resourceSet = this.resourceSet;
                if (!resourceSet$set) resourceSet = LoadArguments.$default$resourceSet();
                Optional<String> version = this.version;
                if (!version$set) version = LoadArguments.$default$version();
                Optional<String> checksum = this.checksum;
                if (!checksum$set) checksum = LoadArguments.$default$checksum();
                Optional<String> acceptedMetaVersionRange = this.acceptedMetaVersionRange;
                if (!acceptedMetaVersionRange$set) acceptedMetaVersionRange = LoadArguments.$default$acceptedMetaVersionRange();
                Optional<Map<Object, Object>> loadOptions = this.loadOptions;
                if (!loadOptions$set) loadOptions = LoadArguments.$default$loadOptions();
                Optional<File> file = this.file;
                if (!file$set) file = LoadArguments.$default$file();
                Optional<InputStream> inputStream = this.inputStream;
                if (!inputStream$set) inputStream = LoadArguments.$default$inputStream();

                return new LoadArguments(uri, name, asmModelResourceSupport, rootUri, uriHandler, resourceSet, version,
                        checksum, acceptedMetaVersionRange, loadOptions, validateModel, file, inputStream);
            }

            @java.lang.Override
            @java.lang.SuppressWarnings("all")
            public java.lang.String toString() {
                return "AsmModel.LoadArguments.LoadArgumentsBuilder(uri=" + this.uri
                        + ", name=" + this.name
                        + ", asmModelResourceSupport=" + this.asmModelResourceSupport
                        + ", rootUri=" + this.rootUri
                        + ", uriHandler=" + this.uriHandler
                        + ", resourceSet=" + this.resourceSet
                        + ", version=" + this.version
                        + ", checksum=" + this.checksum
                        + ", acceptedMetaVersionRange=" + this.acceptedMetaVersionRange
                        + ", loadOptions=" + this.loadOptions
                        + ", validateModel=" + this.validateModel
                        + ", file=" + this.file
                        + ", inputStream=" + this.inputStream
                        + ")";
            }
        }

        @java.lang.SuppressWarnings("all")
        public static LoadArgumentsBuilder asmLoadArgumentsBuilder() {
            return new LoadArgumentsBuilder();
        }

        @java.lang.SuppressWarnings("all")
        private LoadArguments(final URI uri,
                              final String name,
                              final Optional<AsmModelResourceSupport> asmModelResourceSupport,
                              final Optional<URI> rootUri,
                              final Optional<URIHandler> uriHandler,
                              final Optional<ResourceSet> resourceSet,
                              final Optional<String> version,
                              final Optional<String> checksum,
                              final Optional<String> acceptedMetaVersionRange,
                              final Optional<Map<Object, Object>> loadOptions,
                              final boolean validateModel,
                              final Optional<File> file,
                              final Optional<InputStream> inputStream) {
            this.uri = uri;
            this.name = name;
            this.asmModelResourceSupport = asmModelResourceSupport;
            this.rootUri = rootUri;
            this.uriHandler = uriHandler;
            this.resourceSet = resourceSet;
            this.version = version;
            this.checksum = checksum;
            this.acceptedMetaVersionRange = acceptedMetaVersionRange;
            this.loadOptions = loadOptions;
            this.validateModel = validateModel;
            this.file = file;
            this.inputStream = inputStream;
        }
    }


    /**
     * Arguments for {@link AsmModel#saveAsmModel(SaveArguments)}
     * It can handle variance of the presented arguments.
     */
    public static class SaveArguments {
        Optional<OutputStream> outputStream;
        Optional<File> file;
        Optional<Map<Object, Object>> saveOptions;
        boolean validateModel = true;

        @java.lang.SuppressWarnings("all")
        private static Optional<OutputStream> $default$outputStream() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<File> $default$file() {
            return Optional.empty();
        }

        @java.lang.SuppressWarnings("all")
        private static Optional<Map<Object, Object>> $default$saveOptions() {
            return Optional.empty();
        }


        @java.lang.SuppressWarnings("all")
        /**
         * Builder for {@link AsmModel#saveAsmModel(SaveArguments)}.
         */
        public static class SaveArgumentsBuilder {
            @java.lang.SuppressWarnings("all")
            private boolean outputStream$set;
            @java.lang.SuppressWarnings("all")
            private Optional<OutputStream> outputStream;
            @java.lang.SuppressWarnings("all")
            private boolean file$set;
            @java.lang.SuppressWarnings("all")
            private Optional<File> file;
            @java.lang.SuppressWarnings("all")
            private boolean saveOptions$set;
            @java.lang.SuppressWarnings("all")
            private Optional<Map<Object, Object>> saveOptions;

            private boolean validateModel = true;

            @java.lang.SuppressWarnings("all")
            SaveArgumentsBuilder() {
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines {@link OutputStream} which is used by save. Whe it is not defined, file is used.
             */
            public SaveArgumentsBuilder outputStream(final OutputStream outputStream) {
                this.outputStream = Optional.of(outputStream);
                outputStream$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines {@link File} which is used by save. Whe it is not defined the model's {@link AsmModel#uri is used}
             */
            public SaveArgumentsBuilder file(File file) {
                this.file = Optional.of(file);
                file$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            /**
             * Defines save options. When it is not defined {@link AsmModelResourceSupport#getAsmModelDefaultSaveOptions()} is used.
             */
            public SaveArgumentsBuilder saveOptions(final Map<Object, Object> saveOptions) {
                this.saveOptions = Optional.of(saveOptions);
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

            @java.lang.SuppressWarnings("all")
            public SaveArguments build() {
                Optional<OutputStream> outputStream = this.outputStream;
                if (!outputStream$set) outputStream = SaveArguments.$default$outputStream();
                Optional<File> file = this.file;
                if (!file$set) file = SaveArguments.$default$file();
                Optional<Map<Object, Object>> saveOptions = this.saveOptions;
                if (!saveOptions$set) saveOptions = SaveArguments.$default$saveOptions();
                return new SaveArguments(outputStream, file, saveOptions, validateModel);
            }

            @java.lang.Override
            @java.lang.SuppressWarnings("all")
            public java.lang.String toString() {
                return "AsmModel.SaveArguments.SaveArgumentsBuilder(outputStream=" + this.outputStream + ", file=" + this.file + ", saveOptions=" + this.saveOptions + ")";
            }
        }

        @java.lang.SuppressWarnings("all")
        public static SaveArgumentsBuilder asmSaveArgumentsBuilder() {
            return new SaveArgumentsBuilder();
        }

        @java.lang.SuppressWarnings("all")
        private SaveArguments(final Optional<OutputStream> outputStream,
                              final Optional<File> file,
                              final Optional<Map<Object, Object>> saveOptions,
                              final boolean validateModel) {
            this.outputStream = outputStream;
            this.file = file;
            this.saveOptions = saveOptions;
            this.validateModel = validateModel;
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class AsmModelBuilder {
        @java.lang.SuppressWarnings("all")
        private String name;
        @java.lang.SuppressWarnings("all")
        private URI uri;

        @java.lang.SuppressWarnings("all")
        private boolean version$set;
        @java.lang.SuppressWarnings("all")
        private Optional<String> version;
        @java.lang.SuppressWarnings("all")
        private boolean checksum$set;
        @java.lang.SuppressWarnings("all")
        private Optional<String> checksum;

        @java.lang.SuppressWarnings("all")
        private boolean metaVersionRange$set;
        @java.lang.SuppressWarnings("all")
        private Optional<String> metaVersionRange;

        @java.lang.SuppressWarnings("all")
        private boolean asmModelResourceSupport$set;
        @java.lang.SuppressWarnings("all")
        private Optional<AsmModelResourceSupport> asmModelResourceSupport;

        @java.lang.SuppressWarnings("all")
        AsmModelBuilder() {
        }

        @java.lang.SuppressWarnings("all")
        /**
         * Defines name of the model. Its mandatory.
         */
        public AsmModelBuilder name(final String name) {
            this.name = name;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        /**
         * Defines the uri {@link URI} of the model. Its mandatory.
         */
        public AsmModelBuilder uri(final URI uri) {
            this.uri = uri;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        /**
         * Defines the version of the model. Its mandatory.
         */
        public AsmModelBuilder version(final String version) {
            this.version = Optional.of(version);
            version$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        /**
         * Defines the checksum of the model. Its mandatory.
         */
        public AsmModelBuilder checksum(final String checksum) {
            this.checksum = Optional.of(checksum);
            this.checksum$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        /**
         * Defines the version of the model.
         */
        public AsmModelBuilder metaVersionRange(final String metaVersionRange) {
            this.metaVersionRange = Optional.of(metaVersionRange);
            this.metaVersionRange$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AsmModelBuilder asmModelResourceSupport(final AsmModelResourceSupport asmModelResourceSupport) {
            this.asmModelResourceSupport = Optional.of(asmModelResourceSupport);
            this.asmModelResourceSupport$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AsmModel build() {

            Optional<AsmModelResourceSupport> asmModelResourceSupport = this.asmModelResourceSupport;
            if (!asmModelResourceSupport$set) asmModelResourceSupport = LoadArguments.$default$asmModelResourceSupport();
            Optional<String> version = this.version;
            if (!version$set) version = LoadArguments.$default$version();
            Optional<String> checksum = this.checksum;
            if (!checksum$set) checksum = LoadArguments.$default$checksum();
            Optional<String> metaVersionRange = this.metaVersionRange;
            if (!metaVersionRange$set) metaVersionRange = LoadArguments.$default$acceptedMetaVersionRange();

            return new AsmModel(name, version, uri, checksum, metaVersionRange, asmModelResourceSupport);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AsmModel.AsmModelBuilder(name=" + this.name
                    + ", version=" + this.version
                    + ", uri=" + this.uri
                    + ", checksum=" + this.checksum
                    + ", metaVersionRange=" + this.metaVersionRange
                    + ", asmModelResourceSupport=" + this.asmModelResourceSupport + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AsmModelBuilder buildAsmModel() {
        return new AsmModelBuilder();
    }


    @java.lang.SuppressWarnings("all")
    private static Optional<AsmModelResourceSupport> $default$asmModelResourceSupport() {
        return Optional.empty();
    }

    @java.lang.SuppressWarnings("all")
    private static Optional<String> $default$version() {
        return Optional.empty();
    }

    @java.lang.SuppressWarnings("all")
    private static Optional<String> $default$checksum() {
        return Optional.empty();
    }

    @java.lang.SuppressWarnings("all")
    private static Optional<String> $default$metaVersionRange() {
        return Optional.empty();
    }

    @java.lang.SuppressWarnings("all")
    private AsmModel(final String name,
                     final Optional<String> version,
                     final URI uri,
                     final Optional<String> checksum,
                     final Optional<String> metaVersionRange,
                     final Optional<AsmModelResourceSupport> asmModelResourceSupport) {
        this.name = name;
        this.version = version.orElse("1.0.0");
        this.uri = uri;
        this.checksum = checksum.orElse("notused");
        this.metaVersionRange = metaVersionRange.orElse("[1.0,2)");
        this.asmModelResourceSupport = asmModelResourceSupport
                .orElseGet(() -> AsmModelResourceSupport.asmModelResourceSupportBuilder()
                        .resourceSet(AsmModelResourceSupport.createAsmResourceSet()).build());
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AsmModel(name=" + this.getName()
                + ", version=" + this.getVersion()
                + ", uri=" + this.getUri()
                + ", checksum=" + this.getChecksum()
                + ", metaVersionRange=" + this.getMetaVersionRange()
                + ", asmModelResourceSupport=" + this.getAsmModelResourceSupport() + ")";
    }

    @java.lang.SuppressWarnings("all")
    /**
     * Get the name of the model.
     */
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    /**
     * Get the model version.
     */
    public String getVersion() {
        return this.version;
    }

    @java.lang.SuppressWarnings("all")
    /**
     * Get the {@link URI} of the model.
     */
    public URI getUri() {
        return this.uri;
    }

    @java.lang.SuppressWarnings("all")
    /**
     * Get the checksum of the model.
     */
    public String getChecksum() {
        return this.checksum;
    }

    @java.lang.SuppressWarnings("all")
    /**
     * Get the accepted range of meta model version.
     */
    public String getMetaVersionRange() {
        return this.metaVersionRange;
    }

    @java.lang.SuppressWarnings("all")
    /**
     * Get the {@link AsmModelResourceSupport} instance related this instance.
     */
    public AsmModelResourceSupport getAsmModelResourceSupport() {
        return this.asmModelResourceSupport;
    }

    // TODO: Create ticket on Eclipse. The proble is that the DelegatingResourceLocator
    // creating a baseURL which ends with two trailing slash and the ResourceBundle cannot open the files.
    // Ugly hack: The EcorePlugin baseUri has an extra trailing slash
    // on OSGi, so we try to fix it
    private static void fixEcoreUri() {
        try {
            URL baseUrl = EcorePlugin.INSTANCE.getBaseURL();
            if (baseUrl.toString().startsWith("bundle:") && baseUrl.toString().endsWith("//")) {
                URL fixedUrl = new URL(baseUrl.toString().substring(0, baseUrl.toString().length() - 1));
                Field myField = getField(DelegatingResourceLocator.class, "baseURL");
                myField.setAccessible(true);
                myField.set(EcorePlugin.INSTANCE, fixedUrl);
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }

    }

    private static Field getField(Class clazz, String fieldName)
            throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getField(superClass, fieldName);
            }
        }
    }

}
