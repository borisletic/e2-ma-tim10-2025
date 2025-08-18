package com.example.ma2025.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

public class EmailActivationChecker {

    private static final String TAG = "EmailActivationChecker";
    private static final long ACTIVATION_TIMEOUT_HOURS = 24;
    private static final long ACTIVATION_TIMEOUT_MS = ACTIVATION_TIMEOUT_HOURS * 60 * 60 * 1000;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ScheduledExecutorService scheduler;

    public EmailActivationChecker() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Stores the registration timestamp for timeout tracking
     */
    public void startActivationTimeout(String userId) {
        Map<String, Object> activationData = new HashMap<>();
        activationData.put("registrationTime", System.currentTimeMillis());
        activationData.put("isActivated", false);
        activationData.put("activationDeadline", System.currentTimeMillis() + ACTIVATION_TIMEOUT_MS);

        db.collection("email_activations")
                .document(userId)
                .set(activationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Activation timeout started for user: " + userId);
                    scheduleTimeoutCheck(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to start activation timeout", e);
                });
    }

    /**
     * Schedules a check to delete unactivated accounts after 24 hours
     */
    private void scheduleTimeoutCheck(String userId) {
        scheduler.schedule(() -> {
            checkAndDeleteExpiredAccount(userId);
        }, ACTIVATION_TIMEOUT_HOURS, TimeUnit.HOURS);
    }

    /**
     * Checks if account is still unactivated and deletes it if expired
     */
    private void checkAndDeleteExpiredAccount(String userId) {
        db.collection("email_activations")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isActivated = documentSnapshot.getBoolean("isActivated");
                        Long deadline = documentSnapshot.getLong("activationDeadline");

                        if (Boolean.FALSE.equals(isActivated) &&
                                deadline != null &&
                                System.currentTimeMillis() > deadline) {

                            deleteExpiredAccount(userId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check activation status", e);
                });
    }

    /**
     * Deletes expired unactivated account
     */
    private void deleteExpiredAccount(String userId) {
        // Delete user from Firestore
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deleted expired user from Firestore: " + userId);

                    // Delete activation tracking document
                    db.collection("email_activations")
                            .document(userId)
                            .delete();

                    // Note: Firebase Auth user deletion requires admin SDK or user re-authentication
                    // For production, implement a Cloud Function for this
                    Log.d(TAG, "Account cleanup completed for: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete expired user", e);
                });
    }

    /**
     * Marks account as activated when email is verified
     */
    public void markAccountActivated(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isActivated", true);
        updates.put("activationTime", System.currentTimeMillis());

        db.collection("email_activations")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Account marked as activated: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark account as activated", e);
                });
    }

    /**
     * Checks if activation link has expired
     */
    public void checkActivationStatus(String userId, ActivationStatusCallback callback) {
        db.collection("email_activations")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isActivated = documentSnapshot.getBoolean("isActivated");
                        Long deadline = documentSnapshot.getLong("activationDeadline");

                        if (Boolean.TRUE.equals(isActivated)) {
                            callback.onActivated();
                        } else if (deadline != null && System.currentTimeMillis() > deadline) {
                            callback.onExpired();
                        } else {
                            callback.onPending();
                        }
                    } else {
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Gets remaining time for activation in milliseconds
     */
    public void getRemainingActivationTime(String userId, TimeCallback callback) {
        db.collection("email_activations")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long deadline = documentSnapshot.getLong("activationDeadline");
                        if (deadline != null) {
                            long remaining = deadline - System.currentTimeMillis();
                            callback.onTimeReceived(Math.max(0, remaining));
                        } else {
                            callback.onTimeReceived(0);
                        }
                    } else {
                        callback.onTimeReceived(0);
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onTimeReceived(0);
                });
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    // Callback interfaces
    public interface ActivationStatusCallback {
        void onActivated();
        void onPending();
        void onExpired();
        void onNotFound();
        void onError(String error);
    }

    public interface TimeCallback {
        void onTimeReceived(long remainingTimeMs);
    }
}