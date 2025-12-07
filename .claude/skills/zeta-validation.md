# Zeta Validation Skill

This skill helps create and describe proper validation rules and tests for the Judo Zeta Validation Framework.

## Overview

The Judo Zeta Validation Framework provides annotation-based validation for EMF models. Use this skill when you need to:

- Create new validation rules (`@Constraint` or `@Critique`)
- Write parameterized tests for validation rules
- Understand validation patterns and best practices

## Constants Pattern (REQUIRED)

**Always use constants for constraint names, guard method names, and extension method names.** This provides compile-time safety, IDE autocomplete, and easier refactoring.

### ConstraintNames.java

Location: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/ConstraintNames.java`

```java
package hu.blackbelt.judo.meta.asm.validation;

/**
 * Centralized constants for all validation constraint and critique names.
 */
public final class ConstraintNames {

    private ConstraintNames() {
        throw new UnsupportedOperationException("Utility class");
    }

    // EClass Constraints
    public static final String ECLASS_MUST_HAVE_NAME = "EClassMustHaveName";
    public static final String ECLASS_NAMES_ARE_UNIQUE = "EClassNamesAreUnique";
    
    // EAttribute Constraints
    public static final String EATTRIBUTE_MUST_HAVE_NAME = "EAttributeMustHaveName";
    public static final String EATTRIBUTE_MUST_HAVE_TYPE = "EAttributeMustHaveType";
    
    // EReference Constraints
    public static final String EREFERENCE_TARGET_MUST_EXIST = "EReferenceTargetMustExist";
}
```

### GuardMethodNames.java

Location: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/GuardMethodNames.java`

```java
package hu.blackbelt.judo.meta.asm.validation;

/**
 * Centralized constants for guard method names.
 */
public final class GuardMethodNames {

    private GuardMethodNames() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String IS_ABSTRACT = "isAbstract";
    public static final String IS_CONCRETE = "isConcrete";
    public static final String HAS_SUPERTYPE = "hasSupertype";
    public static final String HAS_NAME = "hasName";
}
```

### ExtensionMethodNames.java

Location: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/ExtensionMethodNames.java`

```java
package hu.blackbelt.judo.meta.asm.validation;

/**
 * Centralized constants for extension method names.
 */
public final class ExtensionMethodNames {

    private ExtensionMethodNames() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String GET_ALL_SUPERTYPES = "getAllSupertypes";
    public static final String GET_ALL_ATTRIBUTES = "getAllAttributes";
    public static final String GET_INHERITANCE_CHAIN = "getInheritanceChain";
}
```

## Validation Rule Structure

### Package Location

Validation classes go in: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/`

### Basic Validator Class (Using Constants)

```java
package hu.blackbelt.judo.meta.asm.validation;

import hu.blackbelt.judo.meta.asm.validation.annotation.*;
import hu.blackbelt.judo.meta.asm.validation.core.*;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EClass;

import static hu.blackbelt.judo.meta.asm.validation.ConstraintNames.*;
import static hu.blackbelt.judo.meta.asm.validation.GuardMethodNames.*;

@ValidationContext(EClass.class)
public class EClassValidations {

    @Constraint(
        name = ECLASS_MUST_HAVE_NAME,
        message = "EClass must have a name"
    )
    public ValidationRule eClassMustHaveName() {
        return (element, ctx) -> {
            EClass eClass = (EClass) element;
            
            if (eClass.getName() == null || eClass.getName().trim().isEmpty()) {
                return ValidationResult.fail(
                    ECLASS_MUST_HAVE_NAME,
                    "EClass must have a name",
                    Severity.ERROR,
                    element
                );
            }
            
            return ValidationResult.pass();
        };
    }

    @Satisfies(constraints = {ECLASS_MUST_HAVE_NAME})
    @Critique(
        name = ECLASS_NAMES_ARE_UNIQUE,
        message = "EClass names should be unique"
    )
    public ValidationRule eClassNamesAreUnique() {
        return (element, ctx) -> {
            EClass eClass = (EClass) element;
            List<EClass> allClasses = ctx.getAllInstances(EClass.class);
            
            long count = allClasses.stream()
                .filter(c -> eClass.getName().equals(c.getName()))
                .count();
            
            return count == 1
                ? ValidationResult.pass()
                : ValidationResult.warn(
                    ECLASS_NAMES_ARE_UNIQUE,
                    "Duplicate EClass name: " + eClass.getName(),
                    element
                );
        };
    }

    @Guard(method = IS_CONCRETE)
    @Constraint(
        name = CONCRETE_ECLASS_MUST_HAVE_ATTRIBUTES,
        message = "Concrete EClass must have attributes"
    )
    public ValidationRule concreteEClassMustHaveAttributes() {
        return (element, ctx) -> {
            EClass eClass = (EClass) element;
            
            if (eClass.getEAttributes().isEmpty()) {
                return ValidationResult.fail(
                    CONCRETE_ECLASS_MUST_HAVE_ATTRIBUTES,
                    "Concrete EClass '" + eClass.getName() + "' must have at least one attribute",
                    Severity.ERROR,
                    element
                );
            }
            
            return ValidationResult.pass();
        };
    }

    // Guard method - name MUST match the constant
    public boolean isConcrete(EObject element, ValidationContext ctx) {
        return !((EClass) element).isAbstract();
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
    name = CONSTRAINT_NAME_CONSTANT,  // Use constant!
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
    name = CRITIQUE_NAME_CONSTANT,  // Use constant!
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
@Constraint(name = RULE_NAME, message = "...")
@Guard(method = GUARD_METHOD_NAME)  // Use constant!
public ValidationRule ruleName() {
    return (element, ctx) -> { ... };
}

// Guard method name MUST match the constant value
public boolean guardMethodName(EObject element, ValidationContext ctx) {
    MyType obj = (MyType) element;
    return obj.someCondition();
}
```

### @Satisfies (Method-Level)

Dependency declaration - rule only runs if specified constraints passed.

```java
@Constraint(name = DEPENDENT_RULE, message = "...")
@Satisfies(constraints = {MUST_HAVE_NAME, MUST_HAVE_TYPE})  // Use constants!
public ValidationRule dependentRule() {
    return (element, ctx) -> {
        // Safe to assume MUST_HAVE_NAME and MUST_HAVE_TYPE passed
    };
}
```

### @Cached (Method-Level)

Cache expensive computation results.

```java
@Cached
@Constraint(name = EXPENSIVE_RULE, message = "...")
public ValidationRule expensiveRule() {
    return (element, ctx) -> {
        // Result cached per element
    };
}
```

### @ExtensionMethod (Class-Level)

Define reusable helper methods for element types.

```java
import static hu.blackbelt.judo.meta.asm.validation.ExtensionMethodNames.*;

@ExtensionMethod(EntityType.class)
public class EntityTypeExtensions {

    @Cached
    public Collection<EntityType> getAllSuperTypes(EntityType self) {
        // Method name matches GET_ALL_SUPERTYPES constant
    }
}
```

## ValidationResult API

```java
// Passing result
ValidationResult.pass()

// Failing result (use constant for name!)
ValidationResult.fail(CONSTRAINT_NAME, "Error message", Severity.ERROR, element)

// Simple fail (for cases where constraint name is already set)
ValidationResult.fail("Error message")

// Warning result (use constant for name!)
ValidationResult.warn(CRITIQUE_NAME, "Warning message", element)
```

## ValidationContext API

```java
// Get all instances of a type in the model
List<EntityType> allEntities = ctx.getAllInstances(EntityType.class);

// Check if another constraint passed for this element (use constant!)
boolean passed = ctx.satisfies(element, OTHER_CONSTRAINT_NAME);

// Call an extension method (use constant!)
Collection<EntityType> superTypes = ctx.call(entity, GET_ALL_SUPERTYPES);

// Cache management
CacheKey key = CacheKey.of("my-cache-key");
Object cached = ctx.getCached(key);
ctx.putCached(key, computedValue);
```

## Common Validation Patterns

### 1. Required Field (Not Null/Empty)

```java
import static hu.blackbelt.judo.meta.asm.validation.ConstraintNames.*;

@Constraint(name = MUST_HAVE_NAME, message = "Name is required")
public ValidationRule mustHaveName() {
    return (element, ctx) -> {
        MyType obj = (MyType) element;
        return obj.getName() != null && !obj.getName().trim().isEmpty()
            ? ValidationResult.pass()
            : ValidationResult.fail(MUST_HAVE_NAME, "Name is required", Severity.ERROR, element);
    };
}
```

### 2. Uniqueness Check

```java
@Satisfies(constraints = {MUST_HAVE_NAME})
@Constraint(name = NAME_MUST_BE_UNIQUE, message = "Name must be unique")
public ValidationRule nameMustBeUnique() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;
        List<EntityType> allEntities = ctx.getAllInstances(EntityType.class);
        
        long count = allEntities.stream()
            .filter(e -> entity.getName().equals(e.getName()))
            .count();
        
        return count == 1
            ? ValidationResult.pass()
            : ValidationResult.fail(NAME_MUST_BE_UNIQUE, 
                "Duplicate name: " + entity.getName(), Severity.ERROR, element);
    };
}
```

### 3. Conditional Validation with Guard

```java
@Guard(method = IS_CONCRETE)
@Constraint(name = CONCRETE_ENTITY_MUST_HAVE_TABLE, message = "...")
public ValidationRule concreteEntityMustHaveTable() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;
        
        if (entity.getTableName() == null || entity.getTableName().isEmpty()) {
            return ValidationResult.fail(
                CONCRETE_ENTITY_MUST_HAVE_TABLE,
                "Concrete entity '" + entity.getName() + "' must have table name",
                Severity.ERROR,
                element
            );
        }
        
        return ValidationResult.pass();
    };
}

// Guard method - name matches IS_CONCRETE constant value
public boolean isConcrete(EObject element, ValidationContext ctx) {
    return !((EntityType) element).isAbstract();
}
```

### 4. Cycle Detection

```java
@Satisfies(constraints = {MUST_HAVE_NAME})
@Constraint(name = NO_CYCLIC_INHERITANCE, message = "Circular inheritance not allowed")
public ValidationRule noCyclicInheritance() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;
        
        if (hasCycle(entity, new HashSet<>())) {
            return ValidationResult.fail(
                NO_CYCLIC_INHERITANCE,
                "Entity '" + entity.getName() + "' has circular inheritance",
                Severity.ERROR,
                element
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

### 5. Collection Validation

```java
@Constraint(name = MUST_HAVE_ATTRIBUTES, message = "Must have at least one attribute")
public ValidationRule mustHaveAttributes() {
    return (element, ctx) -> {
        EntityType entity = (EntityType) element;
        
        if (entity.getAttributes() == null || entity.getAttributes().isEmpty()) {
            return ValidationResult.fail(
                MUST_HAVE_ATTRIBUTES,
                "Entity '" + entity.getName() + "' must have at least one attribute",
                Severity.ERROR,
                element
            );
        }
        
        return ValidationResult.pass();
    };
}
```

## Writing Tests

### Test Location

Tests go in: `model-test/src/test/java/hu/blackbelt/judo/meta/asm/runtime/`

### Parameterized Test Structure (Using Constants)

```java
package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.judo.meta.asm.AbstractAsmValidationTest;
import hu.blackbelt.judo.meta.asm.ValidatorType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.google.common.collect.ImmutableList;

import static hu.blackbelt.judo.meta.asm.validation.ConstraintNames.*;

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
        
        // Use constant for expected error!
        runValidation(
            ImmutableList.of(MUST_HAVE_NAME),
            ImmutableList.of()
        );
    }

    @ParameterizedTest(name = "testMissingDescriptionWarns [{0}]")
    @EnumSource(ValidatorType.class)
    void testMissingDescriptionWarns(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();
        
        // Build model without description
        // ...
        
        // Use constant for expected warning!
        runValidation(
            ImmutableList.of(),
            ImmutableList.of(SHOULD_HAVE_DESCRIPTION)
        );
    }
}
```

## Error Message Best Practices

```java
// BAD: Vague
return ValidationResult.fail(CONSTRAINT_NAME, "Invalid", Severity.ERROR, element);

// BAD: No context
return ValidationResult.fail(CONSTRAINT_NAME, "Name is required", Severity.ERROR, element);

// GOOD: Specific with context
return ValidationResult.fail(
    CONSTRAINT_NAME,
    "Entity '" + entity.getName() + "' must have a name",
    Severity.ERROR,
    element
);

// GOOD: Actionable guidance
return ValidationResult.fail(
    NO_CYCLIC_INHERITANCE,
    "Entity '" + entity.getName() + "' has circular inheritance. " +
    "Remove one of the inheritance relationships to break the cycle.",
    Severity.ERROR,
    element
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

## Checklist for New Validation Rules

1. [ ] Add constraint/critique name constant to `ConstraintNames.java`
2. [ ] Add guard method name constant to `GuardMethodNames.java` (if using guards)
3. [ ] Add extension method name constant to `ExtensionMethodNames.java` (if using extensions)
4. [ ] Use static imports for all constants
5. [ ] Use constant in `@Constraint`/`@Critique` name parameter
6. [ ] Use constant in `ValidationResult.fail()`/`ValidationResult.warn()`
7. [ ] Use constant in `@Guard(method = ...)` if applicable
8. [ ] Use constants in `@Satisfies(constraints = {...})` if applicable
9. [ ] Register validation class in `AsmValidator.java`
10. [ ] Write parameterized test using constants for expected errors/warnings

## See Also

- `docs/validation/index.adoc` - Validation framework overview
- `docs/validation/annotations.adoc` - Annotation reference
- `docs/validation/validation-context.adoc` - API reference
- https://github.com/BlackBeltTechnology/judo-zeta - Judo Zeta Framework
