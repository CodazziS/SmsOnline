package fr.codazzi.smsonline;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import fr.codazzi.smsonline.sync.Synchronisation;

public class MainActivity extends AppCompatActivity {
    private String c_activity;
    private String email = "";
    private String password = "";
    private long last_sync;
    private Boolean wifi_only = true;
    private Boolean reset_api = false;
    private int error = 0;
    private int state = 0;

    private long lastPressTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        putMain();

        Intent myIntent = new Intent(this, Synchronisation.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,  0, myIntent, 0);
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 1); // first time
        long frequency = 10 * 1000; // in ms
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), frequency, pendingIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id;

        id = item.getItemId();
        /*if (id == R.id.main_settings) {
            return true;
        }*/
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
    public void loopPermissions() {
        if (!Tools.checkPermission(this, Manifest.permission.READ_CONTACTS)) {
            Tools.getPermission(this, Manifest.permission.READ_CONTACTS);
        }
        if (!Tools.checkPermission(this, Manifest.permission.READ_SMS)) {
            Tools.getPermission(this, Manifest.permission.READ_SMS);
        }
    }
    public void askPermissions() {
        final Activity ac = this;
        new AlertDialog.Builder(this)
            .setTitle("Permissions")
            .setMessage("The application need permissions for run.")
            //.setIcon(R.drawable.)
            .setPositiveButton("Ask Permissions", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (!Tools.checkPermission(ac, Manifest.permission.READ_CONTACTS)) {
                        Tools.getPermission(ac, Manifest.permission.READ_CONTACTS);
                    }
                    if (!Tools.checkPermission(ac, Manifest.permission.READ_SMS)) {
                        Tools.getPermission(ac, Manifest.permission.READ_SMS);
                    }
                    dialog.dismiss();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .show();
    }

    /* Save and get infos */
    public void saveInfos() {
        SharedPreferences settings;

        settings = this.getSharedPreferences("swb_infos", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("email", this.email);
        editor.putString("password", this.password);
        editor.putBoolean("reset_api", this.reset_api);
        editor.putBoolean("wifi_only", this.wifi_only);
        editor.apply();

        if (!Tools.checkPermission(this, Manifest.permission.READ_CONTACTS) ||
                !Tools.checkPermission(this, Manifest.permission.READ_CONTACTS)) {
            this.askPermissions();
        }
    }

    public void getInfos() {
        SharedPreferences settings;

        settings = this.getSharedPreferences("swb_infos", 0);
        this.email = settings.getString("email", "");
        this.password = settings.getString("password", "");
        this.wifi_only = settings.getBoolean("wifi_only", true);
        this.error = settings.getInt("error", 0);
        this.state = settings.getInt("state", 0);
        this.last_sync = settings.getLong("last_sync", 0);
    }

    /* Navigation */
    public void putMain() {
        TextView status;
        TextView sync;
        Toolbar toolbar;
        Date last_sync;

        c_activity = "main";
        this.getInfos();
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        status = (TextView) findViewById(R.id.status_str);

        if (error != 0) {
            if (error == -1) {
                status.setText(getResources().getStringArray(R.array.errors_str)[0]);
            } else {
                status.setText(getResources().getStringArray(R.array.errors_str)[this.error]);
            }
        } else {
            status.setText(getResources().getStringArray(R.array.actions_str)[this.state]);
        }
        sync = (TextView) findViewById(R.id.sync_str);
        if (this.last_sync != 0) {
            last_sync = new Date(this.last_sync);
            DateFormat dateFormat = DateFormat.getDateInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            sync.setText(dateFormat.format(last_sync) + " " + sdf.format(last_sync));
        } else {
            sync.setText(getResources().getString(R.string.never_sync));
        }
    }

    public void putSettings() {
        EditText email;
        EditText password;
        CheckBox wifi_only;

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        c_activity = "settings";
        email = (EditText) findViewById(R.id.config_email);
        email.setText(this.email);
        password = (EditText) findViewById(R.id.config_password);
        password.setText(this.password);
        wifi_only = (CheckBox) findViewById(R.id.config_wifi_only);
        wifi_only.setChecked(this.wifi_only);
    }

    /* On clicks */
    public void goToSettings(View view) {
        putSettings();
    }
    public void goToMain(View view) {
        putMain();
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.home_refreshed), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    public void saveSettings(View view) {
        EditText email;
        EditText password;
        CheckBox wifi_only;

        email = (EditText) findViewById(R.id.config_email);
        this.email = email.getText().toString();
        password = (EditText) findViewById(R.id.config_password);
        this.password = password.getText().toString();
        wifi_only = (CheckBox) findViewById(R.id.config_wifi_only);
        this.wifi_only = wifi_only.isChecked() || wifi_only.isActivated() || wifi_only.isFocused();
        this.reset_api = true;

        saveInfos();
        putMain();
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.home_change_saved), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    public void testSettings(View view) {

        this.email = "test@example.com";
        this.password = "azertyuiop";
        this.wifi_only = false;
        this.reset_api = true;

        saveInfos();
        putMain();
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.home_change_saved), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        if (!test_mode) {
            test_mode = true;
            testLoop();
        }
    }

    boolean test_mode = false;
    public void testLoop() {
        int time = 3000;

        Intent intent = new Intent(this, Synchronisation.class);
        getApplicationContext().sendBroadcast(intent);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                testLoop();
            }
        }, time);
    }
}
