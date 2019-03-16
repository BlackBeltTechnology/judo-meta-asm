package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.ecore.resource.ResourceSet;

public interface AsmPackageRegistration {

    void loadAsmBaseAndTypes(ResourceSet resourceSet) throws Exception;

}
