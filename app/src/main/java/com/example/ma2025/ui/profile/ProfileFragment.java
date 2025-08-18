// Updated ProfileFragment.java - Add these imports and methods

package com.example.ma2025.ui.profile;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ma2025.R;
import com.example.ma2025.data.models.User;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.databinding.FragmentProfileBinding;
import com.example.ma2025.ui.dialogs.ChangePasswordDialog;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.EquipmentAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferencesManager preferencesManager;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        preferencesManager = new PreferencesManager(requireContext());

        setupClickListeners();
        loadUserProfile();
    }

    private void setupClickListeners() {
        binding.btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Uredi profil funkcionalnost će biti dodana uskoro", Toast.LENGTH_SHORT).show();
        });

        binding.btnChangePassword.setOnClickListener(v -> {
            ChangePasswordDialog dialog = new ChangePasswordDialog();
            dialog.show(getParentFragmentManager(), "change_password");
        });

        binding.cardQrCode.setOnClickListener(v -> {
            if (binding.ivQrCode.getVisibility() == View.VISIBLE) {
                binding.ivQrCode.setVisibility(View.GONE);
                binding.tvQrHint.setText("Pritisnite za prikaz QR koda");
            } else {
                binding.ivQrCode.setVisibility(View.VISIBLE);
                binding.tvQrHint.setText("Podelite ovaj QR kod sa prijateljima");
            }
        });
    }

    private void loadUserProfile() {
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "User ID je null");
            return;
        }

        if (preferencesManager.isCacheValid()) {
            displayCachedUserData();
        }

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            displayUserData(currentUser);
                            cacheUserData(currentUser);
                            generateQRCode(userId);
                            loadUserEquipment();
                        }
                    } else {
                        Log.e(TAG, "User document does not exist");
                        Toast.makeText(getContext(), "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri učitavanju korisnika", e);
                    Toast.makeText(getContext(), "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayCachedUserData() {
        binding.tvUsername.setText(preferencesManager.getCachedUsername());
        binding.tvLevel.setText("Nivo " + preferencesManager.getCachedLevel());
        binding.tvTitle.setText(preferencesManager.getCachedTitle());
        binding.tvXp.setText(String.valueOf(preferencesManager.getCachedXp()));
        binding.tvPp.setText(String.valueOf(preferencesManager.getCachedPp()));
        binding.tvCoins.setText(String.valueOf(preferencesManager.getCachedCoins()));

        setAvatarImage("avatar_1");
    }

    private void displayUserData(User user) {
        binding.tvUsername.setText(user.getUsername());
        binding.tvLevel.setText("Nivo " + user.getLevel());
        binding.tvTitle.setText(user.getTitle());
        binding.tvXp.setText(String.valueOf(user.getXp()));
        binding.tvPp.setText(String.valueOf(user.getPp()));
        binding.tvCoins.setText(String.valueOf(user.getCoins()));
        binding.tvActiveDays.setText(String.valueOf(user.getActiveDays()));
        binding.tvTotalTasks.setText(String.valueOf(user.getTotalTasksCreated()));
        binding.tvCompletedTasks.setText(String.valueOf(user.getTotalTasksCompleted()));
        binding.tvLongestStreak.setText(String.valueOf(user.getLongestStreak()));

        setAvatarImage(user.getAvatar());

        int badgeCount = user.getBadges() != null ? user.getBadges().size() : 0;
        binding.tvBadgeCount.setText(String.valueOf(badgeCount));

        int nextLevelXp = user.getXpForNextLevel();
        binding.tvNextLevelXp.setText("/" + nextLevelXp);

        float progress = (float) user.getXp() / nextLevelXp;
        binding.progressXp.setProgress((int) (progress * 100));
    }

    private void setAvatarImage(String avatarName) {
        int avatarResId = R.drawable.ic_person; // Default avatar
        binding.ivAvatar.setImageResource(avatarResId);
    }

    private void cacheUserData(User user) {
        preferencesManager.cacheUserData(
                user.getUsername(),
                user.getLevel(),
                user.getTitle(),
                user.getXp(),
                user.getPp(),
                user.getCoins()
        );
    }

    private void loadUserEquipment() {
        String userId = preferencesManager.getUserId();
        if (userId == null) return;

        // For now, show placeholder since Equipment collection might not exist yet
        displayEquipment(new ArrayList<>());

        // Uncomment this when Equipment collection is implemented
        /*
        db.collection(Constants.COLLECTION_EQUIPMENT)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Equipment> activeEquipment = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Equipment equipment = doc.toObject(Equipment.class);
                        if (equipment != null) {
                            activeEquipment.add(equipment);
                        }
                    }
                    displayEquipment(activeEquipment);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading equipment", e);
                    displayEquipment(new ArrayList<>());
                });
        */
    }

    private void displayEquipment(List<Equipment> equipmentList) {
        // Check if the views exist in the layout
        if (binding.tvNoEquipment != null && binding.rvEquipment != null) {
            if (equipmentList.isEmpty()) {
                binding.tvNoEquipment.setVisibility(View.VISIBLE);
                binding.rvEquipment.setVisibility(View.GONE);
            } else {
                binding.tvNoEquipment.setVisibility(View.GONE);
                binding.rvEquipment.setVisibility(View.VISIBLE);

                EquipmentAdapter adapter = new EquipmentAdapter(getContext(), equipmentList);
                binding.rvEquipment.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                binding.rvEquipment.setAdapter(adapter);
            }
        }
    }

    private void generateQRCode(String userId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(
                    userId,
                    BarcodeFormat.QR_CODE,
                    Constants.QR_CODE_SIZE,
                    Constants.QR_CODE_SIZE
            );
            binding.ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Log.e(TAG, "Greška pri generisanju QR koda", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}