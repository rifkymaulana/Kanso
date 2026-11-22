# Kanso — Personal Offline-First Android Super App

Kanso is a modular, 100% offline-first Android super app built with Kotlin, Jetpack Compose (Material 3), Hilt, Room (with FTS4 search), DataStore, WorkManager, CameraX + ZXing, BiometricPrompt + Android Keystore, and Jetpack Glance.

## Modules

- **Today (`:feature:today`)**: Daily command center with attention alerts (due bills, expiring IDs/documents, overdue tasks), monthly spend pulse, focus tasks, 1-tap pinned snippets, recent notes, and habits/hydration pulse (`+250ml`).
- **Money (`:feature:money`)**: Fast expense & income logging, monthly category breakdown, overall & per-category budgets, recurring bills with 1-tap mark-paid, and IOUs ("Owes Me / I Owe").
- **Second Brain (`:feature:brain`)**: FTS4-indexed notes, prioritized tasks/checklists, and 1-tap copy snippets with privacy masking.
- **Habits & Health (`:feature:habits`)**: Daily habit streaks, interactive 30-day completion heatmap, hydration/sleep/weight/mood/workout check-ins, and 7-day averages.
- **Life Admin & Private Vault (`:feature:lifeadmin`)**: Expiry alerts (IDs, passport, vehicle tax, warranties), asset maintenance logs, and a Biometric/PIN-gated private vault backed by Android Keystore AES-256-GCM + `FLAG_SECURE`.
- **Pocket Utilities (`:feature:utilities`)**: Offline QR code generator (including Wi-Fi QR) & CameraX scanner, unit & offline currency converter, Pomodoro focus timer, and date countdowns.
- **Android OS Integrations (`:app`)**: Jetpack Glance home widget (`KansoTodayWidget`), long-press launcher shortcuts, Quick Settings tile (`Kanso Capture`), WorkManager local daily reminders, and passphrase-encrypted `.kanso.enc` backup/restore (`:core:backup`).

## Local Development & Testing

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
.\gradlew.bat :core:model:test :core:security:testDebugUnitTest :core:backup:testDebugUnitTest :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

## Downloading the Release APK & In-App Auto-Updates

1. Open the repository's **Releases** page on GitHub to install the initial `Kanso-vX.Y.Z-release.apk`.
2. Because `rifkymaulana/Kanso` is public, future updates work automatically inside the app without a token — Kanso checks GitHub Releases automatically, verifies the `.sha256` checksum, and launches the Android installer. You can also configure an optional read-only Fine-Grained Personal Access Token under **Settings & Backup → App Updates → GitHub Access Token (Optional)** (stored encrypted via Android Keystore) if you hit unauthenticated GitHub API rate limits or fork into a private repository.

## Cutting a New Signed Release (CI/CD)

The workflow [`.github/workflows/release.yml`](.github/workflows/release.yml) runs all unit tests, signs `app-release.apk`, verifies it with `apksigner`, generates a `.sha256` checksum, and publishes a GitHub Release whenever you:

- Push a version tag (`git tag v1.0.0 && git push origin v1.0.0`), **or**
- Go to **Actions → Release Signed APK → Run workflow** and enter the version name (e.g. `1.0.0`).

### Required GitHub Repository Secrets

Configure these under **Settings → Secrets and variables → Actions**:

| Secret | Description |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded `.jks` release keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias inside the keystore |
| `ANDROID_KEY_PASSWORD` | Key password |
