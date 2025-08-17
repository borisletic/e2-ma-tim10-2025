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

        Log.d(TAG, "Firebase inicijalizovan uspešno!");

        // Check if user is logged in
        if (!preferencesManager.isLoggedIn() || mAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        setupUI();
        setupBottomNavigation();

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
            // TODO: Open settings
            Toast.makeText(this, "Podešavanja će biti dodana uskoro", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
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
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }
}