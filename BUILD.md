# Kazi Agrotech - Android APK Build Guide 🚀

This document explains the project build setup, how to build locally or on GitHub Actions, and how to create signed release builds for Google Play Store.

---

## 1. Summary of Fixes & Project Health Checks

- **Kotlin Compilation & Syntax Fixes**:
  - Resolved unresolved imports (`BanglaNumberFormatter`, `ArrowDropDown`, `remember`, `offset`).
  - Corrected `allUsers` collection state in `FarmNotificationDialog`.
  - Fixed syntax/brace structure in `DailyReportScreen` and `MonthlyExpenseScreen`.
  - Refactored `Interactions.kt` / `AccessDeniedView` with clean imports.
- **Gradle & Environment Integrity**:
  - Ensured Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, `gradle-wrapper.properties` targeting Gradle 9.3.1) is complete.
  - Ensured `debug.keystore` is committed and safeguarded with on-the-fly generation in CI if missing.
  - Handled secrets plugin gracefully with fallback from `.env` to `.env.example`.
- **Firebase & Resources**:
  - `applicationId` (`com.aistudio.kaziagro.poultr`) matches `google-services.json` package configuration.
  - All manifest resource links (`@xml/file_paths`, `@xml/backup_rules`, `@xml/data_extraction_rules`, `@mipmap/ic_launcher`) are fully validated.

---

## 2. How to Build Locally

### Option A: Using Android Studio (Recommended)
1. Open **Android Studio** (Ladybug / Koala / 2024.1+).
2. Choose **File > Open** and select this project folder (`kazi-agrotech-fixed-v7`).
3. Allow Gradle to sync dependencies.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Once built, click **locate** to find `app-debug.apk`.

### Option B: Using Command Line (Terminal / PowerShell)
- **Windows (PowerShell / CMD)**:
  ```powershell
  .\gradlew.bat assembleDebug
  ```
- **macOS / Linux**:
  ```bash
  chmod +x ./gradlew
  ./gradlew assembleDebug
  ```
- The resulting APK is generated at:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## 3. How to Build via GitHub Actions (CI/CD)

The repository includes an automated workflow configured in [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

### Automatic Build:
Every time you push code to `main` or `master`, GitHub Actions automatically compiles the project and generates the APK.

### Manual 1-Click Trigger:
1. Navigate to your repository on GitHub: [`https://github.com/Reduwan122/kazi-final`](https://github.com/Reduwan122/kazi-final)
2. Go to the **Actions** tab.
3. In the left sidebar, click **Build Android APK**.
4. Click **Run workflow** -> Select `main` branch -> Click **Run workflow**.

### Downloading the APK:
1. When the build finishes with a green checkmark (✓), click into the workflow run.
2. Scroll down to the **Artifacts** section at the bottom.
3. Click on **`kazi-agrotech-debug-apk`** to download the zip containing `app-debug.apk`.

---

## 4. How to Generate a Signed Release Build (Google Play Store)

Debug APKs are suitable for testing. For distribution on the Google Play Store or production installation, generate a private signing key.

### Step 1: Create a Production Keystore
Run the following command in your terminal:
```bash
keytool -genkey -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
*(Store `my-upload-key.jks` and your passwords in a safe, secure place. **Do NOT commit private production keystores to public git repositories**).*

### Step 2: Build Signed Release APK / AAB
Pass the keystore environment variables or configure them in Android Studio:
```bash
export KEYSTORE_PATH="/path/to/my-upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="upload"
export KEY_PASSWORD="your_key_password"

# Build Release APK
./gradlew assembleRelease

# Or Build Google Play Android App Bundle (.aab)
./gradlew bundleRelease
```
The output will be generated at:
- **APK**: `app/build/outputs/apk/release/app-release.apk`
- **AAB**: `app/build/outputs/bundle/release/app-release.aab`
