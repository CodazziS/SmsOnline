package fr.codazzi.smsonline.objects;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

import fr.codazzi.smsonline.Tools;


class Api implements Callable<String> {
    private String request_method;
    private String request_url;
    private String request_data;

    Api(String method, String url, String data) {
        this.request_method = method;
        this.request_url = url;
        this.request_data = data;
    }

    private void formatUrl() {
        if (this.request_method.equals("GET")) {
            this.request_url = this.request_url + "?" + this.request_data;
            this.request_data = null;
        }
    }

    @Override
    public String call() throws Exception {
        this.formatUrl();
        URL url;
        HttpURLConnection connection = null;
        String result = null;

        try {
            url = new URL(this.request_url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setUseCaches(false);
            connection.setDoInput(true);

            switch (this.request_method) {
                case "GET":
                    connection.setRequestMethod("GET");
                    break;
                case "POST":
                    connection.setRequestMethod("POST");
                    if (this.request_data != null) {
                        connection.setRequestProperty(
                                "Content-Length",
                                String.valueOf(this.request_data.getBytes().length)
                        );
                    }
                    connection.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.writeBytes(this.request_data);
                    wr.flush();
                    wr.close();
                    break;
                default:
                    throw new RuntimeException(
                            "Error: Bad method passed for Api :" + this.request_method);
            }
            result = this.getRequestResult(connection);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    private String getRequestResult(HttpURLConnection connection) {
        InputStream is;
        BufferedReader rd;
        String line;
        String res = "";

        try {
            int status = connection.getResponseCode();
            Tools.logDebug("Status : " + status);
            is = connection.getInputStream();
            rd = new BufferedReader(new InputStreamReader(is));
            while ((line = rd.readLine()) != null) {
                res += line;
            }
            rd.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
