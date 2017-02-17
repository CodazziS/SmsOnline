package fr.codazzi.smsonline.listeners;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import fr.codazzi.smsonline.objects.RevisionsManager;
import fr.codazzi.smsonline.objects.SyncManager;

public class NotificationEvent extends NotificationListenerService {
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        new NotifAsync().execute(context);
    }
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}

class NotifAsync extends AsyncTask<Context, Integer, Long> {
    protected Long doInBackground(Context... contexts) {
        Context context = contexts[0];
        SharedPreferences settings = context.getSharedPreferences("swb_infos", 0);
        RevisionsManager revman = new RevisionsManager(context, settings);
        revman.searchNewRevision();
        SyncManager syncman = new SyncManager(context, settings);
        syncman.startSynchronization();
        return null;
    }
}
