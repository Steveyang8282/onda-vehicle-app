package app.onda.comfort;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * ONDA 시제품 실행 화면.
 * 앱 안에 묶은 화면(assets/web)만 보여 주며, 인터넷 권한이 없고 바깥 주소는 모두 막는다.
 * 차량 신호는 아직 연결하지 않았다(모의 주행 데이터). Pleos Connect SDK 연동은 다음 단계.
 */
public class MainActivity extends Activity {
    static final String HOME = "file:///android_asset/web/index.html";
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        WebView.setWebContentsDebuggingEnabled(false); // 배포판: 원격 디버깅 끔
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(242, 243, 238));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(242, 243, 238));
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        // 상태 표시줄·내비게이션 막대에 화면이 가리지 않게 여백을 준다(Android 15 이상 전체 화면 대응).
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                l = i.left; t = i.top; r = i.right; b = i.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft(); t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight(); b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(l, t, r, b);
            return insets;
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // 프로필·단계 저장(localStorage)
        s.setAllowFileAccess(false);           // 기기 파일 접근 금지(앱 내장 화면은 그대로 열림)
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setGeolocationEnabled(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSaveFormData(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !isBundled(r.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (isBundled(r.getUrl())) return null;
                return new WebResourceResponse("text/plain", "utf-8", 403, "Blocked", null, null);
            }
        });

        setContentView(root);
        if (state != null) web.restoreState(state);
        if (web.getUrl() == null) web.loadUrl(HOME);
    }

    /** 앱에 묶은 화면 파일만 허용한다. */
    static boolean isBundled(Uri u) {
        if (u == null || !"file".equals(u.getScheme())) return false;
        String p = u.getPath();
        return p != null && p.startsWith("/android_asset/web/") && !p.contains("..");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
        }
        super.onDestroy();
    }
}
