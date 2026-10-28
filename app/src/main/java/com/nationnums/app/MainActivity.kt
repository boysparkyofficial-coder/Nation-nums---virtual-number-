package com.nationnums.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NationBlue = Color(0xFF145DDB)
private val Ink = Color(0xFF14213D)
private val Muted = Color(0xFF6C7890)
private val Canvas = Color(0xFFF5F7FC)
private val PaleBlue = Color(0xFFEAF1FF)
private val Border = Color(0xFFE4EAF4)
private val Green = Color(0xFF17845D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = NationBlue,
                    background = Canvas,
                    surface = Color.White,
                    onBackground = Ink,
                    onSurface = Ink
                )
            ) {
                NationNumsApp()
            }
        }
    }
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Numbers("Numbers", Icons.Rounded.PhoneAndroid),
    Orders("Orders", Icons.Rounded.History),
    Wallet("Wallet", Icons.Rounded.Wallet),
    Profile("Profile", Icons.Rounded.AccountCircle)
}

private enum class ServicePage(val title: String, val subtitle: String) {
    VirtualNumbers("Buy virtual numbers", "Choose a country and service"),
    UsNumbers("Buy US numbers", "Browse supported US-number options"),
    Rentals("Rent a number", "View available rental durations"),
    EmailOtp("Email OTP", "Email verification services"),
    Proxies("Proxy services", "Browse supported proxy options"),
    Notifications("Notifications", "Updates for your account"),
    Support("Help & support", "Get help with your account"),
    Security("Security", "Protect your Nation Nums account"),
    ApiKeys("API key management", "Manage keys for supported API access"),
    Team("Team management", "Manage team access and permissions"),
    Telegram("Telegram settings", "Account linking and supported alerts"),
    Faq("Frequently asked questions", "Answers about Nation Nums"),
    Legal("Policies & app information", "Privacy, terms and app details")
}

@Composable
private fun NationNumsApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var selectedPage by remember { mutableStateOf<ServicePage?>(null) }
    var showSignIn by remember { mutableStateOf(false) }

    // Let Android's system Back button close nested screens before leaving the app.
    BackHandler(enabled = showSignIn || selectedPage != null) {
        if (showSignIn) {
            showSignIn = false
        } else {
            selectedPage = null
        }
    }

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            if (selectedPage == null && !showSignIn) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = Color.White
                ) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            showSignIn -> SignInPreview(
                modifier = Modifier.padding(padding),
                onBack = { showSignIn = false }
            )
            selectedPage != null -> ServiceDetailPage(
                page = selectedPage!!,
                onBack = { selectedPage = null },
                onSignIn = { showSignIn = true }
            )
            else -> when (selectedTab) {
                AppTab.Home -> Dashboard(
                    modifier = Modifier.padding(padding),
                    onFund = { selectedTab = AppTab.Wallet },
                    onBuy = { selectedTab = AppTab.Numbers },
                    onOrders = { selectedTab = AppTab.Orders },
                    onNotifications = { selectedPage = ServicePage.Notifications },
                    onProfile = { selectedTab = AppTab.Profile }
                )
                AppTab.Numbers -> ServicesScreen(
                    modifier = Modifier.padding(padding),
                    onOpen = { selectedPage = it }
                )
                AppTab.Orders -> OrdersScreen(
                    modifier = Modifier.padding(padding),
                    onSignIn = { showSignIn = true }
                )
                AppTab.Wallet -> WalletScreen(
                    modifier = Modifier.padding(padding),
                    onSignIn = { showSignIn = true }
                )
                AppTab.Profile -> ProfileScreen(
                    modifier = Modifier.padding(padding),
                    onOpen = { selectedPage = it },
                    onSignIn = { showSignIn = true }
                )
            }
        }
    }
}

@Composable
private fun Dashboard(
    modifier: Modifier = Modifier,
    onFund: () -> Unit,
    onBuy: () -> Unit,
    onOrders: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark()
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Nation Nums", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Your numbers. One dashboard.", fontSize = 12.sp, color = Muted)
                }
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = NationBlue,
                    modifier = Modifier.size(28.dp).clickable(onClick = onNotifications)
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Rounded.AccountCircle,
                    contentDescription = "Profile",
                    tint = NationBlue,
                    modifier = Modifier.size(29.dp).clickable(onClick = onProfile)
                )
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NationBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("WALLET BALANCE", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("₦ ———", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Sign in to load your live balance", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onFund,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NationBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("  Fund wallet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quick actions", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("View all", color = NationBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onBuy))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Buy number", "Virtual & US", Icons.Rounded.PhoneAndroid, onBuy, Modifier.weight(1f))
                QuickAction("My orders", "View purchases", Icons.Rounded.History, onOrders, Modifier.weight(1f))
            }
        }
        item {
            Text("Explore services", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceRow("Rent a number", "Temporary and rental options", Icons.Rounded.PhoneAndroid) { onBuy() }
                ServiceRow("Email OTP", "Supported email verification services", Icons.Rounded.CheckCircle) { onBuy() }
                ServiceRow("Proxy services", "Browse available proxy options", Icons.Rounded.Shield) { onBuy() }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = NationBlue, modifier = Modifier.size(24.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Your account, securely connected", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "This preview does not show live wallet or order data. Sign-in and account actions will be enabled only after the existing backend connection is verified.",
                            color = Muted, fontSize = 12.sp, lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier.size(46.dp).background(NationBlue, RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.padding(15.dp)) {
            Box(Modifier.size(42.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = NationBlue)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp)) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, fontSize = 13.sp, color = Muted, lineHeight = 19.sp)
    }
}

@Composable
private fun ServicesScreen(modifier: Modifier = Modifier, onOpen: (ServicePage) -> Unit) {
    var query by remember { mutableStateOf("") }
    val services = listOf(
        Triple(ServicePage.VirtualNumbers, "Buy virtual numbers", "Choose a country and service"),
        Triple(ServicePage.UsNumbers, "Buy US numbers", "Explore supported US options"),
        Triple(ServicePage.Rentals, "Rent a number", "Longer-use options where available"),
        Triple(ServicePage.EmailOtp, "Email OTP", "Email verification services"),
        Triple(ServicePage.Proxies, "Proxy services", "Available proxy products")
    )
    val filteredServices = services.filter {
        it.second.contains(query, ignoreCase = true) || it.third.contains(query, ignoreCase = true)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item { PageHeader("Numbers & services", "Choose the service you want to use.") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Search services") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
            )
            Spacer(Modifier.height(18.dp))
        }
        items(filteredServices) { item ->
            ServiceRow(item.second, item.third, Icons.Rounded.PhoneAndroid) { onOpen(item.first) }
            Spacer(Modifier.height(10.dp))
        }
        if (filteredServices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = NationBlue, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No matching services", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(5.dp))
                        Text("Try another service name, such as virtual, US, rent, email or proxy.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            InfoNotice("Live availability and pricing will be loaded from Nation Nums after the provider and backend contracts are connected.")
        }
    }
}

@Composable
private fun OrdersScreen(modifier: Modifier = Modifier, onSignIn: () -> Unit) {
    var filter by remember { mutableStateOf("All") }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        PageHeader("Your orders", "Track purchases, number status and OTP activity.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
            listOf("All", "Active", "Completed").forEach { option ->
                val selected = filter == option
                Surface(
                    color = if (selected) NationBlue else Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) NationBlue else Border),
                    modifier = Modifier.clickable { filter = option }
                ) {
                    Text(option, color = if (selected) Color.White else Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        EmptyState(
            icon = Icons.Rounded.History,
            title = "Your orders will appear here",
            body = "Sign in to view real purchases, OTP messages, rental status and order history.",
            action = "Sign in",
            onAction = onSignIn
        )
    }
}

@Composable
private fun WalletScreen(modifier: Modifier = Modifier, onSignIn: () -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item { PageHeader("Wallet", "Manage your balance and funding activity.") }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = NationBlue)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("AVAILABLE BALANCE", color = Color.White.copy(alpha = .8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("₦ ———", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Live balance requires account sign-in", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                }
            }
        }
        item { Text("Funding methods", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold) }
        item { ServiceRow("Nation Flow", "Virtual bank account funding", Icons.Rounded.Wallet) { onSignIn() } }
        item { ServiceRow("NationCheckout", "Online payment gateway", Icons.Rounded.Add) { onSignIn() } }
        item { ServiceRow("Transaction history", "Deposits, purchases and refunds", Icons.Rounded.History) { onSignIn() } }
        item {
            InfoNotice("Payments must be confirmed by the existing backend before a wallet is credited. This screen does not initiate real payments yet.")
        }
        item {
            Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                Text("Sign in to continue")
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier = Modifier,
    onOpen: (ServicePage) -> Unit,
    onSignIn: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item { PageHeader("Profile & settings", "Account tools, preferences and help.") }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = NationBlue, modifier = Modifier.size(32.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Welcome to Nation Nums", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Sign in to access your account", color = Muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Muted)
                }
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Sign in / Create account")
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("ACCOUNT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        item { ServiceRow("Security", "Password, sessions and biometric options", Icons.Rounded.Shield) { onOpen(ServicePage.Security) } }
        item { ServiceRow("Notifications", "Order and wallet updates", Icons.Rounded.Notifications) { onOpen(ServicePage.Notifications) } }
        item { ServiceRow("Telegram settings", "Linking and supported alerts", Icons.Rounded.Settings) { onOpen(ServicePage.Telegram) } }
        item {
            Spacer(Modifier.height(15.dp))
            Text("TOOLS & SUPPORT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        item { ServiceRow("API key management", "API documentation is not included in the app", Icons.Rounded.Shield) { onOpen(ServicePage.ApiKeys) } }
        item { ServiceRow("Team management", "Team members and access permissions", Icons.Rounded.AccountCircle) { onOpen(ServicePage.Team) } }
        item { ServiceRow("Help & support", "Support tickets and contact options", Icons.Rounded.HelpOutline) { onOpen(ServicePage.Support) } }
        item { ServiceRow("FAQs", "Common questions", Icons.Rounded.HelpOutline) { onOpen(ServicePage.Faq) } }
        item { ServiceRow("Policies & app information", "Privacy policy, terms and version", Icons.Rounded.Settings) { onOpen(ServicePage.Legal) } }
    }
}

@Composable
private fun ServiceDetailPage(page: ServicePage, onBack: () -> Unit, onSignIn: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onBack)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = NationBlue)
                Spacer(Modifier.width(8.dp))
                Text("Back", color = NationBlue, fontWeight = FontWeight.SemiBold)
            }
            PageHeader(page.title, page.subtitle)
        }
        item {
            InfoNotice(when (page) {
                ServicePage.ApiKeys -> "API key management is separate from API Documentation. The real key list and key actions must be connected to the verified backend before use."
                ServicePage.Notifications -> "Your real notifications will appear after account and push-notification integration is completed."
                ServicePage.Security -> "Security settings will use the existing account system. Passwords and private keys will not be stored in plain text."
                ServicePage.Support -> "Support ticket history and replies will load after the existing support endpoints are verified."
                ServicePage.Team -> "Team members, invitations and permissions will be loaded from the existing Nation Nums account after team access is verified."
                else -> "This page is part of the native app structure. Live data and actions will be enabled after the existing Nation Nums backend is connected and tested."
            })
            Spacer(Modifier.height(18.dp))
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Next step", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Sign in and connect to your existing Nation Nums account to access this feature.",
                        color = Muted, fontSize = 13.sp, lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text("Sign in to continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun SignInPreview(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark()
        Spacer(Modifier.height(22.dp))
        Text("Welcome back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text("Sign in to your Nation Nums account.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(13.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            shape = RoundedCornerShape(13.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text("Remember me", color = Ink, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        InfoNotice(
            "Sign-in is still a preview. Remember me is not saved yet, and credentials are not sent anywhere until the existing Base44 authentication flow is verified and connected."
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { },
            enabled = email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp)
        ) {
            Text("Authentication not connected yet")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
            Text("Back to app")
        }
    }
}

@Composable
private fun ServiceRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = NationBlue)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Open $title", tint = Muted)
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, body: String, action: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 45.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(76.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = NationBlue, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(7.dp))
        Text(body, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) { Text(action) }
    }
}

@Composable
private fun InfoNotice(text: String) {
    Surface(
        color = PaleBlue,
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = NationBlue, modifier = Modifier.size(19.dp))
            Text(text, color = Ink, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 9.dp))
        }
    }
}
