package fr.codazzi.smsonline.objects;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONArray;

public class MmsManager {

    static JSONArray getAllMms(Context context) {
        JSONArray mms_list = new JSONArray();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://mms");
        Cursor query = contentResolver.query(uri, new String[] { "_id" }, null, null, null);
        if (query != null && query.moveToFirst()) {
            do {
                mms_list.put(query.getInt(query.getColumnIndex("_id")));
            } while (query.moveToNext());
            query.close();
        }
        return mms_list;
    }
}
