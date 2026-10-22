# ⚡ NEXUS VAULT // Gamer Media & Capture Vault

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="Nexus Vault Icon"/>
</p>

<p align="center">
  <b>A 120Hz-ready, high-refresh-rate Gamer Media & Capture Vault built with Kotlin and Jetpack Compose.</b><br>
  Zero-latency indexing, procedural Canvas HUD telemetry, physics-driven gestures, and cyberpunk aesthetics.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.06-4285F4?style=for-the-badge&logo=android&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Material%203-Dark%20HUD-00F0FF?style=for-the-badge" alt="Material 3" />
  <img src="https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-3DDC84?style=for-the-badge&logo=android" alt="Target SDK 34" />
</p>

---

## 🎮 Highlights & Features

### 1. 🌌 3-Stage Parallax Onboarding
* Driven by `HorizontalPager` with real-time `pageOffsetFraction` math.
* **3D Perspective Tilts**: Dynamically applies 3D `rotationY`, depth scaling (`scaleX`/`scaleY`), and alpha interpolation.
* **Procedural Canvas HUD Graphics**: Canvas rendering of rotating dashed telemetry rings and morphing $n$-sided geometric HUD nodes ($N=4, 5, 6$).
* **Spring-Physics Indicators**: Morphing active page pill indicators using bouncy spring transitions.

### 2. ⚡ Procedural Cyber-Loader & Boot Sequence
* 120Hz-tuned procedural hexagonal spinner with dual counter-rotating neon arcs and pulsing telemetry core.
* Realistic mock NVMe shader cache initialization and 4K capture index sequence.

### 3. 🍱 Bento & Staggered Capture Grid
* **Staggered Layout**: `LazyVerticalStaggeredGrid` with instant 2-column / 3-column density switcher.
* **Spring Press Feedback**: Card touch targets use `Spring.DampingRatioMediumBouncy` for responsive tactile feedback.
* **Live Badges**: Overlay tags for real-time framerate (`120 FPS`, `240 FPS`), clip duration (`▶ 01:15`), and HDR status.
* **Category Filters**: Instant chips for `ALL`, `DVR CLIPS`, `4K RAW`, `TROPHIES`, and `STARRED`.
* **Search Filter**: Real-time title and game name query filtering.

### 4. 🔍 Gamer HUD Lightbox & Gesture Controls
* **Pinch-to-Zoom & Pan**: Full 2D transform gestures supporting $1\times$ to $4\times$ magnification and double-tap reset.
* **Physics Drag-to-Dismiss**: Interactive vertical swipe-to-dismiss with spring rebound and dynamic background backdrop opacity scaling.
* **Live Telemetry HUD**: Overlay banner reporting resolution, framerate, and GPU thermals (e.g., `3840x2160 • 120 FPS • GPU 64°C`).
* **HUD Toggle**: Single-tap anywhere on screen fades the HUD in/out for clean screenshot viewing.

---

## 🎨 Cyberpunk Palette

| Token | Hex | Role |
| :--- | :--- | :--- |
| `CyberBg` | `#07080C` | Deep obsidian backdrop |
| `CyberSurface` | `#12151F` | Dark metallic card container |
| `CyberNeonCyan` | `#00F0FF` | Primary action & accent glow |
| `CyberNeonMagenta` | `#FF0055` | Secondary accent & recording badge |
| `CyberAcidGreen` | `#00FF66` | Framerate & status telemetry |
| `CyberAmber` | `#FFB800` | Thermal & sensor warnings |
| `CyberTextPrimary` | `#F2F5FA` | High-contrast body typography |
| `CyberTextSecondary` | `#8A94A6` | Monospace telemetry captions |

---

## 📁 Project Architecture

```text
nifty-goodall/
├── .github/
│   └── workflows/
│       └── build-apk.yml           # Automated GitHub Actions APK build workflow
├── app/
│   ├── build.gradle.kts            # App module dependencies (Compose, Coil, M3)
│   ├── proguard-rules.pro          # Release optimization rules
│   └── src/main/
│       ├── AndroidManifest.xml     # App configuration & INTERNET permission
│       ├── java/com/example/nexusvault/
│       │   └── MainActivity.kt     # Single-file Kotlin + Jetpack Compose app
│       └── res/
│           ├── drawable/           # Adaptive cyber vector icons
│           ├── mipmap-anydpi-v26/  # Adaptive launcher icons
│           └── values/             # App name, strings, and theme definitions
├── gradle/wrapper/
│   └── gradle-wrapper.properties   # Gradle 8.7 distribution
├── build.gradle.kts                # Root project build script (AGP 8.4.2, Kotlin 2.0.0)
├── settings.gradle.kts             # Dependency & plugin repositories
├── gradle.properties               # JVM allocation & AndroidX settings
├── gradlew                         # Unix Gradle wrapper executable
└── README.md                       # Project documentation
```

---

## 🛠️ Tech Stack & Dependencies

* **Language**: [Kotlin 2.0.0](https://kotlinlang.org/)
* **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Compose BOM `2024.06.00`)
* **Design System**: [Material 3](https://m3.material.io/)
* **Image Loading**: [Coil Compose 2.6.0](https://coil-kt.github.io/coil/compose/)
* **Icons**: `androidx.compose.material:material-icons-extended`
* **Build System**: Gradle 8.7 + Android Gradle Plugin 8.4.2
* **Target / Compile SDK**: Android 14 (API 34)
* **Minimum SDK**: Android 8.0 (API 26)

---

## 🚀 Building the App

### Option A: Android Studio (Recommended)

1. Open **Android Studio** (Hedgehog / Iguana / Jellyfish or newer).
2. Select **Open** and choose this project directory.
3. Wait for Gradle sync to complete.
4. Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK will be created at:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Option B: Command Line (Gradle)

Ensure you have **JDK 17+** and the **Android SDK** configured in your environment (`ANDROID_HOME`):

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Option C: Cloud CI/CD (GitHub Actions)

This repository includes a ready-to-run GitHub Actions workflow in [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

1. Push this project to your GitHub repository:
   ```bash
   git add .
   git commit -m "Initialize Nexus Vault"
   git push origin main
   ```
2. In your GitHub repository, open the **Actions** tab.
3. Once the **Build Nexus Vault APK** workflow completes, download the compiled APK artifact (`nexus-vault-debug-apk`).

---

## 📄 License

This project is licensed under the MIT License - feel free to customize and integrate into your own gaming gear and capture suites!
