package hu.blackbelt.judo.meta.asm;


import com.google.common.io.Files;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


@Component(immediate = true, service = AsmResourceLoader.class)
public class AsmResourceLoader {
    Bundle bundle;
    File asmBase;
    File asmTypes;

    private File writeAsBundleFile(Bundle bundle, String targetName, String fileInBundle) throws IOException {
        File outFile = bundle.getDataFile(targetName);

        InputStream initialStream = bundle.getEntry(fileInBundle).openStream();
        byte[] buffer = new byte[initialStream.available()];
        initialStream.read(buffer);
        Files.write(buffer, outFile);
        return outFile;
    }

    @Activate
    public void activate(BundleContext bundleContext) throws IOException {
        bundle = bundleContext.getBundle();
        // Check if global package registry contains the EcorePackage
        if (EPackage.Registry.INSTANCE.getEPackage(EcorePackage.eNS_URI) == null) {
            EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
        }

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("asm", new XMIResourceFactoryImpl());

        asmBase = writeAsBundleFile(bundle, "asm_base.model", "meta/asm/base.ecore");
        asmTypes = writeAsBundleFile(bundle, "asm_types.model", "meta/asm/types.ecore");
    }

    @Deactivate
    public void deactivate() {

    }

    public void loadToReourceSet(ResourceSet resourceSet) throws IOException {
        Resource baseAsmResource = resourceSet.createResource(URI
                .createURI("base.asm"));
        baseAsmResource.load(bundle.getEntry("meta/asm/base.ecore").openStream(), new HashMap<String, Object>());

        Resource typesAsmResource = resourceSet.createResource(URI
                .createURI("types.asm"));
        typesAsmResource.load(bundle.getEntry("meta/asm/types.ecore").openStream(), new HashMap<String, Object>());
    }

    public File getBaseFile() {
        return asmBase;
    }


    public File getTypesFile() {
        return asmTypes;
    }

}