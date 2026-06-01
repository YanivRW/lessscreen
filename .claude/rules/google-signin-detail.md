# Google Sign-In — detailed config & debugging history

> Active debugging. Email/password still works as fallback. This file captures
> the full state so we don't repeat dead ends.

## Current status

**Two fixes in, one to go.**

1. ✅ **SHA-1 mismatch fixed (2026-06-01).** Previously local debug builds and
   CI builds used different keystores; the SHA-1 registered in Google Cloud
   matched only one. Migrated to a single project-owned keystore at
   `keystore/lessscreen.jks` used by both `debug` and `release`. New SHA-1
   `C3:3E:38:45:24:5B:C7:B4:10:6D:06:EB:83:B2:D7:0B:68:38:FF:AE` registered in
   Google Cloud Console (Android OAuth client, package `com.yanivrw.lessscreen`).
   The original "No credentials available" error is gone — the credential picker
   now appears, account selection succeeds, and Google returns a valid ID token.

2. ⏳ **Supabase audience mismatch (current blocker).** The ID token from Google
   has `aud=846082142354-t0uud57mv19lt618nmto47jent3hut81.apps.googleusercontent.com`
   (the Web client ID, same as `GoogleConfig.WEB_CLIENT_ID`). Supabase rejects
   with:

   ```
   Bad Request (Unacceptable audience in id_token:
   [846082142354-t0uud57mv19lt618nmto47jent3hut81.apps.googleusercontent.com])
   URL: https://lhrkbczwlwyefluxbpxt.supabase.co/auth/v1/token?grant_type=id_token
   ```

   Means Supabase → Authentication → Providers → Google is configured with a
   different Client ID than the app sends.

## What's already been tried (don't repeat)

- `GetGoogleIdOption` with `filterByAuthorizedAccounts = false` → failed (was
  SHA-1 issue, not credential-option issue)
- `GetSignInWithGoogleOption` → same error (same root cause)
- Added a SHA-256 nonce → no change (this part is correct, kept in code)
- Added a test user on the OAuth consent screen → no change (test user wasn't
  the issue)
- Earlier attempts used the **wrong client ID** (Android client instead of Web
  client). v0.4.1 corrected this — the Web client ID in `GoogleConfig.kt` is the
  right one to send.

## How the code is wired

`GoogleConfig.WEB_CLIENT_ID` = the **Web application** client ID (not the Android
client). `AuthRepository.signInWithGoogle()` uses Android Credential Manager
(`GetSignInWithGoogleOption`) with a SHA-256 nonce, then exchanges the Google ID
token with Supabase via `supabase.auth.signInWith(IDToken)`.

## Google Cloud Console (project: LessScreen)

- **Android client** registered with package `com.yanivrw.lessscreen` and
  SHA-1 `C3:3E:38:45:24:5B:C7:B4:10:6D:06:EB:83:B2:D7:0B:68:38:FF:AE` (the
  stable project keystore — see CLAUDE.md). This is the single fingerprint used
  by both local debug builds (via `keystore/lessscreen.jks`) and CI builds.
- **Web client** is `846082142354-t0uud57mv19lt618nmto47jent3hut81.apps.googleusercontent.com`,
  referenced in `GoogleConfig.kt`. Its **client secret** lives in Supabase →
  Authentication → Providers → Google (never commit it).
- OAuth consent screen: External, Testing mode (not published). Test users
  added under the consent screen.

## Next things to try (when we return to this)

1. **Fix Supabase Google provider config (current step).**
   Open https://supabase.com/dashboard/project/lhrkbczwlwyefluxbpxt/auth/providers
   → Google. In **Authorized Client IDs** (comma-separated field), set:
   ```
   846082142354-t0uud57mv19lt618nmto47jent3hut81.apps.googleusercontent.com
   ```
   And in **Client Secret**, paste the Web client's secret from Google Cloud
   Console → Credentials → Web client → "Client secret" → Show. Save.
   Then kill+reopen the app and retry "Continue with Google."

2. If sign-in succeeds but the `on_auth_user_created` trigger throws, see
   the separate signup-error debugging in `.claude/rules/supabase-detail.md`.

3. If it still fails after step 1, fall back to the legacy `GoogleSignInClient`
   from `com.google.android.gms:play-services-auth:21.2.0` — more reliable than
   Credential Manager for apps not distributed via the Play Store. (Unlikely to
   be needed now that the SHA-1 issue is solved.)
