package com.example.ma2025.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.ma2025.data.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TaskScheduler {
    private static final String TAG = "TaskScheduler";
    private static final long CHECK_INTERVAL = 6 * 60 * 60 * 1000L; // 6 sati

    private static TaskScheduler INSTANCE;
    private Handler handler;
    private Runnable scheduledTask;
    private TaskRepository taskRepository;
    private boolean isRunning = false;

    private TaskScheduler(Context context) {
        this.handler = new Handler(Looper.getMainLooper());
        this.taskRepository = TaskRepository.getInstance(context);
        setupScheduledTask();
    }

    public static synchronized TaskScheduler getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new TaskScheduler(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private void setupScheduledTask() {
        scheduledTask = new Runnable() {
            @Override
            public void run() {
                try {
                    String userId = getCurrentUserId();
                    if (userId != null) {
                        Log.d(TAG, "Running scheduled task expiration check for user: " + userId);
                        taskRepository.expireOverdueTasks(userId);
                    } else {
                        Log.d(TAG, "No user logged in, skipping task expiration check");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in scheduled task expiration check", e);
                } finally {
                    if (isRunning) {
                        handler.postDelayed(this, CHECK_INTERVAL);
                    }
                }
            }
        };
    }

    public void startScheduler() {
        if (!isRunning) {
            isRunning = true;
            Log.d(TAG, "Starting task scheduler - checking every " + (CHECK_INTERVAL / (60 * 60 * 1000)) + " hours");

            handler.post(scheduledTask);
        }
    }

    public void stopScheduler() {
        if (isRunning) {
            isRunning = false;
            handler.removeCallbacks(scheduledTask);
            Log.d(TAG, "Task scheduler stopped");
        }
    }

    public void runImmediateCheck() {
        Log.d(TAG, "Running immediate task expiration check");
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.expireOverdueTasks(userId);
        }
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    public boolean isRunning() {
        return isRunning;
    }
}