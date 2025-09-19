package com.example.ma2025.utils;

public class Constants {

    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_TASKS = "tasks";
    public static final String COLLECTION_CATEGORIES = "categories";
    public static final String COLLECTION_EQUIPMENT = "equipment";
    public static final String COLLECTION_ALLIANCES = "alliances";
    public static final String COLLECTION_MESSAGES = "messages";
    public static final String COLLECTION_BOSSES = "bosses";
    public static final String COLLECTION_MISSIONS = "missions";
    public static final String COLLECTION_BADGES = "badges";
    public static final String EFFECT_ATTACK_SUCCESS = "attack_success";

    // SharedPreferences
    public static final String PREFS_NAME = "MA2025_Prefs";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_FIRST_LAUNCH = "first_launch";
    public static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String PREF_SOUND_ENABLED = "sound_enabled";
    public static final String PREF_LAST_LOGIN = "last_login";

    // Avatar options
    public static final String[] AVATAR_OPTIONS = {
            "avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5"
    };

    // Game Logic Constants
    public static final int BASE_XP_FOR_LEVEL_1 = 200;
    public static final int BASE_PP_FOR_LEVEL_1 = 40;
    public static final int BASE_BOSS_HP = 200;
    public static final int BASE_BOSS_REWARD = 200;
    public static final double BOSS_REWARD_INCREASE = 1.2;

    // Task Difficulty XP Values
    public static final int XP_VERY_EASY = 1;
    public static final int XP_EASY = 3;
    public static final int XP_HARD = 7;
    public static final int XP_EXTREME = 20;

    // Task Importance XP Values
    public static final int XP_NORMAL = 1;
    public static final int XP_IMPORTANT = 3;
    public static final int XP_VERY_IMPORTANT = 10;
    public static final int XP_SPECIAL = 100;

    // Task Status
    public static final int TASK_STATUS_ACTIVE = 0;
    public static final int TASK_STATUS_COMPLETED = 1;
    public static final int TASK_STATUS_FAILED = 2;
    public static final int TASK_STATUS_CANCELLED = 3;
    public static final int TASK_STATUS_PAUSED = 4;

    // Task Quotas (daily limits)
    public static final int DAILY_QUOTA_VERY_EASY_NORMAL = 5;
    public static final int DAILY_QUOTA_EASY_IMPORTANT = 5;
    public static final int DAILY_QUOTA_HARD_VERY_IMPORTANT = 2;
    public static final int WEEKLY_QUOTA_EXTREME = 1;
    public static final int MONTHLY_QUOTA_SPECIAL = 1;

    // Equipment Types
    public static final int EQUIPMENT_TYPE_POTION = 1;
    public static final int EQUIPMENT_TYPE_CLOTHING = 2;
    public static final int EQUIPMENT_TYPE_WEAPON = 3;

    // Equipment Effects
    public static final String EFFECT_PP_BOOST = "pp_boost";
    public static final String EFFECT_XP_BOOST = "xp_boost";
    public static final String EFFECT_COIN_BOOST = "coin_boost";
    public static final String EFFECT_ATTACK_BOOST = "attack_boost";

    public static final int EQUIPMENT_DROP_CHANCE = 25; // 25% chance
    public static final int CLOTHING_DROP_CHANCE = 15;  // 15% chance
    // Equipment Drop Chances

    // Equipment price percentages (based on boss reward)
    public static final int POTION_TEMP_20_PRICE = 50;   // 50% of boss reward
    public static final int POTION_TEMP_40_PRICE = 70;   // 70% of boss reward
    public static final int POTION_PERM_5_PRICE = 200;   // 200% of boss reward
    public static final int POTION_PERM_10_PRICE = 1000; // 1000% of boss reward
    public static final int CLOTHING_PRICE = 60;         // 60% of boss reward (gloves, shield)
    public static final int BOOTS_PRICE = 80;            // 80% of boss reward (boots)
    public static final int WEAPON_UPGRADE_PRICE = 60;   // 60% of boss reward for weapon upgrades

    // Equipment effect types (additional to existing ones)
    public static final String EFFECT_EXTRA_ATTACK = "extra_attack";

    // Equipment durability
    public static final int CLOTHING_USES = 2;  // Clothing lasts 2 battles
    public static final int POTION_SINGLE_USE = 1;  // Single-use potions

    // Quotas for special missions
    public static final int MISSION_QUOTA_STORE_VISITS = 5;
    public static final int MISSION_QUOTA_SUCCESSFUL_ATTACKS = 10;
    public static final int MISSION_QUOTA_EASY_TASKS = 10;
    public static final int MISSION_QUOTA_HARD_TASKS = 6;

    // Level Titles
    public static final String[] LEVEL_TITLES = {
            "Novajlija",        // Level 0
            "Početnik",         // Level 1
            "Istraživač",       // Level 2
            "Ratnik",           // Level 3
            "Veteran",          // Level 4
            "Majstor",          // Level 5
            "Ekspert",          // Level 6
            "Šampion",          // Level 7
            "Legenda",          // Level 8
            "Mitska Legenda",   // Level 9
            "Besmrtni"          // Level 10+
    };

    // Boss Battle
    public static final int BOSS_BASE_DAMAGE = 10;
    public static final int BOSS_CRITICAL_CHANCE = 15; // Percentage
    public static final double BOSS_CRITICAL_MULTIPLIER = 1.5;

    // Alliance/Mission
    public static final int ALLIANCE_MIN_MEMBERS = 2;
    public static final int ALLIANCE_MAX_MEMBERS = 10;
    public static final int MISSION_DURATION_DAYS = 14;
    public static final int MISSION_BASE_HP_PER_MEMBER = 100;

    // Mission Damage Values
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
    public static final String EXTRA_LEVEL = "extra_level";
    public static final String EXTRA_XP_GAINED = "extra_xp_gained";

    // Notification channels
    public static final String NOTIFICATION_CHANNEL_GENERAL = "general_notifications";
    public static final String NOTIFICATION_CHANNEL_MESSAGES = "message_notifications";
    public static final String NOTIFICATION_CHANNEL_ALLIANCES = "alliance_notifications";
    public static final String NOTIFICATION_CHANNEL_LEVEL_UP = "level_up_notifications";

    // Error messages
    public static final String ERROR_NETWORK = "Greška u mreži. Pokušajte ponovo.";
    public static final String ERROR_INVALID_EMAIL = "Neispravna email adresa.";
    public static final String ERROR_WEAK_PASSWORD = "Lozinka mora imati najmanje 6 karaktera.";
    public static final String ERROR_PASSWORDS_DONT_MATCH = "Lozinke se ne poklapaju.";
    public static final String ERROR_GENERAL = "Došlo je do greške. Pokušajte ponovo.";
    public static final String ERROR_INSUFFICIENT_XP = "Nedovoljno XP za napredovanje.";
    public static final String ERROR_INSUFFICIENT_COINS = "Nedovoljno novčića.";

    // Success messages
    public static final String SUCCESS_REGISTRATION = "Registracija uspešna! Proverite email za aktivaciju.";
    public static final String SUCCESS_LOGIN = "Uspešna prijava!";
    public static final String SUCCESS_LOGOUT = "Uspešna odjava!";
    public static final String SUCCESS_PROFILE_UPDATE = "Profil uspešno ažuriran!";
    public static final String SUCCESS_LEVEL_UP = "Čestitamo! Napredovali ste na viši nivo!";
    public static final String SUCCESS_TASK_COMPLETED = "Zadatak uspešno završen!";
    public static final String SUCCESS_EQUIPMENT_PURCHASED = "Oprema uspešno kupljena!";

    // Validation
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_TASK_TITLE_LENGTH = 3;
    public static final int MAX_TASK_TITLE_LENGTH = 50;
    public static final int MAX_TASK_DESCRIPTION_LENGTH = 200;

    // Animation durations (milliseconds)
    public static final int ANIMATION_DURATION_SHORT = 300;
    public static final int ANIMATION_DURATION_MEDIUM = 600;
    public static final int ANIMATION_DURATION_LONG = 1000;

    public static final String EQUIPMENT_TYPE_INFO = "info";

    // Cache durations (milliseconds)
    public static final long CACHE_DURATION_USER_DATA = 5 * 60 * 1000; // 5 minutes
    public static final long CACHE_DURATION_TASKS = 2 * 60 * 1000; // 2 minutes
    public static final long CACHE_DURATION_CATEGORIES = 10 * 60 * 1000; // 10 minutes

    // Network timeouts (milliseconds)
    public static final int NETWORK_TIMEOUT_CONNECTION = 10000; // 10 seconds
    public static final int NETWORK_TIMEOUT_READ = 15000; // 15 seconds
    public static final String COLLECTION_FRIENDS = "friends";
    public static final String COLLECTION_ALLIANCE_INVITATIONS = "alliance_invitations";
    public static final String COLLECTION_ALLIANCE_MESSAGES = "alliance_messages";

    // Success messages
    public static final String SUCCESS_FRIEND_ADDED = "Prijatelj je uspešno dodat!";
    public static final String SUCCESS_ALLIANCE_CREATED = "Savez je uspešno kreiran!";
    public static final String SUCCESS_ALLIANCE_JOINED = "Uspešno ste se pridružili savezu!";
    public static final String SUCCESS_INVITATION_SENT = "Poziv je poslat!";
    public static final String SUCCESS_MESSAGE_SENT = "Poruka je poslata!";
}