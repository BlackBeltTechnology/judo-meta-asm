package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.epsilon.runtime.execution.EmfUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.List;


@Component(immediate = true)
public class AsmOsgiPackageRegistration implements AsmPackageRegistration {
    Bundle bundle;

    @Activate
    public void activate(BundleContext bundleContext) {
        bundle = bundleContext.getBundle();
    }

    @Deactivate
    public void deactivate() {
    }

    public void loadAsmBaseAndTypes(ResourceSet resourceSet) throws Exception {
        // BundleURIHandler uriHandler = new BundleURIHandler("urn", "/", bundle);
        // resourceSet.getURIConverter().getURIHandlers().add(0, uriHandler);

        List<EPackage> basePackages = EmfUtils.register(resourceSet, URI
                .createURI("urn:meta/asm/base.ecore"), true);
        List<EPackage> typePackages = EmfUtils.register(resourceSet, URI
                .createURI("urn:meta/asm/types.ecore"), true);
    }

}