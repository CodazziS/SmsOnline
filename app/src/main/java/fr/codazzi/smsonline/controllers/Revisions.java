package fr.codazzi.smsonline.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class Revisions {
    JSONObject storage;
    Context context;
    SharedPreferences settings;

    public Revisions(Context _context, SharedPreferences _settings) {
        String revisionStringify;
        this.context = _context;
        this.settings = _settings;

        revisionStringify = this.settings.getString("revisions", null);
        if (revisionStringify == null) {
            InitialiseRevisions();
        } else {
            try {
                this.storage = new JSONObject(revisionStringify);
            } catch (Exception e) {
                e.printStackTrace();
                InitialiseRevisions();
            }
        }
    }

    private void InitialiseRevisions() {
        try {
            this.storage = new JSONObject();
            this.storage.put("revisions", new JSONArray());
            this.storage.put("date_last_search", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject getRevision(int id) {
        try {
            JSONArray revisions = this.storage.getJSONArray("revisions");
            return revisions.getJSONObject(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addRevision(JSONArray sms_ids, JSONArray mms_ids, JSONArray unread_ids,
                            long date_sms, long date_mms) {
        try {
            JSONArray revisions = this.storage.getJSONArray("revisions");
            JSONObject revision = new JSONObject();
            int id = revisions.length() + 1;
            revision.put("sms_ids", sms_ids);
            revision.put("mms_ids", mms_ids);
            revision.put("unread_ids", unread_ids);
            revision.put("id", id);
            revisions.put(revision);
            this.storage.put("date_last_sms", date_sms);
            this.storage.put("date_last_mms", date_mms);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int countRevisions() {
        try {
            return this.storage.getJSONArray("revisions").length();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
