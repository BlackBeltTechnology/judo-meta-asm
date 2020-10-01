package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AsmUtilsCache {

    private final Map<String, Optional<EClassifier>> classifiersByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EReference>> referencesByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EAttribute>> attributesByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EOperation>> operationsByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EClass>> mappedEntityTypes = new ConcurrentHashMap<>();

    private final Map<String, Optional<EClass>> resolvedExposedByAnnotationValues = new ConcurrentHashMap<>();

    private final Map<String, EList<EClass>> actorTypesOfOperations = new ConcurrentHashMap<>();

    private final Map<String, Optional<EAttribute>> mappedAttributes = new ConcurrentHashMap<>();

    private final Map<String, Optional<EReference>> mappedReferences = new ConcurrentHashMap<>();

    private final Map<String, Optional<? extends ENamedElement>> operationOwners = new ConcurrentHashMap<>();

    private EList<EClass> allActorTypes;

    private Optional<EPackage> model;

    public void clear() {
        classifiersByFqName.clear();
        referencesByFqName.clear();
        attributesByFqName.clear();
        operationsByFqName.clear();
        mappedEntityTypes.clear();
        resolvedExposedByAnnotationValues.clear();
        actorTypesOfOperations.clear();
        mappedAttributes.clear();
        mappedReferences.clear();
        operationOwners.clear();
        allActorTypes = null;
        model = null;
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

    public Map<String, Optional<EClass>> getMappedEntityTypes() {
        return mappedEntityTypes;
    }

    public Map<String, Optional<EClass>> getResolvedExposedByAnnotationValues() {
        return resolvedExposedByAnnotationValues;
    }

    public Map<String, EList<EClass>> getActorTypesOfOperations() {
        return actorTypesOfOperations;
    }

    public Map<String, Optional<EAttribute>> getMappedAttributes() {
        return mappedAttributes;
    }

    public Map<String, Optional<EReference>> getMappedReferences() {
        return mappedReferences;
    }

    public Map<String, Optional<? extends ENamedElement>> getOperationOwners() {
        return operationOwners;
    }

    public EList<EClass> getAllActorTypes() {
        return allActorTypes != null ? ECollections.unmodifiableEList(allActorTypes) : null;
    }

    public void setAllActorTypes(EList<EClass> allActorTypes) {
        this.allActorTypes = allActorTypes;
    }

    public Optional<EPackage> getModel() {
        return model;
    }

    public void setModel(Optional<EPackage> model) {
        this.model = model;
    }
}
