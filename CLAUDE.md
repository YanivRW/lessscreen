# LessScreen

Native Android app that tracks time spent in social media apps and lets users
compete with friends on who uses them **least** (a steps counter, inverted —
lower minutes wins). Evolving from a passive tracker into a competitive social
digital-detox app that can actively **block** apps. See product direction below.

<!-- Maintainer note: keep this file under ~200 lines. Deep/rarely-needed detail
     lives in .claude/rules/. Update the "Status" section when features ship. -->

## Tech stack

- **Language:** Kotlin · **UI:** Jetpack Compose
- **Screen-time data:** `UsageStatsManager`
- **Backend/Auth/DB:** Supabase (Postgres + GoTrue)
- **HTTP:** Ktor (OkHttp engine) · **Serialization:** kotlinx.serialization
- **Navigation:** Jetpack Navigation Compose
- **Google Sign-In:** Android Credential Manager (`androidx.credentials`)
- **Build:** Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21 · **Min SDK:** 26 (Android 8.0)
- **CI/APK:** GitHub Actions → release artifact

## Repository

- **GitHub:** https://github.com/YanivRW/lessscreen (public)
- **Local path:** `~/lessscreen` · **Default branch:** `main` (every push builds)
- **Package:** `com.yanivrw.lessscreen` (never change)

## Project structure

```
app/src/main/
├── AndroidManifest.xml              # PACKAGE_USAGE_STATS + INTERNET permissions
├── kotlin/com/yanivrw/lessscreen/
│   ├── MainActivity.kt              # Entry point, hosts Compose root
│   ├── SupabaseClient.kt            # Supabase singleton (lazy init)
│   ├── SupabaseConfig.kt            # URL + anon key (hardcoded, safe for client)
│   ├── GoogleConfig.kt              # WEB_CLIENT_ID for Google Sign-In
│   ├── data/
│   │   ├── AppUsage.kt              # AppUsage data class + TRACKED_PACKAGES
│   │   ├── UsageRepository.kt       # Reads UsageStatsManager, uploads daily total
│   │   ├── AuthRepository.kt        # Sign in/up/out, Google Sign-In, myProfile()
│   │   ├── FriendsRepository.kt     # Friends, leaderboard, 7-day history
│   │   └── models/Models.kt         # Profile, DailyUsageRow, FriendRow, etc.
│   ├── permission/UsagePermission.kt# hasUsageAccess(), openUsageAccessSettings()
│   └── ui/
│       ├── AppNavigation.kt         # AppRoot (auth gate), SignedInApp (nav + top bar)
│       └── screens/                 # SignInScreen, ResultsScreen,
│                                    #   LeaderboardScreen, FriendsScreen
├── res/values/                      # strings.xml, themes.xml (dark, no action bar)
supabase/schema.sql                  # Full DB schema — run in Supabase SQL Editor
.github/workflows/build-apk.yml      # CI: stable keystore + assembleDebug + upload
```

Conventions: repositories under `data/`, screens under `ui/screens/`,
models in `data/models/Models.kt`. New features follow this layout.

## Critical guardrails (do not break)

- **Never regenerate the signing keystore.** It lives at `keystore/lessscreen.jks`
  (gitignored), backed up in 1Password, and mirrored to GitHub secrets
  `KEYSTORE_BASE64` + `KEYSTORE_PASSWORD`. Local builds and CI both consume it
  via `keystore.properties` → `signingConfigs["lessscreen"]` in
  `app/build.gradle.kts`. Losing it = SHA-1 changes = Google Sign-In breaks and
  Play Store updates become impossible.
  Stable SHA-1: `C3:3E:38:45:24:5B:C7:B4:10:6D:06:EB:83:B2:D7:0B:68:38:FF:AE`
- **Never change the package** `com.yanivrw.lessscreen`.
- **No regressions** to working features (today screen, leaderboard, friends,
  email auth) when adding new ones.
- Be honest about Android limitations. App blocking fights the OS — if something
  isn't reliably achievable without root or Play Store review issues, say so up
  front rather than shipping flaky behavior.

## Build & release

Every push to `main` triggers `.github/workflows/build-apk.yml`, which restores
the stable keystore, runs `./gradlew :app:assembleDebug`, and uploads the APK as
an artifact.

Download a build:
```bash
gh run download <run-id> -n LessScreen-debug-apk -D apk
```

Release manually:
```bash
gh release create vX.Y.Z apk/app-debug.apk --title "..." --notes "..."
```

QR for any release URL: `https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=<url-encoded-apk-url>`

## Supabase

- **Project URL:** `https://lhrkbczwlwyefluxbpxt.supabase.co`
- **Project ID:** `lhrkbczwlwyefluxbpxt` · **Anon key:** in `SupabaseConfig.kt`
- **Tables (public schema):** `profiles` (one per user, has auto-generated 6-char
  `invite_code`), `friends` (two rows per friendship, mutual auto-accept),
  `daily_usage` (one row per user per day, `total_minutes`)
- **RLS:** on for all three. Friends can read each other's `daily_usage`;
  profiles readable by all authed users (invite-code lookup).
- **Trigger:** `on_auth_user_created` auto-creates a profile + invite code on signup.
- **Email confirmation:** OFF (signup = immediate sign-in).
- Schema changes go in `supabase/schema.sql`. See `.claude/rules/supabase-detail.md`.

## Tracked apps (hardcoded in `AppUsage.kt`)

Instagram, TikTok (`com.zhiliaoapp.musically`), YouTube, Facebook
(`com.facebook.katana`), X (`com.twitter.android`), LinkedIn. Making this list
customizable is a planned feature — see product direction.

## Status

**Working:** email sign-up/sign-in + session persistence · today screen
(per-app usage, 60s auto-refresh, midnight reset, uploads to Supabase, permission
prompt) · scoreboard (7-day line chart, today's ranking with medals) · friends
(invite code, copy/share, add by code, mutual friendship, list).

**In progress:** Google Sign-In — SHA-1 mismatch fixed (2026-06-01, new keystore
+ new fingerprint registered in Google Cloud). Now blocked on Supabase audience
mismatch: ID token's `aud` is the Web client ID, but Supabase →
Authentication → Providers → Google has a different Client ID configured. Next
step: set "Authorized Client IDs" in Supabase to
`846082142354-t0uud57mv19lt618nmto47jent3hut81.apps.googleusercontent.com`
and paste the Web client's secret. Full history in
`.claude/rules/google-signin-detail.md`. Email/password remains the working
fallback.

**Open (next session):** "Database error saving new user" when signing up a
second account from the same phone with a different email. Likely the
`on_auth_user_created` trigger throwing on a unique-constraint violation in
`profiles.email` or `profiles.invite_code`. Need a Supabase → Logs → Postgres
Logs capture at the time of failure to identify the constraint. Also check that
the prior user wasn't still signed in when retrying — `AuthRepository.signUp`
doesn't call `signOut` first.

**Known quirks:** scoreboard chart needs ≥1 day of data per user to draw a line ·
`UsageStatsManager` only reports apps the user has actually opened (new installs
show 0 until opened) · debug APK shows "unknown source" warning (expected).

## Product direction (where this is heading)

Three pillars guide new work. The detailed session-by-session steering lives in
the prompt I paste in, but the strategy is stable:

1. **Core blocker (new capability).** Beyond *measuring* usage, actively
   *restrict* apps (and later in-app features) during user-defined hours. This is
   the biggest architectural change vs. today's read-only model — needs an
   `AccessibilityService` / overlay approach. Plan before building.
2. **Social/gamified layer (extend what works).** Keep building friends +
   leaderboard. Frame around "time spent **off** the apps." Priority: rock-solid
   syncing and near-real-time standings (competitors are laggy — this is a
   differentiator).
3. **Ultimate accountability (future phase, design for it now).** Let a user hand
   locking privileges to a trusted friend who sets a secret passcode and can
   remotely lock an app. Don't build yet, but keep the data model and blocker
   architecture flexible enough to slot it in without a rewrite.

**Planned features** (many feed the direction above): per-app daily/weekly goals
and a customizable tracked-apps list (foundational to scheduled blocking) ·
streaks (reinforce the detox loop) · per-app friend breakdown on the leaderboard ·
7-day chart on the today screen · push notifications · weekly summaries ·
display-name editing (DB field exists, no UI yet).

## End-of-session habit

When a feature ships, update the **Status** section above and trim it from the
product-direction "planned" list. If work is mid-flight, jot where we stopped so
the next session resumes cleanly.
