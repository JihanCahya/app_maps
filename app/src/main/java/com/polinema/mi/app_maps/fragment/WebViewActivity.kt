package com.polinema.mi.app_maps.fragment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityWebViewBinding

class WebViewActivity : Fragment() {

    private lateinit var b: ActivityWebViewBinding
    lateinit var thisParent: BaseActivity
    lateinit var v: View
    private lateinit var webView: WebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisParent = activity as BaseActivity
        b = ActivityWebViewBinding.inflate(layoutInflater)
        v = b.root

        webView = b.webView
        webView.webViewClient = WebViewClient()
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webView.loadUrl("https://instagram.com/barokahjayamulia")

        return v
    }


}