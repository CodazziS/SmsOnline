package fr.codazzi.smsonline.sync;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import fr.codazzi.smsonline.controllers.Api;


public class Synchronisation  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        runApi(context);
    }

    private void runApi(final Context context) {
        SharedPreferences settings = context.getSharedPreferences("swb_infos", 0);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();

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