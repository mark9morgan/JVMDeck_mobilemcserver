# JvmDeck — Mobile JVM & Server Host for Android

**JvmDeck** is an advanced Android application built with Kotlin and Jetpack Compose that allows users to run Java servers (such as Minecraft servers) natively on Android devices (Android 14+, `arm64-v8a` architecture) via a specialized Android-compatible OpenJDK build (bionic/aarch64).

---

## Key Features

- **Native Java Runtime Execution:** Runs `java` (`libjava*.so`) as a persistent background process managed by a Foreground Service (`ServerService`) with partial wake locks.
- **Tracked Native Binaries (.so):** Native Java runtime binaries (`libjava.so`, `libjava_8.so`, `libjava_17.so`, `libjava_21.so`, `libjava_25.so`, `libunpack200.so`) and MTE bypass agent (`libnotbi.so`) are stored in `app/src/main/jniLibs/arm64-v8a/` and tracked directly in Git.
- **Multi-Version JRE Support:** Supports Java 8, 17, 21, and 25.
- **Dynamic JRE Download:** JRE runtime libraries and support files (`jre.zip`, `jre-17.zip`, `jre-21.zip`, `jre-25.zip`, `jre-new.zip`) are downloaded dynamically from GitHub Releases on first launch.
- **Server Management:** Create, configure, start, stop, and monitor multiple server instances with custom RAM allocations (`-Xmx`), port bindings, and `server.properties` generation.
- **Interactive Console:** Real-time `stdout`/`stderr` log streaming and command input to the running JVM process.
- **Mod & Plugin Integration:** Built-in integration with Modrinth API v2 to search, download, and install compatible plugins and mods.
- **File Manager:** Built-in file explorer to browse server directories, edit configuration files, and import custom `.jar` files or assets.
- **Real-Time Monitoring:** Live performance charts (CPU, RAM usage, process metrics) powered by `MonitoringRuntime` and `PerformanceChartCard`.
- **Process Management & Stale Killer:** Automatic cleanup of orphan/stale Java processes via `StaleProcessKiller`.
- **Localization Support:** Multi-language support managed via `LanguageManager` and `AppStrings`.
- **MTE & Pointer Tagging Bypass:** Includes a native C agent (`libnotbi.so`) to disable Bionic heap pointer tagging and MTE, preventing SIGABRT crashes on modern Android 14+ devices.

---

## Tech Stack & Architecture

- **UI:** Jetpack Compose, Material 3, Kotlin Coroutines, Custom Charts.
- **Background Processing:** Android Foreground Service (`specialUse` type), `ProcessBuilder`, `MonitoringRuntime`.
- **Compression & Archives:** Apache Commons Compress (`tar.xz`, `xz`), `java.util.zip.ZipFile`.
- **Native Development:** C / CMake (Bionic `mallopt` & `prctl` interop, `libnotbi.so`, `libjava*.so`).

---

## Environment Requirements

- **Device Architecture:** `arm64-v8a` only (64-bit ARM). 32-bit ARM and x86/x64 emulators are not supported for native JVM execution.
- **OS Version:** Android 14 (API 34) or higher.
- **Development IDE:** Android Studio (Ladybug / Koala or newer with AGP 8+ and Kotlin 2.x).

---

## Setup & Installation Instructions

1. **Clone or Open Project:**
   Open Android Studio and select `Open an Existing Project`, pointing to the `JvmDeck` root folder.

2. **Build & Run:**
   Connect an `arm64-v8a` Android 14+ device, and run the `:app:assembleDebug` build. The built APK will be output as **`JvmDeck-v0.1.apk`**.

---

## Verification Workflow

1. Open the app and complete the automated JRE setup/download screen.
2. Navigate to **Settings** -> **Diag** to verify that `libjava.so`, `jvm.cfg`, and `libjvm.so` are correctly detected.
3. Create a test server (e.g., Minecraft Vanilla or Paper), let it download `server.jar`, and start it.
4. Check the **Monitoring** tab to review real-time CPU and RAM performance charts.

---

## License

This project is open-source and distributed under the [MIT License](LICENSE).

*Note on Runtimes & Binaries:* Pre-compiled OpenJDK / JRE binaries and native libraries referenced or utilized by the app belong to their respective copyright holders (e.g., PojavLauncherTeam / OpenJDK contributors) and are distributed under their respective terms (such as GPLv2 with Classpath Exception).
