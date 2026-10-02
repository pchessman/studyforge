package com.operationphoenix.game;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Operation Phoenix in a full-screen WebView. The whole game ships inside the APK and the app has no internet
 * permission, so it plays completely offline. On a TV the page is told so through the user agent, which switches
 * it to remote and controller mode; Xbox and other standard controllers reach the game through the Gamepad API.
 */
public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 7;
    private WebView web;
    private ValueCallback<Uri[]> pickCallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // save game, settings and progress
        s.setDatabaseEnabled(true);            // the DJ Booth keeps an uploaded track in IndexedDB
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);            // only to load the game from the APK's own assets
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setGeolocationEnabled(false);
        s.setSaveFormData(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setUserAgentString(s.getUserAgentString() + " OperationPhoenixApp" + (isTv() ? " AndroidTV SmartTV" : ""));

        // never navigate anywhere else: the game is the only page this app shows
        web.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) { return true; }
        });
        // lets the DJ Booth pick an audio file from the device
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pickCallback != null) pickCallback.onReceiveValue(null);
                pickCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("audio/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Choose a track"), PICK_AUDIO);
                } catch (Exception e) {
                    pickCallback = null;
                    return false;
                }
                return true;
            }
        });
        web.setBackgroundColor(0xFF2A3320);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        setContentView(web);
        web.requestFocus();
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/www/index.html");
        immersive();
    }

    private boolean isTv() {
        UiModeManager m = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        return m != null && m.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    private void immersive() {
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    // Back closes whatever is open in the game; on the main screen it leaves the app.
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.appBack ? window.appBack() : 'exit'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String result) {
                if (result == null || result.contains("exit")) finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == PICK_AUDIO && pickCallback != null) {
            Uri uri = (result == RESULT_OK && data != null) ? data.getData() : null;
            pickCallback.onReceiveValue(uri != null ? new Uri[] { uri } : null);
            pickCallback = null;
            return;
        }
        super.onActivityResult(request, result, data);
    }

    @Override
    protected void onPause() {
        web.evaluateJavascript("window.appPause && window.appPause()", null);
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        immersive();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
