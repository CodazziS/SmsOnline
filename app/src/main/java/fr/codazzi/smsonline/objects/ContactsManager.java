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

class ContactsManager {
    static JSONArray getAllContacts (Context c) {
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

    static JSONArray getContactsValues (Context c, JSONArray ids) throws JSONException {
        JSONArray contactsAll = new JSONArray();
        JSONArray contacts_without_img = new JSONArray();
        JSONArray contacts_with_img;
        JSONObject contact;
        String photo_uri;
        String ids_str = ids.toString();
        String[] projection;
        ContentResolver contentResolver = c.getContentResolver();
        ids_str = ids_str.replace("[", "(");
        ids_str = ids_str.replace("]", ")");
        String selection = "_id IN " + ids_str;

        projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
        };
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            while(!cursor.isAfterLast()){
                contact = new JSONObject();
                contact.put("name", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                contact.put("address", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                photo_uri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
                try {
                    if (photo_uri != null) {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(photo_uri));
                        if (bitmap != null) {
                            contact.put("photo", Tools.bitmapToString64(bitmap));
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                if (photo_uri == null) {
                    contacts_without_img.put(contact);
                } else {
                    contacts_with_img = new JSONArray();
                    contacts_with_img.put(contact);
                    contactsAll.put(contacts_with_img);
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        contactsAll.put(contacts_without_img);
        //contactsAll.put(0, contacts_without_img);
        //contactsAll.put(1, contacts_with_img);
        return contactsAll;
    }
}
