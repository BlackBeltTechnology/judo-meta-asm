# Java Validation Framework

Native Java-based validation framework for ASM metamodel, running in parallel with EVL validation.

## ADDED Requirements

### Requirement: Java Validation Entry Point

The system SHALL provide an `AsmValidator` class as the main entry point for Java-based validation.

#### Scenario: Validate ASM model with no expected errors
- **WHEN** `AsmValidator.validateAsm(log, asmModel)` is called with a valid model
- **THEN** validation completes without throwing exceptions
- **AND** no error messages are logged

#### Scenario: Validate ASM model with expected errors
- **WHEN** `AsmValidator.validateAsm(log, asmModel, expectedErrors, expectedWarnings)` is called
- **THEN** validation compares actual constraint failures against expected lists
- **AND** throws exception if unexpected errors occur or expected errors are missing

#### Scenario: Parallel validation execution
- **WHEN** `AsmValidator.validateAsm(log, asmModel, expectedErrors, expectedWarnings, parallel=true)` is called
- **AND** the model contains 5000+ elements
- **THEN** validation executes rules in parallel using work-stealing thread pool

### Requirement: Validation Registry

The system SHALL provide a ValidationRegistry that discovers and registers validator classes.

#### Scenario: Register validator class
- **WHEN** a class annotated with `@ValidationContext(EClass.class)` is registered
- **THEN** the registry stores all `@Constraint` and `@Critique` methods
- **AND** validators are retrievable by EClass type

#### Scenario: Type hierarchy support
- **WHEN** looking up validators for an EClass
- **THEN** validators for the exact type, supertypes, and interfaces are returned

### Requirement: Annotation-Based Validation Rules

The system SHALL support defining validation rules using annotations.

#### Scenario: Define constraint rule
- **WHEN** a method is annotated with `@Constraint(name="RuleName", message="Error message")`
- **AND** returns `ValidationRule` functional interface
- **THEN** the rule is registered as an error-level constraint

#### Scenario: Define critique rule
- **WHEN** a method is annotated with `@Critique(name="RuleName", message="Warning message")`
- **AND** returns `ValidationRule` functional interface
- **THEN** the rule is registered as a warning-level critique

#### Scenario: Guard condition
- **WHEN** a rule method is annotated with `@Guard(method="guardMethodName")`
- **THEN** the guard method is evaluated before the rule
- **AND** the rule only executes if the guard returns true

#### Scenario: Satisfies dependency
- **WHEN** a rule method is annotated with `@Satisfies({"Dependency1", "Dependency2"})`
- **THEN** the rule only executes if all dependency constraints pass
- **AND** dependency results are cached to prevent re-evaluation

### Requirement: Extension Methods

The system SHALL support extension methods for helper operations in validation rules.

#### Scenario: Register extension method class
- **WHEN** a class is annotated with `@ExtensionMethod(EClass.class)`
- **THEN** its methods are available via `ctx.call(element, "methodName", args...)`

#### Scenario: Cached extension method
- **WHEN** an extension method is annotated with `@Cached`
- **THEN** results are cached by element and arguments
- **AND** subsequent calls return cached result without re-execution

### Requirement: Parameterized Validation Tests

The system SHALL support running identical test cases against both EVL and Java validators.

#### Scenario: Test runs with both validators
- **WHEN** a test method is annotated with `@ParameterizedTest` and `@EnumSource(ValidatorType.class)`
- **THEN** the test executes twice: once with EVL, once with Java validator

#### Scenario: Test naming includes validator type
- **WHEN** test name pattern includes `{0}` placeholder
- **THEN** test results show `[EVL]` or `[JAVA]` suffix

#### Scenario: Abstract base class provides dual validation
- **WHEN** test class extends `AbstractAsmValidationTest`
- **THEN** `runValidation(expectedErrors, expectedWarnings)` dispatches to correct validator
- **AND** both validators produce identical results for same test case

### Requirement: Validation Result Model

The system SHALL use immutable ValidationResult objects for validation outcomes.

#### Scenario: Passing result
- **WHEN** a validation rule passes
- **THEN** `ValidationResult.pass()` is returned
- **AND** `result.isPassed()` returns true

#### Scenario: Failing result
- **WHEN** a validation rule fails
- **THEN** `ValidationResult.fail(name, message, severity, element)` is returned
- **AND** result contains constraint name, message, severity, and context element

#### Scenario: Warning result
- **WHEN** a critique rule fails
- **THEN** `ValidationResult.warn(name, message, element)` is returned
- **AND** `result.getSeverity()` returns `Severity.WARNING`
