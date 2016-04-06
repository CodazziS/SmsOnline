package fr.codazzi.smsonline;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import fr.codazzi.smsonline.controllers.Api;

public class Ajax {


    /*static public void get(final String url, final String data, final String callback, final Object... args) {
        new Thread(new Runnable() {
            public void run() {
                Ajax.execute("GET", url + "?" + data, null, callback, args);
            }
        }).start();
    }*/

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
    /*
    static public void post(final String url, final String data, final String callback, final Object... args) {
        new Thread(new Runnable() {
            public void run() {
                Ajax.execute("POST", url, data, callback, args);
            }
        }).start();
    }
    */

    static private void execute(String method, String dataUrl, String dataPost, String callback, Api api) {
        URL url;
        HttpURLConnection connection = null;
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
                    connection.setRequestProperty("Content-Length","" + Integer.toString(dataPost.getBytes().length));
                    connection.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.writeBytes(dataPost);
                    wr.flush();
                    wr.close();
                    break;
                default:
                    return;
            }
            Ajax.getResponce(connection, callback, api);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static public void getResponce(HttpURLConnection connection, String callback, Api api) {
        InputStream is;
        BufferedReader rd;
        String line;
        String response = "";

        if (callback == null) {
            return;
        }
        try {
            is = connection.getInputStream();
            rd = new BufferedReader(new InputStreamReader(is));
            while ((line = rd.readLine()) != null) {
                response += line;
            }
            rd.close();
            switch(callback) {
                case "syncMessagesRes":
                    //Api.syncMessagesRes(response, args);
                    api.syncMessagesRes(response);
                    break;
                case "syncContactsRes":
                    //Api.syncContactsRes(response, args);
                    api.syncContactsRes(response);
                    break;
                case "addDeviceRes":
                    //Api.addDeviceRes(response, args);
                    api.addDeviceRes(response);
                    break;
                case "getTokenRes":
                    //Api test = (Api) args[0];
                    api.getTokenRes(response);
                    //Api.getTokenRes(response, args);
                    break;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
