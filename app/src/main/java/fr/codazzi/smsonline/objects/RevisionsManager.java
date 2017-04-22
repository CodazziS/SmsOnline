package fr.codazzi.smsonline.objects;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;

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
    private JSONArray unreadSms = null;
    private JSONArray unreadMms = null;
    private int lastRevisionDeleted = 1;
    private String name = null;
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
                name = revision_manager_object.getString("name");
                unreadSms = revision_manager_object.getJSONArray("unreadSms");
                unreadMms = revision_manager_object.getJSONArray("unreadMms");
                lastRevisionDeleted = revision_manager_object.getInt("lastRevisionDeleted");
            } catch (Exception e) {
                e.printStackTrace();
                InitialiseRevisionsManager();
            }
        }
        /* DEBUG */
//        Log.d("REVISION", "* ***** START REVISION LIST ***** *");
//        for (int i = 0; i < this.countRevisions(); i++) {
//            Log.d("REVISION_" + i, this.getRevision(i + 1).toString());
//        }
//        Log.d("REVISION", "* ***** END  REVISION  LIST ***** *");
    }

    private void InitialiseRevisionsManager() {
        sms_list_ids = new JSONArray();
        mms_list_ids = new JSONArray();
        contact_list_ids = new JSONArray();
        revisions = new JSONArray();
        unreadSms = new JSONArray();
        unreadMms = new JSONArray();
        name = Tools.getRandomString(20);
    }

    private void SaveRevisionsManager() {
        SharedPreferences.Editor editor = this.settings.edit();
        JSONObject revision_manager_object = new JSONObject();
        try {
            revision_manager_object.put("sms_list_ids", sms_list_ids);
            revision_manager_object.put("mms_list_ids", mms_list_ids);
            revision_manager_object.put("contact_list_ids", contact_list_ids);
            revision_manager_object.put("revisions", revisions);
            revision_manager_object.put("name", name);
            revision_manager_object.put("lastRevisionDeleted", lastRevisionDeleted);
            revision_manager_object.put("unreadSms", unreadSms);
            revision_manager_object.put("unreadMms", unreadMms);

            editor.putString("RevisionsManager", revision_manager_object.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetRevisions() {
        Tools.logDebug("Reset revisions");
        this.InitialiseRevisionsManager();
        this.SaveRevisionsManager();
    }

    void deleteRevision(int id) {
        JSONObject revision;
        try {
            for (int i = this.lastRevisionDeleted - 1; i < id - 1; i++) {
                revision = this.revisions.getJSONObject(i);
                revision.put("new_sms_ids", null);
                revision.put("new_mms_ids", null);
                revision.put("new_contacts_ids", null);
                this.revisions.put(i, revision);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    JSONObject getRevision(int id) {
        try {
            if (id < this.lastRevisionDeleted) {
                this.resetRevisions();
                return null;
            }
            return this.revisions.getJSONObject(id - 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    public void addRevision(JSONObject revision) {
//        this.revisions.put(revision);
//    }

    public String getName() {
        return this.name;
    }

    public int countRevisions() {
        return this.revisions.length();
    }

    public void searchNewRevision() {
        JSONObject revision = new JSONObject();
        boolean new_rev = false;

        if (Tools.checkPermission(context, Manifest.permission.READ_SMS)) {
            new_rev = this.searchNewSms(revision);
            new_rev = this.searchUnreadSms(revision) || new_rev;
            if (this.settings.getBoolean("sync_mms", false)) {
                new_rev = this.searchNewMms(revision) || new_rev;
                new_rev = this.searchUnreadMms(revision) || new_rev;
            } else {
                try {
                    revision.put("new_mms_ids", new JSONArray());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (Tools.checkPermission(context, Manifest.permission.READ_CONTACTS)) {
            new_rev = this.searchNewContacts(revision) || new_rev;
        }
        if(new_rev) {
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
            /* @TODO Remove SMS */
        } catch (Exception e) {
            e.printStackTrace();
        }
        return have_change;
    }

    private boolean searchUnreadSms(JSONObject revision) {
        JSONArray sms_list = SmsManager.getAllUnreadSms(context);
        JSONArray new_sms_ids = new JSONArray();
        boolean have_change = false;

        try {
            if (revision.has("new_sms_ids")) {
                new_sms_ids = revision.getJSONArray("new_sms_ids");
            }

            for (int i = 0; i < this.unreadSms.length(); i++) {
                if (!Tools.isInJSONArray(sms_list, this.unreadSms.getInt(i))) {
                    new_sms_ids.put(this.unreadSms.getInt(i));
                    have_change = true;
                }
            }
            revision.put("new_sms_ids", new_sms_ids);
            this.unreadSms = sms_list;
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

        } catch (Exception e) {
            e.printStackTrace();
        }
        return have_change;
    }

    private boolean searchUnreadMms(JSONObject revision) {
        JSONArray mms_list = MmsManager.getAllUnreadMms(context);
        JSONArray new_mms_ids = new JSONArray();
        boolean have_change = false;

        try {
            if (revision.has("new_mms_ids")) {
                new_mms_ids = revision.getJSONArray("new_mms_ids");
            }

            for (int i = 0; i < this.unreadMms.length(); i++) {
                if (!Tools.isInJSONArray(mms_list, this.unreadMms.getInt(i))) {
                    new_mms_ids.put(this.unreadMms.getInt(i));
                    have_change = true;
                }
            }
            revision.put("new_mms_ids", new_mms_ids);
            this.unreadMms = mms_list;
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
