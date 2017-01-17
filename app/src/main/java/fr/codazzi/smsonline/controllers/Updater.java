package fr.codazzi.smsonline.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;

import fr.codazzi.smsonline.Ajax;

public class Updater {
    SharedPreferences settings;
    Context context;
    Revisions rev;
    int remote_revision;

    public Updater(Context _context, SharedPreferences _settings) {
        this.context = _context;
        this.settings = _settings;
        this.rev = new Revisions(this.context, this.settings);
    }

    public void run() {
        this.updateRevisions();
        this.connexion();
    }

    private void updateRevisions() {
        Messages mess = new Messages();
        JSONArray sms_ids = mess.getAllSmsId(this.context);
        JSONArray mms_ids = new JSONArray();
        JSONArray unread_ids = new JSONArray();
        this.rev.addRevision(sms_ids, mms_ids, unread_ids, mess.lastDateSms, mess.lastDateMms);
    }

    private void syncRevisions () {

    }

    private void getNewActions () {

    }

    private boolean connexion() {
//        String url = this.api_url + "Index/GetVersion";
//        String email = URLEncoder.encode(this.settings.getString("email", ""), "utf-8");
//        String password = URLEncoder.encode(this.settings.getString("password", ""), "utf-8");
//        if (email.length() <= 4 || password.length() <= 4) {
//            this.check_error(1);
//            this.saveSettings();
//            return;
//        }
//        Ajax.get(url, "", "getVersionRes", this);
        // Check version

        return false;
    }

    private checkCredentials() {

    }

    private addDevice() {

    }

    private getRemoteRevision () {

    }
}
