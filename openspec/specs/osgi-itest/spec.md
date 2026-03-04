# osgi-itest Specification

## Purpose

The `osgi-itest` module provides OSGi integration tests that verify the ASM bundle deploys and functions correctly inside an Apache Karaf container using Pax Exam.

## Architecture

Tests use Pax Exam to provision a Karaf container with the ASM OSGi bundle and its dependencies. The `depends-maven-plugin` resolves artifact versions at build time.

## Requirements

### Requirement: Bundle deployment in Karaf

The ASM OSGi bundle SHALL deploy successfully in an Apache Karaf container with all dependencies resolved.

#### Scenario: Bundle starts without errors
- **GIVEN** a Karaf container provisioned with the ASM OSGi bundle and required features
- **WHEN** the container starts
- **THEN** the `hu.blackbelt.judo.meta.asm.osgi` bundle reaches `ACTIVE` state

### Requirement: Service availability in OSGi

Bundle tracker services SHALL be available in the OSGi service registry after deployment.

#### Scenario: AsmModelBundleTracker activates
- **GIVEN** a running Karaf container with the ASM bundle
- **WHEN** the Declarative Services runtime processes `AsmModelBundleTracker`
- **THEN** the component activates and begins tracking bundles with the `Asm-Models` header
