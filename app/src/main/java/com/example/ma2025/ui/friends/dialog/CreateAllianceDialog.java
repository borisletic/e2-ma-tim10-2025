package com.example.ma2025.ui.friends.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.ma2025.R;

public class CreateAllianceDialog extends DialogFragment {

    private OnAllianceCreatedListener listener;

    public interface OnAllianceCreatedListener {
        void onAllianceCreated(String allianceName);
    }

    public void setOnAllianceCreatedListener(OnAllianceCreatedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        EditText editText = new EditText(getContext());
        editText.setHint("Unesite naziv saveza");
        editText.setPadding(64, 32, 64, 32);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Kreiraj novi savez")
                .setMessage("Unesite naziv za vaš novi savez:")
                .setView(editText)
                .setPositiveButton("Kreiraj", (dialog, which) -> {
                    String allianceName = editText.getText().toString().trim();
                    if (!TextUtils.isEmpty(allianceName) && listener != null) {
                        listener.onAllianceCreated(allianceName);
                    }
                })
                .setNegativeButton("Otkaži", null)
                .create();
    }
}