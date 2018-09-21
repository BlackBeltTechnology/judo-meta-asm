package hu.blackbelt.meta.asm;


import com.google.common.io.Files;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class AsmResourceLoader {

    private File writeAsBundleFile(Bundle bundle, String targetName, String fileInBundle) throws IOException {
        File outFile = bundle.getDataFile(targetName);

        InputStream initialStream = bundle.getEntry(fileInBundle).openStream();
        byte[] buffer = new byte[initialStream.available()];
        initialStream.read(buffer);
        Files.write(buffer, outFile);
        return outFile;
    }

    public void init(Bundle bundle) throws IOException {
        // Unpack the bundle file
        // https://www.eclipse.org/forums/index.php/t/130038/
        // shttp://www.vogella.com/tutorials/EclipseEMFPersistence/article.html

        // Check if global package registry contains the EcorePackage
        if (EPackage.Registry.INSTANCE.getEPackage(EcorePackage.eNS_URI) == null) {
            EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
        }

        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

        File baseAsm = writeAsBundleFile(bundle, "asm_base.model", "meta/asm/base.ecore");
        File typesAsm = writeAsBundleFile(bundle, "asm_types.model", "meta/asm/types.ecore");

        Resource baseAsmResource = resourceSet.getResource(URI
                .createURI("http://blackbelt.hu/judo/asm/base"), false);
        baseAsmResource.load(bundle.getEntry("meta/asm/base.ecore").openStream(), );

    }

    /*

    protected static void setDataTypesInstanceClasses(Resource metamodel) {
        Iterator<EObject> it = metamodel.getAllContents();
        while (it.hasNext()) {
            EObject eObject = (EObject) it.next();
            if (eObject instanceof EEnum) {
                // ((EEnum) eObject).setInstanceClassName("java.lang.Integer");
            } else if (eObject instanceof EDataType) {
                EDataType eDataType = (EDataType) eObject;
                String instanceClass = "";
                if (eDataType.getName().equals("String")) {
                    instanceClass = "java.lang.String";
                } else if (eDataType.getName().equals("Boolean")) {
                    instanceClass = "java.lang.Boolean";
                } else if (eDataType.getName().equals("Integer")) {
                    instanceClass = "java.lang.Integer";
                } else if (eDataType.getName().equals("Float")) {
                    instanceClass = "java.lang.Float";
                } else if (eDataType.getName().equals("Double")) {
                    instanceClass = "java.lang.Double";
                }
                if (instanceClass.trim().length() > 0) {
                    eDataType.setInstanceClassName(instanceClass);
                }
            }
        }
    }

    /--**
     * Register all the packages in the metamodel specified by the uri in the registry.
     *
     * @param resourceSet The resourceSet metamodel registered for
     * @param uri The URI of the metamodel
     * @param useUriForResource If True, the URI of the resource created for the metamodel would be overwritten
     * 	with the URI of the [last] EPackage in the metamodel.
     * @return A list of the EPackages registered.
     * @throws Exception If there is an error accessing the resources.
     *--/
    public static List<EPackage> register(ResourceSet resourceSet, URI uri, boolean useUriForResource) throws Exception {

        List<EPackage> ePackages = new ArrayList<EPackage>();

        Resource metamodel = resourceSet.createResource(uri);
        metamodel.load(Collections.EMPTY_MAP);

        setDataTypesInstanceClasses(metamodel);

        Iterator<EObject> it = metamodel.getAllContents();
        while (it.hasNext()) {
            Object next = it.next();
            if (next instanceof EPackage) {
                EPackage p = (EPackage) next;

                if (p.getNsURI() == null || p.getNsURI().trim().length() == 0) {
                    if (p.getESuperPackage() == null) {
                        p.setNsURI(p.getName());
                    }
                    else {
                        p.setNsURI(p.getESuperPackage().getNsURI() + "/" + p.getName());
                    }
                }

                if (p.getNsPrefix() == null || p.getNsPrefix().trim().length() == 0) {
                    if (p.getESuperPackage() != null) {
                        if (p.getESuperPackage().getNsPrefix()!=null) {
                            p.setNsPrefix(p.getESuperPackage().getNsPrefix() + "." + p.getName());
                        }
                        else {
                            p.setNsPrefix(p.getName());
                        }
                    }
                }

                if (p.getNsPrefix() == null) {
                    p.setNsPrefix(p.getName());
                }

                EPackage.Registry.INSTANCE.put(p.getNsURI(), p);
                resourceSet.getPackageRegistry().put(p.getNsURI(), p);

                if (useUriForResource) {
                    metamodel.setURI(URI.createURI(p.getNsURI()));
                }
                ePackages.add(p);
            }
        }
        return ePackages;
    }
   */

}