# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

Mandrel integration test suite for GraalVM/Mandrel Native Image. Tests build real Quarkus/Helidon
apps as native binaries and verify their output, memory, startup time, etc.

- **Language**: Java 17, Maven multi-module
- **Test framework**: JUnit 5 (Surefire for unit tests, Failsafe for integration tests under `src/it/java/`)
- **Key test classes**: `RuntimesSmokeTest` (`@Tag("runtimes")`), `AppReproducersTest` (`@Tag("reproducers")`), `JFRTest`, `DebugSymbolsTest`, `PerfCheckTest`

## Build & Test Commands

### Prerequisites
Must set `GRAALVM_HOME`/`JAVA_HOME` to a Mandrel installation with `native-image` on PATH.

```bash
# Run all tests
mvn clean verify -Ptestsuite -Dquarkus.version=<version>

# Run a single test method
mvn clean verify -Ptestsuite -Dtest=RuntimesSmokeTest#quarkusFullMicroProfile -Dquarkus.version=<version>

# Run reproducers only (no native-image build needed for some)
mvn clean verify -Ptestsuite -DincludeTags=reproducers -Dquarkus.version=<version>

# Run builder-image tests (no local native-image needed)
mvn clean verify -Ptestsuite-builder-image -Dquarkus.version=<version>
```

The `ide` profile is active by default and includes all `apps/` modules (for IDE imports). Use `-Ptestsuite` explicitly for running tests.

The default `includeTags` are `runtimes,reproducers`. Use `-DincludeTags=` / `-DexcludeTags=` to narrow scope.

## Critical Architecture

### Test → App mapping
Every test works against an entry in the [`Apps`](testsuite/src/it/java/org/graalvm/tests/integration/utils/Apps.java) enum. Each `Apps` entry wires together:
- `dir` — relative path under the project root (e.g. `apps/quarkus-full-microprofile`)
- `BuildAndRunCmds` — the exact shell commands used to build **and** run the app (last command = run)
- `WhitelistLogLines` — patterns for expected/acceptable WARN/ERROR lines in build-and-run logs
- `ContainerNames` — container name if the app runs in a container
- `threshold.conf` — optional per-app file with performance thresholds

### Adding a new test app
1. Create app dir under `apps/`
2. Add it to the `ide` profile in the root [`pom.xml`](pom.xml)
3. Add `BuildAndRunCmds` enum entry (last entry = run command)
4. Add `Apps` enum entry wiring all the above
5. Add `WhitelistLogLines` enum entry if the app produces expected WARNs/ERRORs
6. Add `@Tag` to the test method matching the test class tag

### Configuration resolution: `Commands.getProperty()`
All config is resolved via [`Commands.getProperty(key, default)`](testsuite/src/it/java/org/graalvm/tests/integration/utils/Commands.java) which checks **both** env var and system property, normalizing `UPPER_SNAKE` ↔ `lower.dot` forms. E.g., `-Dquarkus.native.builder-image=X` and `QUARKUS_NATIVE_BUILDER_IMAGE=X` are equivalent.

### `threshold.conf` format
Not plain `.properties` — supports version-conditional blocks:
```
# Default values
linux.jvm.time.to.finish.threshold.ms=6000

@IfQuarkusVersion(min ="3.6.0")
linux.jvm.time.to.finish.threshold.ms=6924

@IfMandrelVersion(min = "24", minJDK = "21.0.1")
linux.executable.size.threshold.kB=90000
```
Conditions stack: two consecutive `@If` annotations must **both** be true. Values are `long` only. Threshold for an app can be overridden via env/sys property using the pattern `APPS_<APP_DIR_UPPER_SNAKE>_<KEY_UPPER_SNAKE>=<value>`.

### Log checking
[`Logs.checkLog()`](testsuite/src/it/java/org/graalvm/tests/integration/utils/Logs.java) scans `build-and-run.log` for lines matching `ERROR|SEVERE|WARN|No such file|Not found|unknown` and fails the test unless the line matches a pattern in `WhitelistLogLines`. Always add new expected errors to `WhitelistLogLines`, not to test assertions.

### Version-gating tests
Use `@IfMandrelVersion` / `@IfQuarkusVersion` annotations (closed intervals):
```java
@IfMandrelVersion(min = "23.1.2", minJDK = "21.0.1")
@IfQuarkusVersion(min = "3.6.0")
public void myTest(TestInfo testInfo) { ... }
```
These replace `@Test` (they are meta-annotated with `@Test`). `inContainer = true` means the version is pulled from the builder image, not the local install.

## Code Style

- Static imports are used heavily; all utility methods in `Commands` are imported statically.
- `jboss-logging` (`Logger.getLogger(Foo.class.getName())`) is the logger — not SLF4J or java.util.logging.
- `@formatter:off` / `@formatter:on` markers are present; respect them for dense data structures.
- All test source lives in `testsuite/src/it/java/` (added via `build-helper-maven-plugin`), not `src/test/java/`.
- Test build/run logs land in `<appDir>/logs/build-and-run.log`; archived results in `testsuite/target/archived-logs/`.
- Container runtime defaults to **podman** with sudo; set `QUARKUS_NATIVE_CONTAINER_RUNTIME=docker` to use Docker.
