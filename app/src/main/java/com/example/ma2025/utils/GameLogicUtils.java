package com.example.ma2025.utils;

public class GameLogicUtils {

    /**
     * Računa XP potreban za sledeći nivo
     * Formula: XP prethodnog nivoa * 2 + XP prethodnog nivoa / 2
     */
    public static int calculateXpForLevel(int level) {
        if (level <= 0) return Constants.BASE_XP_FOR_LEVEL_1;

        int xp = Constants.BASE_XP_FOR_LEVEL_1;
        for (int i = 1; i < level; i++) {
            xp = (int) Math.ceil((xp * 2 + xp / 2.0) / 100.0) * 100;
        }
        return xp;
    }

    /**
     * Računa PP dobijen za nivo
     * Formula: PP za prethodni nivo + 3/4 * PP za prethodni nivo
     */
    public static int calculatePpForLevel(int level) {
        if (level <= 0) return 0;
        if (level == 1) return Constants.BASE_PP_FOR_LEVEL_1;

        int pp = Constants.BASE_PP_FOR_LEVEL_1;
        for (int i = 2; i <= level; i++) {
            pp = (int) (pp + 0.75 * pp);
        }
        return pp;
    }

    /**
     * Računa HP bosa za nivo
     * Formula: HP prethodnog bosa * 2 + HP prethodnog bosa / 2
     */
    public static int calculateBossHp(int level) {
        if (level <= 0) return Constants.BASE_BOSS_HP;

        int hp = Constants.BASE_BOSS_HP;
        for (int i = 1; i < level; i++) {
            hp = (int) (hp * 2 + hp / 2.0);
        }
        return hp;
    }

    /**
     * Računa nagradu u novčićima za pobedu nad bosom
     */
    public static int calculateBossReward(int level) {
        if (level <= 0) return Constants.BASE_BOSS_REWARD;

        double reward = Constants.BASE_BOSS_REWARD;
        for (int i = 1; i < level; i++) {
            reward *= Constants.BOSS_REWARD_INCREASE;
        }
        return (int) reward;
    }

    /**
     * Računa XP vrednost za težinu zadatka na određenom nivou
     */
    public static int calculateDifficultyXp(int baseXp, int level) {
        if (level <= 0) return baseXp;

        int xp = baseXp;
        for (int i = 1; i <= level; i++) {
            xp = (int) (xp + xp / 2.0);
        }
        return xp;
    }

    /**
     * Računa XP vrednost za bitnost zadatka na određenom nivou
     */
    public static int calculateImportanceXp(int baseXp, int level) {
        return calculateDifficultyXp(baseXp, level); // Ista formula
    }

    /**
     * Računa ukupan XP za zadatak
     */
    public static int calculateTaskXp(int difficultyXp, int importanceXp, int level) {
        int totalDifficultyXp = calculateDifficultyXp(difficultyXp, level);
        int totalImportanceXp = calculateImportanceXp(importanceXp, level);
        return totalDifficultyXp + totalImportanceXp;
    }

    /**
     * Računa cenu opreme na osnovu nagrade bosa
     */
    public static int calculateEquipmentPrice(double pricePercentage, int bossReward) {
        return (int) (bossReward * pricePercentage);
    }

    /**
     * Računa šansu uspešnog napada na osnovu uspešnosti rešavanja zadataka
     */
    public static int calculateAttackSuccessRate(int completedTasks, int totalTasks) {
        if (totalTasks == 0) return 50; // Default 50% ako nema zadataka
        return Math.min(100, Math.max(10, (completedTasks * 100) / totalTasks));
    }

    /**
     * Generiše random broj između min i max (uključujući krajnje vrednosti)
     */
    public static int randomBetween(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    /**
     * Proverava da li je napad uspešan na osnovu šanse
     */
    public static boolean isAttackSuccessful(int successRate) {
        return randomBetween(1, 100) <= successRate;
    }

    /**
     * Proverava da li treba da se dropuje oprema
     */
    public static boolean shouldDropEquipment() {
        return randomBetween(1, 100) <= Constants.EQUIPMENT_DROP_CHANCE;
    }

    /**
     * Određuje tip opreme koji treba da se dropuje
     */
    public static boolean shouldDropWeapon() {
        return randomBetween(1, 100) > Constants.CLOTHING_DROP_CHANCE;
    }

    /**
     * Računa koliko PP korisnik trenutno ima (bazni + oprema)
     */
    public static int calculateTotalPp(int basePp, int equipmentBonus) {
        return basePp + equipmentBonus;
    }

    /**
     * Računa procenat HP-a koji je ostao bosu
     */
    public static double calculateBossHpPercentage(int currentHp, int maxHp) {
        if (maxHp == 0) return 0;
        return ((double) currentHp / maxHp) * 100;
    }

    /**
     * Proverava da li korisnik može da kupi određenu opremu
     */
    public static boolean canAfford(int userCoins, int itemPrice) {
        return userCoins >= itemPrice;
    }

    /**
     * Računa koliko dana je prošlo od registracije
     */
    public static int calculateActiveDays(long registrationTime) {
        long daysDiff = DateUtils.getDaysDifference(registrationTime, System.currentTimeMillis());
        return (int) Math.max(1, daysDiff); // Minimum 1 dan
    }
}