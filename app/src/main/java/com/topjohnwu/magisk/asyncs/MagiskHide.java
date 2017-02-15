package com.topjohnwu.magisk.asyncs;

import android.app.Activity;

import com.topjohnwu.magisk.MagiskManager;
import com.topjohnwu.magisk.utils.Shell;

import java.util.List;

public class MagiskHide extends SerialTask<Object, Void, Void> {

    private boolean isList = false;

    public MagiskHide() {}

    public MagiskHide(Activity context) {
        super(context);
    }

    @Override
    protected Void doInBackground(Object... params) {
        String command = (String) params[0];
        List<String> ret = Shell.su(MagiskManager.MAGISK_HIDE_PATH + command);
        if (isList)
            magiskManager.magiskHideList = ret;
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        if (isList)
            magiskManager.magiskHideDone.trigger();
    }

    public void add(CharSequence packageName) {
        exec("add " + packageName);
    }

    public void rm(CharSequence packageName) {
        exec("rm " + packageName);
    }

    public void enable() {
        exec("enable; setprop persist.magisk.hide 1");
    }

    public void disable() {
        exec("disable; setprop persist.magisk.hide 0");
    }

    public void list() {
        isList = true;
        if (magiskManager == null)
            return;
        exec("list");
    }

}
