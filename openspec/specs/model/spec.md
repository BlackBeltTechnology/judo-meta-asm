# model Specification

## Purpose

The `model` module is the core Eclipse plugin that wraps the standard EMF Ecore metamodel with JUDO-specific runtime classes, generated builder/helper utilities, Epsilon EVL validation, and CLI SPI integration. It provides `AsmModel` for model lifecycle management, `AsmUtils` for model introspection and FQN resolution, `AsmEpsilonValidator` for constraint checking, and `AsmModelResourceSupport` for EMF ResourceSet management.

## Architecture

The module is organized into four packages:

- **`hu.blackbelt.judo.meta.asm.runtime`** — Core runtime: `AsmModel` (model wrapper with builder pattern), `AsmUtils` (FQN resolution, annotation access, type classification, stream queries), `AsmEpsilonValidator` (EVL validation), `AsmUtilsCache` (caching layer)
- **`hu.blackbelt.judo.meta.asm.support`** — `AsmModelResourceSupport` manages EMF ResourceSets, XMI serialization, URI handling, and diagnostics
- **`hu.blackbelt.judo.meta.asm.cli`** — CLI SPI implementations: `AsmValidatorImpl` (ModelValidator) and `AsmFqnResolverImpl` (FqnResolver)
- **`hu.blackbelt.judo.eclipse.asm`** — Eclipse plugin `Activator` for bundle lifecycle

Generated code in `src-gen/` provides fluent builder classes (e.g., `EClassBuilder`, `EAttributeBuilder`) and helper classes produced by the MWE2 workflow from `model/Ecore.ecore`.

## Requirements

### Requirement: Model construction via builder pattern

`AsmModel` SHALL provide a fluent builder API (`AsmModel.buildAsmModel()`) that creates a configured model instance with URI, ResourceSet, and optional URIHandler.

#### Scenario: Build an empty ASM model
- **GIVEN** no pre-existing model
- **WHEN** `AsmModel.buildAsmModel().uri(uri).build()` is called
- **THEN** an `AsmModel` instance is returned with an initialized ResourceSet and Resource at the given URI

#### Scenario: Build model with custom ResourceSet
- **GIVEN** a pre-configured `ResourceSet`
- **WHEN** `AsmModel.buildAsmModel().resourceSet(rs).uri(uri).build()` is called
- **THEN** the model uses the provided ResourceSet instead of creating a new one

### Requirement: Model loading from various sources

`AsmModel` SHALL support loading models from files, URIs, and InputStreams via `AsmModel.loadAsmModel(LoadArguments)`.

#### Scenario: Load model from file
- **GIVEN** an XMI file containing a serialized Ecore model
- **WHEN** `AsmModel.loadAsmModel(asmLoadArgumentsBuilder().file(file))` is called
- **THEN** the model is deserialized and accessible via `getResourceSet()` and `getResource()`

#### Scenario: Load model with validation enabled
- **GIVEN** a model file and `validateModel(true)`
- **WHEN** the model is loaded
- **THEN** EVL validation runs and `AsmValidationException` is thrown if validation fails

### Requirement: Model saving to various targets

`AsmModel` SHALL support saving models to files and OutputStreams via `saveAsmModel(SaveArguments)`.

#### Scenario: Save model to file
- **GIVEN** a populated `AsmModel`
- **WHEN** `saveAsmModel(asmSaveArgumentsBuilder().file(outputFile))` is called
- **THEN** the model is serialized to XMI format at the specified path

### Requirement: FQN resolution for Ecore elements

`AsmUtils` SHALL compute fully-qualified names for EPackage, EClassifier, EAttribute, EReference, and EOperation using dot-separated package paths and `#` feature/operation separators.

#### Scenario: Package FQN
- **GIVEN** a nested EPackage `com.example.model`
- **WHEN** `AsmUtils.getPackageFQName(ePackage)` is called
- **THEN** the result is `"com.example.model"`

#### Scenario: Attribute FQN
- **GIVEN** an EAttribute `name` on EClass `Person` in package `com.example`
- **WHEN** `AsmUtils.getAttributeFQName(eAttribute)` is called
- **THEN** the result is `"com.example.Person#name"`

#### Scenario: Resolve classifier by FQN
- **GIVEN** a ResourceSet with an EClass at FQN `"com.example.Person"`
- **WHEN** `asmUtils.resolve("com.example.Person")` is called
- **THEN** an `Optional<EClassifier>` containing the EClass is returned

### Requirement: Extended metadata via EAnnotation

`AsmUtils` SHALL read and write extended metadata annotations under the namespace URI `http://blackbelt.hu/judo/meta/ExtendedMetadata`.

#### Scenario: Get annotation value
- **GIVEN** an EClass with an EAnnotation containing source `http://blackbelt.hu/judo/meta/ExtendedMetadata/entity` and detail key `value` = `"true"`
- **WHEN** `AsmUtils.getExtensionAnnotationValue(eClass, "entity", false)` is called
- **THEN** `Optional.of("true")` is returned

#### Scenario: Add annotation
- **GIVEN** an EClass without the `entity` annotation
- **WHEN** `AsmUtils.addExtensionAnnotation(eClass, "entity", "true")` is called
- **THEN** an EAnnotation is created with the correct source URI and detail entry

### Requirement: Type classification

`AsmUtils` SHALL provide static methods to classify EDataTypes and EClasses into JUDO-specific categories (entity type, actor type, numeric, string, boolean, date, timestamp, enumeration, etc.).

#### Scenario: Identify entity type
- **GIVEN** an EClass annotated as entity
- **WHEN** `AsmUtils.isEntityType(eClass)` is called
- **THEN** `true` is returned

#### Scenario: Classify data type as numeric
- **GIVEN** an EDataType representing an integer
- **WHEN** `AsmUtils.isNumeric(eDataType)` is called
- **THEN** `true` is returned

### Requirement: Stream-based model queries

`AsmUtils` SHALL provide `all(Class<T>)` to stream all model elements of a given type from the ResourceSet.

#### Scenario: Stream all EClasses
- **GIVEN** a ResourceSet containing multiple EClasses
- **WHEN** `asmUtils.all(EClass.class)` is called
- **THEN** a `Stream<EClass>` of all EClass instances in the model is returned

### Requirement: Epsilon EVL validation

`AsmEpsilonValidator` SHALL execute Epsilon Validation Language scripts against an `AsmModel` and report errors/warnings.

#### Scenario: Validate a valid model
- **GIVEN** a well-formed AsmModel and EVL script URI
- **WHEN** `AsmEpsilonValidator.validateAsm(log, model, scriptUri)` is called
- **THEN** validation completes without throwing `ScriptExecutionException`

#### Scenario: Validate with expected errors
- **GIVEN** an AsmModel with known constraint violations
- **WHEN** `validateAsm(log, model, scriptUri, expectedErrors, expectedWarnings)` is called
- **THEN** only the expected errors/warnings are reported; unexpected violations cause failure

### Requirement: CLI model validation SPI

`AsmValidatorImpl` SHALL implement `ModelValidator` from judo-cli-api, delegating to `AsmEpsilonValidator` for EVL-based validation.

#### Scenario: Validate via CLI SPI
- **GIVEN** an `AsmModel` instance
- **WHEN** `asmValidatorImpl.validate(logger, asmModel)` is called
- **THEN** EVL validation runs against the model using the bundled validation scripts

#### Scenario: Reject non-AsmModel input
- **GIVEN** an object that is not an `AsmModel`
- **WHEN** `validate(logger, wrongObject)` is called
- **THEN** `IllegalArgumentException` is thrown

### Requirement: CLI FQN resolution SPI

`AsmFqnResolverImpl` SHALL implement `FqnResolver` from judo-cli-api, providing FQN-based lookup with caching for EPackage, EClassifier, EAttribute, EReference, EOperation, EEnumLiteral, EParameter, and EAnnotation.

#### Scenario: Bind and resolve
- **GIVEN** a ResourceSet with Ecore elements
- **WHEN** `bind(resourceSet)` is called, then `resolve("com.example.Person")`
- **THEN** the EClass is returned from the internal cache

#### Scenario: Find by regex pattern
- **GIVEN** a bound FqnResolver with cached FQNs
- **WHEN** `findByPattern("com\\.example\\..*")` is called
- **THEN** a stream of matching FQN strings is returned

#### Scenario: Resolve by XMI ID
- **GIVEN** a bound ResourceSet with XMI fragment IDs
- **WHEN** `resolveByXmiId(xmiId)` is called
- **THEN** the corresponding `EObject` is returned

### Requirement: ResourceSet management

`AsmModelResourceSupport` SHALL manage EMF ResourceSet creation, XMI resource factory registration, URI handling, and provide typed stream accessors for all Ecore metaclasses.

#### Scenario: Create ASM ResourceSet
- **GIVEN** no pre-existing ResourceSet
- **WHEN** `AsmModelResourceSupport.createAsmResourceSet()` is called
- **THEN** a ResourceSet is returned with Ecore metamodel registered and XMI resource factory configured

#### Scenario: Stream typed elements
- **GIVEN** a loaded AsmModelResourceSupport
- **WHEN** `getStreamOfEcoreEClass()` is called
- **THEN** a `Stream<EClass>` of all EClass elements in the resource is returned

### Requirement: Code generation via MWE2 workflow

The MWE2 workflow (`src/workflow/generateModel.mwe2`) SHALL generate builder classes, helper classes, and runtime model wrapper into `src-gen/` from the Ecore model definition.

#### Scenario: Generate builders
- **WHEN** `./mvnw generate-sources -pl model` is executed
- **THEN** fluent builder classes (e.g., `EPackageBuilder`, `EClassBuilder`, `EAttributeBuilder`) are generated in `src-gen/`

#### Scenario: Clean and regenerate
- **WHEN** the MWE2 workflow runs
- **THEN** the `src-gen/` directory is cleaned before generation to remove stale artifacts
