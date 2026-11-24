package app.firetvtest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin shell: the whole UI lives in index.html on the main branch of the GitHub repo.
 * Every launch downloads the latest index.html, so edits pushed to main show up without
 * reinstalling the APK. Falls back to the last downloaded copy, then to the bundled copy.
 */
public class MainActivity extends Activity {
    private static final String REMOTE_URL =
            "https://raw.githubusercontent.com/tylerhartmanbiz/firetv/main/index.html";
    private static final Pattern BASE_RE = Pattern.compile("https://ww1.goojara.to/watch-trends-genre_BASE\\s*=\\s*[\"'`]([^\"'`]+)[\"'`]");

    private FrameLayout root;
    private WebView web;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(true); // so window.open goes to onCreateWindow, where it is blocked
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                // Keep the app page in place: block ads/redirects trying to navigate the main page away.
                return r.isForMainFrame();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView v, boolean dialog, boolean userGesture, Message m) {
                return false; // block popups
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (fullscreenView != null) { cb.onCustomViewHidden(); return; }
                fullscreenView = view;
                fullscreenCallback = cb;
                root.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onHideCustomView() { exitFullscreen(); }
        });

        loadLatest();
    }

    private void loadLatest() {
        new Thread(new Runnable() { public void run() {
            String html = download();
            File cache = new File(getFilesDir(), "index.html");
            if (html != null) {
                write(cache, html);
            } else {
                html = read(cache);
                if (html == null) html = readAsset();
            }
            final String page = html == null ? "<body style='background:#000;color:#fff'>Could not load app.</body>" : html;
            runOnUiThread(new Runnable() { public void run() { show(page); } });
        }}).start();
    }

    private void show(String html) {
        Matcher m = BASE_RE.matcher(html);
        String base = m.find() ? m.group(1).replaceAll("/+$", "") + "/" : REMOTE_URL;
        web.loadDataWithBaseURL(base, html, "text/html", "UTF-8", null);
        web.requestFocus();
    }

    private String download() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(REMOTE_URL + "?t=" + System.currentTimeMillis()).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setUseCaches(false);
            c.setRequestProperty("Cache-Control", "no-cache");
            if (c.getResponseCode() != 200) return null;
            String s = readStream(c.getInputStream());
            return s.contains("<") ? s : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readStream(InputStream in) throws Exception {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    private static String read(File f) {
        try { return f.exists() ? readStream(new FileInputStream(f)) : null; } catch (Exception e) { return null; }
    }

    private String readAsset() {
        try { return readStream(getAssets().open("index.html")); } catch (Exception e) { return null; }
    }

    private static void write(File f, String s) {
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(s.getBytes("UTF-8")); } catch (Exception ignored) { }
    }

    private void exitFullscreen() {
        if (fullscreenView == null) return;
        root.removeView(fullscreenView);
        fullscreenView = null;
        if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
        fullscreenCallback = null;
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_MENU) { loadLatest(); return true; } // Menu button = reload latest
        return super.onKeyDown(code, e);
    }

    @Override
    public void onBackPressed() {
        if (fullscreenView != null) { exitFullscreen(); return; }
        // Back closes the player if it is open, otherwise exits the app.
        web.evaluateJavascript(
                "(function(){try{if(isPlayerOpen){closePlayer();return 'closed';}}catch(e){}return 'none';})()",
                new android.webkit.ValueCallback<String>() {
                    public void onReceiveValue(String r) { if (!"\"closed\"".equals(r)) finish(); }
                });
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
