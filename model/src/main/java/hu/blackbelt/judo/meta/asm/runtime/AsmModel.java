package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;

import hu.blackbelt.judo.meta.asm.support.AsmModelResourceSupport;

import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

public class AsmModel {
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

    public ResourceSet getResourceSet() {
        return asmModelResourceSupport.getResourceSet();
    }

    public static AsmModel loadAsmModel(LoadArguments loadArguments) throws IOException {
        AsmModelResourceSupport asmModelResourceSupport = loadArguments.asmModelResourceSupport.orElseGet(() -> AsmModelResourceSupport.asmModelResourceSupportBuilder().resourceSet(loadArguments.resourceSet.orElse(null)).rootUri(loadArguments.rootUri).uriHandler(loadArguments.uriHandler).build());
        AsmModelBuilder b = AsmModel.buildAsmModel();
        b.name(loadArguments.name).version(loadArguments.version.orElse("1.0.0")).uri(loadArguments.uri).checksum(loadArguments.checksum.orElse("NON-DEFINED")).asmModelResourceSupport(asmModelResourceSupport).metaVersionRange(loadArguments.acceptedMetaVersionRange.orElse("[0,9999)"));
        AsmModel asmModel = b.build();
        Resource resource = asmModel.getResourceSet().createResource(loadArguments.uri);
        resource.load(loadArguments.loadOptions);
        AsmModelResourceSupport.setupRelativeUriRoot(asmModel.getResourceSet(), loadArguments.uri);
        return asmModel;
    }

    public void saveAsmModel() throws IOException {
        getResourceSet().getResource(getUri(), false).save(AsmModelResourceSupport.getAsmModelDefaultSaveOptions());
    }

    public void saveAsmModel(SaveArguments saveArguments) throws IOException {
        try {
            OutputStream outputStream = saveArguments.outputStream.orElseGet(() -> saveArguments.file.map(f -> {
                try {
                    return new FileOutputStream(f);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).orElse(null));
            getResourceSet().getResource(getUri(), false).save(outputStream, saveArguments.saveOptions.orElseGet(() -> AsmModelResourceSupport.getAsmModelDefaultSaveOptions()));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw e;
            }
        }
    }


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
        Map<Object, Object> loadOptions;

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
        private static Map<Object, Object> $default$loadOptions() {
            return AsmModelResourceSupport.getAsmModelDefaultLoadOptions();
        }


        @java.lang.SuppressWarnings("all")
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
            private Map<Object, Object> loadOptions;

            @java.lang.SuppressWarnings("all")
            LoadArgumentsBuilder() {
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder uri(final URI uri) {
                this.uri = uri;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder name(final String name) {
                this.name = name;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder asmModelResourceSupport(final Optional<AsmModelResourceSupport> asmModelResourceSupport) {
                this.asmModelResourceSupport = asmModelResourceSupport;
                asmModelResourceSupport$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder rootUri(final Optional<URI> rootUri) {
                this.rootUri = rootUri;
                rootUri$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder uriHandler(final Optional<URIHandler> uriHandler) {
                this.uriHandler = uriHandler;
                uriHandler$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder resourceSet(final Optional<ResourceSet> resourceSet) {
                this.resourceSet = resourceSet;
                resourceSet$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder version(final Optional<String> version) {
                this.version = version;
                version$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder checksum(final Optional<String> checksum) {
                this.checksum = checksum;
                checksum$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder acceptedMetaVersionRange(final Optional<String> acceptedMetaVersionRange) {
                this.acceptedMetaVersionRange = acceptedMetaVersionRange;
                acceptedMetaVersionRange$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public LoadArgumentsBuilder loadOptions(final Map<Object, Object> loadOptions) {
                this.loadOptions = loadOptions;
                loadOptions$set = true;
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
                Map<Object, Object> loadOptions = this.loadOptions;
                if (!loadOptions$set) loadOptions = LoadArguments.$default$loadOptions();
                return new LoadArguments(uri, name, asmModelResourceSupport, rootUri, uriHandler, resourceSet, version, checksum, acceptedMetaVersionRange, loadOptions);
            }

            @java.lang.Override
            @java.lang.SuppressWarnings("all")
            public java.lang.String toString() {
                return "AsmModel.LoadArguments.LoadArgumentsBuilder(uri=" + this.uri + ", name=" + this.name + ", asmModelResourceSupport=" + this.asmModelResourceSupport + ", rootUri=" + this.rootUri + ", uriHandler=" + this.uriHandler + ", resourceSet=" + this.resourceSet + ", version=" + this.version + ", checksum=" + this.checksum + ", acceptedMetaVersionRange=" + this.acceptedMetaVersionRange + ", loadOptions=" + this.loadOptions + ")";
            }
        }

        @java.lang.SuppressWarnings("all")
        public static LoadArgumentsBuilder loadArgumentsBuilder() {
            return new LoadArgumentsBuilder();
        }

        @java.lang.SuppressWarnings("all")
        public LoadArguments(final URI uri, final String name, final Optional<AsmModelResourceSupport> asmModelResourceSupport, final Optional<URI> rootUri, final Optional<URIHandler> uriHandler, final Optional<ResourceSet> resourceSet, final Optional<String> version, final Optional<String> checksum, final Optional<String> acceptedMetaVersionRange, final Map<Object, Object> loadOptions) {
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
        }
    }


    public static class SaveArguments {
        Optional<OutputStream> outputStream;
        Optional<File> file;
        Optional<Map<Object, Object>> saveOptions;

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

            @java.lang.SuppressWarnings("all")
            SaveArgumentsBuilder() {
            }

            @java.lang.SuppressWarnings("all")
            public SaveArgumentsBuilder outputStream(final Optional<OutputStream> outputStream) {
                this.outputStream = outputStream;
                outputStream$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public SaveArgumentsBuilder file(final Optional<File> file) {
                this.file = file;
                file$set = true;
                return this;
            }

            @java.lang.SuppressWarnings("all")
            public SaveArgumentsBuilder saveOptions(final Optional<Map<Object, Object>> saveOptions) {
                this.saveOptions = saveOptions;
                saveOptions$set = true;
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
                return new SaveArguments(outputStream, file, saveOptions);
            }

            @java.lang.Override
            @java.lang.SuppressWarnings("all")
            public java.lang.String toString() {
                return "AsmModel.SaveArguments.SaveArgumentsBuilder(outputStream=" + this.outputStream + ", file=" + this.file + ", saveOptions=" + this.saveOptions + ")";
            }
        }

        @java.lang.SuppressWarnings("all")
        public static SaveArgumentsBuilder saveArgumentsBuilder() {
            return new SaveArgumentsBuilder();
        }

        @java.lang.SuppressWarnings("all")
        public SaveArguments(final Optional<OutputStream> outputStream, final Optional<File> file, final Optional<Map<Object, Object>> saveOptions) {
            this.outputStream = outputStream;
            this.file = file;
            this.saveOptions = saveOptions;
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class AsmModelBuilder {
        @java.lang.SuppressWarnings("all")
        private String name;
        @java.lang.SuppressWarnings("all")
        private String version;
        @java.lang.SuppressWarnings("all")
        private URI uri;
        @java.lang.SuppressWarnings("all")
        private String checksum;
        @java.lang.SuppressWarnings("all")
        private String metaVersionRange;
        @java.lang.SuppressWarnings("all")
        private AsmModelResourceSupport asmModelResourceSupport;

        @java.lang.SuppressWarnings("all")
        AsmModelBuilder() {
        }

        @java.lang.SuppressWarnings("all")
        public AsmModelBuilder name(final String name) {
            this.name = name;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AsmModelBuilder version(final String version) {
            this.version = version;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AsmModelBuilder uri(final URI uri) {
            this.uri = uri;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AsmModelBuilder checksum(final String checksum) {
            this.checksum = checksum;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AsmModelBuilder metaVersionRange(final String metaVersionRange) {
            this.metaVersionRange = metaVersionRange;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AsmModelBuilder asmModelResourceSupport(final AsmModelResourceSupport asmModelResourceSupport) {
            this.asmModelResourceSupport = asmModelResourceSupport;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AsmModel build() {
            return new AsmModel(name, version, uri, checksum, metaVersionRange, asmModelResourceSupport);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AsmModel.AsmModelBuilder(name=" + this.name + ", version=" + this.version + ", uri=" + this.uri + ", checksum=" + this.checksum + ", metaVersionRange=" + this.metaVersionRange + ", asmModelResourceSupport=" + this.asmModelResourceSupport + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AsmModelBuilder buildAsmModel() {
        return new AsmModelBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public AsmModel(final String name, final String version, final URI uri, final String checksum, final String metaVersionRange, final AsmModelResourceSupport asmModelResourceSupport) {
        this.name = name;
        this.version = version;
        this.uri = uri;
        this.checksum = checksum;
        this.metaVersionRange = metaVersionRange;
        this.asmModelResourceSupport = asmModelResourceSupport;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AsmModel(name=" + this.getName() + ", version=" + this.getVersion() + ", uri=" + this.getUri() + ", checksum=" + this.getChecksum() + ", metaVersionRange=" + this.getMetaVersionRange() + ", asmModelResourceSupport=" + this.getAsmModelResourceSupport() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public String getVersion() {
        return this.version;
    }

    @java.lang.SuppressWarnings("all")
    public URI getUri() {
        return this.uri;
    }

    @java.lang.SuppressWarnings("all")
    public String getChecksum() {
        return this.checksum;
    }

    @java.lang.SuppressWarnings("all")
    public String getMetaVersionRange() {
        return this.metaVersionRange;
    }

    @java.lang.SuppressWarnings("all")
    public AsmModelResourceSupport getAsmModelResourceSupport() {
        return this.asmModelResourceSupport;
    }
}
