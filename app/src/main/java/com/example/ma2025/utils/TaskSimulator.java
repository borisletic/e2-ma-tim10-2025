package com.example.ma2025.utils;

import com.example.ma2025.data.models.User;
import java.util.Random;

/**
 * TaskSimulator - Simulira funkcionalnot zadataka za demonstraciju napredovanja kroz nivoe
 * Ovo će biti zamenjeno pravom implementacijom kada Student 2 završi zadatke
 */
public class TaskSimulator {

    private static final Random random = new Random();

    /**
     * Simulira završavanje random zadatka i vraća XP
     */
    public static int simulateTaskCompletion(User user) {
        // Random difficulty (1-4)
        int difficulty = random.nextInt(4) + 1;

        // Random importance (1-4)
        int importance = random.nextInt(4) + 1;

        int xpGained = calculateTaskXp(difficulty, importance, user.getLevel());

        return xpGained;
    }

    /**
     * Simulira završavanje specifičnog zadatka
     */
    public static int simulateSpecificTask(User user, int difficulty, int importance) {
        return calculateTaskXp(difficulty, importance, user.getLevel());
    }

    /**
     * Simulira lak zadatak (quick XP gain)
     */
    public static int simulateEasyTask(User user) {
        return simulateSpecificTask(user, 1, 1); // Very easy + Normal = 2 base XP
    }

    /**
     * Simulira težak zadatak
     */
    public static int simulateHardTask(User user) {
        return simulateSpecificTask(user, 3, 2); // Hard + Important = 10 base XP
    }

    /**
     * Simulira ekstremni zadatak
     */
    public static int simulateExtremeTask(User user) {
        return simulateSpecificTask(user, 4, 3); // Extreme + Very Important = 30 base XP
    }

    /**
     * Izračunava XP za zadatak na osnovu težine, bitnosti i trenutnog nivoa
     */
    private static int calculateTaskXp(int difficulty, int importance, int userLevel) {
        // Base XP for difficulty
        int difficultyXp = getDifficultyBaseXp(difficulty);

        // Base XP for importance
        int importanceXp = getImportanceBaseXp(importance);

        // Apply level multipliers
        int finalDifficultyXp = GameLogicUtils.calculateDifficultyXp(difficultyXp, userLevel);
        int finalImportanceXp = GameLogicUtils.calculateImportanceXp(importanceXp, userLevel);

        return finalDifficultyXp + finalImportanceXp;
    }

    private static int getDifficultyBaseXp(int difficulty) {
        switch (difficulty) {
            case 1: return Constants.XP_VERY_EASY; // 1 XP
            case 2: return Constants.XP_EASY;      // 3 XP
            case 3: return Constants.XP_HARD;      // 7 XP
            case 4: return Constants.XP_EXTREME;   // 20 XP
            default: return Constants.XP_VERY_EASY;
        }
    }

    private static int getImportanceBaseXp(int importance) {
        switch (importance) {
            case 1: return Constants.XP_NORMAL;          // 1 XP
            case 2: return Constants.XP_IMPORTANT;       // 3 XP
            case 3: return Constants.XP_VERY_IMPORTANT;  // 10 XP
            case 4: return Constants.XP_SPECIAL;         // 100 XP
            default: return Constants.XP_NORMAL;
        }
    }

    /**
     * Generiše opis zadatka za UI
     */
    public static String getTaskDescription(int difficulty, int importance) {
        String difficultyText = getDifficultyText(difficulty);
        String importanceText = getImportanceText(importance);

        return difficultyText + " zadatak (" + importanceText + ")";
    }

    private static String getDifficultyText(int difficulty) {
        switch (difficulty) {
            case 1: return "Veoma lak";
            case 2: return "Lak";
            case 3: return "Težak";
            case 4: return "Ekstremno težak";
            default: return "Nepoznat";
        }
    }

    private static String getImportanceText(int importance) {
        switch (importance) {
            case 1: return "Normalan";
            case 2: return "Važan";
            case 3: return "Ekstremno važan";
            case 4: return "Specijalan";
            default: return "Nepoznat";
        }
    }

    /**
     * Simulira dnevnu aktivnost - vraća listu XP vrednosti
     */
    public static int[] simulateDailyActivity(User user) {
        int taskCount = random.nextInt(5) + 1; // 1-5 zadataka
        int[] xpValues = new int[taskCount];

        for (int i = 0; i < taskCount; i++) {
            xpValues[i] = simulateTaskCompletion(user);
        }

        return xpValues;
    }
}