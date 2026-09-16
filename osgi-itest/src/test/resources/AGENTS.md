# `osgi-itest/src/test/resources` — Karaf feature repository provisioned into the Pax Exam container

| File | Purpose |
|---|---|
| `test-features.xml` | Karaf features descriptor `judo-TEST` (schema `features/v1.5.0`) declaring the single feature `test` version `1`, installed by `KarafFeatureProvider.karafConfig` from `target/test-classes/test-features.xml`. Feature pulls prerequisites `wrap`, `shell`, `scr` and then `osgi-utils`, `tinybundles`, `epsilon-runtime`, `cxf-jaxrs`, `cxf-jackson`, `cxf-rs-description-swagger2`. Repository URLs interpolate maven properties `${cxf-version}`, `${epsilon-runtime-version}`, `${osgi-utils-version}`, `${karaf-features-version}` — the file MUST go through resource filtering, an unfiltered copy leaves literal `${...}` and container provisioning fails. Feature name `test` is hard-coded in `karafConfig`; renaming it here breaks the itest. |
