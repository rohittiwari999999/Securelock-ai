package com.securelock.ai.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.theme.AmberWarn
import com.securelock.ai.ui.theme.CrispWhite
import com.securelock.ai.ui.theme.DeepNavy
import com.securelock.ai.ui.theme.MidnightBlue
import com.securelock.ai.ui.theme.SkyAccent
import java.net.URLEncoder
import java.util.UUID

private data class BrowserTabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Private Tab",
    val url: String = "https://duckduckgo.com"
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivateBrowserScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appSettings by repository.settings.collectAsState()

    val tabs = remember { mutableStateListOf(BrowserTabState()) }
    var activeTabIndex by remember { mutableIntStateOf(0) }
    val activeTab = tabs.getOrElse(activeTabIndex) { BrowserTabState() }

    var addressInput by remember(activeTab.id) { mutableStateOf(activeTab.url) }
    var loadProgress by remember { mutableIntStateOf(100) }
    var insecureWarningUrl by remember { mutableStateOf<String?>(null) }
    var pendingDownload by remember { mutableStateOf<Pair<String, String>?>(null) }

    val webView = remember(context) {
        WebView(context).apply {
            this.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = false
                databaseEnabled = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                setSupportMultipleWindows(false)
                safeBrowsingEnabled = true
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompat.setSafeBrowsingEnabled(this.settings, true)
            }
            CookieManager.getInstance().setAcceptCookie(false)
        }
    }

    fun wipePrivateSessionData() {
        webView.clearHistory()
        webView.clearCache(true)
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }

    fun navigateToInput(rawInput: String) {
        val trimmed = rawInput.trim()
        val resolvedUrl = when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }
        if (appSettings.safeBrowsingWarningsEnabled && resolvedUrl.startsWith("http://")) {
            insecureWarningUrl = resolvedUrl
        } else {
            tabs[activeTabIndex] = tabs[activeTabIndex].copy(url = resolvedUrl)
            addressInput = resolvedUrl
            webView.loadUrl(resolvedUrl)
        }
    }

    DisposableEffect(webView) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (appSettings.safeBrowsingWarningsEnabled && url.startsWith("http://")) {
                    insecureWarningUrl = url
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val finalUrl = url ?: return
                addressInput = finalUrl
                val title = view?.title?.takeIf { it.isNotBlank() } ?: "Private Tab"
                if (activeTabIndex in tabs.indices) {
                    tabs[activeTabIndex] = tabs[activeTabIndex].copy(title = title, url = finalUrl)
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadProgress = newProgress
            }
        }
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
            pendingDownload = url to fileName
        }
        webView.loadUrl(activeTab.url)

        onDispose {
            wipePrivateSessionData()
            webView.destroy()
        }
    }

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            SecureTopBar(
                title = "Private Browser",
                subtitle = "Zero local history · Cookies disabled",
                onBack = {
                    wipePrivateSessionData()
                    onBack()
                },
                actions = {
                    IconButton(onClick = {
                        wipePrivateSessionData()
                        webView.loadUrl("https://duckduckgo.com")
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear browsing data")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightBlue)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    placeholder = { Text("Search DuckDuckGo or enter HTTPS URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CrispWhite,
                        unfocusedContainerColor = CrispWhite,
                        focusedTextColor = DeepNavy,
                        unfocusedTextColor = DeepNavy
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { navigateToInput(addressInput) }) {
                    Text("Go")
                }
            }

            if (loadProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepNavy)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(tabs) { index, tab ->
                    FilterChip(
                        selected = index == activeTabIndex,
                        onClick = {
                            activeTabIndex = index
                            addressInput = tab.url
                            webView.loadUrl(tab.url)
                        },
                        label = { Text(tab.title.take(16)) },
                        trailingIcon = if (tabs.size > 1) {
                            {
                                IconButton(
                                    onClick = {
                                        tabs.removeAt(index)
                                        activeTabIndex = (activeTabIndex).coerceAtMost(tabs.lastIndex)
                                        val next = tabs[activeTabIndex]
                                        addressInput = next.url
                                        webView.loadUrl(next.url)
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close tab")
                                }
                            }
                        } else null
                    )
                }
                item {
                    IconButton(onClick = {
                        tabs.add(BrowserTabState())
                        activeTabIndex = tabs.lastIndex
                        val next = tabs[activeTabIndex]
                        addressInput = next.url
                        webView.loadUrl(next.url)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New private tab", tint = SkyAccent)
                    }
                }
            }

            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Surface(color = MidnightBlue, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { if (webView.canGoBack()) webView.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CrispWhite)
                    }
                    IconButton(onClick = { if (webView.canGoForward()) webView.goForward() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = CrispWhite)
                    }
                    IconButton(onClick = { webView.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = CrispWhite)
                    }
                }
            }
        }
    }

    if (insecureWarningUrl != null) {
        val url = insecureWarningUrl!!
        AlertDialog(
            onDismissRequest = { insecureWarningUrl = null },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberWarn) },
            title = { Text("Unencrypted HTTP Connection") },
            text = {
                Text("This URL uses unencrypted HTTP ($url) instead of HTTPS. Continue only if you trust the destination.")
            },
            confirmButton = {
                Button(onClick = {
                    insecureWarningUrl = null
                    tabs[activeTabIndex] = tabs[activeTabIndex].copy(url = url)
                    addressInput = url
                    webView.loadUrl(url)
                }) {
                    Text("Proceed Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { insecureWarningUrl = null }) {
                    Text("Go Back")
                }
            }
        )
    }

    if (pendingDownload != null) {
        val (downloadUrl, fileName) = pendingDownload!!
        AlertDialog(
            onDismissRequest = { pendingDownload = null },
            title = { Text("Confirm Download") },
            text = { Text("Save $fileName to your device Downloads folder?") },
            confirmButton = {
                Button(onClick = {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val req = DownloadManager.Request(Uri.parse(downloadUrl))
                        .setTitle(fileName)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    runCatching { dm.enqueue(req) }
                    pendingDownload = null
                }) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDownload = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
