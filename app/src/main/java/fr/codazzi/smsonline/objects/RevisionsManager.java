package fr.codazzi.smsonline.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import fr.codazzi.smsonline.Tools;

public class RevisionsManager {
    private Context context;
    private SharedPreferences settings;

    private JSONArray sms_list_ids = null;
    private JSONArray mms_list_ids = null;
    private JSONArray contact_list_ids = null;
    private JSONArray revisions = null;
    /*
        revisions = JSONObject(
            JSONArray new_sms_ids,
            JSONArray new_mms_ids,
            JSONArray new_contacts_ids);
     */

    public RevisionsManager(Context _context, SharedPreferences _settings) {
        String revision_manager_string;
        JSONObject revision_manager_object;
        this.context = _context;
        this.settings = _settings;

        revision_manager_string = this.settings.getString("RevisionsManager", null);
        if (revision_manager_string == null) {
            InitialiseRevisionsManager();
        } else {
            try {
                revision_manager_object = new JSONObject(revision_manager_string);
                sms_list_ids = revision_manager_object.getJSONArray("sms_list_ids");
                mms_list_ids = revision_manager_object.getJSONArray("mms_list_ids");
                contact_list_ids = revision_manager_object.getJSONArray("contact_list_ids");
                revisions = revision_manager_object.getJSONArray("revisions");
            } catch (Exception e) {
                e.printStackTrace();
                InitialiseRevisionsManager();
            }
        }
        /* DEBUG */
        for (int i = 0; i < this.countRevisions(); i++) {
            Log.d("REVISION", this.getRevision(i + 1).toString());
        }
    }

    private void InitialiseRevisionsManager() {
        sms_list_ids = new JSONArray();
        mms_list_ids = new JSONArray();
        contact_list_ids = new JSONArray();
        revisions = new JSONArray();
    }

    void SaveRevisionsManager() {
        SharedPreferences.Editor editor = this.settings.edit();
        JSONObject revision_manager_object = new JSONObject();
        try {
            revision_manager_object.put("sms_list_ids", sms_list_ids);
            revision_manager_object.put("mms_list_ids", mms_list_ids);
            revision_manager_object.put("contact_list_ids", contact_list_ids);
            revision_manager_object.put("revisions", revisions);

            editor.putString("RevisionsManager", revision_manager_object.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject getRevision(int id) {
        try {
            return this.revisions.getJSONObject(id - 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addRevision(JSONObject revision) {
        this.revisions.put(revision);
    }

    public int countRevisions() {
        return this.revisions.length();
    }

    public void searchNewRevision() {
        JSONObject revision = new JSONObject();
        boolean new_rev;

        new_rev = this.searchNewSms(revision);
        new_rev = this.searchNewMms(revision) || new_rev;
        new_rev = this.searchNewContacts(revision) || new_rev;

        if(new_rev) {
            Log.d("NEW_REV", revision.toString());
            this.revisions.put(revision);
            SaveRevisionsManager();
        }
    }

    private boolean searchNewSms(JSONObject revision) {
        JSONArray sms_list = SmsManager.getAllSms(context);
        JSONArray new_sms_ids = new JSONArray();
        boolean have_change = false;

        try {
            /* Add SMS */
            for (int i = 0; i < sms_list.length(); i++) {
                if (!Tools.isInJSONArray(this.sms_list_ids, sms_list.getInt(i))) {
                    this.sms_list_ids.put(sms_list.getInt(i));
                    new_sms_ids.put(sms_list.getInt(i));
                    have_change = true;
                }
            }
            revision.put("new_sms_ids", new_sms_ids);
            /* Remove SMS */

            /* Unread SMS */
        } catch (Exception e) {
            e.printStackTrace();
        }
        return have_change;
    }

    private boolean searchNewMms(JSONObject revision) {
        JSONArray mms_list = MmsManager.getAllMms(context);
        JSONArray new_mms_ids = new JSONArray();
        boolean have_change = false;

        try {
            /* Add MMS */
            for (int i = 0; i < mms_list.length(); i++) {
                if (!Tools.isInJSONArray(this.mms_list_ids, mms_list.getInt(i))) {
                    this.mms_list_ids.put(mms_list.getInt(i));
                    new_mms_ids.put(mms_list.getInt(i));
                    have_change = true;
                }
            }
            revision.put("new_mms_ids", new_mms_ids);
            /* Remove MMS */

            /* Unread MMS */
        } catch (Exception e) {
            e.printStackTrace();
        }
        return have_change;
    }

    private boolean searchNewContacts(JSONObject revision) {
        JSONArray contact_list = ContactsManager.getAllContacts(context);
        JSONArray new_contacts_ids = new JSONArray();
        boolean have_change = false;

        try {
            /* Add contact */
            for (int i = 0; i < contact_list.length(); i++) {
                if (!Tools.isInJSONArray(this.contact_list_ids, contact_list.getInt(i))) {
                    this.contact_list_ids.put(contact_list.getInt(i));
                    new_contacts_ids.put(contact_list.getInt(i));
                    have_change = true;
                }
            }
            revision.put("new_contacts_ids", new_contacts_ids);
            /* Remove contact */

        } catch (Exception e) {
            e.printStackTrace();
        }
        return have_change;
    }
}
