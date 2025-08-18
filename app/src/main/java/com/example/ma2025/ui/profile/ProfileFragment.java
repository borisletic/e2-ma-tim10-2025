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

        // Initialize Firebase and preferences
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            preferencesManager = new PreferencesManager(requireContext());

            setupClickListeners();

            // Show default data first, then try to load from Firebase
            displayDefaultUserData();
            loadUserProfile();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ProfileFragment", e);
            displayErrorState();
        }
    }

    private void setupClickListeners() {
        if (binding == null) return;

        try {
            binding.btnEditProfile.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Uredi profil funkcionalnost će biti dodana uskoro", Toast.LENGTH_SHORT).show();
            });

            binding.btnChangePassword.setOnClickListener(v -> {
                try {
                    ChangePasswordDialog dialog = new ChangePasswordDialog();
                    dialog.show(getParentFragmentManager(), "change_password");
                } catch (Exception e) {
                    Log.e(TAG, "Error opening change password dialog", e);
                    Toast.makeText(getContext(), "Greška pri otvaranju dijaloga", Toast.LENGTH_SHORT).show();
                }
            });

            binding.cardQrCode.setOnClickListener(v -> {
                try {
                    if (binding.ivQrCode.getVisibility() == View.VISIBLE) {
                        binding.ivQrCode.setVisibility(View.GONE);
                        binding.tvQrHint.setText("Pritisnite za prikaz QR koda");
                    } else {
                        binding.ivQrCode.setVisibility(View.VISIBLE);
                        binding.tvQrHint.setText("Podelite ovaj QR kod sa prijateljima");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error toggling QR code", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    private void displayDefaultUserData() {
        try {
            if (binding == null) return;

            // Set default values to prevent crashes
            binding.tvUsername.setText("Korisnik");
            binding.tvLevel.setText("Nivo 0");
            binding.tvTitle.setText("Novajlija");
            binding.tvXp.setText("0");
            binding.tvPp.setText("0");
            binding.tvCoins.setText("0");
            binding.tvActiveDays.setText("1");
            binding.tvTotalTasks.setText("0");
            binding.tvCompletedTasks.setText("0");
            binding.tvLongestStreak.setText("0");
            binding.tvBadgeCount.setText("0");
            binding.tvNextLevelXp.setText("/200");

            // Set default progress
            binding.progressXp.setProgress(0);

            // Set default avatar
            setAvatarImage("avatar_1");

            // Show empty equipment state
            displayEquipment(new ArrayList<>());

            Log.d(TAG, "Default user data displayed");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying default user data", e);
        }
    }

    private void displayErrorState() {
        try {
            if (binding == null) return;

            binding.tvUsername.setText("Greška pri učitavanju");
            binding.tvLevel.setText("Nivo 0");
            binding.tvTitle.setText("Novajlija");

            Toast.makeText(getContext(), "Greška pri učitavanju profila. Pokušava se ponovo...", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error displaying error state", e);
        }
    }

    private void loadUserProfile() {
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "User ID je null");
            Toast.makeText(getContext(), "Greška: korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading profile for user: " + userId);

        // Try to display cached data first
        if (preferencesManager.isCacheValid()) {
            displayCachedUserData();
        }

        // Then load from Firebase
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        if (documentSnapshot.exists()) {
                            currentUser = documentSnapshot.toObject(User.class);
                            if (currentUser != null) {
                                displayUserData(currentUser);
                                cacheUserData(currentUser);
                                generateQRCode(userId);
                                loadUserEquipment();
                                Log.d(TAG, "User data loaded successfully");
                            } else {
                                Log.e(TAG, "Failed to parse user object");
                                createMissingUserData(userId);
                            }
                        } else {
                            Log.e(TAG, "User document does not exist, creating new one");
                            createMissingUserData(userId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing user data", e);
                        Toast.makeText(getContext(), "Greška pri obradi podataka korisnika", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user from Firebase", e);
                    Toast.makeText(getContext(), "Greška pri učitavanju profila: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createMissingUserData(String userId) {
        try {
            // Create a new user object with default values
            String email = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "nepoznat@email.com";

            User newUser = new User();
            newUser.setUid(userId);
            newUser.setEmail(email);
            newUser.setUsername("Korisnik_" + userId.substring(0, 6)); // Use part of UID as username
            newUser.setAvatar("avatar_1");
            newUser.setLevel(0);
            newUser.setTitle("Novajlija");
            newUser.setXp(0);
            newUser.setPp(0);
            newUser.setCoins(0);
            newUser.setActivated(true);

            // Save to Firebase
            db.collection(Constants.COLLECTION_USERS)
                    .document(userId)
                    .set(newUser)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "New user document created successfully");
                        currentUser = newUser;
                        displayUserData(newUser);
                        cacheUserData(newUser);
                        generateQRCode(userId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating user document", e);
                        Toast.makeText(getContext(), "Greška pri kreiranju korisničkih podataka", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error creating missing user data", e);
        }
    }

    private void displayCachedUserData() {
        try {
            if (binding == null) return;

            binding.tvUsername.setText(preferencesManager.getCachedUsername());
            binding.tvLevel.setText("Nivo " + preferencesManager.getCachedLevel());
            binding.tvTitle.setText(preferencesManager.getCachedTitle());
            binding.tvXp.setText(String.valueOf(preferencesManager.getCachedXp()));
            binding.tvPp.setText(String.valueOf(preferencesManager.getCachedPp()));
            binding.tvCoins.setText(String.valueOf(preferencesManager.getCachedCoins()));

            setAvatarImage("avatar_1");

            Log.d(TAG, "Cached user data displayed");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying cached user data", e);
        }
    }

    private void displayUserData(User user) {
        try {
            if (binding == null || user == null) return;

            binding.tvUsername.setText(user.getUsername() != null ? user.getUsername() : "Korisnik");
            binding.tvLevel.setText("Nivo " + user.getLevel());
            binding.tvTitle.setText(user.getTitle() != null ? user.getTitle() : "Novajlija");
            binding.tvXp.setText(String.valueOf(user.getXp()));
            binding.tvPp.setText(String.valueOf(user.getPp()));
            binding.tvCoins.setText(String.valueOf(user.getCoins()));
            binding.tvActiveDays.setText(String.valueOf(user.getActiveDays()));
            binding.tvTotalTasks.setText(String.valueOf(user.getTotalTasksCreated()));
            binding.tvCompletedTasks.setText(String.valueOf(user.getTotalTasksCompleted()));
            binding.tvLongestStreak.setText(String.valueOf(user.getLongestStreak()));

            setAvatarImage(user.getAvatar() != null ? user.getAvatar() : "avatar_1");

            int badgeCount = user.getBadges() != null ? user.getBadges().size() : 0;
            binding.tvBadgeCount.setText(String.valueOf(badgeCount));

            int nextLevelXp = user.getXpForNextLevel();
            binding.tvNextLevelXp.setText("/" + nextLevelXp);

            // Calculate progress safely
            float progress = nextLevelXp > 0 ? (float) user.getXp() / nextLevelXp : 0;
            binding.progressXp.setProgress((int) (progress * 100));

            Log.d(TAG, "User data displayed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying user data", e);
        }
    }

    private void setAvatarImage(String avatarName) {
        try {
            if (binding == null) return;

            int avatarResId = R.drawable.ic_person; // Default avatar
            binding.ivAvatar.setImageResource(avatarResId);

        } catch (Exception e) {
            Log.e(TAG, "Error setting avatar image", e);
        }
    }

    private void cacheUserData(User user) {
        try {
            if (user == null) return;

            preferencesManager.cacheUserData(
                    user.getUsername() != null ? user.getUsername() : "Korisnik",
                    user.getLevel(),
                    user.getTitle() != null ? user.getTitle() : "Novajlija",
                    user.getXp(),
                    user.getPp(),
                    user.getCoins()
            );

        } catch (Exception e) {
            Log.e(TAG, "Error caching user data", e);
        }
    }

    private void loadUserEquipment() {
        try {
            // For now, show placeholder since Equipment collection might not exist yet
            displayEquipment(new ArrayList<>());

        } catch (Exception e) {
            Log.e(TAG, "Error loading user equipment", e);
            displayEquipment(new ArrayList<>());
        }
    }

    private void displayEquipment(List<Equipment> equipmentList) {
        try {
            if (binding == null) return;

            // Check if the views exist in the layout
            if (binding.tvNoEquipment != null && binding.rvEquipment != null) {
                if (equipmentList == null || equipmentList.isEmpty()) {
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

        } catch (Exception e) {
            Log.e(TAG, "Error displaying equipment", e);
        }
    }

    private void generateQRCode(String userId) {
        try {
            if (binding == null || userId == null) return;

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(
                    userId,
                    BarcodeFormat.QR_CODE,
                    Constants.QR_CODE_SIZE,
                    Constants.QR_CODE_SIZE
            );
            binding.ivQrCode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error generating QR code", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}