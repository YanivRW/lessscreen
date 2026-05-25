# Google Sign-In — detailed config & debugging history

> Deferred feature. Email/password is the working fallback. Don't spend time here
> unless explicitly asked. This file captures the full state so we don't repeat
> dead ends.

## Current status

Broken. Error: "No credentials available" from Android Credential Manager.
v0.4.1 pushed the corrected **Web client ID** but was never confirmed working —
it was released before testing.

## What's already been tried (don't repeat)

- `GetGoogleIdOption` with `filterByAuthorizedAccounts = false` → failed
- `GetSignInWithGoogleOption` → same error
- Added a SHA-256 nonce → no change
- Added a test user on the OAuth consent screen → no change
- Earlier attempts used the **wrong client ID** (Android client instead of Web
  client). v0.4.1 corrected this.

## How the code is wired

`GoogleConfig.WEB_CLIENT_ID` = the **Web application** client ID (not the Android
client). `AuthRepository.signInWithGoogle()` uses Android Credential Manager
(`GetSignInWithGoogleOption`) with a SHA-256 nonce, then exchanges the Google ID
token with Supabase via `supabase.auth.signInWith(IDToken)`.

## Google Cloud Console (project: LessScreen)

- **Android client** registered with package `com.yanivrw.lessscreen` and the
  SHA-1 from the GitHub Actions build log ("Set up signing keystore" step → SHA1).
- **Web client** is the one referenced in code; its secret is stored in
  Supabase → Authentication → Providers → Google (never commit it).
- OAuth consent screen: External, Testing mode (not published). Test users
  added under the consent screen.

## Next things to try (when we return to this)

1. Confirm the Supabase Google provider has the Web Client ID **and** secret saved.
2. Test v0.4.1 on a real device.
3. If it still fails, switch to the legacy `GoogleSignInClient` from
   `com.google.android.gms:play-services-auth:21.2.0` — more reliable than
   Credential Manager for apps not distributed via the Play Store.
