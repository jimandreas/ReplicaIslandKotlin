# Release Build Instructions

## Prerequisites

1. **Signing Configuration**: For local builds, create `gradle/signing.properties`:
   ```properties
   STORE_FILE=/path/to/your/release.keystore
   STORE_PASSWORD=your_store_password
   KEY_ALIAS=your_key_alias
   KEY_PASSWORD=your_key_password
   ```

   For CI builds (GitHub Actions), set these repository secrets:
   - `SIGNING_KEY` - base64-encoded keystore file
   - `KEY_STORE_PASSWORD` - keystore password
   - `ALIAS` - key alias
   - `KEY_PASSWORD` - key password

## Build Commands

### Debug APK
```bash
./gradlew :app:assembleDebug
```

### Release APK
```bash
./gradlew :app:assembleRelease
```

## Output Locations

| Build Type   | Output Path                                     |
|--------------|-------------------------------------------------|
| Debug APK    | `app/build/outputs/apk/debug/`                  |
| Release APK  | `app/build/outputs/apk/release/`                |

## Creating a GitHub Release

Push a version tag to trigger the release workflow:

```bash
git tag v1.0
git push origin v1.0
```

The workflow will:
1. Build a signed release APK
2. Upload it as a build artifact
3. Create a GitHub Release with the APK attached and auto-generated release notes

You can also trigger the workflow manually from the Actions tab.

## Version Management

Version numbers are configured in `app/build.gradle.kts`:
```kotlin
versionCode = 1
versionName = "1.0"
```

Update these values before creating a new release.
