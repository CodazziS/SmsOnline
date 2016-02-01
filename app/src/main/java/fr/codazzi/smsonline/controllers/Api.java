package fr.codazzi.smsonline.controllers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import fr.codazzi.smsonline.*;


import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Api {
    String api_url = "";
    String token = null;
    String user = null;
    int lastState = 0;
    Boolean work = false;
    Context context = null;
    int error;

    public Api(Context _context) {
        this.context = _context;
    }

    public void Sync() {
        if (this.token == null || this.user == null) {
            this.getToken();
        } else {

        }
    }

    private void saveState() {
        SharedPreferences settings = this.context.getSharedPreferences("swb_infos", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("last_state", this.lastState);
        //@TODO add date
        //editor.putString("password", this.password);
        editor.apply();
    }

    private void getToken() {
        String email;
        String password;
        final Api self = this;


        try {
            if (!this.work) {
                this.work = true;
                /* Verify is we have all informations */
                SharedPreferences settings = this.context.getSharedPreferences("swb_infos", 0);
                email = URLEncoder.encode(settings.getString("email", ""), "utf-8");
                password = URLEncoder.encode(settings.getString("password", ""), "utf-8");
                if (email.length() > 4 && password.length() > 4) {
                    Log.e("AVCALL", context.getString(R.string.api_url) + "Users/GetToken?email=" + email + "&password=" + password);
                    Ion.with(context)
                            .load(context.getString(R.string.api_url) + "Users/GetToken?email=" + email + "&password=" + password)
                            .asJsonObject()
                            .setCallback(new FutureCallback<JsonObject>() {
                                @Override
                                public void onCompleted(Exception e, JsonObject result) {
                                    try {
                                        Log.e("AFCALL", result.toString());
                                        JSONObject res = new JSONObject(result.toString());
                                        int error = res.getInt("error");
                                        if (error == 0) {
                                            self.token = res.getString("token");
                                            self.user = res.getString("user");
                                        } else {
                                            self.error = error;
                                        }
                                        self.saveState();
                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            });
                } else {
                    //@TODO Mettre un code erreur dans status
                    Log.e("SYNC", "NO CREDENTIALS");
                    this.token = null;
                    this.user = null;
                }
                this.work = false;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            this.work = false;
        }
    }

}
