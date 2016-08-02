package fr.codazzi.smsonline.controllers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.codazzi.smsonline.Tools;

class Contacts {

    public static JSONArray getAllContacts (Context c) throws JSONException {
        JSONArray contactsAll = new JSONArray();
        JSONArray contacts_without_img = new JSONArray();
        JSONArray contacts_with_img = new JSONArray();
        JSONObject contact;

        String photo_uri;

        ContentResolver contentResolver = c.getContentResolver();
        String[] projection;

        projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
        };
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
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
                    contacts_with_img.put(contact);
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        contactsAll.put(0, contacts_without_img);
        contactsAll.put(1, contacts_with_img);
        return contactsAll;
    }
}
