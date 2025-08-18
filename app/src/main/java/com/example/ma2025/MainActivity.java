// Updated MainActivity.java
package com.example.ma2025;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ma2025.data.DatabaseManager;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.databinding.ActivityMainBinding;
import com.example.ma2025.ui.auth.LoginActivity;
import com.example.ma2025.ui.profile.ProfileFragment;
import com.example.ma2025.ui.statistics.StatisticsFragment;
import com.example.ma2025.ui.levels.LevelsFragment;
import com.example.ma2025.ui.equipment.EquipmentFragment;
import com.example.ma2025.ui.friends.FriendsFragment;
import com.example.ma2025.utils.Constants;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferencesManager preferencesManager;
    private DatabaseManager databaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        preferencesManager = new PreferencesManager(this);

        // Initialize Database Manager
        databaseManager = DatabaseManager.getInstance(this);

        Log.d(TAG, "Firebase inicijalizovan uspešno!");

        // Check if user is logged in
        if (!preferencesManager.isLoggedIn() || mAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        setupUI();
        setupBottomNavigation();

        // Initialize user data in local database
        initializeUserDatabase();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new ProfileFragment());
            binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }

        // Track app usage
        preferencesManager.incrementAppOpens();
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("MA2025");
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (item.getItemId() == R.id.nav_statistics) {
                selectedFragment = new StatisticsFragment();
            } else if (item.getItemId() == R.id.nav_levels) {
                selectedFragment = new LevelsFragment();
            } else if (item.getItemId() == R.id.nav_equipment) {
                selectedFragment = new EquipmentFragment();
            } else if (item.getItemId() == R.id.nav_friends) {
                selectedFragment = new FriendsFragment();
            }

            return loadFragment(selectedFragment);
        });
    }

    private void initializeUserDatabase() {
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "User ID is null during database initialization");
            return;
        }

        Log.d(TAG, "Initializing local database for user: " + userId);

        databaseManager.initializeUserData(userId, new DatabaseManager.OnInitializationCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Database initialization successful: " + message);

                // Optionally sync with Firebase after initialization
                syncWithFirebase();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Database initialization failed: " + error);
                Toast.makeText(MainActivity.this,
                        "Greška pri inicijalizaciji podataka: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void syncWithFirebase() {
        String userId = preferencesManager.getUserId();
        if (userId == null) return;

        Log.d(TAG, "Starting Firebase sync for user: " + userId);

        databaseManager.syncWithFirebase(userId, new DatabaseManager.OnSyncCallback() {
            @Override
            public void onSyncCompleted(String message) {
                Log.d(TAG, "Firebase sync completed: " + message);
                // Optionally show a subtle success indicator
            }

            @Override
            public void onSyncFailed(String error) {
                Log.w(TAG, "Firebase sync failed: " + error);
                // Don't show error to user unless it's critical
                // App should work offline with SQLite
            }
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            openSettings();
            return true;
        } else if (item.getItemId() == R.id.action_sync) {
            // Now this will work with the updated menu
            manualSync();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSettings() {
        // TODO: Open settings activity
        Toast.makeText(this, "Podešavanja će biti dodana uskoro", Toast.LENGTH_SHORT).show();
    }

    private void manualSync() {
        String userId = preferencesManager.getUserId();
        if (userId == null) return;

        Toast.makeText(this, "Počinje sinhronizacija...", Toast.LENGTH_SHORT).show();

        databaseManager.syncWithFirebase(userId, new DatabaseManager.OnSyncCallback() {
            @Override
            public void onSyncCompleted(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Sinhronizacija završena!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onSyncFailed(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Sinhronizacija neuspešna: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void logout() {
        String userId = preferencesManager.getUserId();

        if (userId != null) {
            // Clear user data from local database
            databaseManager.clearUserData(userId, new DatabaseManager.OnClearDataCallback() {
                @Override
                public void onDataCleared(String message) {
                    Log.d(TAG, "Local user data cleared: " + message);
                    completeLogout();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error clearing local data: " + error);
                    // Still proceed with logout even if local cleanup fails
                    completeLogout();
                }
            });
        } else {
            completeLogout();
        }
    }

    private void completeLogout() {
        // Sign out from Firebase
        mAuth.signOut();

        // Clear user data from preferences
        preferencesManager.clearUserData();

        // Show success message
        Toast.makeText(this, Constants.SUCCESS_LOGOUT, Toast.LENGTH_SHORT).show();

        // Redirect to login
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if user is still authenticated
        if (mAuth.getCurrentUser() == null || !preferencesManager.isLoggedIn()) {
            redirectToLogin();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }
}