package com.example.ma2025.data.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.ma2025.data.database.AppDatabase;
import com.example.ma2025.data.database.dao.BossDao;
import com.example.ma2025.data.database.dao.UserProgressDao;
import com.example.ma2025.data.database.dao.TaskDao;
import com.example.ma2025.data.database.entities.BossEntity;
import com.example.ma2025.data.database.entities.UserProgressEntity;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.models.Alliance;
import com.example.ma2025.data.models.SpecialMission;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BossRepository {
    private static final String TAG = "BossRepository";

    private BossDao bossDao;
    private UserProgressDao userProgressDao;
    private TaskDao taskDao;
    private ExecutorService executor;
    private static volatile BossRepository INSTANCE;

    private BossRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        bossDao = database.bossDao();
        userProgressDao = database.userProgressDao();
        taskDao = database.taskDao();
        executor = Executors.newFixedThreadPool(2);
    }

    public static BossRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BossRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BossRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // ========== BOSS OPERATIONS ==========

    /**
     * Kreira bosa za određeni nivo
     */
    public void createBossForLevel(String userId, int level, OnBossOperationCallback callback) {
        executor.execute(() -> {
            try {
                BossEntity boss = new BossEntity(userId, level);
                long bossId = bossDao.insertBoss(boss);
                boss.id = bossId;

                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onSuccess("Bos kreiran za nivo " + level));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating boss", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Greška pri kreiranju bosa: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Dohvata bosa za trenutni nivo korisnika
     */
    public LiveData<BossEntity> getCurrentBoss(String userId) {
        MutableLiveData<BossEntity> result = new MutableLiveData<>();

        executor.execute(() -> {
            try {
                // Prvo dohvati trenutni nivo korisnika
                UserProgressEntity userProgress = getUserProgressSync(userId);
                if (userProgress != null) {
                    int currentLevel = userProgress.currentLevel;

                    // Provjeri da li bos za trenutni nivo postoji
                    LiveData<BossEntity> bossLiveData = bossDao.getBossForLevel(userId, currentLevel);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        bossLiveData.observeForever(boss -> {
                            if (boss == null) {
                                // Ako bos ne postoji, kreiraj ga
                                createBossForLevel(userId, currentLevel, null);
                            } else {
                                result.postValue(boss);
                            }
                        });
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting current boss", e);
            }
        });

        return result;
    }

    /**
     * Dohvata bosa za određeni nivo
     */
    public LiveData<BossEntity> getBossForLevel(String userId, int level) {
        return bossDao.getBossForLevel(userId, level);
    }

    // ========== BATTLE LOGIC ==========

    /**
     * Napadni bosa - glavna logika borbe
     */
    public void attackBoss(String userId, int level, OnAttackResult callback) {
        executor.execute(() -> {
            try {
                // Dohvati bosa
                BossEntity boss = getBossForLevelSync(userId, level);
                UserProgressEntity userProgress = getUserProgressSync(userId);

                if (boss == null || userProgress == null) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onAttackFailed("Bos ili korisnik nisu pronađeni"));
                    }
                    return;
                }

                if (!boss.isAlive()) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onAttackFailed("Bos je već poražen"));
                    }
                    return;
                }

                // Izračunaj šansu uspešnosti napada na osnovu rešenih zadataka
                float successRate = calculateAttackSuccessRate(userId);

                // Generiši random broj za određivanje da li je napad uspešan
                boolean attackHits = Math.random() < successRate;

                if (attackHits) {
                    // Napad pogađa - nanesi štetu jednaku PP korisnika
                    int damage = userProgress.totalPp;
                    boolean bossDefeated = boss.takeDamage(damage);

                    // Ažuriraj bosa u bazi
                    bossDao.updateBoss(boss);

                    recordSuccessfulAttackInMission(userId);

                    if (bossDefeated) {
                        // Bos je poražen - dodaj nagrade
                        rewardPlayer(userId, boss, callback);
                    } else {
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onAttackSuccess(damage, boss.currentHp, false));
                        }
                    }
                } else {
                    // Napad promašuje
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onAttackMissed());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error attacking boss", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onAttackFailed("Greška pri napadu: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Registruje uspešan napad u specijalnoj misiji
     */
    private void recordSuccessfulAttackInMission(String userId) {
        Log.d(TAG, "=== RECORDING ATTACK FOR USER: " + userId + " ===");

        AllianceRepository allianceRepository = new AllianceRepository();
        allianceRepository.getUserAlliance(userId,
                new AllianceRepository.OnAllianceLoadedListener() {
                    @Override
                    public void onSuccess(Alliance alliance) {
                        Log.d(TAG, "✅ Alliance found: " + alliance.getId() + ", name: " + alliance.getName());

                        SpecialMissionRepository.getInstance().recordSuccessfulAttack(
                                alliance.getId(),
                                userId,
                                new SpecialMissionRepository.OnTaskRecordedCallback() {
                                    @Override
                                    public void onSuccess(SpecialMission mission) {
                                        Log.d(TAG, "✅✅ ATTACK RECORDED! Attacks: " +
                                                mission.getMemberProgress().get(userId).getSuccessfulAttacks() +
                                                ", Boss HP: " + mission.getBossHp());
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "❌ Failed to record attack: " + error);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Alliance error: " + error);
                    }

                    @Override
                    public void onNotInAlliance() {
                        Log.e(TAG, "❌ User NOT in alliance");
                    }
                });
    }

    /**
     * Računa šansu uspešnosti napada na osnovu procenta rešenih zadataka
     */
    private float calculateAttackSuccessRate(String userId) {
        try {
            // Dohvati sve zadatke korisnika
            List<TaskEntity> allTasks = getAllTasksSync(userId);
            if (allTasks.isEmpty()) {
                return 0.5f; // 50% ako nema zadataka
            }

            int completedTasks = 0;
            int totalValidTasks = 0;

            for (TaskEntity task : allTasks) {
                // Ne računaj pauzirane i otkazane zadatke
                if (task.status != TaskEntity.STATUS_PAUSED && task.status != TaskEntity.STATUS_CANCELED) {
                    totalValidTasks++;
                    if (task.status == TaskEntity.STATUS_COMPLETED) {
                        completedTasks++;
                    }
                }
            }

            if (totalValidTasks == 0) {
                return 0.5f; // 50% ako nema validnih zadataka
            }

            return (float) completedTasks / totalValidTasks;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating success rate", e);
            return 0.5f; // Default 50%
        }
    }

    /**
     * Dodeljuje nagrade igraču nakon završetka borbe
     */
    public void finalizeBattle(String userId, int level, int attacksUsed, OnAttackResult callback) {
        executor.execute(() -> {
            try {
                BossEntity boss = getBossForLevelSync(userId, level);
                if (boss == null) return;

                boolean bossDefeated = !boss.isAlive();
                int coinsReward = 0;
                String equipmentType = null;
                double equipmentChance = 0.2; // 20% base chance

                if (bossDefeated) {
                    // Punu nagradu za poraženog bosa
                    coinsReward = boss.coinsReward;
                } else if (boss.getHpPercentage() <= 0.5f) {
                    // Pola nagrade ako je umanjeno 50% HP-a
                    coinsReward = boss.coinsReward / 2;
                    equipmentChance = 0.1; // 10% šanse za opremu
                }

                // Šansa za opremu
                if (coinsReward > 0 && Math.random() < equipmentChance) {
                    equipmentType = Math.random() < 0.95 ? "clothing" : "weapon";
                }

                // Dodaj novčiće korisniku
                if (coinsReward > 0) {
                    UserProgressEntity userProgress = getUserProgressSync(userId);
                    if (userProgress != null) {
                        userProgress.addCoins(coinsReward);
                        userProgressDao.updateUserProgress(userProgress);
                    }
                }

                final int finalCoinsReward = coinsReward;
                final String finalEquipmentType = equipmentType;
                final boolean finalBossDefeated = bossDefeated;

                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (finalBossDefeated) {
                            callback.onBossDefeated(finalCoinsReward, finalEquipmentType);
                        } else {
                            callback.onBattleEnded(finalCoinsReward, finalEquipmentType);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error finalizing battle", e);
            }
        });
    }

    /**
     * Kreira sve neporažene bosove za korisnika
     */
    public void createUndefeatedBosses(String userId, int currentLevel, OnBossOperationCallback callback) {
        executor.execute(() -> {
            try {
                // Kreiraj bosove za sve nivoe do trenutnog
                for (int level = 1; level <= currentLevel; level++) {
                    BossEntity existingBoss = getBossForLevelSync(userId, level);
                    if (existingBoss == null || !existingBoss.isDefeated) {
                        // Kreiraj ili resetuj bosa
                        if (existingBoss == null) {
                            BossEntity newBoss = new BossEntity(userId, level);
                            bossDao.insertBoss(newBoss);
                        } else {
                            existingBoss.reset();
                            bossDao.updateBoss(existingBoss);
                        }
                    }
                }

                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onSuccess("Neporaženi bosovi kreirani"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating undefeated bosses", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Greška: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Dodeljuje nagrade igraču nakon poražavanja bosa
     */
    private void rewardPlayer(String userId, BossEntity defeatedBoss, OnAttackResult callback) {
        try {
            UserProgressEntity userProgress = getUserProgressSync(userId);
            if (userProgress == null) return;

            // Dodaj novčiće
            userProgress.addCoins(defeatedBoss.coinsReward);

            // Postoji 20% šanse za dobijanje opreme
            boolean getEquipment = Math.random() < 0.2;
            final String equipmentType;

            if (getEquipment) {
                // 95% odeća, 5% oružje
                equipmentType = Math.random() < 0.95 ? "clothing" : "weapon";
            } else {
                equipmentType = null;
            }

            // Ažuriraj korisnikov progres
            userProgressDao.updateUserProgress(userProgress);

            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onBossDefeated(defeatedBoss.coinsReward, equipmentType));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error rewarding player", e);
        }
    }

    // ========== HELPER METHODS ==========

    private UserProgressEntity getUserProgressSync(String userId) {
        // Sinhronni pristup - trebalo bi implementirati
        // Možeš koristiti existujuće metode iz UserProgressDao ili TaskRepository
        return null; // Placeholder - treba implementirati
    }

    private BossEntity getBossForLevelSync(String userId, int level) {
        // Sinhronni pristup - trebalo bi implementirati
        return null; // Placeholder - treba implementirati
    }

    private List<TaskEntity> getAllTasksSync(String userId) {
        // Sinhronni pristup - trebalo bi implementirati
        return null; // Placeholder - treba implementirati
    }

    // ========== CALLBACKS ==========

    public interface OnBossOperationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface OnAttackResult {
        void onAttackSuccess(int damage, int remainingHp, boolean bossDefeated);
        void onAttackMissed();
        void onBossDefeated(int coinsReward, String equipmentType); // equipmentType može biti null
        void onBattleEnded(int coinsReward, String equipmentType); // Za 50% HP slučaj
        void onAttackFailed(String error);
    }
}