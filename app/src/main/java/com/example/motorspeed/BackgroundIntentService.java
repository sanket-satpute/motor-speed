package com.example.motorspeed;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BackgroundIntentService extends Service {

    private static final String TAG = "MyBackgroundService";
    private static final int NOTIFICATION_ID = 1;
    ValueEventListener temperatureListener, temperatureLimitListener, airQualityListener, airQualityLimitListener;
    DatabaseReference databaseReference;
    int temperature = -1, temperature_limit = -1, airQuality = -1, airQualityThreshold = -1;


    public static boolean SERVICE_IS_ON = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!SERVICE_IS_ON) {
            Log.d(TAG, "Service started");
            SERVICE_IS_ON = true;
            try {
                Notification serviceObj = createNotification();
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(
                            NOTIFICATION_ID,
                            serviceObj,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                }else {
                    startForeground(NOTIFICATION_ID, serviceObj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Thread thread = new Thread(this::registerListener);
            thread.start();
        }

        // Return START_STICKY to restart the service if it gets terminated by the system
        return START_STICKY;
    }

    private void registerListener() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        initListeners();
    }

    private void initListeners() {
        temperatureLimitListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    temperature_limit = dataSnapshot.getValue(Integer.class);
                    if (temperature >= temperature_limit) {
                        notifyHighTemperature(temperature);
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("temp_limit").addValueEventListener(temperatureLimitListener);

        temperatureListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                temperature = dataSnapshot.getValue(Integer.class);
                if (temperature >= temperature_limit) {
                    notifyHighTemperature(temperature);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("temp").addValueEventListener(temperatureListener);

        airQualityLimitListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                airQualityThreshold = dataSnapshot.getValue(Integer.class);
                if (airQuality > airQualityThreshold) {
                    notifyBadAirQuality(airQuality, airQualityThreshold);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("sen_threshold").addValueEventListener(airQualityLimitListener);

        airQualityListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                airQuality = dataSnapshot.getValue(Integer.class);
                if (airQuality > airQualityThreshold && airQualityThreshold != -1) {
                    notifyBadAirQuality(airQuality, airQualityThreshold);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("sen").addValueEventListener(airQualityListener);
    }

    private void notifyHighTemperature(int temp) {
        // Notification channel constants
        String CHANNEL_ID = "foreground_channel";//"temp_channel_id";
        CharSequence CHANNEL_NAME = "Temperature Channel Name";
        String CHANNEL_DESCRIPTION = "Temperature Channel Description";

        String message = "High Temperature Detected " + temp + "°c";
        int drawable = R.mipmap.tempreture_icon;
        String title = "High Temperature";

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(drawable)
                .setContentTitle(title)
                .setContentText(message)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notificationManager.notify(2, builder.build());
    }

    private void notifyBadAirQuality(int airQuality, int maintainQuality) {
        String CHANNEL_ID = "air_channel_id";
        CharSequence CHANNEL_NAME = "Air Quality Channel Name";
        String CHANNEL_DESCRIPTION = "Air Quality Channel Description";

        String message;
        String title;
        int drawable;

        message = "Very bad air quality " + airQuality;
        drawable = R.mipmap.notification_icon;
        title = "Bad Air Quality";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(drawable)
                .setContentTitle(title)
                .setContentText(message)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(3, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SERVICE_IS_ON = false;
        Log.d(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        String channelId = "foreground_channel";
        String channelName = "Foreground Channel";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Foreground Service Title")
                .setContentText("Foreground Service Content")
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }

        return builder.build();
    }

}