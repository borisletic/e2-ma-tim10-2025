package com.example.ma2025;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Test Firebase konekcije
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "Firebase inicijalizovan uspešno!");
    }
}