package com.example.ma2025.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ma2025.R;
import com.example.ma2025.data.models.User;
import com.example.ma2025.databinding.ActivityRegisterBinding;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.ValidationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedAvatar = Constants.AVATAR_OPTIONS[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupUI();
        setupClickListeners();
    }

    private void setupUI() {
        // Setup avatar spinner
        ArrayAdapter<String> avatarAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Constants.AVATAR_OPTIONS);
        avatarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAvatar.setAdapter(avatarAdapter);

        binding.spinnerAvatar.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedAvatar = Constants.AVATAR_OPTIONS[position];
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Setup password strength indicator
        binding.etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String strength = ValidationUtils.getPasswordStrengthMessage(s.toString());
                binding.tvPasswordStrength.setText(strength);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        binding.btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = binding.etEmail.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!validateInput(email, username, password, confirmPassword)) {
            return;
        }

        showLoading(true);

        // Prvo proveravamo da li username već postoji
        checkUsernameAvailability(username, () -> {
            // Username je dostupan, kreiraj nalog
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                sendEmailVerification(firebaseUser);
                                createUserInFirestore(firebaseUser.getUid(), email, username);
                            }
                        } else {
                            showLoading(false);
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : Constants.ERROR_GENERAL;
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void checkUsernameAvailability(String username, Runnable onSuccess) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            onSuccess.run();
                        } else {
                            showLoading(false);
                            binding.etUsername.setError("Korisničko ime već postoji");
                            binding.etUsername.requestFocus();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, Constants.ERROR_NETWORK, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, Constants.SUCCESS_REGISTRATION, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Greška pri slanju email-a za aktivaciju",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserInFirestore(String uid, String email, String username) {
        User user = new User(uid, email, username, selectedAvatar);

        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .set(user)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        mAuth.signOut(); // Odjavi korisnika dok ne aktivira email
                        Toast.makeText(this, "Registracija uspešna! Proverite email za aktivaciju.",
                                Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Greška pri kreiranju profila", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInput(String email, String username, String password, String confirmPassword) {
        if (!ValidationUtils.isValidEmail(email)) {
            binding.etEmail.setError(Constants.ERROR_INVALID_EMAIL);
            binding.etEmail.requestFocus();
            return false;
        }

        String usernameError = ValidationUtils.getUsernameErrorMessage(username);
        if (usernameError != null) {
            binding.etUsername.setError(usernameError);
            binding.etUsername.requestFocus();
            return false;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            binding.etPassword.setError(Constants.ERROR_WEAK_PASSWORD);
            binding.etPassword.requestFocus();
            return false;
        }

        if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            binding.etConfirmPassword.setError(Constants.ERROR_PASSWORDS_DONT_MATCH);
            binding.etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!show);
        binding.etEmail.setEnabled(!show);
        binding.etUsername.setEnabled(!show);
        binding.etPassword.setEnabled(!show);
        binding.etConfirmPassword.setEnabled(!show);
        binding.spinnerAvatar.setEnabled(!show);
    }
}