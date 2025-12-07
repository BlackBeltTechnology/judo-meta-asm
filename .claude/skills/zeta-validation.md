# Zeta Validation Skill

This skill helps create and describe proper validation rules and tests for the Judo Zeta Validation Framework.

## Overview

The Judo Zeta Validation Framework provides annotation-based validation for EMF models. Use this skill when you need to:

- Create new validation rules (`@Constraint` or `@Critique`)
- Write parameterized tests for validation rules
- Understand validation patterns and best practices

## Validation Rule Structure

### Package Location

Validation classes go in: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/`

### Basic Validator Class

```java
package hu.blackbelt.judo.meta.asm.validation;

import hu.blackbelt.judo.meta.asm.validation.annotation.*;
import hu.blackbelt.judo.meta.asm.validation.core.*;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EClass;

@ValidationContext(EClass.class)
public class EClassValidations {

    @Constraint(
        name = "EClassMustHaveName",
        message = "EClass must have a name"
    )
    public ValidationRule eClassMustHaveName() {
        return (element, ctx) -> {
            EClass eClass = (EClass) element;
            
            if (eClass.getName() == null || eClass.getName().trim().isEmpty()) {
                return ValidationResult.fail(
                    "EClassMustHaveName",
                    "EClass must have a name",
                    Severity.ERROR,
                    element
                );
            }
            
            return ValidationResult.pass();
        };
    }
}
```

## Annotations Reference

### @ValidationContext (Class-Level)

Marks a class as a validator for a specific EClass type.

```java
@ValidationContext(EClass.class)
public class EClassValidations {
    // All rules in this class validate EClass instances
}
```

### @Constraint (Method-Level)

Error-level validation rule. Model is invalid if this fails.

```java
@Constraint(
    name = "UniqueConstraintName",   // Unique identifier
    message = "Default error message"
)
public ValidationRule ruleName() {
    return (element, ctx) -> {
        // Return ValidationResult.pass() or ValidationResult.fail(...)
    };
}
```

**When to use @Constraint:**
- Required fields are missing
- Invalid references or relationships
- Type violations
- Cyclic dependencies
- Business rules that must be enforced

### @Critique (Method-Level)

Warning-level validation rule. Recommendations and best practices.

```java
@Critique(
    name = "UniqueCritiqueName",
    message = "Default warning message"
)
public ValidationRule ruleName() {
    return (element, ctx) -> {
        // Return ValidationResult.pass() or ValidationResult.warn(...)
    };
}
```

**When to use @Critique:**
- Missing documentation
- Naming convention violations
- Performance recommendations
- Style suggestions

### @Guard (Method-Level)

Conditional execution - rule only runs if guard returns true.

```java
@Constraint(name = "RuleName", message = "...")
@Guard(method = "guardMethodName")
public ValidationRule ruleName() {
    return (element, ctx) -> { ... };
}

// Guard method signature - MUST match exactly
private boolean guardMethodName(EObject element, ValidationContext ctx) {
    MyType obj = (MyType) element;
    return obj.someCondition();
}
```

### @Satisfies (Method-Level)

Dependency declaration - rule only runs if specified constraints passed.

```java
@Constraint(name = "DependentRule", message = "...")
@Satisfies(constraints = {"MustHaveName", "MustHaveType"})
public ValidationRule dependentRule() {
    return (element, ctx) -> {
        // Safe to assume MustHaveName and MustHaveType passed
    };
}
```

### @Cached (Method-Level)

Cache expensive computation results.

```java
@Cached
@Constraint(name = "ExpensiveRule", message = "...")
public ValidationRule expensiveRule() {
    return (element, ctx) -> {
        // Result cached per element
    };
}
```

### @ExtensionMethod (Class-Level)

Define reusable helper methods for element types.

```java
@ExtensionMethod(EntityType.class)
public class EntityTypeExtensions {

    @Cached
    public Collection<EntityType> getAllSuperTypes(EntityType self) {
        // Expensive recursive operation - cached
    }
    
    public boolean hasAttribute(EntityType self, String name) {
        return self.getAttributes().stream()
            .anyMatch(attr -> name.equals(attr.getName()));
    }
}
```

## ValidationResult API

```java
// Passing result
ValidationResult.pass()

// Failing result (simple)
ValidationResult.fail("Error message")

// Failing result (full metadata)
ValidationResult.fail(
    "ConstraintName",    // Constraint identifier
    "Error message",     // Message
    Severity.ERROR,      // Severity
    element              // The element that failed
)

// Warning result
ValidationResult.warn("Warning message")

ValidationResult.warn(
    "CritiqueName",
    "Warning message",
    element
)
```

## ValidationContext API

```java
// Get all instances of a type in the model
List<EntityType> allEntities = ctx.getAllInstances(EntityType.class);

// Check if another constraint passed for this element
boolean passed = ctx.satisfies(element, "OtherConstraintName");

// Call an extension method
Collection<EntityType> superTypes = ctx.call(entity, "getAllSuperTypes");

// Cache management
CacheKey key = CacheKey.of("my-cache-key");
Object cached = ctx.getCached(key);
ctx.putCached(key, computedValue);
```

## Common Validation Patterns

### 1. Required Field (Not Null/Empty)

```java
@Constraint(name = "MustHaveName", message = "Name is required")
public ValidationRule mustHaveName() {
    return (element, ctx) -> {
        MyType obj = (MyType) element;
        return obj.getName() != null && !obj.getName().trim().isEmpty()
            ? ValidationResult.pass()
            : ValidationResult.fail("Name is required");
    };
}
```

### 2. Uniqueness Check

```java
@Constraint(name = "NameMustBeUnique", message = "Name must be unique")
@Satisfies(constraints = {"MustHaveName"})
public ValidationRule nameMustBeUnique() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;
        List<EntityType> allEntities = ctx.getAllInstances(EntityType.class);
        
        long count = allEntities.stream()
            .filter(e -> entity.getName().equals(e.getName()))
            .count();
        
        return count == 1
            ? ValidationResult.pass()
            : ValidationResult.fail("Duplicate name: " + entity.getName());
    };
}
```

### 3. Pattern Matching

```java
@Constraint(name = "ValidIdentifier", message = "Must be valid identifier")
public ValidationRule validIdentifier() {
    return (element, ctx) -> {
        MyType obj = (MyType) element;
        String name = obj.getName();
        
        if (name == null || name.isEmpty()) {
            return ValidationResult.pass(); // Let other constraint handle
        }
        
        // Pattern: starts with letter, letters/digits/underscores
        if (!name.matches("^[A-Za-z][A-Za-z0-9_]*$")) {
            return ValidationResult.fail(
                "Name '" + name + "' must start with letter and contain only letters, digits, underscores"
            );
        }
        
        return ValidationResult.pass();
    };
}
```

### 4. Cycle Detection

```java
@Constraint(name = "NoCyclicInheritance", message = "Circular inheritance not allowed")
public ValidationRule noCyclicInheritance() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;
        
        if (hasCycle(entity, new HashSet<>())) {
            return ValidationResult.fail(
                "Entity '" + entity.getName() + "' has circular inheritance"
            );
        }
        
        return ValidationResult.pass();
    };
}

private boolean hasCycle(EntityType entity, Set<EntityType> visited) {
    if (visited.contains(entity)) {
        return true;
    }
    EntityType superType = entity.getSuperType();
    if (superType == null) {
        return false;
    }
    visited.add(entity);
    return hasCycle(superType, visited);
}
```

### 5. Conditional Validation with Guard

```java
@Constraint(name = "ConcreteEntityMustHaveTable", message = "...")
@Guard(method = "isConcrete")
public ValidationRule concreteEntityMustHaveTable() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;
        
        if (entity.getTableName() == null || entity.getTableName().isEmpty()) {
            return ValidationResult.fail(
                "Concrete entity '" + entity.getName() + "' must have table name"
            );
        }
        
        return ValidationResult.pass();
    };
}

private boolean isConcrete(EObject element, ValidationContext ctx) {
    return !((EntityType) element).isAbstract();
}
```

### 6. Collection Validation

```java
@Constraint(name = "MustHaveAttributes", message = "Must have at least one attribute")
public ValidationRule mustHaveAttributes() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;
        
        if (entity.getAttributes() == null || entity.getAttributes().isEmpty()) {
            return ValidationResult.fail(
                "Entity '" + entity.getName() + "' must have at least one attribute"
            );
        }
        
        return ValidationResult.pass();
    };
}
```

## Writing Tests

### Test Location

Tests go in: `model-test/src/test/java/hu/blackbelt/judo/meta/asm/runtime/`

### Parameterized Test Structure

Tests run with both EVL and Java validators using parameterized tests:

```java
package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.judo.meta.asm.AbstractAsmValidationTest;
import hu.blackbelt.judo.meta.asm.ValidatorType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.google.common.collect.ImmutableList;

public class MyValidationTest extends AbstractAsmValidationTest {

    @ParameterizedTest(name = "testValidModelPasses [{0}]")
    @EnumSource(ValidatorType.class)
    void testValidModelPasses(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();
        
        // Build valid model
        // ...
        
        // Expect no errors or warnings
        runValidation(ImmutableList.of(), ImmutableList.of());
    }

    @ParameterizedTest(name = "testMissingNameFails [{0}]")
    @EnumSource(ValidatorType.class)
    void testMissingNameFails(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();
        
        // Build model with missing name
        // ...
        
        // Expect specific error
        runValidation(
            ImmutableList.of("MustHaveName"),  // Expected errors
            ImmutableList.of()                  // Expected warnings
        );
    }

    @ParameterizedTest(name = "testMissingDescriptionWarns [{0}]")
    @EnumSource(ValidatorType.class)
    void testMissingDescriptionWarns(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();
        
        // Build model without description
        // ...
        
        // Expect warning, no errors
        runValidation(
            ImmutableList.of(),                     // Expected errors
            ImmutableList.of("ShouldHaveDescription") // Expected warnings
        );
    }
}
```

### AbstractAsmValidationTest Methods

```java
// Initialize the model (call first in each test)
initModel();

// Run validation with expected results
runValidation(
    Collection<String> expectedErrors,
    Collection<String> expectedWarnings
);

// Access the validator type (EVL or JAVA)
this.validatorType

// Access the model
this.asmModel
```

## Error Message Best Practices

```java
// BAD: Vague
return ValidationResult.fail("Invalid");

// BAD: No context
return ValidationResult.fail("Name is required");

// GOOD: Specific with context
return ValidationResult.fail(
    "Entity '" + entity.getName() + "' must have a name"
);

// GOOD: Actionable guidance
return ValidationResult.fail(
    "Entity '" + entity.getName() + "' has circular inheritance. " +
    "Remove one of the inheritance relationships to break the cycle."
);
```

## Decision Guide: @Constraint vs @Critique

| Scenario | @Constraint | @Critique |
|----------|-------------|-----------|
| Name is null or empty | Yes | |
| Name doesn't follow PascalCase | | Yes |
| Required relationship missing | Yes | |
| Optional description missing | | Yes |
| Circular inheritance detected | Yes | |
| Deep inheritance (>5 levels) | | Yes |
| Type mismatch in operation | Yes | |
| Missing @deprecated annotation | | Yes |

## Registering Validators

Validators must be registered in `AsmValidator.java`:

```java
public static void validateAsm(...) {
    ValidationRegistry registry = new ValidationRegistry();
    
    // Register validation classes
    registry.register(EClassValidations.class);
    registry.register(MyNewValidations.class);
    
    // ...
}
```

## See Also

- `docs/validation/index.adoc` - Validation framework overview
- `docs/validation/annotations.adoc` - Annotation reference
- `docs/validation/validation-context.adoc` - API reference
- https://github.com/BlackBeltTechnology/judo-zeta - Judo Zeta Framework
