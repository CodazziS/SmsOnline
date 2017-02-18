package fr.codazzi.smsonline.listeners;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import fr.codazzi.smsonline.objects.SyncManager;

public class SyncEvent extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        new SyncAsync().execute(context);
    }
}

class SyncAsync extends AsyncTask<Context, Integer, Long> {
    protected Long doInBackground(Context... contexts) {
        Context context = contexts[0];
        SharedPreferences settings = context.getSharedPreferences("smsonline", 0);
        SyncManager syncman = new SyncManager(context, settings);
        syncman.startSynchronization();
        return null;
    }
}