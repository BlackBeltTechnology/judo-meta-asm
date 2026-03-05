# osgi Specification

## Purpose

The `osgi` module repackages the ASM model as a standalone OSGi bundle using the Apache Felix Maven Bundle Plugin. It exports all runtime packages and provides `AsmModelBundleTracker` for dynamic model registration/deregistration in OSGi service registries.

## Architecture

- **`AsmModelBundleTracker`** (`hu.blackbelt.judo.meta.asm.osgi`) — An OSGi Declarative Services component that tracks bundles with the `Asm-Models` header. When such a bundle is installed, it loads the referenced ASM model file and registers it as an `AsmModel` service. When the bundle is uninstalled, it unregisters the service.
- The bundle exports: `hu.blackbelt.judo.meta.asm.*`, `org.eclipse.emf.ecore.util.builder`, `org.eclipse.emf.ecore.runtime`, `org.eclipse.emf.ecore.support`
- Resources included: Ecore model file and Epsilon validation scripts

## Requirements

### Requirement: Bundle tracking for ASM models

`AsmModelBundleTracker` SHALL automatically detect bundles with the `Asm-Models` manifest header and register their models as OSGi services.

#### Scenario: Bundle with ASM model installed
- **GIVEN** a bundle with header `Asm-Models: name=myModel;file=model.xmi` is installed
- **WHEN** the bundle tracker processes the bundle
- **THEN** an `AsmModel` service is registered in the OSGi service registry with the model name as a property

#### Scenario: Bundle with ASM model uninstalled
- **GIVEN** a previously tracked bundle with a registered `AsmModel` service
- **WHEN** the bundle is uninstalled
- **THEN** the `AsmModel` service registration is unregistered and removed from internal maps

#### Scenario: Duplicate model name
- **GIVEN** a model with name `myModel` is already registered
- **WHEN** another bundle with the same model name is installed
- **THEN** the duplicate is rejected and an error is logged

### Requirement: Package exports

The OSGi bundle SHALL export all public packages from the model module so consumers can use `AsmModel`, `AsmUtils`, builder classes, and resource support in OSGi environments.

#### Scenario: Import AsmModel in consumer bundle
- **GIVEN** a consumer bundle that imports `hu.blackbelt.judo.meta.asm.runtime`
- **WHEN** the consumer bundle resolves its dependencies
- **THEN** `AsmModel`, `AsmUtils`, and `AsmEpsilonValidator` are available on the classpath
