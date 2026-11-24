package app.streamflow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fire TV shell: full-screen WebView.
 * - The UI (index.html) is fetched live from GitHub on every launch, so pushing a change to
 *   assets/index.html on main updates the app with no reinstall. Falls back to the last
 *   downloaded copy, then the copy bundled in the APK, when offline.
 * - The app also checks version.txt and offers to install a newer APK (native changes).
 * - Menu key (three lines) reloads the UI from GitHub.
 */
public class MainActivity extends Activity {
    static final String REPO_RAW = "https://raw.githubusercontent.com/tylerhartmanbiz/firetv/main/";
    static final String BASE = "file:///android_asset/";
    private WebView web;
    private boolean updateOffered;

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
        web.requestFocus();
        loadUi(false);
    }

    // ---------- live UI ----------
    private void loadUi(final boolean manual) {
        new Thread(new Runnable() { public void run() {
            String html = null;
            try { html = new String(get(REPO_RAW + "assets/index.html?t=" + System.currentTimeMillis()), "UTF-8"); }
            catch (Exception ignored) {}
            File cache = new File(getFilesDir(), "index.html");
            if (html != null && html.contains("<html")) {
                try { write(cache, html.getBytes("UTF-8")); } catch (Exception ignored) {}
            } else if (cache.exists()) {
                try { html = new String(read(cache), "UTF-8"); } catch (Exception e) { html = null; }
            } else html = null;
            final String page = html;
            runOnUiThread(new Runnable() { public void run() {
                if (page != null) web.loadDataWithBaseURL(BASE, page, "text/html", "UTF-8", null);
                else web.loadUrl(BASE + "index.html");
                if (manual) Toast.makeText(MainActivity.this, "Reloaded", Toast.LENGTH_SHORT).show();
            }});
            checkForApkUpdate();
        }}).start();
    }

    // ---------- APK self-update ----------
    private void checkForApkUpdate() {
        if (updateOffered) return;
        try {
            int latest = Integer.parseInt(new String(get(REPO_RAW + "version.txt?t=" + System.currentTimeMillis()), "UTF-8").trim());
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (latest <= pi.versionCode) return;
            updateOffered = true;
            runOnUiThread(new Runnable() { public void run() {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("StreamFlow update available")
                    .setMessage("Download and install the new version now?")
                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) { downloadAndInstall(); }
                    })
                    .setNegativeButton("Later", null).show();
            }});
        } catch (Exception ignored) {}
    }

    private void downloadAndInstall() {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(this, "Allow StreamFlow to install apps, then press Back", Toast.LENGTH_LONG).show();
            updateOffered = false;
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
            return;
        }
        Toast.makeText(this, "Downloading update...", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() { public void run() {
            try {
                File apk = new File(getCacheDir(), ApkProvider.FILE);
                write(apk, get(REPO_RAW + "streamflow.apk?t=" + System.currentTimeMillis()));
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse("content://" + getPackageName() + ".apk/" + ApkProvider.FILE),
                        "application/vnd.android.package-archive");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (final Exception e) {
                runOnUiThread(new Runnable() { public void run() {
                    Toast.makeText(MainActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }});
            }
        }}).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        // Returning from the "install unknown apps" settings screen: offer the update again.
        if (!updateOffered) new Thread(new Runnable() { public void run() { checkForApkUpdate(); } }).start();
    }

    // ---------- remote keys ----------
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        String media = null;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE: media = "playpause"; break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: media = "ff"; break;
            case KeyEvent.KEYCODE_MEDIA_REWIND: media = "rew"; break;
            case KeyEvent.KEYCODE_MENU:
                if (e.getAction() == KeyEvent.ACTION_DOWN) loadUi(true);
                return true;
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
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }

    // ---------- io ----------
    static byte[] get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(4000);
        c.setReadTimeout(15000);
        c.setUseCaches(false);
        try {
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            return readAll(c.getInputStream());
        } finally { c.disconnect(); }
    }
    static byte[] readAll(InputStream in) throws Exception {
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buf = new byte[16384]; int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            return o.toByteArray();
        } finally { in.close(); }
    }
    static byte[] read(File f) throws Exception { return readAll(new FileInputStream(f)); }
    static void write(File f, byte[] data) throws Exception {
        File tmp = new File(f.getPath() + ".tmp");
        OutputStream o = new FileOutputStream(tmp);
        try { o.write(data); } finally { o.close(); }
        if (!tmp.renameTo(f)) throw new Exception("rename failed");
    }

    /** The page never navigates away from the UI (blocks redirects/popups). */
    static class LocalOnlyClient extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            return !r.getUrl().toString().startsWith(BASE);
        }
        @SuppressWarnings("deprecation")
        @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
            return !url.startsWith(BASE);
        }
    }

    /** Exit the app only when the page didn't handle Back. */
    class BackResult implements ValueCallback<String> {
        public void onReceiveValue(String v) {
            if (v == null || !v.contains("true")) finish();
        }
    }
}
