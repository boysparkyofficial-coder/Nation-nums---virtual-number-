package com.nationnums.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NationBlue = Color(0xFF145DDB)
private val Ink = Color(0xFF14213D)
private val Muted = Color(0xFF6C7890)

private fun latestWalletBalance(transactions: List<WalletTransactionRecord>): String? =
    transactions.asSequence()
        .filter { it.balanceAfter.toBigDecimalOrNull() != null }
        .maxByOrNull { transaction -> transaction.createdAt }
        ?.balanceAfter

private fun formatNairaAmount(rawAmount: String): String {
    val normalized = rawAmount.trim().replace(",", "").replace("₦", "").trim()
    val amount = normalized.toBigDecimalOrNull() ?: return "₦$rawAmount"
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "NG"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 0
    return "₦${formatter.format(amount)}"
}
private val Canvas = Color(0xFFF5F7FC)
private val PaleBlue = Color(0xFFEAF1FF)
private val Border = Color(0xFFE4EAF4)
private val Green = Color(0xFF17845D)

class MainActivity : FragmentActivity() {
    var shouldLockOnResume by mutableStateOf(false)
        private set

    var notificationDestination by mutableStateOf<String?>(null)
        private set
    fun consumeNotification() { notificationDestination = null }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationDestination = intent.getStringExtra("notification_destination")
    }
    fun clearRelockFlag() { shouldLockOnResume = false }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isFinishing) shouldLockOnResume = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationDestination = intent?.getStringExtra("notification_destination")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7401)
        }
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
                NationNumsApp(this@MainActivity)
            }
        }
    }
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Dashboard", Icons.Rounded.Home),
    Numbers("Phone", Icons.Rounded.PhoneAndroid),
    Orders("Order", Icons.Rounded.History),
    Wallet("Wallet", Icons.Rounded.Wallet),
    Profile("Profile", Icons.Rounded.AccountCircle)
}

internal enum class ServicePage(val title: String, val subtitle: String) {
    AccountDetails("Account details", "Your verified Nation Nums profile"),
    ProxyOrders("My proxies", "Delivered proxy credentials and usage"),
    SocialOrders("Social account orders", "Your delivered account credentials"),
    VirtualNumbers("Buy virtual numbers", "Choose a country and service"),
    QrNumbers("Buy by QR number", "Browse supported QR-number options"),
    UsNumbers("Buy US numbers", "Browse supported US-number options"),
    Rentals("Rent a number", "View available rental durations"),
    EmailOtp("Email OTP", "Email verification services"),
    Proxies("Proxy services", "Browse supported proxy options"),
    SocialMediaLogs("Social media accounts", "Eligible account listings from YoungPG Media"),
    VirtualTopUp("Virtual top-up", "Airtime, data and supported bill payments"),
    Notifications("Notifications", "Updates for your account"),
    Support("Help & support", "Get help with your account"),
    Security("Security", "Protect your Nation Nums account"),
    ApiKeys("API key management", "Manage keys for supported API access"),
    Documentation("API Documentation", "Open the official Nation Nums API documentation in your browser"),
    Team("Team management", "Manage team access and permissions"),
    Telegram("Telegram settings", "Account linking and supported alerts"),
    Faq("Frequently asked questions", "Answers about Nation Nums"),
    Legal("Policies & app information", "Privacy, terms and app details")
}

private enum class GateState { Setup, Locked, Dashboard }

@Composable
private fun NationNumsApp(activity: MainActivity) {
    val lockStore = remember { AppLockStore(activity) }
    val sessionStore = remember { Base44SessionStore(activity) }
    var gateState by remember {
        mutableStateOf(
            if (lockStore.isConfigured() && !sessionStore.readToken().isNullOrBlank())
                GateState.Locked else GateState.Dashboard
        )
    }
    var gateError by remember { mutableStateOf("") }
    val biometricAvailable = remember {
        BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(activity.shouldLockOnResume) {
        if (activity.shouldLockOnResume) {
            if (gateState == GateState.Dashboard &&
                lockStore.isConfigured() && !sessionStore.readToken().isNullOrBlank()
            ) {
                gateState = GateState.Locked
                gateError = ""
            }
            activity.clearRelockFlag()
        }
    }

    fun promptBiometric() {
        if (!biometricAvailable || !lockStore.isBiometricEnabled()) {
            gateError = "Biometric unlock is not available. Use your app password."
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    gateError = ""
                    gateState = GateState.Dashboard
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    gateError = "Biometric not recognised. Try again or use your app password."
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        gateError = errString.toString()
                    }
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Nation Nums")
            .setSubtitle("Confirm it's you to open the app")
            .setNegativeButtonText("Use app password")
            .build()
        prompt.authenticate(promptInfo)
    }

    if (gateState != GateState.Dashboard) {
        AppLockScreen(
            isSetup = gateState == GateState.Setup,
            biometricAvailable = biometricAvailable,
            biometricEnabled = lockStore.isBiometricEnabled(),
            errorMessage = gateError,
            onCreatePassword = { password, enableBiometric ->
                if (password.length < 4) {
                    gateError = "Choose an app password with at least 4 characters."
                } else {
                    lockStore.configurePassword(password, enableBiometric && biometricAvailable)
                    gateError = ""
                    gateState = GateState.Dashboard
                }
            },
            onUnlockPassword = { password ->
                if (lockStore.isLockedOut()) {
                    gateError = "Too many incorrect attempts. Try again in ${lockStore.remainingLockoutSeconds()} seconds."
                } else if (lockStore.verifyPassword(password)) {
                    lockStore.clearFailedAttempts()
                    gateError = ""
                    gateState = GateState.Dashboard
                } else {
                    val attemptsRemaining = lockStore.recordFailedAttempt()
                    gateError = if (attemptsRemaining <= 0) {
                        "Too many incorrect attempts. App unlock is paused for 30 seconds."
                    } else {
                        "Incorrect passcode. ${attemptsRemaining} attempt(s) left before a short lockout."
                    }
                }
            },
            onBiometric = { promptBiometric() }
        )
    } else {
        var selectedTab by remember { mutableStateOf(AppTab.Home) }
        var selectedPage by remember { mutableStateOf<ServicePage?>(null) }
        var showSignIn by remember { mutableStateOf(sessionStore.readToken().isNullOrBlank()) }
        val authApi = remember { Base44AuthApi(sessionStore) }
        var signedInUser by remember { mutableStateOf<Base44AuthApi.User?>(null) }
        var checkingSession by remember { mutableStateOf(true) }
        var walletTransactions by remember { mutableStateOf<List<WalletTransactionRecord>>(emptyList()) }
        var walletBalance by remember { mutableStateOf<String?>(null) }
        var walletLoading by remember { mutableStateOf(false) }
        var walletError by remember { mutableStateOf("") }
        var walletRefresh by remember { mutableStateOf(0) }
        var virtualNumbers by remember { mutableStateOf<List<VirtualNumberRecord>>(emptyList()) }
        var serviceCatalog by remember { mutableStateOf<List<ServiceCatalogRecord>>(emptyList()) }
        var catalogLoading by remember { mutableStateOf(false) }
        var catalogError by remember { mutableStateOf("") }
        var numbersLoading by remember { mutableStateOf(false) }
        var numbersError by remember { mutableStateOf("") }
        var numbersRefresh by remember { mutableStateOf(0) }
        var ordersRefresh by remember { mutableStateOf(0) }
        var notifications by remember { mutableStateOf<List<NotificationRecord>>(emptyList()) }
        var notificationsLoading by remember { mutableStateOf(false) }
        var notificationsError by remember { mutableStateOf("") }
        var notificationsRefresh by remember { mutableStateOf(0) }
        var smsMessages by remember { mutableStateOf<List<SmsMessageRecord>>(emptyList()) }
        var ordersLoading by remember { mutableStateOf(false) }
        var ordersError by remember { mutableStateOf("") }
        val repository = remember(signedInUser?.id) { NationNumsRepository { sessionStore.readToken() } }
        val backend = remember { Base44Api { sessionStore.readToken() } }
        val pushRegistration = remember { AndroidPushRegistration(activity, sessionStore) }
        var pushStatus by remember { mutableStateOf("") }
        var walletMore by remember { mutableStateOf(false) }
        var numbersMore by remember { mutableStateOf(false) }
        var ordersMore by remember { mutableStateOf(false) }
        var notificationsMore by remember { mutableStateOf(false) }

        fun loadWallet(more: Boolean = false) {
            val owner = signedInUser?.id ?: return
            walletLoading = true; walletError = ""
            repository.loadWalletTransactions(more) { result, records ->
                if (signedInUser?.id == owner) { walletLoading = false; walletTransactions = records; walletBalance = if (result.success) repository.walletBalance else null; walletMore = repository.hasMore("wallet"); walletError = if (result.success) "" else result.error ?: "Could not load your wallet." }
            }
        }
        fun loadNumbers(more: Boolean = false) {
            val owner = signedInUser?.id ?: return
            numbersLoading = true; numbersError = ""
            repository.loadVirtualNumbers(more) { result, records ->
                if (signedInUser?.id == owner) { numbersLoading = false; virtualNumbers = records; numbersMore = repository.hasMore("numbers"); numbersError = if (result.success) "" else result.error ?: "Could not load your numbers." }
            }
        }
        fun loadNotifications(more: Boolean = false) {
            val owner = signedInUser?.id ?: return
            notificationsLoading = true; notificationsError = ""
            repository.loadNotifications(more) { result, records ->
                if (signedInUser?.id == owner) { notificationsLoading = false; notifications = records; notificationsMore = repository.hasMore("notifications"); notificationsError = if (result.success) "" else result.error ?: "Could not load notifications." }
            }
        }
        fun loadOrders(more: Boolean = false) {
            val owner = signedInUser?.id ?: return
            ordersLoading = true; ordersError = ""
            var remaining = 2
            fun finish() { remaining--; if (remaining == 0) { ordersLoading = false; ordersMore = repository.hasMore("numbers") || repository.hasMore("messages") } }
            repository.loadVirtualNumbers(more) { result, records -> if (signedInUser?.id == owner) { virtualNumbers = records; if (!result.success) ordersError = result.error ?: "Could not load your orders."; finish() } }
            repository.loadSmsMessages(more) { result, records -> if (signedInUser?.id == owner) { smsMessages = records; if (!result.success) ordersError = result.error ?: "Could not load SMS."; finish() } }
        }
        LaunchedEffect(signedInUser?.id) { if (signedInUser != null) pushRegistration.sync { pushStatus = it } }
        LaunchedEffect(activity.notificationDestination, signedInUser?.id) {
            if (signedInUser != null && activity.notificationDestination != null) {
                when (activity.notificationDestination) { "wallet" -> { selectedPage = null; selectedTab = AppTab.Wallet }; "orders" -> { selectedPage = null; selectedTab = AppTab.Orders }; else -> selectedPage = ServicePage.Notifications }
                activity.consumeNotification()
            }
        }
        LaunchedEffect(signedInUser?.id, selectedTab, selectedPage, notificationsRefresh, numbersRefresh, ordersRefresh, walletRefresh) {
            if (signedInUser != null) {
                if (selectedPage == ServicePage.Notifications) loadNotifications()
                else when (selectedTab) { AppTab.Home, AppTab.Wallet -> loadWallet(); AppTab.Numbers -> loadNumbers(); AppTab.Orders -> loadOrders(); else -> Unit }
            }
        }

        LaunchedEffect(Unit) {
            authApi.currentUser { result ->
                signedInUser = if (result.success) result.user else null
                checkingSession = false
                if (!result.success) showSignIn = true
            }
        }

        BackHandler(enabled = selectedPage != null) {
            selectedPage = null
        }

        if (showSignIn) {
            SignInScreen(
                onBack = { showSignIn = false },
                onRegister = { email, password, complete ->
                    authApi.register(email, password) { result ->
                        complete(if (result.success) null else result.error ?: "Account creation failed.")
                    }
                },
                onVerifyOtp = { email, code, complete ->
                    authApi.verifyOtp(email, code) { result ->
                        if (!result.success) complete(result.error ?: "Email verification failed.")
                        else authApi.currentUser { current ->
                            complete(if (current.success) null else current.error ?: "Please sign in to continue.")
                            if (current.success) { signedInUser = current.user; checkingSession = false; showSignIn = false }
                        }
                    }
                },
                onForgotPassword = { email, complete ->
                    authApi.requestPasswordReset(email) { result ->
                        complete(if (result.success) null else result.error ?: "Password reset request failed.")
                    }
                },
                onLogin = { email, password, rememberMe, complete ->
                    authApi.login(email, password, rememberMe) { result ->
                        complete(if (result.success) null else result.error ?: "Sign-in failed.")
                        if (result.success) {
                            signedInUser = result.user
                            checkingSession = false
                            showSignIn = false
                        }
                    }
                }
            )
        } else Scaffold(
            containerColor = Canvas,
            bottomBar = {
                if (selectedPage == null) {
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
            if (selectedPage == ServicePage.Notifications) {
                NotificationsScreen(
                    modifier = Modifier.padding(padding),
                    notifications = notifications,
                    isLoading = notificationsLoading,
                    errorMessage = notificationsError,
                    isSignedIn = signedInUser != null,
                    onBack = { selectedPage = null },
                    onSignIn = { showSignIn = true },
                    onRefresh = { notificationsRefresh++ },
                    hasMore = notificationsMore, onLoadMore = { loadNotifications(true) },
                    pushStatus = pushStatus,
                    onRead = { id -> repository.readNotification(id) { result -> if (result.success) notificationsRefresh++ else notificationsError = result.error ?: "Could not update notification." } }
                )
            } else if (selectedPage in setOf(ServicePage.VirtualNumbers, ServicePage.UsNumbers, ServicePage.Rentals, ServicePage.EmailOtp, ServicePage.Proxies, ServicePage.SocialMediaLogs, ServicePage.ProxyOrders, ServicePage.SocialOrders, ServicePage.Support, ServicePage.ApiKeys, ServicePage.Telegram, ServicePage.AccountDetails)) {
                if (signedInUser == null) Button(onClick = { showSignIn = true }, modifier = Modifier.padding(padding)) { Text("Sign in to continue") }
                else key(signedInUser!!.id, selectedPage) {
                    NativeConnectedPage(selectedPage!!, backend, authApi, Modifier.padding(padding), onBack = { selectedPage = null }, onPurchased = { selectedPage = null; selectedTab = AppTab.Orders; ordersRefresh++; walletRefresh++ }, onPage = { selectedPage = it }, onAccountChanged = { ordersRefresh++; walletRefresh++; authApi.currentUser { result -> if (result.success) signedInUser = result.user } })
                }
            } else if (selectedPage != null) {
                ServiceDetailPage(
                    page = selectedPage!!,
                    onBack = { selectedPage = null },
                    onSignIn = { showSignIn = true },
                    onSetupLock = {
                        selectedPage = null
                        gateError = ""
                        gateState = GateState.Setup
                    },
                    lockConfigured = lockStore.isConfigured(),
                    biometricAvailable = biometricAvailable
                )
            } else {
                when (selectedTab) {
                    AppTab.Home -> Dashboard(
                        modifier = Modifier.padding(padding),
                        signedIn = signedInUser != null,
                        displayName = signedInUser?.username?.takeIf { it.isNotBlank() } ?: signedInUser?.email?.substringBefore("@").orEmpty(),
                        walletBalance = walletBalance,
                        walletLoading = walletLoading,
                        walletError = walletError,
                        onFund = { selectedTab = AppTab.Wallet },
                        onBuy = { selectedTab = AppTab.Numbers },
                        onOrders = { selectedTab = AppTab.Orders },
                        onNotifications = { selectedPage = ServicePage.Notifications },
                        onProfile = { selectedTab = AppTab.Profile },
                        onService = { page ->
                            if (page == ServicePage.Documentation) {
                                runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://developer-nation-nums.base44.app/api-reference"))) }
                            } else selectedPage = page
                        }
                    )
                    AppTab.Numbers -> ServicesScreen(
                        modifier = Modifier.padding(padding),
                        numbers = virtualNumbers,
                        serviceCatalog = serviceCatalog,
                        catalogLoading = catalogLoading,
                        catalogError = catalogError,
                        numbersLoading = numbersLoading,
                        numbersError = numbersError,
                        isSignedIn = signedInUser != null,
                        onSignIn = { showSignIn = true },
                        onRefresh = { numbersRefresh++ },
                        hasMore = numbersMore, onLoadMore = { loadNumbers(true) },
                        onOpen = { selectedPage = it }
                    )
                    AppTab.Orders -> OrdersScreen(
                        modifier = Modifier.padding(padding),
                        numbers = virtualNumbers,
                        smsMessages = smsMessages,
                        isLoading = ordersLoading,
                        errorMessage = ordersError,
                        isSignedIn = signedInUser != null,
                        onSignIn = { showSignIn = true },
                        onRefresh = { ordersRefresh++ },
                        hasMore = ordersMore, onLoadMore = { loadOrders(true) },
                        backend = backend, onAccountChanged = { ordersRefresh++; walletRefresh++ }, onOpen = { selectedPage = it }
                    )
                    AppTab.Wallet -> WalletScreen(
                        modifier = Modifier.padding(padding),
                        transactions = walletTransactions,
                        walletBalance = walletBalance,
                        isLoading = walletLoading,
                        errorMessage = walletError,
                        isSignedIn = signedInUser != null,
                        onSignIn = { showSignIn = true },
                        onRefresh = { walletRefresh++ },
                        hasMore = walletMore, onLoadMore = { loadWallet(true) }, backend = backend
                    )
                    AppTab.Profile -> ProfileScreen(
                        modifier = Modifier.padding(padding),
                        user = signedInUser,
                        checkingSession = checkingSession,
                        onOpen = { page ->
                            if (page == ServicePage.Documentation) {
                                runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://developer-nation-nums.base44.app/api-reference"))) }
                            } else selectedPage = page
                        },
                        onSignIn = { showSignIn = true },
                        onSignOut = {
                            pushRegistration.unlink {
                            authApi.logout()
                            signedInUser = null
                            walletTransactions = emptyList()
                            walletBalance = null
                            virtualNumbers = emptyList()
                            smsMessages = emptyList()
                            notifications = emptyList()
                            pushStatus = ""
                            showSignIn = true
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun SignInScreen(
    onBack: () -> Unit,
    onLogin: (String, String, Boolean, (String?) -> Unit) -> Unit,
    onRegister: (String, String, (String?) -> Unit) -> Unit,
    onVerifyOtp: (String, String, (String?) -> Unit) -> Unit,
    onForgotPassword: (String, (String?) -> Unit) -> Unit
) {
    var mode by remember { mutableStateOf("login") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    fun runAction(action: ((String?) -> Unit) -> Unit, onSuccess: () -> Unit) {
        busy = true
        message = ""
        successMessage = ""
        action { error ->
            busy = false
            if (error == null) onSuccess() else message = error
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Canvas).verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(28.dp))
        BrandMark()
        Spacer(Modifier.height(18.dp))
        Text(
            when (mode) {
                "register" -> "Create your account"
                "verify" -> "Verify your email"
                "forgot" -> "Reset your password"
                else -> "Sign in to Nation Nums"
            },
            color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when (mode) {
                "register" -> "Create a Nation Nums account. We'll send a verification code to your email."
                "verify" -> "Enter the one-time code sent to $email to verify your account."
                "forgot" -> "Enter your account email and we'll send password-reset instructions if the account exists."
                else -> "Use the same email and password as the Nation Nums website."
            },
            color = Muted, fontSize = 14.sp, lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        if (mode != "verify") {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; message = ""; successMessage = "" },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp)
            )
        }
        if (mode == "login" || mode == "register") {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; message = "" },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(if (mode == "register") "Create password" else "Password") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(14.dp)
            )
            if (mode == "register") {
                Spacer(Modifier.height(5.dp))
                Text("Use at least 8 characters for your password.", color = Muted, fontSize = 11.sp)
            }
        }
        if (mode == "verify") {
            OutlinedTextField(
                value = otp, onValueChange = { otp = it.filter(Char::isDigit).take(8); message = "" },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Email verification code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(14.dp)
            )
        }
        if (mode == "login") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { rememberMe = !rememberMe }) {
                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Column {
                    Text("Remember me", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(if (rememberMe) "Keep the encrypted session on this device." else "Session stays in memory and is not saved after closing the app.", color = Muted, fontSize = 11.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Forgot password?", color = NationBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { mode = "forgot"; message = ""; successMessage = "" }.padding(vertical = 8.dp))
            }
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFFB42318), fontSize = 13.sp, lineHeight = 18.sp)
        }
        if (successMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(successMessage, color = Green, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                when (mode) {
                    "login" -> {
                        if (email.isBlank() || password.isBlank()) message = "Enter your email and password."
                        else runAction({ done -> onLogin(email, password, rememberMe, done) }) { }
                    }
                    "register" -> {
                        if (email.isBlank() || password.length < 8) message = "Enter a valid email and a password with at least 8 characters."
                        else runAction({ done -> onRegister(email, password, done) }) {
                            mode = "verify"
                            successMessage = "Account created. Check your email for the verification code."
                        }
                    }
                    "verify" -> {
                        if (otp.isBlank()) message = "Enter the verification code from your email."
                        else runAction({ done -> onVerifyOtp(email, otp, done) }) {
                            mode = "login"
                            password = ""
                            otp = ""
                            successMessage = "Email verified. You can now sign in."
                        }
                    }
                    "forgot" -> {
                        if (email.isBlank()) message = "Enter your account email address."
                        else runAction({ done -> onForgotPassword(email, done) }) {
                            successMessage = "If an account exists for that email, password-reset instructions have been sent."
                        }
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (busy) "Please wait…" else when (mode) {
                    "register" -> "Create account"
                    "verify" -> "Verify email"
                    "forgot" -> "Send reset instructions"
                    else -> "Sign in securely"
                },
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(14.dp))
        when (mode) {
            "login" -> Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text("Don't have an account? ", color = Muted, fontSize = 13.sp)
                Text("Create account", color = NationBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { mode = "register"; message = ""; successMessage = "" })
            }
            "register" -> Text("Already have an account? Sign in", color = NationBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { mode = "login"; message = ""; successMessage = "" })
            else -> Text("Back to sign in", color = NationBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { mode = "login"; message = ""; successMessage = "" })
        }
        Spacer(Modifier.height(18.dp))
        Text("Your account is authenticated by the existing Nation Nums Base44 backend. App passcode and biometric reauthentication can be configured after sign-in in Profile → Security.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun AppLockScreen(
    isSetup: Boolean,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    errorMessage: String,
    onCreatePassword: (String, Boolean) -> Unit,
    onUnlockPassword: (String) -> Unit,
    onBiometric: () -> Unit
) {
    var passcode by remember(isSetup) { mutableStateOf("") }
    var confirmCode by remember(isSetup) { mutableStateOf("") }
    var confirming by remember(isSetup) { mutableStateOf(false) }
    var enableBiometric by remember(isSetup) { mutableStateOf(biometricEnabled) }
    var localError by remember(isSetup) { mutableStateOf("") }
    var showRecoveryInfo by remember { mutableStateOf(false) }
    val shownError = if (localError.isNotBlank()) localError else errorMessage
    val activeCode = if (isSetup && confirming) confirmCode else passcode

    fun addDigit(digit: String) {
        localError = ""
        if (isSetup && confirming) {
            if (confirmCode.length < 5) confirmCode += digit
        } else if (passcode.length < 5) {
            passcode += digit
        }
    }
    fun deleteDigit() {
        localError = ""
        if (isSetup && confirming) {
            if (confirmCode.isNotEmpty()) confirmCode = confirmCode.dropLast(1)
        } else if (passcode.isNotEmpty()) {
            passcode = passcode.dropLast(1)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White).imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandMark()
            Spacer(Modifier.height(20.dp))
            Text(
                when {
                    isSetup && !confirming -> "Secure your app"
                    isSetup && confirming -> "Confirm passcode"
                    else -> "Welcome back"
                },
                color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    isSetup && !confirming -> "Create a 5-digit app passcode"
                    isSetup && confirming -> "Enter the same 5 digits again"
                    else -> "Enter your Nation Nums app passcode"
                },
                color = Muted, fontSize = 14.sp
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier.size(width = 49.dp, height = 57.dp)
                            .background(Color(0xFFF5F5F7), RoundedCornerShape(12.dp))
                            .then(
                                if (index < activeCode.length)
                                    Modifier.background(Color(0xFFF0EAFE), RoundedCornerShape(12.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < activeCode.length) {
                            Text("●", color = Color(0xFF4A238B), fontSize = 18.sp)
                        } else {
                            Text(" ", color = Muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            val keypadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )
            keypadRows.forEach { rowDigits ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowDigits.forEach { key ->
                        val isSpecial = key == "BIO" || key == "DEL"
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp).size(68.dp)
                                .background(
                                    if (key == "BIO" && !isSetup && biometricEnabled && biometricAvailable)
                                        Color(0xFF7A7A7A)
                                    else Color(0xFFF4F4F5),
                                    CircleShape
                                )
                                .clickable {
                                    when (key) {
                                        "DEL" -> deleteDigit()
                                        "BIO" -> {
                                            if (!isSetup && biometricEnabled && biometricAvailable) onBiometric()
                                            else localError = "Fingerprint unlock is not enabled. Use your 5-digit passcode."
                                        }
                                        else -> addDigit(key)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "BIO" -> Text(
                                    "◉",
                                    color = if (!isSetup && biometricEnabled && biometricAvailable) Color.White else Muted,
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                "DEL" -> Text("⌫", color = Muted, fontSize = 27.sp)
                                else -> Text(key, color = Color(0xFF666666), fontSize = 22.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            if (isSetup && biometricAvailable) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = enableBiometric, onCheckedChange = { enableBiometric = it })
                    Text("Enable fingerprint unlock", color = Ink, fontSize = 13.sp)
                }
            }
            if (shownError.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(shownError, color = Color(0xFFB42318), fontSize = 12.sp, lineHeight = 17.sp)
            }
            if (showRecoveryInfo) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "For security, resetting this app-only passcode requires account re-authentication. The Base44 account recovery flow must be connected before passcode reset or account switching can be completed here.",
                    color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    localError = ""
                    if (isSetup && !confirming) {
                        if (passcode.length != 5) localError = "Enter all 5 digits to continue."
                        else confirming = true
                    } else if (isSetup) {
                        if (confirmCode.length != 5) localError = "Enter all 5 confirmation digits."
                        else if (passcode != confirmCode) {
                            confirmCode = ""
                            localError = "Passcodes do not match. Please confirm again."
                        } else {
                            onCreatePassword(passcode, enableBiometric && biometricAvailable)
                        }
                    } else {
                        if (passcode.length != 5) localError = "Enter all 5 digits to unlock."
                        else onUnlockPassword(passcode)
                    }
                },
                enabled = activeCode.length == 5,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    when {
                        isSetup && !confirming -> "Continue"
                        isSetup && confirming -> "Create passcode"
                        else -> "Unlock Nation Nums"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            if (isSetup && confirming) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "Back",
                    color = NationBlue,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        confirming = false
                        confirmCode = ""
                        localError = ""
                    }.padding(8.dp)
                )
            }
            if (!isSetup) {
                Spacer(Modifier.height(15.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Forgot passcode?",
                        color = Color(0xFF4A238B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            showRecoveryInfo = !showRecoveryInfo
                            localError = ""
                        }
                    )
                    Text("  |  ", color = Muted, fontSize = 13.sp)
                    Text(
                        "Switch account",
                        color = Color(0xFF4A238B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            showRecoveryInfo = true
                            localError = "Account switching is not available until secure Base44 sign-in is connected."
                        }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (isSetup)
                    "This passcode protects this mobile app only. It is separate from your website password."
                else
                    "Your app passcode is stored securely on this device and is separate from your website password.",
                color = Muted, fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun Dashboard(
    modifier: Modifier = Modifier,
    signedIn: Boolean,
    displayName: String,
    walletBalance: String?,
    walletLoading: Boolean,
    walletError: String,
    onFund: () -> Unit,
    onBuy: () -> Unit,
    onOrders: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onService: (ServicePage) -> Unit
) {
    var openingPage by remember { mutableStateOf<ServicePage?>(null) }
    LaunchedEffect(openingPage) {
        val page = openingPage
        if (page != null) {
            kotlinx.coroutines.delay(650)
            onService(page)
            openingPage = null
        }
    }
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "brandLogoSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "brandLogoRotation"
    )
    val quickLinks = listOf(
        Triple("Buy virtual number", "SMS verification", ServicePage.VirtualNumbers),
        Triple("Buy by QR number", "QR number options", ServicePage.QrNumbers),
        Triple("Buy US number", "US number options", ServicePage.UsNumbers),
        Triple("Rent a number", "Longer access", ServicePage.Rentals),
        Triple("Email OTP", "Email verification", ServicePage.EmailOtp),
        Triple("Proxy services", "Proxy marketplace", ServicePage.Proxies),
        Triple("Social media logs", "Available account listings", ServicePage.SocialMediaLogs),
        Triple("VTU services", "Airtime and data", ServicePage.VirtualTopUp)
    )
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    BrandMark()
                    Column(Modifier.weight(1f).padding(start = 11.dp)) {
                        Text("Nation Nums", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("Your numbers. One dashboard.", fontSize = 12.sp, color = Muted)
                    }
                    IconButton(onClick = onNotifications) { Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = NationBlue) }
                    IconButton(onClick = onProfile) { Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", tint = NationBlue) }
                }
            }
            item {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = NationBlue), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (signedIn) "WELCOME BACK" else "YOUR NATION NUMS ACCOUNT", color = Color.White.copy(alpha = .78f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(if (signedIn && displayName.isNotBlank()) "Hello, $displayName" else if (signedIn) "Your workspace is ready" else "Everything in one place", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)
                            }
                            Box(Modifier.size(52.dp).background(Color.White.copy(alpha = .15f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Wallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.height(19.dp))
                        Text("WALLET BALANCE", color = Color.White.copy(alpha = .76f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(if (signedIn && walletBalance != null) formatNairaAmount(walletBalance) else "₦ — — —", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Text(when {
                            !signedIn -> "Sign in to connect your live wallet"
                            walletLoading -> "Loading live wallet balance…"
                            walletBalance != null -> "Live balance from your wallet ledger"
                            walletError.isNotBlank() -> "Wallet balance could not be loaded. Open Wallet to retry."
                            else -> "No wallet balance record is available yet"
                        }, color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
                        Spacer(Modifier.height(17.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onFund, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NationBlue), shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f).height(47.dp)) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Fund wallet", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = onOrders, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .65f)), shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f).height(47.dp)) { Text("View orders", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
            item {
                Column {
                    Text("Quick access", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Choose a service. Each opens in its own dedicated page.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(11.dp))
                    quickLinks.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { (title, subtitle, page) ->
                                QuickAction(title, subtitle, when (page) {
                                    ServicePage.VirtualNumbers, ServicePage.QrNumbers, ServicePage.UsNumbers -> Icons.Rounded.PhoneAndroid
                                    ServicePage.Rentals -> Icons.Rounded.History
                                    ServicePage.EmailOtp -> Icons.Rounded.CheckCircle
                                    ServicePage.Proxies -> Icons.Rounded.Shield
                                    ServicePage.SocialMediaLogs -> Icons.Rounded.AccountCircle
                                    else -> Icons.Rounded.Wallet
                                }, { openingPage = page }, Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        if (openingPage != null) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .96f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painter = painterResource(id = R.drawable.ic_launcher_logo), contentDescription = "Nation Nums logo loading", modifier = Modifier.size(78.dp).graphicsLayer { rotationZ = rotation }, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(when (openingPage) { ServicePage.EmailOtp -> Color(0xFF7C3AED); ServicePage.Proxies -> Color(0xFF0F766E); ServicePage.SocialMediaLogs -> Color(0xFFDB2777); ServicePage.VirtualTopUp -> Color(0xFFEA580C); ServicePage.Rentals -> Color(0xFF2563EB); ServicePage.UsNumbers, ServicePage.QrNumbers -> Color(0xFF0891B2); else -> NationBlue }))
                    Spacer(Modifier.height(15.dp))
                    Text("Opening ${openingPage!!.title}…", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_logo),
        contentDescription = "Nation Nums logo",
        modifier = Modifier.size(46.dp)
    )
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
private fun ServicesScreen(
    modifier: Modifier = Modifier,
    numbers: List<VirtualNumberRecord>,
    serviceCatalog: List<ServiceCatalogRecord>,
    catalogLoading: Boolean,
    catalogError: String,
    numbersLoading: Boolean,
    numbersError: String,
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onOpen: (ServicePage) -> Unit,
    onRefresh: () -> Unit,
    hasMore: Boolean, onLoadMore: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }
    val services = listOf(
        Triple(ServicePage.VirtualNumbers, "Buy virtual numbers", "Choose a country and service"),
        Triple(ServicePage.QrNumbers, "Buy by QR number", "Explore supported QR-number options"),
        Triple(ServicePage.UsNumbers, "Buy US numbers", "Explore supported US options"),
        Triple(ServicePage.Rentals, "Rent a number", "Longer-use options where available"),
        Triple(ServicePage.EmailOtp, "Email OTP", "Email verification services"),
        Triple(ServicePage.Proxies, "Proxy services", "Available proxy products"),
        Triple(ServicePage.SocialMediaLogs, "Social media accounts", "Eligible listings supplied by YoungPG Media"),
        Triple(ServicePage.VirtualTopUp, "Virtual top-up", "Airtime, data and bill payments · Coming soon")
    )
    val filteredServices = services.filter {
        it.second.contains(query, ignoreCase = true) || it.third.contains(query, ignoreCase = true)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { PageHeader("Your numbers", "View virtual numbers linked to your account.") }
                OutlinedButton(onClick = onRefresh, enabled = isSignedIn && !numbersLoading, shape = RoundedCornerShape(10.dp)) { Text(if (numbersLoading) "Loading…" else "Refresh") }
            }
        }
        if (!isSignedIn) {
            item {
                EmptyState(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "Sign in to view your numbers",
                    body = "Your existing numbers will appear here after you sign in.",
                    action = "Sign in",
                    onAction = onSignIn
                )
            }
        } else if (numbersLoading) {
            item { Text("Loading your numbers…", color = Muted, fontSize = 13.sp) }
        } else if (numbersError.isNotBlank()) {
            item { InfoNotice(numbersError) }
        } else if (numbers.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "No numbers found",
                    body = "Numbers recorded by the backend will appear here.",
                    action = "Browse services",
                    onAction = { onOpen(ServicePage.VirtualNumbers) }
                )
            }
        } else {
            items(numbers, key = { it.id }) { number ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = NationBlue, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f).padding(start = 2.dp)) {
                                Text(number.phoneNumber.ifBlank { "Number unavailable" }, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(listOf(number.service, number.country).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Virtual number" }, color = Muted, fontSize = 12.sp)
                            }
                            Text(number.status.replaceFirstChar { it.uppercase() }, color = if (number.status.equals("active", true)) Green else Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (number.phoneNumber.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { clipboardManager.setText(AnnotatedString(number.phoneNumber)) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Copy number", fontSize = 11.sp)
                            }
                        }
                        if (number.expiresAt.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Expires: " + number.expiresAt, color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        item { Text("Buy a service", color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.padding(top = 10.dp)) }
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
        if (hasMore) item { OutlinedButton(onClick = onLoadMore, enabled = !numbersLoading, modifier = Modifier.fillMaxWidth()) { Text("Load more numbers") } }
    }
}

@Composable
private fun OrdersScreen(
    modifier: Modifier = Modifier,
    numbers: List<VirtualNumberRecord>,
    smsMessages: List<SmsMessageRecord>,
    isLoading: Boolean,
    errorMessage: String,
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit,
    hasMore: Boolean, onLoadMore: () -> Unit,
    backend: Base44Api, onAccountChanged: () -> Unit, onOpen: (ServicePage) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var filter by remember { mutableStateOf("All") }
    val visibleNumbers = numbers.filter { number ->
        when (filter) {
            "Active" -> number.status.equals("active", true) || number.status.equals("pending", true)
            "Completed" -> number.status.equals("completed", true) || number.status.equals("expired", true) || number.status.equals("cancelled", true)
            else -> true
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { PageHeader("Your orders", "Track numbers, activation status and received OTP messages."); if (isSignedIn) Row { TextButton(onClick = { onOpen(ServicePage.ProxyOrders) }) { Text("Proxy orders") }; TextButton(onClick = { onOpen(ServicePage.SocialOrders) }) { Text("Social accounts") } } }
                OutlinedButton(onClick = onRefresh, enabled = isSignedIn && !isLoading, shape = RoundedCornerShape(10.dp)) { Text(if (isLoading) "Loading…" else "Refresh") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
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
        }
        if (!isSignedIn) {
            item {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = "Sign in to view your orders",
                    body = "Your numbers and OTP messages are loaded from your Nation Nums account.",
                    action = "Sign in",
                    onAction = onSignIn
                )
            }
        } else if (isLoading) {
            item { Text("Loading your orders and OTP activity…", color = Muted, fontSize = 13.sp) }
        } else if (errorMessage.isNotBlank()) {
            item { InfoNotice(errorMessage) }
        } else {
            item { Text("NUMBERS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            if (visibleNumbers.isEmpty()) {
                item {
                    Text(
                        if (filter == "Active") "No active numbers found." else if (filter == "Completed") "No completed numbers found." else "No numbers recorded yet.",
                        color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(visibleNumbers, key = { "order-" + it.id }) { number ->
                    Card(
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = NationBlue)
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(number.phoneNumber.ifBlank { "Number unavailable" }, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(listOf(number.service, number.country).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Virtual number" }, color = Muted, fontSize = 12.sp)
                                }
                                Text(number.status.replaceFirstChar { it.uppercase() }, color = if (number.status.equals("active", true)) Green else Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (number.phoneNumber.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { clipboardManager.setText(AnnotatedString(number.phoneNumber)) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                    Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Copy number", fontSize = 11.sp)
                                }
                            }
                            NativeNumberActions(backend, number, onAccountChanged)
                            if (number.expiresAt.isNotBlank()) {
                                Spacer(Modifier.height(7.dp))
                                Text("Expires: " + number.expiresAt, color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            item { Text("RECEIVED OTP MESSAGES", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
            if (smsMessages.isEmpty()) {
                item { Text("No OTP messages recorded yet.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp)) }
            } else {
                items(smsMessages, key = { "sms-" + it.id }) { sms ->
                    Card(
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("OTP", color = NationBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(sms.code.ifBlank { "Code not detected" }, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                                if (sms.code.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { clipboardManager.setText(AnnotatedString(sms.code)) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text("Copy code", fontSize = 11.sp) }
                                }
                            }
                            if (sms.sender.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("From: " + sms.sender, color = Muted, fontSize = 11.sp)
                            }
                            if (sms.message.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(sms.message, color = Ink, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                            }
                            if (sms.createdAt.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(sms.createdAt, color = Muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        if (hasMore) item { OutlinedButton(onClick = onLoadMore, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text("Load more orders and SMS") } }
    }
}

@Composable
private fun WalletScreen(
    modifier: Modifier = Modifier,
    transactions: List<WalletTransactionRecord>,
    walletBalance: String?,
    isLoading: Boolean,
    errorMessage: String,
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit,
    hasMore: Boolean, onLoadMore: () -> Unit, backend: Base44Api
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { PageHeader("Wallet", "Manage your balance and funding activity.") }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = NationBlue)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("AVAILABLE BALANCE", color = Color.White.copy(alpha = .8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isSignedIn && walletBalance != null) formatNairaAmount(walletBalance) else "₦ — — —",
                        color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            !isSignedIn -> "Sign in to view account activity"
                            isLoading -> "Refreshing your wallet ledger…"
                            walletBalance != null -> "Balance from your Nation Nums wallet ledger"
                            errorMessage.isNotBlank() -> "Balance could not be loaded. Refresh to retry."
                            else -> "No balance snapshot is available yet"
                        },
                        color = Color.White.copy(alpha = .85f), fontSize = 12.sp
                    )
                }
            }
        }
        item { Text("Funding methods", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold) }
        if (isSignedIn) { item { NativeCheckout(backend, onRefresh) }; item { NativeBankTransfer(backend, onRefresh) } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("Recent transactions", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onRefresh, enabled = isSignedIn && !isLoading, shape = RoundedCornerShape(10.dp)) { Text(if (isLoading) "Loading…" else "Refresh") }
            }
        }
        if (!isSignedIn) {
            item { EmptyState(icon = Icons.Rounded.Wallet, title = "Sign in to view transactions", body = "Your transaction history is available after connecting your Nation Nums account.", action = "Sign in", onAction = onSignIn) }
            item { Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Text("Sign in securely") } }
        } else if (isLoading) {
            item { Text("Loading your transaction history…", color = Muted, fontSize = 13.sp) }
        } else if (errorMessage.isNotBlank()) {
            item { InfoNotice(errorMessage) }
        } else if (transactions.isEmpty()) {
            item { EmptyState(icon = Icons.Rounded.History, title = "No transactions found", body = "Transactions will appear here after they are recorded by the Nation Nums backend.", action = "Refresh", onAction = onRefresh) }
        } else {
            items(transactions) { transaction ->
                Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.History, contentDescription = null, tint = NationBlue) }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(transaction.description.ifBlank { transaction.type.replaceFirstChar { it.uppercase() } }, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${transaction.status.replaceFirstChar { it.uppercase() }} · ${transaction.createdAt.ifBlank { "Date unavailable" }}", color = Muted, fontSize = 11.sp)
                        }
                        Text(formatNairaAmount(transaction.amount), color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        if (hasMore) item { OutlinedButton(onClick = onLoadMore, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text("Load more transactions") } }
    }
}
@Composable
private fun ProfileScreen(
    modifier: Modifier = Modifier,
    user: Base44AuthApi.User?,
    checkingSession: Boolean,
    onOpen: (ServicePage) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
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
                        Text(
                            when {
                                checkingSession -> "Checking account…"
                                user != null -> user.username.ifBlank { user.email.ifBlank { "Nation Nums user" } }
                                else -> "Welcome to Nation Nums"
                            },
                            color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            when {
                                checkingSession -> "Validating your secure session"
                                user != null -> user.email
                                else -> "Sign in to connect your account"
                            },
                            color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (user != null) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Signed in", tint = Green)
                    }
                }
                if (user == null) {
                    Button(
                        onClick = onSignIn,
                        enabled = !checkingSession,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (checkingSession) "Checking session…" else "Sign in securely") }
                } else {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Sign out of this device") }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("ACCOUNT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        item { ServiceRow("Account details", "Name, username and WhatsApp verification", Icons.Rounded.AccountCircle) { onOpen(ServicePage.AccountDetails) } }
        item { ServiceRow("Security", "Password, sessions and biometric options", Icons.Rounded.Shield) { onOpen(ServicePage.Security) } }
        item { ServiceRow("Notifications", "Order and wallet updates", Icons.Rounded.Notifications) { onOpen(ServicePage.Notifications) } }
        item {
            Spacer(Modifier.height(15.dp))
            Text("TOOLS & SUPPORT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        item { ServiceRow("API key management", "Create, regenerate and revoke your keys", Icons.Rounded.Settings) { onOpen(ServicePage.ApiKeys) } }
        item { ServiceRow("Telegram notifications", "Link your account and manage alerts", Icons.Rounded.Notifications) { onOpen(ServicePage.Telegram) } }
        item { ServiceRow("API Documentation", "Open the official documentation website", Icons.Rounded.HelpOutline) { onOpen(ServicePage.Documentation) } }
        item { ServiceRow("Help & support", "Support tickets and contact options", Icons.Rounded.HelpOutline) { onOpen(ServicePage.Support) } }
        item { ServiceRow("FAQs", "Common questions", Icons.Rounded.HelpOutline) { onOpen(ServicePage.Faq) } }
        item { ServiceRow("Policies & app information", "Privacy policy, terms and version", Icons.Rounded.Settings) { onOpen(ServicePage.Legal) } }
    }
}

@Composable
private fun NotificationsScreen(
    modifier: Modifier = Modifier,
    notifications: List<NotificationRecord>,
    isLoading: Boolean,
    errorMessage: String,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit,
    hasMore: Boolean, onLoadMore: () -> Unit,
    pushStatus: String, onRead: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable(onClick = onBack)) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = NationBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("Back", color = NationBlue, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = onRefresh, enabled = isSignedIn && !isLoading, shape = RoundedCornerShape(10.dp)) {
                    Text(if (isLoading) "Loading…" else "Refresh")
                }
            }
            PageHeader("Notifications", "Updates recorded for your Nation Nums account.")
            if (pushStatus.isNotBlank()) Text(pushStatus, style = MaterialTheme.typography.bodySmall)
        }
        if (!isSignedIn) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Notifications,
                    title = "Sign in to view notifications",
                    body = "Account notifications appear here after sign-in.",
                    action = "Sign in",
                    onAction = onSignIn
                )
            }
        } else if (isLoading) {
            item { Text("Loading notifications…", color = Muted, fontSize = 13.sp) }
        } else if (errorMessage.isNotBlank()) {
            item { InfoNotice(errorMessage) }
        } else if (notifications.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Notifications,
                    title = "You're all caught up",
                    body = "New account updates will appear here when the backend records them.",
                    action = "Back",
                    onAction = onBack
                )
            }
        } else {
            items(notifications, key = { it.id }) { notice ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(40.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = NationBlue)
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(notice.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (!notice.isRead) TextButton(onClick = { onRead(notice.id) }) { Text("Mark as read") }
                            if (notice.body.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(notice.body, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                            }
                            if (notice.createdAt.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(notice.createdAt, color = Muted, fontSize = 10.sp)
                            }
                        }
                        if (!notice.isRead) {
                            Box(Modifier.size(8.dp).background(NationBlue, CircleShape))
                        }
                    }
                }
            }
        }
        if (hasMore) item { OutlinedButton(onClick = onLoadMore, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) { Text("Load more notifications") } }
    }
}

@Composable
private fun ServiceDetailPage(page: ServicePage, onBack: () -> Unit, onSignIn: () -> Unit, onSetupLock: () -> Unit, lockConfigured: Boolean, biometricAvailable: Boolean) {
    val context = LocalContext.current
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
                ServicePage.SocialMediaLogs -> "Only account listings actually supplied by YoungPG Media and legitimately transferable will be shown. Live catalogue, prices and purchase actions must be connected to the verified backend before use."
                ServicePage.VirtualTopUp -> "Nigerian VTU is coming soon. Airtime, data and bill payments will be available here once a supported provider is connected. No top-up order can be started yet, and nothing is charged to your wallet."
                ServicePage.Notifications -> "Your real notifications will appear after account and push-notification integration is completed."
                ServicePage.Security -> "Your account uses Nation Nums authentication. Password resets are available in Profile → Account details; the app passcode below protects this device only."
                ServicePage.Support -> "Support ticket history and replies will load after the existing support endpoints are verified."
                ServicePage.Team -> "Team members, invitations and permissions will be loaded from the existing Nation Nums account after team access is verified."
                ServicePage.QrNumbers -> "QR-number purchases are not offered by the connected backend. Use Buy virtual numbers or Buy US numbers for currently supported services."
                else -> "This option is not currently offered by the connected backend. No payment is accepted here."
            })
            Spacer(Modifier.height(18.dp))
        }
        if (page == ServicePage.Support) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Email customer support", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "Contact the Nation Nums support team at nationnums@gmail.com. Your email app will open with the support address filled in.",
                            color = Muted, fontSize = 13.sp, lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                val emailIntent = Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("mailto:nationnums@gmail.com?subject=Nation%20Nums%20Support")
                                )
                                runCatching { context.startActivity(emailIntent) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Email Nation Nums Support")
                        }
                    }
                }
            }
        }
        if (page == ServicePage.Security) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("App reauthentication", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            if (lockConfigured) "A device passcode is configured. You can change it here. " + (if (biometricAvailable) "Biometric authentication is supported on this device." else "Biometric authentication is not available on this device.")
                            else "Optional: create a device-only passcode and optionally enable biometrics. This is separate from your Nation Nums account password.",
                            color = Muted, fontSize = 13.sp, lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = onSetupLock, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(if (lockConfigured) "Change app passcode" else "Set up app passcode")
                        }
                    }
                }
            }
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
