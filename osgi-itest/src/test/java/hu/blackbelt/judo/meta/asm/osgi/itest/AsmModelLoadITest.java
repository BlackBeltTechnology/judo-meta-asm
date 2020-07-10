package hu.blackbelt.judo.meta.asm.osgi.itest;

import hu.blackbelt.epsilon.runtime.execution.impl.StringBuilderLogger;
import hu.blackbelt.judo.meta.asm.runtime.AsmEpsilonValidator;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel.AsmValidationException;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel.SaveArguments;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import hu.blackbelt.osgi.utils.osgi.api.BundleTrackerManager;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.builder.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

import javax.inject.Inject;
import java.io.*;

import static hu.blackbelt.judo.meta.asm.osgi.itest.AsmKarafFeatureProvider.*;
import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.buildAsmModel;
import static org.junit.Assert.assertFalse;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.useEClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class AsmModelLoadITest {

    private static final String DEMO = "northwind-asm";

    @Inject
    LogService log;

    @Inject
    protected BundleTrackerManager bundleTrackerManager;

    @Inject
    BundleContext bundleContext;

    @Inject
    AsmModel asmModel;

    @Configuration
    public Option[] config() throws IOException, AsmValidationException {

        return combine(getRuntimeFeaturesForMetamodel(this.getClass()),
                mavenBundle(maven()
                        .groupId("hu.blackbelt.judo.meta")
                        .artifactId("hu.blackbelt.judo.meta.asm.osgi")
                        .versionAsInProject()),
                getProvisonModelBundle());
    }

    public Option getProvisonModelBundle() throws IOException, AsmValidationException {
        return provision(
                getAsmModelBundle()
        );
    }

    private InputStream getAsmModelBundle() throws IOException, AsmValidationException {
    	
    	AsmModel asmModel = buildAsmModel()
                .name(DEMO)
                .build();
    	
    	ByteArrayOutputStream os = new ByteArrayOutputStream();
    	
    	asmModel.saveAsmModel(SaveArguments.asmSaveArgumentsBuilder().outputStream(os));
    	
    	return bundle()
                .add("model/" + DEMO + ".judo-meta-asm",
                        new ByteArrayInputStream(os.toByteArray()))
                .set( Constants.BUNDLE_MANIFESTVERSION, "2")
                .set( Constants.BUNDLE_SYMBOLICNAME, DEMO + "-asm" )
                //set( Constants.IMPORT_PACKAGE, "meta/psm;version=\"" + getConfiguration(META_PSM_IMPORT_RANGE) +"\"")
                .set( "Asm-Models", "file=model/" + DEMO + ".judo-meta-asm;version=1.0.0;name=" + DEMO + ";checksum=notset;meta-version-range=\"[1.0.0,2)\"")
                .build( withBnd());
    }

    @Test
    public void testModelValidation() {
        StringBuilderLogger logger = new StringBuilderLogger(StringBuilderLogger.LogLevel.DEBUG);
        try {
            AsmEpsilonValidator.validateAsm(logger,
                    asmModel,
                    AsmEpsilonValidator.calculateAsmValidationScriptURI());

        } catch (Exception e) {
            log.log(LogService.LOG_ERROR, logger.getBuffer());
            assertFalse(true);
        }
    }
}
