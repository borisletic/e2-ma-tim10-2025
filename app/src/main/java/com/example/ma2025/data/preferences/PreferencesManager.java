package com.example.ma2025.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.ma2025.utils.Constants;

public class PreferencesManager {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Login state management
    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    public void setUserId(String userId) {
        editor.putString(Constants.PREF_USER_ID, userId);
        editor.apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(Constants.PREF_USER_ID, null);
    }

    public void setUserEmail(String email) {
        editor.putString(Constants.PREF_USER_EMAIL, email);
        editor.apply();
    }

    public String getUserEmail() {
        return sharedPreferences.getString(Constants.PREF_USER_EMAIL, null);
    }

    public void setLastLogin(long timestamp) {
        editor.putLong(Constants.PREF_LAST_LOGIN, timestamp);
        editor.apply();
    }

    public long getLastLogin() {
        return sharedPreferences.getLong(Constants.PREF_LAST_LOGIN, 0);
    }

    // User data caching (to reduce Firebase calls)
    public void cacheUserData(String username, int level, String title, int xp, int pp, int coins) {
        editor.putString("cached_username", username);
        editor.putInt("cached_level", level);
        editor.putString("cached_title", title);
        editor.putInt("cached_xp", xp);
        editor.putInt("cached_pp", pp);
        editor.putInt("cached_coins", coins);
        editor.putLong("cache_timestamp", System.currentTimeMillis());
        editor.apply();
    }

    public String getCachedUsername() {
        return sharedPreferences.getString("cached_username", "");
    }

    public int getCachedLevel() {
        return sharedPreferences.getInt("cached_level", 0);
    }

    public String getCachedTitle() {
        return sharedPreferences.getString("cached_title", "Novajlija");
    }

    public int getCachedXp() {
        return sharedPreferences.getInt("cached_xp", 0);
    }

    public int getCachedPp() {
        return sharedPreferences.getInt("cached_pp", 0);
    }

    public int getCachedCoins() {
        return sharedPreferences.getInt("cached_coins", 0);
    }

    public boolean isCacheValid() {
        long cacheTime = sharedPreferences.getLong("cache_timestamp", 0);
        long currentTime = System.currentTimeMillis();
        long cacheValidDuration = 5 * 60 * 1000; // 5 minuta
        return (currentTime - cacheTime) < cacheValidDuration;
    }

    // App settings
    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean("notifications_enabled", enabled);
        editor.apply();
    }

    public boolean areNotificationsEnabled() {
        return sharedPreferences.getBoolean("notifications_enabled", true);
    }

    public void setFirstTime(boolean isFirstTime) {
        editor.putBoolean("is_first_time", isFirstTime);
        editor.apply();
    }

    public boolean isFirstTime() {
        return sharedPreferences.getBoolean("is_first_time", true);
    }

    // Statistics tracking
    public void incrementAppOpens() {
        int currentOpens = sharedPreferences.getInt("app_opens", 0);
        editor.putInt("app_opens", currentOpens + 1);
        editor.apply();
    }

    public int getAppOpens() {
        return sharedPreferences.getInt("app_opens", 0);
    }

    // Clear all data (logout)
    public void clearAllData() {
        editor.clear();
        editor.apply();
    }

    // Clear only user-specific data (keep app settings)
    public void clearUserData() {
        editor.remove(Constants.PREF_IS_LOGGED_IN);
        editor.remove(Constants.PREF_USER_ID);
        editor.remove(Constants.PREF_USER_EMAIL);
        editor.remove(Constants.PREF_LAST_LOGIN);
        editor.remove("cached_username");
        editor.remove("cached_level");
        editor.remove("cached_title");
        editor.remove("cached_xp");
        editor.remove("cached_pp");
        editor.remove("cached_coins");
        editor.remove("cache_timestamp");
        editor.apply();
    }
}