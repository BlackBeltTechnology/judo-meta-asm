# judo-meta-asm

Supplies the JUDO platform's ASM (Abstract Syntax Model) layer. The metamodel is plain
Ecore — `model/model/Ecore.ecore` is the EMF Ecore metamodel itself (`nsURI`
`http://www.eclipse.org/emf/2002/Ecore`), and no EMF implementation classes are generated
from it — so everything JUDO-specific lives in the runtime around it: `AsmModel` owns the
load/save/validate lifecycle, `AsmUtils` + `AsmUtilsCache` resolve fully-qualified names and
read the extended-metadata `EAnnotation`s that carry all ASM semantics, `AsmEpsilonValidator`
drives EVL constraint scripts, and generated fluent builders let callers construct models in
code. The same runtime ships three ways: as an Eclipse plugin/feature/P2 site (Tycho), as a
standalone OSGi bundle that publishes `AsmModel` services out of model-carrying bundles, and
as a `judo-cli-api` `ModelValidator` / `FqnResolver` provider for the CLI.

**Repository:** BlackBeltTechnology/judo-meta-asm
**License:** Eclipse Public License 2.0 (EPL-2.0)
**Java Version:** 21
**Build System:** Maven 3.9.4+ with Tycho 4.0.13 (Eclipse plugin builds) and Maven Wrapper (`./mvnw`)
**Version:** `1.1.4-SNAPSHOT`, CI-friendly via `${revision}` + flatten-maven-plugin

## Reactor map

Root `pom.xml` has `pom` packaging and declares its six modules inside the `modules` profile,
which is active unless `-DskipModules=true` is passed. `targetdefinition/` is NOT a reactor
module — it is the Tycho target platform working directory.

<modules>

| Module | Packaging | What it is for |
|---|---|---|
| `model` | `eclipse-plugin` (`hu.blackbelt.judo.meta.asm.model`) | The only code-producing module. Carries `Ecore.ecore` (the metamodel the whole platform types against), the hand-written runtime (`runtime/`, `support/`), the CLI SPI implementations (`cli/`), the Java validator (`validation/`), the EMF resource-factory overrides (`org/eclipse/emf/ecore/util/`), the EVL scripts, and the MWE2 workflow that regenerates `src-gen/`. Tycho cannot compile Lombok, so sources are delomboked before the plugin is built; `MANIFEST.MF` (bundle `hu.blackbelt.judo.meta.asm.model`, activator `hu.blackbelt.judo.eclipse.asm.Activator`) is the authority on what this plugin exports, not the POM. |
| `model-test` | `jar` (test only) | Executable specification of that runtime — FQN resolution, builder API, EAnnotation handling, inheritance resolution, and EVL-vs-Java validation parity. Held out of `model` so the Tycho-built plugin ships no test code; it depends on `model` as an ordinary Maven artifact, which is why it also runs under `-Dtycho.mode=maven`. |
| `osgi` | `bundle` (Felix maven-bundle-plugin 5.1.8) | Republishes the `model` packages as a plain OSGi bundle for non-Eclipse containers and adds `AsmModelBundleTracker`. Its `Include-Resource` copies `../model/model` to `meta/asm` and `../model/src/main/epsilon/validations` to `validations` — that copy is what makes `AsmEpsilonValidator.calculateAsmValidationScriptURI()` resolve under a `jar:bundle:` URI. `hu.blackbelt.judo.cli.api` is deliberately excluded from `Import-Package`, so the CLI SPI classes are dead weight inside OSGi rather than an unresolvable wire. |
| `osgi-itest` | `jar` (test only) | Proves the `osgi` bundle actually resolves and registers `AsmModel` services inside a real container (Pax Exam 4.13.5 driving Apache Karaf 4.4.7). Catches wiring faults — a missing `Import-Package`, a resource that did not get embedded — that no unit test can see. Needs a provisionable Maven repository, so it is the module that fails first offline. |
| `feature` | `eclipse-feature` (packaging-only) | Eclipse feature `hu.blackbelt.judo.meta.asm.feature` — the installable grouping that lets Eclipse users pull the plugin in via "Install New Software". Holds `feature.xml`, `p2.inf` and the EPL-2.0 text only; no sources, so no `AGENTS.md`. |
| `site` | `eclipse-repository` (packaging-only) | Builds the P2 update site from `category.xml`, exposing categories `asm` (the feature) and `asm_source` (`…feature.source`, produced by tycho-source-plugin). Consumed by Eclipse, never by Maven callers; no sources, so no `AGENTS.md`. |

</modules>

## Directory Structure

```
judo-meta-asm/
├── model/                  # Core Eclipse plugin: Ecore metamodel, hand-written Java, generated code, EVL validation
│   ├── model/              # Ecore model definitions (Ecore.ecore, Ecore.genmodel, Ecore.aird)
│   ├── src/main/java/      # Hand-written Java sources (runtime, support, CLI SPI, validation)
│   │   ├── hu/blackbelt/judo/meta/asm/runtime/    # AsmModel, AsmUtils, AsmEpsilonValidator, AsmUtilsCache
│   │   ├── hu/blackbelt/judo/meta/asm/support/    # AsmModelResourceSupport
│   │   ├── hu/blackbelt/judo/meta/asm/cli/        # AsmValidatorImpl, AsmFqnResolverImpl
│   │   ├── hu/blackbelt/judo/meta/asm/validation/ # AsmValidator, AsmModelProvider
│   │   ├── hu/blackbelt/judo/eclipse/asm/         # Activator (Eclipse plugin lifecycle)
│   │   └── org/eclipse/emf/ecore/util/            # Ecore resource factory / resource overrides
│   ├── src/main/epsilon/   # Epsilon EVL validation rules
│   ├── src/workflow/       # MWE2 code generation workflow
│   ├── src-gen/            # Generated builders, helpers, runtime model (DO NOT EDIT)
│   └── META-INF/           # Eclipse plugin manifest
├── model-test/             # JUnit 5 unit tests for model utilities and validation
├── osgi/                   # OSGi bundle wrapper (Maven Bundle Plugin)
├── osgi-itest/             # OSGi integration tests (Karaf + Pax Exam)
├── feature/                # Eclipse feature definition
├── site/                   # Eclipse P2 update site
├── targetdefinition/       # Tycho target platform working directory (not a reactor module)
├── .github/workflows/      # GitHub Actions CI/CD pipeline
├── docs/                   # Validation documentation
├── openspec/               # OpenSpec capability specifications
├── logback-test.xml        # Test logging configuration
└── pom.xml                 # Parent POM
```

<!-- dox-doctrine -->
## Documentation Update Protocol (WRITE discipline)

Per-directory `AGENTS.md` files form a tree. Each directory `AGENTS.md` is the per-file
record for the files in that directory. This module-root `AGENTS.md` holds doctrine, the
module purpose, the reactor map, and build/operational instruction only — never a per-file
index.

**Keep the root lean.** This file loads into every agent turn — every byte costs tokens on
every turn. A verbose root buries the rules the model must follow (signal dilution) and
measurably degrades adherence. Default assumption: your update does NOT belong here — route
it by the table below.

**Route every doc update by kind:**

| Kind of update | Goes in |
|---|---|
| New file in a directory, or its per-file detail / change history | Nearest directory `AGENTS.md`. Add a `` | `<basename>` | <purpose> | `` row, path-alphabetical. |
| Data flow, protocol, architecture rationale | `docs/<topic>.md` |
| End-user / developer setup | `README.md` / `CONTRIBUTING.md` |
| Cross-cutting rule every agent needs every turn (rare) | this file |

**Row quality bar.** A row must not be reconstructible from the file's basename alone. Open
the file, then write purpose + named exports + the contract a caller can violate. Caveman
style: present tense, subject-verb-object, articles dropped, identifiers verbatim.

**Read before editing (chain walk).** Before editing a file, read the nearest `AGENTS.md`
chain root→leaf so you know the file's recorded purpose, contracts, and change history. Do
not edit blind.

**Update after editing (closeout pass).** After changing a file, update its row in the
nearest directory `AGENTS.md`: find the row, update its purpose in place; if absent, add it
in path-alphabetical order. New source-bearing directory → scaffold its `AGENTS.md`. One row
per file. Packaging-only maven directories appear in the reactor map above and get no file.

**Size rule — split an over-large directory `AGENTS.md` file-based.** pi auto-injects a
directory `AGENTS.md` on every turn when cwd sits at/below it, so an over-large directory
`AGENTS.md` is not supported. A row exceeding ~400 characters of detail promotes to a
per-file `<File>.AGENTS.md` sidecar beside it; the directory `AGENTS.md` keeps a one-line
summary plus a `→ see \`<File>.AGENTS.md\`` pointer. The sidecar is pull-only (pi never
auto-injects it) yet stays search-indexed. Do not inflate a row to justify a sidecar.

## Finding docs (READ discipline)

`kb_*` tools are faster and cheaper than raw search — they return a one-line purpose + key
exports per file, not raw bytes. **This fires on the ACTION, not the intent** — before you
`grep`/`rg` for a symbol, `cat`/read a file to learn what it does, or chase an import, the kb
call goes first. It fires **even mid-task when you already know the file**.

| You're about to… | Do this FIRST instead |
|---|---|
| `grep -rn "SymbolName" model/src` — find where a fn / type / const lives | `kb_search --doc-type agents "SymbolName"` |
| `grep -rn "feature\|topic" model/src` — how does X work | `kb_search "feature topic"` |
| `cat` / read a file just to learn its purpose before editing | `kb agents <path>` |
| chase imports / callers across files | `kb_neighbors <path\|heading>` |
| read one doc section in full | `kb_get <path> <section>` |

**Fall-through:** if the kb call returns nothing relevant, `rg` / source read is allowed —
then add the missing directory `AGENTS.md` row per the WRITE discipline above.

## Code Instructions

1. First think through the problem, read the codebase for relevant files.
2. Before you make any major changes, check in with me and I will verify the plan.
3. Please every step of the way just give me a high level explanation of what changes you made.
4. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
5. Maintain a documentation file that describes how the architecture of the app works inside and out.
6. Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
7. For implementation use TDD (Test-Driven Development): write or update tests first to define the expected behaviour, verify they fail, then write the minimal implementation to make them pass.
8. Use DRY (Don't Repeat Yourself): extract reusable logic into separate classes, utilities, or components. If the same pattern appears in multiple places, refactor it into a shared helper.

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

## Build Inputs Outside Any Source Directory

These files govern the build but live in directories that carry no `AGENTS.md`; their
behaviour is recorded here.

- **`pom.xml`** (root) — parent POM. Pins every dependency version, the `${revision}` version
  scheme, Tycho/Lombok/JaCoCo/Sonar plugin management, and all deploy profiles. The reactor
  module list lives inside the `modules` profile, not at top level, so `-DskipModules=true`
  builds the parent alone.
- **`model/model/Ecore.ecore`** — the metamodel every generator and every JUDO consumer types
  against. It is the EMF Ecore metamodel verbatim (`nsURI`
  `http://www.eclipse.org/emf/2002/Ecore`), which is why `AsmModelResourceSupport` registers
  only `EcorePackage.eINSTANCE` and no ASM-specific package. `Ecore.genmodel` and `Ecore.aird`
  accompany it for EMF/Sirius tooling; the standard `EcoreGenerator` step is disabled in the
  MWE2 workflow, so editing the genmodel alone changes nothing.
- **`model/META-INF/MANIFEST.MF`** — Eclipse bundle descriptor for
  `hu.blackbelt.judo.meta.asm.model`, `singleton:=true`, version `1.1.4.qualifier`, execution
  environment `JavaSE-21`, activator `hu.blackbelt.judo.eclipse.asm.Activator`. Exports
  `…asm.runtime`, `…asm.support`, `org.eclipse.emf.ecore.runtime/.support/.util/.util.builder`
  — a new public package is invisible to Eclipse consumers until it is added here.
  `Require-Bundle` re-exports `org.eclipse.emf.ecore` and `…ecore.xmi`, so dependents inherit
  EMF transitively. `Bundle-ActivationPolicy: lazy`.
- **`logback-test.xml`** (root) — test-only logging: single `CONSOLE` appender at `info`, plus
  a `LevelChangePropagator` with `resetJUL` so `java.util.logging` output from EMF/Epsilon is
  routed into SLF4J instead of bypassing it. Raising a test's log expectations means changing
  this file, not the test.
- **`.github/workflows/build.yml`** — CI pipeline: version calculation, build, deploy,
  release. Sibling workflows cover version bumps, tagged releases, PR labelling, and draft
  cleanup.

## Technology Stack

### Core Technologies
- **Eclipse EMF Ecore** — metamodel foundation
- **Tycho 4.0.13** — Eclipse plugin/feature/site build integration with Maven
- **Epsilon Runtime** — EVL (Epsilon Validation Language) validation engine
- **Lombok 1.18.34** — boilerplate reduction (delomboked for Eclipse/Tycho compatibility)
- **Xtext 2.39.0 / MWE2** — code generation workflow for builders and helpers
- **judo-cli-api** — CLI integration SPI (ModelValidator, FqnResolver)
- **Guava** — `LoadingCache` backing `AsmUtilsCache`

### Build & Quality
- **Maven 3.9.4+** with Maven Wrapper (`./mvnw`)
- **JUnit 5.9.1** with **Hamcrest 2.2** matchers for unit testing
- **Pax Exam 4.13.5** with **Apache Karaf 4.4.7** for OSGi integration testing
- **JaCoCo 0.8.12** — code coverage
- **SonarQube** — static analysis (via sonar-maven-plugin 3.9.1)
- **flatten-maven-plugin** — CI-friendly version resolution (`${revision}`)

## Code Generation Pipeline

The MWE2 workflow (`model/src/workflow/generateModel.mwe2`) generates code into `model/src-gen/`:

1. **DirectoryCleaner** — wipes `src-gen/` to remove stale artifacts
2. **HelperGeneratorWorkflow** — generates utility methods for Ecore model elements
3. **BuilderGeneratorWorkflow** — generates fluent builder API classes (e.g., `EPackageBuilder`, `EClassBuilder`, `EAttributeBuilder`)
4. **RuntimeModelGeneratorWorkflow** — generates JUDO-specific runtime model wrapper with name/version resolution

Input: `model/model/Ecore.ecore`. Triggered via `exec-maven-plugin` in the `generate-sources` phase.

**Never hand-edit files in `model/src-gen/`** — they are regenerated.

## Extended Metadata Pattern

ASM-specific metadata is stored in Ecore's `EAnnotation` mechanism under the namespace
`http://blackbelt.hu/judo/meta/ExtendedMetadata`. Key operations in `AsmUtils`:
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
8. `AsmUtils` (~1,765 lines) is the largest and most important utility — FQN resolution, type classification, annotation access, and stream-based model queries all funnel through it

## Related Documentation

- [README](README.md) — project introduction, module overview, architecture diagrams
- [CONTRIBUTING](CONTRIBUTING.md) — development setup, code structure, troubleshooting, submission guidelines
- [CI Flow](.github/CIFLOW.md) — branch strategy, version numbers, GitHub Actions workflows
