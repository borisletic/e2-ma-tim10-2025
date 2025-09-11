package com.example.ma2025;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ma2025.data.DatabaseManager;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.databinding.ActivityMainBinding;
import com.example.ma2025.ui.auth.LoginActivity;
import com.example.ma2025.ui.boss.BossFragment;
import com.example.ma2025.ui.categories.CategoriesFragment;
import com.example.ma2025.ui.missions.MissionsFragment;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferencesManager preferencesManager;
    private DatabaseManager databaseManager;

    private LinearLayout currentSelectedItem;
    private int currentSelectedColor;
    private int defaultColor;

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                currentSelectedColor = getResources().getColor(R.color.primary_color, getTheme());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                defaultColor = getResources().getColor(R.color.text_secondary, getTheme());
            }

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
                setSelectedNavigationItem(binding.navProfile);
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
            // Setup click listeners for all navigation items
            binding.navProfile.setOnClickListener(v -> {
                loadFragment(new ProfileFragment());
                setSelectedNavigationItem(binding.navProfile);
            });

            binding.navTasks.setOnClickListener(v -> {
                loadFragment(new StatisticsFragment()); // StatisticsFragment is now TaskFragment
                setSelectedNavigationItem(binding.navTasks);
            });

            binding.navCategories.setOnClickListener(v -> {
                loadFragment(new CategoriesFragment());
                setSelectedNavigationItem(binding.navCategories);
            });

            binding.navLevels.setOnClickListener(v -> {
                loadFragment(new LevelsFragment());
                setSelectedNavigationItem(binding.navLevels);
            });

            binding.navEquipment.setOnClickListener(v -> {
                loadFragment(new EquipmentFragment());
                setSelectedNavigationItem(binding.navEquipment);
            });

            binding.navFriends.setOnClickListener(v -> {
                loadFragment(new FriendsFragment());
                setSelectedNavigationItem(binding.navFriends);
            });

            binding.navBoss.setOnClickListener(v -> {
                loadFragment(new BossFragment());
                setSelectedNavigationItem(binding.navBoss);
            });

            binding.navMissions.setOnClickListener(v -> {
                loadFragment(new MissionsFragment());
                setSelectedNavigationItem(binding.navMissions);
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
            loadFragment(new EquipmentFragment());
            setSelectedNavigationItem(binding.navEquipment);
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

    private void setSelectedNavigationItem(LinearLayout selectedItem) {
        try {
            // Reset previous selection
            if (currentSelectedItem != null) {
                setNavigationItemStyle(currentSelectedItem, defaultColor);
            }

            // Set new selection
            currentSelectedItem = selectedItem;
            setNavigationItemStyle(selectedItem, currentSelectedColor);

        } catch (Exception e) {
            Log.e(TAG, "Error setting selected navigation item", e);
        }
    }

    private void setNavigationItemStyle(LinearLayout item, int color) {
        try {
            // Find ImageView and TextView in the item
            ImageView icon = (ImageView) item.getChildAt(0);
            TextView text = (TextView) item.getChildAt(1);

            if (icon != null) {
                icon.setColorFilter(color);
            }
            if (text != null) {
                text.setTextColor(color);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting navigation item style", e);
        }
    }

    private Fragment createPlaceholderFragment(String title) {
        // Create a simple placeholder fragment for features not yet implemented
        return new Fragment() {
            @Override
            public android.view.View onCreateView(android.view.LayoutInflater inflater,
                                                  android.view.ViewGroup container, Bundle savedInstanceState) {

                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setGravity(android.view.Gravity.CENTER);
                layout.setPadding(32, 32, 32, 32);

                TextView titleView = new TextView(getContext());
                titleView.setText(title);
                titleView.setTextSize(20);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    titleView.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
                }
                titleView.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);

                TextView subtitle = new TextView(getContext());
                subtitle.setText("Uskoro dostupno");
                subtitle.setTextSize(16);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    subtitle.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                }
                subtitle.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);

                layout.addView(titleView);
                layout.addView(subtitle);

                return layout;
            }
        };
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