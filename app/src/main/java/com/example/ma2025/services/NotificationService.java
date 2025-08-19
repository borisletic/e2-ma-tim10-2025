package com.example.ma2025.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.receivers.InvitationActionReceiver;
import com.example.ma2025.utils.Constants;

public class NotificationService {

    private Context context;
    private NotificationManager notificationManager;

    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Alliance notifications channel
            NotificationChannel allianceChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ALLIANCES,
                    "Savez notifikacije",
                    NotificationManager.IMPORTANCE_HIGH
            );
            allianceChannel.setDescription("Notifikacije vezane za savez i pozive");
            notificationManager.createNotificationChannel(allianceChannel);

            // Message notifications channel
            NotificationChannel messagesChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_MESSAGES,
                    "Poruke",
                    NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription("Notifikacije za nove poruke u savezu");
            notificationManager.createNotificationChannel(messagesChannel);
        }
    }

    public void sendAllianceInvitationNotification(String allianceName, String fromUsername) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_tab", "friends");
        intent.putExtra("open_subtab", "invitations");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALLIANCES)
                .setSmallIcon(R.drawable.ic_alliance)
                .setContentTitle("Poziv za savez")
                .setContentText(fromUsername + " vas poziva u savez '" + allianceName + "'")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false) // Cannot be dismissed until responded
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_check, "Prihvati", createInvitationActionIntent("accept"))
                .addAction(R.drawable.ic_close, "Odbij", createInvitationActionIntent("decline"));

        notificationManager.notify(1001, builder.build());
    }

    public void sendAllianceMessageNotification(String allianceName, String senderUsername, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_alliance_chat", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(allianceName)
                .setContentText(senderUsername + ": " + message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(1002, builder.build());
    }

    public void sendAllianceAcceptanceNotification(String username) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_tab", "friends");
        intent.putExtra("open_subtab", "alliance");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALLIANCES)
                .setSmallIcon(R.drawable.ic_alliance)
                .setContentTitle("Poziv prihvaćen")
                .setContentText(username + " je prihvatio poziv za vaš savez")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(1003, builder.build());
    }

    private PendingIntent createInvitationActionIntent(String action) {
        Intent intent = new Intent(context, InvitationActionReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}