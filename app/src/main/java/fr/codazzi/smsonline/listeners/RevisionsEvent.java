package fr.codazzi.smsonline.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import fr.codazzi.smsonline.objects.RevisionsManager;

public class RevisionsEvent extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        new RevAsync().execute(context);
    }
}

class RevAsync extends AsyncTask<Context, Integer, Long> {
    protected Long doInBackground(Context... contexts) {
        Context context = contexts[0];
        SharedPreferences settings = context.getSharedPreferences("swb_infos", 0);
        RevisionsManager revman = new RevisionsManager(context, settings);
        revman.searchNewRevision();
        return null;
    }
}

