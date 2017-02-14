package fr.codazzi.smsonline.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import fr.codazzi.smsonline.objects.RevisionsManager;

public class RevisionsEvent extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences("swb_infos", 0);

        Log.d("DEBUGSTR", "START REVISIONEVENT");
        RevisionsManager revman = new RevisionsManager(context, settings);
        revman.searchNewRevision();
    }
}
