package com.majid.browser

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import java.net.URLEncoder

class MainActivity : Activity() {
    private val bg = Color.rgb(16, 17, 27)
    private val panel = Color.rgb(30, 32, 48)
    private val purple = Color.rgb(111, 99, 245)
    private val white = Color.WHITE
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var webContainer: FrameLayout
    private lateinit var titleText: TextView
    private lateinit var tabsText: TextView
    private val tabs = mutableListOf<WebView>()
    private var active = 0
    private var incognito = false
    private val prefs by lazy { getSharedPreferences("majid_browser", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        newTab("https://www.google.com")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float = 14f, color: Int = white): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
        }

    private fun button(s: String, action: () -> Unit): TextView =
        text(s, 18f).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { action() }
            isClickable = true
        }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(12), dp(8))
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(text("MAJID", 11f, 0xFF9B91FF.toInt()).apply { gravity = Gravity.LEFT })
        titleText = text("Browser", 20f).apply { gravity = Gravity.LEFT; typeface = Typeface.DEFAULT_BOLD }
        brand.addView(titleText)
        tabsText = button("1") { showTabs() }
        header.addView(brand)
        header.addView(button("＋") { newTab("https://www.google.com") })
        header.addView(tabsText)
        root.addView(header)

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(5), dp(12), dp(8))
        }
        address = EditText(this).apply {
            hint = "Search or enter website"
            textSize = 15f
            isSingleLine = true
            setTextColor(white)
            setHintTextColor(0xFFB7B8C7.toInt())
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundColor(panel)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    loadInput(text.toString()); true
                } else false
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        }
        addressRow.addView(address)
        addressRow.addView(button("↻") { current()?.reload() })
        root.addView(addressRow)

        webContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        root.addView(webContainer)

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(panel)
        }
        nav.addView(button("‹") { goBack() }, weightParams())
        nav.addView(button("›") { current()?.let { if (it.canGoForward()) it.goForward() } }, weightParams())
        nav.addView(button("⌂") { newTab("https://www.google.com") }, weightParams())
        nav.addView(button("☆") { bookmark() }, weightParams())
        nav.addView(button("⋮") { showMenu() }, weightParams())
        root.addView(nav)
        setContentView(root)
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, dp(52), 1f)

    private fun current(): WebView? = tabs.getOrNull(active)

    private fun newTab(url: String) {
        val w = WebView(this)
        configure(w)
        tabs.add(w)
        active = tabs.lastIndex
        webContainer.removeAllViews()
        webContainer.addView(w, FrameLayout.LayoutParams(-1, -1))
        updateTabCount()
        w.loadUrl(url)
    }

    private fun configure(w: WebView) {
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.loadsImagesAutomatically = true
        w.settings.javaScriptCanOpenWindowsAutomatically = false
        w.settings.setSupportMultipleWindows(false)
        w.settings.cacheMode = WebSettings.LOAD_DEFAULT
        w.settings.builtInZoomControls = true
        w.settings.displayZoomControls = false
        w.settings.useWideViewPort = true
        w.settings.loadWithOverviewMode = true
        w.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                address.setText(url ?: "")
                titleText.text = view?.title?.takeIf { it.isNotBlank() } ?: "Loading…"
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                address.setText(url ?: "")
                titleText.text = view?.title?.takeIf { it.isNotBlank() } ?: "Majid Browser"
                if (!incognito && url != null) addHistory(url)
            }
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                if (url.startsWith("http://") || url.startsWith("https://")) return false
                return try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))); true } catch (_: Exception) { true }
            }
        }
        w.webChromeClient = WebChromeClient()
        w.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = android.app.DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType))
                (getSystemService(DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(request)
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this, "Download could not start", Toast.LENGTH_SHORT).show() }
        })
        if (incognito) {
            w.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            w.clearCache(true)
            w.clearHistory()
            CookieManager.getInstance().setAcceptCookie(false)
            CookieManager.getInstance().setAcceptThirdPartyCookies(w, false)
        }
    }

    private fun loadInput(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val url = if (value.contains(" ") || (!value.contains(".") && !value.startsWith("http"))) {
            "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8")
        } else if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
        current()?.loadUrl(url)
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(address.windowToken, 0)
        current()?.requestFocus()
    }

    private fun updateTabCount() { tabsText.text = tabs.size.toString() }

    private fun showTabs() {
        if (tabs.isEmpty()) return
        val names = tabs.mapIndexed { i, w -> "${i + 1}. ${w.title?.takeIf { it.isNotBlank() } ?: w.url ?: "New tab"}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Open tabs")
            .setItems(names) { _, which -> switchTab(which) }
            .setPositiveButton("New tab") { _, _ -> newTab("https://www.google.com") }
            .setNegativeButton("Close", null).show()
    }

    private fun switchTab(index: Int) {
        if (index !in tabs.indices) return
        active = index
        webContainer.removeAllViews()
        webContainer.addView(tabs[active], FrameLayout.LayoutParams(-1, -1))
        address.setText(current()?.url ?: "")
        titleText.text = current()?.title ?: "Browser"
        updateTabCount()
    }

    private fun closeCurrentTab() {
        if (tabs.size <= 1) { current()?.loadUrl("https://www.google.com"); return }
        tabs.removeAt(active).destroy()
        active = (active - 1).coerceAtLeast(0)
        switchTab(active)
    }

    private fun goBack() {
        val w = current() ?: return
        if (w.canGoBack()) w.goBack() else Toast.makeText(this, "No previous page", Toast.LENGTH_SHORT).show()
    }

    private fun addHistory(url: String) {
        val old = prefs.getString("history", "") ?: ""
        val lines = (listOf(url) + old.lines().filter { it.isNotBlank() && it != url }).take(100)
        prefs.edit().putString("history", lines.joinToString("\n")).apply()
    }

    private fun bookmark() {
        val url = current()?.url ?: return
        val old = prefs.getString("bookmarks", "") ?: ""
        val lines = (old.lines().filter { it.isNotBlank() } + url).distinct().take(200)
        prefs.edit().putString("bookmarks", lines.joinToString("\n")).apply()
        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
    }

    private fun showList(title: String, key: String) {
        val items = (prefs.getString(key, "") ?: "").lines().filter { it.isNotBlank() }.toTypedArray()
        if (items.isEmpty()) {
            AlertDialog.Builder(this).setTitle(title).setMessage("Nothing saved yet").setPositiveButton("OK", null).show()
            return
        }
        AlertDialog.Builder(this).setTitle(title).setItems(items) { _, which -> newTab(items[which]) }
            .setPositiveButton("Clear") { _, _ -> prefs.edit().remove(key).apply() }
            .setNegativeButton("Close", null).show()
    }

    private fun showMenu() {
        val items = arrayOf(
            "Bookmarks", "History", "New incognito tab", "Normal browsing",
            "Desktop site: toggle", "Close current tab", "Share page", "Clear browsing data"
        )
        AlertDialog.Builder(this).setTitle("Majid Browser").setItems(items) { _, which ->
            when (which) {
                0 -> showList("Bookmarks", "bookmarks")
                1 -> showList("History", "history")
                2 -> {
                    incognito = true
                    newTab("https://www.google.com")
                    Toast.makeText(this, "Incognito tab opened (basic mode)", Toast.LENGTH_SHORT).show()
                }
                3 -> {
                    incognito = false
                    CookieManager.getInstance().setAcceptCookie(true)
                    newTab("https://www.google.com")
                }
                4 -> current()?.let {
                    it.settings.userAgentString = if (it.settings.userAgentString.contains("Mobile")) {
                        it.settings.userAgentString.replace(" Mobile", "")
                    } else it.settings.userAgentString + " Desktop"
                    it.reload()
                }
                5 -> closeCurrentTab()
                6 -> current()?.url?.let { url ->
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url)
                    }, "Share page"))
                }
                7 -> {
                    prefs.edit().remove("history").remove("bookmarks").apply()
                    tabs.forEach { it.clearHistory(); it.clearCache(true) }
                    Toast.makeText(this, "Local history and bookmarks cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }.show()
    }

    override fun onBackPressed() {
        val w = current()
        if (w != null && w.canGoBack()) w.goBack() else if (tabs.size > 1) closeCurrentTab() else super.onBackPressed()
    }

    override fun onDestroy() {
        tabs.forEach { it.stopLoading(); it.destroy() }
        tabs.clear()
        super.onDestroy()
    }
}
