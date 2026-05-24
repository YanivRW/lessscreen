# LessScreen

A native Android app that tracks how much time you spend in specific social
media apps. Phase 1: local-only, single user, today's usage.

## Tech

- Kotlin + Jetpack Compose
- `UsageStatsManager` for screen-time data
- Min SDK 26 (Android 8.0)

## Get the APK

Every push to `main` triggers a GitHub Actions build. To download:

1. Go to the **Actions** tab in this repo.
2. Click the latest successful **Build Android APK** run.
3. Scroll to **Artifacts** and download `LessScreen-debug-apk`.
4. Unzip to get `app-debug.apk`.
5. Transfer to your phone (USB, Drive, email) and tap it. Approve "install
   from unknown source" if prompted.

## First-run permission

On launch the app will ask you to grant **Usage Access** in system Settings.
Tap "Open Settings", find LessScreen in the list, toggle it on, hit back.
The screen will refresh and show today's usage.

## Build locally

If you have Android Studio installed, open the project and hit Run.
