package fr.codazzi.smsonline;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

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
        Convert a bitmap image to string (For web view for example)
     */
    public static String bitmapToString64(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG,100, baos);
        byte [] b=baos.toByteArray();
        String temp=null;
        try{
            System.gc();
            temp=Base64.encodeToString(b, Base64.DEFAULT);
        }catch(Exception e){
            e.printStackTrace();
        }catch(OutOfMemoryError e){
            baos=new  ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG,50, baos);
            b=baos.toByteArray();
            temp=Base64.encodeToString(b, Base64.DEFAULT);
            Log.e("EWN", "Out of memory error catched");
        }
        return temp;
    }
//    static public String bitmapToString64(Bitmap photo) {
//        try {
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            photo.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//            byte[] byteArray = byteArrayOutputStream.toByteArray();
//            return Base64.encodeToString(byteArray, Base64.DEFAULT);
//        } catch (Exception e) {
//            Tools.logDebug("Image too big");
//        }
//        return "";
//    }

    /*
        Return true if the permission is granted, ask the permission on
            Android 6+
     */
    static public void getPermission (Activity ac, String permission) {

        if (Build.VERSION.SDK_INT > 22) {
            if (ac.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                ac.requestPermissions(new String[]{permission}, 0);
            }
        }
    }

    static public boolean checkPermission (Context c, String permission) {

        if (Build.VERSION.SDK_INT > 22) {
            if (c.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    static public void logDebug(int type, String tag, String data) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        switch (type) {
            case 0:
                Log.i(tag, data);
                break;
            case 1:
                Log.d(tag, data);
                break;
            case 2:
                Log.w(tag, data);
                break;
            case 3:
                Log.wtf(tag, data);
                break;
            case 4:
                Log.e(tag, data);
                break;
            default:
                Log.e(tag, data);
                break;
        }
    }

    static public void logDebug(int type, String data) {
        Tools.logDebug(type, "SWB", data);
    }

    static public void logDebug(String data) {
        Tools.logDebug(0, "SWB", data);
    }
}
