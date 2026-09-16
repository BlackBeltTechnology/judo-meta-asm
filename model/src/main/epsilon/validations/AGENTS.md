# `model/src/main/epsilon/validations` — EVL constraint scripts run against the ASM model

| File | Purpose |
|---|---|
| `asm-plugin-validation.evl` | Eclipse-plugin entry point for EVL validation. Imports `asm.evl` and adds a `pre` block that instantiates `Native("hu.blackbelt.judo.meta.asm.runtime.AsmUtils")(ASM.resource.resourceSet, false)` into variable `asmUtils`. Requires a loaded model named `ASM`; standalone runs bind `asmUtils` from Java via `injectContexts` instead, so constraints must never assume this file ran. |
| `asm.evl` | Constraint script resolved by `AsmEpsilonValidator.calculateAsmValidationScriptURI()` and imported by `asm-plugin-validation.evl`. Currently empty — declares zero contexts and zero constraints, so every model passes EVL. `AsmValidationTest` asserts exactly that empty-result parity with `AsmValidator`; adding a constraint here requires a matching Java rule or parity tests break. |
