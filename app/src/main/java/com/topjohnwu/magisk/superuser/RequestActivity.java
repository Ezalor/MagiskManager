package com.topjohnwu.magisk.superuser;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.topjohnwu.magisk.Global;

public class RequestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Global.initSuConfigs(this);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(this, SuRequestActivity.class);
        startActivity(intent);
        finish();
    }
}
