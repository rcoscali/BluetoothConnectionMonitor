package com.rcsnet.bluetoothmonitor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 */
public class BluetoothClientServer
        extends AppCompatActivity
    implements View.OnClickListener
{
    // Android log tag
    private static final String TAG = "BluetoothMonitor";

    public BluetoothClientServer ()
    {
        PING_STATE_NAMES = getResources().getStringArray(R.array.states_names);
    }

    public final static String NAME = "BluetoothMonitor";
    public final static UUID MY_UUID = UUID.fromString("86706344-b90c-478e-aa84-ec67f9631031");

    // States of the Ping state machine
    public static final int PING_STATE_INIT = 0;
    public static final int PING_STATE_CONNECTING = 1;
    public static final int PING_STATE_CONNECTED = 2;
    public static final int PING_STATE_REQUESTED = 3;
    public static final int PING_STATE_ACKNOWLEDGED = 4;
    public static final int PING_STATE_FAILURE1 = 5;
    public static final int PING_STATE_FAILURE2 = 6;
    public static final int PING_STATE_FAILURE3 = 7;
    public static final int PING_STATE_ALARM = 8;
    public static final int PING_STATE_STOPPED = 9;

    // Ping timeout (millis)
    public static final int PING_TIMEOUT = 1000;
    // Ping period (millis)
    public static final int PING_REQUEST_PERIOD = 1000;

    private int mCurPingState;

    public int getState()
    {
        return mCurPingState;
    }
    public int setState(boolean err)
    {
        int new_state = PING_STATE_STOPPED;
        switch (mCurPingState)
        {
            case PING_STATE_INIT:
                if (!err)
                    new_state = PING_STATE_CONNECTING;
                else
                    new_state = PING_STATE_STOPPED;
                break;
            case PING_STATE_CONNECTING:
                if (!err)
                    new_state = PING_STATE_CONNECTED;
                else
                    new_state = PING_STATE_STOPPED;
                break;
            case PING_STATE_CONNECTED:
                if (!err)
                    new_state = PING_STATE_REQUESTED;
                else
                    new_state = PING_STATE_FAILURE1;
                break;
            case PING_STATE_REQUESTED:
                if (!err)
                    new_state = PING_STATE_ACKNOWLEDGED;
                else
                    new_state = PING_STATE_FAILURE1;
                break;
            case PING_STATE_ACKNOWLEDGED:
                if (!err)
                    new_state = PING_STATE_REQUESTED;
                else
                    new_state = PING_STATE_FAILURE1;
                break;
            case PING_STATE_FAILURE1:
                if (!err)
                    new_state = PING_STATE_ACKNOWLEDGED;
                else
                    new_state = PING_STATE_FAILURE2;
                break;
            case PING_STATE_FAILURE2:
                if (!err)
                    new_state = PING_STATE_ACKNOWLEDGED;
                else
                    new_state = PING_STATE_FAILURE3;
                break;
            case PING_STATE_FAILURE3:
                if (!err)
                    new_state = PING_STATE_ACKNOWLEDGED;
                else
                {
                    new_state = PING_STATE_ALARM;
                    Intent intent = new Intent(
                            getApplicationContext(),
                            ConnectionLostAlarm.class);
                    startActivity(intent);
                }
                break;
            case PING_STATE_ALARM:
                new_state = PING_STATE_STOPPED;
                break;
        }
        return new_state;
    }

    public final String PING_STATE_NAMES[];

    public final static int MESSAGE_STATE_TRANSITION = 1;
    public final static int MESSAGE_WARN = 2;
    public final static int MESSAGE_DATAOUT = 3;
    public final static int MESSAGE_DATAIN = 4;
    public final static int MESSAGE_ERROR = 5;
    public final static int MESSAGE_PING_THREAD_STOP = 6;

    private SharedPreferences mSettings;

    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_TRANSITION:
                {
                    int state_from = msg.arg1;
                    int state_to = msg.arg2;
                    if (mCurPingState == state_from) {
                        mCurPingState = state_to;
                        sendStateMessage(msg.getData().getString("reason"));
                    }
                    break;
                }

                case MESSAGE_WARN:
                {
                    sendStateMessage(msg.getData().getString("msg"));
                    break;
                }

                case MESSAGE_DATAOUT:
                {
                    int outbytes = msg.arg1;
                    String outdata = msg.getData().getString("dataout");
                    sendStateMessage(String.format(getString(R.string.bytes_of_data_sent), outbytes, outdata));
                    break;
                }

                case MESSAGE_DATAIN:
                {
                    int inbytes = msg.arg1;
                    String indata = msg.getData().getString("datain");
                    sendStateMessage(String.format(getString(R.string.bytes_of_data_received), inbytes, indata));
                    break;
                }

                case MESSAGE_ERROR:
                {
                    sendErrorMessage(msg.getData().getString("msg"));
                    mConnectThread.interrupt();
                    try
                    {
                        mConnectThread.join(200);
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                    break;

                }

                case MESSAGE_PING_THREAD_STOP:
                    mClientButtonView.setEnabled(true);
                    mCurPingState = PING_STATE_INIT;
                    break;

                default:
                    sendStateMessage(String.format((String) getResources().getText(R.string.unexpected_msg_received), msg.what));
            }

        }
    };
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = false;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private View mClientButtonView;
    private EditText mStateText;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private BluetoothDevice mDevice;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private int mMsgNr;
    private boolean mLocal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bluetooth_client_server);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        mClientButtonView = findViewById(R.id.client_button);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        Intent intent = getIntent();

        mDevice = null;
        mLocal = intent.getBooleanExtra("local", false);
        if (!mLocal) {
            mDevice = intent.getParcelableExtra("device");
            TextView deviceName = (TextView) findViewById(R.id.device_name);
            if (deviceName != null)
                deviceName.setText(mDevice.getName() == null ? getResources().getString(R.string.unknown_device) : mDevice.getName());
        } else {
            TextView deviceName = (TextView) findViewById(R.id.device_name);
            if (deviceName != null)
                deviceName.setText(getResources().getString(R.string.local_server));
        }

        mStateText = (EditText) findViewById(R.id.state_text);
        mMsgNr = 0;
        mCurPingState = PING_STATE_INIT;
        sendStateMessage(R.string.starting);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //noinspection ConstantConditions
        assert (findViewById(R.id.client_button)) != null;
        //noinspection ConstantConditions
        findViewById(R.id.client_button).setOnTouchListener(mDelayHideTouchListener);
        assert findViewById(R.id.client_button) != null;
        //noinspection ConstantConditions
        findViewById(R.id.client_button).setOnClickListener(this);

        if (mLocal) {
            sendStateMessage((String) getResources().getText(R.string.launching_server));
            assert (findViewById(R.id.client_button)) != null;
            //noinspection ConstantConditions
            findViewById(R.id.client_button).setEnabled(false);
            mAcceptThread = new AcceptThread(this);
            mAcceptThread.start();
        }

        // Preferences
        mSettings = getSharedPreferences(BluetoothConnect.PREFS_NAME, 0);
        mSettings.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

            }
        });
    }

    private void sendStateMessage(int resid) {
        String msg = String.format(getResources().getString(R.string.state_message_format),
                mMsgNr++,
                PING_STATE_NAMES[mCurPingState],
                getResources().getText(resid));
        Log.v(TAG, msg);
        mStateText.append(msg);
    }

    private void sendStateMessage(String str) {
        String msg = String.format(getResources().getString(R.string.state_message_format), mMsgNr++,
                PING_STATE_NAMES[mCurPingState], str);
        Log.v(TAG, msg);
        mStateText.append(msg);
    }

    private void sendErrorMessage(String str) {
        String msg = String.format(getResources().getString(R.string.state_error_format),
                PING_STATE_NAMES[mCurPingState], str);
        Log.v(TAG, msg);
        mStateText.append(msg);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        if (AUTO_HIDE) delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (AUTO_HIDE) {
            if (mVisible)
                hide();
            else
                show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        if (AUTO_HIDE) mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        if (AUTO_HIDE) mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onClick(View v) {
        View button = findViewById(R.id.client_button);
        if (button != null)
            button.setEnabled(false);
        sendStateMessage((String) getResources().getText(R.string.launching_client));
        mConnectThread = new ConnectThread(this, mDevice);
        mConnectThread.start();
    }
}