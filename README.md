# 🔓 Zip Unlocker (`zip-unlocker-android`)

[![Build Android APK & Release](https://github.com/jh4315444-bit/zip-unlocker-android/actions/workflows/android-apk.yml/badge.svg)](https://github.com/jh4315444-bit/zip-unlocker-android/actions/workflows/android-apk.yml)
![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?logo=android)
![Language](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose)

**Zip Unlocker (জিপ আনলকার ও আর্কাইভ ভল্ট)** is a complete, offline-first Android application built with Kotlin, Jetpack Compose (Material 3), and `Zip4j`. It allows users to inspect `.zip` archives, unlock and extract password-protected ZIP files (`AES-256`, `AES-128`, and `ZipCrypto`), choose custom output folders via the Android Storage Access Framework (SAF), test candidate passwords/hints on authorized archives when a password is forgotten, and create new encrypted ZIP archives.

---

## 📥 Download Installable APK

You can download and install the compiled Android APK directly in two ways:

1. **From GitHub Actions Artifacts (Automatic on Every Push):**
   - Go to the [**Actions**](../../actions/workflows/android-apk.yml) tab of this repository.
   - Click on the latest successful **Build Android APK & Release** workflow run.
   - Scroll down to the **Artifacts** section and download **`zip-unlocker-android-apk`** (contains `zip-unlocker-android-v1.0-debug.apk`).
2. **From GitHub Releases (Tagged Releases):**
   - Visit the [**Releases**](../../releases/latest) page of this repository.
   - Download `zip-unlocker-android-v1.0-debug.apk` directly to your Android phone and tap to install.

---

## ✨ Features

### 1. ZIP Unlocker & Archive Inspector (`আনলকার`)
- **Select Any `.zip` File:** Pick ZIP files from device storage or SD card using Android's zero-permission Storage Access Framework (`ActivityResultContracts.OpenDocument`).
- **Deep Archive Metadata Inspection:** View encrypted status, encryption algorithm (`AES-256`, `AES-128`, `ZipCrypto Standard`, or `Unlocked`), uncompressed/compressed sizes, compression ratios, and individual entry checkboxes.
- **Custom Output Folder & Real-Time Extraction Progress:**
  - Extract to the built-in **App Vault** or choose any folder on your device (`ActivityResultContracts.OpenDocumentTree`).
  - Live progress bar showing entry-by-entry extraction progress and completion percentage.
- **Clear Error Feedback:** Displays clear Bengali & English error banners if an invalid ZIP or wrong password is supplied.
- **Built-in Test Archives:** Includes 3 one-tap sample generators (`AES-256 PIN: 1234`, `ZipCrypto Hint: dhaka123`, and `Standard Unlocked ZIP`) for instant verification.

### 2. Password Testing & Recovery Assistant (`রিকভারি`)
- Designed for **authorized recovery of your own archives** when you forget a PIN or password variation:
  - **Common Archive Passwords:** Tests frequently used passwords and year patterns.
  - **4-Digit PIN Sweep (`0000–9999`):** Rapidly tests all 4-digit numeric PINs and years (`1970–2030`).
  - **Smart Hint Permutations:** Generates and tests hundreds of combinations from a user-supplied keyword/hint (case variations, common prefixes/suffixes like `123`, `2026`, `@`, `#`, `!`).
  - **Custom Wordlist Input:** Paste comma- or newline-separated candidate passwords to test automatically.

### 3. Encrypted ZIP Creator (`জিপ তৈরি`)
- Select multiple files from your device or write a quick text note inside the app.
- Compress and lock them into a new `.zip` archive using **AES-256 Military-Grade Encryption** or **Standard ZipCrypto**.

### 4. Unlocked File Vault & In-App Viewer (`ভল্ট ফাইল`)
- Browse all extracted folders, read text/CSV/JSON/code files directly inside the built-in viewer, or share/export files to any app via Android `FileProvider`.

---

## 📸 Screenshots & Visual Overview

| Vault Hero Artwork | App Icon |
| :---: | :---: |
| ![Hero Banner](app/src/main/res/drawable/img_hero_vault_1791675391814.jpg) | ![App Icon](app/src/main/res/drawable/img_app_icon_fg_1791675381624.jpg) |

> *Tip: Once installed on your device or emulator, capture screenshots of the **Unlocker**, **Recovery**, **Create ZIP**, and **Vault** tabs and place them in a `docs/screenshots/` folder if you wish to showcase device frames.*

---

## 🔒 Security, Privacy & Limitations

- **Zero Password Logging:** User-entered passwords, candidate wordlists, and recovered passwords are kept strictly in transient memory during archive operations and are **never** written to `Logcat`, analytics, or persistent disk storage.
- **Offline-Only Execution:** This app does **not** request the `INTERNET` permission. All archive inspection, decryption, extraction, and password testing run 100% locally on your Android device.
- **Least-Privilege Permissions:** Uses Android's system document pickers (`OpenDocument`, `OpenMultipleDocuments`, `OpenDocumentTree`) instead of requesting broad storage permissions (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`). Only `VIBRATE` is declared in `AndroidManifest.xml`.
- **Technical Limitations of Password Recovery:**
  - **No Guaranteed Recovery:** Strong cryptographic algorithms like **AES-256** cannot be bypassed or "cracked" instantaneously without the correct key.
  - Dictionary and hint-based testing can only recover passwords that match common patterns, 4-digit PINs, or permutations of a known hint/wordlist. Long, random passwords cannot be recovered if completely forgotten.
  - Use the password-testing feature **only on archives you own or are explicitly authorized to access**.

---

## 🛠️ Requirements & Tech Stack

- **Android Studio:** Ladybug / Meerkat or newer (AGP `9.1.1`)
- **JDK:** Java 11+ (JDK 21 recommended for Gradle `9.3.1`)
- **Android SDK:** `minSdk = 24` (Android 7.0), `targetSdk = 36`, `compileSdk = 36`
- **Core Libraries:**
  - Kotlin `2.2.10` & Coroutines `1.10.2`
  - Jetpack Compose BOM `2024.09.00` & Material 3
  - `net.lingala.zip4j:zip4j:2.11.5` for standard and AES-encrypted ZIP operations
  - Robolectric `4.16.1` & JUnit 4 for local JVM unit testing

---

## 🚀 Build & Installation Instructions

### Build Locally with Gradle

1. **Clone the repository:**
   ```bash
   git clone https://github.com/jh4315444-bit/zip-unlocker-android.git
   cd zip-unlocker-android
   ```

2. **Run Unit & Robolectric Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

3. **Assemble the Installable Debug APK:**
   ```bash
   gradle :app:assembleDebug
   ```
   The compiled, signed, and installable APK will be generated at:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Install on a Connected Android Device:**
   - Copy `app-debug.apk` to your phone and open it to install (allow *"Install unknown apps"* if prompted), or run:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
