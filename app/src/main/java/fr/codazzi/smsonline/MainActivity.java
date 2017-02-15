package fr.codazzi.smsonline;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import fr.codazzi.smsonline.listeners.LaunchEvent;

public class MainActivity extends AppCompatActivity {
    private String c_activity;
    private long lastPressTime;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.settings = this.getSharedPreferences("swb_infos", 0);
        putMain();
        LaunchEvent.setAlarm(this);
        this.refresh_loop();
    }

    private void refresh_loop() {
        try {
            TextView status;
            int status_int;
            status = (TextView) findViewById(R.id.status_str);
            status_int = this.settings.getInt("SyncManagerStatus", R.string.sta_unknow);
            status.setText(getResources().getString(status_int));
        } catch (Exception e) {
            e.printStackTrace();
        }
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        refresh_loop();
                    }
                },
                2000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (c_activity.equals("main")) {
            long pressTime = System.nanoTime();
            long diff = (pressTime - lastPressTime) / 1000000;
            if (diff <= 2000) {
                finish();
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.home_back_to_quit),
                        Toast.LENGTH_SHORT
                ).show();
            }
            lastPressTime = pressTime;
        } else {
            putMain();
            Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.home_change_not_saved),
                    Snackbar.LENGTH_LONG
            ).setAction("Action", null).show();
        }
    }

    /* Ask Permissions */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String perm[], @NonNull int[] grtRes) {
        if (grtRes.length > 0) {
            this.loopPermissions();
        }
    }

    private void loopPermissions() {
        if (!Tools.checkPermission(this, Manifest.permission.READ_CONTACTS)) {
            Tools.getPermission(this, Manifest.permission.READ_CONTACTS);
        }
        if (!Tools.checkPermission(this, Manifest.permission.READ_SMS)) {
            Tools.getPermission(this, Manifest.permission.READ_SMS);
        }
        if (!Tools.checkPermission(this, Manifest.permission.SEND_SMS)) {
            Tools.getPermission(this, Manifest.permission.SEND_SMS);
        }
    }

    private void askPermissions() {
        final MainActivity main = this;
        if (Tools.checkPermission(main, Manifest.permission.READ_CONTACTS) &&
                Tools.checkPermission(main, Manifest.permission.READ_SMS) &&
                Tools.checkPermission(main, Manifest.permission.SEND_SMS)) {
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.permissions_title))
            .setMessage(getString(R.string.permissions_text))
            .setPositiveButton(getString(R.string.permissions_ask), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    main.loopPermissions();
                    dialog.dismiss();
                }
            })
            .setNegativeButton(getString(R.string.permission_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .show();
    }

    /* Navigation */
    private void putMain() {
        TextView status;
        Toolbar toolbar;
        int status_int;

        c_activity = "main";
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        status = (TextView) findViewById(R.id.status_str);
        status_int = this.settings.getInt("SyncManagerStatus", R.string.sta_unknow);
        status.setText(getResources().getString(status_int));

        try {
            if (!Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners").contains(getPackageName())) {
                LinearLayout notification_warning = (LinearLayout) findViewById(R.id.notification_warning);
                notification_warning.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* On clicks */
    public void goToSettings(View view) {
        EditText email;
        EditText password;
        EditText server_uri;
        TextView settings_logs;
        Button saveBtn;
        Button clear_logs;
        CheckBox wifi_only;
        CheckBox show_full;
        CheckBox sync_mms;

        this.c_activity = "settings";

        /* Set view */
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* Find all components */
        email = (EditText) findViewById(R.id.config_email);
        password = (EditText) findViewById(R.id.config_password);
        wifi_only = (CheckBox) findViewById(R.id.config_wifi_only);
        show_full = (CheckBox) findViewById(R.id.displayFullSettings);
        sync_mms = (CheckBox) findViewById(R.id.settingsSyncMms);
        server_uri = (EditText) findViewById(R.id.config_uri);
        saveBtn = (Button) findViewById(R.id.saveSettings);
        clear_logs = (Button) findViewById(R.id.settingsResetLogs);
        settings_logs = (TextView) findViewById(R.id.settingsLogs);

        /* set values */
        email.setText(this.settings.getString("email", ""));
        password.setText(this.settings.getString("password", ""));
        wifi_only.setChecked(this.settings.getBoolean("wifi_only", true));
        sync_mms.setChecked(this.settings.getBoolean("sync_mms", true));
        server_uri.setText(this.settings.getString("server_uri2", getString(R.string.api_url)));
        settings_logs.setText(Tools.getStoreLog(this.getApplicationContext()));

        /* Submit form */
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings(v);
            }
        });
        clear_logs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetLogs(v);
            }
        });
        show_full.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullSettings(v);
            }
        });
    }
    public void goToMain(View view) {
        putMain();
        this.askPermissions();
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.home_refreshed), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
    public void putInfos(View view) {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.app_name) + " - V" + pInfo.versionName + " (" + pInfo.versionCode + ")",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void goToWebsite(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.main_url)));
        startActivity(browserIntent);
    }
    public void goToNotificationSettings(View view) {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
    }

    private void resetLogs(View view) {
        Tools.resetLogs(this.getApplicationContext());
        TextView settings_logs = (TextView) findViewById(R.id.settingsLogs);
        settings_logs.setText(Tools.getStoreLog(this.getApplicationContext()));
    }

    private void showFullSettings(View view) {
        CheckBox checkbox_display;
        CheckBox checkbox_sync_mms;
        EditText server_api;
        TextView settings_logs;
        Button clear_logs;

        settings_logs = (TextView) findViewById(R.id.settingsLogs);
        server_api = (EditText) findViewById(R.id.config_uri);
        checkbox_display = (CheckBox) findViewById(R.id.displayFullSettings);
        checkbox_sync_mms = (CheckBox) findViewById(R.id.settingsSyncMms);
        clear_logs = (Button) findViewById(R.id.settingsResetLogs);

        if (checkbox_display.isChecked() || checkbox_display.isActivated() || checkbox_display.isFocused()) {
            checkbox_sync_mms.setVisibility(View.VISIBLE);
            settings_logs.setVisibility(View.VISIBLE);
            server_api.setVisibility(View.VISIBLE);
            clear_logs.setVisibility(View.VISIBLE);
        } else {
            checkbox_sync_mms.setVisibility(View.GONE);
            settings_logs.setVisibility(View.GONE);
            server_api.setVisibility(View.GONE);
            clear_logs.setVisibility(View.GONE);
        }
    }

    private void saveSettings(View view) {
        EditText email;
        EditText password;
        EditText server_uri;
        CheckBox wifi_only;
        CheckBox checkbox_sync_mms;

        email = (EditText) findViewById(R.id.config_email);
        password = (EditText) findViewById(R.id.config_password);
        server_uri = (EditText) findViewById(R.id.config_uri);
        wifi_only = (CheckBox) findViewById(R.id.config_wifi_only);
        checkbox_sync_mms = (CheckBox) findViewById(R.id.settingsSyncMms);

        SharedPreferences.Editor editor = this.settings.edit();
        editor.putString("email", email.getText().toString());
        editor.putString("password", password.getText().toString());
        editor.putString("server_uri2", server_uri.getText().toString());
        editor.putBoolean("wifi_only", wifi_only.isChecked() || wifi_only.isActivated() || wifi_only.isFocused());
        editor.putBoolean("sync_mms", checkbox_sync_mms.isChecked() || checkbox_sync_mms.isActivated() || checkbox_sync_mms.isFocused());
        editor.apply();

        putMain();
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.home_change_saved),
            Snackbar.LENGTH_LONG
        ).setAction("Action", null).show();
        this.askPermissions();
    }
}
