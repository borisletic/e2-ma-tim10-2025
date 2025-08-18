package com.example.ma2025.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.ma2025.databinding.DialogChangePasswordBinding;
import com.example.ma2025.utils.ValidationUtils;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordDialog extends DialogFragment {

    private DialogChangePasswordBinding binding;
    private FirebaseAuth mAuth;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        binding = DialogChangePasswordBinding.inflate(LayoutInflater.from(getContext()));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Promeni lozinku")
                .setView(binding.getRoot())
                .setPositiveButton("Promeni", null)
                .setNegativeButton("Otkaži", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> changePassword());
        });

        return dialog;
    }

    private void changePassword() {
        String currentPassword = binding.etCurrentPassword.getText().toString().trim();
        String newPassword = binding.etNewPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!validateInput(currentPassword, newPassword, confirmPassword)) {
            return;
        }

        showLoading(true);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            showLoading(false);
            Toast.makeText(getContext(), "Greška: korisnik nije pronađen", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate user with current password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update password
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    showLoading(false);
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(getContext(), "Lozinka je uspešno promenjena", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    } else {
                                        String error = updateTask.getException() != null ?
                                                updateTask.getException().getMessage() : "Greška pri promeni lozinke";
                                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        showLoading(false);
                        Toast.makeText(getContext(), "Trenutna lozinka nije ispravna", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInput(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            binding.etCurrentPassword.setError("Unesite trenutnu lozinku");
            return false;
        }

        if (!ValidationUtils.isValidPassword(newPassword)) {
            binding.etNewPassword.setError("Lozinka mora imati najmanje 6 karaktera");
            return false;
        }

        if (!ValidationUtils.doPasswordsMatch(newPassword, confirmPassword)) {
            binding.etConfirmPassword.setError("Lozinke se ne poklapaju");
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}