# ValidationContext API Reference

## Table of Contents
- [Overview](#overview)
- [Import](#import)
- [Key Methods](#key-methods)
- [Common Usage Patterns](#common-usage-patterns)
- [AsmModelProvider](#asmmodelprovider)
- [Thread Safety](#thread-safety)

## Overview

The `ValidationContext` class from `hu.blackbelt.judo.zeta.validation.core` provides access to the model and validation state during rule execution. It is passed to every validation rule as the second parameter.

## Import

```java
import hu.blackbelt.judo.zeta.validation.core.ValidationContext;
```

## Key Methods

### Model Traversal

```java
/**
 * Get all instances of a given EClass type from the resource set.
 *
 * @param eClass the EClass type
 * @return collection of instances
 */
<T extends EObject> Collection<T> getAllInstances(Class<T> eClass);
```

### Constraint Dependencies

```java
/**
 * Check if a constraint is satisfied for the current element.
 *
 * @param constraintName the constraint name
 * @return true if the constraint is satisfied
 */
boolean satisfies(String constraintName);

/**
 * Check if a constraint is satisfied for a specific element.
 *
 * @param element the element to check
 * @param constraintName the constraint name
 * @return true if the constraint is satisfied
 */
boolean satisfies(EObject element, String constraintName);

/**
 * Check if all elements in a collection satisfy a constraint.
 *
 * @param elements the elements to check
 * @param constraintName the constraint name
 * @return true if all elements satisfy the constraint
 */
boolean allSatisfy(Collection<? extends EObject> elements, String constraintName);
```

### Extension Methods

```java
/**
 * Call cached extension method on any EObject.
 *
 * @param target the target object
 * @param methodName the method name
 * @param args method arguments
 * @return the method result
 */
<T> T call(EObject target, String methodName, Object... args);

/**
 * Call extension method on current element.
 *
 * @param methodName the method name
 * @param args method arguments
 * @return the method result
 */
<T> T call(String methodName, Object... args);
```

### Current Element

```java
/**
 * Get the current element being validated.
 * Uses ThreadLocal for thread-safety in parallel validation.
 */
EObject getCurrentElement();
```

### Custom Attributes

```java
/**
 * Set a custom attribute (for use in pre/post hooks).
 */
void setAttribute(String key, Object value);

/**
 * Get a custom attribute.
 */
<T> T getAttribute(String key);
```

### Resource Access

```java
/**
 * Get the resource set containing the model.
 */
ResourceSet getResourceSet();

/**
 * Get the ModelProvider instance.
 */
ModelProvider getModelProvider();
```

## Common Usage Patterns

### Getting All Instances of a Type

```java
@Constraint(name = "NameMustBeUnique", message = "Name must be unique")
public ValidationRule nameMustBeUnique() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;

        // Get all EntityType instances in the model
        Collection<EntityType> allEntities = ctx.getAllInstances(EntityType.class);

        // Check for duplicates
        long count = allEntities.stream()
            .filter(e -> entity.getName().equals(e.getName()))
            .count();

        return count == 1
            ? ValidationResult.pass()
            : ValidationResult.fail("Duplicate name: " + entity.getName());
    };
}
```

### Checking Constraint Dependencies

```java
@Constraint(name = "ValidateName", message = "...")
public ValidationRule validateName() {
    return (element, ctx) -> {
        // Check if another constraint passed
        if (!ctx.satisfies("EntityMustExist")) {
            // Skip validation if dependency not satisfied
            return ValidationResult.pass();
        }

        // Proceed with validation
        // ...
    };
}
```

### Calling Extension Methods

```java
@Constraint(name = "ValidateSuperTypes", message = "...")
public ValidationRule validateSuperTypes() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;

        // Call extension method
        @SuppressWarnings("unchecked")
        Collection<EntityType> superTypes = ctx.call(entity, "getAllSuperTypes");

        // Use the result
        if (superTypes.contains(entity)) {
            return ValidationResult.fail("Circular inheritance detected");
        }

        return ValidationResult.pass();
    };
}
```

## AsmModelProvider

The `AsmModelProvider` class implements the `ModelProvider` interface for ASM models:

```java
import hu.blackbelt.judo.meta.asm.validation.AsmModelProvider;
import hu.blackbelt.judo.zeta.common.ModelProvider;

// Used internally by AsmValidator
ModelProvider modelProvider = new AsmModelProvider();
```

## Thread Safety

The `ValidationContext` is designed for use in parallel validation:

* `getAllInstances()` returns a thread-safe collection
* `satisfies()` uses concurrent caching
* `getCurrentElement()` uses ThreadLocal for thread-safety
* Extension method calls are thread-safe when methods are stateless

## See Also

* [Validation Framework Overview](index.md)
* [Annotations Reference](annotations.md)
* [Judo Zeta Framework](https://github.com/BlackBeltTechnology/judo-zeta)
