package com.example.ma2025.viewmodels;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.models.User;
import com.example.ma2025.data.repositories.TaskRepository;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BossViewModel extends AndroidViewModel {

    private static final String TAG = "BossViewModel";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TaskRepository taskRepository;
    private ExecutorService executor;

    // PP tracking
    private MutableLiveData<Integer> userBasePp = new MutableLiveData<>();
    private MutableLiveData<Integer> equipmentPpBonus = new MutableLiveData<>();
    private MutableLiveData<Integer> totalPp = new MutableLiveData<>();

    // Boss data
    private MutableLiveData<Integer> userLevel = new MutableLiveData<>();
    private MutableLiveData<Integer> bossMaxHp = new MutableLiveData<>();
    private MutableLiveData<Integer> bossCurrentHp = new MutableLiveData<>();

    // Status
    private MutableLiveData<String> statusMessage = new MutableLiveData<>();

    // Active equipment
    private MutableLiveData<List<Equipment>> activeEquipment = new MutableLiveData<>();
    private MutableLiveData<Integer> attackSuccessRate = new MutableLiveData<>();
    public LiveData<Integer> getAttackSuccessRate() { return attackSuccessRate; }
    private MutableLiveData<Integer> attacksRemaining = new MutableLiveData<>();
    private MutableLiveData<List<Integer>> undefeatedBossLevels = new MutableLiveData<>();
    private MutableLiveData<Integer> currentBossIndex = new MutableLiveData<>();
    private MutableLiveData<Integer> currentBossLevel = new MutableLiveData<>();

    public BossViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        taskRepository = TaskRepository.getInstance(application);
        executor = Executors.newFixedThreadPool(2);

        // Initialize values
        userBasePp.setValue(0);
        equipmentPpBonus.setValue(0);
        totalPp.setValue(0);
        userLevel.setValue(0);
        bossMaxHp.setValue(200);
        bossCurrentHp.setValue(200);
        activeEquipment.setValue(new ArrayList<>());
        attacksRemaining.setValue(5);
        currentBossIndex.setValue(0);
        undefeatedBossLevels.setValue(new ArrayList<>());
    }

    /**
     * Učitava korisničke podatke i base PP
     */
    public void loadUserPp() {
        Log.d(TAG, "=== loadUserPp() started ===");

        String userId = getCurrentUserId();
        if (userId == null) {
            Log.d(TAG, "User ID is null - user not logged in");
            statusMessage.setValue("Korisnik nije ulogovan");
            return;
        }

        Log.d(TAG, "User ID: " + userId);
        statusMessage.setValue("Učitavaju se korisnički podaci...");

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    Log.d(TAG, "Firebase query success");
                    if (document.exists()) {
                        Log.d(TAG, "Document exists");
                        User user = document.toObject(User.class);
                        if (user != null) {
                            int basePp = user.getPp();
                            int level = user.getLevel();

                            Log.d(TAG, "=== USER DATA LOADED ===");
                            Log.d(TAG, "Level: " + level);
                            Log.d(TAG, "PP: " + basePp);
                            Log.d(TAG, "========================");

                            userBasePp.setValue(basePp);
                            userLevel.setValue(1);

                            checkUndefeatedBosses(level);
                            calculateTotalPp();
                            statusMessage.setValue("Korisnik učitan: Nivo " + level + ", Osnovna PP = " + basePp);
                        } else {
                            Log.d(TAG, "User object is null");
                        }
                    } else {
                        Log.d(TAG, "Document does not exist");
                        statusMessage.setValue("Korisnik nije pronađen");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase error: " + e.getMessage(), e);
                    statusMessage.setValue("Greška pri učitavanju korisničkih podataka");
                });
    }

    public void checkUndefeatedBosses(int currentLevel) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        List<Integer> undefeatedLevels = new ArrayList<>();
        checkBossRecursively(userId, 1, currentLevel, undefeatedLevels, () -> {
            if (!undefeatedLevels.isEmpty()) {
                undefeatedBossLevels.setValue(undefeatedLevels);
                currentBossIndex.setValue(0);
                currentBossLevel.setValue(undefeatedLevels.get(0));
                loadBossForLevel(undefeatedLevels.get(0));
            } else {
                loadOrCreateBossState(currentLevel);
            }
        });
    }

    private void checkBossRecursively(String userId, int level, int maxLevel, List<Integer> undefeated, Runnable onComplete) {
        if (level > maxLevel) {
            onComplete.run();
            return;
        }

        db.collection("boss_states")
                .document(userId + "_level_" + level)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists() || !Boolean.TRUE.equals(document.getBoolean("isDefeated"))) {
                        undefeated.add(level);
                    }
                    checkBossRecursively(userId, level + 1, maxLevel, undefeated, onComplete);
                });
    }

    private void loadBossForLevel(int level) {
        String userId = getCurrentUserId();
        db.collection("boss_states")
                .document(userId + "_level_" + level)
                .get()
                .addOnSuccessListener(document -> {
                    int maxHp = calculateMaxHp(level);
                    int currentHp = document.exists() ?
                            document.getLong("currentHp").intValue() : maxHp;

                    bossMaxHp.setValue(maxHp);
                    bossCurrentHp.setValue(currentHp);
                    attacksRemaining.setValue(5);
                });
    }

    public void markCurrentBossDefeated() {
        Integer level = currentBossLevel.getValue();
        if (level == null) return;

        // Označava bosa kao poraženog u bazi
        saveBossAsDefeated(level);

        // Prelazi na sledećeg bosa
        List<Integer> levels = undefeatedBossLevels.getValue();
        Integer index = currentBossIndex.getValue();

        if (levels != null && index != null) {
            levels.remove(index.intValue());

            if (levels.isEmpty()) {
                // Svi bosovi poraženi
                statusMessage.setValue("Svi bosovi poraženi!");
            } else {
                // Učitaj sledećeg bosa
                currentBossLevel.setValue(levels.get(0));
                loadBossForLevel(levels.get(0));
            }
            undefeatedBossLevels.setValue(levels);
        }
    }

    private void saveBossAsDefeated(int level) {
        String userId = getCurrentUserId();
        Map<String, Object> update = new HashMap<>();
        update.put("isDefeated", true);
        update.put("currentHp", 0);

        db.collection("boss_states")
                .document(userId + "_level_" + level)
                .update(update);
    }

    /**
     * Učitava equipment PP bonus
     */
    public void loadEquipmentPpBonus() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection(Constants.COLLECTION_EQUIPMENT)
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", true)  // Samo aktivna oprema
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalBonus = 0;
                    int equipmentCount = 0;
                    List<Equipment> activeEquipmentList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Equipment eq = doc.toObject(Equipment.class);
                        equipmentCount++;
                        activeEquipmentList.add(eq); // Dodaj u listu aktivne opreme

                        // Saberi PP bonus iz opreme
                        if (Constants.EFFECT_PP_BOOST.equals(eq.getEffectType())) {
                            // effectValue je npr. 0.1 (10%), pretvori u PP points
                            int bonus = (int)(eq.getEffectValue() * 100);
                            totalBonus += bonus;
                            Log.d(TAG, "Equipment " + eq.getName() + " adds " + bonus + " PP");
                        }
                    }

                    equipmentPpBonus.setValue(totalBonus);
                    activeEquipment.setValue(activeEquipmentList); // Postavi aktivnu opremu
                    calculateTotalPp();

                    String message = "Oprema učitana: " + equipmentCount + " aktivna, +" + totalBonus + " PP bonus";
                    statusMessage.setValue(message);
                    Log.d(TAG, message);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading equipment", e);
                    equipmentPpBonus.setValue(0);
                    activeEquipment.setValue(new ArrayList<>()); // Postavi praznu listu
                    calculateTotalPp();
                    statusMessage.setValue("Greška pri učitavanju opreme");
                });
    }

    /**
     * Kalkuliše ukupan PP (base + equipment bonus)
     */
    private void calculateTotalPp() {
        Integer basePp = userBasePp.getValue();
        Integer bonusPp = equipmentPpBonus.getValue();

        if (basePp != null && bonusPp != null) {
            int total = basePp + bonusPp;
            totalPp.setValue(total);
            Log.d(TAG, "Total PP calculated: " + basePp + " (base) + " + bonusPp + " (equipment) = " + total);
        }
    }

    /**
     * Učitava ili kreira stanje bosa za određeni nivo
     */
    private void loadOrCreateBossState(int userLevel) {
        if (userLevel <= 0) {
            // Nema bosa za nivo 0 ili manji
            statusMessage.setValue("Dostignite nivo 1 da se borite sa prvim bosom!");
            return;
        }
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("boss_states")
                .document(userId + "_level_" + userLevel)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long currentHpLong = document.getLong("currentHp");
                        Long maxHpLong = document.getLong("maxHp");
                        Long attacksLong = document.getLong("attacksRemaining");
                        Boolean isDefeated = document.getBoolean("isDefeated");

                        // Proveri da li je boss već poražen
                        if (Boolean.TRUE.equals(isDefeated)) {
                            statusMessage.setValue("Boss za nivo " + userLevel + " je već poražen!");
                            bossMaxHp.setValue(0);
                            bossCurrentHp.setValue(0);
                            attacksRemaining.setValue(0);
                            Log.d(TAG, "Boss already defeated for level " + userLevel);
                            return;
                        }

                        Integer savedCurrentHp = currentHpLong != null ? currentHpLong.intValue() : calculateMaxHp(userLevel);
                        Integer savedMaxHp = maxHpLong != null ? maxHpLong.intValue() : calculateMaxHp(userLevel);
                        Integer savedAttacks = attacksLong != null ? attacksLong.intValue() : 5;

                        // Ne resetuj HP ako je boss već oštećen
                        bossMaxHp.setValue(savedMaxHp);
                        bossCurrentHp.setValue(savedCurrentHp);
                        attacksRemaining.setValue(savedAttacks);

                        if (attacksLong == null) {
                            saveBossState(userLevel, savedMaxHp, savedCurrentHp, savedAttacks);
                        }

                        Log.d(TAG, "Boss state loaded: " + savedCurrentHp + "/" + savedMaxHp + " HP, " + savedAttacks + " attacks");
                    } else {
                        // Kreiranje novog bosa samo ako dokument ne postoji
                        int maxHp = calculateMaxHp(userLevel);
                        bossMaxHp.setValue(maxHp);
                        bossCurrentHp.setValue(maxHp);
                        attacksRemaining.setValue(5);
                        saveBossState(userLevel, maxHp, maxHp, 5);

                        Log.d(TAG, "New boss created with " + maxHp + " HP");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading boss state", e);
                    int maxHp = calculateMaxHp(userLevel);
                    bossMaxHp.setValue(maxHp);
                    bossCurrentHp.setValue(maxHp);
                    attacksRemaining.setValue(5);
                });
    }

    /**
     * Računa maksimalan HP za boss na određenom nivou
     */
    private int calculateMaxHp(int userLevel) {
        int bossHp = 200; // Prvi boss uvek ima 200 HP

        for (int i = 1; i <= userLevel; i++) {
            int previousHp = bossHp;
            bossHp = (int) (previousHp * 2 + previousHp / 2.0);
        }

        return bossHp;
    }

    /**
     * Čuva trenutno stanje bosa
     */
    public void saveBossState(int level, int maxHp, int currentHp, int attacks) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> bossState = new HashMap<>();
        bossState.put("maxHp", maxHp);
        bossState.put("currentHp", currentHp);
        bossState.put("level", level);
        bossState.put("lastUpdate", System.currentTimeMillis());
        bossState.put("attacksRemaining", attacks);

        db.collection("boss_states")
                .document(userId + "_level_" + level)
                .set(bossState)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Boss state saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving boss state", e));
    }

    /**
     * Računa šansu napada na osnovu uspešnosti zadataka
     */
    public void calculateAttackSuccessRate() {
        String userId = getCurrentUserId();
        if (userId == null) {
            attackSuccessRate.setValue(50);
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        attackSuccessRate.setValue(50);
                        return;
                    }

                    User user = document.toObject(User.class);
                    if (user == null) {
                        attackSuccessRate.setValue(50);
                        return;
                    }

                    long levelUpTime = user.getLastLevelUpTime();
                    int currentLevel = user.getLevel();

                    long regTime = user.getRegistrationTime();
                    levelUpTime = (regTime > 0) ? regTime : levelUpTime;
                    Log.d(TAG, "Using registration time as start period: " + levelUpTime);

                    Log.d(TAG, "=== CALCULATING SUCCESS RATE FOR CURRENT LEVEL ===");
                    Log.d(TAG, "Current level: " + currentLevel + ", Level up time: " + levelUpTime);

                    calculateSuccessRateForPeriod(userId, levelUpTime);
                });
    }

    private void calculateSuccessRateForPeriod(String userId, long fromTime) {
        long currentTime = System.currentTimeMillis();

        Log.d(TAG, "Calculating success rate from " + new Date(fromTime) + " to " + new Date(currentTime));

        taskRepository.getTasksForPeriod(userId, fromTime, currentTime, (tasks) -> {
            executor.execute(() -> {
                int totalValidTasks = 0;
                int completedValidTasks = 0;

                if (tasks != null) {
                    for (TaskEntity task : tasks) {
                        if (task.status == TaskEntity.STATUS_PAUSED ||
                                task.status == TaskEntity.STATUS_CANCELED) {
                            Log.d(TAG, "Excluding paused/canceled task: " + task.title);
                            continue;
                        }

                        if (taskRepository.doesTaskExceedQuota(task)) {
                            Log.d(TAG, "Task exceeds quota, excluding: " + task.title +
                                    " (difficulty: " + task.difficulty + ", importance: " + task.importance + ")");
                            continue;
                        }

                        totalValidTasks++;

                        if (task.status == TaskEntity.STATUS_COMPLETED) {
                            completedValidTasks++;
                            Log.d(TAG, "Valid completed task: " + task.title);
                        } else {
                            Log.d(TAG, "Valid non-completed task: " + task.title + " (status: " + task.status + ")");
                        }
                    }
                }

                Log.d(TAG, String.format("FINAL CALCULATION: %d completed out of %d valid tasks",
                        completedValidTasks, totalValidTasks));

                int successRate = totalValidTasks > 0 ?
                        (completedValidTasks * 100) / totalValidTasks : 50;

                successRate = Math.max(10, Math.min(95, successRate));

                Log.d(TAG, "Attack success rate: " + successRate + "%");

                attackSuccessRate.postValue(successRate);
            });
        });
    }

    /**
     * Proverava da li je task mogao da zarade XP kada je završen
     * Računa kvote za datum kada je task završen, ne za danas
     */
    private boolean couldTaskEarnXpWhenCompleted(TaskEntity task, String userId) {
        long completionDate = task.updatedAt; // Datum kada je task završen

        return checkDifficultyQuotaForDate(task.difficulty, userId, completionDate) &&
                checkImportanceQuotaForDate(task.importance, userId, completionDate);
    }

    private boolean checkDifficultyQuotaForDate(int difficulty, String userId, long date) {
        switch (difficulty) {
            case TaskEntity.DIFFICULTY_VERY_EASY:
                return getDailyCompletedCountForDate(userId, difficulty, date) < 5;

            case TaskEntity.DIFFICULTY_EASY:
                return getDailyCompletedCountForDate(userId, difficulty, date) < 5;

            case TaskEntity.DIFFICULTY_HARD:
                return getDailyCompletedCountForDate(userId, difficulty, date) < 2;

            case TaskEntity.DIFFICULTY_EXTREME:
                return getWeeklyCompletedCountForDate(userId, difficulty, date) < 1;

            default:
                return true;
        }
    }

    private boolean checkImportanceQuotaForDate(int importance, String userId, long date) {
        switch (importance) {
            case TaskEntity.IMPORTANCE_NORMAL:
                return getDailyCompletedCountByImportanceForDate(userId, importance, date) < 5;

            case TaskEntity.IMPORTANCE_IMPORTANT:
                return getDailyCompletedCountByImportanceForDate(userId, importance, date) < 5;

            case TaskEntity.IMPORTANCE_VERY_IMPORTANT:
                return getDailyCompletedCountByImportanceForDate(userId, importance, date) < 2;

            case TaskEntity.IMPORTANCE_SPECIAL:
                return getMonthlyCompletedCountByImportanceForDate(userId, importance, date) < 1;

            default:
                return true;
        }
    }

    private int getDailyCompletedCountForDate(String userId, int difficulty, long date) {
        return taskRepository.getCompletedTasksCountByDifficultyForDate(userId, difficulty, date);
    }

    private int getDailyCompletedCountByImportanceForDate(String userId, int importance, long date) {
        return taskRepository.getCompletedTasksCountByImportanceForDate(userId, importance, date);
    }

    private int getWeeklyCompletedCountForDate(String userId, int difficulty, long date) {
        return taskRepository.getWeeklyCompletedTasksCountForDate(userId, difficulty, date);
    }

    private int getMonthlyCompletedCountByImportanceForDate(String userId, int importance, long date) {
        return taskRepository.getMonthlyCompletedTasksCountByImportanceForDate(userId, importance, date);
    }

    public void updateBossHp(int newHp) {
        bossCurrentHp.setValue(newHp);
        Integer level = currentBossLevel.getValue();
        Integer maxHp = bossMaxHp.getValue();
        Integer attacks = attacksRemaining.getValue();
        if (level != null && maxHp != null && attacks != null) {
            saveBossState(level, maxHp, newHp, attacks);
        }
    }

    public void useAttack() {
        Integer current = attacksRemaining.getValue();
        if (current != null && current > 0) {
            int newAttacks = current - 1;
            attacksRemaining.setValue(newAttacks);

            Integer level = currentBossLevel.getValue();
            Integer maxHp = bossMaxHp.getValue();
            Integer currentHp = bossCurrentHp.getValue();
            if (level != null && maxHp != null && currentHp != null) {
                saveBossState(level, maxHp, currentHp, newAttacks);
            }
        }
    }

    public void resetAttacks() {
        attacksRemaining.setValue(5);
    }

    /**
     * Refresh-uje sve PP podatke
     */
    public void refreshAllPpData() {
        Log.d(TAG, "Refreshing all PP data...");
        loadUserPp();
        loadEquipmentPpBonus();
        calculateAttackSuccessRate();
    }

    // Getters
    public LiveData<Integer> getUserBasePp() {
        return userBasePp;
    }

    public LiveData<Integer> getEquipmentPpBonus() {
        return equipmentPpBonus;
    }

    public LiveData<Integer> getTotalPp() {
        return totalPp;
    }

    public LiveData<Integer> getUserLevel() {
        return userLevel;
    }

    public LiveData<Integer> getBossMaxHp() {
        return bossMaxHp;
    }

    public LiveData<Integer> getBossCurrentHp() {
        return bossCurrentHp;
    }

    public LiveData<List<Equipment>> getActiveEquipment() {
        return activeEquipment;
    }

    public LiveData<Integer> getAttacksRemaining() {
        return attacksRemaining;
    }

    // Helper
    private String getCurrentUserId() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }

    public LiveData<Integer> getCurrentBossLevel() { return currentBossLevel; }
    public LiveData<List<Integer>> getUndefeatedBossLevels() { return undefeatedBossLevels; }

}