package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AsmUtilsCache {

    private final Map<String, Optional<EClassifier>> classifiersByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EReference>> referencesByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EAttribute>> attributesByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EOperation>> operationsByFqName = new ConcurrentHashMap<>();

    public void clear() {
        classifiersByFqName.clear();
        referencesByFqName.clear();
        attributesByFqName.clear();
        operationsByFqName.clear();
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
}
