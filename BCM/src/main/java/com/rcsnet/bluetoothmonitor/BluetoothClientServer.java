/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 */
package com.rcsnet.bluetoothmonitor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Scroller;
import android.widget.TextView;

import java.util.UUID;

/**
 * Activity driving client/server
 */
public class BluetoothClientServer
        extends AppCompatActivity
    implements View.OnClickListener
{
    // Name of app
    public final static  String  NAME                             = "BluetoothMonitor";

    // Universal Unique ID for the service we connect to bluetooth devices
    public final static UUID MY_UUID = UUID.fromString("86706344-b90c-478e-aa84-ec67f9631031");

    // States of the Ping state machine
    public static final  int     PING_STATE_NONE                  = -1;
    public static final  int     PING_STATE_INIT                  = 0;
    public static final  int     PING_STATE_CONNECTING            = 1;
    public static final  int     PING_STATE_CONNECTED             = 2;
    public static final  int     PING_STATE_REQUESTED             = 3;
    public static final  int     PING_STATE_ACKNOWLEDGED          = 4;
    public static final  int     PING_STATE_FAILURE               = 5;
    public static final  int     PING_STATE_ALARM                 = 6;
    public static final  int     PING_STATE_STOPPED               = 7;
    public static final  int     PING_STATE_LISTENING             = 8;
    public static final  int     PING_STATES_NUMBER               = 9;

    // Id of mode in which the threads are launched
    public static final  int     MODE_CLIENT                      = 1;
    public static final  int     MODE_SERVER                      = 2;

    // Handler messages ids
    public final static  int     MESSAGE_STATE_TRANSITION         = 1;
    public final static  int     MESSAGE_WARN                     = 2;
    public final static  int     MESSAGE_DATAOUT                  = 3;
    public final static  int     MESSAGE_DATAIN                   = 4;
    public final static  int     MESSAGE_ERROR                    = 5;
    public final static  int     MESSAGE_PING_THREAD_STOP         = 6;

    // Android log tag
    private static final String  TAG                              = "BluetoothMonitor";

    // Preferences default values
    private static final String  PING_FREQUENCY_DEFAULT           = "1000";
    private static final String  PING_TIMEOUT_DEFAULT             = "1000";
    private static final String  PING_FAILURE_NUMBER_DEFAULT      = "3";
    private static final int     PING_REQUEST_MIN_CHAR_NR_DEFAULT = 2;
    private static final int     PING_REQUEST_MAX_CHAR_NR_DEFAULT = 16;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE                        = false;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int     AUTO_HIDE_DELAY_MILLIS           = 3000;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int     UI_ANIMATION_DELAY               = 300;
    private final        Handler mHideHandler                     = new Handler();
    public  String            PING_STATE_NAMES[];
    public boolean mEnforceStatesChanges = false;
    private int               mPingTimeout;
    private int               mPingRequestMinCharNr;
    private int               mPingRequestMaxCharNr;
    private int               mPingFrequency;
    private int               mPingFailureNumber;
    private int               mFailureNumber;
    private int               mMode;
    private int               mCurPingState;
    private SharedPreferences mSettings;
    private View              mContentView;
    private final Runnable mHidePart2Runnable = new Runnable()
    {
        @SuppressLint("InlinedApi")
        @Override
        public void run()
        {
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
    private View     mClientButtonView;
    private EditText mStateText;
    private View     mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable()
    {
        @Override
        public void run()
        {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
            {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable             mHideRunnable           = new Runnable()
    {
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
    private final View.OnTouchListener
                                       mDelayHideTouchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            if (AUTO_HIDE)
            {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private BluetoothDevice mDevice;
    private AcceptThread    mAcceptThread;
    private ConnectThread   mConnectThread;
    private int             mMsgNr;
    public Handler mHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case MESSAGE_STATE_TRANSITION:
            {
                int state_from = msg.arg1;
                int state_to   = msg.arg2;
                if (mCurPingState == state_from)
                {
                    mCurPingState = state_to;
                    postStateMessage(msg.getData().getString("reason"));
                }
                break;
            }

            case MESSAGE_WARN:
            {
                postStateMessage(msg.getData().getString("msg"));
                break;
            }

            case MESSAGE_DATAOUT:
            {
                int    outbytes = msg.arg1;
                String outdata  = msg.getData().getString("dataout");
                postStateMessage(String.format(getString(R.string.bytes_of_data_sent), outbytes, outdata));
                break;
            }

            case MESSAGE_DATAIN:
            {
                int    inbytes = msg.arg1;
                String indata  = msg.getData().getString("datain");
                postStateMessage(String.format(getString(R.string.bytes_of_data_received), inbytes, indata));
                break;
            }

            case MESSAGE_ERROR:
            {
                postErrorMessage(msg.getData().getString("msg"));
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
                postStateMessage(String.format((String) getResources().getText(R.string.unexpected_msg_received), msg.what));
            }

        }
    };
    private boolean mLocal;
    private SharedPreferences.OnSharedPreferenceChangeListener mSettingsChangeLsnr =
            new SharedPreferences.OnSharedPreferenceChangeListener()
            {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s)
                {
                    switch (s)
                    {
                    // Preference for control of ping frequency (actually period)
                    case "ping_frequency":
                        String ping_frequency_str = sharedPreferences.getString("ping_frequency", PING_FREQUENCY_DEFAULT);
                        mPingFrequency = Integer.getInteger(ping_frequency_str);
                        break;
                    // Preference for controling ping timeout
                    case "ping_timeout":
                        String ping_timeout_str = sharedPreferences.getString("ping_timeout", PING_TIMEOUT_DEFAULT);
                        mPingTimeout = Integer.getInteger(ping_timeout_str);
                        break;
                    // Preference for setting number of failure before raising alarm
                    case "ping_failure_number":
                        String ping_failure_number_str = sharedPreferences.getString("ping_failure_number",
                                                                                     PING_FAILURE_NUMBER_DEFAULT);
                        mPingFailureNumber = Integer.getInteger(ping_failure_number_str);
                        break;
                    // Preference controlling Minimum number of bytes in ping request
                    case "ping_request_min_char_number":
                        mPingRequestMinCharNr = sharedPreferences.getInt("ping_request_min_char_number",
                                                                         PING_REQUEST_MIN_CHAR_NR_DEFAULT);
                        break;
                    // Preference for controlling maximum number of bytes in ping request
                    case "ping_request_max_char_number":
                        mPingRequestMaxCharNr = sharedPreferences.getInt("ping_request_max_char_number",
                                                                         PING_REQUEST_MAX_CHAR_NR_DEFAULT);
                        break;
                    }
                }
            };

    public BluetoothClientServer()
    {
    }

    public int getPingTimeout()
    {
        return mPingTimeout;
    }

    public int getPingFrequency()
    {
        return mPingFrequency;
    }

    public int getPingFailureNumber()
    {
        return mPingFailureNumber;
    }

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
            {
                if (mMode == MODE_SERVER)
                    new_state = PING_STATE_LISTENING;
                else
                    new_state = PING_STATE_CONNECTING;
            }
            else
                new_state = PING_STATE_STOPPED;
            break;
        case PING_STATE_LISTENING:
            if (!err)
                new_state = PING_STATE_ACKNOWLEDGED;
            else
                new_state = PING_STATE_FAILURE;
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
                new_state = PING_STATE_FAILURE;
            break;
        case PING_STATE_REQUESTED:
            if (!err)
                new_state = PING_STATE_ACKNOWLEDGED;
            else
            {
                new_state = PING_STATE_FAILURE;
                if (mFailureNumber == mPingFailureNumber)
                {
                    new_state = PING_STATE_ALARM;
                    Intent intent = new Intent(
                            getApplicationContext(),
                            ConnectionLostAlarm.class);
                    startActivity(intent);
                }
                else
                    mFailureNumber++;
            }
            break;
        case PING_STATE_ACKNOWLEDGED:
            if (!err)
            {
                if (mMode == MODE_SERVER)
                    new_state = PING_STATE_LISTENING;
                else
                    new_state = PING_STATE_REQUESTED;
            }
            else
            {
                new_state = PING_STATE_FAILURE;
                if (mFailureNumber == mPingFailureNumber)
                {
                    new_state = PING_STATE_ALARM;
                    Intent intent = new Intent(
                            getApplicationContext(),
                            ConnectionLostAlarm.class);
                    startActivity(intent);
                }
                else
                    mFailureNumber++;
            }
            break;
        case PING_STATE_FAILURE:
            if (!err)
            {
                new_state = PING_STATE_ACKNOWLEDGED;
                mFailureNumber = 0;
            }
            else
            {
                new_state = PING_STATE_FAILURE;
                if (mFailureNumber == mPingFailureNumber)
                {
                    new_state = PING_STATE_ALARM;
                    Intent intent = new Intent(
                            getApplicationContext(),
                            ConnectionLostAlarm.class);
                    startActivity(intent);
                }
                else
                    mFailureNumber++;
            }
            break;
        case PING_STATE_ALARM:
        case PING_STATE_STOPPED:
            new_state = PING_STATE_STOPPED;
            break;
        }
        return new_state;
    }

    /**
     * Return the state name for the provided ID
     *
     * @param state The ID of the state for which the name is wanted
     * @return A string containing the name of the state with id state
     * @throws IndexOutOfBoundsException When state is either less than 0 or higher than 9
     */
    public String getStateName(int state)
            throws IndexOutOfBoundsException
    {
        if (state < 0 || state >= PING_STATES_NUMBER)
            throw new IndexOutOfBoundsException("Invalid state value");
        return PING_STATE_NAMES[state];
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PING_STATE_NAMES = getResources().getStringArray(R.array.states_names);

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
            mMode = MODE_CLIENT;
            mDevice = intent.getParcelableExtra("device");
            TextView deviceName = (TextView) findViewById(R.id.device_name);
            if (deviceName != null)
                deviceName.setText(mDevice.getName() == null ? getResources().getString(R.string.unknown_device) : mDevice.getName());
        } else {
            mMode = MODE_SERVER;
            TextView deviceName = (TextView) findViewById(R.id.device_name);
            if (deviceName != null)
                deviceName.setText(getResources().getString(R.string.local_server));
        }

        mStateText = (EditText) findViewById(R.id.state_text);
        mStateText.setScroller(new Scroller(getApplicationContext()));
        mStateText.setVerticalScrollBarEnabled(true);
        mStateText.setMovementMethod(new ScrollingMovementMethod());
        mStateText.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public
            boolean onTouch(View v, MotionEvent event)
            {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        mMsgNr = 0;
        mCurPingState = PING_STATE_INIT;
        postStateMessage(R.string.starting);

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
            postStateMessage((String) getResources().getText(R.string.launching_server));
            assert (findViewById(R.id.client_button)) != null;
            //noinspection ConstantConditions
            findViewById(R.id.client_button).setEnabled(false);
            mAcceptThread = new AcceptThread(this);
            mAcceptThread.start();
        }

        // Preferences
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        //mSettings = getSharedPreferences(BluetoothConnect.PREFS_NAME, MODE_PRIVATE);
        mPingFrequency = Integer.parseInt(mSettings.getString("ping_frequency", "1000"));
        mPingTimeout = Integer.parseInt(mSettings.getString("ping_timeout", "1000"));
        mPingFailureNumber = Integer.parseInt(mSettings.getString("ping_failure_number", "3"));
        mPingRequestMinCharNr = Integer.parseInt(mSettings.getString("ping_request_min_char_number",
                                                                     String.format("%d", PING_REQUEST_MIN_CHAR_NR_DEFAULT)));
        mPingRequestMaxCharNr = Integer.parseInt(mSettings.getString("ping_request_max_char_number",
                                                                     String.format("%d", PING_REQUEST_MAX_CHAR_NR_DEFAULT)));
        mEnforceStatesChanges = mSettings.getBoolean("pref_enforce_state_changes", false);
        mSettings.registerOnSharedPreferenceChangeListener(mSettingsChangeLsnr);
    }

    /**
     * Send a message display request to the UI main thread (main application thread)
     * Takes as a parameter the resource id of the string to display
     *
     * @param resid Identifier of the string to display as message
     */
    private void postStateMessage(int resid)
    {
        String msg;
        if (mCurPingState != PING_STATE_FAILURE)
            msg = String.format(getResources().getString(R.string.state_message_format),
                                mMsgNr++,
                                PING_STATE_NAMES[mCurPingState],
                                getResources().getText(resid));
        else
        {
            String failure = String.format("%s %d", PING_STATE_NAMES[mCurPingState], mFailureNumber);
            msg = String.format(getResources().getString(R.string.state_message_format),
                                mMsgNr++,
                                failure,
                                getResources().getText(resid));
        }
        Log.v(TAG, msg);
        mStateText.append(msg);
    }

    /**
     * Send a message display request to the UI main thread (main application thread)
     * Takes as a parameter the string to display
     *
     * @param str String to display
     */
    private void postStateMessage(String str)
    {
        String msg;
        if (mCurPingState != PING_STATE_FAILURE)
            msg = String.format(getResources().getString(R.string.state_message_format), mMsgNr++,
                                PING_STATE_NAMES[mCurPingState], str);
        else
        {
            String failure = String.format("%s %d", PING_STATE_NAMES[mCurPingState], mFailureNumber);
            msg = String.format(getResources().getString(R.string.state_message_format), mMsgNr++,
                                failure, str);
        }
        Log.v(TAG, msg);
        mStateText.append(msg);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        if (AUTO_HIDE) delayedHide(100);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis)
    {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void toggle()
    {
        if (AUTO_HIDE)
        {
            if (mVisible)
                hide();
            else
                show();
        }
    }

    private void hide()
    {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        if (AUTO_HIDE) mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show()
    {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                           | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        if (AUTO_HIDE) mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mSettings.unregisterOnSharedPreferenceChangeListener(mSettingsChangeLsnr);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mSettings.registerOnSharedPreferenceChangeListener(mSettingsChangeLsnr);
    }

    private void postErrorMessage(String str)
    {
        String msg;
        if (mCurPingState != PING_STATE_FAILURE)
            msg = String.format(getResources().getString(R.string.state_error_format),
                                PING_STATE_NAMES[mCurPingState], str);
        else
        {
            String failure = String.format("%s %d", PING_STATE_NAMES[mCurPingState], mFailureNumber);
            msg = String.format(getResources().getString(R.string.state_error_format),
                                failure, str);
        }
        Log.v(TAG, msg);
        mStateText.append(msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        View button = findViewById(R.id.client_button);
        if (button != null)
            button.setEnabled(false);
        postStateMessage((String) getResources().getText(R.string.launching_client));
        mConnectThread = new ConnectThread(this, mDevice);
        mConnectThread.setPingRequestMinSize(mPingRequestMinCharNr);
        mConnectThread.setPingRequestMaxSize(mPingRequestMaxCharNr);
        mConnectThread.start();
    }
}