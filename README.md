# Nation Nums — Native Android App

Native Android application for Nation Nums, built with Kotlin and Jetpack Compose.

## Build an APK
1. Open the Actions tab in this repository.
2. Select Build Nation Nums Android APK.
3. Run the workflow or push a commit to main.
4. Download the nation-nums-debug-apk artifact from a successful workflow run.

## Current implementation
- Native blue-and-white dashboard and navigation.
- Account sign-in is the first step for users without an existing session. Device-only app passcode and biometric unlock are optional settings available after sign-in.
- Users create their own app passcode; only a salted PBKDF2 hash is stored.
- Optional biometric unlock is available when the Android device reports a supported enrolled biometric.
- The app uses its bundled Nation Nums launcher artwork in the app's brand mark. The exact logo asset from the live website still needs verification.
- Base44 email/password authentication code and encrypted Android Keystore session storage are present.
- The Base44 entity HTTP client is intentionally read-only and requires the user's access token. The dashboard reads the latest available `balance_after` snapshot from the existing `WalletTransaction` ledger; it never invents a balance when no ledger snapshot is available. Wallet credits, purchases, and provider actions still require verified authenticated backend functions.
- The API Documentation option opens the supplied developer reference externally: https://developer-nation-nums.base44.app/api-reference.
- The native profile stays focused on core mobile needs; web-oriented API key management, team management and Telegram settings are intentionally not exposed as native screens.

## Remaining verification and integration
- Confirm Base44 email/password authentication, current-user validation via `/auth/me`, and session restoration against the live app.
- Continue with Google native flow.
- Live wallet, payment, catalog, purchases, orders, OTP messages, account notifications and push notifications must use the existing verified backend functions.
- Exact live website logo and screen-by-screen visual parity still need verification.
- Do not treat placeholder dashboard values as live account data.

## Backend rules
The app must use the existing Nation Nums Base44 backend as the source of truth. Do not add a second database, Supabase, Cloudflare backend, or payment-provider secret keys to the APK. Verify the actual website/Base44 authentication and function contracts before connecting account actions.
