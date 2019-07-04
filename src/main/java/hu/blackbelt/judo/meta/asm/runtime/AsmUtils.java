package hu.blackbelt.judo.meta.asm.runtime;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.empty;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;

@Slf4j
@RequiredArgsConstructor
public class AsmUtils {

    public static final String extendedMetadataUri = "http://blackbelt.hu/judo/meta/ExtendedMetadata";
    public static final String NAMESPACE_SEPARATOR = ".";
    public static final String FEATURE_SEPARATOR = "#";
    public static final String OPERATION_SEPARATOR = ".";

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

    @NonNull
    ResourceSet resourceSet;

    public static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        return asStream(sourceIterator, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    public <T> Stream<T> all() {
        return asStream((Iterator<T>) resourceSet.getAllContents(), false);
    }

    public <T> Stream<T> all(final Class<T> clazz) {
        return all().filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }

    /*
    accesPoint.eol
    */

    /*
    @cached
    operation getAllAccessPoints() : Collection {
        return ASM!EClass.all.select(c | c.isAccessPoint());
    }
    */

    /*
    @cached
    operation getAllExposedGraphs() : Collection {
        return ASM!EClass.all.select(c | c.isExposedGraph());
    }
    */

    /*
    @cached
    operation ASM!EClass isAccessPoint() : Boolean {
        return self.interface and self.annotatedAsTrue("accessPoint");
    }
    */

    /*
    @cached
    operation ASM!EClass getResolvedExposedBy() : ASM!EClass {
        var exposedBy = self.getAnnotationValue("exposedBy", false);
        if (exposedBy.isDefined()) {
            return exposedBy.resolve();
        } else {
            return null;
        }
    }
    */

    /*
    @cached
    operation ASM!EClass getResolvedRoot() : ASM!EClass {
        var root = self.getAnnotationValue("root", false);
        if (root.isDefined()) {
            return root.resolve();
        } else {
            return null;
        }
    }
    */

    /*
    @cached
    operation ASM!EClass isExposedGraph() : Boolean {
        var exposedBy = self.getResolvedExposedBy();
        var root = self.getResolvedRoot();

        if (exposedBy.isDefined() and root.isDefined()) {
            return exposedBy.isAccessPoint() and root.isMappedTransferObjectType() and self.interface;
        } else {
            return false;
        }
    }
    */

    /*
    @cached
    operation ASM!EClass getExposedGraphName() : String {
        if (self.isExposedGraph()) {
            return self.name.substring(self.getContainerClass().name.length + 1);
        } else {
            return null;
        }
    }
    */

    /*
    @cached
    operation ASM!EClass getGraphs() : Collection {
        var annotations = self.eAnnotations.selectOne(a | a.source = extendedMetadataURI);
        if (annotations.isDefined()) {
            return annotations.details.select(d | d.key.startsWith("graphs.")).collect(d | d.value.resolve());
        } else {
            return new Set;
        }
    }
    */

    /*
    @cached
    operation ASM!EClass getExposedMappedTransferObjectTypes() : Collection {
        if (self.isExposedGraph()) {
            return getAllMappedTransferObjectTypes().select(t | t.getGraphs().contains(self));
        } else {
            return new Sequence;
        }
    }
    */

    /*
    @cached
    operation ASM!EClass getExposedOperations() : Collection {
        if (self.isExposedGraph()) {
            return self.getExposedMappedTransferObjectTypes().collect(t | t.getNestedClasses()).flatten().select(og | og.interface).collect(og | og.eOperations).flatten();
        } else {
            return new Sequence;
        }
    }
    */


    /*
    attribute.eol
    */

    /*
    @cached
    operation ASM!EAttribute isID() : Boolean {
        return self.annotatedAsTrue("id");
    }
    */

    /*
    @cached
    operation ASM!EAttribute getClass() : ASM!EClass {
        return ASM!EClass.all.selectOne(c | c.eAllAttributes.includes(self));
    }
    */

    /*
    @cached
    operation ASM!EAttribute getFQName() : String {
        return self.getClass().getFQName() + "#" + self.name;
    }
    */
    public static String getAttributeFQName(final EAttribute eAttribute) {
        return getClassifierFQName(eAttribute.getEContainingClass()) + FEATURE_SEPARATOR + eAttribute.getName();
    }

    public static String getOperationFQName(final EOperation eOperation) {
        return getClassifierFQName(eOperation.getEContainingClass()) + OPERATION_SEPARATOR + eOperation.getName();
    }

    /*
    annotation.eol
    */

    /**
     * Get annotation in which the given attribute (map entry) can be found as details.
     *
     * @param mapEntry    attribute (map entry)
     * @return container annotation
     */
    Optional<EAnnotation> getAnnotation(final Map.Entry<String, String> mapEntry) {
        return all()
                .filter(e -> e instanceof EAnnotation).map(e -> (EAnnotation) e)
                .filter(e -> e.getDetails().contains(mapEntry)).findFirst();
    }

    /**
     * Get JUDO extension annotation of a given Ecore model element.
     * <p>
     * Annotation will be added if createIfNotExists parameter is <code>true</code> and it is not existing yet.
     *
     * @param eModelElement     model element
     * @param createIfNotExists create annotation is not exists yet
     * @return JUDO extension annotation
     */
    public static Optional<EAnnotation> getExtensionAnnotation(final EModelElement eModelElement, boolean createIfNotExists) {
        final Optional<EAnnotation> annotation = eModelElement.getEAnnotations().stream().filter(a -> AsmUtils.extendedMetadataUri.equals(a.getSource())).findFirst();
        if (!annotation.isPresent() && createIfNotExists) {
            final EAnnotation a = newEAnnotationBuilder().withSource(AsmUtils.extendedMetadataUri).build();
            eModelElement.getEAnnotations().add(a);
            return Optional.of(a);
        } else {
            return annotation;
        }
    }

    /**
     * Add value to a given model element as indexed list entry of prefixed JUDO extension annotation.
     * <p>
     * JUDO extension annotation will be created if it is not existing yet.
     *
     * @param eModelElement model element to which annotation value is added
     * @param prefix        annotation attribute prefix
     * @param value         annotation attribute value
     */
    public static void addAnnotationIfNotExists(final EModelElement eModelElement, final String prefix, final String value) {
        final Optional<EAnnotation> annotation = getExtensionAnnotation(eModelElement, true);
        if (annotation.isPresent()) {
            final EMap<String, String> details = annotation.get().getDetails();
            if (details.entrySet().stream()
                    .filter(e -> e.getKey().matches("^" + Pattern.quote(prefix + ".") + "[0-9]+$") && Objects.equals(e.getValue(), value))
                    .count() == 0) {
                final OptionalInt max = details.entrySet().stream()
                        .filter(e -> e.getKey().matches("^" + Pattern.quote(prefix + ".") + "[0-9]+$"))
                        .mapToInt(e -> Integer.parseInt(e.getKey().replace(prefix + ".", "")))
                        .max();

                if (max.isPresent()) {
                    details.put(prefix + "." + (max.getAsInt() + 1), value);
                } else {
                    details.put(prefix + "." + 0, value);
                }
            } else {
                log.debug("Annotation (prefix: {}, value: {}) is already added to model element {}", new Object[]{prefix, value, eModelElement});
            }
        } else {
            throw new IllegalStateException("Unable to create JUDO extension annotation");
        }
    }

    /**
     * Get annotated Ecore model element for a given annotation attribute (map entry).
     *
     * @param mapEntry    attribute (map entry)
     * @return owner Ecore model element
     */
    Optional<? extends EModelElement> getAnnotatedElement(final Map.Entry<String, String> mapEntry) {
        final Optional<EAnnotation> annotation = getAnnotation(mapEntry);
        if (annotation.isPresent()) {
            return Optional.of(annotation.get().getEModelElement());
        } else {
            return empty();
        }
    }

    /**
     * Get annotated Ecore class for a given annotation attribute (map entry).
     *
     * @param mapEntry    attribute (map entry)
     * @return owner Ecore class
     */
    public Optional<? extends EClass> getAnnotatedClass(final Map.Entry<String, String> mapEntry) {
        final Optional<? extends EModelElement> element = getAnnotatedElement(mapEntry);
        if (element.isPresent() && (element.get() instanceof EClass)) {
            return (Optional<? extends EClass>) element;
        } else {
            return empty();
        }
    }

    /**
     * Get annotated Ecore attribute for a given annotation attribute (map entry).
     *
     * @param mapEntry    attribute (map entry)
     * @return owner Ecore attribute
     */
    public Optional<? extends EAttribute> getAnnotatedAttribute(final Map.Entry<String, String> mapEntry) {
        final Optional<? extends EModelElement> element = getAnnotatedElement(mapEntry);
        if (element.isPresent() && (element.get() instanceof EAttribute)) {
            return (Optional<? extends EAttribute>) element;
        } else {
            return empty();
        }
    }

    /**
     * Get annotated Ecore reference for a given annotation attribute (map entry).
     *
     * @param mapEntry    attribute (map entry)
     * @return owner Ecore reference
     */
    public Optional<? extends EReference> getAnnotatedReference(final Map.Entry<String, String> mapEntry) {
        final Optional<? extends EModelElement> element = getAnnotatedElement(mapEntry);
        if (element.isPresent() && (element.get() instanceof EReference)) {
            return (Optional<? extends EReference>) element;
        } else {
            return empty();
        }
    }

    /**
     * Get annotated Ecore operation for a given annotation attribute (map entry).
     *
     * @param mapEntry    attribute (map entry)
     * @return owner Ecore operation
     */
    public Optional<? extends EOperation> getAnnotatedOperation(final Map.Entry<String, String> mapEntry) {
        final Optional<? extends EModelElement> element = getAnnotatedElement(mapEntry);
        if (element.isPresent() && (element.get() instanceof EOperation)) {
            return (Optional<? extends EOperation>) element;
        } else {
            return empty();
        }
    }

    /**
     * Get annotated Ecore parameter for a given annotation attribute (map entry).
     *
     * @param mapEntry    attribute (map entry)
     * @return owner Ecore parameter
     */
    public Optional<? extends EParameter> getAnnotatedParameter(final Map.Entry<String, String> mapEntry) {
        final Optional<? extends EModelElement> element = getAnnotatedElement(mapEntry);
        if (element.isPresent() && (element.get() instanceof EParameter)) {
            return (Optional<? extends EParameter>) element;
        } else {
            return empty();
        }
    }

    public static boolean isEntityType(final EClass eClass) {
        return annotatedAsTrue(eClass, "entity");
    }

    public boolean isMappedTransferObjectType(final EClass eClass) {
        return getEntityType(eClass).isPresent();
    }

    public EList<EOperation> getAllStatelessOperations() {
        return new BasicEList<>(all(EOperation.class)
                .filter(o -> isStateless(o))
                .collect(Collectors.toList()));
    }

    public EList<EClass> getAllMappedTransferObjectTypes() {
        return new BasicEList<>(all(EClass.class)
                .filter(c -> isMappedTransferObjectType(c))
                .collect(Collectors.toList()));
    }

    public Optional<EClass> getEntityType(final EClass eClass) {
        final Optional<String> mappedEntityTypeFQName = getExtensionAnnotationValue(eClass, "mappedEntityType", false);
        if (mappedEntityTypeFQName.isPresent()) {
            final Optional<EClassifier> resolved = resolve(mappedEntityTypeFQName.get());
            if (resolved.isPresent()) {
                final EClassifier eClassifier = resolved.get();
                if (eClassifier instanceof EClass) {
                    return Optional.of((EClass) eClassifier);
                } else {
                    log.error("Invalid mapped entity type: {}", mappedEntityTypeFQName.get());
                    return Optional.empty();
                }
            } else {
                log.error("Unable to resolve mapped entity type: {}", mappedEntityTypeFQName.get());
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public EList<EClass> getNestedClasses(final EClass eClass) {
        return new BasicEList<>(all(EClass.class)
                .filter(c -> c.getName().startsWith(eClass.getName() + SEPARATOR) && !c.getName().substring(eClass.getName().length() + 1).contains(SEPARATOR)).collect(Collectors.toList()));
    }

    public Optional<EClass> getContainerClass(final EClass eClass)  {
        return all(EClass.class)
                .filter(c -> getClassifierFQName(eClass).startsWith(getClassifierFQName(c) + SEPARATOR) && !eClass.getName().substring(c.getName().length() + 1).contains(SEPARATOR))
                .findAny();
    }

    /**
     * Returns the EClass of the given fully qualified name.
     * @param fqName Fully qualified name
     * @return the EClass instance of the given name
     */
    public Optional<EClass> getClassByFQName(final String fqName) {
        final Optional<EClassifier> classifier = resolve(fqName);
        if (classifier.isPresent()) {
            final EClassifier cl = classifier.get();
            if (cl instanceof EClass) {
                return Optional.of((EClass) cl);
            } else {
                throw new IllegalStateException("Fully qualified name represents no EClass");
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the the Extension annotation's given element in map. If failNotFound true it log a warning, otherwise
     * if the extension annotation or the given name not found returns null.
     * @param eModelElement The model element which is used to determinate
     * @param name The entry name of extension annotation
     * @param logIfNotFound When the extension or name in details not found log warn.
     * @return The value of annotation (<code>null</code> value is returned if key is found but value is not set)
     */
    public static Optional<String> getExtensionAnnotationValue(final EModelElement eModelElement, final String name, final boolean logIfNotFound) {
        final Optional<EAnnotation> annotation = getExtensionAnnotation(eModelElement, false);
        if (annotation.isPresent()) {
            final EMap<String, String> details = annotation.get().getDetails();
            if (details.containsKey(name)) {
                return Optional.ofNullable(details.get(name));
            } else {
                if (logIfNotFound) {
                    log.warn("No annotation " + name + " found on element: " + eModelElement.toString());
                }
                return empty();
            }
        } else {
            if (logIfNotFound) {
                log.warn("No annotation " + name + " found on element: " + eModelElement.toString());
            }
            return empty();
        }
    }

    /**
     * Check the given element's extension annotation on the given name is true. When the element have no
     * extension annotation returns false.
     *
     * @param eModelElement The model element which is used to determinate
     * @param name The entry name of extension annotation
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
     * @param name The entry name of extension annotation
     * @return <code>true</code> if annotation value represents a Java false value, <code>false</code> otherwise
     */
    public static boolean annotatedAsFalse(final EModelElement eModelElement, final String name) {
        final Optional<String> value = getExtensionAnnotationValue(eModelElement, name, false);
        return value.isPresent() && !Boolean.valueOf(value.get());
    }

    /**
     * Resolve a name to get a classifier. Fully qualified names are checked first, searching by name in second turn.
     *
     * @param fqName name to resolve
     * @return resolved classifier
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

    public static String getClassifierFQName(final EClassifier eClassifier) {
        return getPackageFQName(eClassifier.getEPackage()) + NAMESPACE_SEPARATOR + eClassifier.getName();
    }

    /*
    package.eol
    */

    /**
     * Get the fully qualified name of the package.
     *
     * @param ePackage package
     * @return fully qualified name of the package
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
        return isMappedTransferObjectType(eOperation.getEContainingClass());
    }

    /*
    service.eol
    */
    /*
    @cached
    operation getAllStatelessOperations() : Collection {
        return ASM!EOperation.all.select(o | o.isStateless());
    }
    */

    /*
    @cached
    operation getAllMappedTransferObjectTypes() : Collection {
        return ASM!EClass.all.select(c | c.isMappedTransferObjectType());
    }
    */

    /*
    @cached
    operation ASM!EOperation isStateless() : Boolean {
        var exposedBy = self.getAnnotationValue("exposedBy", false);
        if (exposedBy.isDefined()) {
            return exposedBy.resolve().isAccessPoint() and self.annotatedAsFalse("stateful");
        } else {
            return false;
        }
    }
    */

    /*
    @cached
    operation ASM!EClass isMappedTransferObjectType() : Boolean {
        return self.hasAnnotation("mappedEntityType");
    }
    */


    /*
    reference.eol
    */

    /*
    @cached
    operation ASM!EReference isEmbedded() : Boolean {
        return self.annotatedAsTrue("embedded");
    }
    */

    /*
    @cached
    operation ASM!EReference getFQName() : String {
        return self.eContainingClass.getFQName() + "#" + self.name;
    }
    */

    /**
     *
     * @param eReference
     * @return
     */
    public static String getReferenceFQName(EReference eReference) {
        return getClassifierFQName(eReference.getEContainingClass()) + FEATURE_SEPARATOR + eReference.getName();
    }

    /*
    @cached
    operation ASM!EReference getReferenceFQName() : String {
        return self.eReferenceType.getFQName() + "#" + self.name;
    }
    */

    /*
    type.eol
    */

    /*
    @cached
    operation String getJudoDataType() : ASM!EDataType {
        var dataType : ASM!EDataType =
                ASM.resource.resourceSet.packageRegistry.get("http://blackbelt.hu/judo/asm/types").eClassifiers
                    .selectOne(clazz | clazz.name = self);
        return dataType;
    }
    */

    /*
    @cached
    operation getIdentifierClass() : ASM!EClass {
        var identifierClass : ASM!EClass = ASM.resource.resourceSet.packageRegistry.get("http://blackbelt.hu/judo/asm/base")
                .eClassifiers.selectOne(clazz | clazz.name = "Identifiable");
        return identifierClass;
    }

    @cached
    operation ASM!EClass getEntityType() : ASM!EClass {
        var entityTypeName = self.getAnnotationValue("mappedEntityType", false);
        if (entityTypeName.isDefined()) {
            return entityTypeName.resolve();
        } else {
            return null;
        }
    }
    */

    /**
     * Returns the given class mapped entity when extension annotation is given and class is presented in the conaining fully qualified name.
     *
     * @param type The given ECLass type.
     * @return
     */
    public Optional<EClass> getMappedEntityType(EClass type) {
        Optional<String> mappedEntityTypeFQName =  getExtensionAnnotationValue(type, "mappedEntityType", false);
        if (mappedEntityTypeFQName.isPresent()) {
            return getClassByFQName(mappedEntityTypeFQName.get());
        } else {
            return empty();
        }
    }


    /**
     * Returns the given attribute's mapped attribute when extension annotation is given and attribute is presented the parent's class and
     * the given attribute name also.
     *
     * @param type The given EAttibute type.
     * @return
     */
    public Optional<EAttribute> getMappedAttribute(EAttribute type) {
        Optional<String> mappedAttributeName =  getExtensionAnnotationValue(type, "mappedAttribute", false);
        Optional<EClass> mappedEntityType =  getMappedEntityType(type.getEContainingClass());
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
     * @return
     */
    public Optional<EReference> getMappedReference(EReference type) {
        Optional<String> mappedReferenceName =  getExtensionAnnotationValue(type, "mappedReference", false);
        Optional<EClass> mappedEntityType =  getMappedEntityType(type.getEContainingClass());
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


    public static boolean isInteger(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && INTEGER_TYPES.contains(instanceClassName);
    }

    public static boolean isDecimal(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && DECIMAL_TYPES.contains(instanceClassName);
    }

    public static boolean isNumeric(final EDataType eDataType) {
        return isInteger(eDataType) || isDecimal(eDataType);
    }

    public static boolean isBoolean(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return "boolean".equals(instanceClassName)
                || "java.lang.Boolean".equals(instanceClassName);
    }

    public static boolean isString(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return "java.lang.String".equals(instanceClassName);
    }

    public static boolean isDate(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        if ("java.util.Date".equals(instanceClassName)) {
            return !isTimestampJavaUtilDate(eDataType);
        } else {
            return instanceClassName != null && DATE_TYPES.contains(instanceClassName);
        }
    }

    public static boolean isByteArray(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return "byte[]".equals(instanceClassName)
                || "java.sql.Blob".equals(instanceClassName);
    }

    public static boolean isTimestamp(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        if ("java.util.Date".equals(instanceClassName)) {
            return isTimestampJavaUtilDate(eDataType);
        } else {
            return instanceClassName != null && TIMESTAMP_TYPES.contains(instanceClassName);
        }
    }

    public static boolean isEnumeration(final EDataType eDataType) {
        return eDataType instanceof EEnum;
    }

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
    public static <T> Stream<T> getAllContents(final EObject eObject, final Class<T> clazz) {
        final ResourceSet resourceSet = eObject.eResource().getResourceSet();
        final Iterable<Notifier> asmContents = resourceSet::getAllContents;
        return StreamSupport.stream(asmContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }
}