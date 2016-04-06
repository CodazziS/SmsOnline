package fr.codazzi.smsonline.sync;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.os.Process;
import android.util.Log;

import fr.codazzi.smsonline.BuildConfig;
import fr.codazzi.smsonline.controllers.Api;


public class Synchronisation  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SYNC LOOP", "LOOP");
        SharedPreferences settings = context.getSharedPreferences("swb_infos", 0);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();

        /*
        if (settings.getBoolean("reset_api", false)) {
            Log.d("Sync", "Reset API");
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong("last_sms", 0);
            editor.putLong("last_mms", 0);
            editor.putString("api_token", null);
            editor.putString("api_key", null);
            editor.putString("api_user", null);
            editor.putString("api_unread_sms", "");
            editor.putBoolean("reset_api", false);
            editor.putBoolean("working", false);
            editor.apply();
            return;
        }
        */
        if (        network == null
                || (settings.getBoolean("wifi_only", true) && network.getType() != ConnectivityManager.TYPE_WIFI)
                || !network.isConnectedOrConnecting()) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("error", 2);
            editor.apply();
            return;
        }

        new Api(context, settings).Run();
    }
}