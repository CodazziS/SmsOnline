package fr.codazzi.smsonline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SyncReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent myIntent = new Intent(context, Synchronisation.class);
        context.startService(myIntent);
    }
}