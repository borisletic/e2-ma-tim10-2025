package com.example.ma2025.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.databinding.ActivityLoginBinding;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.ValidationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                saveLoginState(user.getUid(), email);
                                Toast.makeText(this, Constants.SUCCESS_LOGIN, Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Molimo aktivirajte vaš email nalog pre prijave.",
                                        Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : Constants.ERROR_GENERAL;
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInput(String email, String password) {
        if (!ValidationUtils.isValidEmail(email)) {
            binding.etEmail.setError(Constants.ERROR_INVALID_EMAIL);
            binding.etEmail.requestFocus();
            return false;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            binding.etPassword.setError(Constants.ERROR_WEAK_PASSWORD);
            binding.etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void saveLoginState(String userId, String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.putString(Constants.PREF_USER_ID, userId);
        editor.putString(Constants.PREF_USER_EMAIL, email);
        editor.putLong(Constants.PREF_LAST_LOGIN, System.currentTimeMillis());
        editor.apply();
    }

    private void showForgotPasswordDialog() {
        // TODO: Implementirati dialog za reset lozinke
        Toast.makeText(this, "Funkcionalnost će biti dodana uskoro", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.etEmail.setEnabled(!show);
        binding.etPassword.setEnabled(!show);
    }
}