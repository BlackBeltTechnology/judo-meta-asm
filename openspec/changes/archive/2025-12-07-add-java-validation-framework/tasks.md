# Tasks: Add Java Validation Framework for ASM

## 1. Add Dependencies

- [x] 1.1 Copy validation core classes from ESM (embedded, not external dependency)
- [x] 1.2 Copy validation annotation classes from ESM

## 2. Create Validation Infrastructure

- [x] 2.1 Create `AsmValidator.java` - Main entry point class that:
  - Creates ValidationRegistry and ExtensionMethodRegistry
  - Registers validator classes (initially empty, framework only)
  - Creates ValidationContext with AsmUtils
  - Executes validation and reports results
  - Mirrors the pattern from ESM's `EsmValidator.java`

- [x] 2.2 Create `validation/` package structure:
  - `hu.blackbelt.judo.meta.asm.validation.annotation/` - Validation annotations
  - `hu.blackbelt.judo.meta.asm.validation.core/` - Core framework classes
  - `hu.blackbelt.judo.meta.asm.validation.extensions/` - Extension methods (empty, for future use)

- [x] 2.3 AsmUtilsExtensions.java - Skipped (not needed until validation rules are added)

## 3. Create Test Infrastructure

- [x] 3.1 Create `ValidatorType.java` enum with `EVL` and `JAVA` values

- [x] 3.2 Create `AbstractAsmValidationTest.java` base class that:
  - Initializes AsmModel in `initModel()`
  - Provides `runValidation(expectedErrors, expectedWarnings)` method
  - Dispatches to EVL or Java validator based on `validatorType` field
  - Handles EVL exception parsing to extract constraint names
  - Follows ESM's `AbstractEsmValidationTest` pattern

- [x] 3.3 Update `AsmValidationTest.java`:
  - Extend `AbstractAsmValidationTest`
  - Convert `@Test` methods to `@ParameterizedTest` with `@EnumSource(ValidatorType.class)`
  - Add `validatorType` parameter to test methods
  - Update test name pattern to include validator type: `{0}`

## 4. Add Validation Framework Tests

- [x] 4.1 ValidatorEngineTest.java - Skipped (core framework classes are copied from tested ESM implementation)

- [x] 4.2 Test validators as inner classes - Skipped (no validation rules to test yet)

## 5. Verification

- [x] 5.1 Run `mvn clean install` to verify build succeeds
- [x] 5.2 Verify all tests pass with both EVL and JAVA validator types
- [x] 5.3 Verify no regressions in existing functionality

## Implementation Notes

- The validation framework core classes are embedded directly in ASM (copied from ESM), not as an external dependency
- The current `asm.evl` is empty, so no validation rules exist to convert
- The Java validation framework provides infrastructure for future rules
- Tests run with both EVL and JAVA validators (parameterized tests)
- All 34 tests pass successfully

## Files Created

### model/src/main/java/hu/blackbelt/judo/meta/asm/validation/
- `AsmValidator.java` - Entry point for Java validation
- `annotation/` - 9 annotation classes (Cached, Constraint, Critique, ExtensionMethod, Guard, PostValidation, PreValidation, Satisfies, ValidationContext)
- `core/` - 13 core framework classes (ValidationResult, ValidationRule, ValidationRegistry, ValidationExecutor, ValidationContext, ValidatorDescriptor, etc.)

### model-test/src/test/java/hu/blackbelt/judo/meta/asm/
- `ValidatorType.java` - Enum for EVL/JAVA selection
- `AbstractAsmValidationTest.java` - Base test class for dual validation
- `runtime/AsmValidationTest.java` - Updated to parameterized tests
