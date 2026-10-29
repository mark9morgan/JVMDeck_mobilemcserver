# JvmDeck — Mobile JVM & Server Host for Android

**JvmDeck** is an advanced Android application built with Kotlin and Jetpack Compose that allows users to run Java servers (such as Minecraft servers) natively on Android devices (Android 14+, `arm64-v8a` architecture) via a specialized Android-compatible OpenJDK build (bionic/aarch64).

---

## Key Features

- **Native Java Runtime Execution:** Runs `java` as a persistent background process managed by a Foreground Service (`ServerService`) with partial wake locks.
- **Multi-Version JRE Support:** Supports Java 8, 17, 21, and 25 (via PojavLauncherTeam / android-openjdk-build-multiarch builds).
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
- **Compression & Archives:** Apache Commons Compress (`tar.xz`, `xz`).
- **Native Development:** C / CMake (Bionic `mallopt` & `prctl` interop, `libnotbi.so`).

---

## Environment Requirements

- **Device Architecture:** `arm64-v8a` only (64-bit ARM). 32-bit ARM and x86/x64 emulators are not supported for native JVM execution.
- **OS Version:** Android 14 (API 34) or higher.
- **Development IDE:** Android Studio (Ladybug / Koala or newer with AGP 8+ and Kotlin 2.x).

---

## Setup & Installation Instructions

1. **Clone or Open Project:**
   Open Android Studio and select `Open an Existing Project`, pointing to the `JvmDeck` root folder. Gradle wrapper will download automatically.

2. **Required JRE Binaries & Archives (Manual Placement):**
   Due to large file sizes and platform licensing, JRE archives and native binaries must be placed manually before building:
   - **Native Java binary:** `app/src/main/jniLibs/arm64-v8a/libjava.so` (renamed from `bin/java` from aarch64 bionic OpenJDK build).
   - **JRE Asset archives:** Place tar.xz archives (`universal.tar.xz`, `bin-arm64.tar.xz`, `jre-21/`, etc.) into `app/src/main/assets/`.
   *(Source: PojavLauncherTeam / android-openjdk-build-multiarch builds).*

3. **Build & Run:**
   Connect an `arm64-v8a` Android 14+ device, and run the `:app:assembleDebug` build or click **Run** in Android Studio.

---

## Verification Workflow

1. Open the app and navigate to **Settings** -> **Diag** to verify that `libjava.so`, `jvm.cfg`, and `libjvm.so` are correctly detected.
2. Run a simple test or check `java -version`.
3. Create a test server (e.g., Paper 1.20.4), let it auto-download `server.jar`, and start it.
4. Check the **Monitoring** tab to review real-time CPU and RAM performance charts.

---

## Known Limitations & Troubleshooting

- **Symlinks in Tar.xz:** Standard `ZipInputStream` / `TarArchiveInputStream` handles symbolic links where supported, but ensure your JRE tarballs are correctly structured.
- **16 KB Page Size:** On Android 15+ devices with 16 KB page size support, ensure all native `.so` binaries are properly page-aligned.
- **Battery Optimization:** For reliable background server execution without being killed by the OS, grant permission to ignore battery optimizations when prompted.

---

## License

This project is open-source and distributed under the [MIT License](LICENSE).

*Note on Runtimes & Binaries:* Pre-compiled OpenJDK / JRE binaries and native libraries referenced or utilized by the app belong to their respective copyright holders (e.g., PojavLauncherTeam / OpenJDK contributors) and are distributed under their respective terms (such as GPLv2 with Classpath Exception).
