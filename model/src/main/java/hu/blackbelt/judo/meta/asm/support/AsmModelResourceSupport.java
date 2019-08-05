package hu.blackbelt.judo.meta.asm.support;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.DelegatingResourceLocator;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.ContentHandler;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.URIHandlerImpl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * This class wraps EMF ResourceSet. This helps to manage URI handler and gives Java 8 stream api over it.
 * It can help handle the model / load save using builder pattern for parameter construction.
 * Examples:
 *
 * Load an model from file. The file URI is used as base URI.
 * <pre>
 *    AsmModelResourceSupport asmModel = AsmModelResourceSupport.loadAsm(
 *    		AsmModelResourceSupport.asmLoadArgumentsBuilder()
 *				.uri(URI.createFileURI(new File("src/test/model/test.asm").getAbsolutePath())));
 *
 * </pre>
 *
 * More complex example, where model is loaded over an {@link URIHandler} in OSGi environment.
 * The BundleURIHandler using the given path inside the bundle to resolve URI.
 * <pre>
 *
 *    BundleURIHandler bundleURIHandler = new BundleURIHandler("urn", "", bundleContext.getBundle());
 *
 *    AsmModelResourceSupport asmModel = AsmModelResourceSupport.loadAsm(
 *    		AsmModelResourceSupport.asmLoadArgumentsBuilder()
 *                 .uri(URI.createURI("urn:test.asm"))
 *                 .uriHandler(bundleURIHandler)
 *                 .build());
 * </pre>
 *
 * When we want to use {@link URI} as logical reference, so not use it for the resource loading,
 * the {@link File} or {@link InputStream} can be defined for load.
 * <pre>
 *
 *    AsmModelResourceSupport asmModel = AsmModelResourceSupport.loadAsm(
 *    		AsmModelResourceSupport.asmLoadArgumentsBuilder()
 *                 .uri(URI.createURI("urn:test.asm"))
 *                 .file(new File("path_to_model"));
 * </pre>
 *
 *
 * Create an empty asm model and load from {@link InputStream}. In that case when save called on model the
 * file will be created on the given URI.
 * <pre>
 *    AsmModelResourceSupport asmModel = AsmModelResourceSupport.asmModelResourceSupportBuilder()
 *                 .uri(URI.createFileURI("test.model"))
 *                 .build();
 *
 *    asmModel.loadResource(AsmModelResourceSupport.asmLoadArgumentsBuilder()
 *    			.inputStream(givenStream));
 *
 *    asmModel.save();
 * </pre>
 *
 */
public class AsmModelResourceSupport {

	private static Diagnostician diagnostician = new Diagnostician();

	private ResourceSet resourceSet;
	
	private URI uri;

	/**
	 * Create {@link Stream} from {@link Iterator}.
	 * @param sourceIterator the {@link Iterator} {@link Stream} made from
	 * @param <T> the generic type
	 * @return the {@link Stream} representation of {@link Iterator}
	 */
	public <T> Stream<T> asStream(Iterator<T> sourceIterator) {
	    return asStream(sourceIterator, false);
	}

	/**
	 * Create {@link Stream} from {@link Iterator}.
	 * @param sourceIterator the {@link Iterator} {@link Stream} made from
	 * @param parallel parallel execution
	 * @param <T> the generic type
	 * @return the {@link Stream} representation of {@link Iterator}
	 */
	@SuppressWarnings("WeakerAccess")
	public <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
	    Iterable<T> iterable = () -> sourceIterator;
	    return StreamSupport.stream(iterable.spliterator(), parallel);
	}

	/**
	 * Get all content of the {@link ResourceSet}
	 * @param <T> the generic type
	 * @return the {@link Stream} representation of {@link ResourceSet} contents
	 */
	@SuppressWarnings("unchecked")
	public <T> Stream<T> all() {
		return asStream((Iterator<T>) resourceSet.getAllContents(), false);
	}

	/**
	 * Get the given class from {@link ResourceSet}
	 * @param clazz The {@link Class} which required
	 * @param <T> the generic type
	 * @return the {@link Stream} representation of {@link Class} type from {@link ResourceSet} contents
	 */
	@SuppressWarnings({"WeakerAccess", "NullableProblems", "unchecked"})
	public <T> Stream<T> getStreamOf(final Class<T> clazz) {
	    final Iterable<Notifier> contents = resourceSet::getAllContents;
	    return StreamSupport.stream(contents.spliterator(), false)
	            .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
	}

	public Stream<org.eclipse.emf.ecore.EAttribute> getStreamOfEcoreEAttribute() {
		return getStreamOf(org.eclipse.emf.ecore.EAttribute.class);
	}
	public Stream<org.eclipse.emf.ecore.EAnnotation> getStreamOfEcoreEAnnotation() {
		return getStreamOf(org.eclipse.emf.ecore.EAnnotation.class);
	}
	public Stream<org.eclipse.emf.ecore.EClass> getStreamOfEcoreEClass() {
		return getStreamOf(org.eclipse.emf.ecore.EClass.class);
	}
	public Stream<org.eclipse.emf.ecore.EClassifier> getStreamOfEcoreEClassifier() {
		return getStreamOf(org.eclipse.emf.ecore.EClassifier.class);
	}
	public Stream<org.eclipse.emf.ecore.EDataType> getStreamOfEcoreEDataType() {
		return getStreamOf(org.eclipse.emf.ecore.EDataType.class);
	}
	public Stream<org.eclipse.emf.ecore.EEnum> getStreamOfEcoreEEnum() {
		return getStreamOf(org.eclipse.emf.ecore.EEnum.class);
	}
	public Stream<org.eclipse.emf.ecore.EEnumLiteral> getStreamOfEcoreEEnumLiteral() {
		return getStreamOf(org.eclipse.emf.ecore.EEnumLiteral.class);
	}
	public Stream<org.eclipse.emf.ecore.EFactory> getStreamOfEcoreEFactory() {
		return getStreamOf(org.eclipse.emf.ecore.EFactory.class);
	}
	public Stream<org.eclipse.emf.ecore.EModelElement> getStreamOfEcoreEModelElement() {
		return getStreamOf(org.eclipse.emf.ecore.EModelElement.class);
	}
	public Stream<org.eclipse.emf.ecore.ENamedElement> getStreamOfEcoreENamedElement() {
		return getStreamOf(org.eclipse.emf.ecore.ENamedElement.class);
	}
	public Stream<org.eclipse.emf.ecore.EObject> getStreamOfEcoreEObject() {
		return getStreamOf(org.eclipse.emf.ecore.EObject.class);
	}
	public Stream<org.eclipse.emf.ecore.EOperation> getStreamOfEcoreEOperation() {
		return getStreamOf(org.eclipse.emf.ecore.EOperation.class);
	}
	public Stream<org.eclipse.emf.ecore.EPackage> getStreamOfEcoreEPackage() {
		return getStreamOf(org.eclipse.emf.ecore.EPackage.class);
	}
	public Stream<org.eclipse.emf.ecore.EParameter> getStreamOfEcoreEParameter() {
		return getStreamOf(org.eclipse.emf.ecore.EParameter.class);
	}
	public Stream<org.eclipse.emf.ecore.EReference> getStreamOfEcoreEReference() {
		return getStreamOf(org.eclipse.emf.ecore.EReference.class);
	}
	public Stream<org.eclipse.emf.ecore.EStructuralFeature> getStreamOfEcoreEStructuralFeature() {
		return getStreamOf(org.eclipse.emf.ecore.EStructuralFeature.class);
	}
	public Stream<org.eclipse.emf.ecore.ETypedElement> getStreamOfEcoreETypedElement() {
		return getStreamOf(org.eclipse.emf.ecore.ETypedElement.class);
	}
	public Stream<org.eclipse.emf.ecore.EGenericType> getStreamOfEcoreEGenericType() {
		return getStreamOf(org.eclipse.emf.ecore.EGenericType.class);
	}
	public Stream<org.eclipse.emf.ecore.ETypeParameter> getStreamOfEcoreETypeParameter() {
		return getStreamOf(org.eclipse.emf.ecore.ETypeParameter.class);
	}

	/**
	 * Set the relative {@link URI} to given {@link URI} and {@link ResourceSet}.
	 * Means there is no baseUri the rootUri is used.
	 * @param resourceSet {@link ResourceSet} is used
	 * @param rootUri {{@link URI}} which us used as root
	 */
	public static void setupRelativeUriRoot(ResourceSet resourceSet, URI rootUri) {
	    EList<URIHandler> uriHandlers = resourceSet.getURIConverter().getURIHandlers();
	    EList<ContentHandler> contentHandlers = resourceSet.getURIConverter().getContentHandlers();
	
	    // Set custom URI handler where URL without base part replaced with the base URI
	    resourceSet.setURIConverter(new ExtensibleURIConverterImpl() {
	        @Override
	        public URI normalize(URI uriPar) {
	
	            String fragment = uriPar.fragment();
	            String query = uriPar.query();
	            URI trimmedURI = uriPar.trimFragment().trimQuery();
	            URI result = getInternalURIMap().getURI(trimmedURI);
	            String scheme = result.scheme();
	            if (scheme == null) {
	                result = rootUri;
	            }
	
	            if (result == trimmedURI) {
	                return uriPar;
	            }
	
	            if (query != null) {
	                result = result.appendQuery(query);
	            }
	            if (fragment != null) {
	                result = result.appendFragment(fragment);
	            }
	            return normalize(result);
	        }
	    });
	
	    resourceSet.getURIConverter().getURIHandlers().clear();
	    resourceSet.getURIConverter().getURIHandlers().addAll(uriHandlers);
	    resourceSet.getURIConverter().getContentHandlers().clear();
	    resourceSet.getURIConverter().getContentHandlers().addAll(contentHandlers);
	}

	/**
	 * Set the relative {@link URI} to root {@link URI} in the {@link ResourceSet}.
	 * Means there is no baseUri the rootUri is used.
	 */
	public void setupRelativeUriRoot() {
		setupRelativeUriRoot(getResourceSet(), uri);
	}

	/**
	 * Setup the given {@link URIHandler} as the first {@link URIHandler} in the {@link ResourceSet}
	 * @param resourceSet {@link ResourceSet} which applied
	 * @param uriHandler {@link URIHandler} which is set as primary
	 */
	@SuppressWarnings("WeakerAccess")
	public static void setPrimaryUriHandler(ResourceSet resourceSet, URIHandler uriHandler) {
	    resourceSet.getURIConverter().getURIHandlers().add(0, uriHandler);
	}


	/**
	 * Create a {@link ResourceSet} and register all {@link Resource.Factory} belongs to Asm metamodel.
	 * @return the created {@link ResourceSet}
	 */
	public static ResourceSet createAsmResourceSet() {
	    ResourceSet resourceSet = new ResourceSetImpl();
	    registerAsmMetamodel(resourceSet);
	    resourceSet.getResourceFactoryRegistry()
				.getExtensionToFactoryMap()
				.put(ResourceFactoryRegistryImpl.DEFAULT_EXTENSION, getAsmFactory());
	    return resourceSet;
	}

	/**
	 * Get sensibe default for model loading options.
	 * @return Map of options
	 */
	public static Map<Object, Object> getAsmModelDefaultLoadOptions() {
	    Map<Object, Object> loadOptions = new HashMap<>();
	    //loadOptions.put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
	    //loadOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
	    loadOptions.put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
	    loadOptions.put(XMLResource.OPTION_LAX_FEATURE_PROCESSING, Boolean.TRUE);
	    loadOptions.put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
	    return loadOptions;
	}

	/**
	 * Get sensibe default for model saving options.
	 * @return Map of options
	 */
	public static Map<Object, Object> getAsmModelDefaultSaveOptions() {
	    Map<Object, Object> saveOptions = new HashMap<>();
	    saveOptions.put(XMLResource.OPTION_DECLARE_XML, Boolean.TRUE);
	    saveOptions.put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
	    saveOptions.put(XMLResource.OPTION_URI_HANDLER, new URIHandlerImpl() {
	        public URI deresolve(URI uri) {
	            return uri.hasFragment()
						&& uri.hasOpaquePart()
						&& this.baseURI.hasOpaquePart()
						&& uri.opaquePart().equals(this.baseURI.opaquePart())
							? URI.createURI("#" + uri.fragment())
							: super.deresolve(uri);
	        }
	    });
	    saveOptions.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
	    saveOptions.put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
	    saveOptions.put(XMLResource.OPTION_SKIP_ESCAPE_URI, Boolean.FALSE);
	    saveOptions.put(XMLResource.OPTION_ENCODING, "UTF-8");
	    return saveOptions;
	}

	/**
	 * Create default {@link Resource.Factory} for the Asm model.
	 * @return the created {@link Resource.Factory}
	 */
	@SuppressWarnings("WeakerAccess")
	public static Resource.Factory getAsmFactory() {
	    return new EcoreResourceFactoryImpl() {
	        @Override
	        public Resource createResource(URI uri) {
	            return new XMIResourceImpl(uri) {
	                @Override
	                protected boolean useUUIDs() {
	                    return true;
	                }
	            };
	        }
	    };
	}

	/**
	 * Register namespaces for Asm metamodel.
	 * @param resourceSet Register Asm packages for the given {@link ResourceSet}
	 */
	@SuppressWarnings("WeakerAccess")
	public static void registerAsmMetamodel(ResourceSet resourceSet) {
		resourceSet.getPackageRegistry().put(EcorePackage.eINSTANCE.getNsURI(), EcorePackage.eINSTANCE);
	}

    // Builder specific code
    private static ResourceSet $default$resourceSet() {
        return createAsmResourceSet();
    }

    private static URIHandler $default$uriHandler() {
        return null;
    }

    private AsmModelResourceSupport(final ResourceSet resourceSet,
									final URIHandler uriHandler,
									final URI uri) {
        this.resourceSet = resourceSet;
        this.uri = uri;

		if (uri == null && resourceSet != null) {
			if (resourceSet.getResources().size() == 0) {
				throw new IllegalStateException("URI is not defined and the given ResourceSet is empty. " +
						"At least one resource can be presented.");
			}
			this.uri = resourceSet.getResources().get(0).getURI();
		}

		if (uriHandler != null) {
        	setPrimaryUriHandler(resourceSet, uriHandler);
		}
        setupRelativeUriRoot();
    }

	/**
	 * Builder for {@link AsmModelResourceSupport}
	 */
    public static class AsmModelResourceSupportBuilder {
        private ResourceSet resourceSet;
        private URIHandler uriHandler;
        private URI uri;

        AsmModelResourceSupportBuilder() {
        }

        public AsmModelResourceSupportBuilder resourceSet(final ResourceSet resourceSet) {
            this.resourceSet = resourceSet;
            return this;
        }

		public AsmModelResourceSupportBuilder uriHandler(final URIHandler uriHandler) {
			this.uriHandler = uriHandler;
			return this;
		}

        public AsmModelResourceSupportBuilder uri(final URI uri) {
            this.uri = uri;
            return this;
        }

        public AsmModelResourceSupport build() {
			if (uri == null && resourceSet == null) {
				throw new NullPointerException("URI or ResourceSet have to be defined");
			}
			return new AsmModelResourceSupport(
					resourceSet != null ? resourceSet : $default$resourceSet(),
                    uriHandler != null ? uriHandler : $default$uriHandler(),
					uri);
        }

        @Override
        public java.lang.String toString() {
            return "AsmModelResourceSupportBuilder(resourceSet=" + this.resourceSet
					+ ", uriHandler=" + this.uriHandler
					+ ", uri=" + this.uri + ")";
        }
    }

	/**
	 * Construct a {@link AsmModelResourceSupportBuilder} to build {@link AsmModelResourceSupport}.
	 * @return instance of {@link AsmModelResourceSupportBuilder}
	 */
	public static AsmModelResourceSupportBuilder asmModelResourceSupportBuilder() {
        return new AsmModelResourceSupportBuilder();
    }

	/**
	 * Get the resourceSet this helper based on.
	 * @return instance of {@link ResourceSet}
	 */
    public ResourceSet getResourceSet() {
        return this.resourceSet;
    }

	/**
	 * Get the model's root resource which represents the model's uri {@link URI} itself.
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
	 * @return this {@link AsmModelResourceSupport}
	 */
	@SuppressWarnings("UnusedReturnValue")
	public AsmModelResourceSupport addContent(EObject object) {
		getResource().getContents().add(object);
		return this;
	}

	/**
	 * Load an model into {@link AsmModelResourceSupport} default {@link Resource}.
	 * The {@link URI}, {@link URIHandler} and {@link ResourceSet} arguments are not used here, because it has
	 * already set.
	 * @param loadArgumentsBuilder {@link LoadArguments.LoadArgumentsBuilder} used for load.
	 * @return this {@link AsmModelResourceSupport}
	 * @throws IOException when IO error occured
	 * @throws AsmValidationException when model validation is true and the model is invalid.
	 */
	public AsmModelResourceSupport loadResource(AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder
														loadArgumentsBuilder)
			throws IOException, AsmValidationException {
		return loadResource(loadArgumentsBuilder.build());
	}

	/**
	 * Load an model into {@link AsmModelResourceSupport} default {@link Resource}.
	 * The {@link URI}, {@link URIHandler} and {@link ResourceSet} arguments are not used here, because it has
	 * already set.
	 * @param loadArguments {@link LoadArguments} used for load.
	 * @return this {@link AsmModelResourceSupport}
	 * @throws IOException when IO error occured
	 * @throws AsmValidationException when model validation is true and the model is invalid.
	 */
	@SuppressWarnings("WeakerAccess")
	public AsmModelResourceSupport loadResource(AsmModelResourceSupport.LoadArguments
												loadArguments)
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
			throw new AsmModelResourceSupport.AsmValidationException(this);
		}
		return this;
	}


	/**
	 * Load an model. {@link AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder} contains all parameter
	 * @param loadArgumentsBuilder {@link LoadArguments.LoadArgumentsBuilder} used for load.
	 * @return created instance of {@link AsmModelResourceSupport}
	 * @throws IOException when IO error occured
	 * @throws AsmValidationException when model validation is true and the model is invalid.
	 */
	public static AsmModelResourceSupport loadAsm(AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder
														  loadArgumentsBuilder)
			throws IOException, AsmModelResourceSupport.AsmValidationException {
		return loadAsm(loadArgumentsBuilder.build());
	}

	/**
	 * Load an model. {@link AsmModelResourceSupport.LoadArguments} contains all parameter
	 * @param loadArguments {@link LoadArguments} used for load.
	 * @return created instance of {@link AsmModelResourceSupport}
	 * @throws IOException when IO error occured
	 * @throws AsmValidationException when model validation is true and the model is invalid.
	 */
	public static AsmModelResourceSupport loadAsm(AsmModelResourceSupport.LoadArguments loadArguments)
			throws IOException, AsmModelResourceSupport.AsmValidationException {

		AsmModelResourceSupport asmModelResourceSupport = asmModelResourceSupportBuilder()
						.resourceSet(loadArguments.getResourceSet()
								.orElseGet(AsmModelResourceSupport::createAsmResourceSet))
						.uri(loadArguments.getUri()
								.orElseThrow(() -> new IllegalArgumentException("URI must be set")))
						.uriHandler(loadArguments.getUriHandler()
								.orElse(null))
						.build();

		asmModelResourceSupport.loadResource(loadArguments);
		if (loadArguments.isValidateModel() && !asmModelResourceSupport.isValid()) {
			throw new AsmModelResourceSupport.AsmValidationException(asmModelResourceSupport);
		}
		return asmModelResourceSupport;
	}

	/**
	 * Save the model to the given URI.
	 * @throws IOException when IO error occured
	 * @throws AsmValidationException when model validation is true and the model is invalid.
	 */
	public void saveAsm() throws IOException, AsmModelResourceSupport.AsmValidationException {
		saveAsm(AsmModelResourceSupport.SaveArguments.asmSaveArgumentsBuilder());
	}

	/**
	 * Save the model as the given {@link SaveArguments.SaveArgumentsBuilder} defines
	 * @param saveArgumentsBuilder {@link SaveArguments.SaveArgumentsBuilder} used for save
	 * @throws IOException when IO error occured
	 * @throws AsmValidationException when model validation is true and the model is invalid.
	 */
	public void saveAsm(AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder saveArgumentsBuilder)
			throws IOException, AsmModelResourceSupport.AsmValidationException {
		saveAsm(saveArgumentsBuilder.build());
	}

	/**
	 * Save the model as the given {@link AsmModelResourceSupport.SaveArguments} defines
	 * @param saveArguments {@link SaveArguments} used for save
	 * @throws IOException when IO error occured
	 * @throws AsmValidationException when model validation is true and the model is invalid.
	 */
	@SuppressWarnings("WeakerAccess")
	public void saveAsm(AsmModelResourceSupport.SaveArguments saveArguments)
			throws IOException, AsmModelResourceSupport.AsmValidationException {
		if (saveArguments.isValidateModel() && !isValid()) {
			throw new AsmModelResourceSupport.AsmValidationException(this);
		}
		Map saveOptions = saveArguments.getSaveOptions()
				.orElseGet(AsmModelResourceSupport::getAsmModelDefaultSaveOptions);
		try {
			OutputStream outputStream = saveArguments.getOutputStream()
					.orElseGet(() -> saveArguments.getFile().map(f -> {
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
						String.format("Diagnosis of %s\n", diagnostician.getObjectLabel(eObject)),
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
	 * @return set of {@link Diagnostic}
	 */
	public Set<Diagnostic> getDiagnostics() {
		return all()
				.filter(EObject.class :: isInstance)
				.map(EObject.class :: cast)
				.map(this :: getDiagnostic)
				.filter(d -> d.getSeverity() > Diagnostic.INFO)
				.filter(d -> d.getChildren().size() > 0)
				.flatMap(d -> d.getChildren().stream())
				.filter(distinctByKey(Object::toString))
				.collect(Collectors.toSet());
	}

	/**
	 * Checks the model have any {@link Diagnostic}.ERROR diagnostics. When there is no any the model assumed as valid.
	 * @return true when model is valid
	 */
	public boolean isValid() {
		Set<Diagnostic> diagnostics = getDiagnostics();
		return diagnostics.stream().noneMatch(e -> e.getSeverity() >= Diagnostic.ERROR);
	}

	/**
	 * Print model as string
	 * @return model as XML string
	 */
	public String asString() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			// Do not call save on model to bypass the validation
			getResource().save(byteArrayOutputStream, Collections.EMPTY_MAP);
		} catch (IOException ignored) {
		}
		return new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset());
	}

	/**
	 * Get diagnostics as a String
	 * @return diagnostic list as string. Every line represents one diagnostic.
	 */
	public String getDiagnosticsAsString() {
		return getDiagnostics().stream().map(Object::toString).collect(Collectors.joining("\n"));
	}

	/**
	 * Arguments for {@link AsmModelResourceSupport#loadAsm(AsmModelResourceSupport.LoadArguments)}
	 * It can handle variance of the presented arguments.
	 */
	public static class LoadArguments {
		private URI uri;
		private URIHandler uriHandler;
		private ResourceSet resourceSet;
		private Map<Object, Object> loadOptions;
		private boolean validateModel;
		private InputStream inputStream;
		private File file;

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

		Optional<InputStream> getInputStream() {
			return ofNullable(inputStream);
		}

		Optional<File> getFile() {
			return ofNullable(file);
		}



		@java.lang.SuppressWarnings("all")
		/**
		 * Builder for {@link AsmModelResourceSupport#loadAsmModel(AsmModelResourceSupport.LoadArguments)}.
		 */
		public static class LoadArgumentsBuilder {
			private URI uri;
			private boolean validateModel = true;

			private boolean uriHandler$set;
			private URIHandler uriHandler;

			private boolean resourceSet$set;
			private ResourceSet resourceSet;

			private boolean loadOptions$set;
			private Map<Object, Object> loadOptions;

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
			public AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder uri(final URI uri) {
				requireNonNull(uri);
				this.uri = uri;
				return this;
			}

			@java.lang.SuppressWarnings("all")
			/**
			 * Defines the {@link URIHandler} used for model IO. If not defined the default is EMF used.
			 */
			public AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder uriHandler(
					final URIHandler uriHandler) {
				requireNonNull(uriHandler);
				this.uriHandler = uriHandler;
				uriHandler$set = true;
				return this;
			}

			@java.lang.SuppressWarnings("all")
			/**
			 * Defines the default {@link ResourceSet}. If it is not defined the factory based resourceSet is used.
			 */
			public AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder resourceSet(
					final ResourceSet resourceSet) {
				requireNonNull(resourceSet);
				this.resourceSet = resourceSet;
				resourceSet$set = true;
				return this;
			}

			@java.lang.SuppressWarnings("all")
			/**
			 * Defines the load options for model. If not defined the
			 * {@link AsmModelResourceSupport#getAsmModelDefaultLoadOptions()} us used.
			 */
			public AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder loadOptions(
					final Map<Object, Object> loadOptions) {
				requireNonNull(loadOptions);
				loadOptions$set = true;
				return this;
			}

			@java.lang.SuppressWarnings("all")
			/**
			 * Defines that model validation required or not on load. Default: true
			 */
			public AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder validateModel(boolean validateModel) {
				this.validateModel = validateModel;
				return this;
			}

			@java.lang.SuppressWarnings("all")
			/**
			 * Defines the file if it is not loaded from URI. If not defined, URI is used. If inputStream is defined
			 * it is used.
			 */
			public AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder file(final File file) {
				requireNonNull(file);
				this.file = file;
				file$set = true;
				return this;
			}

			@java.lang.SuppressWarnings("all")
			/**
			 * Defines the file if it is not loaded from  File or URI. If not defined, File or URI is used.
			 */
			public AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder inputStream(
					final InputStream inputStream) {
				requireNonNull(inputStream);
				inputStream$set = true;
				return this;
			}


			public AsmModelResourceSupport.LoadArguments build() {
				URIHandler uriHandler = this.uriHandler;
				if (!uriHandler$set) uriHandler = AsmModelResourceSupport.LoadArguments.$default$uriHandler();
				ResourceSet resourceSet = this.resourceSet;
				if (!resourceSet$set) resourceSet = AsmModelResourceSupport.LoadArguments.$default$resourceSet();
				Map<Object, Object> loadOptions = this.loadOptions;
				if (!loadOptions$set) loadOptions = AsmModelResourceSupport.LoadArguments.$default$loadOptions();
				File file = this.file;
				if (!file$set) file = AsmModelResourceSupport.LoadArguments.$default$file();
				InputStream inputStream = this.inputStream;
				if (!inputStream$set) inputStream = AsmModelResourceSupport.LoadArguments.$default$inputStream();

				return new AsmModelResourceSupport.LoadArguments(uri, uriHandler, resourceSet,
						loadOptions, validateModel, file, inputStream);
			}

			@java.lang.Override
			public java.lang.String toString() {
				return "AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder(uri=" + this.uri
						+ ", uri=" + this.uri
						+ ", uriHandler=" + this.uriHandler
						+ ", resourceSet=" + this.resourceSet
						+ ", loadOptions=" + this.loadOptions
						+ ", validateModel=" + this.validateModel
						+ ", file=" + this.file
						+ ", inputStream=" + this.inputStream
						+ ")";
			}
		}

		public static AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder asmLoadArgumentsBuilder() {
			return new AsmModelResourceSupport.LoadArguments.LoadArgumentsBuilder();
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
	}

	/**
	 * Arguments for {@link AsmModelResourceSupport#saveAsm(AsmModelResourceSupport.SaveArguments)}
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

		Optional<OutputStream> getOutputStream() {
			return ofNullable(outputStream);
		}

		Optional<File> getFile() {
			return ofNullable(file);
		}

		Optional<Map<Object, Object>> getSaveOptions() {
			return ofNullable(saveOptions);
		}

		boolean isValidateModel() {
			return validateModel;
		}

		@java.lang.SuppressWarnings("all")
		/**
		 * Builder for {@link AsmModelResourceSupport#saveAsmModel(AsmModelResourceSupport.SaveArguments)}.
		 */
		public static class SaveArgumentsBuilder {
			private boolean outputStream$set;
			private OutputStream outputStream;

			private boolean file$set;
			private File file;

			private boolean saveOptions$set;
			private Map<Object, Object> saveOptions;

			private boolean validateModel = true;

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

			SaveArgumentsBuilder() {
			}

			/**
			 * Defines {@link OutputStream} which is used by save. Whe it is not defined, file is used.
			 */
			public AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder outputStream(
					final OutputStream outputStream) {
				requireNonNull(outputStream);
				this.outputStream = outputStream;
				outputStream$set = true;
				return this;
			}

			/**
			 * Defines {@link File} which is used by save. Whe it is not defined the model's
			 * {@link AsmModelResourceSupport#uri is used}
			 */
			public AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder file(File file) {
				requireNonNull(file);
				this.file = file;
				file$set = true;
				return this;
			}

			/**
			 * Defines save options. When it is not defined
			 * {@link AsmModelResourceSupport#getAsmModelDefaultSaveOptions()} is used.
			 * @param saveOptions
			 * @return
			 */
			public AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder saveOptions(
					final Map<Object, Object> saveOptions) {
				requireNonNull(saveOptions);
				this.saveOptions = saveOptions;
				saveOptions$set = true;
				return this;
			}

			/**
			 * Defines that model validation required or not on save. Default: true
			 */
			public AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder validateModel(boolean validateModel) {
				this.validateModel = validateModel;
				return this;
			}

			public AsmModelResourceSupport.SaveArguments build() {
				OutputStream outputStream = this.outputStream;
				if (!outputStream$set) outputStream = AsmModelResourceSupport.SaveArguments.$default$outputStream();
				File file = this.file;
				if (!file$set) file = AsmModelResourceSupport.SaveArguments.$default$file();
				Map<Object, Object> saveOptions = this.saveOptions;
				if (!saveOptions$set) saveOptions = AsmModelResourceSupport.SaveArguments.$default$saveOptions();
				return new AsmModelResourceSupport.SaveArguments(outputStream, file, saveOptions, validateModel);
			}

			@java.lang.Override
			public java.lang.String toString() {
				return "AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder(outputStream=" + this.outputStream
						+ ", file=" + this.file
						+ ", saveOptions=" + this.saveOptions + ")";
			}
		}

		public static AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder asmSaveArgumentsBuilder() {
			return new AsmModelResourceSupport.SaveArguments.SaveArgumentsBuilder();
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

	/**
	 * This exception is thrown when validateModel is true on load or save and the model is not conform with its
	 * defined metamodel.
	 */
	public static class AsmValidationException extends Exception {
		AsmModelResourceSupport asmModelResourceSupport;

		AsmValidationException(AsmModelResourceSupport asmModelResourceSupport) {
			super("Invalid model\n" +
					asmModelResourceSupport.getDiagnosticsAsString() + "\n" + asmModelResourceSupport.asString()
			);
			this.asmModelResourceSupport = asmModelResourceSupport;
		}

		AsmModelResourceSupport getAsmModelResourceSupport() {
			return asmModelResourceSupport;
		}
	}

	// TODO: Create ticket on Eclipse. The problem is that the DelegatingResourceLocator
	// creating a baseURL which ends with two trailing slash and the ResourceBundle cannot open the files.
	// This bug is came on Felix based OSGi container.
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
