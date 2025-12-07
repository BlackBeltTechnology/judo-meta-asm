# Zeta Validation Skill

This skill helps create and describe proper validation rules and tests for the Judo Zeta Validation Framework.

## Overview

The Judo Zeta Validation Framework provides annotation-based validation for EMF models. ASM uses the [Judo Zeta](https://github.com/BlackBeltTechnology/judo-zeta) library for validation.

Use this skill when you need to:

- Create new validation rules (`@Constraint` or `@Critique`)
- Write parameterized tests for validation rules
- Understand validation patterns and best practices

> **IMPORTANT**: Always use constants for constraint names, guard method names, critique names, satisfies constraint references, and ValidationResult names. Never use string literals directly in annotations or validation results.

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

> **IMPORTANT**: Always use constants for ALL validation-related names. This provides compile-time safety, IDE autocomplete, and easier refactoring. Never use string literals directly.

### Constants Naming Convention

Convert PascalCase constraint names to UPPER_SNAKE_CASE constants:

| Constraint Name (PascalCase) | Constant Name (UPPER_SNAKE_CASE) |
|------------------------------|----------------------------------|
| `EClassMustHaveName` | `ECLASS_MUST_HAVE_NAME` |
| `EntityNamesAreUnique` | `ENTITY_NAMES_ARE_UNIQUE` |
| `NoCyclicInheritance` | `NO_CYCLIC_INHERITANCE` |
| `AttributeMustHaveType` | `ATTRIBUTE_MUST_HAVE_TYPE` |
| `ConcreteEntityMustHaveTable` | `CONCRETE_ENTITY_MUST_HAVE_TABLE` |

### ConstraintNames.java

Location: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/ConstraintNames.java`

```java
package hu.blackbelt.judo.meta.asm.validation;

/**
 * Centralized constants for all validation constraint and critique names.
 * 
 * <p>Usage:</p>
 * <pre>
 * import static hu.blackbelt.judo.meta.asm.validation.ConstraintNames.*;
 * 
 * {@literal @}Constraint(name = ECLASS_MUST_HAVE_NAME, message = "...")
 * </pre>
 */
public final class ConstraintNames {

    private ConstraintNames() {
        throw new UnsupportedOperationException("Utility class");
    }

    // EClass Constraints
    public static final String ECLASS_MUST_HAVE_NAME = "EClassMustHaveName";
    public static final String ECLASS_NAMES_ARE_UNIQUE = "EClassNamesAreUnique";
    public static final String NO_CYCLIC_INHERITANCE = "NoCyclicInheritance";
    public static final String CONCRETE_ECLASS_MUST_HAVE_ATTRIBUTES = "ConcreteEClassMustHaveAttributes";
    
    // EAttribute Constraints
    public static final String EATTRIBUTE_MUST_HAVE_NAME = "EAttributeMustHaveName";
    public static final String EATTRIBUTE_MUST_HAVE_TYPE = "EAttributeMustHaveType";
    public static final String EATTRIBUTE_NAMES_UNIQUE_IN_CLASS = "EAttributeNamesUniqueInClass";
    
    // EReference Constraints
    public static final String EREFERENCE_TARGET_MUST_EXIST = "EReferenceTargetMustExist";
    public static final String EREFERENCE_MUST_HAVE_NAME = "EReferenceMustHaveName";
}
```

### GuardMethodNames.java

Location: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/GuardMethodNames.java`

```java
package hu.blackbelt.judo.meta.asm.validation;

/**
 * Centralized constants for guard method names.
 * 
 * <p>The constant value must match the actual method name exactly.</p>
 */
public final class GuardMethodNames {

    private GuardMethodNames() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String IS_ABSTRACT = "isAbstract";
    public static final String IS_CONCRETE = "isConcrete";
    public static final String HAS_SUPERTYPE = "hasSupertype";
    public static final String HAS_NAME = "hasName";
    public static final String IS_NOT_PROXY = "isNotProxy";
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
    public static final String HAS_ATTRIBUTE = "hasAttribute";
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

    // Guard method - name MUST match the IS_CONCRETE constant value
    public boolean isConcrete(EObject element, ValidationContext ctx) {
        return !((EClass) element).isAbstract();
    }
}
```

## Common Validation Patterns (All Using Constants)

### 1. Required Field (Not Null/Empty)

```java
import static hu.blackbelt.judo.meta.asm.validation.ConstraintNames.*;

@Constraint(name = ECLASS_MUST_HAVE_NAME, message = "Name is required")
public ValidationRule eClassMustHaveName() {
    return (element, ctx) -> {
        EClass eClass = (EClass) element;
        return eClass.getName() != null && !eClass.getName().trim().isEmpty()
            ? ValidationResult.pass()
            : ValidationResult.fail(ECLASS_MUST_HAVE_NAME, "EClass name is required", Severity.ERROR, element);
    };
}
```

### 2. Uniqueness Check

```java
@Satisfies(constraints = {ECLASS_MUST_HAVE_NAME})
@Constraint(name = ECLASS_NAMES_ARE_UNIQUE, message = "Name must be unique")
public ValidationRule eClassNamesAreUnique() {
    return (element, ctx) -> {
        EClass eClass = (EClass) element;
        Collection<EClass> allClasses = ctx.getAllInstances(EClass.class);
        
        long count = allClasses.stream()
            .filter(c -> eClass.getName().equals(c.getName()))
            .count();
        
        return count == 1
            ? ValidationResult.pass()
            : ValidationResult.fail(ECLASS_NAMES_ARE_UNIQUE, 
                "Duplicate name: " + eClass.getName(), Severity.ERROR, element);
    };
}
```

### 3. Conditional Validation with Guard

```java
@Guard(method = IS_CONCRETE)
@Constraint(name = CONCRETE_ECLASS_MUST_HAVE_ATTRIBUTES, message = "...")
public ValidationRule concreteEClassMustHaveAttributes() {
    return (element, ctx) -> {
        EClass eClass = (EClass) element;
        
        if (eClass.getEAttributes().isEmpty()) {
            return ValidationResult.fail(
                CONCRETE_ECLASS_MUST_HAVE_ATTRIBUTES,
                "Concrete EClass '" + eClass.getName() + "' must have attributes",
                Severity.ERROR,
                element
            );
        }
        
        return ValidationResult.pass();
    };
}

// Guard method - name matches IS_CONCRETE constant value
public boolean isConcrete(EObject element, ValidationContext ctx) {
    return !((EClass) element).isAbstract();
}
```

### 4. Cycle Detection

```java
@Satisfies(constraints = {ECLASS_MUST_HAVE_NAME})
@Constraint(name = NO_CYCLIC_INHERITANCE, message = "Circular inheritance not allowed")
public ValidationRule noCyclicInheritance() {
    return (element, ctx) -> {
        EClass eClass = (EClass) element;
        
        if (hasCycle(eClass, new HashSet<>())) {
            return ValidationResult.fail(
                NO_CYCLIC_INHERITANCE,
                "EClass '" + eClass.getName() + "' has circular inheritance",
                Severity.ERROR,
                element
            );
        }
        
        return ValidationResult.pass();
    };
}

private boolean hasCycle(EClass eClass, Set<EClass> visited) {
    if (visited.contains(eClass)) {
        return true;
    }
    visited.add(eClass);
    for (EClass superType : eClass.getESuperTypes()) {
        if (hasCycle(superType, visited)) {
            return true;
        }
    }
    visited.remove(eClass);
    return false;
}
```

### 5. Collection Validation

```java
@Constraint(name = ECLASS_MUST_HAVE_ATTRIBUTES, message = "Must have at least one attribute")
public ValidationRule eClassMustHaveAttributes() {
    return (element, ctx) -> {
        EClass eClass = (EClass) element;
        
        if (eClass.getEAttributes().isEmpty()) {
            return ValidationResult.fail(
                ECLASS_MUST_HAVE_ATTRIBUTES,
                "EClass '" + eClass.getName() + "' must have at least one attribute",
                Severity.ERROR,
                element
            );
        }
        
        return ValidationResult.pass();
    };
}
```

### 6. Pattern Matching

```java
@Satisfies(constraints = {ECLASS_MUST_HAVE_NAME})
@Constraint(name = ECLASS_NAME_MUST_BE_VALID_IDENTIFIER, message = "Name must be valid identifier")
public ValidationRule eClassNameMustBeValidIdentifier() {
    return (element, ctx) -> {
        EClass eClass = (EClass) element;
        String name = eClass.getName();
        
        if (!name.matches("^[A-Za-z][A-Za-z0-9_]*$")) {
            return ValidationResult.fail(
                ECLASS_NAME_MUST_BE_VALID_IDENTIFIER,
                "Name '" + name + "' must start with letter and contain only letters, digits, underscores",
                Severity.ERROR,
                element
            );
        }
        
        return ValidationResult.pass();
    };
}
```

### 7. Reference Validation

```java
@Constraint(name = EREFERENCE_TARGET_MUST_EXIST, message = "Reference target must exist")
public ValidationRule eReferenceTargetMustExist() {
    return (element, ctx) -> {
        EReference ref = (EReference) element;
        EClass target = ref.getEReferenceType();
        
        if (target == null || target.eIsProxy()) {
            return ValidationResult.fail(
                EREFERENCE_TARGET_MUST_EXIST,
                "Reference '" + ref.getName() + "' has invalid or missing target type",
                Severity.ERROR,
                element
            );
        }
        
        return ValidationResult.pass();
    };
}
```

### 8. Cross-Element Validation

```java
@Satisfies(constraints = {EATTRIBUTE_MUST_HAVE_NAME})
@Constraint(name = EATTRIBUTE_NAMES_UNIQUE_IN_CLASS, message = "Attribute names must be unique within class")
public ValidationRule eAttributeNamesUniqueInClass() {
    return (element, ctx) -> {
        EAttribute attr = (EAttribute) element;
        EClass containingClass = attr.getEContainingClass();
        
        if (containingClass == null) {
            return ValidationResult.pass();
        }
        
        long count = containingClass.getEAttributes().stream()
            .filter(a -> attr.getName().equals(a.getName()))
            .count();
        
        return count == 1
            ? ValidationResult.pass()
            : ValidationResult.fail(
                EATTRIBUTE_NAMES_UNIQUE_IN_CLASS,
                "Duplicate attribute name '" + attr.getName() + "' in class '" + containingClass.getName() + "'",
                Severity.ERROR,
                element
            );
    };
}
```

### 9. Warning/Critique Pattern

```java
@Satisfies(constraints = {ECLASS_MUST_HAVE_NAME})
@Critique(name = ECLASS_SHOULD_HAVE_DOCUMENTATION, message = "EClass should have documentation")
public ValidationRule eClassShouldHaveDocumentation() {
    return (element, ctx) -> {
        EClass eClass = (EClass) element;
        
        boolean hasDoc = eClass.getEAnnotations().stream()
            .anyMatch(a -> "http://www.eclipse.org/emf/2002/GenModel".equals(a.getSource())
                && a.getDetails().containsKey("documentation"));
        
        return hasDoc
            ? ValidationResult.pass()
            : ValidationResult.warn(
                ECLASS_SHOULD_HAVE_DOCUMENTATION,
                "EClass '" + eClass.getName() + "' should have documentation",
                element
            );
    };
}
```

## Writing Tests (Using Constants)

### Test Location

Tests go in: `model-test/src/test/java/hu/blackbelt/judo/meta/asm/runtime/`

### Parameterized Test Structure

```java
package hu.blackbelt.judo.meta.asm.runtime;

import hu.blackbelt.judo.meta.asm.AbstractAsmValidationTest;
import hu.blackbelt.judo.meta.asm.ValidatorType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.google.common.collect.ImmutableList;

import static hu.blackbelt.judo.meta.asm.validation.ConstraintNames.*;

public class EClassValidationTest extends AbstractAsmValidationTest {

    @ParameterizedTest(name = "testValidModelPasses [{0}]")
    @EnumSource(ValidatorType.class)
    void testValidModelPasses(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();
        
        // Build valid model...
        
        runValidation(ImmutableList.of(), ImmutableList.of());
    }

    @ParameterizedTest(name = "testMissingNameFails [{0}]")
    @EnumSource(ValidatorType.class)
    void testMissingNameFails(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();
        
        // Build model with missing name...
        
        // Use constant!
        runValidation(
            ImmutableList.of(ECLASS_MUST_HAVE_NAME),
            ImmutableList.of()
        );
    }

    @ParameterizedTest(name = "testDuplicateNameWarns [{0}]")
    @EnumSource(ValidatorType.class)
    void testDuplicateNameWarns(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();
        
        // Build model with duplicate names...
        
        // Use constant!
        runValidation(
            ImmutableList.of(),
            ImmutableList.of(ECLASS_NAMES_ARE_UNIQUE)
        );
    }
}
```

## Best Practices

1. **Always use constants** - Never use string literals for constraint names, guard methods, or satisfies references
2. **Use static imports** - Import constants classes statically for clean code
3. **One validator class per element type** - Organize validators by the type they validate
4. **Use @Satisfies for dependencies** - Ensure prerequisite constraints pass before running dependent rules
5. **Use @Guard for conditional rules** - Skip rules that don't apply to certain elements
6. **Actionable error messages** - Include element name and guidance on how to fix
7. **Test with both validators** - Use parameterized tests with `ValidatorType.class`

## EVL Migration Reference

When migrating from EVL to Java validation, use this mapping:

| EVL Construct | Java Equivalent (with Constants) |
|---------------|----------------------------------|
| `context EntityType` | `@ValidationContext(EntityType.class)` |
| `constraint MustHaveName` | `@Constraint(name = MUST_HAVE_NAME, ...)` |
| `critique ShouldHaveDoc` | `@Critique(name = SHOULD_HAVE_DOC, ...)` |
| `guard: self.isAbstract()` | `@Guard(method = IS_ABSTRACT)` |
| `satisfies MustHaveName` | `@Satisfies(constraints = {MUST_HAVE_NAME})` |
| `check: ...` | `return ValidationResult.pass()` or `.fail(CONSTRAINT_NAME, ...)` |
| `message: "..."` | `message = "..."` in annotation |

## Registering Validators

Validators must be registered in `AsmValidator.java`:

```java
public static void validateAsm(...) {
    ValidationRegistry registry = new ValidationRegistry();
    
    // Register validation classes
    registry.register(EClassValidations.class);
    registry.register(EAttributeValidations.class);
    registry.register(EReferenceValidations.class);
    
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
