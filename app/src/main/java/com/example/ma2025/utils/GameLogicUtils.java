package com.example.ma2025.utils;

import com.example.ma2025.data.models.Equipment;

import java.util.Random;

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

    // ========== DODANE METODE ZA TaskRepository ==========

    /**
     * Računa total XP potreban za određeni nivo (compatibility metoda)
     */
    public static int calculateTotalXpForLevel(int level) {
        int totalXp = 0;
        for (int i = 1; i <= level; i++) {
            totalXp += calculateXpForLevel(i);
        }
        return totalXp;
    }

    /**
     * Računa nivo na osnovu ukupnog XP-a
     */
    public static int calculateLevelFromXp(int totalXp) {
        int level = 0;
        int xpUsed = 0;

        while (xpUsed < totalXp) {
            int xpForNextLevel = calculateXpForLevel(level + 1);
            if (xpUsed + xpForNextLevel <= totalXp) {
                xpUsed += xpForNextLevel;
                level++;
            } else {
                break;
            }
        }

        return level;
    }

    /**
     * Računa preostali XP za sledeći nivo
     */
    public static int calculateRemainingXpForNextLevel(int currentXp, int currentLevel) {
        int totalXpForCurrentLevel = calculateTotalXpForLevel(currentLevel);
        int xpForNextLevel = calculateXpForLevel(currentLevel + 1);
        int totalXpForNextLevel = totalXpForCurrentLevel + xpForNextLevel;

        return totalXpForNextLevel - currentXp;
    }

    /**
     * Calculate coins earned from XP
     * 1 coin per 10 XP earned
     */
    public static int calculateCoinsFromXp(int xp) {
        return xp / 10;
    }

    /**
     * Calculate streak bonus multiplier
     * Streak days: 1-6 = 1x, 7-13 = 1.5x, 14-29 = 2x, 30+ = 2.5x
     */
    public static double calculateStreakMultiplier(int streakDays) {
        if (streakDays < 7) return 1.0;
        else if (streakDays < 14) return 1.5;
        else if (streakDays < 30) return 2.0;
        else return 2.5;
    }

    /**
     * Validate difficulty level
     */
    public static boolean isValidDifficulty(int difficulty) {
        return difficulty >= 1 && difficulty <= 4;
    }

    /**
     * Validate importance level
     */
    public static boolean isValidImportance(int importance) {
        return importance >= 1 && importance <= 4;
    }

    /**
     * Get difficulty name
     */
    public static String getDifficultyName(int difficulty) {
        switch (difficulty) {
            case 1: return "Veoma lak";
            case 2: return "Lak";
            case 3: return "Težak";
            case 4: return "Ekstremno težak";
            default: return "Nepoznato";
        }
    }

    /**
     * Get importance name
     */
    public static String getImportanceName(int importance) {
        switch (importance) {
            case 1: return "Normalan";
            case 2: return "Važan";
            case 3: return "Ekstremno važan";
            case 4: return "Specijalan";
            default: return "Nepoznato";
        }
    }

    /**
     * Calculate task priority score for sorting
     * Higher score = higher priority
     */
    public static int calculateTaskPriority(int difficulty, int importance, long dueTime) {
        int baseScore = difficulty * 10 + importance * 15;

        // Add urgency bonus based on due time
        long timeUntilDue = dueTime - System.currentTimeMillis();
        long hoursUntilDue = timeUntilDue / (1000 * 60 * 60);

        int urgencyBonus;
        if (hoursUntilDue < 1) urgencyBonus = 100;      // Due in less than 1 hour
        else if (hoursUntilDue < 6) urgencyBonus = 50;   // Due in less than 6 hours
        else if (hoursUntilDue < 24) urgencyBonus = 25;  // Due today
        else if (hoursUntilDue < 72) urgencyBonus = 10;  // Due in 3 days
        else urgencyBonus = 0;

        return baseScore + urgencyBonus;
    }

    // ========== ORIGINALNE METODE ==========

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

    /**
     * Generiše nasumičnu opremu nakon pobede nad bosom (20% šanse)
     */
    public static Equipment generateRandomEquipmentReward(int playerLevel) {
        Random random = new Random();

        // 20% šanse za opremu
        if (random.nextFloat() > 0.20f) {
            return null; // Nema nagrade
        }

        int bossReward = calculateBossReward(playerLevel);

        // 95% šanse za odeću, 5% šanse za oružje
        boolean isClothing = random.nextFloat() <= 0.95f;

        if (isClothing) {
            return generateRandomClothing(bossReward, random);
        } else {
            return generateRandomWeapon(bossReward, random);
        }
    }

    /**
     * Generiše nasumičnu odeću
     */
    private static Equipment generateRandomClothing(int bossReward, Random random) {
        String[] clothingNames = {
                "Rukavice Snage", "Štit Preciznosti", "Čizme Brzine",
                "Oklop Zaštite", "Kaciga Moći", "Plašt Tame"
        };

        String[] effects = {
                Constants.EFFECT_PP_BOOST,
                Constants.EFFECT_ATTACK_BOOST,
                "extra_attack"
        };

        String name = clothingNames[random.nextInt(clothingNames.length)];
        String effect = effects[random.nextInt(effects.length)];
        double effectValue = 0.05 + (random.nextDouble() * 0.15); // 5-20% efekat
        int rarity = 30 + random.nextInt(71); // Rarity 30-100

        Equipment clothing = Equipment.createClothing(name, effect, effectValue, rarity);
        clothing.setPrice(0); // Besplatno jer je nagrada

        return clothing;
    }

    /**
     * Generiše nasumično oružje
     */
    private static Equipment generateRandomWeapon(int bossReward, Random random) {
        String[] weaponNames = {
                "Mač Svetlosti", "Sekira Groma", "Luk Vetra",
                "Štap Vatre", "Bodež Senki", "Čekić Zemlje"
        };

        String name = weaponNames[random.nextInt(weaponNames.length)];
        double effectValue = 0.10 + (random.nextDouble() * 0.20); // 10-30% efekat
        int rarity = 50 + random.nextInt(51); // Rarity 50-100
        boolean isPermanent = random.nextBoolean(); // 50% šanse da bude permanentno

        Equipment weapon = Equipment.createPotion(name, effectValue, rarity, isPermanent);
        weapon.setPrice(0); // Besplatno jer je nagrada

        return weapon;
    }

}