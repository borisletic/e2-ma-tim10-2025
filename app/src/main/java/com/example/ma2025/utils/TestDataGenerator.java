package com.example.ma2025.utils;

import com.example.ma2025.data.models.Equipment;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * TestDataGenerator - Creates sample equipment for testing
 * Remove this class before production
 */
public class TestDataGenerator {

    /**
     * Generate test equipment for user (for demonstration)
     */
    public static void generateTestEquipment(String userId, FirebaseFirestore db) {
        List<Equipment> testEquipment = new ArrayList<>();

        // Create some test equipment
        Equipment potion = Equipment.createPotion("Test Napitak", 0.20, 50, false);
        potion.setUserId(userId);
        potion.activate(); // Pre-activate for testing

        Equipment gloves = Equipment.createClothing("Test Rukavice", Constants.EFFECT_PP_BOOST, 0.10, 60);
        gloves.setUserId(userId);

        Equipment sword = Equipment.createWeapon("Test Mač", Constants.EFFECT_PP_BOOST, 0.05);
        sword.setUserId(userId);
        sword.activate(); // Pre-activate for testing

        testEquipment.add(potion);
        testEquipment.add(gloves);
        testEquipment.add(sword);

        // Save to Firebase
        for (Equipment equipment : testEquipment) {
            db.collection(Constants.COLLECTION_EQUIPMENT)
                    .add(equipment);
        }
    }

    /**
     * Add test coins to user for testing purchases
     */
    public static void addTestCoins(String userId, FirebaseFirestore db) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("coins", 5000) // Give 5000 coins for testing
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("TestData", "Test coins added");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("TestData", "Failed to add test coins", e);
                });
    }
}