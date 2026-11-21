# Volt Plus Android APK

The standard Gradle package is `vp.net.voltplus`, with the launcher label `volt plus`.
Java classes retain their original namespace; this is not the installed application ID.
The existing dev workflow still builds its separate `dev.horizon.android.dev` package.

The Volt Plus APK workflow builds ARM64 on pushes to the dedicated Arena branch and
uploads a ZIP artifact containing the APK, checksum and build metadata. Artifacts
expire after 14 days and require GitHub sign-in. The workflow never falls back to a
development key: it requires Actions secrets `ANDROID_KEYSTORE_BASE64`,
`ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_ALIAS`. Set `ANDROID_KEY_PASSWORD` too
if the key password differs from the keystore password. Keep private keys and
passwords out of the repository, artifacts and chat.

Android permits an in-place update only when the installed app has the same
application ID and signing certificate and the new APK has a higher version code.
Changing the ID from `com.goydevv.horizon` or `dev.horizon.android.dev` creates a
separate installation; it cannot update or automatically migrate the old app's
data, even with the same key. Back up data before removing the old app.

The dedicated workflow uses `2026112100 + GITHUB_RUN_NUMBER` as its version code.
Do not reset the workflow run counter, and ensure any future release of this
application ID uses a higher code than the installed APK. For a local public build,
pass `--release-version-code` explicitly and provide the same release keystore via
the `HORIZON_ANDROID_KEYSTORE*` and `HORIZON_ANDROID_KEY_*` environment variables;
otherwise the build defaults to version code 30 and a development signing key and
will not update the workflow's APK.
