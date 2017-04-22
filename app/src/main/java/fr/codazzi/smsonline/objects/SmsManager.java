package fr.codazzi.smsonline.objects;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class SmsManager {

    static JSONArray getAllSms(Context context) {
        JSONArray sms_list = new JSONArray();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms");
        Cursor query = contentResolver.query(uri, new String[] { "_id" }, null, null, null);

        if (query != null && query.moveToFirst()) {
            do {
                sms_list.put(query.getInt(query.getColumnIndex("_id")));
            } while (query.moveToNext());
            query.close();
        }
        return sms_list;
    }

    static JSONArray getAllUnreadSms(Context context) {
        JSONArray sms_list = new JSONArray();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms");
        Cursor query = contentResolver.query(uri, new String[] { "_id" }, "read = 0", null, null);

        if (query != null && query.moveToFirst()) {
            do {
                sms_list.put(query.getInt(query.getColumnIndex("_id")));
            } while (query.moveToNext());
            query.close();
        }
        return sms_list;
    }

    static JSONArray getSmsValues(Context context, JSONArray ids) throws JSONException {
        JSONArray sms_list = new JSONArray();
        JSONObject sms;
        String ids_str = ids.toString();
        if (ids.length() > 0) {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://sms");
            ids_str = ids_str.replace("[", "(");
            ids_str = ids_str.replace("]", ")");
            String selection = "_id IN " + ids_str;
            Cursor query = contentResolver.query(uri, null, selection, null, null);
            if (query != null && query.moveToFirst()) {
                do {
                    sms = new JSONObject();
                    sms.put("id", query.getString(query.getColumnIndex("_id")));
                    sms.put("mess_type", "sms");
                    sms.put("address", query.getString(query.getColumnIndex("address")));
                    sms.put("date_sent", query.getString(query.getColumnIndex("date_sent")));
                    sms.put("type", query.getString(query.getColumnIndex("type")));
                    sms.put("body", query.getString(query.getColumnIndex("body")));
                    sms.put("date", query.getString(query.getColumnIndex("date")));
                    sms.put("read", query.getInt(query.getColumnIndex("read")));
                    sms_list.put(sms);
                } while (query.moveToNext());
                query.close();
            }
        }
        return sms_list;
    }

    static void sendMessage(String address, String body) {
        android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
        ArrayList<String> mSMSMessage = sms.divideMessage(body);
        ArrayList<PendingIntent> deliveredIntents;
        deliveredIntents = new ArrayList<>();
        for (int i = 0; i < mSMSMessage.size(); i++) {
            deliveredIntents.add(i, null);
        }
        sms.sendMultipartTextMessage(address, null, mSMSMessage, null, deliveredIntents);
    }
}
