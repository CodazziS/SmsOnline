package fr.codazzi.smsonline.objects;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import fr.codazzi.smsonline.Tools;

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

    static JSONArray getMmsValues(Context context, JSONArray ids) throws JSONException {
        JSONArray mms_list = new JSONArray();
        JSONObject mms;
        String ids_str = ids.toString();
        String mms_type;
        int mms_id;

        if (ids.length() > 0) {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://mms");
            ids_str = ids_str.replace("[", "(");
            ids_str = ids_str.replace("]", ")");
            String selection = "_id IN " + ids_str;
            Log.d("smsManagerIDS", selection);
            Cursor query = contentResolver.query(uri, null, selection, null, null);
            if (query != null && query.moveToFirst()) {
                do {
                    mms_id = query.getInt(query.getColumnIndex("_id"));
                    mms_type = query.getString(query.getColumnIndex("msg_box"));

                    mms = new JSONObject();
                    mms.put("id",mms_id );
                    mms.put("mess_type", "mms");
                    mms.put("address", MmsManager.getAddressNumber(context, mms_id, mms_type));
                    mms.put("date_sent", query.getString(query.getColumnIndex("date")) + "000");
                    mms.put("type", mms_type);
                    mms.put("date", query.getString(query.getColumnIndex("date")) + "000");
                    mms.put("read", query.getString(query.getColumnIndex("read")));
                    mms = MmsManager.getMmsInfos(context, mms_id, mms);
                    mms_list.put(mms);
                } while (query.moveToNext());
                query.close();
            }
        }
        return mms_list;
    }

    static private String getAddressNumber(Context context, int id, String type) {
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

    static private JSONObject getMmsInfos(Context context, int mmsId, JSONObject mms)
            throws JSONException {
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
                        body += MmsManager.getMmsText(context, partId);
                    } else {
                        body += cursor.getString(cursor.getColumnIndex("text"));
                    }
                }
                if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
                        "image/gif".equals(type) || "image/jpg".equals(type) ||
                        "image/png".equals(type)) {
                    parts.put(Tools.bitmapToString64(MmsManager.getMmsImage(context, partId)));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        mms.put("body", body);
        mms.put("parts", parts);
        return mms;
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
}
