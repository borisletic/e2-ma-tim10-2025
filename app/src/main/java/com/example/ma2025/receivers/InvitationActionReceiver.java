package com.example.ma2025.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.ma2025.data.repositories.AllianceRepository;

public class InvitationActionReceiver extends BroadcastReceiver {

    private static final String TAG = "InvitationActionReceiver";

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String invitationId = intent.getStringExtra("invitation_id");

        if (invitationId == null) {
            Log.e(TAG, "Invitation ID is null");
            return;
        }

        AllianceRepository repository = new AllianceRepository();

        if ("accept".equals(action)) {
            repository.respondToInvitation(invitationId, true, new AllianceRepository.OnOperationCompleteListener() {
                @SuppressLint("LongLogTag")
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Invitation accepted successfully");
                    // Remove notification
                    removeNotification(context);
                }

                @SuppressLint("LongLogTag")
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error accepting invitation: " + error);
                }
            });

        } else if ("decline".equals(action)) {
            repository.respondToInvitation(invitationId, false, new AllianceRepository.OnOperationCompleteListener() {
                @SuppressLint("LongLogTag")
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Invitation declined successfully");
                    // Remove notification
                    removeNotification(context);
                }

                @SuppressLint("LongLogTag")
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error declining invitation: " + error);
                }
            });
        }
    }

    private void removeNotification(Context context) {
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1001); // Alliance invitation notification ID
    }
}