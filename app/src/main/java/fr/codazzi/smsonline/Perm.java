package fr.codazzi.smsonline;

import android.Manifest;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

class Perm {

    static void loopPermissions(MainActivity ac) {
        if (!Tools.checkPermission(ac, Manifest.permission.READ_CONTACTS)) {
            Tools.getPermission(ac, Manifest.permission.READ_CONTACTS);
        }
        if (!Tools.checkPermission(ac, Manifest.permission.READ_SMS)) {
            Tools.getPermission(ac, Manifest.permission.READ_SMS);
        }
        if (!Tools.checkPermission(ac, Manifest.permission.SEND_SMS)) {
            Tools.getPermission(ac, Manifest.permission.SEND_SMS);
        }
    }

    static void askPermissions(MainActivity ac) {
        final MainActivity fac = ac;
        if (Tools.checkPermission(ac, Manifest.permission.READ_CONTACTS) &&
                Tools.checkPermission(ac, Manifest.permission.READ_SMS) &&
                Tools.checkPermission(ac, Manifest.permission.SEND_SMS)) {
            return;
        }
        new AlertDialog.Builder(ac)
                .setTitle(ac.getString(R.string.permissions_title))
                .setMessage(ac.getString(R.string.permissions_text))
                .setPositiveButton(ac.getString(R.string.permissions_ask), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Perm.loopPermissions(fac);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(ac.getString(R.string.permission_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
