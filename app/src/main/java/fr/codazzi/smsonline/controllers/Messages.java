package fr.codazzi.smsonline.controllers;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.telephony.SmsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;

import fr.codazzi.smsonline.Tools;

class Messages {
    public long lastDateSms = 0;
    public long lastDateMms = 0;
    public String unreadSmsList = "";
    public String unreadMmsList = "";

    public JSONArray getLastsSms(Context context, long _lastDateSms, String _unreadSmsList) {
        this.lastDateSms = _lastDateSms;
        this.unreadSmsList = _unreadSmsList;

        String selection_sms = "date > " + this.lastDateSms + " " +
            "OR (_id IN (" + this.unreadSmsList + ") AND read = 1)";

        return this.getSms(context, selection_sms);
    }

    public JSONArray getLastsMms(Context context, long _lastDateMms, String _unreadMmsList) {
        this.lastDateMms = _lastDateMms;
        this.unreadMmsList = _unreadMmsList;
        JSONArray messages = new JSONArray();
        String selection_mms = "date > " + this.lastDateMms + " " +
                "OR (_id IN (" + this.unreadMmsList + ") AND read = 1)";
        messages = this.getMms(context, selection_mms, messages);
        return messages;
    }

    public JSONArray getAllSms(Context context) {
        return this.getSms(context, null);
    }

    public JSONArray getAllMms(Context context) {
        JSONArray messages = new JSONArray();
        return this.getMms(context, null, messages);
    }

    private JSONArray getMms(Context context, String selection, JSONArray messages) {
        JSONObject message;
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://mms/");
        Cursor query = contentResolver.query(uri, null, selection, null, null);
        long date;
        String id;
        String type;

        try {
            if (query != null && query.moveToFirst()) {
                do {
                    message = new JSONObject();
                    id = query.getString(query.getColumnIndex("_id"));
                    date = query.getLong(query.getColumnIndex("date"));
                    type = query.getString(query.getColumnIndex("msg_box"));
                    // MMS
                    message.put("id", id);
                    message.put("mess_type", "mms");
                    message.put("address", this.getAddressNumber(context, id, type));
                    message.put("date_sent", query.getString(query.getColumnIndex("date")) + "000");
                    message.put("type", type);
                    message.put("date", query.getString(query.getColumnIndex("date")) + "000");
                    message.put("read", query.getString(query.getColumnIndex("read")));
                    if (this.lastDateMms < date) {
                        this.lastDateMms = date;
                    }
                    messages.put(message);
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
        long date;

        try {
            if (query != null && query.moveToFirst()) {
                do {
                    message = new JSONObject();
                    // SMS
                    date = query.getLong(query.getColumnIndex("date"));
                    message.put("id", query.getString(query.getColumnIndex("_id")));
                    message.put("mess_type", "sms");
                    message.put("address", query.getString(query.getColumnIndex("address")));
                    message.put("date_sent", query.getString(query.getColumnIndex("date_sent")));
                    message.put("type", query.getString(query.getColumnIndex("type")));
                    message.put("body", query.getString(query.getColumnIndex("body")));
                    message.put("date", query.getString(query.getColumnIndex("date")));
                    message.put("read", query.getString(query.getColumnIndex("read")));
                    if (this.lastDateSms < date) {
                        this.lastDateSms = date;
                    }
                    messages.put(message);
                } while (query.moveToNext());
                query.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    static public JSONObject getMmsInfos(Context context, String mmsId, JSONObject message) throws JSONException {
        String selectionPart = "mid=" + mmsId;
        Uri uri = Uri.parse("content://mms/part");
        Cursor cursor = context.getContentResolver().query(uri, null, selectionPart, null, null);
        String body = "";
        JSONArray parts = new JSONArray();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String partId = cursor.getString(cursor.getColumnIndex("_id"));
                String type = cursor.getString(cursor.getColumnIndex("ct"));
                if ("text/plain".equals(type)) {
                    String data = cursor.getString(cursor.getColumnIndex("_data"));
                    if (data != null) {
                        body += Messages.getMmsText(context, partId);
                    } else {
                        body += cursor.getString(cursor.getColumnIndex("text"));
                    }
                }
                if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
                        "image/gif".equals(type) || "image/jpg".equals(type) ||
                        "image/png".equals(type)) {
                    parts.put(Tools.bitmapToString64(Messages.getMmsImage(context, partId)));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        message.put("body", body);
        message.put("parts", parts);
        return message;
    }

    private String getAddressNumber(Context context, String id, String type) {
        String selectionAdd = "msg_id=" + id;
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        Cursor cAdd = context.getContentResolver().query(uriAddress, null, selectionAdd, null, null);
        String name = null;

        if (cAdd != null && cAdd.moveToFirst()) {
            do {
                String val = cAdd.getString(cAdd.getColumnIndex("address"));
                String c_type = cAdd.getString(cAdd.getColumnIndex("type"));

                if (val != null) {
                    name = val;
                    if (type.equals("2") && c_type.equals("151") ||
                            type.equals("1") && c_type.equals("137")) {
                        break;
                    }
                }
            } while (cAdd.moveToNext());
        }
        if (cAdd != null) {
            cAdd.close();
        }
        return name;
    }

    static public Bitmap getMmsImage(Context context, String _id) {
        Uri partURI = Uri.parse("content://mms/part/" + _id);
        InputStream is;
        Bitmap bitmap = null;
        try {
            is = context.getContentResolver().openInputStream(partURI);
            if (is != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeStream(is, null, options);
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    static public String getMmsText(Context context, String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is;
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
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

//    private void showCursorRow(Cursor c) {
//        int nb_cols = c.getColumnCount();
//        int col;
//        Tools.logDebug("-----------------------------------------------------");
//        for (col = 0; col < nb_cols; col++) {
//            Tools.logDebug(2, c.getColumnName(col) + " => " + c.getString(col));
//        }
//        Tools.logDebug("-----------------------------------------------------");
//    }

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
