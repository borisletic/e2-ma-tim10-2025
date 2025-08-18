package com.example.ma2025.utils;

import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * EquipmentManager - handles equipment effects and calculations
 */
public class EquipmentManager {

    /**
     * Calculate total PP bonus from active equipment
     */
    public static double calculatePpBonus(List<Equipment> equipment) {
        double totalBonus = 0.0;

        for (Equipment item : equipment) {
            if (item.isActive() && item.getEffectType().equals(Constants.EFFECT_PP_BOOST)) {
                totalBonus += item.getEffectValue();
            }
        }

        return totalBonus;
    }

    /**
     * Calculate total attack success bonus from active equipment
     */
    public static double calculateAttackBonus(List<Equipment> equipment) {
        double totalBonus = 0.0;

        for (Equipment item : equipment) {
            if (item.isActive() && item.getEffectType().equals(Constants.EFFECT_ATTACK_BOOST)) {
                totalBonus += item.getEffectValue();
            }
        }

        return totalBonus;
    }

    /**
     * Calculate coin bonus from active equipment
     */
    public static double calculateCoinBonus(List<Equipment> equipment) {
        double totalBonus = 0.0;

        for (Equipment item : equipment) {
            if (item.isActive() && item.getEffectType().equals(Constants.EFFECT_COIN_BOOST)) {
                totalBonus += item.getEffectValue();
            }
        }

        return totalBonus;
    }

    /**
     * Calculate chance for extra attack from boots
     */
    public static double calculateExtraAttackChance(List<Equipment> equipment) {
        double totalChance = 0.0;

        for (Equipment item : equipment) {
            if (item.isActive() && item.getEffectType().equals("extra_attack")) {
                totalChance += item.getEffectValue();
            }
        }

        return Math.min(totalChance, 1.0); // Max 100%
    }

    /**
     * Calculate user's effective PP with equipment bonuses
     */
    public static int calculateEffectivePp(User user, List<Equipment> equipment) {
        double ppBonus = calculatePpBonus(equipment);
        double effectivePp = user.getPp() * (1.0 + ppBonus);
        return (int) effectivePp;
    }

    /**
     * Calculate user's attack success rate with equipment bonuses
     */
    public static int calculateAttackSuccessRate(int baseSuccessRate, List<Equipment> equipment) {
        double attackBonus = calculateAttackBonus(equipment);
        double effectiveRate = baseSuccessRate + (attackBonus * 100);
        return Math.min(100, Math.max(0, (int) effectiveRate));
    }

    /**
     * Use equipment once (for temporary items)
     */
    public static void useEquipmentOnce(List<Equipment> equipment) {
        for (Equipment item : equipment) {
            if (item.isActive() && !item.isPermanent()) {
                item.useOnce();
            }
        }
    }



    /**
     * Check if user has any active equipment
     */
    public static boolean hasActiveEquipment(List<Equipment> equipment) {
        for (Equipment item : equipment) {
            if (item.isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get equipment summary text for UI
     */
    public static String getEquipmentSummary(List<Equipment> equipment) {
        List<Equipment> active = getActiveEquipment(equipment);

        if (active.isEmpty()) {
            return "Nema aktivne opreme";
        }

        StringBuilder summary = new StringBuilder();
        double ppBonus = calculatePpBonus(equipment);
        double attackBonus = calculateAttackBonus(equipment);
        double coinBonus = calculateCoinBonus(equipment);
        double extraAttackChance = calculateExtraAttackChance(equipment);

        if (ppBonus > 0) {
            summary.append("PP +").append((int)(ppBonus * 100)).append("% ");
        }

        if (attackBonus > 0) {
            summary.append("Napad +").append((int)(attackBonus * 100)).append("% ");
        }

        if (coinBonus > 0) {
            summary.append("Novčići +").append((int)(coinBonus * 100)).append("% ");
        }

        if (extraAttackChance > 0) {
            summary.append("Bonus napad ").append((int)(extraAttackChance * 100)).append("% ");
        }

        return summary.toString().trim();
    }

    public static List<Equipment> getActiveEquipment(List<Equipment> equipmentList) {
        List<Equipment> activeEquipment = new ArrayList<>();

        if (equipmentList == null) {
            return activeEquipment;
        }

        for (Equipment equipment : equipmentList) {
            if (equipment != null && equipment.isActive() && !equipment.isExpired()) {
                activeEquipment.add(equipment);
            }
        }

        return activeEquipment;
    }
}