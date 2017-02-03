package fr.codazzi.smsonline.objects;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONArray;

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
}
