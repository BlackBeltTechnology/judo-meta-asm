package hu.blackbelt.judo.meta.asm.osgi;

/*-
 * #%L
 * Judo :: Asm :: Model
 * %%
 * Copyright (C) 2018 - 2022 BlackBelt Technology
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

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
                    // Unpack model
                    try {
                                AsmModel asmModel = AsmModel.loadAsmModel(
                                AsmModel.LoadArguments.asmLoadArgumentsBuilder()
                                        .inputStream(trackedBundle.getEntry(params.get("file")).openStream()));

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
