# judo-meta-asm - Project Documentation

## Project Overview


**Repository:** BlackBeltTechnology/judo-meta-asm
**License:** Eclipse Public License 2.0 (EPL-2.0)
**Java Version:** 21
**Build System:** Maven 3.9.4+ with Tycho 4.0.13 (Eclipse plugin builds) and Maven Wrapper (`./mvnw`)

1. Wraps the standard Eclipse EMF Ecore metamodel (`http://www.eclipse.org/emf/2002/Ecore`) with builder utilities, validation, and OSGi/Eclipse integration
2. Provides the ASM (Abstract Syntax Model) layer for the JUDO platform — runtime model loading/saving, FQN (fully-qualified name) resolution, and Epsilon-based validation
3. Generates fluent builder API classes (e.g., `EClassBuilder`, `EAttributeBuilder`) via an MWE2 code generation workflow
4. Offers CLI SPI integration for model validation (`AsmValidatorImpl`) and FQN resolution (`AsmFqnResolverImpl`)
5. Packages as both an Eclipse plugin (with P2 update site) and a standalone OSGi bundle for use outside Eclipse

## Code Instructions

1. First think through the problem, read the codebase for relevant files.
2. Before you make any major changes, check in with me and I will verify the plan.
3. Please every step of the way just give me a high level explanation of what changes you made.
4. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
5. Maintain a documentation file that describes how the architecture of the app works inside and out.
6. Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
7. For implementation use TDD (Test-Driven Development): write or update tests first to define the expected behaviour, verify they fail, then write the minimal implementation to make them pass.
8. Use DRY (Don't Repeat Yourself): extract reusable logic into separate classes, utilities, or components. If the same pattern appears in multiple places, refactor it into a shared helper.

## Directory Structure

```
judo-meta-asm/
├── model/                  # Core Eclipse plugin: Ecore metamodel, hand-written Java, generated code, EVL validation
│   ├── model/              # Ecore model definitions (Ecore.ecore)
│   ├── src/main/java/      # Hand-written Java sources (runtime, support, CLI SPI)
│   │   ├── hu/blackbelt/judo/meta/asm/runtime/   # AsmModel, AsmUtils, AsmEpsilonValidator, AsmUtilsCache
│   │   ├── hu/blackbelt/judo/meta/asm/support/   # AsmModelResourceSupport
│   │   ├── hu/blackbelt/judo/meta/asm/cli/       # AsmValidatorImpl, AsmFqnResolverImpl
│   │   └── hu/blackbelt/judo/eclipse/asm/        # Activator (Eclipse plugin lifecycle)
│   ├── src/main/epsilon/   # Epsilon EVL validation rules
│   ├── src/workflow/        # MWE2 code generation workflow
│   ├── src-gen/            # Generated builders, helpers, runtime model (DO NOT EDIT)
│   └── META-INF/           # Eclipse plugin manifest
├── model-test/             # JUnit 5 unit tests for model utilities and validation
├── osgi/                   # OSGi bundle wrapper (Maven Bundle Plugin)
├── osgi-itest/             # OSGi integration tests (Karaf + Pax Exam)
├── feature/                # Eclipse feature definition
├── site/                   # Eclipse P2 update site
├── .github/workflows/      # GitHub Actions CI/CD pipeline
├── openspec/               # OpenSpec capability specifications
├── logback-test.xml        # Test logging configuration
└── pom.xml                 # Parent POM
```

## Core Modules

### Model Layer

| Module | Type | Purpose |
|--------|------|---------|
| `model/` | Eclipse Plugin (`eclipse-plugin`) | Core metamodel wrapping EMF Ecore with hand-written runtime classes (`AsmModel`, `AsmUtils`, `AsmEpsilonValidator`, `AsmUtilsCache`), generated builders/helpers via MWE2, Epsilon EVL validation rules, and CLI SPI implementations |
| `model-test/` | Test JAR | JUnit 5 tests for FQN resolution (`AsmUtilsTest`), builder API (`EmfBuilderTest`), validation engine (`AsmValidationTest`), EAnnotation handling (`AnnotationTest`), and inheritance (`InheritanceTest`). Base class: `ExecutionContextOnAsmTest` |

### OSGi/Eclipse Packaging

| Module | Type | Purpose |
|--------|------|---------|
| `osgi/` | OSGi Bundle (`bundle`) | Repackages model as standalone OSGi bundle using Apache Felix Maven Bundle Plugin v5.1.8; exports all runtime packages; tracks bundles via `AsmModelBundleTracker`; embeds Ecore model and validation files |
| `osgi-itest/` | Integration Tests (`jar`) | Tests OSGi bundle deployment in Apache Karaf 4.4.7 container using Pax Exam 4.13.5 |
| `feature/` | Eclipse Feature (`eclipse-feature`) | Eclipse feature definition for plugin installation |
| `site/` | Eclipse Repository (`eclipse-repository`) | P2 update site for Eclipse "Install New Software" |

## Key Runtime Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `AsmModel` | `runtime` | Builder-pattern model wrapper with load/save/validate lifecycle; `buildAsmModel()`, `loadAsmModel()`, `saveAsmModel()` |
| `AsmUtils` | `runtime` | Core utility (1,765 lines): FQN resolution, stream queries (`all(Class<T>)`), extended metadata access, type classification (`isEntityType`, `isNumeric`, etc.) |
| `AsmUtilsCache` | `runtime` | Performance caching layer for AsmUtils operations (ResourceSet-scoped) |
| `AsmEpsilonValidator` | `runtime` | Runs Epsilon EVL validation scripts; handles JAR/bundle/file URI resolution |
| `AsmModelResourceSupport` | `support` | Manages EMF ResourceSet, XMI serialization, diagnostics, typed stream accessors (`getStreamOfEcoreEClass()`, etc.) |
| `AsmValidatorImpl` | `cli` | Implements `ModelValidator` SPI from judo-cli-api; delegates to `AsmEpsilonValidator` |
| `AsmFqnResolverImpl` | `cli` | Implements `FqnResolver` SPI from judo-cli-api; thread-safe FQN→EObject cache via `ConcurrentHashMap`; supports pattern matching |
| `AsmModelBundleTracker` | `osgi` | OSGi DS component that tracks bundles with `Asm-Models` header; dynamically registers/unregisters `AsmModel` services |

## Technology Stack

### Core Technologies
- **Eclipse EMF Ecore** — metamodel foundation
- **Tycho 4.0.13** — Eclipse plugin/feature/site build integration with Maven
- **Epsilon Runtime** — EVL (Epsilon Validation Language) validation engine
- **Lombok 1.18.34** — boilerplate reduction (delomboked for Eclipse/Tycho compatibility)
- **Xtext 2.39.0 / MWE2** — code generation workflow for builders and helpers
- **judo-cli-api** — CLI integration SPI (ModelValidator, FqnResolver)

### Build & Quality
- **Maven 3.9.4+** with Maven Wrapper (`./mvnw`)
- **JUnit 5.9.1** with **Hamcrest 2.2** matchers for unit testing
- **Pax Exam 4.13.5** with **Apache Karaf 4.4.7** for OSGi integration testing
- **JaCoCo 0.8.12** — code coverage
- **SonarQube** — static analysis (via sonar-maven-plugin 3.9.1)
- **flatten-maven-plugin** — CI-friendly version resolution (`${revision}`)

## Build Commands

```bash
# Full build (all modules)
./mvnw clean install

# Run unit tests only
./mvnw clean test -pl model-test

# Run a single test class
./mvnw test -pl model-test -Dtest=AsmUtilsTest

# Run a single test method
./mvnw test -pl model-test -Dtest=AsmUtilsTest#testGetFQN

# Skip Tycho overhead for faster local builds
./mvnw clean install -Dtycho.mode=maven

# Regenerate code from Ecore model
./mvnw generate-sources -pl model
```

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `modules` | Activates all submodules (enabled by default; disable with `-DskipModules=true`) |
| `sign-artifacts` | GPG-signs built artifacts using simplify4u sign-maven-plugin |
| `release-dummy` | Deploys to local `/tmp/` directory for testing |
| `release-judong` | Deploys to JUDO Nexus repository (`nexus.judo.technology`) |
| `release-central` | Deploys to Maven Central via Sonatype OSSRH |
| `generate-github-asciidoc-diagrams` | Renders AsciiDoc diagrams to PNG using asciidoctor-maven-plugin |
| `update-source-code-license` | Updates EPL-2.0 license headers in source files |

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Parent POM with all dependency versions, plugin management, and profile definitions |
| `model/model/Ecore.ecore` | The Ecore metamodel definition that all code generation is based on |
| `model/src/workflow/generateModel.mwe2` | MWE2 workflow that generates builders, helpers, and runtime model into `src-gen/` |
| `model/META-INF/MANIFEST.MF` | Eclipse plugin manifest (bundle ID: `hu.blackbelt.judo.meta.asm.model`, exports, dependencies) |
| `model/src/main/epsilon/validations/asm.evl` | Core Epsilon validation rules for ASM models |
| `model/src/main/epsilon/validations/asm-plugin-validation.evl` | Plugin-specific validation (imports asm.evl, injects AsmUtils) |
| `logback-test.xml` | Logback configuration for test execution |
| `.github/workflows/build.yml` | CI/CD pipeline: version calculation, build, deploy, release |

## Code Generation Pipeline

The MWE2 workflow (`model/src/workflow/generateModel.mwe2`) generates code into `model/src-gen/`:

1. **DirectoryCleaner** — wipes `src-gen/` to remove stale artifacts
2. **HelperGeneratorWorkflow** — generates utility methods for Ecore model elements
3. **BuilderGeneratorWorkflow** — generates fluent builder API classes (e.g., `EPackageBuilder`, `EClassBuilder`, `EAttributeBuilder`)
4. **RuntimeModelGeneratorWorkflow** — generates JUDO-specific runtime model wrapper with name/version resolution

Input: `model/Ecore.ecore`. Triggered via `exec-maven-plugin` in `generate-sources` phase.

**Never hand-edit files in `model/src-gen/`** — they are regenerated.

## Extended Metadata Pattern

ASM-specific metadata is stored in Ecore's `EAnnotation` mechanism under the namespace `http://blackbelt.hu/judo/meta/ExtendedMetadata`. Key operations in `AsmUtils`:
- `getExtensionAnnotationValue()` — read annotation value
- `addExtensionAnnotation()` — create/update annotation
- `annotatedAsTrue()` / `annotatedAsFalse()` — boolean annotation checks
- `getExtensionAnnotationListByName()` — find all annotations by name

## Development Environment

**Required:**
- Java 21 JDK
- Maven 3.9.4+ (or use included `./mvnw` wrapper)

**Recommended:**
- Eclipse IDE with m2e, Epsilon, and Modeling Tools plugins (for Ecore editing and MWE2 workflow execution)
- VSCode or Zed with Java extensions (settings files included in `.vscode/` and `.zed/`)

## Git Workflow

- **Main Branch:** `develop`
- **Versioning:** `1.1.4-SNAPSHOT` (CI-friendly via `${revision}` property and flatten-maven-plugin)
- **Branch Naming:** GitFlow-based — `feature/JNG-xxx_description`, `bugfix/JNG-xxx_description`, `release/x.y.z`
- **Commit Rule:** Every commit must reference a JIRA ticket (`JNG-xxx`)
- **CI Versions:** Development builds use `major.minor.qualifier.timestamp_commitHash_branchName` format

## Important Notes

1. **Never hand-edit files in `model/src-gen/`** — they are regenerated from the Ecore model via the MWE2 workflow
2. Extended metadata is stored in EAnnotations under `http://blackbelt.hu/judo/meta/ExtendedMetadata`
3. Tycho does not support Lombok directly; all Eclipse plugin sources use delomboked code
4. Maven and Eclipse use different version conventions (`-SNAPSHOT` vs `.qualifier`); Tycho Versions Plugin handles the translation
5. The `model/` module uses `eclipse-plugin` packaging — standard Maven lifecycle phases may behave differently under Tycho
6. OSGi integration tests require a Karaf container and may need network access for provisioning
7. Test models are constructed programmatically using the generated builder API (see `EmfBuilderTest`)
8. The `AsmUtils` class (1,765 lines) is the largest and most important utility — it handles FQN resolution, type classification, annotation access, and stream-based model queries

## Related Documentation

- [README](README.md) — project introduction, module overview, architecture diagrams
- [CONTRIBUTING](CONTRIBUTING.md) — development setup, code structure, troubleshooting, submission guidelines
- [CI Flow](.github/CIFLOW.md) — branch strategy, version numbers, GitHub Actions workflows
