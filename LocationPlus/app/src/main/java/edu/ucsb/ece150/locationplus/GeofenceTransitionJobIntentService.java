package edu.ucsb.ece150.locationplus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceTransitionJobIntentService extends JobIntentService {

    private NotificationChannel mNotificationChannel;
    private NotificationManager mNotificationManager;
    private NotificationManagerCompat mNotificationManagerCompat;
    private Notification.Builder mBuilder;
    private NotificationManager mNoticeManager;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionJobIntentService.class, 0, intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onHandleWork(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()) {
            Log.e("Geofence", GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode()));
            return;
        }

        // [TODO] This is where you will handle detected Geofence transitions. If the user has
        // arrived at their destination (is within the Geofence), then
        // 1. Create a notification and display it
        // 2. Go back to the main activity (via Intent) to handle cleanup (Geofence removal, etc.)

        mBuilder = new Notification.Builder(this);
        mBuilder.setContentTitle("GeoFence Notification");
        mBuilder.setContentText("Arrive at Destination");
        mBuilder.setSmallIcon(R.drawable.info);
        mBuilder.setPriority(Notification.PRIORITY_DEFAULT);
        mNoticeManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                "GeoFence Channel",
                "notification channel for arriving",
                NotificationManager.IMPORTANCE_HIGH);

        mNoticeManager.createNotificationChannel(channel);
        mBuilder.setChannelId("GeoFence Channel");

        int id = 0;
        mNoticeManager.notify(id, mBuilder.build());
    }
}
