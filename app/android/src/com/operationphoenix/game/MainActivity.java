package com.operationphoenix.game;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * Operation Phoenix in a full-screen WebView. The whole game ships inside the APK; the internet permission is
 * used only for online play with a friend (a direct peer-to-peer connection the player starts). On a TV the page
 * is told so through the user agent, which switches it to remote and controller mode; Xbox and other standard
 * controllers reach the game through the Gamepad API.
 *
 * Targets Android 16 (API 36): back goes through OnBackInvokedCallback on Android 13+ and onBackPressed below
 * that, the layout is edge to edge with the game kept clear of camera cutouts, and a crashed or reclaimed WebView
 * renderer is rebuilt instead of taking the app down with it.
 */
public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 7;
    private static final String HOME = "file:///android_asset/www/index.html";
    private FrameLayout root;
    private WebView web;
    private ValueCallback<Uri[]> pickCallback;
    private Object backCallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(lp);
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF2A3320);
        // keep buttons out from under a camera notch or hole punch; the bars themselves stay hidden
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                int l = 0, t = 0, r = 0, b = 0;
                if (Build.VERSION.SDK_INT >= 30) {
                    Insets c = in.getInsets(WindowInsets.Type.displayCutout());
                    l = c.left; t = c.top; r = c.right; b = c.bottom;
                } else if (Build.VERSION.SDK_INT >= 28) {
                    DisplayCutout c = in.getDisplayCutout();
                    if (c != null) { l = c.getSafeInsetLeft(); t = c.getSafeInsetTop(); r = c.getSafeInsetRight(); b = c.getSafeInsetBottom(); }
                }
                v.setPadding(l, t, r, b);
                return in;
            }
        });
        setContentView(root);
        makeWeb(state);
        immersive();

        if (Build.VERSION.SDK_INT >= 33) {
            OnBackInvokedCallback cb = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() { goBack(); }
            };
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
            backCallback = cb;
        }
    }

    private void makeWeb(Bundle state) {
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // save game, settings and progress
        s.setDatabaseEnabled(true);            // the DJ Booth keeps an uploaded track in IndexedDB
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);           // assets still load; nothing else on the device is reachable
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setGeolocationEnabled(false);
        s.setSaveFormData(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setLoadWithOverviewMode(false);   // never zoom out to fit: the page sizes itself to the screen
        s.setUseWideViewPort(true);
        s.setTextZoom(100);                    // the game sizes its own text; system font scaling would break the layout
        if (Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(false); // no web pages are ever visited
        s.setUserAgentString(s.getUserAgentString() + " OperationPhoenixApp" + (isTv() ? " AndroidTV SmartTV" : ""));

        // never navigate anywhere else: the game is the only page this app shows
        web.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) { return true; }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) { return true; }

            // The renderer crashed or Android reclaimed it for memory. Returning false here kills the whole app,
            // so throw the dead view away and start a fresh one; progress is already saved in local storage.
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (view == web) {
                    dropWeb();
                    makeWeb(null);
                    immersive();
                } else {
                    ((ViewGroup) view.getParent()).removeView(view);
                    view.destroy();
                }
                return true;
            }
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
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.requestFocus();
        if (state == null || web.restoreState(state) == null) web.loadUrl(HOME);
    }

    private void dropWeb() {
        if (web == null) return;
        WebView old = web;
        web = null;
        if (pickCallback != null) { pickCallback.onReceiveValue(null); pickCallback = null; }
        root.removeView(old);
        old.stopLoading();
        old.setWebChromeClient(null);
        old.destroy();
    }

    private boolean isTv() {
        UiModeManager m = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        return m != null && m.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else if (web != null) {
            web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    // Back closes whatever is open in the game; on the main screen it leaves the app.
    private void goBack() {
        if (web == null) { finish(); return; }
        web.evaluateJavascript("window.appBack ? window.appBack() : 'exit'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String result) {
                if (result == null || result.contains("exit")) finish();
            }
        });
    }

    // Android 12 and older (and 13-15, where the callback above is not switched on for this app)
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() { goBack(); }

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
        if (web != null) {
            web.evaluateJavascript("window.appPause && window.appPause()", null);
            web.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        immersive();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (web != null && level >= TRIM_MEMORY_RUNNING_LOW) web.evaluateJavascript("window.appTrim && window.appTrim()", null);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) backCallback);
            backCallback = null;
        }
        dropWeb();
        super.onDestroy();
    }
}
