package fr.codazzi.smsonline;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import fr.codazzi.smsonline.controllers.Api;

public class Ajax {

    static public void get(final String url, final String data, final String callback, final Api api) {
        new Thread(new Runnable() {
            public void run() {
                Ajax.execute("GET", url + "?" + data, null, callback, api);
            }
        }).start();
    }

    static public void post(final String url, final String data, final String callback, final Api api) {
        new Thread(new Runnable() {
            public void run() {
                Ajax.execute("POST", url, data, callback, api);
            }
        }).start();
    }

    static private void execute(String method, String dataUrl, String dataPost, String callback, Api api) {
        URL url;
        HttpURLConnection connection = null;
        if (dataPost != null) {
            Tools.logDebug(dataPost);
        }

        try {
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            connection.setUseCaches(false);
            connection.setDoInput(true);

            switch (method) {
                case "GET":
                    connection.setRequestMethod("GET");
                    break;
                case "POST":
                    connection.setRequestMethod("POST");
                    if (dataPost != null) {
                        connection.setRequestProperty("Content-Length", "" + Integer.toString(dataPost.getBytes().length));
                    }
                    connection.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.writeBytes(dataPost);
                    wr.flush();
                    wr.close();
                    break;
                default:
                    return;
            }
            Ajax.getResponse(connection, callback, api);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static private void getResponse(HttpURLConnection connection, String callback, Api api) {
        InputStream is;
        BufferedReader rd;
        String line;
        String response = "";

        try {
            int status = connection.getResponseCode();
            Tools.logDebug("Status : " + status);
            is = connection.getInputStream();
            rd = new BufferedReader(new InputStreamReader(is));
            while ((line = rd.readLine()) != null) {
                response += line;
            }
            rd.close();

            if (callback == null) {
                return;
            }
            switch(callback) {
                case "syncMessagesRes":
                    api.syncMessagesRes(response);
                    break;
                case "syncMmsRes":
                    api.syncMmsRes(response);
                    break;
                case "sendMessage":
                    api.sendMessage(response);
                    break;
                case "syncContactsRes":
                    api.syncContactsRes(response);
                    break;
                case "addDeviceRes":
                    api.addDeviceRes(response);
                    break;
                case "getTokenRes":
                    api.getTokenRes(response);
                    break;
                case "getVersionRes":
                    api.getVersionRes(response);
                    break;
        }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
