package com.example.ma2025.utils;

public class Constants {

    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_EQUIPMENT = "equipment";
    public static final String COLLECTION_FRIENDS = "friends";
    public static final String COLLECTION_ALLIANCES = "alliances";
    public static final String COLLECTION_MISSIONS = "special_missions";
    public static final String COLLECTION_MESSAGES = "alliance_messages";

    // SharedPreferences
    public static final String PREFS_NAME = "MA2025_PREFS";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_LAST_LOGIN = "last_login";

    // Avatar options
    public static final String[] AVATAR_OPTIONS = {
            "avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5"
    };

    // XP Values
    public static final int XP_VERY_EASY = 1;
    public static final int XP_EASY = 3;
    public static final int XP_HARD = 7;
    public static final int XP_EXTREME = 20;

    public static final int XP_NORMAL = 1;
    public static final int XP_IMPORTANT = 3;
    public static final int XP_VERY_IMPORTANT = 10;
    public static final int XP_SPECIAL = 100;

    // Daily Quotas
    public static final int QUOTA_VERY_EASY_NORMAL = 5;
    public static final int QUOTA_EASY_IMPORTANT = 5;
    public static final int QUOTA_HARD_VERY_IMPORTANT = 2;
    public static final int QUOTA_EXTREME = 1; // weekly
    public static final int QUOTA_SPECIAL = 1; // monthly

    // Level progression
    public static final int BASE_XP_FOR_LEVEL_1 = 200;
    public static final int BASE_PP_FOR_LEVEL_1 = 40;

    // Boss HP
    public static final int BASE_BOSS_HP = 200;

    // Equipment prices (percentage of boss reward)
    public static final double POTION_TEMP_20_PRICE = 0.5;
    public static final double POTION_TEMP_40_PRICE = 0.7;
    public static final double POTION_PERM_5_PRICE = 2.0;
    public static final double POTION_PERM_10_PRICE = 10.0;

    public static final double CLOTHING_GLOVES_PRICE = 0.6;
    public static final double CLOTHING_SHIELD_PRICE = 0.6;
    public static final double CLOTHING_BOOTS_PRICE = 0.8;

    public static final double WEAPON_UPGRADE_PRICE = 0.6;

    // Boss rewards
    public static final int BASE_BOSS_REWARD = 200;
    public static final double BOSS_REWARD_INCREASE = 1.2; // 20% increase per level

    // Equipment drop chances
    public static final int EQUIPMENT_DROP_CHANCE = 20; // 20%
    public static final int CLOTHING_DROP_CHANCE = 95; // 95% clothing, 5% weapon

    // Battle settings
    public static final int MAX_ATTACKS_PER_BOSS = 5;
    public static final int MIN_BOSS_HP_FOR_PARTIAL_REWARD = 50; // 50%

    // Special mission settings
    public static final int MISSION_DURATION_DAYS = 14;
    public static final int BOSS_HP_PER_MEMBER = 100;

    // Mission task limits
    public static final int MISSION_MAX_STORE_VISITS = 5;
    public static final int MISSION_MAX_SUCCESSFUL_ATTACKS = 10;
    public static final int MISSION_MAX_EASY_TASKS = 10;
    public static final int MISSION_MAX_HARD_TASKS = 6;

    // Mission damage values
    public static final int MISSION_DAMAGE_STORE_VISIT = 2;
    public static final int MISSION_DAMAGE_SUCCESSFUL_ATTACK = 2;
    public static final int MISSION_DAMAGE_EASY_TASK = 1;
    public static final int MISSION_DAMAGE_HARD_TASK = 4;
    public static final int MISSION_DAMAGE_NO_FAILED_TASKS = 10;
    public static final int MISSION_DAMAGE_MESSAGE_DAY = 4;

    // Email activation
    public static final int EMAIL_ACTIVATION_TIMEOUT_HOURS = 24;

    // QR Code settings
    public static final int QR_CODE_SIZE = 300;

    // Date formats
    public static final String DATE_FORMAT_DISPLAY = "dd.MM.yyyy";
    public static final String DATE_FORMAT_TIME = "dd.MM.yyyy HH:mm";
    public static final String TIME_FORMAT = "HH:mm";

    // Intent extras
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_EQUIPMENT_ID = "extra_equipment_id";
    public static final String EXTRA_ALLIANCE_ID = "extra_alliance_id";
    public static final String EXTRA_MISSION_ID = "extra_mission_id";
    public static final String EXTRA_FRIEND_ID = "extra_friend_id";

    // Notification channels
    public static final String NOTIFICATION_CHANNEL_GENERAL = "general_notifications";
    public static final String NOTIFICATION_CHANNEL_MESSAGES = "message_notifications";
    public static final String NOTIFICATION_CHANNEL_ALLIANCES = "alliance_notifications";

    // Error messages
    public static final String ERROR_NETWORK = "Greška u mreži. Pokušajte ponovo.";
    public static final String ERROR_INVALID_EMAIL = "Neispravna email adresa.";
    public static final String ERROR_WEAK_PASSWORD = "Lozinka mora imati najmanje 6 karaktera.";
    public static final String ERROR_PASSWORDS_DONT_MATCH = "Lozinke se ne poklapaju.";
    public static final String ERROR_GENERAL = "Došlo je do greške. Pokušajte ponovo.";

    // Success messages
    public static final String SUCCESS_REGISTRATION = "Registracija uspešna! Proverite email za aktivaciju.";
    public static final String SUCCESS_LOGIN = "Uspešna prijava!";
    public static final String SUCCESS_LOGOUT = "Uspešna odjava!";
    public static final String SUCCESS_PROFILE_UPDATE = "Profil uspešno ažuriran!";

    // Validation
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
}