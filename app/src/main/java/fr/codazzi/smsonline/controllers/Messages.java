package fr.codazzi.smsonline.controllers;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;

class Messages {
    public long lastDateMessage = 0;
    public String unreadMessagesList = "";

    public JSONArray getLastsMessages(Context context, long _lastDateMessage, String _unreadMessagesList) {
        this.lastDateMessage = _lastDateMessage;
        this.unreadMessagesList = _unreadMessagesList;

        JSONArray messages;
        String selection = "date > " + this.lastDateMessage + " " +
            "OR (_id IN (" + this.unreadMessagesList + ") AND read = 1)";

        messages = this.getSms(context, selection);
        // SELECTION messages = this.getMms(context, null, messages);
        return messages;
    }

    public JSONArray getAllMessages(Context context) {
        JSONArray messages;

        messages = this.getSms(context, null);
        messages = this.getMms(context, null, messages);
        return messages;
    }

    private JSONArray getMms(Context context, String selection, JSONArray messages) {
        JSONObject message;
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://mms/");
        Cursor query = contentResolver.query(uri, null, selection, null, null);
        try {
            if (query != null && query.moveToFirst()) {
                do {
                    String id = query.getString(query.getColumnIndex("_id"));
                    Log.e("MMS ID", id);
                } while (query.moveToNext());
                query.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    private JSONArray getSms(Context context, String selection) {
        JSONArray messages = new JSONArray();
        JSONObject message;
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms");
        Cursor query = contentResolver.query(uri, null, selection, null, null);
        try {
            if (query != null && query.moveToFirst()) {
                do {
                    message = new JSONObject();
                    // SMS
                    message.put("id", query.getString(query.getColumnIndex("_id")));
                    message.put("mess_type", "sms");
                    message.put("address", query.getString(query.getColumnIndex("address")));
                    message.put("date_sent", query.getString(query.getColumnIndex("date_sent")));
                    message.put("type", query.getString(query.getColumnIndex("type")));
                    message.put("body", query.getString(query.getColumnIndex("body")));
                    message.put("date", query.getString(query.getColumnIndex("date")));
                    message.put("read", query.getString(query.getColumnIndex("read")));
                    updateLastMessage(query.getLong(query.getColumnIndex("date")));
                    messages.put(message);
                } while (query.moveToNext());
                query.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    private String getAddressNumber(Context context, String id) {
        String selectionAdd = "msg_id=" + id;
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        Cursor cAdd = context.getContentResolver().query(uriAddress, null,
                selectionAdd, null, null);
        String name = null;
        if (cAdd != null && cAdd.moveToFirst()) {
            do {
                String number = cAdd.getString(cAdd.getColumnIndex("address"));
                if (number != null) {
                    try {
                        Long.parseLong(number.replace("-", ""));
                        name = number;
                    } catch (NumberFormatException nfe) {
                        if (name == null) {
                            name = number;
                        }
                    }
                }
            } while (cAdd.moveToNext());
        }
        if (cAdd != null) {
            cAdd.close();
        }
        return name;
    }

    private Bitmap getMmsImage(Context context, String _id) {
        Uri partURI = Uri.parse("content://mms/part/" + _id);
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = context.getContentResolver().openInputStream(partURI);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return bitmap;
    }

    private String getMmsText(Context context, String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = context.getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException e) {}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return sb.toString();
    }

    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /* **************************************************************************/
    /*
    public JSONArray getAllMessagesOLD(Context context) {

        JSONArray messages = null;
        JSONArray sms = this.getSms(context, null);
        JSONArray mms = this.getMms(context, null);


        messages = sms;
        return messages;
    }*/
/*
    public JSONObject confirmDates() {
        this.lastDateSms = this.maxSmsCurrentDate;
        JSONObject obj = new JSONObject();
        try {
            obj.put("lastDateSms", lastDateSms);
            obj.put("lastDateMms", lastDateMms);
            obj.put("unreadSmsList", unreadSmsList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
/*
    private JSONArray getMms(Context context, String selection) {
        JSONArray mms = null;
        Cursor mms_cursor;
        ContentResolver cr = context.getContentResolver();
        String [] projection_mms = {"address", "date", "msg_box", "read", "_id"};
        Uri mms_uri = Uri.parse("content://mms/");

        mms_cursor = cr.query(mms_uri, null, selection, null, null);

        if (mms_cursor != null) {
            mms =  messagesToArray("mms", mms_cursor);
            //setMessages("mms", mms_cursor);
            mms_cursor.close();
        }
        return mms;
    }

    private JSONArray getSms(Context context, String selection) {
        JSONArray sms = null;
        Cursor sms_cursor;
        ContentResolver cr = context.getContentResolver();
        String [] projection_sms = {"address", "body", "date", "date_sent", "type", "read", "_id"};
        Uri sms_uri = Uri.parse("content://sms/");

        sms_cursor = cr.query(sms_uri, projection_sms, selection, null, null);
        if (sms_cursor != null) {
            sms = messagesToArray("sms", sms_cursor);
            sms_cursor.close();
        }
        return sms;
    }
*/
    private void showCursorRow(Cursor c) {
        int nb_cols = c.getColumnCount();
        int col = 0;
        Log.w("MESSAGE", "-----------------------------------------------------");
        for (col = 0; col < nb_cols; col++) {
            Log.w("MESSAGE", c.getColumnName(col) + " => " + c.getString(col));
        }
        Log.w("MESSAGE", "-----------------------------------------------------");
    }

    private void updateLastMessage(long date) {
        if (this.lastDateMessage < date) {
            this.lastDateMessage = date;
        }
    }

    static public void sendMessage(String address, String body, String type) {
        if (type.equals("sms")) {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> mSMSMessage = sms.divideMessage(body);
            ArrayList<PendingIntent> deliveredIntents;

            deliveredIntents = new ArrayList<>();
            for (int i = 0; i < mSMSMessage.size(); i++) {
                deliveredIntents.add(i, null);
            }
            sms.sendMultipartTextMessage(address, null, mSMSMessage, null, deliveredIntents);
        }
    }
}
