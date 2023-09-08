package com.gbs_soft.galib_web.helper;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gbs_soft.galib_web.controller.MyControl;
import com.gbs_soft.galib_web.controller.MyMethods;

import java.net.URISyntaxException;

public class HelloWebViewClient extends WebViewClient {

    Activity activity;
    static MyHelper myHelper;

    public HelloWebViewClient(MyHelper myHelper) {
        HelloWebViewClient.myHelper = myHelper;
    }

    public HelloWebViewClient(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

        if (!MyMethods.isConnected(activity)) {
            myHelper.finishLoading();
        } else {
            myHelper.loading();
        }

        if (url.startsWith("http")) return false;//open web links as usual
        //try to find browse activity to handle uri
        Uri parsedUri = Uri.parse(url);
        PackageManager packageManager = activity.getPackageManager();
        Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);
        if (browseIntent.resolveActivity(packageManager) != null) {
            activity.startActivity(browseIntent);
            return true;
        }
        //if not activity found, try to parse intent://
        if (url.startsWith("intent:")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(intent);
                    return true;
                }

                //try to find fallback url
                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                if (fallbackUrl != null) {
                    myHelper.webLoadUrl(fallbackUrl);
                    return true;
                }

                //invite to install
                Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(
                        Uri.parse("market://details?id=" + intent.getPackage()));
                if (marketIntent.resolveActivity(packageManager) != null) {
                    activity.startActivity(marketIntent);
                    return true;
                }
            } catch (URISyntaxException e) {
                //not an intent uri
            }
        }

        return true;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String url) {
        MyControl.LOAD_ERROR_REASON = description;

        if (MyControl.NETWORK_AVAILABLE) {
            //We have network connection. So why failed? Lets catch it.
            MyControl.FAILED_FOR_OTHER_REASON = true;

        }
        myHelper.errorLoading();

    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // do your stuff here

    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        MyControl.FAILED_FOR_OTHER_REASON = false;
    }
} // HelloWebViewClient end here ======================
