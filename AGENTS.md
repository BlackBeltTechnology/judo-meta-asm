# Judo ASM (Architecture Specific Model) Meta - Project Documentation

## Project Overview

**Repository:** BlackBeltTechnology/judo-meta-asm  
**License:** Eclipse Public License 2.0 (EPL-2.0)  
**Java Version:** 21  
**Build System:** Maven 3.9.4+ with Tycho (Eclipse build tooling)

This is an Eclipse/Tycho-based metamodel project that:
1. **Wraps** the EMF/Ecore metamodel for programmatic access
2. **Provides** runtime utilities (`AsmUtils`) for navigating Ecore structures
3. **Implements** validation via Epsilon Validation Language (EVL)
4. **Supports** OSGi deployment and Karaf container integration
5. **Distributes** via both Maven Central and Eclipse P2 repositories

## Directory Structure

```
judo-meta-asm/
├── model/                          # Core ASM runtime (wraps Ecore)
├── model-test/                     # Unit tests for AsmUtils and validation
├── osgi/                           # OSGi bundle repackaging
├── osgi-itest/                     # OSGi integration tests (Pax Exam)
├── feature/                        # Eclipse feature packaging
├── site/                           # Eclipse P2 update site
└── openspec/                       # OpenSpec change management
```

## Core Modules

### Model Layer

| Module | Type | Purpose |
|--------|------|---------|
| `model/` | eclipse-plugin | Core ASM runtime wrapping Ecore. Provides `AsmUtils`, `AsmModel`, and validation infrastructure. |
| `model-test/` | test | Unit tests for AsmUtils, builders, and validation using JUnit 5 |

### Runtime/OSGi Layer

| Module | Type | Purpose |
|--------|------|---------|
| `osgi/` | bundle | Repackages model for OSGi environments using Apache Felix Bundle Plugin |
| `osgi-itest/` | test | Pax Exam integration tests for Karaf container (4.4.7) |

### Distribution Layer

| Module | Type | Purpose |
|--------|------|---------|
| `feature/` | eclipse-feature | Bundles model and plugins for Eclipse |
| `site/` | eclipse-repository | P2 update site for Eclipse distribution |

## ASM Metamodel Structure

ASM wraps the **Ecore metamodel** - the Eclipse Modeling Framework's core metamodel:

| Ecore Element | Purpose |
|---------------|---------|
| `EPackage` | Package containers |
| `EClass` | Class definitions |
| `EAttribute` | Data attributes |
| `EReference` | Inter-object references |
| `EDataType` | Primitive and custom types |
| `EEnum` | Enumeration types |
| `EOperation` | Operations/methods |
| `EStructuralFeature` | Attributes and references |
| `EAnnotation` | Metadata annotations |

**Validation Rules:** 
- **EVL (Epsilon):** Located in `model/src/main/epsilon/validations/` using Epsilon Validation Language
- Entry point: `asm-plugin-validation.evl`
- Main rules: `asm.evl` (currently minimal)

## Technology Stack

### Core Technologies
- **Eclipse Modeling Framework (EMF)** 2.38.0+ - Metamodel foundation
- **Ecore** - Model definition language (the metamodel ASM wraps)
- **MWE2** (Model Workflow Engine) 2.13.0 - Code generation workflows
- **Epsilon** 2.8.0 - Model validation
- **Tycho** 4.0.13 - Eclipse plugin build

### Runtime
- **Apache Karaf** 4.4.7 - OSGi container
- **Apache Felix** 6.0.0 - OSGi bundle plugin
- **Pax Exam** 4.13.5 - OSGi testing

### Build & Quality
- **Maven** 3.9.4+ with wrapper
- **Lombok** 1.18.34 - Annotation processing

## Build Commands

```bash
# Standard build
mvn clean install
# or with wrapper
./mvnw clean install
```

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Parent POM with module definitions and plugin management |
| `model/model/Ecore.ecore` | Standard EMF Ecore metamodel |
| `model/model/Ecore.genmodel` | EMF code generation model |

## Key Runtime Classes

| Class | Purpose |
|-------|---------|
| `AsmModel` | Wrapper around EMF ResourceSet with builder pattern |
| `AsmUtils` | 60+ helper methods for navigating Ecore (FQName, type checking, queries) |
| `AsmUtilsCache` | Caching layer for AsmUtils operations |
| `AsmEpsilonValidator` | Executes EVL validation scripts |

## Development Environment

**Required:**
- Java 21 JDK
- Maven 3.9.4+
- Eclipse IDE with:
  - m2e (Maven integration)
  - Epsilon plugin
  - Modeling tools

## Git Workflow

- **Main Branch:** `develop`
- **Versioning:** SNAPSHOT-based development
- Feature branches for significant changes
- OpenSpec proposals for architectural changes

## Important Notes

1. **Understand EMF/Ecore patterns** before modifying model code
2. **Respect Tycho build constraints** when modifying Eclipse plugins
3. **Validation rules** use EVL (Epsilon Validation Language)
4. **Use OpenSpec for significant changes** - See `openspec/AGENTS.md` for proposal workflow

## Related Documentation

- `README.adoc` - Detailed project documentation
- `openspec/AGENTS.md` - OpenSpec workflow for spec-driven development
- `openspec/project.md` - Project conventions for OpenSpec
- `docs/validation/` - Validation documentation
- `docs/epsilon/` - Epsilon language reference
