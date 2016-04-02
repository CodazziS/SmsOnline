package fr.codazzi.smsonline;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Azzod on 02/04/2016.
 */
public class Ajax {


    static public void get() {
        // String URL,
        // String? data
        final String data = "name=test";

        new Thread(new Runnable() {
            public void run() {
                Ajax.execute("GET", "https://devapi.swb.ovh/api/index/tester?" + data, null);
            }
        }).start();
    }

    static private void execute(String method, String dataUrl, String dataPost) {
        Log.d("THREAD", "Start execute");
        //String dataUrl = "https://devapi.swb.ovh/api/index/tester";
        //String dataUrlParameters = "name=Test";
        URL url;
        HttpURLConnection connection = null;

        try {
            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            //connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            if (method.equals("GET")) {
                connection.setRequestMethod("GET");
            } else if (method.equals("POST")) {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Length","" + Integer.toString(dataPost.getBytes().length));
                connection.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(dataPost);
                wr.flush();
                wr.close();
            } else {
                throw new IOException("Only GET and POST methods are allowed");
            }

            Log.d("THREAD", "End");
            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;

            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                Log.d("THREAD", line);
                response.append(line);
                response.append('\r');
            }
            rd.close();
            String responseStr = response.toString();
            Log.d("THREAD", "RES");

            Log.d("THREAD", responseStr);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
