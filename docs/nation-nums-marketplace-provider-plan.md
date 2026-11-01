# Nation Nums Marketplace Provider Plan

This document is the shared implementation reference for the Base44 application and its GitHub codebase.

## Marketplace categories

1. SMS verification / temporary virtual numbers
2. Number rentals
3. Email OTP
4. Proxy services
5. Social media logs/accounts
6. Nigerian virtual top-up (VTU)
7. VPN is postponed and is not part of the current implementation scope.

## SMS providers

Selected:
- Young PG Virtual
- Fleexa
- Billy Plus
- AccNumbers
- TempNumber.ng
- OTPWave
- EasyNums

Still undecided:
- SMSvirtual.io

Permanently removed:
- 5SIM.net

PipeSync was also named by the user as a provider they no longer need and must not be re-added unless the user explicitly changes this decision.

## Current integration readiness

- OTPWave: adapter is being wired; its API key must be entered in Base44 Secrets before its connection test can pass.
- EasyNums: selected and already registered in the live Base44 `ProviderServer` registry as `server8` (customer-facing label `Server F`). The record is NGN, enabled and visible, with `sms_otp` and `us_number` enabled; Base44 reports `configuration_status=configured`, `connection_status=connected`, and `last_check=2026-11-01T05:47:11Z`. The credential reference is `EASYNUMS_API_KEY` and must remain in Base44 Secrets only. The official EasyNums homepage links its API documentation to the public Postman collection at https://documenter.getpostman.com/view/48935302/2sB3QFRYLG; the collection body could not be extracted in this review, so do not invent or change endpoints until the full Postman contract is checked. Important: the 35 current `ServiceCatalog` records tagged `operators=virtual8` are all Canada-only and were last updated 2026-10-28. Treat that catalogue as stale/incomplete; refresh countries, services, live stock and prices through the existing Base44 backend before relying on it for customer offers.
- TempNumber.ng: selected; adapter work is in progress and requires its reseller API key in Base44 Secrets. Its catalogue must remain API-driven; do not impose a hardcoded country restriction.
- AccNumbers: selected in the provider roster, but keep it disabled/awaiting API documentation and credentials until the official integration contract is available.
- SMSvirtual.io: hold/disabled while undecided.
- 5SIM.net and PipeSync: removed; do not re-add.
- YoungPG Media: the only selected Social Media Logs supplier.
- BillsJet: VTU candidate only; no purchases until account registration and API access are available.

Provider API keys and other credentials must never be committed to this repository. Add them only through Base44's secure Secrets settings.

The Android app now includes customer-facing entry points for Social Media Accounts and Virtual Top-Up. Those screens must use the same Nation Nums backend; they must not contain provider credentials or direct supplier API calls.

## Number rentals

Use every selected SMS provider that actually supports rentals. Discover rental duration, country, service, inventory, renewal/extension, incoming-message retrieval, cancellation and refund capabilities from each provider's own API. Do not assume every SMS provider supports rentals.

## Email OTP

Fleexa is a candidate because it has been discussed as an existing provider. Shadow OTP, OtpGet and MeOTP are research candidates only, not selected providers. Verify each provider's actual API, available inbox products, price, message retrieval, expiry, and refund behavior before enabling it.

## Proxy services

Provider selection is not finalized. The catalogue should include only proxy products actually returned by connected suppliers, such as residential, datacenter, mobile, static and rotating proxies where supported. Do not assume all suppliers offer every proxy type.

## Social media logs / accounts

Selected provider:
- YoungPG Media only.

Potential product types, only when actually listed by YoungPG Media and legitimately transferable:
- Aged social-media accounts
- Verified social-media accounts
- Bulk account listings

The integration must respect supplier authorization and applicable platform terms. Do not source or sell stolen credentials, session cookies, compromised accounts, or accounts without authority to transfer them.

## VTU

The VTU marketplace is required, with Nigerian services such as airtime, data, cable TV, electricity and exam pins only where a selected API supports them.

BillsJet is a preferred candidate under consideration, but account registration was unavailable to the user. It is not yet a confirmed selected provider. Do not integrate or claim it is active until the user can register, obtain API access, confirm NGN funding and test the API. VTU provider selection remains open.

## VPN

Postponed. Do not implement or integrate VPN providers at this stage.

## Shared provider-engine requirements

- Discover each provider's real API capabilities; do not invent endpoints, products, countries, service coverage or prices.
- Show every supported country/service/product that the provider actually offers. Do not impose arbitrary country or service restrictions.
- Synchronize live provider catalogue, stock/availability, provider price and product constraints where APIs support it.
- Keep provider credentials server-side only; never expose secrets in browser/mobile code or customer responses.
- Customer-facing offers use generic labels such as Server A, Server B, Server C. Do not expose supplier names to customers.
- Keep provider-specific identifiers and order references internally.
- Calculate the customer price from the provider's actual cost plus configured Nation Nums markup. Show and record provider cost, markup and customer price separately.
- Revalidate price and stock immediately before purchase. If price changes, ask the customer to confirm the new total before charging.
- Use unique internal transaction references and idempotency keys where supported.
- Protect against duplicate purchases after timeouts: check the original provider transaction/order status before retrying.
- Do not mark uncertain transactions as successful or failed prematurely. Keep them pending and reconcile through provider status endpoints or webhooks.
- Deduct wallet funds consistently and refund only when failure is confirmed or the provider's documented refund rule applies. Prevent duplicate refunds.
- Support webhooks where available and polling where webhooks are unavailable.
- Store raw provider response metadata safely for troubleshooting, with secrets and sensitive credentials redacted.
- Build provider adapters so each supplier's different API format maps into a common internal catalogue/order model without pretending their capabilities are identical.
- Admins can enable/disable providers and configure markup; provider secrets remain backend-only.
- Maintain audit logs for price changes, orders, status transitions, refunds and provider errors.

## Implementation status discipline

A provider marked selected is approved for integration discovery, not proof that its API has already been connected or tested. A provider marked undecided or under consideration must remain disabled until the user confirms it. Build and test category-by-category, and keep the Base44 implementation and this GitHub project documentation aligned.
