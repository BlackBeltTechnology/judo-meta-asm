package hu.blackbelt.judo.meta.asm;

import hu.blackbelt.judo.meta.asm.AsmResourceLoader;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class AsmResourceLoaderTest {

    @Mock
    BundleContext bundleContextMock;

    @Mock
    Bundle bundleMock;

    AsmResourceLoader asmResourceLoader = new AsmResourceLoader();


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(bundleContextMock.getBundle()).thenReturn(bundleMock);
        when(bundleMock.getEntry("meta/asm/base.ecore")).thenReturn(getBundleFile("base.ecore").toURI().toURL());
        when(bundleMock.getEntry("meta/asm/types.ecore")).thenReturn(getBundleFile("types.ecore").toURI().toURL());
        when(bundleMock.getDataFile("asm_base.model")).thenReturn(getTargetFile("asm_base.model"));
        when(bundleMock.getDataFile("asm_types.model")).thenReturn(getTargetFile("asm_types.model"));

        asmResourceLoader.activate(bundleContextMock);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoadToReourceSet() throws Exception {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(resourceSet.getResourceFactoryRegistry().DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

        asmResourceLoader.loadToReourceSet(resourceSet);

        assertEquals("asm_base.model", asmResourceLoader.getBaseFile().getName());
        assertEquals("asm_types.model", asmResourceLoader.getTypesFile().getName());

        TreeIterator<Notifier> iter = resourceSet.getAllContents();
        while (iter.hasNext()) {
            final Notifier obj = iter.next();
            System.out.println(obj.toString());
        }
    }


    private File getBundleFile(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(new File(classLoader.getResource("").getFile()).getParentFile().getParentFile().getAbsoluteFile() + File.separator + "model");

        return new File(file.getAbsolutePath() + File.separator + name);
    }

    private File getTargetFile(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("").getFile()).getAbsoluteFile();

        return new File(file.getAbsolutePath() + File.separator + name);
    }

}