package com.example.ma2025.ui.friends;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ma2025.R;
import com.example.ma2025.data.models.User;
import com.example.ma2025.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("user_id");

        if (userId == null) {
            Toast.makeText(this, "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        loadUserProfile();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profil korisnika");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadUserProfile() {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            displayUserProfile(user);
                        }
                    } else {
                        Toast.makeText(this, "Korisnik nije pronađen", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user profile", e);
                    Toast.makeText(this, "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayUserProfile(User user) {
        // Implement profile display logic
        // You can create a simple layout showing user's public info
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(user.getUsername());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}