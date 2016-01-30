package fr.codazzi.smsonline;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import fr.codazzi.smsonline.synchronisation.Synchronisation;

public class MainActivity extends AppCompatActivity {
    private String c_activity;
    private String email = "";
    private String password = "";

    private long lastPressTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getInfos();

        super.onCreate(savedInstanceState);
        putMain();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (c_activity == "main") {
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

    /* Save and get infos */
    public void saveInfos() {
        SharedPreferences settings = this.getSharedPreferences("swb_infos", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("email", this.email);
        editor.putString("password", this.password);
        editor.apply();
    }

    public void getInfos() {
        SharedPreferences settings = this.getSharedPreferences("swb_infos", 0);
        this.email = settings.getString("email", "");
        this.password = settings.getString("password", "");
    }

    /* Navigation */
    public void putMain() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        c_activity = "main";

        startService(new Intent(MainActivity.this, Synchronisation.class));
    }

    public void putSettings() {
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        c_activity = "settings";
        EditText email = (EditText) findViewById(R.id.config_email);
        email.setText(this.email);

        EditText password = (EditText) findViewById(R.id.config_password);
        password.setText(this.password);
    }

    /* On clicks */
    public void goToSettings(View view) {
        putSettings();
    }
    public void saveSettings(View view) {
        EditText email = (EditText) findViewById(R.id.config_email);
        this.email = email.getText().toString();

        EditText password = (EditText) findViewById(R.id.config_password);
        this.password = password.getText().toString();

        saveInfos();
        putMain();

    }
}
