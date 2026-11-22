# JVM / launcher compatibility: feasibility and integration status

**Status: not implemented.** Volt Plus does not currently provide JVM compatibility
controls, per-application overrides, or an adapter that configures another Android
launcher. No universal compatibility claim is supported by the current tests.

## Current architecture

- The shipping Android launcher and settings pages are Rust (`crates/horizon-client/src/android/launcher.rs`,
  `launcher_pages.rs`, and `catalog.rs`). Settings are serialized to
  `android-launcher.json` by `catalog::Config`, not SharedPreferences or DataStore.
- `packaging/android/compose/HorizonComposeLauncher.kt` is present, but the shipping
  Gradle source set contains only `packaging/android/java`; it is not the shipping
  Compose settings UI. `HorizonActivity` is an Android `NativeActivity`, which loads
  the native Horizon library. The current bridge is manually registered JNI
  (`crates/horizon-client/src/android/document_bridge.rs`), not generated UniFFI
  bindings. No generated compatibility struct exists.
- The documented Android game path is a native Rust port, not an arbitrary Java
  Edition JAR runner; see [Android documentation](android.md). A legacy build script
  mentions Java preparation launchers; that does not establish a JVM launch path for
  external Android apps or a capability to configure them.
- `packaging/android/build.gradle` defaults to API 28 minimum and targets API 35;
  release builds have `debuggable false` and `minifyEnabled false`. The actual values
  for a particular build depend on its Gradle properties and build path.

## Limits of the requested controls

Another installed Android application's process is isolated from Volt Plus. Unless
that application explicitly cooperates through a defined interface, Volt Plus cannot
set its Java heap or GC flags, choose ART versus an embedded JVM, turn on debugging
or JIT wait, change its perceived Android SDK, or disable its hidden-API restrictions.
Starting an app with an intent does not give the caller control of those process
settings. Changing Volt Plus's own settings does not configure another process.

For a **cooperating runtime that Volt Plus starts**, heap and GC options may be passed
before the runtime starts and only where the specific runtime recognizes them. They
cannot generally be changed on a running JVM. G1 and CMS are runtime/version specific;
CMS has been removed from newer HotSpot releases. An Android ART process does not
accept arbitrary HotSpot JVM flags. Debugging and interpreter/JIT options likewise
require runtime- and process-specific support and may require a debuggable build.
Android API-level compatibility and hidden-API restrictions cannot be generically
changed for another application; no bypass or SDK spoof is implemented here.

A useful per-package override requires a known target package, a trustworthy way to
identify it, and an explicit mechanism that target consumes. Neither an arbitrary
package name nor a stored preset makes a target cooperate.

## Required integration decision

Identify the specific launcher(s) and process to configure, and provide their
supported configuration/launch interface or authorize a coordinated change to their
source. If the intended target is instead a JVM embedded or launched **by Volt Plus**,
identify its existing runtime entry point and versions, or specify that a new runtime
integration is in scope. Only after that boundary is established can the project
implement and test real application of settings without creating misleading knobs.

## Verification required before any compatibility claim

No device matrix has been run for this feature. At minimum, record launcher and
runtime versions, Android versions and device/ABI, setting, launch mode, observed
runtime value, outcome, and logs. Tests should cover validated persistence and
per-package precedence, default-path regression, the actual Kotlin/Rust boundary if
one is introduced, supported and rejected runtime flags, and a cooperating sample
launcher that observes a setting after a real process start. Tests on a defined
matrix can establish those cases; they cannot prove universal compatibility across
all launchers, Android releases, ROMs, and runtime versions.
