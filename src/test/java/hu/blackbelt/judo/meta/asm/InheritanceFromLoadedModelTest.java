package hu.blackbelt.judo.meta.asm;

import hu.blackbelt.judo.meta.asm.runtime.AsmModelLoader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

@Slf4j
public class InheritanceFromLoadedModelTest {

    ResourceSet resourceSet;

    @Before
    public void setUp() {
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
    }

    @After
    public void tearDown() {
        resourceSet = null;
    }

    @Test
    public void testInheritedAttributesInXMI() throws Exception {
        log.info("Testing XMI ...");
        final File modelFile = new File(srcDir(), "test/resources/inheritance.model");
        AsmModelLoader.loadAsmModel(resourceSet, URI.createURI(modelFile.getAbsolutePath()),"test","1.0.0");
        resourceSet.setURIConverter(new ModelFileBasedUriConverter(modelFile));

        test(2 + 1);
    }

    @Test
    public void testInheritedAttributesInXML() throws Exception {
        log.info("Testing XML ...");
        final File modelFile = new File(srcDir(), "test/resources/asm.model");
        AsmModelLoader.loadAsmModel(resourceSet, URI.createURI(modelFile.getAbsolutePath()),"test","1.0.0");
        resourceSet.setURIConverter(new ModelFileBasedUriConverter(modelFile));

        test(3 + 8);
    }

    @Test
    public void testInheritedAttributesInXMIWithStandardLoader() {
        log.info("Testing XMI (standard loader) ...");
        final File modelFile = new File(srcDir(), "test/resources/inheritance.model");
        resourceSet.getResource(URI.createURI(modelFile.getAbsolutePath()), true);
        resourceSet.setURIConverter(new ModelFileBasedUriConverter(modelFile));

        test(2 + 1);
    }

    @Test
    public void testInheritedAttributesInXMLWithStandardLoader() {
        log.info("Testing XML (standard loader) ...");
        final File modelFile = new File(srcDir(), "test/resources/asm.model");
        resourceSet.getResource(URI.createURI(modelFile.getAbsolutePath()), true);
        resourceSet.setURIConverter(new ModelFileBasedUriConverter(modelFile));

        test(3 + 8);
    }

    private void test(int expectedAttributes) {
        final EClass employeeClass = (EClass) resourceSet.getResources().get(0).getEObject("//entities/Employee");

        employeeClass.getEAllAttributes().forEach(a -> log.debug(" - attribute: {}", a.getName()));

        Assert.assertEquals(expectedAttributes, employeeClass.getEAllAttributes().size());
    }

    public File srcDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "../../src");
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }

    public static class ModelFileBasedUriConverter extends ExtensibleURIConverterImpl {

        private final File baseFile;

        public ModelFileBasedUriConverter(final File baseFile) {
            this.baseFile = baseFile;
        }

        @Override
        public URI normalize(URI uri) {
            String fragment = uri.fragment();
            String query = uri.query();
            URI trimmedURI = uri.trimFragment().trimQuery();
            URI result = getInternalURIMap().getURI(trimmedURI);
            String scheme = result.scheme();
            if (scheme == null) {
                if (result.hasAbsolutePath()) {
                    result = URI.createURI("file:" + result);
                } else {
                    result = URI.createFileURI(new File(baseFile, result.toString()).getAbsolutePath());
                }
            }

            if (result == trimmedURI) {
                return uri;
            }

            if (query != null) {
                result = result.appendQuery(query);
            }
            if (fragment != null) {
                result = result.appendFragment(fragment);
            }


            return normalize(result);
        }
    }
}
