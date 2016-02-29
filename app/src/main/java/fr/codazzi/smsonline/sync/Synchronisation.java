package fr.codazzi.smsonline.sync;


import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;

import fr.codazzi.smsonline.BuildConfig;
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
        if (this.api == null) {
            this.api = new Api(this);
        }
        syncLoop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void syncLoop() {
        int time = 20000;
        if (this.api.Sync()) {
            time = 700;
        }
        if (BuildConfig.DEBUG) {
            time = 5000;
        }
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                syncLoop();
            }
        }, time);
    }
}
