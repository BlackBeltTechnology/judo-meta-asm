# Zeta Validation Skill

This skill helps create and describe proper validation rules and tests for the Judo Zeta Validation Framework.

## Overview

The Judo Zeta Validation Framework provides annotation-based validation for EMF models. ASM uses the [Judo Zeta](https://github.com/BlackBeltTechnology/judo-zeta) library for validation.

Use this skill when you need to:

- Create new validation rules (`@Constraint` or `@Critique`)
- Write parameterized tests for validation rules
- Understand validation patterns and best practices

## Dependencies

The validation framework uses these Zeta dependencies (already configured in `model/pom.xml`):

```xml
<dependency>
    <groupId>hu.blackbelt.judo.zeta</groupId>
    <artifactId>hu.blackbelt.judo.zeta.validation-core</artifactId>
    <version>${judo-zeta-version}</version>
</dependency>
<dependency>
    <groupId>hu.blackbelt.judo.zeta</groupId>
    <artifactId>hu.blackbelt.judo.zeta.annotations</artifactId>
    <version>${judo-zeta-version}</version>
</dependency>
<dependency>
    <groupId>hu.blackbelt.judo.zeta</groupId>
    <artifactId>hu.blackbelt.judo.zeta.common</artifactId>
    <version>${judo-zeta-version}</version>
</dependency>
```

## Package Imports

**Annotations** (from `hu.blackbelt.judo.zeta.annotation`):
- `@ValidationContext`
- `@Constraint`
- `@Critique`
- `@Guard`
- `@Satisfies`
- `@Cached`
- `@ExtensionMethod`

**Core classes** (from `hu.blackbelt.judo.zeta.validation.core`):
- `ValidationRegistry`
- `ValidationExecutor`
- `ValidationContext`
- `ValidationResult`
- `ValidationRule`
- `Severity`

**Common classes** (from `hu.blackbelt.judo.zeta.common`):
- `ExtensionMethodRegistry`
- `ModelProvider`

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
}
```

## Validation Rule Structure

### Package Location

Validation classes go in: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/`

### Basic Validator Class (Using Constants)

```java
package hu.blackbelt.judo.meta.asm.validation;

import hu.blackbelt.judo.zeta.annotation.*;
import hu.blackbelt.judo.zeta.validation.core.*;
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
            Collection<EClass> allClasses = ctx.getAllInstances(EClass.class);
            
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
import hu.blackbelt.judo.zeta.annotation.ValidationContext;

@ValidationContext(EClass.class)
public class EClassValidations {
    // All rules in this class validate EClass instances
}
```

### @Constraint (Method-Level)

Error-level validation rule. Model is invalid if this fails.

```java
import hu.blackbelt.judo.zeta.annotation.Constraint;

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

### @Critique (Method-Level)

Warning-level validation rule. Recommendations and best practices.

```java
import hu.blackbelt.judo.zeta.annotation.Critique;

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

### @Guard (Method-Level)

Conditional execution - rule only runs if guard returns true.

```java
import hu.blackbelt.judo.zeta.annotation.Guard;

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
import hu.blackbelt.judo.zeta.annotation.Satisfies;

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
import hu.blackbelt.judo.zeta.annotation.Cached;

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
import hu.blackbelt.judo.zeta.annotation.ExtensionMethod;

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
import hu.blackbelt.judo.zeta.validation.core.ValidationResult;
import hu.blackbelt.judo.zeta.validation.core.Severity;

// Passing result
ValidationResult.pass()

// Failing result (use constant for name!)
ValidationResult.fail(CONSTRAINT_NAME, "Error message", Severity.ERROR, element)

// Simple fail
ValidationResult.fail("Error message")

// Warning result (use constant for name!)
ValidationResult.warn(CRITIQUE_NAME, "Warning message", element)
```

## ValidationContext API

```java
import hu.blackbelt.judo.zeta.validation.core.ValidationContext;

// Get all instances of a type in the model
Collection<EntityType> allEntities = ctx.getAllInstances(EntityType.class);

// Check if another constraint passed (use constant!)
boolean passed = ctx.satisfies(OTHER_CONSTRAINT_NAME);

// Check if constraint passed for specific element
boolean passed = ctx.satisfies(element, OTHER_CONSTRAINT_NAME);

// Call an extension method
Object result = ctx.call(entity, "getAllSuperTypes");
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
        Collection<EntityType> allEntities = ctx.getAllInstances(EntityType.class);
        
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
}
```

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
3. [ ] Use static imports for all constants
4. [ ] Use constant in `@Constraint`/`@Critique` name parameter
5. [ ] Use constant in `ValidationResult.fail()`/`ValidationResult.warn()`
6. [ ] Use constant in `@Guard(method = ...)` if applicable
7. [ ] Use constants in `@Satisfies(constraints = {...})` if applicable
8. [ ] Register validation class in `AsmValidator.java`
9. [ ] Write parameterized test using constants for expected errors/warnings

## See Also

- `docs/validation/index.adoc` - Validation framework overview
- `docs/validation/annotations.adoc` - Annotation reference
- `docs/validation/validation-context.adoc` - API reference
- https://github.com/BlackBeltTechnology/judo-zeta - Judo Zeta Framework
