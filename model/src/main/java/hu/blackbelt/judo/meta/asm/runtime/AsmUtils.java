package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.UniqueEList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.empty;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAnnotationBuilder;

public class AsmUtils {

    public static final String EXTENDED_METADATA_URI = "http://blackbelt.hu/judo/meta/ExtendedMetadata";
    public static final String EXTENDED_METADATA_DETAILS_VALUE_KEY = "value";

    public static final String NAMESPACE_SEPARATOR = ".";
    public static final String FEATURE_SEPARATOR = "#";
    public static final String OPERATION_SEPARATOR = "#";

    private static final List<String> INTEGER_TYPES = Arrays.asList("byte", "short", "int", "long",
            "java.math.BigInteger", "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long");
    private static final List<String> DECIMAL_TYPES = Arrays.asList("float", "double",
            "java.math.BigDecimal", "java.lang.Float", "java.lang.Double");
    private static final List<String> DATE_TYPES = Arrays.asList("java.sql.Date",
            "java.time.LocalDate", "org.joda.time.LocalDate");
    private static final List<String> TIMESTAMP_TYPES = Arrays.asList("java.sql.Timestamp",
            "java.time.LocalDateTime", "java.time.OffsetDateTime", "java.time.ZonedDateTime",
            "org.joda.time.DateTime", "org.joda.time.LocalDateTime", "org.joda.time.MutableDateTime");
    private static final List<String> STRING_TYPES = Arrays.asList("java.lang.String");
    private static final List<String> BOOLEAN_TYPES = Arrays.asList("boolean", "java.lang.Boolean");
    private static final List<String> BYTE_ARRAY_TYPES = Arrays.asList("byte[]", "java.sql.Blob");
    private static final List<String> TEXT_TYPES = Arrays.asList("java.sql.Clob");

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AsmUtils.class);

    private static final String EXPOSED_BY_ANNOTATION_NAME = "exposedBy";

    private final ResourceSet resourceSet;

    private final AsmUtilsCache cache = new AsmUtilsCache();

    public AsmUtils(final ResourceSet resourceSet) {
        this.resourceSet = resourceSet;
    }

    /**
     * Get the fully qualified name of a package.
     *
     * @param ePackage package
     * @return fully qualified name
     */
    public static String getPackageFQName(final EPackage ePackage) {
        EPackage pack = ePackage.getESuperPackage();
        String fqName = "";
        while (pack != null) {
            fqName = pack.getName() + NAMESPACE_SEPARATOR + fqName;
            pack = pack.getESuperPackage();
        }

        return fqName + ePackage.getName();
    }

    /**
     * Get fully qualified name of a classifier.
     *
     * @param eClassifier classifier
     * @return fully qualified name
     */
    public static String getClassifierFQName(final EClassifier eClassifier) {
        return getPackageFQName(eClassifier.getEPackage()) + NAMESPACE_SEPARATOR + eClassifier.getName();
    }

    /**
     * Get fully qualified name of an attribute.
     *
     * @param eAttribute attribute
     * @return fully qualified name
     */
    public static String getAttributeFQName(final EAttribute eAttribute) {
        return getClassifierFQName(eAttribute.getEContainingClass()) + FEATURE_SEPARATOR + eAttribute.getName();
    }

    /**
     * Get fully qualified name of a reference.
     *
     * @param eReference reference
     * @return fully qualified name
     */
    public static String getReferenceFQName(EReference eReference) {
        return getClassifierFQName(eReference.getEContainingClass()) + FEATURE_SEPARATOR + eReference.getName();
    }

    /**
     * Get fully qualified name of an operation.
     *
     * @param eOperation operation
     * @return fully qualified name
     */
    public static String getOperationFQName(final EOperation eOperation) {
        return getClassifierFQName(eOperation.getEContainingClass()) + OPERATION_SEPARATOR + eOperation.getName();
    }

    /**
     * Resolve a name to get a classifier. Fully qualified names are checked first, searching by name in second turn.
     *
     * @param fqName name to resolve
     * @return resolved classifier (if found)
     */
    public Optional<EClassifier> resolve(final String fqName) {
        if (cache.getClassifiersByFqName().isEmpty()) {
            cache.getClassifiersByFqName().putAll(all(EClassifier.class)
                    .collect(Collectors.toMap(c -> getClassifierFQName(c), c -> Optional.of(c))));
        }
        if (cache.getClassifiersByFqName().containsKey(fqName)) {
            return cache.getClassifiersByFqName().get(fqName);
        } else {
            final Optional<EClassifier> resolved = all(EClassifier.class)
                    .filter(c -> Objects.equals(fqName, getClassifierFQName(c)))
                    .findAny();

            if (resolved.isPresent()) {
                cache.getClassifiersByFqName().put(fqName, resolved);
                return resolved;
            } else {
                log.warn("EClassifier by fully qualified name '" + fqName + "' not found, trying to resolve by name only");
                final Optional<EClassifier> resolvedByNameOnly = all(EClassifier.class)
                        .filter(c -> Objects.equals(fqName, c.getName()))
                        .findAny();
                cache.getClassifiersByFqName().put(fqName, resolvedByNameOnly);
                return resolvedByNameOnly;
            }
        }
    }

    /**
     * Resolve a name to get a reference.
     *
     * @param fqName name to resolve
     * @return resolved reference (if found)
     */
    public Optional<EReference> resolveReference(final String fqName) {
        if (cache.getReferencesByFqName().isEmpty()) {
            cache.getReferencesByFqName().putAll(all(EReference.class)
                    .collect(Collectors.toMap(r -> getReferenceFQName(r), r -> Optional.of(r))));
        }
        if (cache.getReferencesByFqName().containsKey(fqName)) {
            return cache.getReferencesByFqName().get(fqName);
        } else {
            final Optional<EReference> result = all(EReference.class)
                    .filter(r -> Objects.equals(fqName, getReferenceFQName(r)))
                    .findAny();
            cache.getReferencesByFqName().put(fqName, result);
            return result;
        }
    }

    /**
     * Resolve a name to get an attribute.
     *
     * @param fqName name to resolve
     * @return resolved attribute (if found)
     */
    public Optional<EAttribute> resolveAttribute(final String fqName) {
        if (cache.getAttributesByFqName().isEmpty()) {
            cache.getAttributesByFqName().putAll(all(EAttribute.class)
                    .collect(Collectors.toMap(a -> getAttributeFQName(a), a -> Optional.of(a))));
        }
        if (cache.getAttributesByFqName().containsKey(fqName)) {
            return cache.getAttributesByFqName().get(fqName);
        } else {
            final Optional<EAttribute> result = all(EAttribute.class)
                    .filter(a -> Objects.equals(fqName, getAttributeFQName(a)))
                    .findAny();
            cache.getAttributesByFqName().put(fqName, result);
            return result;
        }
    }

    /**
     * Resolve a name to get an operation.
     *
     * @param fqName name to resolve
     * @return resolved operation (if found)
     */
    public Optional<EOperation> resolveOperation(final String fqName) {
        if (cache.getOperationsByFqName().isEmpty()) {
            cache.getOperationsByFqName().putAll(all(EOperation.class)
                    .collect(Collectors.toMap(o -> getOperationFQName(o), o -> Optional.of(o))));
        }
        if (cache.getOperationsByFqName().containsKey(fqName)) {
            return cache.getOperationsByFqName().get(fqName);
        } else {
            final Optional<EOperation> result = all(EOperation.class)
                    .filter(o -> Objects.equals(fqName, getOperationFQName(o)))
                    .findAny();
            cache.getOperationsByFqName().put(fqName, result);
            return result;
        }
    }

    /**
     * Returns the EClass of the given fully qualified name.
     *
     * @param fqName Fully qualified name
     * @return the EClass instance of the given name (if found and resolved)
     */
    public Optional<EClass> getClassByFQName(final String fqName) {
        final Optional<EClassifier> classifier = resolve(fqName);
        if (classifier.isPresent()) {
            final EClassifier cl = classifier.get();
            if (cl instanceof EClass) {
                return Optional.of((EClass) cl);
            } else {
                log.error("Fully qualified name represents no EClass: {}", fqName);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get source URI of an annotation name.
     *
     * @param annotationName annotation name
     * @return source URI
     */
    public static String getAnnotationUri(final String annotationName) {
        return EXTENDED_METADATA_URI + "/" + annotationName;
    }

    /**
     * Get single JUDO extension annotation of a given Ecore model element by annotation name.
     * <p>
     * Annotation will be added if createIfNotExists parameter is <code>true</code> and it not exists yet.
     *
     * @param eModelElement     model element
     * @param annotationName    annotation name
     * @param createIfNotExists create annotation is not exists yet
     * @return JUDO extension annotation (or null if createIfNotExists flag is <code>false</code> and annotation not exists yet)
     */
    public static Optional<EAnnotation> getExtensionAnnotationByName(final EModelElement eModelElement, final String annotationName, final boolean createIfNotExists) {
        final Optional<EAnnotation> annotation = getExtensionAnnotationsAsStreamByName(eModelElement, annotationName).findAny();
        if (!annotation.isPresent() && createIfNotExists) {
            final EAnnotation a = newEAnnotationBuilder().withSource(getAnnotationUri(annotationName)).build();
            eModelElement.getEAnnotations().add(a);
            return Optional.of(a);
        } else {
            return annotation;
        }
    }

    /**
     * Get list of JUDO extension annotations of a given Ecore model element by annotation name.
     *
     * @param eModelElement  model element
     * @param annotationName annotation name
     * @return JUDO extension annotation
     */
    public static EList<EAnnotation> getExtensionAnnotationListByName(final EModelElement eModelElement, final String annotationName) {
        return new BasicEList<>(getExtensionAnnotationsAsStreamByName(eModelElement, annotationName).collect(Collectors.toList()));
    }

    /**
     * Add new JUDO extension annotation to a given model element with a given value (if not exists yet).
     *
     * @param eModelElement  model element to which annotation value is added
     * @param annotationName annotation name
     * @param value          annotation value
     */
    public static boolean addExtensionAnnotation(final EModelElement eModelElement, final String annotationName, final String value) {
        final String sourceUri = getAnnotationUri(annotationName);

        final Optional<EAnnotation> annotation = getExtensionAnnotationsAsStreamByName(eModelElement, annotationName)
                .filter(a -> Objects.equals(a.getDetails().get(EXTENDED_METADATA_DETAILS_VALUE_KEY), value))
                .findAny();

        if (annotation.isPresent()) {
            log.trace("Annotation (prefix: {}, value: {}) is already added to model element {}", new Object[]{annotationName, value, eModelElement});
            return false;
        } else {
            final EAnnotation newAnnotation = newEAnnotationBuilder()
                    .withSource(sourceUri)
                    .build();
            newAnnotation.getDetails().put(EXTENDED_METADATA_DETAILS_VALUE_KEY, value);
            eModelElement.getEAnnotations().add(newAnnotation);
            return true;
        }
    }

    /**
     * Add new JUDO extension annotation to a given model element with a given value (if not exists yet).
     *
     * @param eModelElement  model element to which annotation value is added
     * @param annotationName annotation name
     * @param details        annotation details
     */
    public static void addExtensionAnnotationDetails(final EModelElement eModelElement, final String annotationName, final Map<String, String> details) {
        final String sourceUri = getAnnotationUri(annotationName);

        Optional<EAnnotation> annotation = getExtensionAnnotationsAsStreamByName(eModelElement, annotationName)
                .findAny();

        if (!annotation.isPresent()) {
            annotation = Optional.of(newEAnnotationBuilder()
                    .withSource(sourceUri)
                    .build());
            eModelElement.getEAnnotations().add(annotation.get());
        }

        annotation.get().getDetails().putAll(details);
    }

    /**
     * Get annotated Ecore class for a given annotation.
     *
     * @param annotation annotation
     * @return owner Ecore class
     */
    @Deprecated
    public Optional<EClass> getAnnotatedClass(final EAnnotation annotation) {
        final EModelElement eModelElement = annotation.getEModelElement();
        if (eModelElement != null && eModelElement instanceof EClass) {
            return Optional.of((EClass) eModelElement);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get annotated Ecore attribute for a given annotation.
     *
     * @param annotation annotation
     * @return owner Ecore attribute
     */
    @Deprecated
    public Optional<EAttribute> getAnnotatedAttribute(final EAnnotation annotation) {
        final EModelElement eModelElement = annotation.getEModelElement();
        if (eModelElement != null && eModelElement instanceof EAttribute) {
            return Optional.of((EAttribute) eModelElement);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get annotated Ecore reference for a given annotation.
     *
     * @param annotation annotation
     * @return owner Ecore reference
     */
    @Deprecated
    public Optional<EReference> getAnnotatedReference(final EAnnotation annotation) {
        final EModelElement eModelElement = annotation.getEModelElement();
        if (eModelElement != null && eModelElement instanceof EReference) {
            return Optional.of((EReference) eModelElement);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get annotated Ecore operation for a given annotation.
     *
     * @param annotation annotation
     * @return owner Ecore operation
     */
    @Deprecated
    public Optional<EOperation> getAnnotatedOperation(final EAnnotation annotation) {
        final EModelElement eModelElement = annotation.getEModelElement();
        if (eModelElement != null && eModelElement instanceof EOperation) {
            return Optional.of((EOperation) eModelElement);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get annotated Ecore parameter for a given annotation.
     *
     * @param annotation annotation
     * @return owner Ecore parameter
     */
    @Deprecated
    public Optional<EParameter> getAnnotatedParameter(final EAnnotation annotation) {
        final EModelElement eModelElement = annotation.getEModelElement();
        if (eModelElement != null && eModelElement instanceof EParameter) {
            return Optional.of((EParameter) eModelElement);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the the Extension annotation's given element in map. If failNotFound true it logs a warning, otherwise
     * if the extension annotation or the given name not found returns null.
     *
     * @param eModelElement  The model element which is used to determinate
     * @param annotationName The entry name of extension annotation
     * @param logIfNotFound  When the extension or name in details not found log warn.
     * @return The value of annotation (<code>null</code> value is returned if key is found but value is not set)
     */
    public static Optional<String> getExtensionAnnotationValue(final EModelElement eModelElement, final String annotationName, final boolean logIfNotFound) {
        final Optional<EAnnotation> eAnnotation = getExtensionAnnotationByName(eModelElement, annotationName, false);
        if (eAnnotation.isPresent()) {
            final String value = eAnnotation.get().getDetails().get(EXTENDED_METADATA_DETAILS_VALUE_KEY);
            if (value == null && logIfNotFound) {
                log.warn("No annotation value {} found on element {}", annotationName, eModelElement);
            }
            return Optional.ofNullable(value);
        } else {
            if (logIfNotFound) {
                log.warn("No annotation {} found on element {}", annotationName, eModelElement);
            }
            return empty();
        }
    }

    /**
     * Get the the Extension annotation's given element in map if key of map entry is specified (as something other than "value"). If failNotFound true it log a warning, otherwise
     * if the extension annotation or the given name not found returns null.
     *
     * @param eModelElement  The model element which is used to determinate
     * @param annotationName The entry name of extension annotation
     * @param attributeName  Name of annotation attribute (key of details)
     * @param logIfNotFound  When the extension or name in details not found log warn.
     * @return The value of annotation (<code>null</code> value is returned if key is found but value is not set)
     */
    public static Optional<String> getExtensionAnnotationCustomValue(final EModelElement eModelElement, final String annotationName, final String attributeName, final boolean logIfNotFound) {
        final Optional<EAnnotation> eAnnotation = getExtensionAnnotationByName(eModelElement, annotationName, false);
        if (eAnnotation.isPresent()) {
            final String value = eAnnotation.get().getDetails().get(attributeName);
            if (value == null && logIfNotFound) {
                log.warn("No annotation value {} found on element {}", annotationName, eModelElement);
            }
            return Optional.ofNullable(value);
        } else {
            if (logIfNotFound) {
                log.warn("No annotation {} found on element {}", annotationName, eModelElement);
            }
            return empty();
        }
    }

    /**
     * Check the given element's extension annotation on the given name is true. When the element have no
     * extension annotation returns false.
     *
     * @param eModelElement The model element which is used to determinate
     * @param name          The entry name of extension annotation
     * @return <code>true</code> if annotation value represents a Java true value, <code>false</code> otherwise
     */
    public static boolean annotatedAsTrue(final EModelElement eModelElement, final String name) {
        final Optional<String> value = getExtensionAnnotationValue(eModelElement, name, false);
        return value.isPresent() && Boolean.valueOf(value.get());
    }

    /**
     * Check the given element's extension annotation on the given name is false. When the element have no
     * extension annotation returns false.
     *
     * @param eModelElement The model element which is used to determinate
     * @param name          The entry name of extension annotation
     * @return <code>true</code> if annotation value represents a Java false value, <code>false</code> otherwise
     */
    public static boolean annotatedAsFalse(final EModelElement eModelElement, final String name) {
        final Optional<String> value = getExtensionAnnotationValue(eModelElement, name, false);
        return value.isPresent() && !Boolean.valueOf(value.get());
    }

    /**
     * Get mapped entity type of a mapped transfer object type.
     *
     * @param eClass class of mapped transfer object type
     * @return mapped entity type (or null if no mappedEntityType annotation found nor it represents a valid entity type)
     */
    public Optional<EClass> getMappedEntityType(final EClass eClass) {
        final Optional<String> mappedEntityTypeFQName = getExtensionAnnotationValue(eClass, "mappedEntityType", false);
        if (mappedEntityTypeFQName.isPresent()) {
            final Optional<EClass> entityType = getClassByFQName(mappedEntityTypeFQName.get());
            if (entityType.isPresent()) {
                if (isEntityType(entityType.get())) {
                    return entityType;
                } else {
                    log.error("Invalid entity type: {}", mappedEntityTypeFQName.get());
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Check if an operation is stateless.
     *
     * @param eOperation operation
     * @return <code>true</code> if operation is marked as stateless, <code>false</code> otherwise
     */
    public static boolean isStateless(final EOperation eOperation) {
        return annotatedAsFalse(eOperation, "stateful");
    }

    /**
     * Check if an operation is stateful.
     *
     * @param eOperation operation
     * @return <code>true</code> if operation is marked as stateful, <code>false</code> otherwise
     */
    public static boolean isStateful(final EOperation eOperation) {
        return annotatedAsTrue(eOperation, "stateful");
    }

    /**
     * Check if an operation is bound.
     *
     * @param eOperation operation
     * @return <code>true</code> if operation is bound (to transfer object type), <code>false</code> otherwise
     */
    public static boolean isBound(final EOperation eOperation) {
        return annotatedAsTrue(eOperation, "bound");
    }

    /**
     * Check if an operation is unbound.
     * <p>
     * Unbound operations must be interfaces and container class must not be mapped transfer object type nor nested class.
     *
     * @param eOperation operation
     * @return <code>true</code> if operation is unbound, <code>false</code> otherwise.
     */
    public static boolean isUnbound(final EOperation eOperation) {
        return annotatedAsFalse(eOperation, "bound");
    }

    /**
     * Check if a class represents an entity type.
     *
     * @param eClass class
     * @return <code>true</code> if class is an entity type, <code>false</code> otherwise
     */
    public static boolean isEntityType(final EClass eClass) {
        return annotatedAsTrue(eClass, "entity");
    }

    /**
     * Check if a class represents a mapped transfer object type.
     *
     * @param eClass class
     * @return <code>true</code> if class is a mapped transfer object type, <code>false</code> otherwise
     */
    public boolean isMappedTransferObjectType(final EClass eClass) {
        return getMappedEntityType(eClass).isPresent();
    }

    /**
     * Check if a class represents an actor type.
     *
     * @param eClass class
     * @return <code>true</code> if class is an actor type, <code>false</code> otherwise
     */
    public static boolean isActorType(final EClass eClass) {
        return annotatedAsTrue(eClass, "actorType");
    }

    /**
     * Get resolved exposed by annotation.
     * <p>
     * Exposed by must be an access point.
     *
     * @param eAnnotation annotation
     * @return access point (or null if no exposedBy annotation found nor it is a valid access point)
     */
    public Optional<EClass> getResolvedExposedBy(final EAnnotation eAnnotation) {
        if (Objects.equals(eAnnotation.getSource(), getAnnotationUri(EXPOSED_BY_ANNOTATION_NAME))) {
            if (eAnnotation.getDetails().containsKey(EXTENDED_METADATA_DETAILS_VALUE_KEY)) {
                final String exposedByFqName = eAnnotation.getDetails().get(EXTENDED_METADATA_DETAILS_VALUE_KEY);
                final Optional<EClass> resolvedExposedBy = getClassByFQName(exposedByFqName);
                if (resolvedExposedBy.isPresent()) {
                    if (isActorType(resolvedExposedBy.get())) {
                        return resolvedExposedBy;
                    } else {
                        log.error("Exposed by is not an access point: {}", exposedByFqName);
                        return Optional.empty();
                    }
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get list of actor types exposing an operation.
     *
     * @param eOperation operation
     * @return list of actor types
     */
    public EList<EClass> getActorTypesOfOperation(final EOperation eOperation) {
        return new BasicEList<>(eOperation.getEAnnotations().stream()
                .map(a -> getResolvedExposedBy(a))
                .filter(exposedBy -> exposedBy.isPresent())
                .map(exposedBy -> exposedBy.get())
                .collect(Collectors.toList()));
    }

    /**
     * Returns the given attribute's mapped attribute when extension annotation is given and attribute is presented the parent's class and
     * the given attribute name also.
     *
     * @param type The given EAttibute type.
     * @return mapped attribute
     */
    public Optional<EAttribute> getMappedAttribute(EAttribute type) {
        Optional<String> mappedAttributeName = getExtensionAnnotationValue(type, "binding", false);
        Optional<EClass> mappedEntityType = getMappedEntityType(type.getEContainingClass());
        if (mappedAttributeName.isPresent()) {
            if (!mappedEntityType.isPresent()) {
                log.warn("Mapped attribute container class is not mapped: " + getAttributeFQName(type));
                return empty();
            } else {
                if (mappedEntityType.get().getEStructuralFeature(mappedAttributeName.get()) instanceof EAttribute) {
                    return Optional.of((EAttribute) mappedEntityType.get().getEStructuralFeature(mappedAttributeName.get()));
                } else {
                    log.warn("The given mapped alias is not attribute type: " + getAttributeFQName(type));
                    return empty();
                }
            }
        } else {
            return empty();
        }
    }

    /**
     * Returns the given reference's mapped reference when extension annotation is given and reference is presented the parent's class and
     * the given reference name also.
     *
     * @param type The given EReference type.
     * @return mapped reference
     */
    public Optional<EReference> getMappedReference(EReference type) {
        Optional<String> mappedReferenceName = getExtensionAnnotationValue(type, "binding", false);
        Optional<EClass> mappedEntityType = getMappedEntityType(type.getEContainingClass());
        if (mappedReferenceName.isPresent()) {
            if (!mappedEntityType.isPresent()) {
                log.warn("Mapped reference container class is not mapped: " + getReferenceFQName(type));
                return empty();
            } else {
                if (mappedEntityType.get().getEStructuralFeature(mappedReferenceName.get()) instanceof EReference) {
                    return Optional.of((EReference) mappedEntityType.get().getEStructuralFeature(mappedReferenceName.get()));
                } else {
                    log.warn("The given mapped alias is not attribute type: " + getReferenceFQName(type));
                    return empty();
                }
            }
        } else {
            return empty();
        }
    }

    /**
     * Check if a given data type is integer.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of integer data type, <code>false</code> otherwise
     */
    public static boolean isInteger(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && INTEGER_TYPES.contains(instanceClassName);
    }

    /**
     * Check if a given data type is decimal.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of decimal data type, <code>false</code> otherwise
     */
    public static boolean isDecimal(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && DECIMAL_TYPES.contains(instanceClassName);
    }

    /**
     * Check if a given data type is numeric.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of numeric data type, <code>false</code> otherwise
     */
    public static boolean isNumeric(final EDataType eDataType) {
        return isInteger(eDataType) || isDecimal(eDataType);
    }

    /**
     * Check if a given data type is boolean.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of boolean data type, <code>false</code> otherwise
     */
    public static boolean isBoolean(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && BOOLEAN_TYPES.contains(instanceClassName);
    }

    /**
     * Check if a given data type is string.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of string data type, <code>false</code> otherwise
     */
    public static boolean isString(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && STRING_TYPES.contains(instanceClassName);
    }

    /**
     * Check if a given data type is (free) text.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of text data type, <code>false</code> otherwise
     */
    public static boolean isText(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && TEXT_TYPES.contains(instanceClassName);
    }

    /**
     * Check if a given data type is byte array.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of byte array data type, <code>false</code> otherwise
     */
    public static boolean isByteArray(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && BYTE_ARRAY_TYPES.contains(instanceClassName);
    }

    /**
     * Check if a given data type is date.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of date data type, <code>false</code> otherwise
     */
    public static boolean isDate(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        if ("java.util.Date".equals(instanceClassName)) {
            return !isTimestampJavaUtilDate(eDataType);
        } else {
            return instanceClassName != null && DATE_TYPES.contains(instanceClassName);
        }
    }

    /**
     * Check if a given data type is timestamp.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of timestamp data type, <code>false</code> otherwise
     */
    public static boolean isTimestamp(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        if ("java.util.Date".equals(instanceClassName)) {
            return isTimestampJavaUtilDate(eDataType);
        } else {
            return instanceClassName != null && TIMESTAMP_TYPES.contains(instanceClassName);
        }
    }

    /**
     * Check if a given data type is enumeration.
     *
     * @param eDataType data type
     * @return <code>true</code> in case of enumeration data type, <code>false</code> otherwise
     */
    public static boolean isEnumeration(final EDataType eDataType) {
        return eDataType instanceof EEnum;
    }

    /**
     * Check if a given attribute is identifier.
     *
     * @param eAttribute attribute
     * @return <code>true</code> if attribute is marked as identifier, <code>false</code> otherwise
     */
    public static boolean isIdentifier(final EAttribute eAttribute) {
        return annotatedAsTrue(eAttribute, "identifier");
    }
    
    /**
     * Get all access points.
     *
     * @return access points
     */
    public EList<EClass> getAllActorTypes() {
        return new BasicEList<>(all(EClass.class)
                .filter(c -> isActorType(c))
                .collect(Collectors.toList()));
    }

    /**
     * Add exposed by annotation to (both mapped an unmapped) transfer object types.
     *
     * @param transferObjectType transfer object type
     * @param actorTypeFqName    fully qualified name of the actor type
     * @param includeAccess      include access relations
     */
    void addExposedByAnnotationToTransferObjectType(final EClass transferObjectType, final String actorTypeFqName, final int level, final boolean includeAccess) {
        if (log.isDebugEnabled()) {
            log.debug(pad(level, "  - transfer object type: {}"), getClassifierFQName(transferObjectType));
        }
        final boolean exposedByAdded = addExtensionAnnotation(transferObjectType, EXPOSED_BY_ANNOTATION_NAME, actorTypeFqName);
        transferObjectType.getEAllAttributes().stream().forEach(a -> addExtensionAnnotation(a, EXPOSED_BY_ANNOTATION_NAME, actorTypeFqName));
        transferObjectType.getEAllReferences().stream()
                .filter(r -> includeAccess && annotatedAsTrue(r, "access") ||
                        (annotatedAsTrue(r, "embedded") || isMappedTransferObjectType(r.getEContainingClass())) && !annotatedAsTrue(r, "access"))
                .forEach(r -> {
                    final boolean added = addExtensionAnnotation(r, EXPOSED_BY_ANNOTATION_NAME, actorTypeFqName);
                    if (r.isContainment() && added) {
                        addExposedByAnnotationToTransferObjectType(r.getEReferenceType(), actorTypeFqName, level + 1, false);
                    }
                });
        if (exposedByAdded) {
            transferObjectType.getEAllSuperTypes().forEach(superType -> addExposedByAnnotationToTransferObjectType(superType, actorTypeFqName, level + 1, false));
        }

        if (isMappedTransferObjectType(transferObjectType)) {
            addExtensionAnnotation(getMappedEntityType(transferObjectType).get(), EXPOSED_BY_ANNOTATION_NAME, actorTypeFqName);
        }

        getAllOperationImplementations(transferObjectType).stream()
                .filter(o -> exposedByAdded)
                .forEach(operation -> addExposedByAnnotationToTransferOperation(transferObjectType, actorTypeFqName, operation, level));
    }

    private void addExposedByAnnotationToTransferOperation(final EClass transferObjectType, final String actorTypeFqName, final EOperation operation, final int level) {
        if (log.isDebugEnabled()) {
            log.debug(pad(level, "    - operation: {}"), getOperationFQName(operation));
        }

        if (getBehaviour(operation)
                .filter(b -> OperationBehaviour.GET_PRINCIPAL.equals(b) && !EcoreUtil.equals(transferObjectType, operation.getEContainingClass()))
                .isPresent()) {
            if (log.isDebugEnabled()) {
                log.debug(pad(level, "    - GET_PRINCIPAL operation is exposed by it container transfer object type only"));
            }
            return;
        }

        addExtensionAnnotation(operation, EXPOSED_BY_ANNOTATION_NAME, actorTypeFqName);

        operation.getEParameters().forEach(inputParameter -> {
            if (log.isDebugEnabled()) {
                log.debug(pad(level, "      - input parameter ({}): {}"), inputParameter.getName(), getClassifierFQName(inputParameter.getEType()));
            }
            final EClassifier type = inputParameter.getEType();
            if (type instanceof EClass) {
                addExtensionAnnotation(inputParameter, EXPOSED_BY_ANNOTATION_NAME, actorTypeFqName);
                addExposedByAnnotationToTransferObjectType((EClass) inputParameter.getEType(), actorTypeFqName, level + 1, false);
            } else {
                log.error("Input parameters must be transfer object types (EClass)");
            }
        });
        if (operation.getEType() != null) {
            if (log.isDebugEnabled()) {
                log.debug(pad(level, "        - output parameter ({}): {}"), getOutputParameterName(operation).orElse(""), getClassifierFQName(operation.getEType()));
            }
            final EClassifier type = operation.getEType();
            if (type instanceof EClass) {
                addExtensionAnnotation(operation, EXPOSED_BY_ANNOTATION_NAME, actorTypeFqName);
                addExposedByAnnotationToTransferObjectType((EClass) operation.getEType(), actorTypeFqName, level + 1, false);
            } else {
                log.error("Output parameter must be transfer object type (EClass)");
            }
        }
        operation.getEExceptions().forEach(faultParameter -> {
            if (log.isDebugEnabled()) {
                log.debug(pad(level, "        - fault parameter ({}): {}"), faultParameter.getName(), getClassifierFQName(faultParameter));
            }
            if (faultParameter instanceof EClass) {
                addExposedByAnnotationToTransferObjectType((EClass) faultParameter, actorTypeFqName, level + 1, false);
            } else {
                log.error("Fault parameters must be transfer object types (EClass)");
            }
        });
    }

    /**
     * Decorate model elements with annotations required to process ASM model (ie generating OpenAPI model).
     */
    public void enrichWithAnnotations() {
        getAllActorTypes().forEach(actorType -> {
            final String actorTypeFqName = getClassifierFQName(actorType);
            if (log.isDebugEnabled()) {
                log.debug("Actor type: {}", actorTypeFqName);
            }

            all(EClass.class)
                    .filter(ap -> getExtensionAnnotationListByName(ap, "actor").stream()
                            .anyMatch(a -> a.getDetails().get("name") != null && a.getDetails().get("name").replace("::", ".").equals(getClassifierFQName(actorType))))
                    .forEach(accessPoint -> addExposedByAnnotationToTransferObjectType(accessPoint, actorTypeFqName, 0, false));
            addExposedByAnnotationToTransferObjectType(actorType, actorTypeFqName, 0, true);
        });
    }

    /**
     * Check if the given data type represents timestamp.
     *
     * @param eDataType data type (with instanceClassName = "java.util.Date")
     * @return <code>true</code> if timestamp is represented by data type, <code>false</code> (default) if not (or data type is not temporal).
     */
    static boolean isTimestampJavaUtilDate(final EDataType eDataType) {
        // TODO - check annotations of EDataType in ASM model, false by default
        return false;
    }

    /*
     * Get all contents with a type from resource set of a given EObject.
     *
     * @param eObject EObject in the resource set
     * @param clazz   type class for filtering
     * @param <T>     type for filtering
     * @return stream of contents
     */
    static <T> Stream<T> getAllContents(final EObject eObject, final Class<T> clazz) {
        final ResourceSet resourceSet = eObject.eResource().getResourceSet();
        final Iterable<Notifier> asmContents = resourceSet::getAllContents;
        return StreamSupport.stream(asmContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }

    /**
     * Get stream of source iterator.
     *
     * @param sourceIterator source iterator
     * @param <T>            type of source iterator
     * @return (serial) stream
     */
    static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        return asStream(sourceIterator, false);
    }

    /**
     * Get stream of source iterator.
     *
     * @param sourceIterator source iterator
     * @param parallel       flag controlling returned stream (serial or parallel)
     * @param <T>            type of source iterator
     * @return return serial (parallel = <code>false</code>) or parallel (parallel = <code>true</code>) stream
     */
    static <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    /**
     * Get all model elements.
     *
     * @param <T> generic type of model elements
     * @return model elements
     */
    <T> Stream<T> all() {
        return asStream((Iterator<T>) resourceSet.getAllContents(), false);
    }

    /**
     * Get model elements with specific type
     *
     * @param clazz class of model element types
     * @param <T>   specific type
     * @return all elements with clazz type
     */
    public <T> Stream<T> all(final Class<T> clazz) {
        if (cache.getElementsByType().containsKey(clazz)) {
            return cache.getElementsByType().get(clazz).stream();
        } else {
            final Collection<T> result = all().filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e).collect(Collectors.toList());
            cache.getElementsByType().put(clazz, result);
            return result.stream();
        }
    }


    /**
     * Get list of JUDO extension annotation of a given Ecore model element by annotation name.
     *
     * @param eModelElement  model element
     * @param annotationName annotation name
     * @return JUDO extension annotation
     */
    static Stream<EAnnotation> getExtensionAnnotationsAsStreamByName(final EModelElement eModelElement, final String annotationName) {
        return eModelElement.getEAnnotations().stream().filter(a -> getAnnotationUri(annotationName).equals(a.getSource()));
    }

    /**
     * Makes the model MappedTransferObjectType by adding the necessary annotations to the model and its references and super types if the model is an
     * entity type, and not a transfer object already
     *
     * @param eclass EClass to perform the operation on
     */
    public void createMappedTransferObjectTypeByEntityType(EClass eclass) {
        createMappedTransferObjectTypeByEntityType(eclass, null);
    }

    /**
     * Makes the model MappedTransferObjectType by adding the necessary annotations to the model and its references and super types if the model is an
     * entity type, and not a transfer object already
     *
     * @param eclass   EClass to perform the operation on
     * @param doneList contains the already processed EClasses
     */
    private void createMappedTransferObjectTypeByEntityType(EClass eclass, EList<EClass> doneList) {
        if (doneList == null) {
            doneList = new UniqueEList<EClass>();
        }

        if (isEntityType(eclass) && !isMappedTransferObjectType(eclass) && !doneList.contains(eclass)) {
            addExtensionAnnotation(eclass, "mappedEntityType", getPackageFQName(eclass.getEPackage()));
            doneList.add(eclass);

            //add annotation to all references and make them transfer object recursively
            for (EReference ereference : eclass.getEAllReferences()) {
                if (!getExtensionAnnotationByName(ereference, "binding", false).isPresent()) {
                    addExtensionAnnotation(ereference, "binding", ereference.getName());
                }

                createMappedTransferObjectTypeByEntityType(ereference.getEReferenceType(), doneList);
            }

            //add annotation to all attributes
            for (EAttribute eattribute : eclass.getEAllAttributes()) {
                if (!getExtensionAnnotationByName(eattribute, "binding", false).isPresent()) {
                    addExtensionAnnotation(eattribute, "binding", eattribute.getName());
                }

            }

            //call the function on all supertypes
            for (EClass superType : eclass.getEAllSuperTypes()) {
                createMappedTransferObjectTypeByEntityType(superType, doneList);
            }
        }
    }


    /**
     * Returns a safe conversion of the parameter string
     *
     * @param str the string to be converted
     * @return the converted string
     */
    public static String safeName(String str) {
        if (Arrays.asList(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                "continue", "default", "do", "double", "else", "enum", "exports", "extends",
                "final", "finally", "float", "for", "if", "implements", "import", "instanceof",
                "long", "module", "native", "new", "package", "private", "protected",
                "public", "requires", "return", "short", "static", "strictfp", "super",
                "switch", "synchronized", "this", "throw", "throws", "transient", "try",
                "void", "volatile", "while", "true", "null", "false", "var", "const", "goto", "class", "Class").contains(str)) {
            return str + "_";
        } else {
            return str;
        }
    }

    /**
     * Returns the setter method signature of a given attribute or reference
     *
     * @param eStructuralFeature attribute or reference
     * @return the setter method signature
     */
    public static String setterName(EStructuralFeature eStructuralFeature) {
        return "set" + safeName(
                eStructuralFeature.getName().substring(0, 1).toUpperCase() + eStructuralFeature.getName().substring(1));
    }

    /**
     * Returns the getter method signature of a given attribute or reference
     *
     * @param eStructuralFeature attribute or reference
     * @return the getter method signature
     */
    public static String getterName(EStructuralFeature eStructuralFeature) {
        if ("boolean".equals(eStructuralFeature.getEType().getInstanceClassName())) {
            return "is" + safeName(
                    eStructuralFeature.getName().substring(0, 1).toUpperCase()
                            + eStructuralFeature.getName().substring(1));
        } else {
            return "get" + safeName(
                    eStructuralFeature.getName().substring(0, 1).toUpperCase()
                            + eStructuralFeature.getName().substring(1));
        }
    }

    /**
     * Check if a reference is embedded in a transfer object type.
     *
     * @param eReference transfer object relation
     * @return <code>true</code> if relation is embedded, <code>false</code> otherwise
     */
    public static boolean isEmbedded(final EReference eReference) {
        return getExtensionAnnotationByName(eReference, "embedded", false).isPresent();
    }

    /**
     * Check if a new embedded instance of a given transfer object relation can be created.
     *
     * @param eReference transfer object relation
     * @return <code>true</code> if relation is embedded and creating a new instance is allowed by model, <code>false</code> otherwise
     */
    public static boolean isAllowedToCreateEmbeddedObject(final EReference eReference) {
        final Optional<String> value = getExtensionAnnotationCustomValue(eReference, "embedded", "create", false);
        return value.isPresent() && Boolean.valueOf(value.get());
    }

    /**
     * Check if an embedded instance of a given transfer object relation can be updated.
     *
     * @param eReference transfer object relation
     * @return <code>true</code> if relation is embedded and updating a new instance is allowed by model, <code>false</code> otherwise
     */
    public static boolean isAllowedToUpdateEmbeddedObject(final EReference eReference) {
        final Optional<String> value = getExtensionAnnotationCustomValue(eReference, "embedded", "update", false);
        return value.isPresent() && Boolean.valueOf(value.get());
    }

    /**
     * Check if an embedded instance of a given transfer object relation can be deleted.
     *
     * @param eReference transfer object relation
     * @return <code>true</code> if relation is embedded and deleting a new instance is allowed by model, <code>false</code> otherwise
     */
    public static boolean isAllowedToDeleteEmbeddedObject(final EReference eReference) {
        final Optional<String> value = getExtensionAnnotationCustomValue(eReference, "embedded", "delete", false);
        return value.isPresent() && Boolean.valueOf(value.get());
    }

    /**
     * Get default behaviour of a given operation (defined by annotation).
     *
     * @param operation operation
     * @return default behaviour
     */
    public static Optional<OperationBehaviour> getBehaviour(final EOperation operation) {
        final Optional<EAnnotation> annotation = getExtensionAnnotationByName(operation, "behaviour", false);
        if (annotation.isPresent()) {
            return Optional.ofNullable(OperationBehaviour.resolve(annotation.get().getDetails().get("type")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get owner of an operation with default behaviour. Owner is a named element (mapped transfer object type,
     * reference or operation parameter) referencing the given operation with a given name.
     *
     * @param operation operation
     * @return operation owner (referencer)
     */
    public Optional<? extends ENamedElement> getOwnerOfOperationWithDefaultBehaviour(final EOperation operation) {
        final Optional<EAnnotation> annotation = getExtensionAnnotationByName(operation, "behaviour", false);
        if (annotation.isPresent()) {
            final OperationBehaviour behaviour = OperationBehaviour.resolve(annotation.get().getDetails().get("type"));

            if (behaviour == null) {
                return Optional.empty();
            }

            final String ownerString = annotation.get().getDetails().get("owner");

            switch (behaviour) {
                case REFRESH:
                case UPDATE_INSTANCE:
                case VALIDATE_UPDATE:
                case DELETE_INSTANCE:
                case GET_TEMPLATE:
                case GET_PRINCIPAL: {
                    return resolve(ownerString);
                }
                case LIST:
                case CREATE_INSTANCE:
                case VALIDATE_CREATE:
                case SET_REFERENCE:
                case UNSET_REFERENCE:
                case ADD_REFERENCE:
                case REMOVE_REFERENCE:
                case GET_REFERENCE_RANGE: {
                    final Optional<EReference> resolvedReference = resolveReference(ownerString);
                    final Optional<EOperation> resolvedOperation = resolveOperation(ownerString);

                    if (resolvedReference.isPresent()) {
                        return resolvedReference;
                    } else if (resolvedOperation.isPresent()) {
                        return resolvedOperation;
                    } else {
                        throw new IllegalStateException("Invalid owner: " + ownerString);
                    }
                }
                default: {
                    final String[] parts = ownerString.split("#");
                    final Optional<EClassifier> classifier = resolve(parts[0]);
                    if (classifier.isPresent()) {
                        if (classifier.get() instanceof EClass) {
                            return ((EClass) classifier.get()).getEAllReferences().stream()
                                    .filter(r -> Objects.equals(r.getName(), parts[1]))
                                    .findAny();
                        } else {
                            throw new IllegalStateException("Invalid owner: " + ownerString);
                        }
                    } else {
                        throw new IllegalStateException("Unable to resolve owner: " + ownerString);
                    }
                }
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get class having implementation of the given operation.
     *
     * @param operation operation
     * @return class having implementation
     */
    public static Optional<EOperation> getImplementationClassOfOperation(final EOperation operation) {
        return getOperationImplementationByName(operation.getEContainingClass(), operation.getName());
    }

    public static boolean isAbstract(final EOperation operation) {
        return annotatedAsTrue(operation, "abstract");
    }

    public static Optional<String> getOutputParameterName(final EOperation operation) {
        return getExtensionAnnotationValue(operation, "outputParameterName", false);
    }

    public Optional<EPackage> getModel() {
        return all(EPackage.class).filter(p -> p.eContainer() == null).findAny();
    }

    public static Set<String> getAllOperationNames(final EClass clazz) {
        return clazz.getEAllOperations().stream()
                .map(op -> op.getName())
                .collect(Collectors.toSet());
    }

    private static EList<EOperation> getOperationsByName(final EClass clazz, final String operationName, final boolean ignoreAbstract) {
        final Optional<EOperation> operation = clazz.getEOperations().stream()
                .filter(op -> Objects.equals(op.getName(), operationName))
                .filter(op -> !ignoreAbstract || !AsmUtils.isAbstract(op))
                .findAny(); // at most one operations can be found because operation overloading is denied

        if (operation.isPresent()) {
            return ECollections.singletonEList(operation.get());
        } else {
            final EList<EOperation> inheritedOperations = new UniqueEList<>();
            inheritedOperations.addAll(clazz.getESuperTypes().stream()
                    .flatMap(s -> getOperationsByName(s, operationName, ignoreAbstract).stream())
                    .collect(Collectors.toSet()));
            return inheritedOperations;
        }
    }

    public static EList<EOperation> getOperationDeclarationsByName(final EClass clazz, final String operationName) {
        final EList<EOperation> operations = getOperationsByName(clazz, operationName, false);

        return ECollections.asEList(operations.stream().filter(o -> !operations.stream().anyMatch(sup -> !EcoreUtil.equals(o, sup) && o.isOverrideOf(sup))).collect(Collectors.toList()));
    }

    public static EList<EOperation> getAllOperationDeclarations(final EClass clazz, boolean ignoreOverrides) {
        final EList<EOperation> allOperationDeclarations = new UniqueEList<>();
        allOperationDeclarations.addAll(getAllOperationNames(clazz).stream()
                .flatMap(operationName -> getOperationDeclarationsByName(clazz, operationName).stream())
                .collect(Collectors.toSet()));
        if (ignoreOverrides) {
            return ECollections.asEList(allOperationDeclarations.stream().filter(o -> !allOperationDeclarations.stream().anyMatch(sup -> !EcoreUtil.equals(o, sup) && o.isOverrideOf(sup))).collect(Collectors.toList()));
        } else {
            return allOperationDeclarations;
        }
    }

    public static EList<EOperation> getOperationImplementationListByName(final EClass clazz, final String operationName) {
        final EList<EOperation> operationImplementations = new UniqueEList<>();
        operationImplementations.addAll(getOperationsByName(clazz, operationName, true).stream()
                .filter(op -> !AsmUtils.isAbstract(op))
                .collect(Collectors.toSet()));
        return operationImplementations;
    }

    public static Optional<EOperation> getOperationImplementationByName(final EClass clazz, final String operationName) {
        final EList<EOperation> operationImplementations = getOperationImplementationListByName(clazz, operationName);
        if (operationImplementations.size() > 1) {
            log.error("Multiple operation implementations found for {}#{}: {}", new Object[]{AsmUtils.getClassifierFQName(clazz), operationName, operationImplementations.stream().map(op -> AsmUtils.getOperationFQName(op)).collect(Collectors.joining(", "))});
            return Optional.empty();
        } else if (operationImplementations.isEmpty()) {
            log.warn("No operation implementation found for: {}#{}", AsmUtils.getClassifierFQName(clazz), operationName);
            return Optional.empty();
        } else {
            return Optional.of(operationImplementations.get(0));
        }
    }

    public static Set<String> getAllAbstractOperationNames(final EClass clazz) {
        final Set<String> abstractOperationNames = getAllOperationNames(clazz);
        abstractOperationNames.removeAll(getAllOperationImplementations(clazz).stream().map(op -> op.getName()).collect(Collectors.toSet()));
        return abstractOperationNames;
    }

    public static EList<EOperation> getAllOperationImplementations(final EClass clazz) {
        final EList<EOperation> allOperationImplementations = new UniqueEList<>();
        allOperationImplementations.addAll(getAllOperationNames(clazz).stream()
                .map(operationName -> getOperationImplementationByName(clazz, operationName))
                .filter(implementation -> implementation.isPresent())
                .map(implementation -> implementation.get())
                .collect(Collectors.toSet()));
        return allOperationImplementations;
    }

    public enum OperationBehaviour {
        LIST("list"),

        CREATE_INSTANCE("createInstance"),

        VALIDATE_CREATE("validateCreate"),

        REFRESH("refresh"),

        UPDATE_INSTANCE("updateInstance"),

        VALIDATE_UPDATE("validateUpdate"),

        DELETE_INSTANCE("deleteInstance"),

        SET_REFERENCE("setReference"),

        UNSET_REFERENCE("unsetReference"),

        ADD_REFERENCE("addReference"),

        REMOVE_REFERENCE("removeReference"),

        GET_REFERENCE_RANGE("getReferenceRange"),

        /**
         * Get template (a pre-instance filled with default values) of a given transfer object type.
         */
        GET_TEMPLATE("getTemplate"),

        /**
         * Get principal of a given access point.
         */
        GET_PRINCIPAL("getPrincipal");

        private final String type;

        OperationBehaviour(final String type) {
            this.type = type;
        }

        public static OperationBehaviour resolve(final String type) {
            for (OperationBehaviour operationBehaviour : values()) {
                if (Objects.equals(type, operationBehaviour.type)) {
                    return operationBehaviour;
                }
            }

            return null;
        }

        public String getType() {
            return type;
        }
    }

    private static String pad(int level, final String message) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("      ");
        }
        sb.append(message);
        return sb.toString();
    }
}
