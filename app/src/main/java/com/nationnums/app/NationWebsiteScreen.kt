package com.nationnums.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONArray

private const val NATION_SITE = "https://nation-nums.base44.app"

@Composable
internal fun NationWebsiteScreen(
    activity: MainActivity,
    sessionStore: Base44SessionStore
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableStateOf(0) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var lastSessionToken by remember { mutableStateOf(sessionStore.readToken().orEmpty()) }
    val pushRegistration = remember { AndroidPushRegistration(activity, sessionStore) }
    var biometricBridge by remember { mutableStateOf<NationNumsBiometricBridge?>(null) }

    var pendingFileCallback by remember { mutableStateOf<android.webkit.ValueCallback<Array<Uri>>?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = pendingFileCallback
        pendingFileCallback = null
        val uris = if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            when {
                data?.clipData != null -> Array(data.clipData!!.itemCount) { index -> data.clipData!!.getItemAt(index).uri }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else null
        callback?.onReceiveValue(uris)
    }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
        canGoBack = webView?.canGoBack() == true
    }

    LaunchedEffect(activity.notificationDestination, webView) {
        val destination = activity.notificationDestination
        val view = webView
        if (!destination.isNullOrBlank() && view != null) {
            val safe = runCatching { Uri.parse(destination) }.getOrNull()
            if (safe?.scheme == "https" && safe.host.equals("nation-nums.base44.app", true)) {
                view.loadUrl(safe.toString())
            }
            activity.consumeNotification()
        }
    }

    // Base44's website stores its authenticated access token in localStorage.
    // Read it only from the first-party website origin so native FCM registration
    // can use the same customer session without introducing another account store.
    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        while (true) {
            if (pageLoaded && view.url?.startsWith(NATION_SITE) == true) {
                view.evaluateJavascript("(function(){return localStorage.getItem('base44_access_token') || '';})()") { encoded ->
                    val token = runCatching { JSONArray("[$encoded]").optString(0) }.getOrDefault("")
                    if (token != lastSessionToken) {
                        lastSessionToken = token
                        if (token.isBlank()) {
                            sessionStore.clear()
                        } else {
                            runCatching { sessionStore.saveToken(token, rememberMe = true) }
                            if (Build.VERSION.SDK_INT < 33 ||
                                ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                pushRegistration.sync()
                            } else {
                                activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 7401)
                                pushRegistration.sync()
                            }
                        }
                    } else if (token.isNotBlank() && sessionStore.readToken().isNullOrBlank()) {
                        runCatching { sessionStore.saveToken(token, rememberMe = true) }
                        pushRegistration.sync()
                    }
                }
            }
            delay(2000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            webView?.removeJavascriptInterface("NationNative")
            webView?.removeJavascriptInterface("NationNumsNative")
            webView?.removeJavascriptInterface("NativeApp")
            webView?.removeJavascriptInterface("Android")
            biometricBridge = null
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    NationNumsBiometricBridge(activity, this).also { bridge ->
                        biometricBridge = bridge
                        // The Base44 website's AppLockGate expects this exact bridge name.
                        addJavascriptInterface(bridge, "NationNative")
                        addJavascriptInterface(bridge, "NationNumsNative")
                        addJavascriptInterface(bridge, "NativeApp")
                        addJavascriptInterface(bridge, "Android")
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.allowFileAccess = false
                    settings.allowContentAccess = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.safeBrowsingEnabled = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            pageError = null
                            pageLoaded = false
                            progress = 5
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            progress = 100
                            pageLoaded = url?.startsWith(NATION_SITE) == true
                            // Let the first-party website distinguish the installed native app
                            // from a normal browser and suppress its app-download promotion.
                            if (pageLoaded) {
                                view?.evaluateJavascript("window.__NATION_NUMS_NATIVE_APP__ = true; document.documentElement.setAttribute('data-nation-native-app', 'true');", null)
                            }
                            canGoBack = view?.canGoBack() == true
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return true
                            if (uri.scheme.equals("https", true) && uri.host.equals("nation-nums.base44.app", true)) return false
                            if (uri.scheme.equals("https", true) && uri.host.equals("accounts.google.com", true)) {
                                runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                return true
                            }
                            if (uri.scheme.equals("https", true) || uri.scheme.equals("mailto", true) || uri.scheme.equals("tel", true)) {
                                runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                return true
                            }
                            return true
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                pageLoaded = false
                                pageError = "We couldn't connect to Nation Nums. Check your internet connection and try again."
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }

                        override fun onShowFileChooser(
                            view: WebView?,
                            filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            pendingFileCallback?.onReceiveValue(null)
                            pendingFileCallback = filePathCallback
                            val accept = fileChooserParams?.acceptTypes
                                ?.filter { it.isNotBlank() }
                                ?.joinToString(",")
                                ?.ifBlank { "*/*" } ?: "*/*"
                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = if (accept.contains(",")) "*/*" else accept
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                            }
                            return try {
                                filePicker.launch(intent)
                                true
                            } catch (_: Exception) {
                                pendingFileCallback?.onReceiveValue(null)
                                pendingFileCallback = null
                                false
                            }
                        }
                    }

                    setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        val uri = runCatching { Uri.parse(url) }.getOrNull()
                        if (uri?.scheme == "https") {
                            runCatching {
                                val request = DownloadManager.Request(uri)
                                    .setMimeType(mimeType)
                                    .addRequestHeader("User-Agent", userAgent)
                                    .setTitle(android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType))
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType))
                                (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                            }.onFailure {
                                pageError = "The download could not be started. Please try again."
                            }
                        }
                    })

                    val incomingLink = activity.intent?.data
            val safeIncomingLink = incomingLink?.takeIf {
                it.scheme.equals("https", true) && it.host.equals("nation-nums.base44.app", true)
            }
            loadUrl(safeIncomingLink?.toString() ?: NATION_SITE)
                }
            },
            update = { view ->
                if (webView !== view) webView = view
            }
        )

        if (progress < 100 && pageError == null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_logo),
                        contentDescription = "Nation Nums",
                        modifier = Modifier.fillMaxWidth(0.42f)
                    )
                    CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.primary)
                    Text("Loading Nation Nums…", modifier = Modifier.padding(top = 14.dp), color = Color(0xFF52627A))
                }
            }
        }

        if (pageError != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("You're offline", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF14213D))
                    Text(pageError.orEmpty(), modifier = Modifier.padding(top = 10.dp), color = Color(0xFF52627A))
                    Button(onClick = {
                        pageError = null
                        webView?.reload()
                    }, modifier = Modifier.padding(top = 20.dp)) { Text("Try again") }
                }
            }
        }
    }
}
