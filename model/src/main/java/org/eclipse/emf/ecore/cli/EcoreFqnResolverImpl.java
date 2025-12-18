package org.eclipse.emf.ecore.cli;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;

import hu.blackbelt.judo.cli.api.FqnResolver;

/**
 * Ecore FQN resolver implementation.
 * <p>
 * Implements {@link FqnResolver} from model-cli-api for type-safe FQN resolution.
 */
public class EcoreFqnResolverImpl implements FqnResolver {

    private static final String SEPARATOR = "::";
    private final Map<String, EObject> cache = new ConcurrentHashMap<>();
    private ResourceSet resourceSet;

    @Override
    public synchronized void bind(ResourceSet resourceSet) {
        this.resourceSet = resourceSet;
        rebuildCache();
    }

    @Override
    public synchronized void unbind() {
        this.resourceSet = null;
        cache.clear();
    }

    @Override
    public boolean isBound() {
        return resourceSet != null;
    }

    @Override
    public String getModelType() {
        return "ecore";
    }

    @Override
    public Optional<EObject> resolve(String fqn) {
        if (fqn == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(fqn));
    }

    @Override
    public Stream<String> getFqnCollection() {
        return cache.keySet().stream();
    }

    @Override
    public Optional<String> getFqn(EObject eObject) {
        return computeFqn(eObject);
    }

    @Override
    public Stream<String> findByPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return getFqnCollection();
        }
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return cache.keySet().stream()
                .filter(fqn -> fqn.matches(regex));
    }

    private void rebuildCache() {
        cache.clear();
        if (resourceSet == null) {
            return;
        }
        TreeIterator<?> iterator = resourceSet.getAllContents();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof EObject) {
                EObject current = (EObject) next;
                computeFqn(current).ifPresent(fqn -> cache.put(fqn, current));
            }
        }
    }

    private Optional<String> computeFqn(EObject eObject) {
        if (eObject instanceof EPackage) {
            EPackage pkg = (EPackage) eObject;
            return Optional.ofNullable(getPackageFqn(pkg));
        }
        if (eObject instanceof EClassifier) {
            EClassifier classifier = (EClassifier) eObject;
            EPackage pkg = classifier.getEPackage();
            if (pkg != null) {
                String pkgFqn = getPackageFqn(pkg);
                if (pkgFqn != null && classifier.getName() != null) {
                    return Optional.of(pkgFqn + SEPARATOR + classifier.getName());
                }
            }
        }
        if (eObject instanceof EStructuralFeature) {
            EStructuralFeature feature = (EStructuralFeature) eObject;
            if (feature.getEContainingClass() != null) {
                EPackage pkg = feature.getEContainingClass().getEPackage();
                if (pkg != null) {
                    String pkgFqn = getPackageFqn(pkg);
                    String className = feature.getEContainingClass().getName();
                    if (pkgFqn != null && className != null && feature.getName() != null) {
                        return Optional.of(pkgFqn + SEPARATOR + className + "." + feature.getName());
                    }
                }
            }
        }
        if (eObject instanceof ENamedElement) {
            ENamedElement named = (ENamedElement) eObject;
            if (named.getName() != null) {
                return Optional.of(named.getName());
            }
        }
        return Optional.of(EcoreUtil.getURI(eObject).toString());
    }

    private String getPackageFqn(EPackage pkg) {
        if (pkg == null) {
            return null;
        }
        EPackage superPackage = pkg.getESuperPackage();
        if (superPackage != null) {
            String superFqn = getPackageFqn(superPackage);
            if (superFqn != null && pkg.getName() != null) {
                return superFqn + SEPARATOR + pkg.getName();
            }
        }
        return pkg.getName();
    }

    @Override
    public Optional<EObject> resolveByXmiId(String xmiId) {
        if (xmiId == null || resourceSet == null) {
            return Optional.empty();
        }
        // Try to find by URI fragment
        for (var resource : resourceSet.getResources()) {
            EObject obj = resource.getEObject(xmiId);
            if (obj != null) {
                return Optional.of(obj);
            }
        }
        return Optional.empty();
    }
}
