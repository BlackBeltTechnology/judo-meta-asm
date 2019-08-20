package hu.blackbelt.judo.meta.asm.osgi;

import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.osgi.utils.osgi.api.BundleCallback;
import hu.blackbelt.osgi.utils.osgi.api.BundleTrackerManager;
import hu.blackbelt.osgi.utils.osgi.api.BundleUtil;
import lombok.extern.slf4j.Slf4j;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.VersionRange;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Component(immediate = true)
@Slf4j
@Designate(ocd = AsmModelBundleTracker.TrackerConfig.class)
public class AsmModelBundleTracker {

    public static final String ASM_MODELS = "Asm-Models";

    @ObjectClassDefinition(name="Asm Model Bundle Tracker")
    public @interface TrackerConfig {
        @AttributeDefinition(
                name = "Tags",
                description = "Which tags are on the loaded model when there is no one defined in bundle"
        )
        String tags() default "";
    }

    @Reference
    BundleTrackerManager bundleTrackerManager;

    Map<String, ServiceRegistration<AsmModel>> asmModelRegistrations = new ConcurrentHashMap<>();

    Map<String, AsmModel> asmModels = new HashMap<>();

    TrackerConfig config;

    @Activate
    public void activate(final ComponentContext componentContext, final TrackerConfig trackerConfig) {
        this.config = trackerConfig;
        bundleTrackerManager.registerBundleCallback(this.getClass().getName(),
                new AsmRegisterCallback(componentContext.getBundleContext()),
                new AsmUnregisterCallback(),
                new AsmBundlePredicate());
    }

    @Deactivate
    public void deactivate(final ComponentContext componentContext) {
        bundleTrackerManager.unregisterBundleCallback(this.getClass().getName());
    }

    private static class AsmBundlePredicate implements Predicate<Bundle> {
        @Override
        public boolean test(Bundle trackedBundle) {
            return BundleUtil.hasHeader(trackedBundle, ASM_MODELS);
        }
    }

    private class AsmRegisterCallback implements BundleCallback {

        BundleContext bundleContext;

        public AsmRegisterCallback(BundleContext bundleContext) {
            this.bundleContext = bundleContext;
        }

        @Override
        public void accept(Bundle trackedBundle) {
            List<Map<String, String>> entries = BundleUtil.getHeaderEntries(trackedBundle, ASM_MODELS);


            for (Map<String, String> params : entries) {
                String key = params.get(AsmModel.NAME);
                if (asmModelRegistrations.containsKey(key)) {
                    log.error("Asm model already loaded: " + key);
                } else {
                    if (params.containsKey(AsmModel.META_VERSION_RANGE)) {
                        VersionRange versionRange = new VersionRange(params.get(AsmModel.META_VERSION_RANGE).replaceAll("\"", ""));
                        if (versionRange.includes(bundleContext.getBundle().getVersion())) {
                            // Unpack model
                            try {
                                        AsmModel asmModel = AsmModel.loadAsmModel(
                                        AsmModel.LoadArguments.asmLoadArgumentsBuilder()
                                                .inputStream(trackedBundle.getEntry(params.get("file")).openStream())
                                                .name(params.get(AsmModel.NAME))
                                                .version(trackedBundle.getVersion().toString())
                                                .checksum(params.get(AsmModel.CHECKSUM))
                                                .tags(Stream.of(ofNullable(params.get(AsmModel.TAGS)).orElse(config.tags()).split(",")).collect(Collectors.toSet()))
                                                .acceptedMetaVersionRange(versionRange.toString())
                                );

                                log.info("Registering Asm model: " + asmModel);

                                ServiceRegistration<AsmModel> modelServiceRegistration = bundleContext.registerService(AsmModel.class, asmModel, asmModel.toDictionary());
                                asmModels.put(key, asmModel);
                                asmModelRegistrations.put(key, modelServiceRegistration);

                            } catch (IOException | AsmModel.AsmValidationException e) {
                                log.error("Could not load Psm model: " + params.get(AsmModel.NAME) + " from bundle: " + trackedBundle.getBundleId(), e);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public Thread process(Bundle bundle) {
            return null;
        }
    }

    private class AsmUnregisterCallback implements BundleCallback {

        @Override
        public void accept(Bundle trackedBundle) {
            List<Map<String, String>> entries = BundleUtil.getHeaderEntries(trackedBundle, ASM_MODELS);
            for (Map<String, String> params : entries) {
                String key = params.get(AsmModel.NAME);

                if (asmModels.containsKey(key)) {
                    ServiceRegistration<AsmModel> modelServiceRegistration = asmModelRegistrations.get(key);

                    if (modelServiceRegistration != null) {
                        log.info("Unregistering Asm model: " + asmModels.get(key));
                        modelServiceRegistration.unregister();
                        asmModelRegistrations.remove(key);
                        asmModels.remove(key);
                    }
                } else {
                    log.error("Asm Model is not registered: " + key);
                }
            }
        }

        @Override
        public Thread process(Bundle bundle) {
            return null;
        }
    }

}
