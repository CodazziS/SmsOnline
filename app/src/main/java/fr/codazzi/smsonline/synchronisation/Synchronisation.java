package fr.codazzi.smsonline.synchronisation;


import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;

import fr.codazzi.smsonline.controllers.Api;


public class Synchronisation extends Service {
    Api api = null;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        if (this.api != null) {
            this.api = new Api();
        }
        syncLoop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void syncLoop() {
        Log.i("SYNCHRONIZATION", "SYNC NOW");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                syncLoop();
            }
        }, 15000);
    }
}
