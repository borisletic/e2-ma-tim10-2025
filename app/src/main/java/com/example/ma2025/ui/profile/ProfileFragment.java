package com.example.ma2025.ui.profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import com.example.ma2025.ui.equipment.adapter.EquipmentAdapter;
import com.google.firebase.auth.FirebaseUser;
import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.data.models.User;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.databinding.FragmentProfileBinding;
import com.example.ma2025.ui.dialogs.ChangePasswordDialog;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.util.ArrayList;
import java.util.List;
import com.example.ma2025.utils.EquipmentManager;
// DODANO ZA STATISTIKE:
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.*;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferencesManager preferencesManager;
    private User currentUser;
    private EquipmentAdapter equipmentAdapter;
    private List<Equipment> activeEquipment;

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
            setupEquipmentRecyclerView();
            // DODANO ZA STATISTIKE:
            setupCharts();

            // Show default data first, then try to load from Firebase
            displayDefaultUserData();
            loadUserProfile();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ProfileFragment", e);
            displayErrorState();
        }
    }

    private void setupEquipmentRecyclerView() {
        try {
            if (binding.rvEquipment != null) {
                equipmentAdapter = new EquipmentAdapter(activeEquipment, null); // Read-only adapter
                binding.rvEquipment.setLayoutManager(new LinearLayoutManager(getContext()));
                binding.rvEquipment.setAdapter(equipmentAdapter);
                Log.d(TAG, "Equipment RecyclerView setup completed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up equipment RecyclerView", e);
        }
    }

    // DODANO ZA STATISTIKE:
    private void setupCharts() {
        try {
            setupTasksDonutChart();
            setupCategoryBarChart();
            setupXpLineChart();
            setupDifficultyLineChart();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up charts", e);
        }
    }

    // DODANO ZA STATISTIKE:
    private void setupTasksDonutChart() {
        try {
            if (binding.chartTasksDonut != null) {
                PieChart chart = binding.chartTasksDonut;

                Description description = new Description();
                description.setText("");
                chart.setDescription(description);
                chart.setHoleRadius(40f);
                chart.setTransparentCircleRadius(45f);
                chart.setDrawCenterText(false);
                chart.setRotationEnabled(false);
                chart.getLegend().setEnabled(true);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up tasks donut chart", e);
        }
    }

    // DODANO ZA STATISTIKE:
    private void setupCategoryBarChart() {
        try {
            if (binding.chartCategoryBar != null) {
                BarChart chart = binding.chartCategoryBar;

                Description description = new Description();
                description.setText("");
                chart.setDescription(description);
                chart.setDrawGridBackground(false);
                chart.getAxisRight().setEnabled(false);

                XAxis xAxis = chart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setDrawGridLines(false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up category bar chart", e);
        }
    }

    // DODANO ZA STATISTIKE:
    private void setupXpLineChart() {
        try {
            if (binding.chartXpLine != null) {
                LineChart chart = binding.chartXpLine;

                Description description = new Description();
                description.setText("");
                chart.setDescription(description);
                chart.setDrawGridBackground(false);
                chart.getAxisRight().setEnabled(false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up XP line chart", e);
        }
    }

    // DODANO ZA STATISTIKE:
    private void setupDifficultyLineChart() {
        try {
            if (binding.chartDifficultyLine != null) {
                LineChart chart = binding.chartDifficultyLine;

                Description description = new Description();
                description.setText("");
                chart.setDescription(description);
                chart.setDrawGridBackground(false);
                chart.getAxisRight().setEnabled(false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up difficulty line chart", e);
        }
    }

    private void setupClickListeners() {
        if (binding == null) return;

        try {
            binding.btnChangePassword.setOnClickListener(v -> {
                try {
                    ChangePasswordDialog dialog = new ChangePasswordDialog();
                    dialog.show(getParentFragmentManager(), "change_password");
                } catch (Exception e) {
                    Log.e(TAG, "Error opening change password dialog", e);
                    Toast.makeText(getContext(), "Greška pri otvaranju dijaloga", Toast.LENGTH_SHORT).show();
                }
            });

            if (binding.cardQrCode != null) {
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
            }

            if (binding.btnGoToEquipment != null) {
                binding.btnGoToEquipment.setOnClickListener(v -> {
                    try {
                        // Navigate to Equipment tab
                        if (getActivity() instanceof MainActivity) {
                            MainActivity mainActivity = (MainActivity) getActivity();
                            mainActivity.navigateToEquipment();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to equipment", e);
                        Toast.makeText(getContext(), "Greška pri navigaciji", Toast.LENGTH_SHORT).show();
                    }
                });
            }

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

            // DODANO ZA STATISTIKE - postavi default vrednosti:
            if (binding.tvCompletionRate != null) binding.tvCompletionRate.setText("0%");
            if (binding.tvSkippedTasks != null) binding.tvSkippedTasks.setText("0");
            if (binding.tvCanceledTasks != null) binding.tvCanceledTasks.setText("0");
            if (binding.tvSpecialMissionsStarted != null) binding.tvSpecialMissionsStarted.setText("0");
            if (binding.tvSpecialMissionsCompleted != null) binding.tvSpecialMissionsCompleted.setText("0");

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
                                // DODANO ZA STATISTIKE:
                                loadStatistics();
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

    @Override
    public void onResume() {
        super.onResume();
        try {
            // Refresh data when fragment becomes visible
            if (currentUser != null) {
                loadUserEquipment();
                // DODANO ZA STATISTIKE:
                loadStatistics();
            }
            Log.d(TAG, "ProfileFragment resumed");

        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
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
                        // DODANO ZA STATISTIKE:
                        loadStatistics();
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
            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            if (firebaseUser == null) {
                displayEquipment(new ArrayList<>());
                return;
            }

            String userId = firebaseUser.getUid();

            db.collection(Constants.COLLECTION_EQUIPMENT)
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Equipment> userEquipment = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Equipment equipment = document.toObject(Equipment.class);
                            equipment.setId(document.getId());
                            userEquipment.add(equipment);
                        }

                        displayEquipment(userEquipment);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading user equipment", e);
                        displayEquipment(new ArrayList<>());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error loading user equipment", e);
            displayEquipment(new ArrayList<>());
        }
    }

    private void displayEquipment(List<Equipment> equipmentList) {
        try {
            if (binding == null) return;

            // Get active equipment only
            List<Equipment> activeEquipmentList = EquipmentManager.getActiveEquipment(equipmentList);

            if (activeEquipmentList.isEmpty()) {
                // Show "no equipment" message
                if (binding.tvNoEquipment != null) {
                    binding.tvNoEquipment.setVisibility(View.VISIBLE);
                    binding.tvNoEquipment.setText("Nemate aktivnu opremu");
                }
                if (binding.rvEquipment != null) {
                    binding.rvEquipment.setVisibility(View.GONE);
                }
                if (binding.tvEquipmentBonus != null) {
                    binding.tvEquipmentBonus.setVisibility(View.GONE);
                }
            } else {
                // Show equipment list
                if (binding.tvNoEquipment != null) {
                    binding.tvNoEquipment.setVisibility(View.GONE);
                }
                if (binding.rvEquipment != null) {
                    binding.rvEquipment.setVisibility(View.VISIBLE);
                    activeEquipment.clear();
                    activeEquipment.addAll(activeEquipmentList);
                    if (equipmentAdapter != null) {
                        equipmentAdapter.notifyDataSetChanged();
                    }
                }

                // Show equipment bonus summary
                String bonusText = calculateEquipmentBonuses(activeEquipmentList);
                if (binding.tvEquipmentBonus != null && !bonusText.isEmpty()) {
                    binding.tvEquipmentBonus.setVisibility(View.VISIBLE);
                    binding.tvEquipmentBonus.setText(bonusText);
                } else if (binding.tvEquipmentBonus != null) {
                    binding.tvEquipmentBonus.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error displaying equipment", e);
        }
    }

    private String calculateEquipmentBonuses(List<Equipment> activeEquipment) {
        try {
            if (activeEquipment == null || activeEquipment.isEmpty()) {
                return "";
            }

            List<String> bonuses = new ArrayList<>();
            double totalPpBoost = 0;
            double totalAttackBoost = 0;
            double totalCoinBoost = 0;
            int extraAttacks = 0;

            for (Equipment equipment : activeEquipment) {
                if (!equipment.isActive()) continue;

                switch (equipment.getEffectType()) {
                    case Constants.EFFECT_PP_BOOST:
                        totalPpBoost += equipment.getEffectValue();
                        break;
                    case Constants.EFFECT_ATTACK_BOOST:
                        totalAttackBoost += equipment.getEffectValue();
                        break;
                    case Constants.EFFECT_COIN_BOOST:
                        totalCoinBoost += equipment.getEffectValue();
                        break;
                    case "extra_attack":
                        extraAttacks++;
                        break;
                }
            }

            if (totalPpBoost > 0) {
                bonuses.add(String.format("PP +%.1f%%", totalPpBoost * 100));
            }
            if (totalAttackBoost > 0) {
                bonuses.add(String.format("Napad +%.1f%%", totalAttackBoost * 100));
            }
            if (totalCoinBoost > 0) {
                bonuses.add(String.format("Novčići +%.1f%%", totalCoinBoost * 100));
            }
            if (extraAttacks > 0) {
                bonuses.add(String.format("+%d napad%s", extraAttacks, extraAttacks > 1 ? "a" : ""));
            }

            return String.join(", ", bonuses);

        } catch (Exception e) {
            Log.e(TAG, "Error calculating equipment bonuses", e);
            return "";
        }
    }

    private void updateEquipmentBonus(List<Equipment> equipmentList) {
        try {
            String bonusText = EquipmentManager.getEquipmentSummary(equipmentList);

            // If you have a TextView for equipment bonus in your layout:
            if (binding.tvEquipmentBonus != null) {
                binding.tvEquipmentBonus.setText(bonusText);
                binding.tvEquipmentBonus.setVisibility(
                        EquipmentManager.hasActiveEquipment(equipmentList) ? View.VISIBLE : View.GONE
                );
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating equipment bonus", e);
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

    // ========== DODANO ZA STATISTIKE ==========

    private void loadStatistics() {
        try {
            if (currentUser == null) return;

            displayBasicStats();
            loadTasksDonutChart();
            loadCategoryBarChart();
            loadXpLineChart();
            loadDifficultyLineChart();

        } catch (Exception e) {
            Log.e(TAG, "Error loading statistics", e);
        }
    }

    private void displayBasicStats() {
        try {
            if (currentUser == null || binding == null) return;

            // Calculate active days
            int activeDays = calculateActiveDays();
            if (binding.tvActiveDays != null) {
                binding.tvActiveDays.setText(String.valueOf(activeDays));
            }

            // Display basic user stats - proverava da li postoje
            if (binding.tvTotalTasks != null) {
                binding.tvTotalTasks.setText(String.valueOf(currentUser.getTotalTasksCreated()));
            }
            if (binding.tvCompletedTasks != null) {
                binding.tvCompletedTasks.setText(String.valueOf(currentUser.getTotalTasksCompleted()));
            }
            if (binding.tvSkippedTasks != null) {
                binding.tvSkippedTasks.setText(String.valueOf(currentUser.getTotalTasksSkipped()));
            }
            if (binding.tvCanceledTasks != null) {
                binding.tvCanceledTasks.setText(String.valueOf(currentUser.getTotalTasksCanceled()));
            }
            if (binding.tvLongestStreak != null) {
                binding.tvLongestStreak.setText(String.valueOf(currentUser.getLongestStreak()));
            }

            // Calculate completion rate
            double completionRate = currentUser.getTotalTasksCreated() > 0 ?
                    (double) currentUser.getTotalTasksCompleted() / currentUser.getTotalTasksCreated() * 100 : 0;
            if (binding.tvCompletionRate != null) {
                binding.tvCompletionRate.setText(String.format("%.1f%%", completionRate));
            }

            // Special missions - samo ako postoje u layout-u
            if (binding.tvSpecialMissionsStarted != null) {
                binding.tvSpecialMissionsStarted.setText("0");
            }
            if (binding.tvSpecialMissionsCompleted != null) {
                binding.tvSpecialMissionsCompleted.setText("0");
            }

            Log.d(TAG, "Basic stats displayed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying basic stats", e);
        }
    }

    private int calculateActiveDays() {
        try {
            // Simple calculation based on registration time
            long registrationTime = currentUser.getRegistrationTime();
            if (registrationTime == 0) return currentUser.getActiveDays();

            long currentTime = System.currentTimeMillis();
            long daysSinceRegistration = (currentTime - registrationTime) / (1000 * 60 * 60 * 24);

            return Math.max(1, (int) Math.min(daysSinceRegistration + 1, currentUser.getActiveDays()));

        } catch (Exception e) {
            Log.e(TAG, "Error calculating active days", e);
            return Math.max(1, currentUser.getActiveDays());
        }
    }

    private void loadTasksDonutChart() {
        try {
            if (binding == null || binding.chartTasksDonut == null) {
                Log.d(TAG, "Tasks donut chart not available in layout");
                return;
            }

            PieChart chart = binding.chartTasksDonut;

            List<PieEntry> entries = new ArrayList<>();

            // Mock podaci umesto realnih
            if (currentUser != null) {
                int completed = currentUser.getTotalTasksCompleted();
                int skipped = currentUser.getTotalTasksSkipped();
                int canceled = currentUser.getTotalTasksCanceled();

                // Ako nema realnih podataka, koristi mock podatke
                if (completed == 0 && skipped == 0 && canceled == 0) {
                    entries.add(new PieEntry(12, "Završeni"));
                    entries.add(new PieEntry(3, "Preskočeni"));
                    entries.add(new PieEntry(2, "Otkazani"));
                } else {
                    if (completed > 0) entries.add(new PieEntry(completed, "Završeni"));
                    if (skipped > 0) entries.add(new PieEntry(skipped, "Preskočeni"));
                    if (canceled > 0) entries.add(new PieEntry(canceled, "Otkazani"));
                }
            } else {
                // Default mock podaci
                entries.add(new PieEntry(15, "Završeni"));
                entries.add(new PieEntry(4, "Preskočeni"));
                entries.add(new PieEntry(1, "Otkazani"));
            }

            if (!entries.isEmpty()) {
                PieDataSet dataSet = new PieDataSet(entries, "Zadaci");
                dataSet.setColors(
                        Color.parseColor("#4CAF50"), // Green for completed
                        Color.parseColor("#FF9800"), // Orange for skipped
                        Color.parseColor("#F44336")  // Red for canceled
                );

                // Stilizovanje
                dataSet.setValueTextSize(12f);
                dataSet.setValueTextColor(Color.WHITE);

                PieData data = new PieData(dataSet);
                chart.setData(data);

                // Ukloni opis ispod
                Description desc = new Description();
                desc.setText("");
                chart.setDescription(desc);

                chart.invalidate();

                Log.d(TAG, "Tasks donut chart loaded with " + entries.size() + " entries");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading tasks donut chart", e);
        }
    }

    private void loadCategoryBarChart() {
        try {
            if (binding == null || binding.chartCategoryBar == null) {
                Log.d(TAG, "Category bar chart not available in layout");
                return;
            }

            BarChart chart = binding.chartCategoryBar;

            // Mock podaci za kategorije
            List<BarEntry> entries = new ArrayList<>();
            entries.add(new BarEntry(0f, 8f)); // Zdravlje
            entries.add(new BarEntry(1f, 12f)); // Učenje
            entries.add(new BarEntry(2f, 5f)); // Zabava
            entries.add(new BarEntry(3f, 7f)); // Sređivanje
            entries.add(new BarEntry(4f, 3f)); // Sport

            BarDataSet dataSet = new BarDataSet(entries, "Zadaci po kategoriji");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setValueTextSize(10f);

            BarData data = new BarData(dataSet);
            data.setBarWidth(0.8f);

            chart.setData(data);

            // Stilizovanje chart-a
            Description desc = new Description();
            desc.setText("");
            chart.setDescription(desc);

            // X-axis labels
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new ValueFormatter() {
                private String[] labels = {"Zdravlje", "Učenje", "Zabava", "Sređivanje", "Sport"};

                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < labels.length) {
                        return labels[index];
                    }
                    return "";
                }
            });
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);

            chart.invalidate();

            Log.d(TAG, "Category bar chart loaded with " + entries.size() + " categories");

        } catch (Exception e) {
            Log.e(TAG, "Error loading category bar chart", e);
        }
    }

    private void loadXpLineChart() {
        try {
            // Proveri da li chart postoji u layout-u
            if (binding == null || binding.chartXpLine == null) {
                Log.d(TAG, "XP line chart not available in layout");
                return;
            }

            // TODO: Load actual XP data from database
            // For now, show placeholder data
            LineChart chart = binding.chartXpLine;

            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                entries.add(new Entry(i, (float) (Math.random() * 50)));
            }

            LineDataSet dataSet = new LineDataSet(entries, "XP po danu");
            dataSet.setColor(Color.parseColor("#2196F3"));
            dataSet.setCircleColor(Color.parseColor("#2196F3"));

            LineData data = new LineData(dataSet);
            chart.setData(data);
            chart.invalidate();

        } catch (Exception e) {
            Log.e(TAG, "Error loading XP line chart", e);
        }
    }

    private void loadDifficultyLineChart() {
        try {
            // Proveri da li chart postoji u layout-u
            if (binding == null || binding.chartDifficultyLine == null) {
                Log.d(TAG, "Difficulty line chart not available in layout");
                return;
            }

            // TODO: Load actual difficulty data from database
            // For now, show placeholder data
            LineChart chart = binding.chartDifficultyLine;

            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                entries.add(new Entry(i, (float) (Math.random() * 10 + 5)));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Prosečna težina");
            dataSet.setColor(Color.parseColor("#FF5722"));
            dataSet.setCircleColor(Color.parseColor("#FF5722"));

            LineData data = new LineData(dataSet);
            chart.setData(data);
            chart.invalidate();

        } catch (Exception e) {
            Log.e(TAG, "Error loading difficulty line chart", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}