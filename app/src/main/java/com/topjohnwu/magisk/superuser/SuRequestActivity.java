package com.topjohnwu.magisk.superuser;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.FileObserver;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.topjohnwu.magisk.Global;
import com.topjohnwu.magisk.R;
import com.topjohnwu.magisk.utils.Async;
import com.topjohnwu.magisk.utils.CallbackHandler;

import java.io.DataInputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SuRequestActivity extends AppCompatActivity implements CallbackHandler.EventListener {

    private static final int[] timeoutList = {0, -1, 10, 20, 30, 60};
    private static final int SU_PROTOCOL_PARAM_MAX = 20;
    private static final int SU_PROTOCOL_NAME_MAX = 20;
    private static final int SU_PROTOCOL_VALUE_MAX = 256;

    private static final int PROMPT = 0;
    private static final int AUTO_DENY = 1;
    private static final int AUTO_ALLOW = 2;

    @BindView(R.id.su_popup) LinearLayout suPopup;
    @BindView(R.id.timeout) Spinner timeout;
    @BindView(R.id.app_icon) ImageView appIcon;
    @BindView(R.id.app_name) TextView appNameView;
    @BindView(R.id.package_name) TextView packageNameView;
    @BindView(R.id.grant_btn) Button grant_btn;
    @BindView(R.id.deny_btn) Button deny_btn;

    private String socketPath;
    private LocalSocket socket;
    private PackageManager pm;

    private int uid;
    private Policy policy;
    private CountDownTimer timer;
    private CallbackHandler.EventListener self;
    private CallbackHandler.Event event = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        pm = getPackageManager();

        Intent intent = getIntent();
        socketPath = intent.getStringExtra("socket");
        self = this;

        new FileObserver(socketPath) {
            @Override
            public void onEvent(int fileEvent, String path) {
                if (fileEvent == FileObserver.DELETE_SELF) {
                    if (event != null)
                        event.trigger();
                    finish();
                }
            }
        }.startWatching();

        new SocketManager().exec();
    }

    void showRequest() {

        switch (Global.Configs.suResponseType) {
            case AUTO_DENY:
                handleAction(Policy.DENY, 0);
                return;
            case AUTO_ALLOW:
                handleAction(Policy.ALLOW, 0);
                return;
            case PROMPT:
            default:
        }

        setContentView(R.layout.activity_request);
        ButterKnife.bind(this);

        appIcon.setImageDrawable(policy.info.applicationInfo.loadIcon(pm));
        appNameView.setText(policy.appName);
        packageNameView.setText(policy.packageName);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.allow_timeout, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeout.setAdapter(adapter);

        timer = new CountDownTimer(Global.Configs.suRequestTimeout * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                deny_btn.setText(getString(R.string.deny_with_str, "(" + millisUntilFinished / 1000 + ")"));
            }
            @Override
            public void onFinish() {
                deny_btn.setText(getString(R.string.deny_with_str, "(0)"));
                event.trigger();
            }
        };

        grant_btn.setOnClickListener(v -> handleAction(Policy.ALLOW));
        deny_btn.setOnClickListener(v -> handleAction(Policy.DENY));
        suPopup.setOnClickListener((v) -> {
            timer.cancel();
            deny_btn.setText(getString(R.string.deny));
        });
        timeout.setOnTouchListener((v, event) -> {
            timer.cancel();
            deny_btn.setText(getString(R.string.deny));
            return false;
        });

        timer.start();
    }

    @Override
    public void onBackPressed() {
        event.trigger();
    }

    @Override
    public void onTrigger(CallbackHandler.Event event) {
        Policy policy = (Policy) event.getResult();
        String response = "socket:DENY";
        if (policy != null) {
            Global.Events.uidMap.remove(policy.uid);
            if (policy.policy == Policy.ALLOW)
                response = "socket:ALLOW";
        }
        try {
            socket.getOutputStream().write((response).getBytes());
        } catch (Exception ignored) {}
        finish();
    }

    void handleAction(int action) {
        handleAction(action, timeoutList[timeout.getSelectedItemPosition()]);
    }

    void handleAction(int action, int time) {
        policy.policy = action;
        event.trigger(policy);
        if (time >= 0) {
            policy.until = time == 0 ? 0 : (System.currentTimeMillis() / 1000 + time * 60);
            new SuDatabaseHelper(this).addPolicy(policy);
        }
    }

    private class SocketManager extends Async.NormalTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try{
                socket = new LocalSocket();
                socket.connect(new LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM));

                DataInputStream is = new DataInputStream(socket.getInputStream());

                ContentValues payload = new ContentValues();

                for (int i = 0; i < SU_PROTOCOL_PARAM_MAX; i++) {

                    int nameLen = is.readInt();
                    if (nameLen > SU_PROTOCOL_NAME_MAX)
                        throw new IllegalArgumentException("name length too long: " + nameLen);

                    byte[] nameBytes = new byte[nameLen];
                    is.readFully(nameBytes);

                    String name = new String(nameBytes);

                    if (TextUtils.equals(name, "eof"))
                        break;

                    int dataLen = is.readInt();
                    if (dataLen > SU_PROTOCOL_VALUE_MAX)
                        throw new IllegalArgumentException(name + " data length too long: " + dataLen);

                    byte[] dataBytes = new byte[dataLen];
                    is.readFully(dataBytes);

                    String data = new String(dataBytes);

                    payload.put(name, data);
                }

                if (payload.getAsInteger("uid") == null)
                    return false;
                uid = payload.getAsInteger("uid");

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                finish();
                return;
            }
            boolean showRequest = false;
            event = Global.Events.uidMap.get(uid);
            if (event == null) {
                showRequest = true;
                event = new CallbackHandler.Event() {
                    @Override
                    public void trigger(Object result) {
                        super.trigger(result);
                        CallbackHandler.unRegister(this);
                    }
                };
                Global.Events.uidMap.put(uid, event);
            }
            CallbackHandler.register(event, self);
            try {
                if (showRequest) {
                    policy = new Policy(uid, pm);
                    showRequest();
                } else {
                    finish();
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                event.trigger();
            }
        }
    }
}
