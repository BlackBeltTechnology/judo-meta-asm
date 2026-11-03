# `model/src/main/java/org/eclipse/emf/ecore/util` — split-package shims placed in the EMF namespace so the bundle exports Ecore XMI resource types under `org.eclipse.emf.ecore.util`

| File | Purpose |
|---|---|
| `EcoreResourceFactoryImpl.java` | Empty subclass of `org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl`, declared in package `org.eclipse.emf.ecore.util`. Adds no field, no method, no override — its only effect is relocating the factory type into the package this bundle exports (see `osgi/pom.xml` `org.eclipse.emf.ecore.util.*` export). Behaviour is inherited verbatim; do NOT expect the UUID-forcing behaviour of `AsmModelResourceSupport.getAsmFactory()` here. |
| `EcoreResourceImpl.java` | Thin `XMIResourceImpl` subclass in package `org.eclipse.emf.ecore.util`, exposing only the `EcoreResourceImpl(URI)` constructor delegating to `super(uri)`. No override — notably `useUUIDs()` stays default `false`, so resources created through this type carry no xmiids, unlike the anonymous resource `AsmModelResourceSupport.getAsmFactory()` builds. Exists as the resource counterpart of the co-located factory shim. |
