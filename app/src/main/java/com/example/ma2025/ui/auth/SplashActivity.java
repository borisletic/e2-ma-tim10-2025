package com.example.ma2025.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 sekunde
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        new Handler().postDelayed(this::checkUserStatus, SPLASH_DURATION);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean isLoggedIn = sharedPreferences.getBoolean(Constants.PREF_IS_LOGGED_IN, false);

        if (currentUser != null && isLoggedIn && currentUser.isEmailVerified()) {
            // Korisnik je ulogovan i email je verifikovan
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // Korisnik nije ulogovan ili email nije verifikovan
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}