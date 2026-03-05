# judo-meta-asm

[![Build](https://github.com/BlackBeltTechnology/judo-meta-asm/actions/workflows/build.yml/badge.svg?branch=develop)](https://github.com/BlackBeltTechnology/judo-meta-asm/actions/workflows/build.yml)

## Introduction

**judo-meta-asm** provides the Abstract Syntax Model (ASM) layer for the [JUDO platform](https://github.com/BlackBeltTechnology/judo-community). It wraps the standard [Eclipse EMF Ecore](https://www.eclipse.org/modeling/emf/) metamodel with:

- **Fluent builder API** — generated classes like `EClassBuilder` and `EAttributeBuilder` for constructing Ecore models programmatically
- **Runtime utilities** — `AsmModel` for model lifecycle (load/save/validate) and `AsmUtils` for FQN resolution, type classification, and annotation access
- **Epsilon-based validation** — EVL (Epsilon Validation Language) rules for model constraint checking
- **CLI integration** — SPI implementations for `ModelValidator` and `FqnResolver` from `judo-cli-api`
- **Multi-platform packaging** — available as an Eclipse plugin (with P2 update site), a standalone OSGi bundle, and a plain Maven dependency

## Module Overview

The project consists of six modules organized into three layers: core model, OSGi packaging, and Eclipse distribution.

```mermaid
graph TD
    subgraph Core
        MODEL["model<br/><i>Eclipse plugin: metamodel,<br/>builders, runtime, validation</i>"]
        TEST["model-test<br/><i>JUnit 5 tests</i>"]
    end
    subgraph OSGi Packaging
        OSGI["osgi<br/><i>OSGi bundle wrapper</i>"]
        ITEST["osgi-itest<br/><i>Karaf integration tests</i>"]
    end
    subgraph Eclipse Distribution
        FEAT["feature<br/><i>Eclipse feature</i>"]
        SITE["site<br/><i>P2 update site</i>"]
    end

    TEST -.->|tests| MODEL
    OSGI -->|repackages| MODEL
    ITEST -.->|tests| OSGI
    FEAT -->|includes| MODEL
    SITE -->|hosts| FEAT
```

| Module | Packaging | Purpose |
|--------|-----------|---------|
| `model/` | `eclipse-plugin` | Core Ecore metamodel, hand-written runtime classes (`AsmModel`, `AsmUtils`, `AsmEpsilonValidator`), generated builders/helpers via MWE2, and EVL validation rules |
| `model-test/` | `jar` | JUnit 5 tests for FQN resolution, builder API, validation engine, annotations, and inheritance |
| `osgi/` | `bundle` | Repackages the model as a standalone OSGi bundle with `AsmModelBundleTracker` for dynamic model registration |
| `osgi-itest/` | `jar` | Integration tests that deploy the OSGi bundle in Apache Karaf via Pax Exam |
| `feature/` | `eclipse-feature` | Eclipse feature definition for plugin installation |
| `site/` | `eclipse-repository` | P2 update site for Eclipse "Install New Software" |

## Key Architecture

The core runtime revolves around four classes that manage model construction, introspection, resource handling, and validation.

```mermaid
classDiagram
    class AsmModel {
        +buildAsmModel() AsmModelBuilder
        +loadAsmModel(LoadArguments) AsmModel
        +saveAsmModel(SaveArguments) void
        +addContent(EObject) AsmModel
        +getResourceSet() ResourceSet
        +getResource() Resource
        +isValid() boolean
        +getDiagnostics() Set~Diagnostic~
    }

    class AsmUtils {
        +getPackageFQName(EPackage) String
        +getClassifierFQName(EClassifier) String
        +getAttributeFQName(EAttribute) String
        +resolve(String) Optional~EClassifier~
        +all(Class~T~) Stream~T~
        +getExtensionAnnotationValue(...) Optional~String~
        +isEntityType(EClass) boolean
    }

    class AsmModelResourceSupport {
        +createAsmResourceSet() ResourceSet
        +loadAsm(LoadArguments) AsmModelResourceSupport
        +saveAsm(SaveArguments) void
        +getStreamOfEcoreEClass() Stream~EClass~
        +registerAsmMetamodel(ResourceSet) void
    }

    class AsmEpsilonValidator {
        +validateAsm(Logger, AsmModel, URI) void
        +calculateAsmValidationScriptURI() URI
    }

    AsmModel --> AsmModelResourceSupport : delegates resource ops
    AsmModel --> AsmEpsilonValidator : validates via EVL
    AsmUtils --> AsmModel : introspects
```

## Dependency Graph

The project builds on Eclipse EMF, Epsilon, and several JUDO platform components.

```mermaid
graph LR
    subgraph External
        EMF["Eclipse EMF Ecore<br/><i>metamodel foundation</i>"]
        EPSILON["Epsilon Runtime<br/><i>EVL validation engine</i>"]
        TYCHO["Tycho 4.0.13<br/><i>Eclipse build integration</i>"]
        LOMBOK["Lombok 1.18.34<br/><i>boilerplate reduction</i>"]
        XTEXT["Xtext / MWE2<br/><i>code generation</i>"]
    end
    subgraph JUDO Platform
        CLI["judo-cli-api<br/><i>CLI SPI contracts</i>"]
        EPP["judo-epp-common<br/><i>Eclipse platform</i>"]
        OSGIUTIL["osgi-utils<br/><i>bundle tracking</i>"]
    end
    subgraph judo-meta-asm
        MODEL[model]
        OSGI_MOD[osgi]
    end

    MODEL --> EMF
    MODEL --> EPSILON
    MODEL --> LOMBOK
    MODEL --> XTEXT
    MODEL --> CLI
    OSGI_MOD --> OSGIUTIL
    OSGI_MOD --> MODEL
```

## Build Commands

The build requires **Java 21** and **Maven 3.9.4+**. Use the included Maven wrapper (`./mvnw`).

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

## Build Lifecycle

The Maven build uses Tycho for Eclipse plugin packaging and MWE2 for code generation.

```mermaid
flowchart LR
    GEN["generate-sources<br/><i>MWE2 workflow:<br/>builders, helpers</i>"]
    COMP["compile<br/><i>javac + Tycho<br/>compiler</i>"]
    TEST["test<br/><i>JUnit 5 +<br/>Pax Exam</i>"]
    PKG["package<br/><i>eclipse-plugin,<br/>bundle, feature,<br/>P2 site</i>"]
    INST["install<br/><i>local Maven repo</i>"]

    GEN --> COMP --> TEST --> PKG --> INST

    SIGN["sign-artifacts<br/><i>GPG signing</i>"]
    DEPLOY_JN["release-judong<br/><i>Nexus deploy</i>"]
    DEPLOY_MC["release-central<br/><i>Maven Central</i>"]

    PKG -->|profile| SIGN
    INST -->|profile| DEPLOY_JN
    INST -->|profile| DEPLOY_MC
```

## Context

This project is a building block of the [judo-community](https://github.com/BlackBeltTechnology/judo-community) aggregator project. In order to better understand how this module fits into our ecosystem, please check the corresponding documentation!

## Contributing to the project

Everyone is welcome to contribute to JUDO! As a starter, please read the corresponding [CONTRIBUTING](CONTRIBUTING.md) guide for details!

## License

This project is licensed under the [Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/).
