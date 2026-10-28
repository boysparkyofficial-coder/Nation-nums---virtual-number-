package com.nationnums.app

import android.os.Bundle
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    fun clearRelockFlag() { shouldLockOnResume = false }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isFinishing) shouldLockOnResume = true
    }

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
                NationNumsApp(this@MainActivity)
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

private enum class GateState { Setup, Locked, Dashboard }

@Composable
private fun NationNumsApp(activity: MainActivity) {
    val lockStore = remember { AppLockStore(activity) }
    var gateState by remember {
        mutableStateOf(if (lockStore.isConfigured()) GateState.Locked else GateState.Setup)
    }
    var gateError by remember { mutableStateOf("") }
    val biometricAvailable = remember {
        BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(activity.shouldLockOnResume) {
        if (activity.shouldLockOnResume) {
            if (gateState == GateState.Dashboard) {
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
        var showSignIn by remember { mutableStateOf(false) }
        val sessionStore = remember { Base44SessionStore(activity) }
        val authApi = remember { Base44AuthApi(sessionStore) }
        var signedInUser by remember { mutableStateOf<Base44AuthApi.User?>(null) }
        var checkingSession by remember { mutableStateOf(true) }
        var walletTransactions by remember { mutableStateOf<List<WalletTransactionRecord>>(emptyList()) }
        var walletLoading by remember { mutableStateOf(false) }
        var walletError by remember { mutableStateOf("") }
        var walletRefresh by remember { mutableStateOf(0) }
        var virtualNumbers by remember { mutableStateOf<List<VirtualNumberRecord>>(emptyList()) }
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
        val repository = remember { NationNumsRepository { sessionStore.readToken() } }

        LaunchedEffect(signedInUser?.id, selectedTab, selectedPage, notificationsRefresh, numbersRefresh, ordersRefresh, walletRefresh) {
            if (signedInUser != null && selectedPage == ServicePage.Notifications) {
                notificationsLoading = true
                notificationsError = ""
                repository.loadNotifications { result, records ->
                    notificationsLoading = false
                    notifications = records
                    notificationsError = if (result.success) "" else result.error ?: "Could not load notifications."
                }
            }
            if (signedInUser != null && selectedTab == AppTab.Numbers) {
                numbersLoading = true
                numbersError = ""
                repository.loadVirtualNumbers { result, records ->
                    numbersLoading = false
                    virtualNumbers = records
                    numbersError = if (result.success) "" else result.error ?: "Could not load your numbers."
                }
            }
            if (signedInUser != null && selectedTab == AppTab.Wallet) {
                walletLoading = true
                walletError = ""
                repository.loadWalletTransactions { result, records ->
                    walletLoading = false
                    walletTransactions = records
                    walletError = if (result.success) "" else result.error ?: "Could not load transactions."
                }
            }
            if (signedInUser != null && selectedTab == AppTab.Orders) {
                ordersLoading = true
                ordersError = ""
                var numberResult: Base44Api.Result? = null
                var smsResult: Base44Api.Result? = null
                var loadedNumbers: List<VirtualNumberRecord> = emptyList()
                var loadedMessages: List<SmsMessageRecord> = emptyList()
                fun finishIfReady() {
                    if (numberResult != null && smsResult != null) {
                        ordersLoading = false
                        virtualNumbers = loadedNumbers
                        smsMessages = loadedMessages
                        ordersError = listOfNotNull(
                            numberResult?.takeIf { !it.success }?.error,
                            smsResult?.takeIf { !it.success }?.error
                        ).firstOrNull().orEmpty()
                    }
                }
                repository.loadVirtualNumbers { result, records ->
                    numberResult = result
                    loadedNumbers = records
                    finishIfReady()
                }
                repository.loadSmsMessages { result, records ->
                    smsResult = result
                    loadedMessages = records
                    finishIfReady()
                }
            }
        }

        LaunchedEffect(Unit) {
            authApi.currentUser { result ->
                signedInUser = if (result.success) result.user else null
                checkingSession = false
            }
        }

        BackHandler(enabled = selectedPage != null) {
            selectedPage = null
        }

        if (showSignIn) {
            SignInScreen(
                onBack = { showSignIn = false },
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
                    onRefresh = { notificationsRefresh++ }
                )
            } else if (selectedPage != null) {
                ServiceDetailPage(
                    page = selectedPage!!,
                    onBack = { selectedPage = null },
                    onSignIn = { showSignIn = true }
                )
            } else {
                when (selectedTab) {
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
                        numbers = virtualNumbers,
                        numbersLoading = numbersLoading,
                        numbersError = numbersError,
                        isSignedIn = signedInUser != null,
                        onSignIn = { showSignIn = true },
                        onRefresh = { numbersRefresh++ },
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
                        onRefresh = { ordersRefresh++ }
                    )
                    AppTab.Wallet -> WalletScreen(
                        modifier = Modifier.padding(padding),
                        transactions = walletTransactions,
                        isLoading = walletLoading,
                        errorMessage = walletError,
                        isSignedIn = signedInUser != null,
                        onSignIn = { showSignIn = true },
                        onRefresh = { walletRefresh++ }
                    )
                    AppTab.Profile -> ProfileScreen(
                        modifier = Modifier.padding(padding),
                        user = signedInUser,
                        checkingSession = checkingSession,
                        onOpen = { selectedPage = it },
                        onSignIn = { showSignIn = true },
                        onSignOut = {
                            authApi.logout()
                            signedInUser = null
                            showSignIn = false
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
    onLogin: (String, String, Boolean, (String?) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(Canvas).verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onBack)) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = NationBlue)
            Spacer(Modifier.width(8.dp))
            Text("Back to app", color = NationBlue, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(28.dp))
        BrandMark()
        Spacer(Modifier.height(18.dp))
        Text("Sign in to Nation Nums", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Use the same email and password you use on the Nation Nums website.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it; message = "" },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Email address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; message = "" },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { rememberMe = !rememberMe }) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Column {
                Text("Remember me", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (rememberMe) "Keep the encrypted session on this device." else "Session stays in memory and is not saved after closing the app.", color = Muted, fontSize = 11.sp)
            }
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFFB42318), fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = "Enter your email and password."
                } else {
                    busy = true
                    message = ""
                    onLogin(email, password, rememberMe) { error ->
                        busy = false
                        message = error.orEmpty()
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (busy) "Signing in…" else "Sign in securely", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Text("Your password is sent only to the Nation Nums Base44 sign-in endpoint over HTTPS. The app does not create or modify wallet records directly.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
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
    Image(
        painter = painterResource(id = R.drawable.ic_launcher),
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
    numbersLoading: Boolean,
    numbersError: String,
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onOpen: (ServicePage) -> Unit,
    onRefresh: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
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
        item {
            Spacer(Modifier.height(8.dp))
            InfoNotice("Live availability and pricing will be loaded from Nation Nums after the provider and backend contracts are connected.")
        }
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
    onRefresh: () -> Unit
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
                Column(Modifier.weight(1f)) { PageHeader("Your orders", "Track numbers, activation status and received OTP messages.") }
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
        item { InfoNotice("This page is read-only. Number purchases, rentals, OTP requests and cancellations must be completed through verified Nation Nums backend operations.") }
    }
}

@Composable
private fun WalletScreen(
    modifier: Modifier = Modifier,
    transactions: List<WalletTransactionRecord>,
    isLoading: Boolean,
    errorMessage: String,
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { PageHeader("Wallet", "Manage your balance and funding activity.") }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = NationBlue)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("AVAILABLE BALANCE", color = Color.White.copy(alpha = .8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Not available", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(if (isSignedIn) "Balance display needs a verified balance endpoint" else "Sign in to view account activity", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                }
            }
        }
        item { Text("Funding methods", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold) }
        item { ServiceRow("Nation Flow", "Virtual account funding · connection pending", Icons.Rounded.Wallet) { } }
        item { ServiceRow("NationCheckout", "Online payment · connection pending", Icons.Rounded.Add) { } }
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
            item { EmptyState(icon = Icons.Rounded.History, title = "No transactions found", body = "Transactions will appear here after they are recorded by the Nation Nums backend.", action = "Refresh later", onAction = {}) }
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
        item { InfoNotice("Payments are not initiated from this screen. Wallet credits must be confirmed by the existing backend; the app never writes wallet transactions directly.") }
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
        item { ServiceRow("Security", "Password, sessions and biometric options", Icons.Rounded.Shield) { onOpen(ServicePage.Security) } }
        item { ServiceRow("Notifications", "Order and wallet updates", Icons.Rounded.Notifications) { onOpen(ServicePage.Notifications) } }
        item { ServiceRow("Telegram settings", "Linking and supported alerts", Icons.Rounded.Settings) { onOpen(ServicePage.Telegram) } }
        item {
            Spacer(Modifier.height(15.dp))
            Text("TOOLS & SUPPORT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        item { ServiceRow("API key management", "Manage supported API access", Icons.Rounded.Shield) { onOpen(ServicePage.ApiKeys) } }
        item { ServiceRow("Team management", "Team members and access permissions", Icons.Rounded.AccountCircle) { onOpen(ServicePage.Team) } }
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
    onRefresh: () -> Unit
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
        item { InfoNotice("This screen is read-only. Mark-as-read actions will be added only through a verified backend operation.") }
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
                        "The app-only password or biometric unlock protects this device. Live account access will be connected to the existing Base44 authentication flow.",
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
