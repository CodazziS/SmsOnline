package fr.codazzi.smsonline;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.logging.LogRecord;

public class Synchronisation extends Service {

    private NotificationManager mNM;
    private int NOTIFICATION = R.string.app_name;

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, "SWB crash", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("TEST ICI", "TEST OK");
        return null;
    }

    //private final IBinder mBinder = new LocalBinder();

    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Started";



        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_info_black_24dp)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("Test")  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    public void SayHello() {
        Log.e("HELLOWORD", "HELLO !");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SayHello();
            }
        }, 10000);
    }
}
