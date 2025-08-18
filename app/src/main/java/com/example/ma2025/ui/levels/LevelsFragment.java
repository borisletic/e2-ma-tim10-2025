package com.example.ma2025.ui.levels;

import android.animation.ObjectAnimator;
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
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.databinding.FragmentLevelsBinding;
import com.example.ma2025.ui.levels.adapter.LevelProgressAdapter;
import com.example.ma2025.ui.levels.dialog.LevelUpDialog;
import com.example.ma2025.ui.levels.model.LevelInfo;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.GameLogicUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class LevelsFragment extends Fragment {

    private static final String TAG = "LevelsFragment";
    private FragmentLevelsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferencesManager preferencesManager;
    private User currentUser;
    private LevelProgressAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLevelsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        setupUI();
        loadUserData();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        preferencesManager = new PreferencesManager(requireContext());
    }

    private void setupUI() {
        // Setup RecyclerView for level progression
        adapter = new LevelProgressAdapter();
        binding.recyclerViewLevels.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewLevels.setAdapter(adapter);

        // Setup test buttons for demonstration
        binding.btnAddXp.setOnClickListener(v -> addTestXp());
        binding.btnAddMoreXp.setOnClickListener(v -> addMoreTestXp());
        binding.btnResetProgress.setOnClickListener(v -> resetProgress());
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            showErrorState();
            return;
        }

        String userId = firebaseUser.getUid();

        // Try to load from Firebase first
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            displayUserProgress();
                            generateLevelsList();
                        } else {
                            createDefaultUser(userId);
                        }
                    } else {
                        createDefaultUser(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    createDefaultUser(userId);
                });
    }

    private void createDefaultUser(String userId) {
        currentUser = new User();
        currentUser.setUid(userId);
        currentUser.setUsername("Test User");
        currentUser.setLevel(0);
        currentUser.setXp(0);
        currentUser.setPp(0);
        currentUser.setCoins(0);
        currentUser.setTitle("Novajlija");
        currentUser.setAvatar("avatar_1");

        displayUserProgress();
        generateLevelsList();

        // Save to Firebase
        saveUserToFirebase();
    }

    private void displayUserProgress() {
        if (currentUser == null) return;

        try {
            // Current level info
            binding.tvCurrentLevel.setText("Nivo " + currentUser.getLevel());
            binding.tvCurrentTitle.setText(currentUser.getTitle());
            binding.tvCurrentXp.setText(currentUser.getXp() + " XP");
            binding.tvCurrentPp.setText(currentUser.getPp() + " PP");
            binding.tvCurrentCoins.setText(currentUser.getCoins() + " novčića");

            // Next level info
            int nextLevelXp = currentUser.getXpForNextLevel();
            binding.tvNextLevelXp.setText("Sledeći nivo: " + nextLevelXp + " XP");

            int remainingXp = Math.max(0, nextLevelXp - currentUser.getXp());
            binding.tvRemainingXp.setText("Preostalo: " + remainingXp + " XP");

            // Progress calculation
            int currentLevelBaseXp = currentUser.getLevel() == 0 ? 0 :
                    GameLogicUtils.calculateXpForLevel(currentUser.getLevel() - 1);
            int currentLevelTotalXp = nextLevelXp - currentLevelBaseXp;
            int currentLevelProgress = currentUser.getXp() - currentLevelBaseXp;

            int progressPercentage = currentLevelTotalXp > 0 ?
                    (int) ((float) currentLevelProgress / currentLevelTotalXp * 100) : 0;

            // Animate progress bar
            ObjectAnimator progressAnimator = ObjectAnimator.ofInt(
                    binding.progressBarXp, "progress", 0, progressPercentage);
            progressAnimator.setDuration(800);
            progressAnimator.start();

            binding.tvProgressPercentage.setText(progressPercentage + "%");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying user progress", e);
        }
    }

    private void generateLevelsList() {
        List<LevelInfo> levels = new ArrayList<>();

        // Generate levels 0-10 for display
        for (int i = 0; i <= 10; i++) {
            LevelInfo levelInfo = new LevelInfo();
            levelInfo.setLevel(i);
            levelInfo.setXpRequired(GameLogicUtils.calculateXpForLevel(i));
            levelInfo.setPpReward(GameLogicUtils.calculatePpForLevel(i));
            levelInfo.setTitle(getTitleForLevel(i));
            levelInfo.setUnlocked(currentUser.getLevel() >= i);
            levelInfo.setCurrent(currentUser.getLevel() == i);

            levels.add(levelInfo);
        }

        adapter.updateLevels(levels);
    }

    private String getTitleForLevel(int level) {
        switch (level) {
            case 0: return "Novajlija";
            case 1: return "Početnik";
            case 2: return "Istraživač";
            case 3: return "Ratnik";
            case 4: return "Veteran";
            case 5: return "Majstor";
            case 6: return "Ekspert";
            case 7: return "Šampion";
            case 8: return "Legenda";
            case 9: return "Mitska Legenda";
            case 10: return "Besmrtni";
            default: return "Legenda (Nivo " + level + ")";
        }
    }

    // Test methods for demonstration
    private void addTestXp() {
        if (currentUser == null) return;

        int xpToAdd = 25;
        currentUser.addXp(xpToAdd);

        Toast.makeText(getContext(), "Dodano " + xpToAdd + " XP!", Toast.LENGTH_SHORT).show();

        checkLevelUp();
        displayUserProgress();
        generateLevelsList();
        saveUserToFirebase();
    }

    private void addMoreTestXp() {
        if (currentUser == null) return;

        int xpToAdd = 100;
        currentUser.addXp(xpToAdd);

        Toast.makeText(getContext(), "Dodano " + xpToAdd + " XP!", Toast.LENGTH_SHORT).show();

        checkLevelUp();
        displayUserProgress();
        generateLevelsList();
        saveUserToFirebase();
    }

    private void resetProgress() {
        if (currentUser == null) return;

        currentUser.setLevel(0);
        currentUser.setXp(0);
        currentUser.setPp(0);
        currentUser.setCoins(0);
        currentUser.setTitle("Novajlija");

        Toast.makeText(getContext(), "Napredak resetovan!", Toast.LENGTH_SHORT).show();

        displayUserProgress();
        generateLevelsList();
        saveUserToFirebase();
    }

    private void checkLevelUp() {
        if (currentUser == null) return;

        boolean leveledUp = false;
        while (currentUser.canLevelUp()) {
            int oldLevel = currentUser.getLevel();
            currentUser.levelUp();
            leveledUp = true;

            Log.d(TAG, "Level up! From " + oldLevel + " to " + currentUser.getLevel());

            // Show level up notification
            showLevelUpDialog(oldLevel, currentUser.getLevel());
        }

        if (leveledUp) {
            // Update cached data
            preferencesManager.cacheUserData(
                    currentUser.getUsername(),
                    currentUser.getLevel(),
                    currentUser.getTitle(),
                    currentUser.getXp(),
                    currentUser.getPp(),
                    currentUser.getCoins()
            );
        }
    }

    private void showLevelUpDialog(int oldLevel, int newLevel) {
        if (getContext() == null || currentUser == null) return;

        int ppGained = GameLogicUtils.calculatePpForLevel(newLevel) -
                GameLogicUtils.calculatePpForLevel(oldLevel);

        LevelUpDialog dialog = LevelUpDialog.newInstance(
                oldLevel,
                newLevel,
                currentUser.getTitle(),
                ppGained,
                currentUser.getPp()
        );

        dialog.show(getParentFragmentManager(), "level_up_dialog");
    }

    private void saveUserToFirebase() {
        if (currentUser == null || mAuth.getCurrentUser() == null) return;

        db.collection(Constants.COLLECTION_USERS)
                .document(mAuth.getCurrentUser().getUid())
                .set(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data", e);
                });
    }

    private void showErrorState() {
        if (binding != null) {
            binding.tvCurrentLevel.setText("Greška");
            binding.tvCurrentTitle.setText("Nije moguće učitati podatke");
            Toast.makeText(getContext(), "Greška pri učitavanju napredovanja", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}