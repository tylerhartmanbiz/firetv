package app.streamflow;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Fire TV shell: full-screen WebView running assets/index.html. Remote keys are passed to the page. */
public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false); // no popups
        s.setSupportMultipleWindows(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new LocalOnlyClient());
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        web.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        String media = null;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE: media = "playpause"; break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: media = "ff"; break;
            case KeyEvent.KEYCODE_MEDIA_REWIND: media = "rew"; break;
            case KeyEvent.KEYCODE_MENU: media = "menu"; break;
        }
        if (media != null) {
            if (e.getAction() == KeyEvent.ACTION_DOWN)
                web.evaluateJavascript("window.onRemoteKey&&window.onRemoteKey('" + media + "')", null);
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.handleBack?String(window.handleBack()):'false'", new BackResult());
    }

    @Override protected void onPause() { web.evaluateJavascript("window.onAppPause&&window.onAppPause()", null); web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }

    /** The page never navigates away from the bundled UI (blocks redirects/popups). */
    static class LocalOnlyClient extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            return !r.getUrl().toString().startsWith("file:///android_asset/");
        }
        @SuppressWarnings("deprecation")
        @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
            return !url.startsWith("file:///android_asset/");
        }
    }

    /** Exit the app only when the page didn't handle Back. */
    @SuppressWarnings("rawtypes")
    class BackResult implements ValueCallback {
        public void onReceiveValue(Object o) {
            String v = (String) o;
            if (v == null || !v.contains("true")) finish();
        }
    }
}
