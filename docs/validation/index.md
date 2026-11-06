# ASM Model Validation Framework

## Table of Contents
- [Introduction](#introduction)
- [Validation Approaches](#validation-approaches)
- [Dependencies](#dependencies)
- [Architecture](#architecture)
- [Core Annotations](#core-annotations)
- [Quick Example](#quick-example)
- [Executing Validation](#executing-validation)
- [Test Infrastructure](#test-infrastructure)
- [Related Documentation](#related-documentation)

## Introduction

The ASM (Architecture Specific Model) validation framework provides dual validation capabilities for EMF-based ASM models. It supports both EVL (Epsilon Validation Language) scripts and Java-based validation using the [Judo Zeta Validation Framework](https://github.com/BlackBeltTechnology/judo-zeta).

## Validation Approaches

### EVL (Epsilon Validation Language)

EVL is the traditional script-based validation approach that uses the Epsilon platform. EVL scripts are stored in `.evl` files and provide a declarative way to define validation rules.

**Location**: `model/src/main/epsilon/validations/`

### Java Validation (Judo Zeta)

The Java validation framework uses the [Judo Zeta](https://github.com/BlackBeltTechnology/judo-zeta) library for annotation-based validation that offers:

* **Type Safety** - Compile-time type checking
* **IDE Support** - Full autocomplete, refactoring, and debugging
* **Performance** - No interpretation overhead, automatic parallelization
* **Testability** - Standard unit testing for validation rules
* **Maintainability** - Familiar Java code

**Validation Entry Point**: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/AsmValidator.java`

## Dependencies

The ASM validation uses the Judo Zeta framework. Add these dependencies to your `pom.xml`:

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

## Architecture

```
                    ┌─────────────────────────────────────────┐
                    │           AsmValidator                  │
                    │  (Entry point for Java validation)      │
                    └─────────────────────┬───────────────────┘
                                          │
                                          │ uses
                                          ▼
                    ┌─────────────────────────────────────────┐
                    │         Judo Zeta Framework             │
                    │  hu.blackbelt.judo.zeta.validation.core │
                    └─────────────────────┬───────────────────┘
                                          │
                    ┌─────────────────────┴───────────────────┐
                    │                                         │
        ┌───────────▼───────────┐             ┌───────────────▼───────────┐
        │  ValidationRegistry   │             │  ExtensionMethodRegistry  │
        │  (Scans annotations)  │             │  (Helper methods)         │
        └───────────┬───────────┘             └───────────────────────────┘
                    │
        ┌───────────▼───────────┐
        │  ValidationExecutor   │
        │  (Executes rules)     │
        └───────────┬───────────┘
                    │
        ┌───────────▼───────────┐
        │   ValidationResult    │
        │  (Pass/Fail/Warning)  │
        └───────────────────────┘
```

## Core Annotations

All annotations are from the `hu.blackbelt.judo.zeta.annotation` package:

| Annotation | Level | Purpose |
|------------|-------|---------|
| `@ValidationContext` | Class | Declares the element type this class validates |
| `@Constraint` | Method | Defines an error-level validation rule |
| `@Critique` | Method | Defines a warning-level validation rule |
| `@Guard` | Method | Adds a conditional guard to a rule |
| `@Satisfies` | Method | Declares dependencies on other constraints |
| `@Cached` | Method | Caches the result of expensive computations |
| `@ExtensionMethod` | Method | Defines a reusable helper method |

## Quick Example

```java
package hu.blackbelt.judo.meta.asm.validation;

import hu.blackbelt.judo.zeta.annotation.*;
import hu.blackbelt.judo.zeta.validation.core.*;
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

            if (eClass.getName() == null || eClass.getName().isEmpty()) {
                return ValidationResult.fail("EClass name is required");
            }

            return ValidationResult.pass();
        };
    }

    @Critique(
        name = "EClassShouldHaveDocumentation",
        message = "EClass should have documentation"
    )
    public ValidationRule eClassShouldHaveDocumentation() {
        return (element, ctx) -> {
            EClass eClass = (EClass) element;

            // Check for EAnnotation with documentation
            boolean hasDoc = eClass.getEAnnotations().stream()
                .anyMatch(a -> "http://www.eclipse.org/emf/2002/GenModel".equals(a.getSource())
                    && a.getDetails().containsKey("documentation"));

            return hasDoc
                ? ValidationResult.pass()
                : ValidationResult.warn("Consider adding documentation");
        };
    }
}
```

## Executing Validation

```java
import hu.blackbelt.judo.meta.asm.validation.AsmValidator;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;

public class ValidationExample {

    public void validateModel(AsmModel asmModel) throws AsmModel.AsmValidationException {
        // Validate with expected errors/warnings
        AsmValidator.validateAsm(
            log,
            asmModel,
            Collections.emptyList(),  // Expected errors
            Collections.emptyList(),  // Expected warnings
            false                     // Parallel execution
        );
    }
}
```

## Test Infrastructure

The ASM validation test infrastructure supports parameterized testing with both EVL and Java validators:

```java
import hu.blackbelt.judo.meta.asm.ValidatorType;
import hu.blackbelt.judo.meta.asm.AbstractAsmValidationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class AsmValidationTest extends AbstractAsmValidationTest {

    @ParameterizedTest(name = "testValidation [{0}]")
    @EnumSource(ValidatorType.class)
    void testValidation(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();

        // Test runs with both EVL and JAVA validators
        runValidation(
            ImmutableList.of(),  // Expected errors
            ImmutableList.of()   // Expected warnings
        );
    }
}
```

## Related Documentation

* [Judo Zeta Framework](https://github.com/BlackBeltTechnology/judo-zeta) - The validation framework source and documentation
* [Annotations Reference](annotations.md) - Complete annotation documentation
* [ValidationContext API](validation-context.md) - Full API reference

## See Also

* [ASM Model Documentation](../../README.md)
* [EVL Documentation](https://www.eclipse.org/epsilon/doc/evl/) - Epsilon Validation Language
