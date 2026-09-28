# NIVORA — Ultimate Android Browser

> **Search Freely. Browse Privately.**  
> **Created by Bhaskar Gautam** • *Developed by Bhaskar*

---

## 1. Project Overview

**NIVORA** is a native, modern, privacy-first Android web browser built from the ground up using **Kotlin**, **Jetpack Compose (Material 3)**, and **Android WebView / GeckoView architecture**.

NIVORA eliminates telemetry and silent data collection, providing a fully transparent, high-performance web experience with on-device intelligence, tracker blocking, tab management, and an offline productivity suite.

---

## 2. Core Features Matrix

### Web Browsing & Navigation
- **Multi-Tab Architecture**: Standard tabs, Incognito/Private tabs, and Named Tab Groups.
- **Biometric App Lock**: Fingerprint, Face, and Device Credential lock protection with startup challenge.
- **Search & Navigation**: Multi-engine search resolver (Google, DuckDuckGo, Bing, Brave, Ecosia, StartPage) with custom keyword search shortcuts (`yt`, `wiki`, `gh`, `ddg`, `reddit`, and custom entries).
- **Desktop Mode**: Per-site and global Desktop User-Agent toggle.
- **Reader Mode**: Distraction-free article extractor with typography controls (font size, line height, reading width) and themes (Light, Sepia, Dark, AMOLED Pure Black).
- **Fullscreen Video & Media**: HTML5 fullscreen video playback, orientation handling, and custom view dismissal.
- **Native File Uploads & Downloads**: Integrated Android Photo Picker (`PickVisualMedia`), file chooser, and SQLite-backed multi-threaded download manager with progress tracking and MIME-type opening.

### Privacy & Security Suite
- **NIVORA Privacy Shield**: Network-level blocking of advertising networks, cryptominers, fingerprinting scripts, and tracking beacons via EasyList/Disconnect blocklists.
- **Granular Cookie & Cache Manager**: Isolate third-party cookies, clear cookies per-domain, and perform instant emergency session purges.
- **HTTPS & Security Center**: TLS certificate inspector, mixed-content blocking, and connection security badges.
- **Encrypted Sync (Default: OFF)**: End-to-end encrypted synchronization for bookmarks, notes, reading list, and settings with strictly opt-in history syncing.
- **Zero Fake Infrastructure**: No mock trackers, simulated VPNs, or fake counters.

### Native Utilities & Productivity Suite
- **NIVORA AI**: Context-aware webpage intelligence (summarization, key points, plain-English explanation, translation, and automated research notes) powered by Gemini and local on-device heuristic fallbacks.
- **Translation Engine**: Real client-side script translation across 12 languages with original webpage restoration.
- **NIVORA Notes**: SQLite/Room-backed research notebook with search, pin/unpin, and Android system sharing.
- **Reading List**: Offline reading list with read/unread toggles and search.
- **Web Apps & PWA Manager**: Install, run, and manage standalone Progressive Web Applications.
- **Offline Dev Tools**:
  - QR Code Scanner (CameraX) & QR Barcode Generator
  - Cryptographically secure Password Generator with entropy calculation
  - JSON Viewer & Formatter (Syntax validation and 2-space indentation)
  - Offline Text Scratchpad with word/character/line counter
  - Markdown Viewer with real-time preview
  - URL Query Encoder / Decoder
  - Unit Converter (Storage, Length, Temperature)
  - Color Picker & HEX/RGB palette tool
  - Timestamp & Unix Epoch converter

### Data Ownership & Portability
- **Data Export**: Export bookmarks, notes, reading list, and settings to JSON and standard Netscape Bookmark HTML.
- **Data Import**: Validated importer rejecting scripts, with confirmation required before any destructive data restore.

---

## 3. Architecture & Tech Stack

```
com.nivora.browser/
├── ai/                 # Gemini API & Local Heuristic AI Engine
├── browser/            # Browser Engine abstraction & WebView implementations
├── data/
│   ├── backup/         # JSON & Netscape HTML Data Transfer Manager
│   └── local/          # DataStore Preferences (Theme, Search, Privacy settings)
├── database/           # Room Database, DAOs, and Entities
├── downloads/          # Native Download Manager & HTTP file streaming
├── permissions/        # Runtime permission contracts
├── privacy/            # Blocklist engine, Tracker Shield, Biometrics
├── search/             # Multi-provider query builder & keyword shortcuts
├── sync/               # Encrypted sync models & device management
├── tabs/               # TabManager, Tab Groups, and session state
├── translation/        # Multilingual DOM translation engine
├── ui/
│   ├── ai/             # NIVORA AI interface & tablet split-panel
│   ├── backup/         # Backup & Restore UI
│   ├── bookmarks/      # Bookmarks & Folder manager
│   ├── browser/        # Browser screen, WebView host, toolbars
│   ├── downloads/      # Download progress and history
│   ├── history/        # History list, search, and clearing
│   ├── home/           # Homepage, quick dial, search bar
│   ├── notes/          # Room-backed NIVORA Notes UI
│   ├── privacy/        # Privacy Shield, Security Center, Cookie Manager
│   ├── qr/             # QR Scanner & Generator
│   ├── reader/         # Clean Reader Mode
│   ├── readinglist/    # Reading list screen
│   ├── settings/       # Settings, Theming, Search, and About
│   ├── sync/           # Sync & device management UI
│   ├── tabs/           # Tab switcher grid & group manager
│   ├── theme/          # M3 Theme, Typography, Accent colors, AMOLED
│   └── tools/          # Offline utilities suite
└── webapps/            # Standalone PWA management
```

---

## 4. Android Studio Setup & Requirements

- **Android Studio**: Android Studio Ladybug (2024.2.1) or newer
- **Gradle**: 8.9+ (Kotlin DSL)
- **Android Gradle Plugin (AGP)**: 8.7+
- **JDK**: Java 17 or Java 21
- **Minimum SDK (`minSdk`)**: 24 (Android 7.0 Nougat)
- **Target SDK (`targetSdk`)**: 36
- **Compile SDK (`compileSdk`)**: 36

---

## 5. Build Instructions

### Debug APK Build
Run the following command from the root directory:
```bash
gradle :app:assembleDebug
```
The debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Release APK Build
```bash
gradle :app:assembleRelease
```
The release APK will be generated at:
`app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if keystore is configured).

### Android App Bundle (AAB) for Google Play
```bash
gradle :app:bundleRelease
```
The output bundle will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

---

## 6. API & Environment Configuration

### Secrets & Environment Variables (`.env`)
NIVORA uses the **Secrets Gradle Plugin**. Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Configuration variables:
```properties
# Optional Gemini API key for remote AI capabilities
GEMINI_API_KEY=your_gemini_api_key_here
```
*Note: If no API key is provided, NIVORA automatically switches to its offline, on-device local heuristic AI engine.*

---

## 7. ProGuard, R8 & Release Optimization

Release builds have R8 and ProGuard rules configured in `app/proguard-rules.pro` to:
- Preserve `@android.webkit.JavascriptInterface` methods for WebView bridge communication.
- Preserve Room Database entities, schemas, and DAOs.
- Optimize bytecode and shrink unused dependencies while maintaining full reflection safety.

---

## 8. Privacy & Security Principles

1. **Zero Silent Telemetry**: No third-party analytics SDKs.
2. **Default-Off Sync**: Sync never runs in the background without explicit opt-in.
3. **Local-First Data**: Bookmarks, history, notes, and preferences live in local SQLite databases protected by Android application sandboxing and hardware-backed biometrics.
4. **Least-Privilege Permissions**: Runtime permissions (Camera, Microphone, Location, Notifications) are requested on demand with graceful fallbacks.

---

## 9. Attribution & Branding

- **Application**: NIVORA
- **Tagline**: Search Freely. Browse Privately.
- **Creator**: Created by Bhaskar Gautam
- **Developer Credit**: Developed by Bhaskar
