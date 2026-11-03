# `model/src/main/java/hu/blackbelt/judo/eclipse/asm` — Eclipse UI plug-in lifecycle for the ASM metamodel bundle

| File | Purpose |
|---|---|
| `Activator.java` | Eclipse plug-in lifecycle hook, extends `AbstractUIPlugin`. Exports constant `PLUGIN_ID` = `hu.blackbelt.judo.meta.asm`, `start(BundleContext)` / `stop(BundleContext)`, and static `getDefault()` returning the singleton. Singleton is set in `start` and nulled in `stop`, so `getDefault()` returns `null` outside an active Eclipse/OSGi runtime — non-UI callers (CLI, Karaf, tests) must not depend on it. Not the OSGi bundle tracker; runtime model registration lives in `AsmModelBundleTracker`. |
