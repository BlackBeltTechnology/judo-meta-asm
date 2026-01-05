package hu.blackbelt.judo.meta.asm.cli;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;

import hu.blackbelt.judo.cli.api.FqnResolver;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;

/**
 * ASM FQN resolver implementation.
 * <p>
 * Implements {@link FqnResolver} from model-cli-api for type-safe FQN resolution.
 * <p>
 * ASM uses Ecore model elements (EPackage, EClass, EAttribute, EReference, EOperation, EEnum).
 */
public class AsmFqnResolverImpl implements FqnResolver {

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
        return "asm";
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
        return cache.keySet().stream()
                .filter(fqn -> fqn.matches(pattern));
    }

    @Override
    public Optional<EObject> resolveByXmiId(String xmiId) {
        if (xmiId == null || resourceSet == null) {
            return Optional.empty();
        }
        // Try to find by URI fragment
        for (Resource resource : resourceSet.getResources()) {
            EObject obj = resource.getEObject(xmiId);
            if (obj != null) {
                return Optional.of(obj);
            }
        }
        return Optional.empty();
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
        // EPackage
        if (eObject instanceof EPackage) {
            EPackage ePackage = (EPackage) eObject;
            return Optional.of(AsmUtils.getPackageFQName(ePackage));
        }

        // EClassifier (EClass, EDataType, EEnum)
        if (eObject instanceof EClassifier) {
            EClassifier eClassifier = (EClassifier) eObject;
            if (eClassifier.getEPackage() != null) {
                return Optional.of(AsmUtils.getClassifierFQName(eClassifier));
            }
        }

        // EAttribute
        if (eObject instanceof EAttribute) {
            EAttribute eAttribute = (EAttribute) eObject;
            if (eAttribute.getEContainingClass() != null) {
                return Optional.of(AsmUtils.getAttributeFQName(eAttribute));
            }
        }

        // EReference
        if (eObject instanceof EReference) {
            EReference eReference = (EReference) eObject;
            if (eReference.getEContainingClass() != null) {
                return Optional.of(AsmUtils.getReferenceFQName(eReference));
            }
        }

        // EOperation
        if (eObject instanceof EOperation) {
            EOperation eOperation = (EOperation) eObject;
            if (eOperation.getEContainingClass() != null) {
                return Optional.of(AsmUtils.getOperationFQName(eOperation));
            }
        }

        // EEnumLiteral
        if (eObject instanceof EEnumLiteral) {
            EEnumLiteral eEnumLiteral = (EEnumLiteral) eObject;
            EEnum eEnum = eEnumLiteral.getEEnum();
            if (eEnum != null && eEnum.getEPackage() != null) {
                return Optional.of(AsmUtils.getClassifierFQName(eEnum) +
                        AsmUtils.FEATURE_SEPARATOR + eEnumLiteral.getName());
            }
        }

        // EParameter (operation parameter)
        if (eObject instanceof EParameter) {
            EParameter eParameter = (EParameter) eObject;
            EOperation eOperation = eParameter.getEOperation();
            if (eOperation != null && eOperation.getEContainingClass() != null) {
                String operationFqn = AsmUtils.getOperationFQName(eOperation);
                String paramName = eParameter.getName();
                if (paramName != null && !paramName.trim().isEmpty()) {
                    return Optional.of(operationFqn + "/" + paramName);
                }
            }
        }

        // EAnnotation (uses source as identifier)
        if (eObject instanceof EAnnotation) {
            EAnnotation eAnnotation = (EAnnotation) eObject;
            EModelElement container = eAnnotation.getEModelElement();
            if (container != null) {
                Optional<String> containerFqn = computeFqn(container);
                if (containerFqn.isPresent()) {
                    String source = eAnnotation.getSource();
                    if (source != null && !source.trim().isEmpty()) {
                        // Use last part of source URI as annotation name
                        String annotationName = source.contains("/")
                                ? source.substring(source.lastIndexOf('/') + 1)
                                : source;
                        return Optional.of(containerFqn.get() + "@" + annotationName);
                    }
                }
            }
        }

        // EStringToStringMapEntry (annotation detail)
        if (eObject.eClass().getName().equals("EStringToStringMapEntry")) {
            EObject container = eObject.eContainer();
            if (container instanceof EAnnotation) {
                EAnnotation eAnnotation = (EAnnotation) container;
                Optional<String> annotationFqn = computeFqn(eAnnotation);
                if (annotationFqn.isPresent()) {
                    // Get the key from the map entry
                    Object keyValue = eObject.eGet(eObject.eClass().getEStructuralFeature("key"));
                    if (keyValue instanceof String) {
                        String key = (String) keyValue;
                        if (key != null && !key.isEmpty()) {
                            return Optional.of(annotationFqn.get() + "/" + key);
                        }
                    }
                }
            }
        }

        // Fallback: use URI
        return Optional.of(EcoreUtil.getURI(eObject).toString());
    }
}
