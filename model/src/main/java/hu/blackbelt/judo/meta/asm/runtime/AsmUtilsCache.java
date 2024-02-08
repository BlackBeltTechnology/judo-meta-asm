package hu.blackbelt.judo.meta.asm.runtime;

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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AsmUtilsCache {

    private final static CacheLoader<ResourceSet, AsmUtilsCache> cacheLoader = new CacheLoader<>() {
        @Override
        public AsmUtilsCache load(ResourceSet resourceSet) {
            AsmUtilsCache cache = new AsmUtilsCache();
            return cache;
        }
    };

    private final static LoadingCache<ResourceSet, AsmUtilsCache> cacheProvider = CacheBuilder
            .newBuilder()
            .expireAfterAccess(Long.parseLong(System.getProperty("AsmUtilsCacheExpiration", "60")), TimeUnit.SECONDS)
            .build(cacheLoader);


    private final Map<String, Optional<EClassifier>> classifiersByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EReference>> referencesByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EAttribute>> attributesByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EOperation>> operationsByFqName = new ConcurrentHashMap<>();

    private final Map<EPackage, String> packageFqName = new ConcurrentHashMap<>();

    private Map<Class, Collection> elementsByType = new ConcurrentHashMap<>();

    private final Map<EClass, Optional<EClass>> entityByMappedTransfer = new ConcurrentHashMap<>();

    private final Map<EAttribute, Optional<EAttribute>> entityAttributeByMappedAttribute = new ConcurrentHashMap<>();

    private final Map<EReference, Optional<EReference>> entityReferenceByMappedReference = new ConcurrentHashMap<>();

    public static AsmUtilsCache getCache(ResourceSet resourceSet) {
        AsmUtilsCache cache = null;
        try {
            cache = cacheProvider.get(resourceSet);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return cache;
    }

    public void clear() {
        classifiersByFqName.clear();
        referencesByFqName.clear();
        attributesByFqName.clear();
        operationsByFqName.clear();
        elementsByType.clear();
        packageFqName.clear();
        entityByMappedTransfer.clear();
        entityAttributeByMappedAttribute.clear();
        entityReferenceByMappedReference.clear();
    }

    public Map<String, Optional<EClassifier>> getClassifiersByFqName() {
        return classifiersByFqName;
    }

    public Map<String, Optional<EReference>> getReferencesByFqName() {
        return referencesByFqName;
    }

    public Map<String, Optional<EAttribute>> getAttributesByFqName() {
        return attributesByFqName;
    }

    public Map<String, Optional<EOperation>> getOperationsByFqName() {
        return operationsByFqName;
    }

    public Map<Class, Collection> getElementsByType() {
        return elementsByType;
    }

    public Map<EPackage, String> getPackageFqName() {
        return packageFqName;
    }

    public Map<EClass, Optional<EClass>> getEntityByMappedTransfer() {
        return entityByMappedTransfer;
    }

    public Map<EAttribute, Optional<EAttribute>> getEntityAttributeByMappedAttribute() {
        return entityAttributeByMappedAttribute;
    }

    public Map<EReference, Optional<EReference>> getEntityReferenceByMappedReference() {
        return entityReferenceByMappedReference;
    }

}
