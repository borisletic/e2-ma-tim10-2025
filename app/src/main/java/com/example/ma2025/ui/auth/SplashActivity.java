// Fixed SplashActivity.java - Added better debugging and error handling
package com.example.ma2025.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.utils.Constants;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 2000; // 2 sekunde
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d(TAG, "SplashActivity started");

        try {
            // Initialize Firebase first
            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

            Log.d(TAG, "Firebase and preferences initialized");

            // Check user status with delay
            new Handler().postDelayed(this::checkUserStatus, SPLASH_DURATION);

        } catch (Exception e) {
            Log.e(TAG, "Error in SplashActivity onCreate", e);
            // If there's an error, go to login as fallback
            redirectToLogin();
        }
    }

    private void checkUserStatus() {
        try {
            Log.d(TAG, "Checking user status...");

            FirebaseUser currentUser = mAuth.getCurrentUser();
            boolean isLoggedIn = sharedPreferences.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
            String userId = sharedPreferences.getString(Constants.PREF_USER_ID, null);

            Log.d(TAG, "Firebase user exists: " + (currentUser != null));
            Log.d(TAG, "Is logged in (prefs): " + isLoggedIn);
            Log.d(TAG, "User ID from prefs: " + userId);

            if (currentUser != null) {
                Log.d(TAG, "Firebase user UID: " + currentUser.getUid());
                Log.d(TAG, "Email verified: " + currentUser.isEmailVerified());
            }

            // Check all conditions for successful login
            if (currentUser != null && isLoggedIn && currentUser.isEmailVerified()) {
                Log.d(TAG, "User is authenticated, going to MainActivity");
                redirectToMain();
            } else {
                // Log specific reasons for redirect to login
                if (currentUser == null) {
                    Log.d(TAG, "No Firebase user found, going to login");
                } else if (!isLoggedIn) {
                    Log.d(TAG, "User not marked as logged in preferences, going to login");
                } else if (!currentUser.isEmailVerified()) {
                    Log.d(TAG, "Email not verified, going to login");
                }
                redirectToLogin();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking user status", e);
            Toast.makeText(this, "Greška pri proveri korisnika", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        }
    }

    private void redirectToMain() {
        try {
            Log.d(TAG, "Redirecting to MainActivity");
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error redirecting to MainActivity", e);
            Toast.makeText(this, "Greška pri otvaranju glavne stranice", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        }
    }

    private void redirectToLogin() {
        try {
            Log.d(TAG, "Redirecting to LoginActivity");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error redirecting to LoginActivity", e);
            // Force close app if we can't even redirect to login
            finishAffinity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SplashActivity destroyed");
    }
}