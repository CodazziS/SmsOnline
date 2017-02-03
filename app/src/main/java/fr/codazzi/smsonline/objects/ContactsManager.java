package fr.codazzi.smsonline.objects;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.codazzi.smsonline.Tools;

public class ContactsManager {
    public static JSONArray getAllContacts (Context c) {
        JSONArray contacts = new JSONArray();
        ContentResolver contentResolver = c.getContentResolver();
        Cursor query = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[] { "_id" },
                null,
                null,
                null);

        if (query != null && query.moveToFirst()) {
            do {
                contacts.put(query.getInt(query.getColumnIndex("_id")));
            } while (query.moveToNext());
            query.close();
        }

        return contacts;
    }
}
