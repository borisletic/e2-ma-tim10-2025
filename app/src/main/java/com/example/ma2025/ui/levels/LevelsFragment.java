// LevelsFragment.java - KOMPLETNA ZAMENA sa integracijom Task sistema

package com.example.ma2025.ui.levels;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.R;
import com.example.ma2025.data.models.User;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.databinding.FragmentLevelsBinding;
import com.example.ma2025.ui.levels.adapter.LevelProgressAdapter;
import com.example.ma2025.ui.levels.model.LevelInfo;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.GameLogicUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

// DODANI IMPORTOVI ZA TASK INTEGRATION:
import com.example.ma2025.data.repositories.TaskRepository;
import com.example.ma2025.data.database.entities.UserProgressEntity;
import com.example.ma2025.data.database.entities.TaskEntity;

import java.util.ArrayList;
import java.util.List;

public class LevelsFragment extends Fragment {

    private static final String TAG = "LevelsFragment";
    private FragmentLevelsBinding binding;

    // Firebase components
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferencesManager preferencesManager;

    // User data
    private User currentUser; // Firebase User model za UI display
    private UserProgressEntity userProgress; // Task system progress

    // Task integration
    private TaskRepository taskRepository;

    // UI components
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

        try {
            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            preferencesManager = new PreferencesManager(requireContext());

            // KLJUČNO: Initialize TaskRepository
            taskRepository = TaskRepository.getInstance(requireContext());

            setupRecyclerView();
            setupButtons();

            // Load data from both Firebase and Task system
            loadUserData();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing LevelsFragment", e);
            Toast.makeText(getContext(), "Greška pri učitavanju", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        try {
            // ISPRAVNO: koristi recyclerViewLevels (camelCase za binding)
            adapter = new LevelProgressAdapter(); // Postojeći adapter nema argumente u konstruktoru
            binding.recyclerViewLevels.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.recyclerViewLevels.setAdapter(adapter);

            Log.d(TAG, "RecyclerView setup successful with existing adapter");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up recycler view", e);
        }
    }

    private void setupButtons() {
        try {
            // POSTOJEĆI TEST BUTTON-I iz layout-a - zameniti sa Task integracijom
            binding.btnAddXp.setOnClickListener(v -> {
                // UMESTO addTestXp() - koristi TaskRepository simulaciju
                simulateTaskCompletion();
            });

            binding.btnAddMoreXp.setOnClickListener(v -> {
                // UMESTO addMoreTestXp() - koristi veći XP gain simulaciju
                simulateTaskCompletionBig();
            });

            binding.btnResetProgress.setOnClickListener(v -> {
                // UMESTO resetProgress() - koristi TaskRepository reset
                resetUserProgress();
            });

            Log.d(TAG, "Buttons setup with Task integration");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up buttons", e);
        }
    }



    private void simulateTaskCompletionBig() {
        if (userProgress == null) {
            Toast.makeText(getContext(), "Učitavam podatke...", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String userId = preferencesManager.getUserId();
            if (userId == null) return;

            // Simuliraj završetak "teškog" zadatka sa važnom bitnosti
            simulateTaskWithDifficulty(TaskEntity.DIFFICULTY_HARD, TaskEntity.IMPORTANCE_IMPORTANT);

        } catch (Exception e) {
            Log.e(TAG, "Error simulating big task completion", e);
            Toast.makeText(getContext(), "Greška pri simulaciji", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetUserProgress() {
        if (taskRepository == null) return;

        String userId = preferencesManager.getUserId();
        if (userId == null) return;

        // Prikaži potvrdu
        new AlertDialog.Builder(requireContext())
                .setTitle("Reset napretka")
                .setMessage("Da li ste sigurni da želite da resetujete napredak? Ova akcija se ne može poništiti.")
                .setPositiveButton("Da", (dialog, which) -> {
                    try {
                        // Resetuj TaskRepository UserProgress
                        taskRepository.resetUserProgress(userId, new TaskRepository.OnTaskStatusChangeCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(getContext(), "Napredak resetovan!", Toast.LENGTH_SHORT).show();
                                // Reload data
                                loadUserData();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error resetting progress", e);
                        Toast.makeText(getContext(), "Greška pri resetovanju", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Ne", null)
                .show();
    }

    private void simulateTaskWithDifficulty(int difficulty, int importance) {
        if (userProgress == null || taskRepository == null) return;

        try {
            int currentLevel = userProgress.currentLevel;

            // Kalkuliraj XP kao pravi Task sistem
            int difficultyXp = getDifficultyBaseXp(difficulty);
            int importanceXp = getImportanceBaseXp(importance);

            int finalDifficultyXp = GameLogicUtils.calculateDifficultyXp(difficultyXp, currentLevel);
            int finalImportanceXp = GameLogicUtils.calculateImportanceXp(importanceXp, currentLevel);
            int totalXp = finalDifficultyXp + finalImportanceXp;

            // Dodaj XP koristeTaskRepository
            String userId = preferencesManager.getUserId();
            taskRepository.addXpToUser(userId, totalXp, new TaskRepository.OnTaskCompletedCallback() {
                @Override
                public void onSuccess(int xpEarned, int newLevel) {
                    // ISPRAVKA: Toast na main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (newLevel > userProgress.currentLevel) {
                            // Level up!
                            showLevelUpDialog(userProgress.currentLevel, newLevel,
                                    GameLogicUtils.calculatePpForLevel(newLevel));
                            Toast.makeText(getContext(),
                                    String.format("LEVEL UP! Nivo %d! +%d XP", newLevel, xpEarned),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(),
                                    String.format("Zadatak završen! +%d XP", xpEarned),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    // UI će se automatski ažurirati preko LiveData observer-a
                }

                @Override
                public void onError(String error) {
                    // ISPRAVKA: Toast na main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error simulating task with difficulty", e);
        }
    }

    private int getDifficultyBaseXp(int difficulty) {
        switch (difficulty) {
            case TaskEntity.DIFFICULTY_VERY_EASY: return 1;   // Constants.XP_VERY_EASY
            case TaskEntity.DIFFICULTY_EASY: return 3;        // Constants.XP_EASY
            case TaskEntity.DIFFICULTY_HARD: return 7;        // Constants.XP_HARD
            case TaskEntity.DIFFICULTY_EXTREME: return 20;    // Constants.XP_EXTREME
            default: return 1;
        }
    }

    private int getImportanceBaseXp(int importance) {
        switch (importance) {
            case TaskEntity.IMPORTANCE_NORMAL: return 1;          // Constants.XP_NORMAL
            case TaskEntity.IMPORTANCE_IMPORTANT: return 3;       // Constants.XP_IMPORTANT
            case TaskEntity.IMPORTANCE_VERY_IMPORTANT: return 10; // Constants.XP_VERY_IMPORTANT
            case TaskEntity.IMPORTANCE_SPECIAL: return 100;       // Constants.XP_SPECIAL
            default: return 1;
        }
    }

    private void setupUI() {
        try {
            // ISPRAVNO: koristi postojeće binding-ove
            setupRecyclerView(); // binding.recyclerViewLevels
            setupButtons();      // binding.btnAddXp, btnAddMoreXp, btnResetProgress

            Log.d(TAG, "UI setup completed with Task integration");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI", e);
        }
    }

    private void loadUserData() {
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "User ID is null");
            createDefaultData();
            return;
        }

        Log.d(TAG, "Loading user data for: " + userId);

        // PRVO: Load Firebase User podatke (username, avatar, basic info)
        loadFirebaseUser(userId);

        // DRUGO: Load Task system progress (XP, level, PP - AUTORITATIVE)
        loadTaskProgress(userId);
    }

    private void loadFirebaseUser(String userId) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        if (documentSnapshot.exists()) {
                            currentUser = documentSnapshot.toObject(User.class);
                            if (currentUser != null) {
                                Log.d(TAG, "Firebase user loaded: " + currentUser.getUsername());

                                // Prikaži osnovne podatke odmah
                                displayBasicUserInfo();
                            } else {
                                createDefaultUser(userId);
                            }
                        } else {
                            createDefaultUser(userId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase user", e);
                        createDefaultUser(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading Firebase user", e);
                    createDefaultUser(userId);
                });
    }

    private void loadTaskProgress(String userId) {
        // KLJUČNO: Koristi TaskRepository za autorativne level/XP podatke
        taskRepository.getUserProgress(userId).observe(getViewLifecycleOwner(), progressEntity -> {
            if (progressEntity != null) {
                userProgress = progressEntity;

                Log.d(TAG, String.format("Task progress loaded - Level: %d, XP: %d, PP: %d, Coins: %d",
                        progressEntity.currentLevel, progressEntity.currentXp,
                        progressEntity.totalPp, progressEntity.coins));

                // Sync Firebase User sa Task progress podacima
                syncUserWithTaskProgress();

                // Prikaži kompletan progress
                displayUserProgress();
                generateLevelsList();

            } else {
                Log.d(TAG, "No task progress found, creating default");
                createDefaultTaskProgress(userId);
            }
        });
    }

    private void syncUserWithTaskProgress() {
        if (currentUser == null || userProgress == null) return;

        try {
            // SYNC: Firebase User sa Task system podacima
            currentUser.setLevel(userProgress.currentLevel);
            currentUser.setXp(userProgress.currentXp);
            currentUser.setPp(userProgress.totalPp);
            currentUser.setCoins(userProgress.coins);
            currentUser.setTitle(getTitleForLevel(userProgress.currentLevel));

            Log.d(TAG, "User synced with task progress");

            // Save updated user back to Firebase
            saveUserToFirebase();

        } catch (Exception e) {
            Log.e(TAG, "Error syncing user with task progress", e);
        }
    }

    private void createDefaultUser(String userId) {
        currentUser = new User();
        currentUser.setUid(userId);
        currentUser.setUsername("Korisnik");
        currentUser.setLevel(0);
        currentUser.setXp(0);
        currentUser.setPp(0);
        currentUser.setCoins(0);
        currentUser.setTitle("Novajlija");
        currentUser.setAvatar("avatar_1");

        displayBasicUserInfo();
        saveUserToFirebase();
    }

    private void createDefaultTaskProgress(String userId) {
        try {
            // TaskRepository će automatski kreirati UserProgressEntity
            // kada se pozove getUserProgress ako ne postoji

            // Direktan poziv - bez delay-a
            loadTaskProgress(userId);

        } catch (Exception e) {
            Log.e(TAG, "Error creating default task progress", e);
        }
    }

    private void createDefaultData() {
        // Fallback ako nema user ID
        currentUser = new User();
        currentUser.setLevel(0);
        currentUser.setXp(0);
        currentUser.setUsername("Korisnik");
        currentUser.setTitle("Novajlija");

        displayUserProgress();
        generateLevelsList();
    }

    private void displayBasicUserInfo() {
        if (currentUser == null || binding == null) return;

        try {
            // Prikaži basic info iz Firebase
            binding.tvCurrentTitle.setText(currentUser.getTitle() != null ? currentUser.getTitle() : "Novajlija");

            Log.d(TAG, "Basic user info displayed");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying basic user info", e);
        }
    }

    private void displayUserProgress() {
        if (binding == null) return;

        try {
            // Use either userProgress (authoritative) or currentUser (fallback)
            int level = userProgress != null ? userProgress.currentLevel :
                    (currentUser != null ? currentUser.getLevel() : 0);
            int xp = userProgress != null ? userProgress.currentXp :
                    (currentUser != null ? currentUser.getXp() : 0);
            int pp = userProgress != null ? userProgress.totalPp :
                    (currentUser != null ? currentUser.getPp() : 0);
            int coins = userProgress != null ? userProgress.coins :
                    (currentUser != null ? currentUser.getCoins() : 0);

            // Current level info
            binding.tvCurrentLevel.setText("Nivo " + level);
            binding.tvCurrentTitle.setText(getTitleForLevel(level));
            binding.tvCurrentXp.setText(xp + " XP");
            binding.tvCurrentPp.setText(pp + " PP");
            binding.tvCurrentCoins.setText(coins + " novčića");

            // Next level info - KORISTI GAMELOGICUTILS
            int nextLevelXp = GameLogicUtils.calculateXpForLevel(level + 1);
            binding.tvNextLevelXp.setText("Sledeći nivo: " + nextLevelXp + " XP");

            int remainingXp = Math.max(0, nextLevelXp - xp);
            binding.tvRemainingXp.setText("Preostalo: " + remainingXp + " XP");

            // Progress calculation - REALNA FORMULA
            int currentLevelBaseXp = level == 0 ? 0 :
                    GameLogicUtils.calculateTotalXpForLevel(level);
            int currentLevelTotalXp = nextLevelXp - currentLevelBaseXp;
            int currentLevelProgress = Math.max(0, xp - currentLevelBaseXp);

            int progressPercentage = currentLevelTotalXp > 0 ?
                    (int) ((float) currentLevelProgress / currentLevelTotalXp * 100) : 0;

            // Animate progress bar
            ObjectAnimator progressAnimator = ObjectAnimator.ofInt(
                    binding.progressBarXp, "progress", 0, Math.min(100, progressPercentage));
            progressAnimator.setDuration(800);
            progressAnimator.start();

            binding.tvProgressPercentage.setText(Math.min(100, progressPercentage) + "%");

            Log.d(TAG, String.format("Progress displayed - Level: %d, XP: %d/%d, Progress: %d%%",
                    level, xp, nextLevelXp, progressPercentage));

        } catch (Exception e) {
            Log.e(TAG, "Error displaying user progress", e);
        }
    }

    private void generateLevelsList() {
        try {
            List<LevelInfo> levels = new ArrayList<>();

            int currentLevel = userProgress != null ? userProgress.currentLevel :
                    (currentUser != null ? currentUser.getLevel() : 0);

            // Generate levels 0-10 for display - KORISTI GAMELOGICUTILS
            for (int i = 0; i <= 10; i++) {
                LevelInfo levelInfo = new LevelInfo();
                levelInfo.setLevel(i);
                levelInfo.setXpRequired(GameLogicUtils.calculateXpForLevel(i));
                levelInfo.setPpReward(GameLogicUtils.calculatePpForLevel(i));
                levelInfo.setTitle(getTitleForLevel(i));
                levelInfo.setUnlocked(currentLevel >= i);
                levelInfo.setCurrent(currentLevel == i);

                levels.add(levelInfo);
            }

            if (adapter != null) {
                adapter.updateLevels(levels);
                Log.d(TAG, "Levels list generated with " + levels.size() + " levels");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating levels list", e);
        }
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

    private void saveUserToFirebase() {
        if (currentUser == null) return;

        String userId = preferencesManager.getUserId();
        if (userId == null) return;

        try {
            db.collection(Constants.COLLECTION_USERS)
                    .document(userId)
                    .set(currentUser)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved to Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving user to Firebase", e));

        } catch (Exception e) {
            Log.e(TAG, "Error in saveUserToFirebase", e);
        }
    }

    // DEMO METODA: Simulacija task completion-a
    private void simulateTaskCompletion() {
        if (userProgress == null) {
            Toast.makeText(getContext(), "Učitavam podatke...", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String userId = preferencesManager.getUserId();
            if (userId == null) return;

            // Simuliraj XP gain iz task completion-a
            int difficultyXp = 3; // Easy task
            int importanceXp = 1; // Normal importance
            int currentLevel = userProgress.currentLevel;

            // Koristi GameLogicUtils za kalkulaciju kao pravi Task sistem
            int finalDifficultyXp = GameLogicUtils.calculateDifficultyXp(difficultyXp, currentLevel);
            int finalImportanceXp = GameLogicUtils.calculateImportanceXp(importanceXp, currentLevel);
            int totalXp = finalDifficultyXp + finalImportanceXp;

            // Dodaj XP i proveri level up
            userProgress.addXp(totalXp);

            boolean leveledUp = false;
            int oldLevel = userProgress.currentLevel;

            // Check level up - KORISTI GAMELOGICUTILS
            int requiredXp = GameLogicUtils.calculateXpForLevel(userProgress.currentLevel + 1);
            if (userProgress.currentXp >= requiredXp) {
                int newLevel = userProgress.currentLevel + 1;
                int ppGained = GameLogicUtils.calculatePpForLevel(newLevel);
                userProgress.levelUp(newLevel, ppGained);
                leveledUp = true;

                Log.d(TAG, String.format("LEVEL UP! %d -> %d, PP gained: %d", oldLevel, newLevel, ppGained));
            }

            // Save to database
            taskRepository.updateUserProgress(userProgress);

            // Show message
            if (leveledUp) {
                showLevelUpDialog(oldLevel, userProgress.currentLevel, GameLogicUtils.calculatePpForLevel(userProgress.currentLevel));
                Toast.makeText(getContext(),
                        String.format("LEVEL UP! Nivo %d! +%d XP", userProgress.currentLevel, totalXp),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(),
                        String.format("Zadatak završen! +%d XP", totalXp),
                        Toast.LENGTH_SHORT).show();
            }

            // Refresh display
            syncUserWithTaskProgress();
            displayUserProgress();
            generateLevelsList();

        } catch (Exception e) {
            Log.e(TAG, "Error simulating task completion", e);
            Toast.makeText(getContext(), "Greška pri simulaciji", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLevelUpDialog(int oldLevel, int newLevel, int ppGained) {
        try {
            String message = String.format(
                    "🎉 ČESTITAMO! 🎉\n\n" +
                            "Napredovali ste na nivo %d!\n\n" +
                            "Nova titula: %s\n" +
                            "PP nagrada: +%d\n" +
                            "Ukupno PP: %d\n\n" +
                            "Nastavite sa odličnim radom!",
                    newLevel,
                    getTitleForLevel(newLevel),
                    ppGained,
                    userProgress != null ? userProgress.totalPp : 0
            );

            new AlertDialog.Builder(requireContext())
                    .setTitle("Level Up!")
                    .setMessage(message)
                    .setPositiveButton("Odlično!", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing level up dialog", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // UKLONITI sve test metode:
    /*
    private void addTestXp() { ... }
    private void addMoreTestXp() { ... }
    private void resetProgress() { ... }
    private void checkLevelUp() { ... }
    */
}