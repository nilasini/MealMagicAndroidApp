package com.sample.mealmagic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okio.Okio;

public class UserInfoActivity extends AppCompatActivity {

    String idToken;
    String accessToken;
    private static final String TAG = "UserInfoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

    }

    @Override
    protected void onStart() {

        super.onStart();
        handleAuthorizationResponse(getIntent());
    }

    private void handleAuthorizationResponse(Intent intent) {

        final AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationService service = new AuthorizationService(this);
        if (response != null) {
            performTokenRequest(service, response.createTokenExchangeRequest(),
                    this::handleCodeExchangeResponse);
        }
    }

    private void performTokenRequest(AuthorizationService authService, TokenRequest request,
                                     AuthorizationService.TokenResponseCallback callback) {

        authService.performTokenRequest(request, callback);
    }


    private void handleCodeExchangeResponse(TokenResponse tokenResponse,
                                            AuthorizationException authException) {


        idToken = tokenResponse.idToken;
        accessToken = tokenResponse.accessToken;
        callUserInfo();
    }


    private void callUserInfo(){

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                String userinfoEndpoint = ConfigManager.
                        readConfigValues(getApplicationContext(), "userinfo_endpoint");
                URL userInfoEndpoint = new URL(userinfoEndpoint);
                HttpURLConnection conn = (HttpURLConnection) userInfoEndpoint.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setInstanceFollowRedirects(false);
                String response = Okio.buffer(Okio.source(conn.getInputStream())).
                        readString((StandardCharsets.UTF_8));
                JSONObject json = new JSONObject(response);

                TextView username1 = findViewById(R.id.username1);
                TextView username2 = findViewById(R.id.username2);

                username1.setText(json.getString("username"));
                username2.setText(json.getString("username"));

                Button btnClick = findViewById(R.id.logout);
                btnClick.setOnClickListener(new LogoutListener());
            } catch (Exception e) {
                Log.e(TAG, "Error while parsing json object.");
            }
        });
    }

    public class LogoutListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {

            String logoutEndpoint = ConfigManager.
                    readConfigValues(view.getContext(), "logout_endpoint");
            String url = logoutEndpoint + "?id_token_hint=" + idToken;

            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            customTabsIntent.launchUrl(view.getContext(), Uri.parse(url));
        }
    }
}