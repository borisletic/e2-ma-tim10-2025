package com.example.ma2025.viewmodels;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.models.User;
import com.example.ma2025.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class BossViewModel extends AndroidViewModel {

    private static final String TAG = "BossViewModel";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

    public BossViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize values
        userBasePp.setValue(0);
        equipmentPpBonus.setValue(0);
        totalPp.setValue(0);
        userLevel.setValue(0);
        bossMaxHp.setValue(200); // Prvi boss ima 200 HP
        bossCurrentHp.setValue(200);
        activeEquipment.setValue(new ArrayList<>());
    }

    /**
     * Učitava korisničke podatke i base PP
     */
    public void loadUserPp() {
        String userId = getCurrentUserId();
        if (userId == null) {
            statusMessage.setValue("Korisnik nije ulogovan");
            return;
        }

        statusMessage.setValue("Učitavaju se korisnički podaci...");

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            int basePp = user.getPp();
                            int level = user.getLevel();

                            userBasePp.setValue(basePp);
                            userLevel.setValue(level);

                            calculateBossHp(level);

                            calculateTotalPp();
                            statusMessage.setValue("Korisnik učitan: Nivo " + level + ", Osnovna PP = " + basePp);
                            Log.d(TAG, "User: Level " + level + ", PP " + basePp);
                        }
                    } else {
                        statusMessage.setValue("Korisnik nije pronađen");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user", e);
                    statusMessage.setValue("Greška pri učitavanju korisničkih podataka");
                });
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
     * Kalkuliše Boss HP na osnovu user nivoa
     * Prvi boss (nivo 0) = 200 HP
     * Formula: HP prethodnog bosa * 2 + HP prethodnog bosa / 2
     */
    private void calculateBossHp(int userLevel) {
        int bossHp = 200; // Prvi boss uvek ima 200 HP

        // Kalkuliši HP za boss-ove na višim nivoima
        for (int i = 1; i <= userLevel; i++) {
            int previousHp = bossHp;
            bossHp = (int) (previousHp * 2 + previousHp / 2.0);
            Log.d(TAG, "Boss level " + i + ": " + bossHp + " HP");
        }

        bossMaxHp.setValue(bossHp);
        bossCurrentHp.setValue(bossHp); // Boss počinje sa punim HP

        Log.d(TAG, "Boss HP calculated for user level " + userLevel + ": " + bossHp + " HP");
    }

    /**
     * Refresh-uje sve PP podatke
     */
    public void refreshAllPpData() {
        Log.d(TAG, "Refreshing all PP data...");
        loadUserPp();
        loadEquipmentPpBonus();
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

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<List<Equipment>> getActiveEquipment() {
        return activeEquipment;
    }

    // Helper
    private String getCurrentUserId() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }

    public void setBossCurrentHp(int hp) {
        bossCurrentHp.setValue(hp);
    }
}