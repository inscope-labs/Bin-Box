# Task Report: Top Bar Restructure, Persistent Terminal Notification, and App-Wide BinBox Rename

**Timestamp:** 2026-09-02T21:03:39Z  
**Task Slug:** top-bar-restructure-persistent-notification  

---

## 1. What Was Asked
The user requested the first step of a major UI restructuring focusing on clarity, minimal cognitive overhead, and calm aesthetics:
1. **Top Bar (Immersive Header Bar) Restructure:**
   - Convert the Settings icon button in the far right from round to rectangular.
   - Remove the app icon in the far left and replace it with a back arrow button in a similar rectangle.
   - Connect the back arrow button to activate the persistent notification required for terminal applications (pattern modeled on Termux foreground service).
   - Capitalize and position the connection pulse info such that the shell name is on the left-hand side of the color indicator dot, and the status "Connected" (or "Standby") is on the right-hand side.
   - Align both the app name and the connection pulse horizontally and vertically in the center.
   - Maintain the Top Bar height without increasing its size.
2. **App-Wide Rename:**
   - Change the app name from "Bin Box" to "BinBox" app-wide.

---

## 2. Version Increment Assessment (Rule 2)
- **Probability Score:** 95 / 100 (Direct user-facing UI overhaul, foreground service addition, manifest permission updates, app-wide name change, and build configuration adjustments).
- **Action Taken:** Score > 75; incremented `versionCode` by 1 (32 -> 33) and `debugCode` by 1 (0032 -> 0033) in `version.properties`.

---

## 3. Prior Logging Gaps (Rule 3 & 3.1)
- **PRIOR LOGGING GAPS FOUND:** none (`issues/pending/` does not exist or contains no entries).
- **Logging Standard Adherence:** Adequate logging via `BinBoxLogger` was implemented across all entry points, lifecycle hooks, and user trigger branches in `TerminalForegroundService.kt`, `MainActivity.kt`, and `BinBoxApp.kt`.

---

## 4. Single-Responsibility & File Size Compliance (Rule 4 & 4.1)
- `app/src/main/java/com/inscopelabs/abx/binbox/ui/BinBoxApp.kt` (UI role): 406 lines (limit: 1000 lines) — COMPLIANT.
- `app/src/main/java/com/inscopelabs/abx/binbox/MainActivity.kt` (Logic / Orchestrator role): 80 lines (limit: 500 lines) — COMPLIANT.
- `app/src/main/java/com/inscopelabs/abx/binbox/terminal/service/TerminalForegroundService.kt` (Logic / Module role): 186 lines (limit: 500 lines) — COMPLIANT.

---

## 5. What Was Actually Changed

### A. Terminal Foreground Service & Persistent Notification
- **`app/src/main/java/com/inscopelabs/abx/binbox/terminal/service/TerminalForegroundService.kt`** (New File):
  - Created Android Foreground Service implementing a persistent notification pattern identical to Termux.
  - Configured notification channel `binbox_terminal_session` with low importance to avoid intrusive audio/vibration while remaining persistent.
  - Implemented `SPECIAL_USE` foreground service type for Android 14+ compatibility.
  - Configured `PendingIntent` for reopening `MainActivity` on notification click, and an explicit action to terminate/stop the service.
  - Added safe handling of `WakeLock` to prevent CPU throttling while terminal sessions execute in background.
- **`app/src/main/AndroidManifest.xml`**:
  - Declared permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, and `WAKE_LOCK`.
  - Registered `TerminalForegroundService` with `android:foregroundServiceType="specialUse"` and property `androidx.core.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
- **`app/src/main/java/com/inscopelabs/abx/binbox/MainActivity.kt`**:
  - Added runtime request launcher for `POST_NOTIFICATIONS` on Android 13+ (API 33+).

### B. Top Bar UI Restructure
- **`app/src/main/java/com/inscopelabs/abx/binbox/ui/BinBoxApp.kt`**:
  - Converted far-right Settings `IconButton` from circular (`CircleShape`) to rounded rectangle (`RoundedCornerShape(6.dp)`).
  - Replaced far-left terminal icon box with a rectangular (`RoundedCornerShape(6.dp)`) back arrow button using `Icons.AutoMirrored.Filled.ArrowBack`.
  - Connected the back arrow button to start `TerminalForegroundService` with the current session state and move the task to background (`moveTaskToBack(true)`).
  - Centered both the app name "BinBox" and the connection pulse horizontally and vertically using a weighted center column (`horizontalAlignment = Alignment.CenterHorizontally`, `verticalArrangement = Arrangement.Center`).
  - Restructured connection pulse row: capitalized shell name (`uppercase()`) on the left of the pulse indicator dot, and status (`strings.statusConnected.uppercase()`) on the right of the pulse indicator dot.
  - Preserved existing 40.dp button dimensions and 12.dp vertical padding to prevent increasing the Top Bar height.

### C. App-Wide "Bin Box" -> "BinBox" Rename
- **`app/src/main/res/values/strings.xml`**: Updated `app_name` from "Bin Box" to "BinBox".
- **`settings.gradle.kts`**: Updated `rootProject.name` from "Bin Box" to "BinBox".
- **`metadata.json`**: Updated platform `name` from "Bin Box" to "BinBox".
- **`version.properties`**: Incremented `versionCode` (33) and `debugCode` (0033).
- **`app/src/main/java/com/inscopelabs/abx/binbox/ui/i18n/AppStrings.kt`**: Updated `appName`, `statusStandby`, and `languageSectionSubtitle`.
- **All Translation Files** (`TranslationsEs.kt`, `TranslationsDe.kt`, `TranslationsFr.kt`, `TranslationsJa.kt`, `TranslationsKo.kt`, `TranslationsPt.kt`, `TranslationsRu.kt`, `TranslationsZh.kt`): Replaced all occurrences of "Bin Box" with "BinBox".
- **`TerminalBufferView.kt`**: Updated empty state header to "BinBox Terminal".
- **`SandboxDemoShellSession.kt`**: Updated help headers, deploy output, and fastfetch terminal string to "BinBox".
- **`OciEarlyStages.kt` & `OciVerificationDiagnostics.kt` & `OciProvisioningStages.kt`**: Replaced all user-facing guide text from "Bin Box" to "BinBox".
- **`BinBoxBackendApi.kt`, `BinBoxBackendClient.kt`, `BackendDiscoveryResponse.kt`, `WsTerminalFrame.kt`**: Updated internal documentation and comments.
- **`ExampleRobolectricTest.kt` & `GreetingScreenshotTest.kt`**: Updated unit and screenshot tests to verify "BinBox".

---

## 6. Commands Run and Results
1. `wc -l app/src/main/java/com/inscopelabs/abx/binbox/ui/BinBoxApp.kt ...`: Confirmed file lengths within compliance limits.
2. `grep -rIn --exclude-dir=build "Bin Box" app/ metadata.json settings.gradle.kts`: Confirmed zero occurrences of "Bin Box" remaining in the source code.
3. `compile_applet`: Build succeeded cleanly with no compiler warnings or errors.
4. `gradle :app:testDebugUnitTest`: All Robolectric unit tests passed with exit code 0.

---

## 7. Assumptions Made
- The persistent notification back arrow button minimizes the activity to background (`moveTaskToBack(true)`) while launching the foreground notification service, reflecting standard terminal multiplexer behavior in Android (Termux-like background execution).
- Notification channel priority is set to `IMPORTANCE_LOW` to provide persistent session status without unwanted audible chimes during terminal backgrounding.

---

## 8. Errors, Partial Failures, or Unverified Items
- None. Compilation succeeded, tests passed, and all structural requirements were verified.
