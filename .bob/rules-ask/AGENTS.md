# Project Documentation Context (Non-Obvious Only)

- All test utility classes (`Commands`, `Logs`, `Apps`, `BuildAndRunCmds`, `WhitelistLogLines`, `ContainerNames`) are enums or static-utility classes under `testsuite/src/it/java/.../utils/`.
- `threshold.conf` files are per-app, stored in each `apps/<name>/` directory; they use a custom format with `@IfQuarkusVersion`/`@IfMandrelVersion` blocks — documented in [`README.md`](README.md) under "Thresholds properties".
- `build-and-run.log` for each test run is in `apps/<name>/logs/`; archived copies land in `testsuite/target/archived-logs/<TestClass>/<testMethod>/`.
- Version filtering logic (parsing `native-image --version` output for both old single-line and new 3-line formats) is in `UsedVersion.java` — consult it for any version-gating questions.
- `QUARKUS_VERSION` / `quarkus.version` are the same config key due to `Commands.getProperty()` normalization.
- The `apps/quarkus-spöklik-encoding/` directory name intentionally contains a non-ASCII character (ö); this tests locale/encoding handling.
