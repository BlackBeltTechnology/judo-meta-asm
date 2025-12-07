# Change: Add Java Validation Framework for ASM

## Why

The ASM module currently uses Epsilon Validation Language (EVL) for model validation, similar to ESM. The ESM module has already migrated to a dual-validation approach using the Judo Zeta framework, which provides:
- Native Java validation with better IDE support (debugging, refactoring, code completion)
- Improved performance through parallel execution and caching
- Type-safe validation rules with compile-time checking
- Consistent validation behavior between EVL and Java implementations

To maintain consistency across the Judo metamodel projects and leverage the benefits of Java-based validation, ASM should adopt the same dual-validation pattern as ESM.

## What Changes

1. **Add Judo Zeta dependency** - Include `judo-zeta` validation framework in model module
2. **Create AsmValidator entry point** - Main class to orchestrate Java validation (mirrors `EsmValidator`)
3. **Create validation infrastructure** - Extension methods and utility classes for ASM-specific operations
4. **Add ValidatorType enum** - For parameterized test selection between EVL and Java validators
5. **Create AbstractAsmValidationTest** - Base test class supporting dual validation
6. **Update AsmValidationTest** - Convert to parameterized tests running both validators
7. **Add validation framework tests** - Unit tests for the validation engine components

**Note:** The current `asm.evl` file is minimal (empty). The Java validation will mirror this - initially providing the framework infrastructure without specific validation rules. Rules can be added later as needed.

## Impact

- **Affected specs:** New `java-validation` capability
- **Affected code:**
  - `model/pom.xml` - New dependencies
  - `model/src/main/java/hu/blackbelt/judo/meta/asm/runtime/` - New validation classes
  - `model-test/pom.xml` - Test dependencies
  - `model-test/src/test/java/hu/blackbelt/judo/meta/asm/` - New test infrastructure
- **Breaking changes:** None - EVL validation continues to work unchanged
- **Dependencies:** Requires `judo-zeta` framework (already implemented in runtime)
