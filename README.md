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
- EasyNums is already registered in the existing Base44 provider registry as an enabled NGN supplier. Its API key remains server-side in Base44 Secrets; the Android app must never call EasyNums directly or contain supplier credentials. The native app must use the same Base44 catalogue, authenticated purchase functions, wallet ledger and order records as the website.
- The Base44 registry reports EasyNums connected, but its 35 `virtual8` catalogue rows are Canada-only and were last refreshed on 2026-10-28. Refresh and verify the full live EasyNums country/service/stock/price catalogue through Base44 before treating the catalogue as current.
- The API Documentation option opens the supplied developer reference externally: https://developer-nation-nums.base44.app/api-reference.
- The native profile stays focused on core mobile needs; web-oriented API key management, team management and Telegram settings are intentionally not exposed as native screens.

## Remaining verification and integration
- Confirm Base44 email/password authentication, current-user validation via `/auth/me`, and session restoration against the live app.
- Continue with Google native flow.
- Live wallet, payment, catalog, purchases, orders, OTP messages, account notifications and push notifications must use the existing verified backend functions.
- Review the official EasyNums Postman API contract and refresh its stale `virtual8` catalogue through the Base44 backend; do not add provider calls or credentials to the APK.
- Exact live website logo and screen-by-screen visual parity still need verification.
- Do not treat placeholder dashboard values as live account data.

## Backend rules
The app must use the existing Nation Nums Base44 backend as the source of truth. Do not add a second database, Supabase, Cloudflare backend, or payment-provider secret keys to the APK. Verify the actual website/Base44 authentication and function contracts before connecting account actions.

## Native backend connection — 1.0.24

The Android client now calls the same authenticated Base44 function transport as the website, using each customer's encrypted session token. No supplier key or service credential is bundled.

Connected core: server-calculated wallet balance and paginated ledger; session-scoped numbers, SMS and notifications; live SMS/US country/service/offer selection; price confirmation and wallet-backed purchases; per-number SMS synchronization; notification read state; hosted NationCheckout initiation and server-side payment verification; Android device registration, refresh, unlink and notification routing.

Deploy/publish the Base44 changes containing mobileAccount and the updated purchase/payment functions before distributing this APK. CI runs unit tests and produces the rebuilt APK; customer transactions and real push delivery still require device acceptance testing.

Remaining native flows: rental checkout, proxies, email OTP, social marketplace, NationFlow virtual-account setup, support tickets, provider-aware cancellation, and Google sign-in. QR numbers and VTU have no verified backend purchase contract and remain unavailable. Mobile push sending remains disabled until a valid rotated Firebase server service-account credential is supplied securely to the backend; google-services.json is not that credential.


## Connected customer backend — 1.0.25
Native screens use the existing Nation Nums customer session and backend for SMS/US numbers, rental catalogues/prices, Email OTP, proxy quotes/purchases/usage and credentials, social account purchases/delivery history, cancellation/refunds, wallet checkout/bank transfers, support tickets/replies, API keys, Telegram linking, profile updates and WhatsApp verification. Purchases and wallet mutations stay server-side with existing pricing and profit rules unchanged.

Admin/provider credentials and admin-only operations are never embedded in the APK. QR-number purchases, VTU, rental extension and team management have no supported customer backend implementation. Android push registration is connected; sending requires valid server-side Firebase credentials. Install this commit's successfully built APK to receive the native changes.
