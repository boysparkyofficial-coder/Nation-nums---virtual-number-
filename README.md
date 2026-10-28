# Nation Nums — Native Android App

Native Android application for Nation Nums, built with Kotlin and Jetpack Compose.

## Build an APK
1. Open the Actions tab in this repository.
2. Select Build Nation Nums Android APK.
3. Run the workflow or push a commit to main.
4. Download the nation-nums-debug-apk artifact from a successful workflow run.

## Current implementation
- Native blue-and-white dashboard and navigation.
- App-only local password gate appears before the dashboard on first setup and when returning to the app after leaving it.
- Users create their own app password; only a salted PBKDF2 hash is stored.
- Optional biometric unlock is available when the Android device reports a supported enrolled biometric.
- The app uses its bundled Nation Nums launcher artwork in the app's brand mark. The exact logo asset from the live website still needs verification.
- A native Base44 entity HTTP client now supports authenticated entity reads and record creation using the existing Nation Nums app ID. It requires a valid user access token and is not yet wired to the login flow or feature screens.

## Not yet integrated
- Base44 account authentication and Continue with Google.
- Live wallet, payment, catalog, orders, OTP messages, account notifications and push notifications.
- The exact live website logo asset has not yet been verified.
- Do not treat placeholder dashboard values as live account data.

## Backend rules
The app must use the existing Nation Nums Base44 backend as the source of truth. Do not add a second database, Supabase, Cloudflare backend, or payment-provider secret keys to the APK. Verify the actual website/Base44 authentication and function contracts before connecting account actions.
