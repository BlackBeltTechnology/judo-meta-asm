package hu.blackbelt.judo.meta.asm.runtime;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    
    public <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        return asStream(sourceIterator, false);
    }

    public <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
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
    public String getAttributeFQName(EAttribute eAttribute) {
        return getClassFQName(eAttribute.getEContainingClass()) + "#" + eAttribute.getName();
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
    public Optional<EAnnotation> getAnnotation(final Map.Entry<String, String> mapEntry) {
        return asStream(resourceSet.getAllContents())
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
    public Optional<EAnnotation> getExtensionAnnotation(final EModelElement eModelElement, boolean createIfNotExists) {
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
    public void addAnnotationIfNotExists(final EModelElement eModelElement, final String prefix, final String value) {
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
    public Optional<? extends EModelElement> getAnnotatedElement(final Map.Entry<String, String> mapEntry) {
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

    /*
    class.eol
    */

    /*
    @cached
    operation ASM!EClass hasSupertype() : Boolean {
        return self.eSuperTypes.size > 0;
    }
    */
    public boolean hasSupertype(EClass eClass) {
        return eClass.getEAllSuperTypes().size() > 0;
    }


    /*
    @cached
    operation ASM!EClass isEntity() : Boolean {
        return self.annotatedAsTrue("entity");
    }
    */
    public boolean isEntity(EClass eClass) {
        return annotatedAsTrue(eClass, "entity");
    }


    /*
    @cached
    operation ASM!EClass isFacade() : Boolean {
        return self.annotatedAsTrue("facade");
    }
    */
    public boolean isFacade(EClass eClass) {
        return annotatedAsTrue(eClass, "facade");
    }

    /*
    // TODO: Really used?
    @cached
    operation ASM!EClassifier getPackage() : ASM!EPackage {
        return ASM!EPackage.all.selectOne(p | p.eClassifiers.contains(self));
    }
    */

    /*
    @cached
    operation ASM!EClassifier getFQName() : String {
        return self.ePackage.getPackageFQName() + "." + self.name;
    }
    */
    public String getClassFQName(EClassifier eClassifier) {
        return getPackageFQName(eClassifier.getEPackage()) + "." + eClassifier.getName();
    }

    /*
    @cached
    operation ASM!EClass getNestedClasses() : Collection {
        return ASM!EClass.all.select(c | c.name.startsWith(self.name + "ʘ") and not "ʘ".isSubstringOf(c.name.substring(self.name.length() + 1)));
    }
    */
    public List<EClass> getNestedClasses(EClass eClass) {
        return asStream(resourceSet.getAllContents())
                .filter(e -> e instanceof EClass).map(e -> (EClass) e)
                .filter(c -> c.getName().startsWith(eClass.getName() + SEPARATOR) && !c.getName().substring(eClass.getName().length() + 1).contains(SEPARATOR)).collect(Collectors.toList());
    }

    /*
    @cached
    operation ASM!EClass getContainerClass() : ASM!EClass {
        return ASM!EClass.all.selectOne(c | self.name.startsWith(c.name + "ʘ") and not "ʘ".isSubstringOf(self.name.substring(c.name.length() + 1)));
    }
    */


    /**
     * Returns the EClass of the given fully qualified name.
     * @param fqName Fully qualified name
     * @return the EClass instance of the given name
     */
    public Optional<EClass> getClassByFQName(String fqName) {
        return asStream(resourceSet.getAllContents())
                .filter(e -> e instanceof EClass)
                .map(e -> (EClass) e)
                .filter(e -> getClassFQName(e).equals(fqName)).findFirst();
    }



    /*
    modelElement.eol
    */

    /*
    @cached
    operation ASM!EModelElement getAnnotationValue(name : String, failIfNotFound : Boolean) : String {
        var annotations = self.eAnnotations.selectOne(a | a.source = extendedMetadataURI);
        if (annotations.isDefined()) {
            var d = annotations.details.selectOne(d | d.key = name);
            if (d.isDefined() and d.value.isDefined()) {
                return d.value;
            } else {
                return d;
            }
        } else {
            if (failIfNotFound) {
                throw "No annotation " + name + " found on element: " + element.name;
            } else {
                return null;
            }
        }
    }
    */

    /**
     * Get the the Extension annotation's given element in map. If failNotFound true it log a warning, otherwise
     * if the extension annotation or the given name not found returns null.
     * @param eModelElement The model element which is used to determinate
     * @param name The entry name of extension annotation
     * @param failIfNotFound When the extension or name in details not found log warn.
     * @return The value of annotation
     */
    public Optional<String> getExtensionAnnotationValue(EModelElement eModelElement, String name, boolean failIfNotFound) {
        Optional<EAnnotation> annotation = getExtensionAnnotation(eModelElement, false);
        if (annotation.isPresent()) {
            final EMap<String, String> details = annotation.get().getDetails();
            if (details.containsKey(name)) {
                return Optional.of(details.get(name));
            } else {
                if (failIfNotFound) {
                    log.warn("No annotation " + name + " found on element: " + eModelElement.toString());
                } else {
                    return empty();
                }
            }
        } else {
            if (failIfNotFound) {
                log.warn("No annotation " + name + " found on element: " + eModelElement.toString());
            } else {
                return empty();
            }
        }
        return empty();
    }



    /*
    @cached
    operation ASM!EModelElement getAnnotationValue(name : String) : String {
        var annotations = self.eAnnotations.selectOne(a | a.source = extendedMetadataURI);
        if (annotations.isDefined()) {
            var d = annotations.details.selectOne(d | d.key = name);
            if (d.isDefined() and d.value.isDefined()) {
                return d.value;
            } else {
                return d;
            }
        } else {
            throw "No annotation " + name + " found on element: " + element.name;
        }
    }
    */

    /*
    @cached
    operation ASM!EModelElement hasAnnotation(name : String) : Boolean {
        var annotations = self.eAnnotations.selectOne(a | a.source = extendedMetadataURI);
        if (annotations.isDefined()) {
            var d = annotations.details.selectOne(d | d.key = name);
            return d.isDefined();
        }
        return false;
    }
    */

    /*
    @cached
    operation ASM!EModelElement annotatedAsTrue(name : String) : Boolean {
        var annotations = self.eAnnotations.selectOne(a | a.source = extendedMetadataURI);
        if (annotations.isDefined()) {
            var d = annotations.details.selectOne(d | d.key = name);
            if (d.isDefined() and d.value.isDefined()) {
                return d.value.asBoolean();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    */

    /**
     * Check the given element's extension annotation on the given name is true. When the element have no
     * extension annotation returns false.
     *
     * @param eModelElement
     * @param name
     * @return
     */
    public boolean annotatedAsTrue(EModelElement eModelElement, String name) {
        Optional<EAnnotation> annotation = getExtensionAnnotation(eModelElement, false);
        if (annotation.isPresent()) {
            Optional<Map.Entry<String, String>> d = annotation.get().getDetails().stream()
                    .filter(e -> e.getKey().equals(name)).findFirst();
            if (d.isPresent() && d.get().getValue() != null) {
                return Boolean.valueOf(d.get().getValue());
            } else {
                return false;
            }
        }
        return false;
    }


    /*
    @cached
    operation ASM!EModelElement annotatedAsFalse(name : String) : Boolean {
        var annotations = self.eAnnotations.selectOne(a | a.source = extendedMetadataURI);
        if (annotations.isDefined()) {
            var d = annotations.details.selectOne(d | d.key = name);
            if (d.isDefined() and d.value.isDefined()) {
                return not d.value.asBoolean();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    */

    /**
     * Check the given element's extension annotation on the given name is false. When the element have no
     * extension annotation returns false.
     *
     * @param eModelElement
     * @param name
     * @return
     */
    public boolean annotatedAsFalse(EModelElement eModelElement, String name) {
        Optional<EAnnotation> annotations = eModelElement.getEAnnotations().stream()
                .filter(a -> a.getSource().equals(extendedMetadataUri)).findFirst();
        if (annotations.isPresent()) {
            Optional<Map.Entry<String, String>> d = annotations.get().getDetails().stream()
                    .filter(e -> e.getKey().equals(name)).findFirst();
            if (d.isPresent() && d.get().getValue() != null) {
                return !Boolean.valueOf(d.get().getValue());
            } else {
                return false;
            }
        }
        return false;
    }

    /*
    @cached
    operation String resolve() : ASM!ENamedElement {
        return ASM!EClassifier.all.selectOne(e | e.getFQName() == self);
    }
    */

    /*
    package.eol
    */
    /*
    @cached
    operation ASM!EPackage getPackageFQName() : String {
        var package = self.eSuperPackage;
        var name = "";
        while (package.isDefined()) {
            name = package.name + "." + name;
            package = package.eSuperPackage;
        }

        return name + self.name;
    }
    */

    /**
     * Get the fully qualified name of the package.
     *
     * @param ePackage
     * @return
     */
    public String getPackageFQName(EPackage ePackage) {
        EPackage pack = ePackage.getESuperPackage();
        String fqName = "";
        while (pack != null) {
            fqName = pack.getName() + "." + fqName;
            pack = pack.getESuperPackage();
        }

        return fqName + ePackage.getName();
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
    public String getReferenceFQName(EReference eReference) {
        return getClassFQName(eReference.getEContainingClass()) + "#" + eReference.getName();
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


    public boolean isInteger(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && INTEGER_TYPES.contains(instanceClassName);
    }

    public boolean isDecimal(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return instanceClassName != null && DECIMAL_TYPES.contains(instanceClassName);
    }

    public boolean isNumeric(final EDataType eDataType) {
        return isInteger(eDataType) || isDecimal(eDataType);
    }

    public boolean isBoolean(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return "boolean".equals(instanceClassName)
                || "java.lang.Boolean".equals(instanceClassName);
    }

    public boolean isString(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        return "byte[]".equals(instanceClassName)
                || "java.lang.String".equals(instanceClassName);
    }

    public boolean isDate(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        if ("java.util.Date".equals(instanceClassName)) {
            return !isTimestampJavaUtilDate(eDataType);
        } else {
            return instanceClassName != null && DATE_TYPES.contains(instanceClassName);
        }
    }

    public boolean isTimestamp(final EDataType eDataType) {
        final String instanceClassName = eDataType.getInstanceClassName();
        if ("java.util.Date".equals(instanceClassName)) {
            return isTimestampJavaUtilDate(eDataType);
        } else {
            return instanceClassName != null && TIMESTAMP_TYPES.contains(instanceClassName);
        }
    }

    public boolean isEnumeration(final EDataType eDataType) {
        return eDataType instanceof EEnum;
    }

    static boolean isTimestampJavaUtilDate(final EDataType eDataType) {
        // TODO - check annotations of EDataType in ASM model, false by default
        return false;
    }
}