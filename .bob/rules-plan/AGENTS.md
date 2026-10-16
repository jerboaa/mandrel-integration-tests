# Project Architecture Rules (Non-Obvious Only)

- **Apps are not tests** — `apps/` contains independent Maven modules (real Quarkus/Helidon apps). The testsuite builds and runs them as external processes; it does not import them as test dependencies.
- **No shared base class for test methods** — `testRuntime()` and similar helpers exist in `RuntimesSmokeTest` and `AppReproducersTest` but they are not inherited; the test method must call the helper explicitly with its own `TestInfo`.
- **`BASE_DIR` is not the project root** — `Commands.getBaseDir()` returns the **parent** of the Maven `basedir` property (i.e., `testsuite/../` = the repo root), so `apps/` dirs resolve correctly from test code.
- **Container runtime defaults to podman+sudo** — `PODMAN_WITH_SUDO=true` by default; `ROOTLESS_CONTAINER_RUNTIME=false` by default. Builder-image tests require a running container daemon.
- **Quarkus version is switched at test runtime** — the POM property `global.quarkus.version` is a fallback only; all CI runs pass `-Dquarkus.version=<version>` explicitly, which overrides it inside `BuildAndRunCmds`.
- **GraalVM SDK dependency is for version-detection only** — `graal-sdk` (`org.graalvm.sdk`) is used solely for `org.graalvm.home.Version` comparison; Mandrel itself must be installed separately.
- **Two Maven profiles for the testsuite**: `testsuite` (excludes `builder-image` tag) and `testsuite-builder-image` (includes only `builder-image` tag); never run both together.
