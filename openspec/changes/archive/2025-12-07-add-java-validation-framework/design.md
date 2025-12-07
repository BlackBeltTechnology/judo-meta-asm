# Design: Add Java Validation Framework for ASM

## Context

The ASM (Architecture Specific Model) module wraps the EMF/Ecore metamodel and provides validation via Epsilon Validation Language (EVL). The ESM module has successfully implemented a dual-validation approach using the Judo Zeta framework, enabling both EVL and native Java validation to run in parallel with identical results.

**Stakeholders:**
- Developers maintaining ASM validation rules
- Projects consuming ASM validation (downstream metamodels)
- Build/CI systems running validation

**Constraints:**
- Must not break existing EVL validation
- Must use the existing Judo Zeta framework (no reimplementation)
- Must follow ESM patterns for consistency

## Goals / Non-Goals

**Goals:**
- Provide Java-based validation infrastructure for ASM
- Enable parameterized tests that run both EVL and Java validators
- Establish patterns consistent with ESM implementation
- Support future addition of validation rules

**Non-Goals:**
- Converting existing EVL rules to Java (asm.evl is currently empty)
- Replacing EVL validation (both run in parallel)
- Adding new validation rules in this change

## Decisions

### Decision 1: Follow ESM Validation Pattern Exactly

**What:** Mirror the ESM validation framework structure in ASM.

**Why:** 
- Consistency across Judo metamodel projects
- Proven pattern already working in ESM
- Reduces learning curve for developers
- Enables code sharing/templating

**Alternatives considered:**
- Simplified structure without extension methods - Rejected because future rules may need them
- Different package structure - Rejected for consistency

### Decision 2: Use Judo Zeta Framework Directly

**What:** Depend on `judo-zeta` annotations and runtime directly.

**Why:**
- Framework already implemented and tested
- Provides all necessary infrastructure
- Avoids code duplication

**Alternatives considered:**
- Copy Zeta classes into ASM - Rejected, violates DRY
- Create ASM-specific annotations - Rejected, unnecessary complexity

### Decision 3: Empty Initial Validation Rules

**What:** Provide framework infrastructure without actual validation rules.

**Why:**
- Current `asm.evl` is empty (no rules defined)
- Java validation should match EVL behavior exactly
- Rules can be added incrementally as needed

**Alternatives considered:**
- Add sample validation rules - Rejected, would differ from EVL
- Wait until rules are needed - Rejected, infrastructure needed for test pattern

## Architecture

```
model/src/main/java/hu/blackbelt/judo/meta/asm/
├── runtime/
│   ├── AsmValidator.java          # Entry point (new)
│   ├── AsmEpsilonValidator.java   # Existing EVL validator
│   └── AsmUtils.java              # Existing utilities
└── validation/                     # New package
    ├── extensions/
    │   └── AsmUtilsExtensions.java # Extension methods
    └── rules/                      # Empty, for future rules

model-test/src/test/java/hu/blackbelt/judo/meta/asm/
├── ValidatorType.java              # EVL/JAVA enum
├── AbstractAsmValidationTest.java  # Base test class
└── runtime/
    ├── AsmValidationTest.java      # Updated, parameterized
    └── ValidatorEngineTest.java    # Framework tests
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Test execution time doubles (runs twice) | Acceptable overhead; parallel execution minimizes impact |
| Maintenance of two validators | Both share same test cases; rules added to Java only when needed |
| Dependency on Judo Zeta | Framework is internal, stable, and already used in ESM |

## Migration Plan

1. **Phase 1 (This Change):** Add framework infrastructure
   - No migration needed, additive change
   - Existing tests continue to work

2. **Phase 2 (Future):** Add validation rules as needed
   - Convert EVL rules to Java when modifying them
   - New rules written in Java first

**Rollback:** Remove added files and dependencies; EVL continues unchanged.

## Open Questions

None - pattern established in ESM provides clear guidance.
