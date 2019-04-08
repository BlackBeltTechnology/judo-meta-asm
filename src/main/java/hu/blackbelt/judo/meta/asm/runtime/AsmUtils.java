package hu.blackbelt.judo.meta.asm.runtime;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AsmUtils {
    public static final String extendedMetadataUri = "http://blackbelt.hu/judo/meta/ExtendedMetadata";

    public static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        return asStream(sourceIterator, false);
    }

    public static <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
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
    public static String getFQName(EAttribute eAttribute) {
        return getFQName(eAttribute.getEContainingClass()) + "#" + eAttribute.getName();
    }

    /*
    annotation.eol
    */

    /*
    @cached
    operation ASM!EStringToStringMapEntry getAnnotation() : ASM!EAnnotation {
        return ASM!EAnnotation.all.selectOne(a | a.details.contains(self));
    }
    */

    /*
    class.eol
    */

    /*
    @cached
    operation ASM!EClass hasSupertype() : Boolean {
        return self.eSuperTypes.size > 0;
    }
    */
    public static boolean hasSupertype(EClass eClass) {
        return eClass.getEAllSuperTypes().size() > 0;
    }


    /*
    @cached
    operation ASM!EClass isEntity() : Boolean {
        return self.annotatedAsTrue("entity");
    }
    */
    public static boolean isEntity(EClass eClass) {
        return annotatedAsTrue(eClass, "entity");
    }


    /*
    @cached
    operation ASM!EClass isFacade() : Boolean {
        return self.annotatedAsTrue("facade");
    }
    */
    public static boolean isFacade(EClass eClass) {
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
        return self.ePackage.getFullName() + "." + self.name;
    }
    */
    public static String getFQName(EClassifier eClassifier) {
        return getFullName(eClassifier.getEPackage()) + "." + eClassifier.getName();
    }

    /*
    @cached
    operation ASM!EClass getNestedClasses() : Collection {
        return ASM!EClass.all.select(c | c.name.startsWith(self.name + "ʘ") and not "ʘ".isSubstringOf(c.name.substring(self.name.length() + 1)));
    }
    */
    public static List<EClass> getNestedClasses(ResourceSet resourceSet, EClass eClass) {
        return asStream(resourceSet.getAllContents())
                .filter(e -> e instanceof EClass).map(e -> (EClass) e)
                .filter(c -> c.getName().startsWith(eClass.getName() + "ʘ") && !c.getName().substring(eClass.getName().length() + 1).contains("ʘ")).collect(Collectors.toList());
    }

    /*
    @cached
    operation ASM!EClass getContainerClass() : ASM!EClass {
        return ASM!EClass.all.selectOne(c | self.name.startsWith(c.name + "ʘ") and not "ʘ".isSubstringOf(self.name.substring(c.name.length() + 1)));
    }
    */

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
    public static boolean annotatedAsTrue(EModelElement eModelElement, String name) {
        Optional<EAnnotation> annotations = eModelElement.getEAnnotations().stream()
                .filter(a -> a.getSource().equals(extendedMetadataUri)).findFirst();
        if (annotations.isPresent()) {
            Optional<Map.Entry<String, String>> d = annotations.get().getDetails().stream()
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
    public static boolean annotatedAsFalse(EModelElement eModelElement, String name) {
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
    operation ASM!EPackage getFullName() : String {
        var package = self.eSuperPackage;
        var name = "";
        while (package.isDefined()) {
            name = package.name + "." + name;
            package = package.eSuperPackage;
        }

        return name + self.name;
    }
    */
    public static String getFullName(EPackage ePackage) {
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
    public static String getFQName(EReference eReference) {
        return getFQName(eReference.getEContainingClass()) + "#" + eReference.getName();
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
    */

}