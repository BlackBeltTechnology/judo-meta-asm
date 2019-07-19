package hu.blackbelt.judo.meta.asm.osgi;

import hu.blackbelt.epsilon.runtime.osgi.BundleURIHandler;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.osgi.utils.osgi.api.BundleCallback;
import hu.blackbelt.osgi.utils.osgi.api.BundleTrackerManager;
import hu.blackbelt.osgi.utils.osgi.api.BundleUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.VersionRange;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Component(immediate = true)
@Slf4j
public class AsmModelBundleTracker {

    public static final String ASM_MODELS = "Asm-Models";

    @Reference
    BundleTrackerManager bundleTrackerManager;

    Map<String, ServiceRegistration<AsmModel>> asmModelRegistrations = new ConcurrentHashMap<>();

    Map<String, AsmModel> asmModels = new HashMap<>();

    @Activate
    public void activate(final ComponentContext componentContext) {
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
                                        AsmModel.LoadArguments.loadArgumentsBuilder()
                                                .uriHandler(Optional.of(new BundleURIHandler("urn", "", trackedBundle)))
                                                .uri(URI.createURI(params.get("file")))
                                                .name(params.get(AsmModel.NAME))
                                                .version(Optional.of(trackedBundle.getVersion().toString()))
                                                .checksum(Optional.ofNullable(params.get(AsmModel.CHECKSUM)))
                                                .acceptedMetaVersionRange(Optional.of(versionRange.toString()))
                                                .build()
                                );

                                log.info("Registering Asm model: " + asmModel);

                                ServiceRegistration<AsmModel> modelServiceRegistration = bundleContext.registerService(AsmModel.class, asmModel, asmModel.toDictionary());
                                asmModels.put(key, asmModel);
                                asmModelRegistrations.put(key, modelServiceRegistration);

                            } catch (IOException e) {
                                log.error("Could not load Asm model: " + params.get(AsmModel.NAME) + " from bundle: " + trackedBundle.getBundleId());
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
