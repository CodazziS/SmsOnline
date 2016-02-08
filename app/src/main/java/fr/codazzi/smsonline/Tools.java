package fr.codazzi.smsonline;

import android.content.Context;
import android.provider.Settings;

/**
 * Created by Azzod on 08/02/2016.
 */
public class Tools {
    static public String getDeviceID(Context c) {

        String deviceId;
        deviceId = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        return deviceId;
    }
}
