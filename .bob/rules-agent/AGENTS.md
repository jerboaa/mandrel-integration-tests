# Project Coding Rules (Non-Obvious Only)

- Test source is in `testsuite/src/it/java/` (not `src/test/java/`), registered via `build-helper-maven-plugin`.
- Adding a new app under `apps/` requires **four** coordinated enum additions: `BuildAndRunCmds`, `Apps`, `WhitelistLogLines`, and a `ContainerNames` entry if containerized. Missing any one causes a compile or runtime failure.
- The last command in a `BuildAndRunCmds` entry is **always** the run command; all earlier commands are build steps.
- `Commands.getProperty(key, default)` must be used for all externally-configurable values — it normalizes both `UPPER_SNAKE_CASE` env vars and `lower.dot.case` system properties as equivalent forms.
- `@IfMandrelVersion` and `@IfQuarkusVersion` are meta-annotated with `@Test` — use them **instead of** `@Test`, not in addition to it.
- Expected WARN/ERROR log lines must be added to `WhitelistLogLines` (not suppressed in test logic); the log scanner pattern is `ERROR|SEVERE|WARN|No such file|Not found|unknown` (case-insensitive).
- `threshold.conf` values must be `long`; the format supports version-conditional blocks (`@IfQuarkusVersion`, `@IfMandrelVersion`) that are **not** standard `.properties`.
- Use `jboss-logging` (`org.jboss.logging.Logger`) for all logging — not SLF4J, JUL, or Log4j directly.
- `@formatter:off` / `@formatter:on` blocks exist in dense enum constructors; preserve them.
