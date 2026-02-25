# Development Version and Branch Handling

This document describes the branching strategy, version numbering, and CI/CD pipeline for judo-meta-asm. The workflow is based on GitFlow and uses GitHub Actions for automated builds, deployments, and releases.

## Branching Strategy

The project follows [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) with these branch types:

| Branch Pattern | Base | Purpose |
|---------------|------|---------|
| **develop** | — | Main development branch; contains latest development sources |
| **feature/JNG-xxx_summary** | develop | New features for the next release |
| **(release/)x.y.z** | develop | Stabilization before a release (`release/` prefix reserved for CI) |
| **bugfix/JNG-xxx_summary** | release | Bug fixes applied during release testing; merged back to release and develop |
| **support/JNG-xxx_summary** | release | Minor changes for a previous release; merged back to release |
| **hotfix/JNG-xxx_summary** | master | Critical fixes applied to both master and develop |
| **master** | — | Latest released sources |

### Branch Lifecycle

The following diagram shows how branches are created, merged, and flow through the development lifecycle.

```mermaid
gitGraph
   commit id: "init"
   branch develop
   checkout develop
   commit id: "dev-1"
   branch feature/JNG-1
   commit id: "feat-1a"
   commit id: "feat-1b"
   checkout develop
   branch feature/JNG-2
   commit id: "feat-2a"
   checkout develop
   merge feature/JNG-2
   merge feature/JNG-1
   branch feature/JNG-3
   commit id: "feat-3a"
   checkout develop
   merge feature/JNG-3
   branch release/1.0-beta1
   commit id: "rc-1"
   branch bugfix/JNG-4
   commit id: "fix-4a"
   checkout release/1.0-beta1
   merge bugfix/JNG-4
   checkout develop
   merge release/1.0-beta1
   checkout main
   merge release/1.0-beta1
```

## Version Numbers

Versions follow semantic versioning with rules tied to branch type:

| Branch Type | Version Rule | Example |
|-------------|-------------|---------|
| feature/ | No version change | Inherits develop version |
| develop | 2nd number incremented when release branch starts | `1.2.0-SNAPSHOT` |
| bugfix/ | No version change | Applied on release branch |
| support/ | 3rd number incremented when started | `1.1.1-SNAPSHOT` |
| hotfix/ | 4th number incremented when started | `1.1.0.1-SNAPSHOT` |

CI builds on `develop` and `feature/` branches produce long-form versions like:

```
1.1.4.20260225_103456_abc1234_feature_JNG_6382
```

Release builds on `master` and `release/` branches use the clean version from `pom.xml` without `-SNAPSHOT`.

## GitHub Actions CI/CD Pipeline

The CI/CD system consists of four interconnected workflows. When a build on a release branch completes, it triggers merge and release workflows automatically.

### Pipeline Overview

```mermaid
graph TD
    subgraph "Triggers"
        PUSH["Push on develop"]
        PR["PR on develop, master,<br/>increment/*, release/*"]
    end

    subgraph "build.yml"
        BUILD["Build & Deploy<br/>to Nexus"]
        TAG["Create git tag<br/>v&lt;version&gt;"]
    end

    subgraph "merge-pr-tagged.yml"
        MERGE_MASTER["Merge PR to master"]
        SQUASH_DEV["Squash PR to develop"]
    end

    subgraph "create-release-on-master.yml"
        GH_RELEASE["Create GitHub Release<br/>(latest)"]
    end

    subgraph "release.yml"
        REL_PR_MASTER["Create PR on master<br/>with release version"]
        REL_PR_DEV["Create PR on develop<br/>with next version"]
    end

    PUSH --> BUILD
    PR --> BUILD
    BUILD --> TAG
    TAG -->|"increment/*, release/*"| MERGE_MASTER
    TAG -->|"develop"| GH_RELEASE
    MERGE_MASTER -->|"major.minor.qualifier"| GH_RELEASE
    MERGE_MASTER -->|"other format"| SQUASH_DEV
    SQUASH_DEV --> BUILD
    REL_PR_MASTER --> BUILD
    REL_PR_DEV --> BUILD
```

### build.yml — Build and Deploy

This is the main workflow, triggered on pushes to `develop` and PRs targeting `develop`, `master`, `increment/*`, or `release/*`.

```mermaid
flowchart TD
    A["Trigger:<br/>push on develop / PR"] --> B{Branch type?}
    B -->|"master, release/*"| C["Version from pom.xml<br/>(without -SNAPSHOT)"]
    B -->|"develop, increment/*"| D["Version:<br/>major.minor.qual.date_commit_branch"]
    C --> E["Setup JDK 21 (Zulu)<br/>+ Maven settings"]
    D --> E
    E --> F["Set version via<br/>versions:set + Tycho metadata"]
    F --> G["Maven build + deploy<br/>to judong Nexus"]
    G --> H["Create git tag v&lt;version&gt;"]
    H --> I{Branch type?}
    I -->|"increment/*, release/*"| J["Create merge-pr/&lt;version&gt; tag<br/>→ triggers merge-pr-tagged.yml"]
    I -->|"develop"| K["Build changelog<br/>→ Create GitHub prerelease"]
    I -->|other| L[Done]

    G -->|"release/* only"| M["Deploy to Maven Central<br/>(Sonatype OSSRH)"]
```

**Key details:**
- Runs on custom `judong` runner
- 30-minute timeout
- GPG-signs artifacts with `sign-artifacts` profile
- Runs SonarQube analysis on `develop` branch
- Sends Discord notification with build status

### merge-pr-tagged.yml — Merge PR

Triggered when a `merge-pr/*` tag is pushed (by `build.yml`). Routes the PR to either `master` or `develop` based on version format.

```mermaid
flowchart TD
    A["Trigger: push on merge-pr/* tag"] --> B["Extract version from tag"]
    B --> C{"Version format?"}
    C -->|"major.minor.qualifier<br/>(release version)"| D["Merge PR to master"]
    D --> E["Triggers create-release-on-master.yml"]
    C -->|"other<br/>(development version)"| F["Squash PR to develop"]
    F --> G["Triggers build.yml"]
    E --> H["Delete merge-pr/&lt;version&gt; tag"]
    G --> H
```

### create-release-on-master.yml — GitHub Release

Triggered when commits are pushed to `master` (typically from a merged release PR).

```mermaid
flowchart TD
    A["Trigger: push on master"] --> B["Get version from tag"]
    B --> C["Build changelog"]
    C --> D["Create GitHub Release<br/>(latest, not prerelease)"]
```

### release.yml — Start a Release

Manually triggered with a version parameter (`auto` or a specific `major.minor.qualifier`).

```mermaid
flowchart TD
    A["Manual trigger<br/>with version parameter"] --> B{"Version = 'auto'?"}
    B -->|yes| C["Read version from pom.xml<br/>(strip -SNAPSHOT)"]
    B -->|no| D["Use provided version"]
    C --> E["Calculate next version<br/>(qualifier + 1)"]
    D --> E
    E --> F["Create PR on master<br/>with release version"]
    E --> G["Create PR on develop<br/>with next version"]
    F --> H["Triggers build.yml"]
    G --> I["Triggers build.yml"]
```

## Build Infrastructure

### Maven Build Phases

```mermaid
flowchart LR
    GEN["generate-sources<br/><i>MWE2: builders,<br/>helpers, runtime</i>"]
    COMP["compile<br/><i>javac + Tycho</i>"]
    TEST["test<br/><i>JUnit 5 +<br/>Pax Exam</i>"]
    PKG["package<br/><i>eclipse-plugin, bundle,<br/>feature, P2 site</i>"]
    VERIFY["verify<br/><i>JaCoCo coverage</i>"]
    INST["install<br/><i>local repo</i>"]
    DEPLOY["deploy<br/><i>Nexus / Maven Central</i>"]

    GEN --> COMP --> TEST --> PKG --> VERIFY --> INST --> DEPLOY
```

### External Dependencies

```mermaid
graph LR
    subgraph "External"
        EMF["Eclipse EMF Ecore"]
        EPSILON["Epsilon Runtime"]
        TYCHO["Tycho 4.0.13"]
        KARAF["Apache Karaf 4.4.7"]
        PAX["Pax Exam 4.13.5"]
    end
    subgraph "JUDO Platform"
        CLI["judo-cli-api"]
        EPP["judo-epp-common"]
        OSGIUTIL["osgi-utils"]
    end
    subgraph "judo-meta-asm"
        MODEL["model"]
        OSGI["osgi"]
        ITEST["osgi-itest"]
    end

    MODEL --> EMF
    MODEL --> EPSILON
    MODEL --> CLI
    OSGI --> OSGIUTIL
    ITEST --> KARAF
    ITEST --> PAX
```

## How to Develop

For issue tracking we use [JIRA](https://blackbelt.atlassian.net/jira/dashboards).

> **Important:** There is no commit without a ticket number. Every PR and commit message must include `JNG-xxx`.
