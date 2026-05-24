# LessScreen

Track how little time you spend in algorithm-based apps (Instagram, TikTok,
YouTube, Facebook, X, LinkedIn). Compete with friends — lowest wins.

## Tech

- Native Android (Kotlin + Jetpack Compose), min SDK 26
- `UsageStatsManager` for screen-time data
- Supabase (Postgres + Auth) for accounts, friends, and leaderboard

## One-time setup (you, the owner)

1. Create a Supabase project at https://supabase.com — free tier is fine.
2. Open **SQL Editor** → New query → paste the contents of
   [`supabase/schema.sql`](supabase/schema.sql) → Run. Sets up tables, RLS,
   and the auto-profile trigger.
3. (Optional, recommended for v0.2.0) **Authentication → Providers → Email** →
   turn **Confirm email** off. Otherwise users have to click a confirmation link.
4. **Project Settings → API** → copy **Project URL** and **anon public** key.
5. Paste both into [`app/src/main/kotlin/com/yanivrw/lessscreen/SupabaseConfig.kt`](app/src/main/kotlin/com/yanivrw/lessscreen/SupabaseConfig.kt)
   and commit + push. The GitHub Actions build will produce a new APK.

## Get the APK

Every push to `main` triggers a GitHub Actions build. Latest release:
https://github.com/YanivRW/lessscreen/releases

Download `app-debug.apk`, transfer to your Android phone, install, grant
**Usage Access** in Settings.

## Friends / leaderboard

- After sign-up, your unique 6-char invite code appears on the **Friends** tab.
- Share it (Copy or Share button). Anyone who pastes it into their **Friends → Add
  by code** is mutually added — both of you see each other's daily totals.
- The **Scoreboard** tab lists you + friends, ascending by today's total minutes.

## Roadmap

- v0.2: auth, friends, leaderboard, daily upload  ← *current*
- v0.3: 7-day chart, per-app breakdown for friends, streaks
- v0.4: Google sign-in, push notifications, weekly competitions
