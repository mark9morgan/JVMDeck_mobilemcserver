# JvmDeck - Dynamic JRE Download Architecture & Documentation

This document records the technical architecture of the **Dynamic JRE Download** feature in JvmDeck to ensure context persistence across development sessions.

---

## 1. Overview & Purpose
To comply with Google Play's strict **150 MB App Bundle (.aab)** size limit, heavy Java Runtime Environments (JREs) are removed from the local app assets (`assets/`) and downloaded dynamically on first launch (or when a specific server version requires a specific Java runtime).

---

## 2. Server File Structure (`server_test/`)
For local testing and production hosting (e.g., GitHub Releases or static web server), each JRE version is packed into its own individual `.zip` archive:

- `server_test/jre.zip` (Java 8 runtime)
- `server_test/jre-17.zip` (Java 17 runtime)
- `server_test/jre-21.zip` (Java 21 runtime)
- `server_test/jre-25.zip` (Java 25 runtime)

---

## 3. Local Testing Workflow
1. **Start Local Server:**
   ```bash
   python server_test/run.py
   ```
   This spins up a Python HTTP server on port `8000` inside `server_test/`.
2. **App Configuration:**
   - In the Android Emulator: Enter `http://10.0.2.2:8000`
   - On a Real Device (same Wi-Fi): Enter `http://<PC_LOCAL_IP>:8000`
3. **Network Security:**
   - `android:usesCleartextTraffic="true"` is enabled in `AndroidManifest.xml` to allow local HTTP communication.

---

## 4. Key Code Components
- **`JreManager.kt`**: Manages installation checks (`isInstalled()`), download streams (`downloadAndInstall()`), symlinks, and archive unpacking (`unpackTarXzStream()`).
- **`SetupJreScreen.kt`**: A Jetpack Compose screen that guides the user, accepts the base server URL, shows real-time download progress (%), and handles installation.
- **`MainActivity.kt`**: Intercepts app startup, checks if JRE is installed (`JreManager.forJavaVersion(context, version).isInstalled()`), and displays `SetupJreScreen` if missing.
