# model-test Specification

## Purpose

The `model-test` module provides JUnit 5 unit tests that verify the correctness of the `model` module's runtime utilities, builder API, validation engine, annotation handling, and inheritance resolution.

## Architecture

All tests reside in `hu.blackbelt.judo.meta.asm.runtime`:

- **`ExecutionContextOnAsmTest`** — Abstract base class that builds a comprehensive test model with packages, data types, classes, attributes, references, and operations; runs Epsilon validation during setup
- **`AsmUtilsTest`** — Tests FQN resolution for packages, classifiers, attributes, references, and operations
- **`AsmValidationTest`** — Tests the EVL validation engine against empty/invalid models
- **`EmfBuilderTest`** — Tests the fluent builder API by constructing valid and invalid company metamodels
- **`AnnotationTest`** — Tests extended metadata (EAnnotation) read/write/create operations
- **`InheritanceTest`** — Tests Ecore inheritance and inherited feature resolution using builder-constructed class hierarchies

## Requirements

### Requirement: FQN resolution correctness

Test classes SHALL verify that `AsmUtils` computes correct fully-qualified names for all Ecore element types.

#### Scenario: Package FQN resolution
- **GIVEN** a test model with nested EPackages
- **WHEN** `AsmUtils.getPackageFQName()` is called on each package
- **THEN** the FQN matches the expected dot-separated path

#### Scenario: Classifier FQN resolution
- **GIVEN** an EClass inside a named EPackage
- **WHEN** `AsmUtils.getClassifierFQName()` is called
- **THEN** the result is `packageFQN.className`

#### Scenario: Attribute FQN resolution
- **GIVEN** an EAttribute on an EClass
- **WHEN** `AsmUtils.getAttributeFQName()` is called
- **THEN** the result is `classifierFQN#attributeName`

#### Scenario: Simple name resolution
- **GIVEN** a ResourceSet with known classifiers
- **WHEN** `asmUtils.resolve(fqName)` is called
- **THEN** the correct `Optional<EClassifier>` is returned

### Requirement: Builder API correctness

Tests SHALL verify that the generated builder API produces valid Ecore models.

#### Scenario: Build valid company metamodel
- **GIVEN** builder calls for EPackage, EClass, EAttribute, EReference
- **WHEN** a complete company metamodel is built using `newEPackageBuilder()`, `newEClassBuilder()`, etc.
- **THEN** the resulting model passes EVL validation

#### Scenario: Build invalid model
- **GIVEN** builder calls that produce constraint violations
- **WHEN** the invalid metamodel is validated
- **THEN** validation reports the expected errors

### Requirement: EVL validation engine

Tests SHALL verify that `AsmEpsilonValidator` correctly identifies model constraint violations.

#### Scenario: Validate empty model
- **GIVEN** an AsmModel with no content
- **WHEN** EVL validation runs
- **THEN** expected validation errors are reported

### Requirement: Extended metadata operations

Tests SHALL verify EAnnotation read/write operations under the ExtendedMetadata namespace.

#### Scenario: Get existing annotation
- **GIVEN** an EClass with a pre-existing ExtendedMetadata annotation
- **WHEN** `getExtensionAnnotationByName()` is called
- **THEN** the annotation is found and its value is correct

#### Scenario: Create annotation on demand
- **GIVEN** an EClass without the requested annotation
- **WHEN** `getExtensionAnnotationByName(element, name, true)` is called with `createIfNotExists=true`
- **THEN** a new EAnnotation is created and returned

### Requirement: Inheritance feature resolution

Tests SHALL verify that Ecore inheritance correctly propagates structural features to subclasses.

#### Scenario: Inherited attributes
- **GIVEN** a parent EClass with attributes and a child EClass extending it
- **WHEN** `child.getEAllAttributes()` is queried
- **THEN** the inherited attributes from the parent are included
