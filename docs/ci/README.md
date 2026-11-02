# CI: Build and Release

`build-appshare.yml` in this folder is a copy of the active
`.github/workflows/build-appshare.yml`. Keep the two in sync.

## Release signing secrets (required)

The workflow needs four repository Actions secrets to sign the release APK:

| Secret | Meaning |
|---|---|
| `KEY_STORE` | Base64 of the original binary `.jks` (or `.p12`) keystore file |
| `KEY_STORE_PASSWORD` | Password that opens the keystore |
| `KEY_PASSWORD` | Password that unlocks the private key |
| `KEY_ALIAS` | Alias of the signing key inside the keystore |

Add them under **Settings → Secrets and variables → Actions → New repository
secret**. The workflow validates all four before building and fails fast with a
clear message if any is missing, malformed, or does not match the keystore.
Never generate a new keystore for releases — keep the original one so existing
installations can be updated.

## What the workflow does

1. Checks out the code and sets up JDK 17 + Android SDK (API 36).
2. Validates the Gradle wrapper.
3. Decodes and validates the release keystore from the four secrets.
4. Derives the app version from the release tag.
5. Builds the release APK with Gradle (`assembleRelease`).
6. Renames the APK to `DarkNama-<tag>-universal.apk`.
7. Publishes a GitHub Release with the APK attached.

`Create Release` only runs from `master` or a `v*` tag, so branch pushes are
test builds and never create a release.

## Version stamping

| | Before | After |
|---|---|---|
| `Get tag name` step | runs **after** `Build APK` | runs **before** `Build APK` |
| Gradle invocation | `./gradlew assembleRelease` | `./gradlew assembleRelease -PappVersionName=\"${APP_VERSION_NAME}\"` |
| APK `versionName` | always the fallback (`2.1.0`) | the release tag (`v2.5.0` → `2.5.0`) |
| APK `versionCode` | derived from fallback (`20100`) | derived from the tag (`2.5.0` → `20500`) |
| Asset file name | `DarkNama-universal.apk` | `DarkNama-v2.5.0-universal.apk` |
| Release notes | — | includes `**App version:** 2.5.0` |

`APP_VERSION_NAME` is the tag with a leading `v`/`V` stripped, so the version
shown inside the app is `2.5.0`, not `v2.5.0`.

## Where the version lives in the app

`app/build.gradle.kts` resolves the version once and feeds
`BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE`:

```kotlin
val fallbackVersionName = "2.1.0"                       // local builds only

val resolvedVersionName: String =
    (project.findProperty("appVersionName") as String?) // CI: -PappVersionName
        ?.trim()?.removePrefix("v")?.removePrefix("V")
        ?.takeIf { it.isNotBlank() }
        ?: fallbackVersionName
```

`versionCode` is computed as `major * 10000 + minor * 100 + patch`
(`2.5.0` → `20500`) using Long arithmetic so large components cannot overflow,
and can still be overridden with `-PappVersionCode=<n>`.

Consumers of `BuildConfig.VERSION_NAME`:

* `screens/AboutScreen.kt` — version line under the app name (`R.string.app_version`)
* `screens/SettingsScreen.kt` — "current version" row (`R.string.current_version`)
* `screens/SettingsScreen.kt` — comparison against the latest GitHub release tag

## Tagging rules

`versionCode` must increase monotonically or Android refuses to install the
update over an older build:

| Tag | `versionCode` | Installs over previous |
|---|---|---|
| `v2.1.0` | 20100 | — |
| `v2.2.0` | 20200 | yes |
| `v2.2.1` | 20201 | yes |
| `v2.0.9` | 20009 | no (lower) |

The workflow triggers on tags matching `v*`, so tags must start with `v`.
