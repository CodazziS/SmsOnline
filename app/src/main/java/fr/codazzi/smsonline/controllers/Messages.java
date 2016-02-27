package fr.codazzi.smsonline.controllers;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.codazzi.smsonline.Tools;

public class Messages {

    private String body = null;

    /*
    public JSONArray getAllMessages () {
        JSONArray messages = new JSONArray();

        return messages;
    }
    */

    public JSONArray getAllMessages(Context context) {
        JSONArray messages = null;
        Cursor sms_cursor;
        //Cursor mms_cursor;
        ContentResolver cr = context.getContentResolver();

        /* SMS */
        String [] projection_sms = {"address", "body", "date", "date_sent", "type", "read", "_id"};
        //String [] projection_sms = null;
        Uri sms_uri = Uri.parse("content://sms/");
        sms_cursor = cr.query(sms_uri, projection_sms, null, null, null);
        if (sms_cursor != null) {
            messages = messagesToArray("sms", sms_cursor);
            //logMessages("sms", sms_cursor);
            sms_cursor.close();
        }

        /* MMS */
        //String [] projection_mms = {"date", "msg_box", "read", "_id"};
        //Uri mms_uri = Uri.parse("content://mms/");
        //mms_cursor = cr.query(mms_uri, projection_mms, null, null, null);
        /*
        if (mms_cursor != null) {
                setMessages("mms", mms_cursor);
                mms_cursor.close();
            }
         */

        return messages;
    }

    public JSONArray messagesToArray(String message_type, Cursor c) {
        JSONArray messages = new JSONArray();
        JSONObject message;
        int messagesNb;
        try {
            if (c.moveToFirst()) {
                for (messagesNb = 0; messagesNb < c.getCount(); messagesNb++) {
                    if (message_type.equals("sms")) {
                        message = new JSONObject();
                        message.put("id", c.getString(c.getColumnIndex("_id")));
                        message.put("address", c.getString(c.getColumnIndex("address")));
                        message.put("date", c.getString(c.getColumnIndex("date")));
                        message.put("date_sent", c.getString(c.getColumnIndex("date_sent")));
                        message.put("read", c.getString(c.getColumnIndex("read")));
                        message.put("type", c.getString(c.getColumnIndex("type")));
                        message.put("body", c.getString(c.getColumnIndex("body")));
                        messages.put(message);
                    } else if (message_type.equals("mms")) {
                        /* @TODO MMS */
                    }
                    c.moveToNext();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public void logMessages (String message_type, Cursor c) {
        int messagesNb;
        int columnNb;

        if (c.moveToFirst()) {
            for (messagesNb = 0; messagesNb < c.getCount(); messagesNb++) {
                Log.e("-------", "-----------------------");
                for (columnNb = 0; columnNb < c.getColumnCount(); columnNb++) {
                    if (c.getColumnName(columnNb) != null && c.getString(columnNb) != null) {
                        Log.e(messagesNb + " -- " + c.getColumnName(columnNb), c.getString(columnNb));
                    }
                }
                Log.e("-------", "-----------------------");
                c.moveToNext();
            }
        }
    }
}
