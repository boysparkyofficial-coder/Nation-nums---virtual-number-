# Nation Nums — Native Android App

Native Android application for Nation Nums, built with Kotlin and Jetpack Compose.

## Build an APK
1. Open the **Actions** tab in this repository.
2. Select **Build Nation Nums Android APK**.
3. Run the workflow or push a commit to `main`.
4. Download the `nation-nums-debug-apk` artifact from the successful workflow run.

## Current stage
- Native Android project scaffold and dashboard UI.
- GitHub Actions builds a debug APK.
- The app now contains a dependency-free Base44 entity API client scaffold for the existing Nation Nums app ID. It is not yet wired to sign-in screens or account data.
- Live authentication, Continue with Google, wallet, payment, catalog, order, OTP and push-notification flows remain unconnected until their actual website/Base44 contracts and logo asset are verified. Do not treat displayed placeholder values as live account data.

## Backend integration requirements
The app must use the existing Nation Nums Base44 backend as the source of truth. No second database, Supabase, Cloudflare backend, or payment-provider secret keys should be added to the APK. Before wiring account actions, verify the actual Base44 auth and function contracts from the existing website/backend.
