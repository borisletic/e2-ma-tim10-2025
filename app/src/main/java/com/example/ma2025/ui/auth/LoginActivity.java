package com.example.ma2025.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.databinding.ActivityLoginBinding;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.ValidationUtils;
import com.example.ma2025.utils.EmailActivationChecker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private EmailActivationChecker activationChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        activationChecker = new EmailActivationChecker();

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
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkEmailVerificationWithTimeout(user);
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : Constants.ERROR_GENERAL;
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkEmailVerificationWithTimeout(FirebaseUser user) {
        // Reload user to get latest email verification status
        user.reload().addOnCompleteListener(reloadTask -> {
            if (reloadTask.isSuccessful()) {
                if (user.isEmailVerified()) {
                    // Email is verified, mark as activated and proceed
                    activationChecker.markAccountActivated(user.getUid());
                    proceedWithLogin(user);
                } else {
                    // Email not verified, check if still within timeout period
                    checkActivationTimeout(user);
                }
            } else {
                showLoading(false);
                Toast.makeText(this, "Greška pri proveri naloga", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkActivationTimeout(FirebaseUser user) {
        activationChecker.checkActivationStatus(user.getUid(),
                new EmailActivationChecker.ActivationStatusCallback() {
                    @Override
                    public void onActivated() {
                        // Already marked as activated, proceed
                        proceedWithLogin(user);
                    }

                    @Override
                    public void onPending() {
                        // Still within timeout period
                        showLoading(false);
                        mAuth.signOut();
                        showActivationPendingDialog(user.getUid());
                    }

                    @Override
                    public void onExpired() {
                        // Activation link expired
                        showLoading(false);
                        mAuth.signOut();
                        showActivationExpiredDialog();
                    }

                    @Override
                    public void onNotFound() {
                        // No activation record found, treat as pending
                        showLoading(false);
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this,
                                "Molimo aktivirajte vaš email nalog pre prijave.",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this,
                                "Greška pri proveri aktivacije naloga",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showActivationPendingDialog(String userId) {
        activationChecker.getRemainingActivationTime(userId, remainingTimeMs -> {
            long hoursRemaining = remainingTimeMs / (1000 * 60 * 60);
            long minutesRemaining = (remainingTimeMs % (1000 * 60 * 60)) / (1000 * 60);

            String message = String.format(
                    "Molimo aktivirajte vaš email nalog pre prijave.\n\n" +
                            "Aktivacijski link ističe za %d sati i %d minuta.\n\n" +
                            "Želite li da pošaljemo novi aktivacijski email?",
                    hoursRemaining, minutesRemaining
            );

            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                        .setTitle("Email aktivacija")
                        .setMessage(message)
                        .setPositiveButton("Pošalji novi email", (dialog, which) -> {
                            resendActivationEmail();
                        })
                        .setNegativeButton("U redu", null)
                        .show();
            });
        });
    }

    private void showActivationExpiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Aktivacija je istekla")
                .setMessage("Aktivacijski link je istekao (24h). Molimo registrujte se ponovo.")
                .setPositiveButton("Registruj se", (dialog, which) -> {
                    startActivity(new Intent(this, RegisterActivity.class));
                })
                .setNegativeButton("U redu", null)
                .show();
    }

    private void resendActivationEmail() {
        String email = binding.etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Molimo unesite email adresu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sign in temporarily to send verification email
        mAuth.signInWithEmailAndPassword(email, "temporary")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            // Update activation timeout
                                            activationChecker.startActivationTimeout(user.getUid());
                                            Toast.makeText(this,
                                                    "Novi aktivacijski email je poslat. Proverite inbox.",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(this,
                                                    "Greška pri slanju aktivacijskog email-a",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        mAuth.signOut();
                                    });
                        }
                    } else {
                        Toast.makeText(this,
                                "Potrebno je da se registrujete ponovo",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void proceedWithLogin(FirebaseUser user) {
        showLoading(false);
        saveLoginState(user.getUid(), user.getEmail());
        Toast.makeText(this, Constants.SUCCESS_LOGIN, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
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
        // Create a simple input dialog for password reset
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint("Unesite email adresu");
        editText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(this)
                .setTitle("Reset lozinke")
                .setMessage("Unesite email adresu za reset lozinke:")
                .setView(editText)
                .setPositiveButton("Pošalji", (dialog, which) -> {
                    String email = editText.getText().toString().trim();
                    if (ValidationUtils.isValidEmail(email)) {
                        sendPasswordResetEmail(email);
                    } else {
                        Toast.makeText(this, "Unesite ispravnu email adresu", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Email za reset lozinke je poslat na " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Greška pri slanju email-a. Proverite da li je email adresa ispravna.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.etEmail.setEnabled(!show);
        binding.etPassword.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activationChecker != null) {
            activationChecker.shutdown();
        }
    }
}