# Modernization Plan: SIGE Java 25 Runtime Upgrade

**Project**: sige

## Technical Framework

- **Language**: Java 21
- **Framework**: JavaFX desktop application and Java HTTP web module
- **Build Tool**: Maven
- **Database**: MySQL
- **Key Dependencies**: JavaFX, MySQL Connector/J, HikariCP, JUnit

## Overview

Upgrade SIGE from Java 21 to Java 25 while preserving existing application behavior. Bring the build, compiler, runtime, deployment/container, launch, packaging, and relevant documentation configuration into alignment with Java 25. Keep the existing desktop and web application features, JavaFX and MySQL behavior, packaging purpose, and deployment model unchanged. Make only dependency or configuration changes demonstrated to be necessary for Java 25 compatibility.

## Migration Impact Summary

| Application | Current Runtime | Target Runtime | Impact |
|---|---|---|---|
| SIGE desktop and web | Java 21 | Java 25 | Build, launch, package, container, docs |

This is a Java runtime/build modernization only; no Azure service migration or cloud-readiness remediation is in scope.

## Assessment Scope

The supplied assessment identifies three incidents for `azure-java-version-02000` in `pom.xml` (lines 11, 12, and 49), under **Java Version is not the latest LTS**. The report detects Java 21 and Maven. This plan addresses only that Java-version category; the report's unrelated database, networking, configuration, file-system, and cryptography findings are excluded.

## Scope and Constraints

- Target Java 25 from the current Java 21 baseline.
- Align Maven compiler settings and the JDK used by CI, container builds/runtimes, and packaging.
- Review JavaFX and other runtime/build dependencies for Java 25 compatibility; change versions only where required for a working Java 25 build or runtime.
- Check the existing launch and packaging configuration for Java 25 compatibility and update corresponding guidance.
- Update relevant Java-version requirements in project documentation.
- Preserve observable application behavior and existing supported desktop/web workflows.
- Do not upgrade Spring or other application frameworks, migrate services, change database behavior, or remediate unrelated cloud-readiness findings.

## Validation and Completion

The upgrade is complete when the project builds and its existing unit tests pass under Java 25; the desktop and web launch paths and the Docker build/runtime use Java 25 successfully; the packaging workflow remains functional; and the relevant documentation consistently states Java 25. Review the Java 25 dependency/toolchain changes for vulnerabilities limited to components affected by this upgrade. No behavior changes are intended.

## Java 25 execution plan and change review

The project is a single Maven JAR project. Its baseline is Java 21 (`maven.compiler.source`, `maven.compiler.target`, and compiler-plugin `release`), with JavaFX 21.0.2, Maven Compiler Plugin 3.13.0, Surefire 3.5.2, and Maven 3.9.9 in the Docker builder (updated to 3.9.15 below). There is no Maven wrapper. JavaFX and application dependencies are intentionally unchanged: no incompatibility has been established that warrants dependency upgrades for this runtime-only change.

| File | Planned Java 25 change |
|---|---|
| `pom.xml` | Set Maven source, target, and compiler-plugin release to 25. |
| `.github/workflows/ci.yml` | Configure CI `setup-java` to use Java 25. |
| `Dockerfile` | Use Maven 3.9.15/Temurin 25 for build stage and Temurin 25 JRE for runtime stage. |
| `empaquetar.bat` | State that JDK 25 (including `jpackage`) is required. |
| `README.md`, `README_WEB.md`, `DESPLIEGUE_GRATIS.md`, `DEVOPS_PRUEBAS.md` | Update current JDK/runtime requirements, CI description, and `jpackage` reference to Java 25. |

### Execution status

- Applied the changes listed above. The Maven container image moves to 3.9.15 to pair the Java 25 build environment with a current Maven 3.9 release. Desktop/web code, application dependency versions, JavaFX behavior, container command, packaging flow, configuration, and secrets handling were not changed.
- Reviewed the edits for sufficiency and necessity: compiler settings, CI, Docker build/runtime, packaging guidance, and current Java-version documentation align to 25; no unrelated source or configuration changes were made.
- Validation is pending. The Java upgrade tools (JDK/Maven inventory, build, tests, and event/session service) returned `The selected model 'agent-host-copilotcli:auto' is not available`; the local shell is also unavailable. Therefore Java 25 compilation, test execution, CVE validation, and JDK installation status could not be verified in this environment. No claim of successful build or tests is made.
- No version-control operations or commits were performed, as requested.
