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

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialize Firebase safely
            initializeFirebase();

            // Initialize preferences
            preferencesManager = new PreferencesManager(this);

            Log.d(TAG, "MainActivity initialized successfully");

            // Check if user is logged in
            if (!isUserLoggedIn()) {
                redirectToLogin();
                return;
            }

            // Setup UI
            setupUI();
            setupBottomNavigation();

            // Initialize database manager with error handling
            initializeDatabaseManager();

            // Load default fragment
            if (savedInstanceState == null) {
                loadFragment(new ProfileFragment());
                binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
            }

            // Track app usage
            preferencesManager.incrementAppOpens();

        } catch (Exception e) {
            Log.e(TAG, "Critical error in MainActivity onCreate", e);
            handleCriticalError(e);
        }
    }

    private void initializeFirebase() {
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            Log.d(TAG, "Firebase initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            Toast.makeText(this, "Greška pri inicijalizaciji Firebase servisa", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isUserLoggedIn() {
        try {
            boolean isLoggedInPrefs = preferencesManager.isLoggedIn();
            boolean hasFirebaseUser = mAuth.getCurrentUser() != null;

            Log.d(TAG, "User logged in (prefs): " + isLoggedInPrefs + ", Firebase user exists: " + hasFirebaseUser);

            return isLoggedInPrefs && hasFirebaseUser;

        } catch (Exception e) {
            Log.e(TAG, "Error checking login status", e);
            return false;
        }
    }

    private void setupUI() {
        try {
            setSupportActionBar(binding.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("MA2025");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI", e);
        }
    }

    private void setupBottomNavigation() {
        try {
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;

                try {
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

                } catch (Exception e) {
                    Log.e(TAG, "Error in bottom navigation selection", e);
                    Toast.makeText(MainActivity.this, "Greška pri učitavanju stranice", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation", e);
        }
    }

    private void initializeDatabaseManager() {
        try {
            // Initialize Database Manager
            databaseManager = DatabaseManager.getInstance(this);

            // Initialize user data in local database
            initializeUserDatabase();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing database manager", e);
            Toast.makeText(this, "Greška pri inicijalizaciji baze podataka", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeUserDatabase() {
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "User ID is null during database initialization");
            return;
        }

        Log.d(TAG, "Initializing local database for user: " + userId);

        try {
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
                    // Don't show error to user immediately, app should still work
                    // Toast.makeText(MainActivity.this, "Greška pri inicijalizaciji podataka: " + error, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error during database initialization", e);
        }
    }

    private void syncWithFirebase() {
        String userId = preferencesManager.getUserId();
        if (userId == null) return;

        Log.d(TAG, "Starting Firebase sync for user: " + userId);

        try {
            databaseManager.syncWithFirebase(userId, new DatabaseManager.OnSyncCallback() {
                @Override
                public void onSyncCompleted(String message) {
                    Log.d(TAG, "Firebase sync completed: " + message);
                }

                @Override
                public void onSyncFailed(String error) {
                    Log.w(TAG, "Firebase sync failed: " + error);
                    // App should work offline with SQLite, so don't show error
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error during Firebase sync", e);
        }
    }

    private boolean loadFragment(Fragment fragment) {
        try {
            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
                return true;
            }
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment", e);
            Toast.makeText(this, "Greška pri učitavanju stranice", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating options menu", e);
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            if (item.getItemId() == R.id.action_logout) {
                logout();
                return true;
            }
            return super.onOptionsItemSelected(item);

        } catch (Exception e) {
            Log.e(TAG, "Error handling menu item selection", e);
            return false;
        }
    }

    private void logout() {
        try {
            String userId = preferencesManager.getUserId();

            if (userId != null && databaseManager != null) {
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

        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            // Force logout even if there's an error
            completeLogout();
        }
    }

    public void navigateToEquipment() {
        try {
            // Switch to equipment tab
            binding.bottomNavigation.setSelectedItemId(R.id.nav_equipment);

            // Load equipment fragment
            Fragment fragment = new EquipmentFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to equipment", e);
        }
    }

    private void completeLogout() {
        try {
            // Sign out from Firebase
            if (mAuth != null) {
                mAuth.signOut();
            }

            // Clear user data from preferences
            preferencesManager.clearUserData();

            // Show success message
            Toast.makeText(this, Constants.SUCCESS_LOGOUT, Toast.LENGTH_SHORT).show();

            // Redirect to login
            redirectToLogin();

        } catch (Exception e) {
            Log.e(TAG, "Error completing logout", e);
            redirectToLogin(); // Force redirect even if there's an error
        }
    }

    private void redirectToLogin() {
        try {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error redirecting to login", e);
            // Force close app if we can't redirect
            finishAffinity();
        }
    }

    private void handleCriticalError(Exception e) {
        try {
            Toast.makeText(this, "Kritična greška u aplikaciji. Aplikacija će se zatvoriti.", Toast.LENGTH_LONG).show();

            // Try to logout user
            if (preferencesManager != null) {
                preferencesManager.clearUserData();
            }

            // Redirect to login or close app
            redirectToLogin();

        } catch (Exception ex) {
            Log.e(TAG, "Error handling critical error", ex);
            finishAffinity(); // Force close app
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            // Check if user is still authenticated
            if (!isUserLoggedIn()) {
                Log.d(TAG, "User not authenticated in onResume, redirecting to login");
                redirectToLogin();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (binding != null) {
                binding = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
}