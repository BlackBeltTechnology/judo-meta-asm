package hu.blackbelt.judo.meta.asm.runtime;

import com.google.common.collect.Maps;
import hu.blackbelt.epsilon.runtime.execution.EmfUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.URIHandlerImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsmModelLoader {

    public static void registerAsmMetamodel(ResourceSet resourceSet) {
        resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
    }

    public static Resource.Factory getFactory() {
        return new XMIResourceFactoryImpl();
    }

    public static ResourceSet createAsmResourceSet() throws Exception {
        return createAsmResourceSet(null);
    }

    public static ResourceSet createAsmResourceSet(URIHandler uriHandler) throws Exception {
        ResourceSet resourceSet = new ResourceSetImpl();
        if (uriHandler != null) {
            resourceSet.getURIConverter().getURIHandlers().add(0, uriHandler);
        }
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(ResourceFactoryRegistryImpl.DEFAULT_EXTENSION, getFactory());
        registerAsmMetamodel(resourceSet);
        registerAsmPackages(resourceSet, null);
        return resourceSet;
    }

    public static void registerAsmPackages(ResourceSet resourceSet, AsmPackageRegistration asmPackageRegistration) throws Exception {
        if (asmPackageRegistration == null) {
            new LocalAsmPackageRegistration().loadAsmBaseAndTypes(resourceSet);
        } else {
            asmPackageRegistration.loadAsmBaseAndTypes(resourceSet);
        }
    }

    public static AsmModel loadAsmModel(ResourceSet resourceSet, URI uri, String name, String version) throws Exception {
        return loadAsmModel(resourceSet, null, uri, name, version, null, null);
    }

    public static AsmModel loadAsmModel(ResourceSet resourceSet, AsmPackageRegistration asmPackageRegistration, URI uri, String name, String version, String checksum, String acceptedMetaVersionRange) throws Exception {
        Resource resource = resourceSet.createResource(uri);
        Map<Object, Object> loadOptions = new HashMap<>();
        //loadOptions.put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
        //loadOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
        loadOptions.put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
        loadOptions.put(XMLResource.OPTION_LAX_FEATURE_PROCESSING, Boolean.TRUE);
        loadOptions.put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
        resource.load(loadOptions);

        AsmModel.AsmModelBuilder b = AsmModel.asmModelBuilder();
        b.name(name)
                .version(version)
                .uri(uri)
                .checksum(checksum)
                .resource(resource);

        if (checksum != null) {
            b.checksum(checksum);
        }

        if (acceptedMetaVersionRange != null)  {
            b.metaVersionRange(acceptedMetaVersionRange);
        }

        return b.build();
    }

    public static class LocalAsmPackageRegistration implements AsmPackageRegistration {
        public void loadAsmBaseAndTypes(ResourceSet resourceSet) throws Exception {
            List<EPackage> basePackages = EmfUtils.register(resourceSet, URI
                    .createURI("urn:base.ecore"), true);
            List<EPackage> typePackages = EmfUtils.register(resourceSet, URI
                    .createURI("urn:types.ecore"), true);
        }
    }

    public static Map<Object, Object> getDefaultSaveOptions() {
        final Map<Object, Object> saveOptions = Maps.newHashMap(); //asmResourceSet.getDefaultSaveOptions();
        saveOptions.put(XMIResource.OPTION_DECLARE_XML,Boolean.TRUE);
        saveOptions.put(XMIResource.OPTION_PROCESS_DANGLING_HREF,XMIResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
        saveOptions.put(XMIResource.OPTION_URI_HANDLER, new URIHandlerImpl() {
            @Override
            public URI deresolve(URI uri) {
                if (uri.hasFragment() && uri.hasOpaquePart() && baseURI.hasOpaquePart()) {
                    if (uri.opaquePart().equals(baseURI.opaquePart())) {
                        return URI.createURI("#" + uri.fragment());
                    }
                }
                return super.deresolve(uri);
            }
        });
        saveOptions.put(XMIResource.OPTION_SCHEMA_LOCATION,Boolean.TRUE);
        saveOptions.put(XMIResource.OPTION_DEFER_IDREF_RESOLUTION,Boolean.TRUE);
        saveOptions.put(XMIResource.OPTION_SKIP_ESCAPE_URI,Boolean.FALSE);
        saveOptions.put(XMIResource.OPTION_ENCODING,"UTF-8");
        return saveOptions;
    }


    public static void saveAsmModel(AsmModel asmModel) throws IOException {
        asmModel.getResource().save(getDefaultSaveOptions());
    }
}
