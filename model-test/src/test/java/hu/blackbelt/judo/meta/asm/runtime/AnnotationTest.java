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

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static hu.blackbelt.judo.meta.asm.runtime.AsmUtils.*;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAnnotationBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class AnnotationTest extends ExecutionContextOnAsmTest {

    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testGetExtensionAnnotationExisting () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();

        final Optional<EAnnotation> existing = asmUtils.getExtensionAnnotationByName(order.get(), "entity", false);

        assertTrue(existing.isPresent());
        assertTrue(Boolean.valueOf(existing.get().getDetails().get(AsmUtils.EXTENDED_METADATA_DETAILS_VALUE_KEY)));
    }

    @Test
    public void testGetExtensionAnnotationNotExistingButCreated () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EAnnotation> originallyMissingButCreatedAnnotation = asmUtils.getExtensionAnnotationByName(order.get(), "toBeCreated", true);

        assertTrue(originallyMissingButCreatedAnnotation.isPresent());

        assertFalse(Boolean.parseBoolean(originallyMissingButCreatedAnnotation.get().getDetails().get(AsmUtils.EXTENDED_METADATA_DETAILS_VALUE_KEY)));
    }

    @Test
    public void testGetExtensionAnnotationNotExisting () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EAnnotation> missingAndNotCreatedAnnotation = asmUtils.getExtensionAnnotationByName(order.get(), "notToBeCreated", false);

        assertFalse(missingAndNotCreatedAnnotation.isPresent());
    }

    @Test
    public void testGetExtensionAnnotationListByName () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        EList<EAnnotation> annotationList = asmUtils.getExtensionAnnotationListByName(order.get(), "entity");

        assertFalse(annotationList.isEmpty());
        assertTrue(annotationList.contains(asmUtils.getExtensionAnnotationByName(order.get(), "entity", false).get()));
    }

    @Test
    public void testAddExtensionAnnotation() {
        Optional<EClass> optionalOrder = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(optionalOrder.isPresent());
        EClass order = optionalOrder.get();

        String orderId = getId(order);
        assertNotNull(orderId);

        long targetAnnotationCount = order.getEAnnotations().stream()
                .filter(a -> getId(a) != null && getId(a).equals(orderId + "/NewAnnotation/Value"))
                .count();
        assertEquals(0, targetAnnotationCount);

        assertTrue(addExtensionAnnotation(order, "NewAnnotation", "value"));
        assertTrue(getExtensionAnnotationByName(order, "NewAnnotation", false).isPresent());

        targetAnnotationCount = order.getEAnnotations().stream()
                .filter(a -> getId(a) != null && getId(a).equals(orderId + "/NewAnnotation/Value"))
                .count();
        assertEquals(1, targetAnnotationCount);

        assertFalse(addExtensionAnnotation(order, "NewAnnotation", "value"));

        targetAnnotationCount = order.getEAnnotations().stream()
                .filter(a -> getId(a) != null && getId(a).equals(orderId + "/NewAnnotation/Value"))
                .count();
        assertEquals(1, targetAnnotationCount);
    }

    @Test
    public void testAddExtensionAnnotationDetails () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());

        asmUtils.addExtensionAnnotationDetails(order.get(), "NewAnnotation", ImmutableMap.of("key1", "value1"));
        assertTrue(asmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).get().getDetails().containsKey("key1"));

        asmUtils.addExtensionAnnotationDetails(order.get(), "NewAnnotation", ImmutableMap.of("key2", "value2"));
        assertTrue(asmUtils.getExtensionAnnotationByName(order.get(), "NewAnnotation", false).get().getDetails().containsKey("key2"));
    }

    @Test
    public void testGetExtensionAnnotationValue () {
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        Optional<String> mappedEntityTypeValue = asmUtils.getExtensionAnnotationValue(orderInfo.get(), "mappedEntityType", false);

        assertTrue(mappedEntityTypeValue.isPresent());
        assertThat(mappedEntityTypeValue.get(), equalTo("demo.entities.Order"));

        Optional<String> exposedGraphValue = asmUtils.getExtensionAnnotationValue(orderInfo.get(), "missingAnnotation", false);
        assertFalse(exposedGraphValue.isPresent());
    }

    @Test
    public void testGetExtensionAnnotationCustomValue () {
        Optional<EClass> internationalOrderInfo = asmUtils.all(EClass.class).filter(c -> "InternationalOrderInfo".equals(c.getName())).findAny();
        assertTrue(internationalOrderInfo.isPresent());
        Optional<EAttribute> shipperName = internationalOrderInfo.get().getEAttributes().stream().filter(attribute -> "shipperName".equals(attribute.getName())).findAny();
        assertTrue(shipperName.isPresent());

        assertThat(asmUtils.getExtensionAnnotationCustomValue(shipperName.get(), "constraints", "maxLength", false).get(), equalTo("255"));

        //negtest: annotation key not found
        assertFalse(asmUtils.getExtensionAnnotationCustomValue(shipperName.get(), "constraints", "missingKey", false).isPresent());

        //negtest: annotation not found
        assertFalse(asmUtils.getExtensionAnnotationCustomValue(shipperName.get(), "missingAnnotation", "maxLength", false).isPresent());
    }

    @Test
    public void testGetResolvedExposedBy () {
        Optional<EAnnotation> exposedByAnnotation = asmUtils.all(EAnnotation.class).filter(a -> "http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy".equals(a.getSource())).findAny();
        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "InternalAP".equals(a.getName())).findAny();

        assertTrue(exposedByAnnotation.isPresent());
        assertThat(asmUtils.getResolvedExposedBy(exposedByAnnotation.get()), is(internalAP));

        //negtest: annotation ("value") key not found
        EAnnotation annotationWithoutValueKey = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy").build();
        assertThat(asmUtils.getResolvedExposedBy(annotationWithoutValueKey), is(Optional.empty()));

        //negtest: access point not found
        EAnnotation annotationWithInvalidValue = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy").build();
        annotationWithInvalidValue.getDetails().put("value", "demo.services.ExternalAP");
        assertThat(asmUtils.getResolvedExposedBy(annotationWithInvalidValue), is(Optional.empty()));

        //negtest: annotation not pointing to an ActorType
        EAnnotation annotationExposingInvalidActorType = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/exposedBy").build();
        annotationExposingInvalidActorType.getDetails().put("value", "demo.entities.Order");
        assertThat(asmUtils.getResolvedExposedBy(annotationExposingInvalidActorType), is(Optional.empty()));
    }

    @Test
    public void testGetActorTypesOfOperation () {
        Optional<EClass> unboundServices = asmUtils.all(EClass.class).filter(c -> "__UnboundServices".equals(c.getName())).findAny();
        assertTrue(unboundServices.isPresent());
        Optional<EOperation> getAllOrders = unboundServices.get().getEOperations().stream().filter(o -> "getAllOrders".equals(o.getName())).findAny();
        assertTrue(getAllOrders.isPresent());

        Optional<EClass> internalAP = asmUtils.all(EClass.class).filter(a -> "InternalAP".equals(a.getName())).findAny();
        assertTrue(internalAP.isPresent());
        assertThat(asmUtils.getActorTypesOfOperation(getAllOrders.get()), hasItems(internalAP.get()));
    }

    @Test
    public void testGetMappedEntity () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        assertTrue(orderInfo.isPresent());

        Optional<EClass> mappedType = asmUtils.getMappedEntityType(orderInfo.get());
        assertTrue(mappedType.isPresent());
        assertThat(mappedType.get(), equalTo(order.get()));

        final EAnnotation mappedEntityTypeAnnotationWithMissingKey = newEAnnotationBuilder().withSource("http://blackbelt.hu/judo/meta/ExtendedMetadata/mappedEntityType").build();
        final EClass classWithInvalidMappedEntityTypeAnnotation = newEClassBuilder().withName("ClassWithInvalidMappedEntityTypeAnnotation").withEAnnotations(mappedEntityTypeAnnotationWithMissingKey).build();
        assertFalse(asmUtils.getMappedEntityType(classWithInvalidMappedEntityTypeAnnotation).isPresent());
    }

    @Test
    public void testGetMappedAttribute () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        assertTrue(orderInfo.isPresent());

        Optional<EAttribute> orderDate = order.get().getEAllAttributes().stream().filter(attribute -> "orderDate".equals(attribute.getName())).findAny();
        Optional<EAttribute> orderInfoDate = orderInfo.get().getEAllAttributes().stream().filter(attribute -> "orderDate".equals(attribute.getName())).findAny();
        assertTrue(orderDate.isPresent());
        assertTrue(orderInfoDate.isPresent());

        Optional<EAttribute> mappedAttribute = asmUtils.getMappedAttribute(orderInfoDate.get());
        assertTrue(mappedAttribute.isPresent());
        assertThat(mappedAttribute.get(), equalTo(orderDate.get()));
    }

    @Test
    public void testGetMappedReference () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        Optional<EClass> orderInfo = asmUtils.all(EClass.class).filter(c -> "OrderInfo".equals(c.getName())).findAny();
        assertTrue(order.isPresent());
        assertTrue(orderInfo.isPresent());

        Optional<EReference> orderDetails = order.get().getEAllReferences().stream().filter(reference -> "orderDetails".equals(reference.getName())).findAny();
        Optional<EReference> orderInfoItems = orderInfo.get().getEAllReferences().stream().filter(reference -> "items".equals(reference.getName())).findAny();

        Optional<EReference> mappedReference = asmUtils.getMappedReference(orderInfoItems.get());
        assertTrue(mappedReference.isPresent());
        assertThat(mappedReference.get(), equalTo(orderDetails.get()));
    }

    @Test
    public void testAnnotatedAsFalseOrTrue () {
        Optional<EClass> order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny();
        assertTrue(order.isPresent());

        assertTrue(asmUtils.annotatedAsTrue(order.get(), "entity"));
        assertFalse(asmUtils.annotatedAsTrue(order.get(), "missingAnnotation"));

        assertFalse(asmUtils.annotatedAsFalse(order.get(), "entity"));
        assertFalse(asmUtils.annotatedAsFalse(order.get(), "missingAnnotation"));
    }
}
