# site Specification

## Purpose

The `site` module generates an Eclipse P2 update site (repository) that hosts the ASM feature and its plugins for remote installation.

## Architecture

Uses Tycho `eclipse-repository` packaging. References P2 repositories for dependencies (EMF, Epsilon, JUDO EPP commons) and bundles the ASM feature for distribution.

## Requirements

### Requirement: P2 repository generation

The site SHALL produce a valid P2 repository containing the ASM feature and all included plugins.

#### Scenario: Build update site
- **WHEN** `./mvnw clean install` is run on the site module
- **THEN** a P2 repository is generated in `target/repository/` with `content.xml` and `artifacts.xml`

### Requirement: Dependency repository references

The site SHALL reference all required P2 repositories so that Eclipse can resolve transitive dependencies during installation.

#### Scenario: EMF dependencies resolved
- **GIVEN** the update site is added in Eclipse
- **WHEN** the ASM feature is selected for installation
- **THEN** Eclipse can resolve EMF Ecore, Epsilon, and other dependencies from the referenced repositories
