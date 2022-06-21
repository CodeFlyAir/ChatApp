package com.example.chatapp.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.chatapp.R;
import com.example.chatapp.activities.ChatActivity;
import com.example.chatapp.models.Users;
import com.example.chatapp.utilities.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;
import java.util.Random;

public class MessagingService extends FirebaseMessagingService
{
    @Override
    public void onNewToken (@NonNull String token)
    {
        super.onNewToken(token);
        Log.d("FCM", "Token : " + token);
    }
    
    @Override
    public void onMessageReceived (@NonNull RemoteMessage message)
    {
        super.onMessageReceived(message);
        //Log.d("FCM", "Message : " + Objects.requireNonNull(message.getNotification()).getBody());
        
        Users users = new Users();
        users.id = message.getData().get(Constants.KEY_USER_ID);
        users.name = message.getData().get(Constants.KEY_NAME);
        users.token = message.getData().get(Constants.KEY_FCM_TOKEN);
        
        int notificationId = new Random().nextInt();
        String channelId = "chat_message";
        
        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.KEY_USER, users);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(users.name);
        builder.setContentText(message.getData().get(Constants.KEY_MESSAGE));
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                message.getData().get(Constants.KEY_MESSAGE)
        ));
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
        {
            CharSequence channelName = "Chat Message";
            String channelDescription = "This channel is used for chat message notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationChannel.setDescription(channelDescription);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(notificationId, builder.build());
    }
}
