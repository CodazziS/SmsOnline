package fr.codazzi.smsonline.controllers;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class Messages {

    private long lastDateSms = 0;
    private long lastDateMms = 0;
    private long maxSmsCurrentDate;
    private long maxMmsCurrentDate;
    String unreadSmsList = "";


    public JSONArray getLastsMessages(Context context, long _lastDateSms, long _lastDateMms, String _unreadSmsList) {

        this.lastDateSms = _lastDateSms;
        this.lastDateMms = _lastDateMms;
        this.unreadSmsList = _unreadSmsList;
        JSONArray messages = null;
        String selection = "date > " + this.lastDateSms + " " +
                "OR (_id IN (" + this.unreadSmsList + ") AND read = 1)";
        JSONArray sms = this.getSms(context, selection);
        //JSONArray mms = null;

        /* TODO : Concat MMS + SMS */
        messages = sms;
        return messages;
    }

    public JSONArray getAllMessages(Context context) {

        JSONArray messages = null;
        JSONArray sms = this.getSms(context, null);
        //JSONArray mms = null;

        /* TODO : Concat MMS + SMS */
        messages = sms;
        return messages;
    }

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

    private JSONArray getMms(Context context) {
        JSONArray mms = null;
        //Cursor mms_cursor;
        ContentResolver cr = context.getContentResolver();

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

    private JSONArray messagesToArray(String message_type, Cursor c) {
        JSONArray messages = new JSONArray();
        JSONObject message;
        int messagesNb;
        String mess_id;
        maxSmsCurrentDate = 0;

        try {
            if (c.moveToFirst()) {
                for (messagesNb = 0; messagesNb < c.getCount(); messagesNb++) {
                    if (message_type.equals("sms")) {
                        mess_id = c.getString(c.getColumnIndex("_id"));
                        message = new JSONObject();
                        message.put("id", mess_id);
                        message.put("address", c.getString(c.getColumnIndex("address")));
                        message.put("date", c.getString(c.getColumnIndex("date")));
                        message.put("date_sent", c.getString(c.getColumnIndex("date_sent")));
                        message.put("read", c.getString(c.getColumnIndex("read")));
                        message.put("type", c.getString(c.getColumnIndex("type")));
                        message.put("body", c.getString(c.getColumnIndex("body")));
                        messages.put(message);
                        if (c.getLong(c.getColumnIndex("date")) > this.maxSmsCurrentDate) {
                            this.maxSmsCurrentDate = c.getLong(c.getColumnIndex("date"));
                        }
                        if (c.getString(c.getColumnIndex("read")).equals("0")) {
                            if (!this.unreadSmsList.equals("")) {
                                this.unreadSmsList += ",";
                            }
                            this.unreadSmsList += mess_id;
                        } else if (this.unreadSmsList.contains(mess_id)) {
                            /* If the ID is in the string */
                            this.unreadSmsList = this.unreadSmsList.replace(mess_id + ",", "");
                            /* If the ID is the first ID of the String */
                            this.unreadSmsList = this.unreadSmsList.replace("," + mess_id, "");
                            /* If the ID is the unique ID of the string */
                            if (this.unreadSmsList.equals(mess_id)) {
                                this.unreadSmsList = "";
                            }
                        }
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

    /*
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
    */
}
