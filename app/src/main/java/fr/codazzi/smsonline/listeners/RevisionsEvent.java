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
        SharedPreferences settings = context.getSharedPreferences("swb_rev", 0);

        Log.d("DEBUGSTR", "START REVISIONEVENT");
        RevisionsManager revman = new RevisionsManager(context, settings);
        revman.searchNewRevision();
    }
/*
    private void runApi(final Context context) {
//        SharedPreferences settings = context.getSharedPreferences("swb_infos", 0);
//        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo network = cm.getActiveNetworkInfo();
//        Tools.logDebug("Sync");
//        if (network == null
//                || (settings.getBoolean("wifi_only", true) && network.getType() != ConnectivityManager.TYPE_WIFI)
//                || !network.isConnectedOrConnecting()) {
//            SharedPreferences.Editor editor = settings.edit();
//            editor.putInt("error", 2);
//            editor.apply();
//            return;
//        }
//        new Api(context, settings).Run();
    }
    */
}
