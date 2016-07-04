package fr.codazzi.smsonline;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import fr.codazzi.smsonline.controllers.Api;
import fr.codazzi.smsonline.sync.Synchronisation;

public class MainActivity extends AppCompatActivity {
    private String c_activity;
    private long lastPressTime;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.settings = this.getSharedPreferences("swb_infos", 0);
        putMain();
        this.setAlarm();
    }

    private void setAlarm() {
        Intent myIntent = new Intent(this, Synchronisation.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,  0, myIntent, 0);
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 1); // first time
        long frequency = 120000; // in ms
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), frequency, pendingIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (c_activity.equals("main")) {
            long pressTime = System.nanoTime();
            long diff = (pressTime - lastPressTime) / 1000000;
            if (diff <= 2000) {
                finish();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.home_back_to_quit), Toast.LENGTH_SHORT).show();
            }
            lastPressTime = pressTime;
        } else {
            putMain();
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.home_change_not_saved), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    /* Ask Permissions */
    @Override
    public void onRequestPermissionsResult(int requestCode, String perm[], int[] grtRes) {
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
        TextView sync;
        Toolbar toolbar;
        Date last_sync_date;
        int error;
        int state;
        long last_sync;

        c_activity = "main";
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        status = (TextView) findViewById(R.id.status_str);
        error = this.settings.getInt("error", 0);
        state = this.settings.getInt("api_state", 0);
        if (error != 0) {
            if (error == -1) {
                status.setText(getResources().getStringArray(R.array.errors_str)[0]);
            } else {
                status.setText(getResources().getStringArray(R.array.errors_str)[error]);
            }
        } else {
            status.setText(getResources().getStringArray(R.array.actions_str)[state]);
        }
        sync = (TextView) findViewById(R.id.sync_str);
        last_sync = settings.getLong("last_sync", 0);
        if (last_sync != 0) {
            last_sync_date = new Date(last_sync);
            DateFormat dateFormat = DateFormat.getDateInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            sync.setText(String.valueOf(dateFormat.format(last_sync_date) + " " + sdf.format(last_sync_date)));
        } else {
            sync.setText(getResources().getString(R.string.never_sync));
        }
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
        Button saveBtn;
        CheckBox wifi_only;

        String default_api_url;
        String default_email = "";
        String default_password = "";

        if (BuildConfig.DEBUG) {
            default_api_url = getString(R.string.api_url_debug);
        } else {
            default_api_url = getString(R.string.api_url);
        }

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            Snackbar.make(findViewById(android.R.id.content),
                    pInfo.versionName,
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            if (pInfo.versionName.contains("alpha")) {
                default_email = "alpha@example.com";
                default_password = "@z3rtY";
                default_api_url = getString(R.string.api_url_debug);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        c_activity = "settings";
        email = (EditText) findViewById(R.id.config_email);
        email.setText(this.settings.getString("email", default_email));
        password = (EditText) findViewById(R.id.config_password);
        password.setText(this.settings.getString("password", default_password));
        wifi_only = (CheckBox) findViewById(R.id.config_wifi_only);
        wifi_only.setChecked(this.settings.getBoolean("wifi_only", true));

        server_uri = (EditText) findViewById(R.id.config_uri);
        server_uri.setText(this.settings.getString("server_uri", default_api_url));

        saveBtn = (Button) findViewById(R.id.saveSettings);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings(v);
            }
        });
    }
    public void goToMain(View view) {
        putMain();
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

    public void saveSettings(View view) {
        EditText email;
        EditText password;
        EditText server_uri;
        CheckBox wifi_only;

        email = (EditText) findViewById(R.id.config_email);
        password = (EditText) findViewById(R.id.config_password);
        server_uri = (EditText) findViewById(R.id.config_uri);
        wifi_only = (CheckBox) findViewById(R.id.config_wifi_only);

        SharedPreferences.Editor editor = this.settings.edit();
        editor.putString("email", email.getText().toString());
        editor.putString("password", password.getText().toString());
        editor.putString("server_uri", server_uri.getText().toString());
        editor.putBoolean("reset_api", true);
        editor.putBoolean("wifi_only", wifi_only.isChecked() || wifi_only.isActivated() || wifi_only.isFocused());
        editor.apply();

        this.askPermissions();
        putMain();
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.home_change_saved), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        this.logonLoop();
    }

    private void logonLoop() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();
        if ((this.settings.getBoolean("wifi_only", true) && network.getType() != ConnectivityManager.TYPE_WIFI)
                || !network.isConnectedOrConnecting()) {
            SharedPreferences.Editor editor = this.settings.edit();
            editor.putInt("error", 2);
            editor.apply();
            return;
        }

        new Api(this, this.settings).Run();
        if (this.settings.getInt("error", -1) == 0 && this.settings.getInt("api_state", 4) != 4) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    logonLoop();
                }
            }, 5000);
        }
    }
}
