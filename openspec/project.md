# Project Context

## Purpose

**Judo ASM (Architecture Specific Model) Meta** is an Eclipse/Tycho-based metamodel project that:
- Wraps the EMF/Ecore metamodel for programmatic access
- Provides runtime utilities (`AsmUtils`) for navigating Ecore structures
- Implements validation via Epsilon Validation Language (EVL)
- Supports OSGi deployment and Karaf container integration
- Distributes via Maven Central and Eclipse P2 repositories

## Tech Stack

### Core Technologies
- **Java 21** - Primary language
- **Eclipse Modeling Framework (EMF)** 2.38.0+ - Metamodel foundation
- **Ecore** - Model definition (core metamodel that ASM wraps)
- **MWE2** (Model Workflow Engine) 2.13.0 - Code generation workflows
- **Epsilon** 2.8.0 - Model validation (EVL) and object language (EOL)
- **Tycho** 4.0.13 - Eclipse plugin build

### Runtime
- **Apache Karaf** 4.4.7 - OSGi container
- **Apache Felix** 6.0.0 - OSGi bundle plugin

### Build & Testing
- **Maven** 3.9.4+ with wrapper
- **JUnit 5** - Unit testing
- **Pax Exam** 4.13.5 - OSGi integration testing

## Project Conventions

### Code Style
- Java 21 language features (records, pattern matching, sealed classes where applicable)
- Use Lombok for boilerplate reduction (`@Getter`, `@Setter`, `@Builder`, `@Slf4j`)
- EMF-generated code follows GenModel conventions
- Immutable objects preferred for validation results and cache keys
- Functional interfaces for validation rules and guards

### Architecture Patterns
- **EMF/Ecore patterns** for metamodel definition and manipulation
- **Annotation-based configuration** for validation rules
- **Functional interfaces** for validation logic (lambdas supported)
- **Registry pattern** for scanning and discovering validators
- **Caching** for expensive operations (AsmUtilsCache)

### Testing Strategy
- Unit tests in `model-test/` module using JUnit 5
- EVL validation tests use `AsmEpsilonValidator.validateAsm()`
- Expected errors/warnings passed to validator for assertion
- Model fixtures created using EMF builders (`newEClassBuilder()`, etc.)
- OSGi integration tests via Pax Exam in `osgi-itest/`

### Git Workflow
- **Main Branch:** `develop`
- **Versioning:** SNAPSHOT-based development
- Feature branches for significant changes
- OpenSpec proposals for architectural changes

## Domain Context

### ASM Metamodel
ASM wraps the **Ecore metamodel** - the Eclipse Modeling Framework's core metamodel:

| Ecore Element | Purpose |
|---------------|---------|
| `EPackage` | Package containers |
| `EClass` | Class definitions |
| `EAttribute` | Data attributes |
| `EReference` | Inter-object references |
| `EDataType` | Primitive and custom types |
| `EEnum` | Enumeration types |
| `EOperation` | Operations/methods |
| `EStructuralFeature` | Attributes and references |
| `EAnnotation` | Metadata annotations |

### Validation System
Current validation uses **Epsilon Validation Language (EVL)**:
- Entry point: `model/src/main/epsilon/validations/asm-plugin-validation.evl`
- Main file: `model/src/main/epsilon/validations/asm.evl` (currently minimal)
- Relies primarily on Ecore's built-in OCL constraints
- `AsmUtils` injected as context variable for helper operations

### Key Validation Concepts
- **Constraint**: Error-level rule that must pass
- **Critique**: Warning-level rule (advisory)
- **Guard**: Condition that determines if rule should evaluate
- **Satisfies**: Dependency on another constraint's result (cached)
- **Context**: EClass type the rule applies to

## Important Constraints

### Build Constraints
- Tycho build requires Eclipse plugin structure
- OSGi bundle manifests must be maintained
- P2 update site structure for Eclipse distribution

### Validation Constraints
- EVL rules must remain functional during Java framework migration
- Test parity: Java tests must mirror EVL tests exactly
- Error message format consistency between validators

## External Dependencies

### Eclipse Platform
- EMF Runtime 2.38.0+
- Ecore metamodel

### Epsilon Runtime
- EVL (Epsilon Validation Language) for model validation
- EOL (Epsilon Object Language) for helper operations
- EMC (Epsilon Model Connectivity) for EMF integration

### Judo Zeta Framework
- `judo-zeta` - Core validation framework (annotations and runtime)
- Located at `/Users/robson/judo-ng/runtime/judo-zeta`
- Provides: `@ValidationContext`, `@Constraint`, `@Critique`, `@Guard`, `@Satisfies`

## Module Overview

| Module | Purpose |
|--------|---------|
| `model/` | Core ASM runtime, AsmUtils, Epsilon validation scripts |
| `model-test/` | Unit tests for AsmUtils and validation |
| `osgi/` | OSGi bundle repackaging |
| `osgi-itest/` | OSGi integration tests |
| `feature/` | Eclipse feature packaging |
| `site/` | P2 update site |

## Active Changes

See `openspec/changes/` for in-progress proposals.
