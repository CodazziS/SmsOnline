package fr.codazzi.smsonline;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class Tools {

    /*
        Get the Android_id
     */
    static public String getDeviceID(Context c) {
        String deviceId;
        deviceId = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        return deviceId;
    }

    /*
        Convert a bitmap image to string (For web view for exemple)
     */
    static public String bitmapToString64(Bitmap photo) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        return encoded;
    }

    /*
        Return true if the permission is granted, ask the permission on
            Android 6+
     */
    static public boolean getPermission (Activity ac, String permission) {

        if (Build.VERSION.SDK_INT > 22) {
            if (ac.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                ac.requestPermissions(new String[]{permission}, 0);
                return false;
            }
        }
        return true;
    }

    static public boolean checkPermission (Context c, String permission) {

        if (Build.VERSION.SDK_INT > 22) {
            if (c.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
