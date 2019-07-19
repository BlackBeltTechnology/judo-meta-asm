package hu.blackbelt.judo.meta.asm.support;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ContentHandler;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.URIHandlerImpl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EcorePackage;			
import org.eclipse.emf.ecore.util.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.util.EcoreResourceImpl;			

public class AsmModelResourceSupport {

	ResourceSet resourceSet;
	
	Optional<URIHandler> uriHandler;
	
	Optional<URI> rootUri;
	
	public <T> Stream<T> asStream(Iterator<T> sourceIterator) {
	    return asStream(sourceIterator, false);
	}
	
	public <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
	    Iterable<T> iterable = () -> sourceIterator;
	    return StreamSupport.stream(iterable.spliterator(), parallel);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Stream<T> all() {
	    return asStream((Iterator<T>) resourceSet.getAllContents(), false);
	}
	
	@SuppressWarnings("unchecked")
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


	public static void setupUriHandler(ResourceSet resourceSet, URIHandler uriHandler) {
	    resourceSet.getURIConverter().getURIHandlers().add(0, uriHandler);
	}


	public static ResourceSet createAsmResourceSet() {
	    ResourceSet resourceSet = new ResourceSetImpl();
	    registerEcoreMetamodel(resourceSet);
	    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(ResourceFactoryRegistryImpl.DEFAULT_EXTENSION, getEcoreFactory());
	    return resourceSet;
	}
	
	public static Map<Object, Object> getAsmModelDefaultLoadOptions() {
	    Map<Object, Object> loadOptions = new HashMap<>();
	    //loadOptions.put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
	    //loadOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
	    loadOptions.put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
	    loadOptions.put(XMLResource.OPTION_LAX_FEATURE_PROCESSING, Boolean.TRUE);
	    loadOptions.put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
	    return loadOptions;
	}
	
	public static Map<Object, Object> getAsmModelDefaultSaveOptions() {
	    Map<Object, Object> saveOptions = new HashMap<>();
	    saveOptions.put(XMLResource.OPTION_DECLARE_XML, Boolean.TRUE);
	    saveOptions.put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
	    saveOptions.put(XMLResource.OPTION_URI_HANDLER, new URIHandlerImpl() {
	        public URI deresolve(URI uri) {
	            return uri.hasFragment() && uri.hasOpaquePart() && this.baseURI.hasOpaquePart() && uri.opaquePart().equals(this.baseURI.opaquePart()) ? URI.createURI("#" + uri.fragment()) : super.deresolve(uri);
	        }
	    });
	    saveOptions.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
	    saveOptions.put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
	    saveOptions.put(XMLResource.OPTION_SKIP_ESCAPE_URI, Boolean.FALSE);
	    saveOptions.put(XMLResource.OPTION_ENCODING, "UTF-8");
	    return saveOptions;
	}
	
	public static Resource.Factory getEcoreFactory() {
	    return new EcoreResourceFactoryImpl() {
	        @Override
	        public Resource createResource(URI uri) {
	            Resource result = new EcoreResourceImpl(uri) {
	                @Override
	                protected boolean useUUIDs() {
	                    return true;
	                }
	            };
	            return result;
	        }
	    };
	}

	
	public static void registerEcoreMetamodel(ResourceSet resourceSet) {
		resourceSet.getPackageRegistry().put(EcorePackage.eINSTANCE.getNsURI(), EcorePackage.eINSTANCE);
	}

    // Builder specific code
    @SuppressWarnings("all")
    private static ResourceSet $default$resourceSet() {
        return createAsmResourceSet();
    }

    @SuppressWarnings("all")
    private static Optional<URIHandler> $default$uriHandler() {
        return Optional.empty();
    }

    @SuppressWarnings("all")
    private static Optional<URI> $default$rootUri() {
        return Optional.empty();
    }

    @SuppressWarnings("all")
    AsmModelResourceSupport(final ResourceSet resourceSet, final Optional<URIHandler> uriHandler, final Optional<URI> rootUri) {
        this.resourceSet = resourceSet;
        this.uriHandler = uriHandler;
        this.rootUri = rootUri;

        if (uriHandler.isPresent()) {
            setupUriHandler(resourceSet, uriHandler.get());
        }
        if (rootUri.isPresent()) {
            setupRelativeUriRoot(resourceSet, rootUri.get());
        }
    }

    @SuppressWarnings("all")
    public static class AsmModelResourceSupportBuilder {
        @SuppressWarnings("all")
        private ResourceSet resourceSet;
        @SuppressWarnings("all")
        private Optional<URIHandler> uriHandler;
        @SuppressWarnings("all")
        private Optional<URI> rootUri;

        @SuppressWarnings("all")
        AsmModelResourceSupportBuilder() {
        }

        @SuppressWarnings("all")
        public AsmModelResourceSupportBuilder resourceSet(final ResourceSet resourceSet) {
            this.resourceSet = resourceSet;
            return this;
        }

        @SuppressWarnings("all")
        public AsmModelResourceSupportBuilder uriHandler(final Optional<URIHandler> uriHandler) {
            this.uriHandler = uriHandler;
            return this;
        }

        @SuppressWarnings("all")
        public AsmModelResourceSupportBuilder rootUri(final Optional<URI> rootUri) {
            this.rootUri = rootUri;
            return this;
        }

        @SuppressWarnings("all")
        public AsmModelResourceSupport build() {
            return new AsmModelResourceSupport(resourceSet != null ? resourceSet : AsmModelResourceSupport.$default$resourceSet(),
                    uriHandler != null ? uriHandler : AsmModelResourceSupport.$default$uriHandler(),
                    rootUri != null ? rootUri : AsmModelResourceSupport.$default$rootUri());
        }

        @Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "AsmModelResourceSupport.AsmModelResourceSupportBuilder(resourceSet=" + this.resourceSet + ", uriHandler=" + this.uriHandler + ", rootUri=" + this.rootUri + ")";
        }
    }

    @SuppressWarnings("all")
    public static AsmModelResourceSupportBuilder asmModelResourceSupportBuilder() {
        return new AsmModelResourceSupportBuilder();
    }

    @SuppressWarnings("all")
    public ResourceSet getResourceSet() {
        return this.resourceSet;
    }
}
