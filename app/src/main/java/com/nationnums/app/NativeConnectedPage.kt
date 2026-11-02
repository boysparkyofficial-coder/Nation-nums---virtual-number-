package com.nationnums.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun NativeConnectedPage(page: ServicePage, backend: Base44Api, auth: Base44AuthApi, modifier: Modifier, onBack: () -> Unit, onPurchased: () -> Unit, onPage: (ServicePage) -> Unit, onAccountChanged: () -> Unit) {
    val serviceKey = when (page) { ServicePage.VirtualNumbers -> "sms_otp"; ServicePage.UsNumbers -> "us_number"; ServicePage.Rentals -> "rent_number"; ServicePage.EmailOtp -> "email_otp"; ServicePage.Proxies -> "proxy"; else -> "" }
    val content: @Composable () -> Unit = {
        when (page) {
            ServicePage.VirtualNumbers, ServicePage.UsNumbers -> NativeNumberPurchase(backend, page == ServicePage.UsNumbers, modifier, onBack, onPurchased)
            ServicePage.Rentals -> NativeRentalPurchase(backend, modifier, onBack, onPurchased)
            ServicePage.EmailOtp -> NativeEmailPurchase(backend, modifier, onBack, onPurchased)
            ServicePage.Proxies -> NativeProxyPurchase(backend, modifier, onBack, { onAccountChanged(); onPage(ServicePage.ProxyOrders) }, { onPage(ServicePage.ProxyOrders) })
            ServicePage.SocialMediaLogs -> NativeSocialPurchase(backend, modifier, onBack, { onAccountChanged(); onPage(ServicePage.SocialOrders) }, { onPage(ServicePage.SocialOrders) })
            ServicePage.ProxyOrders, ServicePage.SocialOrders -> NativeRecordHistory(backend, page == ServicePage.SocialOrders, modifier, onBack)
            ServicePage.Support -> NativeSupportPage(backend, modifier, onBack)
            ServicePage.ApiKeys -> NativeApiKeysPage(backend, modifier, onBack)
            ServicePage.Telegram -> NativeTelegramPage(backend, modifier, onBack)
            ServicePage.AccountDetails -> NativeAccountDetails(backend, auth, modifier, onBack, onAccountChanged)
            else -> Unit
        }
    }
    if (serviceKey.isNotBlank()) NativeServiceGate(backend, serviceKey, modifier, onBack, content) else content()
}
