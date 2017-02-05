package fr.codazzi.smsonline.listeners;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import fr.codazzi.smsonline.objects.RevisionsManager;
import fr.codazzi.smsonline.objects.SyncManager;

public class SyncEvent extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("DEBUGSTR", "START SyncEVENT");
        SharedPreferences settings = context.getSharedPreferences("smsonline", 0);
        SyncManager syncman = new SyncManager(context, settings);
        syncman.startSynchronization();
    }
}
