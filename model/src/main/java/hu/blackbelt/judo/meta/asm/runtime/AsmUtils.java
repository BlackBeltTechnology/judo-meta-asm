package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.UniqueEList;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.slf4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public static final String SEPARATOR = "ʘ";

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
    private static final List<String> TEXT_TYPTES = Arrays.asList("java.sql.Clob");

    private static final Pattern EXPOSED_GRAPH_PATTERN = Pattern.compile("^(.*)/(.*)$");
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AsmUtils.class);

    final ResourceSet resourceSet;

    private boolean failOnError;
    
    public void setFailOnError(final boolean failOnError) {
        this.failOnError = failOnError;
    }
    
    public AsmUtils(ResourceSet resourceSet) {
        this.resourceSet = resourceSet;
    }

    public AsmUtils(final ResourceSet resourceSet, final boolean failOnError) {
        this.resourceSet = resourceSet;
        this.failOnError = failOnError;

        // TODO: Processes here
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
        final Optional<EClassifier> resolved = all(EClassifier.class)
                .filter(c -> Objects.equals(fqName, getClassifierFQName(c)))
                .findAny();

        if (resolved.isPresent()) {
            return resolved;
        } else {
            log.warn("EClassifier by fully qualified name '" + fqName + "' not found, trying to resolve by name only");
            return all(EClassifier.class)
                    .filter(c -> Objects.equals(fqName, c.getName()))
                    .findAny();
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
     * Get nested classes of a class.
     *
     * @param eClass container class
     * @return list of nested classes
     */
    public EList<EClass> getNestedClasses(final EClass eClass) {
        return new BasicEList<>(all(EClass.class)
                .filter(c -> c.getName().startsWith(eClass.getName() + SEPARATOR) && !c.getName().substring(eClass.getName().length() + 1).contains(SEPARATOR)).collect(Collectors.toList()));
    }

    /**
     * Get container class of a nested class.
     *
     * @param eClass nested class
     * @return container class (or null if eClass is not nested)
     */
    public Optional<EClass> getContainerClass(final EClass eClass) {
        return all(EClass.class)
                .filter(c -> getClassifierFQName(eClass).startsWith(getClassifierFQName(c) + SEPARATOR) && !eClass.getName().substring(c.getName().length() + 1).contains(SEPARATOR))
                .findAny();
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
            log.debug("Annotation (prefix: {}, value: {}) is already added to model element {}", new Object[]{annotationName, value, eModelElement});
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
     * Get the the Extension annotation's given element in map. If failNotFound true it log a warning, otherwise
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
     * Get the the Extension annotation's given element in map. If failNotFound true it log a warning, otherwise
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
    public boolean isBound(final EOperation eOperation) {
        return isMappedTransferObjectType(eOperation.getEContainingClass()) || isBuiltInOperation(eOperation);
    }

    /**
     * Check if an class is container of built-in operations.
     *
     * Containers are nested class of transfer object types.
     *
     * @param eClass class
     * @return <code>true</code> if class is container of built-in operations, <code>false</code> otherwise.
     */
    public boolean isBuiltInOperationGroup(final EClass eClass) {
        final Optional<EClass> containerClass = getContainerClass(eClass);

        return eClass.isInterface() && containerClass.isPresent() && isMappedTransferObjectType(containerClass.get());
    }

    /**
     * Check if an operation is built-in.
     *
     * Built-in operations are operations created for graph navigation and wiring (set/unset/add/remove references). Container class of built-in operations are nested classes.
     *
     * @param eOperation operation
     * @return <code>true</code> if operation is built-in, <code>false</code> otherwise.
     */
    public boolean isBuiltInOperation(final EOperation eOperation) {
        return (eOperation.getEContainingClass() != null) && isBuiltInOperationGroup(eOperation.getEContainingClass());
    }

    /**
     * Check if an operation is unbound.
     *
     * Unbound operations must be interfaces and container class must not be mapped transfer object type nor nested class.
     *
     * @param eOperation operation
     * @return <code>true</code> if operation is unbound, <code>false</code> otherwise.
     */
    public boolean isUnbound(final EOperation eOperation) {
        // TODO - is it really enough to check interface flag? (bound services must be part of mapped transfer object types
        return eOperation.getEContainingClass() != null && eOperation.getEContainingClass().isInterface() && !getContainerClass(eOperation.getEContainingClass()).isPresent() && !isMappedTransferObjectType(eOperation.getEContainingClass());
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
     * Check if a class represents an access point.
     *
     * @param eClass class
     * @return <code>true</code> if class is an access point, <code>false</code> otherwise
     */
    public static boolean isAccessPoint(final EClass eClass) {
        return annotatedAsTrue(eClass, "accessPoint");
    }

    /**
     * Get resolved exposed by annotation.
     *
     * Exposed by must be an access point.
     *
     * @param eAnnotation annotation
     * @return access point (or null if no exposedBy annotation found nor it is a valid access point)
     */
    public Optional<EClass> getResolvedExposedBy(final EAnnotation eAnnotation) {
        if (Objects.equals(eAnnotation.getSource(), getAnnotationUri("exposedBy"))) {
            if (eAnnotation.getDetails().containsKey(EXTENDED_METADATA_DETAILS_VALUE_KEY)) {
                final String exposedByFqName = eAnnotation.getDetails().get(EXTENDED_METADATA_DETAILS_VALUE_KEY);
                final Optional<EClass> resolvedExposedBy = getClassByFQName(exposedByFqName);
                if (resolvedExposedBy.isPresent()) {
                    if (isAccessPoint(resolvedExposedBy.get())) {
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
     * Get list of access points exposing an unbound operation.
     *
     * @param eOperation  unbound operation
     * @return list of access points (empty list is returned if eOperation is not bound)
     */
    public EList<EClass> getAccessPointsOfUnboundOperation(final EOperation eOperation) {
        return isUnbound(eOperation) ?
                new BasicEList<>(eOperation.getEAnnotations().stream()
                        .map(a -> getResolvedExposedBy(a))
                        .filter(exposedBy -> exposedBy.isPresent())
                        .map(exposedBy -> exposedBy.get())
                        .collect(Collectors.toList())) :
                ECollections.emptyEList();
    }

    /**
     * Get list of exposed services (unbound operations) of an access point.
     *
     * @param eClass access point
     * @return list of exposed services (empty list is returned if eClass is not an access point).
     */
    public EList<EOperation> getExposedServicesOfAccessPoint(final EClass eClass) {
        return isAccessPoint(eClass) ?
                new BasicEList<>(all(EOperation.class)
                        .filter(o -> !isBuiltInOperation(o))
                        .filter(o -> o.getEAnnotations().stream()
                                .anyMatch(a -> EcoreUtil.equals(eClass, getResolvedExposedBy(a).orElse(null))))
                        .collect(Collectors.toList())) :
                ECollections.emptyEList();
    }

    /**
     * Check if an operation is exposed by an access point.
     *
     * @param eOperation operation
     * @return <code>true</code> is operation is exposed by an access point, <code>false</code> otherwise
     */
    public boolean isExposedService(final EOperation eOperation) {
        return !getAccessPointsOfUnboundOperation(eOperation).isEmpty();
    }

    /**
     * Get resolved root.
     *
     * @param eAnnotation annotation (representing an exposed graph)
     * @return mapped transfer object type (root) of an exposed graph (or null if root is not resolved)
     */
    public Optional<EClass> getResolvedRoot(final EAnnotation eAnnotation) {
        final String root = eAnnotation.getDetails().get("root");
        final Optional<EClass> resolvedRoot = root != null ? getClassByFQName(root) : Optional.empty();
        if (resolvedRoot.isPresent()) {
            if (isMappedTransferObjectType(resolvedRoot.get())) {
                return resolvedRoot;
            } else {
                log.error("Invalid root of graph: {}", root);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Check if an annotaion represents an exposed graph.
     *
     * @param eAnnotation annotation
     * @return <code>true</code> if annotation represents an exposed graph, <code>false</code> otherwise
     */
    public boolean isGraph(final EAnnotation eAnnotation) {
        final Optional<EClass> root = getResolvedRoot(eAnnotation);
        return Objects.equals(eAnnotation.getSource(), getAnnotationUri("graph")) && root.isPresent();
    }

    /**
     * Get access point of a graph.
     *
     * @param eAnnotation annotation (representing an exposed graph)
     * @return access point of the exposed graph
     */
    public Optional<EClass> getAccessPointOfGraph(final EAnnotation eAnnotation) {
        return isGraph(eAnnotation) ?
                Optional.of((EClass) eAnnotation.getEModelElement()) :
                Optional.empty();
    }

    /**
     * Get list of exposed graphs of an access point.
     *
     * @param eClass class (representing an access point)
     * @return list of access points (empty list is returned is class is not an access point)
     */
    public EList<EAnnotation> getGraphListOfAccessPoint(final EClass eClass) {
        return isAccessPoint(eClass) ?
                new BasicEList<>(eClass.getEAnnotations().stream()
                        .filter(a -> isGraph(a))
                        .collect(Collectors.toList())) :
                ECollections.emptyEList();
    }

    /**
     * Get name of a graph.
     *
     * @param eAnnotation annotation (representing a graph)
     * @return name of the graph (or null is annotation is not an exposed graph)
     */
    public Optional<String> getGraphName(final EAnnotation eAnnotation) {
        return isGraph(eAnnotation) ? Optional.ofNullable(eAnnotation.getDetails().get(EXTENDED_METADATA_DETAILS_VALUE_KEY)) : Optional.empty();
    }

    /**
     * Get exposed graph by fully qualified name.
     *
     * @param fqName fully qualified name of an exposed graph.
     * @return exposed graph (if found and resolved), null otherwise
     */
    public Optional<EAnnotation> getExposedGraphByFqName(final String fqName) {
        final Matcher m = EXPOSED_GRAPH_PATTERN.matcher(fqName);
        if (m.matches()) {
            final Optional<EClass> accessPoint = getClassByFQName(m.group(1));
            final String graphName = m.group(2);

            if (accessPoint.isPresent()) {
                return getGraphListOfAccessPoint(accessPoint.get()).stream()
                        .filter(g -> Objects.equals(getGraphName(g).orElse(null), graphName))
                        .findAny();
            } else {
                log.error("Invalid access point of exposed graph: {}", m.group(1));
                return Optional.empty();
            }
        } else {
            log.error("Invalid exposed graph: {}", fqName);
            return Optional.empty();
        }
    }

    /**
     * Get exposed graph of an annotation
     * @param eAnnotation annotation
     * @return exposed graph represented by the given annotation (or null is graph is not defined by the given annotation nor if can be resolved)
     */
    public Optional<EAnnotation> getResolvedExposedGraph(final EAnnotation eAnnotation) {
        if (Objects.equals(eAnnotation.getSource(), getAnnotationUri("exposedGraph"))) {
            if (eAnnotation.getDetails().containsKey(EXTENDED_METADATA_DETAILS_VALUE_KEY)) {
                final String exposedGraphFqName = eAnnotation.getDetails().get(EXTENDED_METADATA_DETAILS_VALUE_KEY);
                final Optional<EAnnotation> resolvedExposedGraph = getExposedGraphByFqName(exposedGraphFqName);
                if (resolvedExposedGraph.isPresent()) {
                    if (isGraph(resolvedExposedGraph.get())) {
                        return resolvedExposedGraph;
                    } else {
                        log.error("Exposed graph is not a graph: {}", exposedGraphFqName);
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
     * Get list of mapped transfer object types that are available via target links (recursively) of a given mapped transfer object  type (ie. root of graph).
     *
     * @param eClass class (representing a mapped transfer object type)
     * @return list of mapped transfer object types (empty list is returned if class is not a mapped transfer object type)
     */
    public EList<EClass> getMappedTransferObjectGraph(final EClass eClass) {
        return isMappedTransferObjectType(eClass) ?
                getMappedTransferObjectGraph(eClass, new UniqueEList<>()) :
                ECollections.emptyEList();
    }

    /**
     * Add (and return) list of mapped transfer object types that are available via target links (recursively) of a given mapped transfer object type to visited list.
     *
     * @param eClass class (representing a mapped transfer object type)
     * @param visited list of navigable mapped transfer object types
     * @return return visited list
     */
    private EList<EClass> getMappedTransferObjectGraph(final EClass eClass, final EList<EClass> visited) {
        if (visited.contains(eClass)) {
            return visited;
        } else {
            visited.add(eClass);
            eClass.getEAllReferences().forEach(target -> getMappedTransferObjectGraph(target.getEReferenceType(), visited));
            return visited;
        }
    }

    /**
     * Return list of exposed graph in which a mapped transfer object type is exposed.
     *
     * @param eClass class (representing a mapped transfer object type)
     * @return list of exposed graphs (empty list is returned is class is not a mapped transfer object type)
     */
    public EList<EAnnotation> getExposedGraphsOfMappedTransferObjectType(final EClass eClass) {
        return isMappedTransferObjectType(eClass) ?
                new BasicEList<>(eClass.getEAnnotations().stream()
                        .map(a -> getResolvedExposedGraph(a))
                        .filter(exposedGraph -> exposedGraph.isPresent())
                        .map(exposedGraph -> exposedGraph.get())
                        .collect(Collectors.toList())) :
                ECollections.emptyEList();
    }

    /**
     * Get all mapped transfer object types of an access point.
     *
     * @param eClass class (representing an access point)
     * @return list of all mapped transfer object types exposed by exposed graphs of the given access point
     */
    public EList<EClass> getMappedTransferObjectTypesOfAccessPoint(final EClass eClass) {
        return isAccessPoint(eClass) ?
                new BasicEList<>(getGraphListOfAccessPoint(eClass).stream()
                        .flatMap(e -> getMappedTransferObjectTypesOfGraph(e).stream())
                        .collect(Collectors.toList())) :
                ECollections.emptyEList();
    }

    /**
     * Get list of mapped transfer object types that are available in the given exposed graph.
     *
     * @param eAnnotation annotation (representing a graph)
     * @return list of mapped transfer object types (empty list is returned if annotation is not a graph)
     */
    public EList<EClass> getMappedTransferObjectTypesOfGraph(final EAnnotation eAnnotation) {
        return isGraph(eAnnotation) ?
                new BasicEList<>(all(EClass.class)
                        .filter(o -> o.getEAnnotations().stream()
                                .anyMatch(a -> EcoreUtil.equals(eAnnotation, getResolvedExposedGraph(a).orElse(null))))
                        .collect(Collectors.toList())) :
                ECollections.emptyEList();
    }

    /**
     * Get all stateless operations.
     *
     * @return list of operations
     */
    public EList<EOperation> getAllStatelessOperations() {
        return new BasicEList<>(all(EOperation.class)
                .filter(o -> isStateless(o))
                .collect(Collectors.toList()));
    }

    /**
     * Get all stateful operations (including build-in operations).
     *
     * @return list of operations
     */
    public EList<EClass> getAllMappedTransferObjectTypes() {
        return new BasicEList<>(all(EClass.class)
                .filter(c -> isMappedTransferObjectType(c))
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
        return instanceClassName != null && TEXT_TYPTES.contains(instanceClassName);
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
     * Get all access points.
     *
     * @return access points
     */
    public EList<EClass> getAllAccessPoints() {
        return new BasicEList<>(all(EClass.class)
                .filter(c -> isAccessPoint(c))
                .collect(Collectors.toList()));
    }

    /**
     * Get all graphs.
     *
     * @return graphs
     */
    public EList<EAnnotation> getAllGraphs() {
        return new BasicEList<>(all(EAnnotation.class)
                .filter(a -> isGraph(a))
                .collect(Collectors.toList()));
    }

    /**
     * Get all exposed services.
     *
     * @return exposed services
     */
    public EList<EOperation> getAllExposedServices() {
        return new BasicEList<>(all(EOperation.class)
                .filter(op -> isExposedService(op))
                .collect(Collectors.toList()));
    }

    /**
     * Add exposed by annotation to (both mapped an unmapped) transfer object types.
     *
     * @param transferObjectType transfer object type
     * @param accessPointFqName  fully qualified name of the access point
     */
    void addExposedByAnnotationToTransferObjectType(final EClass transferObjectType, final String accessPointFqName) {
        final boolean added = addExtensionAnnotation(transferObjectType, "exposedBy", accessPointFqName);
        if (added) {
            transferObjectType.getEAllReferences().forEach(eReference -> addExposedByAnnotationToTransferObjectType(eReference.getEReferenceType(), accessPointFqName));

            transferObjectType.getEAllAttributes().forEach(eAttribute -> addExtensionAnnotation(eAttribute, "exposedBy", accessPointFqName));
            transferObjectType.getEAllReferences().forEach(eReference-> addExtensionAnnotation(eReference, "exposedBy", accessPointFqName));
        }
    }

    /**
     * Add exposed by and exposed graph annotations to mapped transfer object type.
     * @param mappedTransferObjectType mapped transfer object type
     * @param accessPointFqName        fully qualified name of the access point
     * @param exposedGraphFqName       fully qualified name of the exposed graph
     */
    void addExposedGraphAnnotationToTransferObjectType(final EClass mappedTransferObjectType, final String accessPointFqName, final String exposedGraphFqName) {
        if (log.isDebugEnabled()) {
            log.debug("    - mapped transfer object type: {}", getClassifierFQName(mappedTransferObjectType));
        }
        final Optional<EClass> entityType = getMappedEntityType(mappedTransferObjectType);
        if (entityType.isPresent()) {
            if (log.isDebugEnabled()) {
                log.debug("      - entity type: {}", getClassifierFQName(entityType.get()));
            }

            addExtensionAnnotation(mappedTransferObjectType, "exposedBy", accessPointFqName);
            final boolean added = addExtensionAnnotation(mappedTransferObjectType, "exposedGraph", exposedGraphFqName);
            if (added) {
                addExtensionAnnotation(entityType.get(), "exposedBy", accessPointFqName);

                getNestedClasses(mappedTransferObjectType).forEach(nestedClass -> {
                    if (log.isDebugEnabled()) {
                        log.debug("      - target: {}", getClassifierFQName(nestedClass));
                    }
                    nestedClass.getEAllOperations().forEach(builtInOperation -> {
                        if (log.isDebugEnabled()) {
                            log.debug("        - built-in operation: {}", getOperationFQName(builtInOperation));
                        }
                        addExtensionAnnotation(builtInOperation, "exposedBy", accessPointFqName);
                        addExtensionAnnotation(builtInOperation, "exposedGraph", exposedGraphFqName);

                        builtInOperation.getEParameters().forEach(inputParameter -> {
                            if (log.isDebugEnabled()) {
                                log.debug("          - input parameter ({}): {}", inputParameter.getName(), getClassifierFQName(inputParameter.getEType()));
                            }
                            final EClassifier type = inputParameter.getEType();
                            if ((type instanceof EClass) && isMappedTransferObjectType((EClass) type)) {
                                addExtensionAnnotation(inputParameter, "exposedBy", accessPointFqName);
                                addExtensionAnnotation(inputParameter, "exposedGraph", exposedGraphFqName);
                                addExposedGraphAnnotationToTransferObjectType((EClass) inputParameter.getEType(), accessPointFqName, exposedGraphFqName);
                            } else {
                                log.error("Input parameters must be transfer object types (EClass)");
                            }
                        });
                        if (builtInOperation.getEType() != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("          - output parameter: {}", getClassifierFQName(builtInOperation.getEType()));
                            }
                            final EClassifier type = builtInOperation.getEType();
                            if ((type instanceof EClass) && isMappedTransferObjectType((EClass) type)) {
                                addExtensionAnnotation(builtInOperation, "exposedBy", accessPointFqName);
                                addExtensionAnnotation(builtInOperation, "exposedGraph", exposedGraphFqName);
                                addExposedGraphAnnotationToTransferObjectType((EClass) builtInOperation.getEType(), accessPointFqName, exposedGraphFqName);
                            } else {
                                log.error("Output parameter must be transfer object type (EClass)");
                            }
                        }
                        builtInOperation.getEExceptions().forEach(faultParameter -> {
                            if (log.isDebugEnabled()) {
                                log.debug("          - fault parameter ({}): {}", faultParameter.getName(), getClassifierFQName(faultParameter));
                            }
                            if ((faultParameter instanceof EClass) && isMappedTransferObjectType((EClass) faultParameter)) {
                                addExposedGraphAnnotationToTransferObjectType((EClass) faultParameter, accessPointFqName, exposedGraphFqName);
                            } else {
                                log.error("Fault parameters must be transfer object types (EClass)");
                            }
                        });
                    });
                });

                mappedTransferObjectType.getEAllOperations().forEach(boundOperation -> {
                    if (log.isDebugEnabled()) {
                        log.debug("    - bound operation: ", getOperationFQName(boundOperation));
                    }

                    addExtensionAnnotation(boundOperation, "exposedBy", exposedGraphFqName);
                    addExtensionAnnotation(boundOperation, "exposedGraph", exposedGraphFqName);

                    boundOperation.getEParameters().forEach(inputParameter -> {
                        if (log.isDebugEnabled()) {
                            log.debug("        - input parameter ({}): {}", inputParameter.getName(), getClassifierFQName(inputParameter.getEType()));
                        }
                        final EClassifier type = inputParameter.getEType();
                        if ((type instanceof EClass) && isMappedTransferObjectType((EClass) type)) {
                            addExtensionAnnotation(inputParameter, "exposedBy", accessPointFqName);
                            addExtensionAnnotation(inputParameter, "exposedGraph", exposedGraphFqName);
                            addExposedGraphAnnotationToTransferObjectType((EClass) inputParameter.getEType(), accessPointFqName, exposedGraphFqName);
                        } else {
                            log.error("Input parameters must be transfer object types (EClass)");
                        }
                    });
                    if (boundOperation.getEType() != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("        - output parameter: {}", getClassifierFQName(boundOperation.getEType()));
                        }
                        final EClassifier type = boundOperation.getEType();
                        if ((type instanceof EClass) && isMappedTransferObjectType((EClass) type)) {
                            addExtensionAnnotation(boundOperation, "exposedBy", accessPointFqName);
                            addExtensionAnnotation(boundOperation, "exposedGraph", exposedGraphFqName);
                            addExposedGraphAnnotationToTransferObjectType((EClass) boundOperation.getEType(), accessPointFqName, exposedGraphFqName);
                        } else {
                            log.error("Output parameter must be transfer object type (EClass)");
                        }
                    }
                    boundOperation.getEExceptions().forEach(faultParameter -> {
                        if (log.isDebugEnabled()) {
                            log.debug("        - fault parameter ({}): {}", faultParameter.getName(), getClassifierFQName(faultParameter));
                        }
                        if ((faultParameter instanceof EClass) && isMappedTransferObjectType((EClass) faultParameter)) {
                            addExposedGraphAnnotationToTransferObjectType((EClass) faultParameter, accessPointFqName, exposedGraphFqName);
                        } else {
                            log.error("Fault parameters must be transfer object types (EClass)");
                        }
                    });
                });
            }
        }
    }

    /**
     * Decorate model elements with annotations required to process ASM model (ie generating OpenAPI model).
     */
    public void enrichWithAnnotations() {
        getAllAccessPoints().forEach(accessPoint -> {
            final String accessPointFqName = getClassifierFQName(accessPoint);
            if (log.isDebugEnabled()) {
                log.debug("Access point: {}", accessPointFqName);
            }
            getExposedServicesOfAccessPoint(accessPoint).forEach(exposedService -> {
                if (log.isDebugEnabled()) {
                    log.debug("  - exposed service: {}", getOperationFQName(exposedService));
                }
                exposedService.getEParameters().forEach(inputParameter -> {
                    if (log.isDebugEnabled()) {
                        log.debug("      - input parameter ({}): {}", inputParameter.getName(), getClassifierFQName(inputParameter.getEType()));
                    }
                    final EClassifier type = inputParameter.getEType();
                    if ((type instanceof EClass) && isMappedTransferObjectType((EClass) type)) {
                        addExtensionAnnotation(inputParameter, "exposedBy", accessPointFqName);
                        addExposedByAnnotationToTransferObjectType((EClass) type, accessPointFqName);
                    }
                });
                if (exposedService.getEType() != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("      - output parameter: {}", getClassifierFQName(exposedService.getEType()));
                    }
                    final EClassifier type = exposedService.getEType();
                    if ((type instanceof EClass) && isMappedTransferObjectType((EClass) type)) {
                        addExposedByAnnotationToTransferObjectType((EClass) type, accessPointFqName);
                    }
                }
                exposedService.getEExceptions().forEach(faultParameter -> {
                    if (log.isDebugEnabled()) {
                        log.debug("      - fault parameter ({}): {}", faultParameter.getName(), getClassifierFQName(faultParameter));
                    }
                    if ((faultParameter instanceof EClass) && isMappedTransferObjectType((EClass) faultParameter)) {
                        addExposedByAnnotationToTransferObjectType((EClass) faultParameter, accessPointFqName);
                    }
                });
            });
            getGraphListOfAccessPoint(accessPoint).forEach(exposedGraph -> {
                final String exposedGraphFqName = accessPointFqName + "/" + getGraphName(exposedGraph).orElse("");
                if (log.isDebugEnabled()) {
                    log.debug("  - exposed graph: {}", exposedGraphFqName);
                }

                final String rootFqName = exposedGraph.getDetails().get("root");
                if (rootFqName != null) {
                    final Optional<EClass> root = getClassByFQName(rootFqName);
                    if (root.isPresent()) {
                        if (log.isDebugEnabled()) {
                            log.debug("    - root: {}", rootFqName);
                        }

                        getMappedTransferObjectGraph(root.get()).forEach(mappedTransferObjectType -> {
                            addExposedGraphAnnotationToTransferObjectType(mappedTransferObjectType, accessPointFqName, exposedGraphFqName);
                        });
                    }
                }
            });
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
     * @param clazz class of model element types
     * @param <T>   specific type
     * @return all elements with clazz type
     */
    <T> Stream<T> all(final Class<T> clazz) {
        return all().filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
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
}