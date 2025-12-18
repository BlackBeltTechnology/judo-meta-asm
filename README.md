# judo-meta-asm

[![Build](https://github.com/BlackBeltTechnology/judo-meta-asm/actions/workflows/build.yml/badge.svg?branch=develop)](https://github.com/BlackBeltTechnology/judo-meta-asm/actions/workflows/build.yml)

## Introduction

This project contains the ASM meta model.

It acts as an eclipse plugin with features and sites, and can be used standalone and in standard OSGi (without eclipse).

It is a pure ECore meta model. Additionally, it provides builders for better usability.

## Validation

The ASM model supports dual validation approaches:

### EVL (Epsilon Validation Language)

Traditional script-based validation using the Epsilon platform. EVL scripts are stored in `.evl` files.

**Location**: `model/src/main/epsilon/validations/`

### Java Validation (Judo Zeta Framework)

Annotation-based Java validation providing:

* **Type Safety** - Compile-time type checking
* **IDE Support** - Full autocomplete, refactoring, and debugging
* **Performance** - No interpretation overhead, automatic parallelization
* **Testability** - Standard unit testing for validation rules

**Location**: `model/src/main/java/hu/blackbelt/judo/meta/asm/validation/`

For detailed documentation, see [Validation Framework Documentation](docs/validation/index.md)

## CLI Integration

This metamodel provides CLI support classes for use with [judo-model-cli](https://github.com/BlackBeltTechnology/judo-model-cli):

**CLI Classes**:
- `AsmFqnResolverImpl` - Resolves fully qualified names for ASM elements (hand-written)
- `AsmValidatorImpl` - Validates ASM models via CLI (hand-written)
- `AsmModelSchema` - GraphQL schema for querying ASM models (generated in `model/src-gen/`)

Additionally, Ecore CLI classes are provided since ASM wraps the Ecore metamodel:
- `EcoreFqnResolverImpl` - Resolves FQNs for base Ecore elements (hand-written)
- `EcoreValidatorImpl` - Validates Ecore structure (hand-written)
- `EcoreModelSchema` - GraphQL schema for Ecore queries (generated)

**FQN Format**: `package.ClassName` (e.g., `myapp.Customer`)

**Example CLI Queries**:
```bash
# Count classes in ASM model
judo-model-cli -m model.asm graphql '{ asm { count(type: "EClass") } }'

# List packages
judo-model-cli -m model.asm graphql '{ asm { list(type: "EPackage", limit: 10) { __fqn __type } } }'

# Query Ecore elements directly
judo-model-cli -m model.asm graphql '{ ecore { list(type: "EAttribute") { __fqn } } }'
```

## Context

This project is a building block of the [judo-community](https://github.com/BlackBeltTechnology/judo-community) aggregator
project. In order to better understand how this module fits into our ecosystem, please check the corresponding documentation!

## Contributing to the project

Everyone is welcome to contribute to JUDO! As a starter, please read the corresponding [CONTRIBUTING](CONTRIBUTING.md) guide for details!

## License

This project is licensed under the [Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/).
