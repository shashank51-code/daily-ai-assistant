package com.dailyai.assistant;

import android.os.Bundle;
import android.webkit.CookieManager;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The bundled app runs at https://localhost while the API is on its own
        // HTTPS origin. The session cookie is HttpOnly, Secure and SameSite=None
        // in production, so the WebView must accept cookies for that cross-site
        // request. This does not expose the session to JavaScript and does not
        // bypass authentication or CSRF checks.
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(getBridge().getWebView(), true);
        cookieManager.flush();
    }
}
