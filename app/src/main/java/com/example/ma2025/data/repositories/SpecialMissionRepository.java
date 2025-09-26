package com.example.ma2025.data.repositories;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ma2025.data.models.SpecialMission;
import com.example.ma2025.data.models.MissionProgress;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.models.Badge;
import com.example.ma2025.utils.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpecialMissionRepository {
    private static final String TAG = "SpecialMissionRepo";

    private FirebaseFirestore db;
    private ExecutorService executor;
    private static volatile SpecialMissionRepository INSTANCE;

    private SpecialMissionRepository() {
        db = FirebaseFirestore.getInstance();
        executor = Executors.newFixedThreadPool(2);
    }

    public static SpecialMissionRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (SpecialMissionRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SpecialMissionRepository();
                }
            }
        }
        return INSTANCE;
    }

    // ========== MISSION CREATION ==========

    public void createSpecialMission(String allianceId, String createdByUserId,
                                     List<String> memberIds, OnMissionCreatedCallback callback) {

        checkForActiveMission(allianceId, new OnMissionCheckCallback() {
            @Override
            public void onNoActiveMission() {
                SpecialMission mission = new SpecialMission();
                mission.setAllianceId(allianceId);
                mission.setMaxBossHp(Constants.MISSION_BASE_HP_PER_MEMBER * memberIds.size());
                mission.setBossHp(mission.getMaxBossHp());
                mission.setStartTime(System.currentTimeMillis());
                mission.setEndTime(mission.getStartTime() +
                        (Constants.MISSION_DURATION_DAYS * 24 * 60 * 60 * 1000L));
                mission.setCompleted(false);

                Map<String, MissionProgress> memberProgress = new HashMap<>();
                for (String memberId : memberIds) {
                    memberProgress.put(memberId, new MissionProgress(memberId));
                }
                mission.setMemberProgress(memberProgress);

                db.collection(Constants.COLLECTION_MISSIONS)
                        .add(mission)
                        .addOnSuccessListener(documentReference -> {
                            mission.setId(documentReference.getId());
                            Log.d(TAG, "Special mission created with ID: " + mission.getId());
                            callback.onSuccess(mission);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error creating special mission", e);
                            callback.onError("Greška pri kreiranju specijalne misije: " + e.getMessage());
                        });
            }

            @Override
            public void onActiveMissionExists() {
                callback.onError("Savez već ima aktivnu specijalnu misiju");
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void checkForActiveMission(String allianceId, OnMissionCheckCallback callback) {
        db.collection(Constants.COLLECTION_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("completed", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onNoActiveMission();
                    } else {
                        boolean hasActiveMission = false;
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            SpecialMission mission = doc.toObject(SpecialMission.class);
                            if (mission != null && !mission.isExpired()) {
                                hasActiveMission = true;
                                break;
                            }
                        }

                        if (hasActiveMission) {
                            callback.onActiveMissionExists();
                        } else {
                            callback.onNoActiveMission();
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========== MISSION PROGRESS TRACKING - NOVA IMPLEMENTACIJA ==========

    /**
     * Registruje završen zadatak sa pravilnim brojanjem prema specifikaciji
     */
    public void recordTaskCompleted(String allianceId, String userId,
                                    int tezina, int bitnost, boolean isSuccess,
                                    OnTaskRecordedCallback callback) {
        db.collection(Constants.COLLECTION_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("completed", false)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError("Nema aktivne misije");
                        return;
                    }

                    DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                    SpecialMission mission = document.toObject(SpecialMission.class);

                    if (mission == null || mission.isExpired()) {
                        callback.onError("Misija je istekla");
                        return;
                    }

                    MissionProgress progress = mission.getMemberProgress().get(userId);
                    if (progress == null) {
                        progress = new MissionProgress(userId);
                    }

                    if (!isSuccess) {
                        progress.taskFailed();
                    } else {
                        // Proveri da li je "easy and normal" (računa se 2 puta)
                        boolean isEasyAndNormal = MissionProgress.isEasyAndNormal(tezina, bitnost);

                        // Proveri da li je "easy" zadatak
                        if (MissionProgress.isEasyTask(tezina, bitnost)) {
                            progress.incrementEasyTasks(isEasyAndNormal);
                        } else {
                            // Težak zadatak
                            progress.incrementHardTasks();
                        }
                    }

                    mission.getMemberProgress().put(userId, progress);
                    mission.updateBossHp();

                    // Ažuriraj u bazi
                    db.collection(Constants.COLLECTION_MISSIONS)
                            .document(document.getId())
                            .set(mission)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Task recorded for user " + userId);
                                callback.onSuccess(mission);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Registruje kupovinu u prodavnici
     */
    public void recordStoreVisit(String allianceId, String userId,
                                 OnTaskRecordedCallback callback) {
        db.collection(Constants.COLLECTION_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("completed", false)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError("Nema aktivne misije");
                        return;
                    }

                    DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                    SpecialMission mission = document.toObject(SpecialMission.class);

                    if (mission == null || mission.isExpired()) {
                        callback.onError("Misija je istekla");
                        return;
                    }

                    MissionProgress progress = mission.getMemberProgress().get(userId);
                    if (progress == null) {
                        progress = new MissionProgress(userId);
                    }

                    progress.incrementStoreVisits();
                    mission.getMemberProgress().put(userId, progress);
                    mission.updateBossHp();

                    db.collection(Constants.COLLECTION_MISSIONS)
                            .document(document.getId())
                            .set(mission)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Store visit recorded for user " + userId);
                                callback.onSuccess(mission);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Registruje uspešan udarac u regularnoj borbi
     */
    public void recordSuccessfulAttack(String allianceId, String userId,
                                       OnTaskRecordedCallback callback) {
        db.collection(Constants.COLLECTION_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("completed", false)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError("Nema aktivne misije");
                        return;
                    }

                    DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                    SpecialMission mission = document.toObject(SpecialMission.class);

                    if (mission == null || mission.isExpired()) {
                        callback.onError("Misija je istekla");
                        return;
                    }

                    MissionProgress progress = mission.getMemberProgress().get(userId);
                    if (progress == null) {
                        progress = new MissionProgress(userId);
                    }

                    progress.incrementSuccessfulAttacks();
                    mission.getMemberProgress().put(userId, progress);
                    mission.updateBossHp();

                    db.collection(Constants.COLLECTION_MISSIONS)
                            .document(document.getId())
                            .set(mission)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successful attack recorded for user " + userId);
                                callback.onSuccess(mission);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Registruje poruku poslatu u savezu
     */
    public void recordMessageSent(String allianceId, String userId, String date,
                                  OnTaskRecordedCallback callback) {
        db.collection(Constants.COLLECTION_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("completed", false)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError("Nema aktivne misije");
                        return;
                    }

                    DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                    SpecialMission mission = document.toObject(SpecialMission.class);

                    if (mission == null || mission.isExpired()) {
                        callback.onError("Misija je istekla");
                        return;
                    }

                    MissionProgress progress = mission.getMemberProgress().get(userId);
                    if (progress == null) {
                        progress = new MissionProgress(userId);
                    }

                    progress.addMessageDay(date != null ? date : getCurrentDate());
                    mission.getMemberProgress().put(userId, progress);
                    mission.updateBossHp();

                    db.collection(Constants.COLLECTION_MISSIONS)
                            .document(document.getId())
                            .set(mission)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Message day recorded for user " + userId);
                                callback.onSuccess(mission);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateMissionProgress(String missionId, String userId,
                                      String actionType, OnProgressUpdatedCallback callback) {
        updateMissionProgressWithDate(missionId, userId, actionType, null, callback);
    }

    public void updateMissionProgressWithDate(String missionId, String userId,
                                              String actionType, String date,
                                              OnProgressUpdatedCallback callback) {

        db.collection(Constants.COLLECTION_MISSIONS)
                .document(missionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError("Misija ne postoji");
                        return;
                    }

                    SpecialMission mission = document.toObject(SpecialMission.class);
                    if (mission == null || mission.isCompleted() || mission.isExpired()) {
                        callback.onError("Misija je završena ili je istekla");
                        return;
                    }

                    MissionProgress userProgress = mission.getMemberProgress().get(userId);
                    if (userProgress == null) {
                        userProgress = new MissionProgress(userId);
                        mission.getMemberProgress().put(userId, userProgress);
                    }

                    int damageDealt = updateUserProgress(userProgress, actionType, date);

                    if (damageDealt > 0) {
                        mission.dealDamage(damageDealt);

                        db.collection(Constants.COLLECTION_MISSIONS)
                                .document(missionId)
                                .set(mission)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Mission progress updated: " + damageDealt + " damage dealt");

                                    if (mission.isCompleted()) {
                                        handleMissionCompletion(mission, callback);
                                    } else {
                                        callback.onSuccess(damageDealt, mission.getBossHp());
                                    }
                                })
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    } else {
                        callback.onSuccess(0, mission.getBossHp());
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private int updateUserProgress(MissionProgress progress, String actionType, String date) {
        int initialDamage = progress.getTotalDamageDealt();
        boolean actionPerformed = false;

        switch (actionType) {
            case "store_visit":
                actionPerformed = progress.incrementStoreVisits();
                break;
            case "successful_attack":
                actionPerformed = progress.incrementSuccessfulAttacks();
                break;
            case "easy_task":
                actionPerformed = progress.incrementEasyTasks(false);
                break;
            case "hard_task":
                actionPerformed = progress.incrementHardTasks();
                break;
            case "message_day":
                if (date != null) {
                    actionPerformed = progress.addMessageDay(date);
                } else {
                    String currentDate = getCurrentDate();
                    actionPerformed = progress.addMessageDay(currentDate);
                }
                break;
            case "task_failed":
                progress.taskFailed();
                return 0;
            default:
                Log.w(TAG, "Unknown action type: " + actionType);
                return 0;
        }

        if (actionPerformed) {
            int damageDealt = progress.getTotalDamageDealt() - initialDamage;
            Log.d(TAG, "Action " + actionType + " performed. Damage dealt: " + damageDealt);
            return damageDealt;
        } else {
            Log.d(TAG, "Action " + actionType + " not performed - quota reached or duplicate day");
            return 0;
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void getActiveMissionOnce(String allianceId, OnMissionLoadedCallback callback) {
        db.collection("special_missions")
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        SpecialMission mission = querySnapshot.getDocuments()
                                .get(0)
                                .toObject(SpecialMission.class);
                        callback.onSuccess(mission);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading mission", e);
                    callback.onError(e.getMessage());
                });
    }

    private void handleMissionCompletion(SpecialMission mission, OnProgressUpdatedCallback callback) {
        Log.d(TAG, "Mission completed! Distributing rewards...");

        Map<String, MissionProgress> memberProgress = mission.getMemberProgress();
        for (Map.Entry<String, MissionProgress> entry : memberProgress.entrySet()) {
            String userId = entry.getKey();
            MissionProgress progress = entry.getValue();

            int finalBonus = progress.calculateFinalBonus();
            if (finalBonus > 0) {
                mission.dealDamage(finalBonus);
                Log.d(TAG, "User " + userId + " gets " + finalBonus + " bonus for no failed tasks");
            }

            distributeRewardsToUser(userId, mission.getAllianceId(), progress);
            createMissionBadge(userId, progress);
        }

        callback.onSuccess(0, 0);
    }

    private void distributeRewardsToUser(String userId, String allianceId, MissionProgress progress) {
        executor.execute(() -> {
            try {
                Equipment clothing = generateRandomClothing();
                Equipment potion = generateRandomPotion();

                saveEquipmentReward(userId, clothing);
                saveEquipmentReward(userId, potion);
                updateUserCoinsForMission(userId);

                Log.d(TAG, "Rewards distributed to user: " + userId);
            } catch (Exception e) {
                Log.e(TAG, "Error distributing rewards to user " + userId, e);
            }
        });
    }

    private Equipment generateRandomClothing() {
        String[] clothingNames = {"Misijska Rukavica", "Savezni Štit", "Čizme Jedinstva"};
        String[] clothingTypes = {"gloves", "shield", "boots"};
        int index = (int)(Math.random() * clothingNames.length);

        String name = clothingNames[index];
        String type = clothingTypes[index];

        switch (type) {
            case "gloves":
                return Equipment.createClothing(name, Constants.EFFECT_PP_BOOST, 0.10, 50);
            case "shield":
                return Equipment.createClothing(name, Constants.EFFECT_ATTACK_SUCCESS, 0.10, 50);
            case "boots":
                return Equipment.createClothing(name, Constants.EFFECT_EXTRA_ATTACK, 0.40, 50);
            default:
                return Equipment.createClothing(name, Constants.EFFECT_PP_BOOST, 0.10, 50);
        }
    }

    private Equipment generateRandomPotion() {
        String[] potionNames = {"Napoj Saveza", "Eliksir Misije", "Tonik Tima"};
        double[] potionEffects = {0.20, 0.40, 0.05};
        boolean[] isPermanent = {false, false, true};

        int index = (int)(Math.random() * potionNames.length);
        String name = potionNames[index];
        double effect = potionEffects[index];
        boolean permanent = isPermanent[index];

        return Equipment.createPotion(name, effect, 60, permanent);
    }

    private void createMissionBadge(String userId, MissionProgress progress) {
        int totalSpecialTasks = progress.getStoreVisits() +
                progress.getSuccessfulAttacks() +
                progress.getEasyTasksCompleted() +
                progress.getHardTasksCompleted() +
                progress.getMessageDaysCount();

        Badge missionBadge = new Badge();
        missionBadge.setUserId(userId);
        missionBadge.setType("special_mission");
        missionBadge.setTaskCount(totalSpecialTasks);
        missionBadge.setEarnedDate(System.currentTimeMillis());
        missionBadge.setDescription("Uspešno završena specijalna misija sa " + totalSpecialTasks + " zadataka");

        db.collection(Constants.COLLECTION_BADGES)
                .add(missionBadge)
                .addOnSuccessListener(docRef -> {
                    missionBadge.setId(docRef.getId());
                    Log.d(TAG, "Mission badge created for user: " + userId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error creating mission badge", e));
    }

    private void saveEquipmentReward(String userId, Equipment equipment) {
        equipment.setUserId(userId);
        equipment.setId(null);
        equipment.setPrice(0);

        db.collection(Constants.COLLECTION_EQUIPMENT)
                .add(equipment)
                .addOnSuccessListener(docRef -> {
                    equipment.setId(docRef.getId());
                    Log.d(TAG, "Mission reward equipment saved: " + equipment.getName());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving mission equipment", e));
    }

    private void updateUserCoinsForMission(String userId) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long currentCoins = document.getLong("coins");
                        Long userLevel = document.getLong("level");

                        int coinsToAdd = calculateMissionCoinReward(userLevel != null ? userLevel.intValue() : 1);

                        db.collection(Constants.COLLECTION_USERS)
                                .document(userId)
                                .update("coins", (currentCoins != null ? currentCoins : 0) + coinsToAdd)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "Mission coins awarded: " + coinsToAdd + " to user " + userId))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Error updating mission coins", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting user data", e));
    }

    private int calculateMissionCoinReward(int userLevel) {
        int nextLevelReward = (int)(200 * Math.pow(1.2, userLevel));
        return nextLevelReward / 2;
    }

    public LiveData<SpecialMission> getActiveMission(String allianceId) {
        MutableLiveData<SpecialMission> missionLiveData = new MutableLiveData<>();

        db.collection(Constants.COLLECTION_MISSIONS)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("completed", false)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to mission changes", e);
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            SpecialMission mission = doc.toObject(SpecialMission.class);
                            if (mission != null && !mission.isExpired()) {
                                mission.setId(doc.getId());
                                missionLiveData.setValue(mission);
                                return;
                            }
                        }
                    }

                    missionLiveData.setValue(null);
                });

        return missionLiveData;
    }

    // ========== CALLBACK INTERFACES ==========

    public interface OnMissionCreatedCallback {
        void onSuccess(SpecialMission mission);
        void onError(String error);
    }

    public interface OnMissionCheckCallback {
        void onNoActiveMission();
        void onActiveMissionExists();
        void onError(String error);
    }

    public interface OnProgressUpdatedCallback {
        void onSuccess(int damageDealt, int remainingBossHp);
        void onError(String error);
    }

    public interface OnProgressRetrievedCallback {
        void onSuccess(MissionProgress progress);
        void onError(String error);
    }

    public interface OnMissionStatisticsCallback {
        void onSuccess(Map<String, Object> statistics);
        void onError(String error);
    }

    public interface OnTaskRecordedCallback {
        void onSuccess(SpecialMission mission);
        void onError(String error);
    }

    public interface OnMissionLoadedCallback {
        void onSuccess(SpecialMission mission);
        void onError(String error);
    }
}