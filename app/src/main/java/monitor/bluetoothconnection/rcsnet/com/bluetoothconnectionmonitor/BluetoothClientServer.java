package monitor.bluetoothconnection.rcsnet.com.bluetoothconnectionmonitor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public
class BluetoothClientServer extends AppCompatActivity
{
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
    public static final int NR_PING_STATES = 10;

    public int mCurPingState;

    public static final String PING_STATE_NAMES[NR_PING_STATES] =
    {
        "Init",
        "Connecting",
        "Connected",
        "Requested",
        "Acknowledged",
        "First Failure",
        "Second Failure",
        "Third Failure",
        "ALARM !!!",
        "Stopped"
    }

    private final static int MESSAGE_STATE_TRANSITION = 1;
    private final static int MESSAGE_WARN = 2;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage (Message msg) {
            switch (msg.what)
            {
                case MESSAGE_STATE_TRANSITION:
                    int state_from = msg.arg1;
                    int state_to = msg.arg2;
                    if (mCurPingState == state_from)
                    {
                        mCurPingState = state_to;
                        sendStateMessage(msg.getData().getString("reason"));
                    }
                    break;

                case MESSAGE_WARN:
                    sendStateMessage(msg.getData().getString("msg"));
                    break;

                default:
                    sendStateMessage(String.format("Unexpected message received: %d\n", msg.what));
            }

        }
    };
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

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
    private final Handler  mHideHandler  = new Handler ();
    private View    mContentView;
    private EditText mStateText;
    private final Runnable mHidePart2Runnable = new Runnable ()
    {
        @SuppressLint ("InlinedApi")
        @Override
        public
        void run ()
        {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility (View.SYSTEM_UI_FLAG_LOW_PROFILE
                                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View    mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable ()
    {
        @Override
        public
        void run ()
        {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar ();
            if (actionBar != null)
            {
                actionBar.show ();
            }
            mControlsView.setVisibility (View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable ()
    {
        @Override
        public
        void run ()
        {
            hide ();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener ()
    {
        @Override
        public
        boolean onTouch (View view, MotionEvent motionEvent)
        {
            if (AUTO_HIDE)
            {
                delayedHide (AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private BluetoothDevice mDevice;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private int mMsgNr;

    @Override
    protected
    void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);

        setContentView (R.layout.activity_bluetooth_client_server);
        ActionBar actionBar = getSupportActionBar ();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled (true);
        }

        mVisible = true;
        mControlsView = findViewById (R.id.fullscreen_content_controls);
        mContentView = findViewById (R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener (new View.OnClickListener ()
        {
            @Override
            public
            void onClick (View view)
            {
                toggle ();
            }
        });
        Intent intent = getIntent ();

        mDevice = (BluetoothDevice)intent.getParcelableExtra ("device");

        TextView deviceName = (TextView)findViewById(R.id.device_name);
        deviceName.setText (mDevice.getName ());
        mStateText = (EditText)findViewById(R.id.state_text);
        mMsgNr = 0;
        mCurPingState = PING_STATE_INIT;
        sendStateMessage("Starting");

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById (R.id.client_button).setOnTouchListener (mDelayHideTouchListener);
        findViewById (R.id.server_button).setOnTouchListener (mDelayHideTouchListener);
        ((Button)findViewById (R.id.client_button)).setOnClickListener (new View.OnClickListener ()
        {
            @Override
            public
            void onClick (View v)
            {
                sendStateMessage("Starting ConnectThread");
                mConnectThread = new ConnectThread (mDevice);
                mConnectThread.start();
            }
        });
        ((Button)findViewById (R.id.server_button)).setOnClickListener (new View.OnClickListener ()
        {
            @Override
            public
            void onClick (View v)
            {
                sendStateMessage("Starting AcceptThread");
                mAcceptThread = new AcceptThread ();
                mAcceptThread.start();
            }
        });

    }

    private void sendStateMessage(String str)
    {
        String msg = String.format("[%d] <%s> %s\n", mMsgNr, PING_STATE_NAMES[mCurPingState], str);
        mStateText.append(msg);
    }

    @Override
    protected
    void onPostCreate (Bundle savedInstanceState)
    {
        super.onPostCreate (savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide (100);
    }

    @Override
    public
    boolean onOptionsItemSelected (MenuItem item)
    {
        int id = item.getItemId ();
        if (id == android.R.id.home)
        {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask (this);
            return true;
        }
        return super.onOptionsItemSelected (item);
    }

    private
    void toggle ()
    {
        if (mVisible)
        {
            hide ();
        }
        else
        {
            show ();
        }
    }

    private
    void hide ()
    {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar ();
        if (actionBar != null)
        {
            actionBar.hide ();
        }
        mControlsView.setVisibility (View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks (mShowPart2Runnable);
        mHideHandler.postDelayed (mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint ("InlinedApi")
    private
    void show ()
    {
        // Show the system bar
        mContentView.setSystemUiVisibility (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks (mHidePart2Runnable);
        mHideHandler.postDelayed (mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private
    void delayedHide (int delayMillis)
    {
        mHideHandler.removeCallbacks (mHideRunnable);
        mHideHandler.postDelayed (mHideRunnable, delayMillis);
    }

    private final static String NAME = "BluetoothMonitor";
    private final static UUID MY_UUID = UUID.fromString ("86706344-b90c-478e-aa84-ec67f9631031");

    public class AcceptThread extends Thread {
        private final PingThread mmPingThread;
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = BluetoothAdapter.getDefaultAdapter ().listenUsingRfcommWithServiceRecord (NAME, MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            // Warn UI thread
            Message msg = mHandler.obtainMessage(MESSAGE_WARN);
            msg.setData(new Bundle().putString("msg", "Accept Thread Started"));
            msg.sendToTarget();

            // Start real work
            BluetoothSocket socket = null;
            while (true)
            {
                try
                {
                    msg = mHandler.obtainMessage(MESSAGE_WARN);
                    msg.setData(new Bundle().putString("msg", "Accept Thread: Accepting data ..."));
                    msg.sendToTarget();
                    socket = mmServerSocket.accept();
                    msg = mHandler.obtainMessage(MESSAGE_WARN);
                    msg.setData(new Bundle().putString("msg", "Accept Thread: Data received ..."));
                    msg.sendToTarget();
                }
                catch (IOException e)
                {
                    msg = mHandler.obtainMessage(MESSAGE_WARN);
                    msg.setData(new Bundle().putString("msg", "Accept Thread: I/O exception occured ..."));
                    msg.sendToTarget();
                    break;
                }

                if (socket != null) {
                    ConnectedThread connectedThread = new ConnectedThread (socket);
                    connectedThread.start();
                    mmPingThread = new PingThread(connectedThread);
                    mmPingThread.start();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Warn UI thread
            Message msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
            msg.arg1 = mCurPingState;
            msg.arg2 = PING_STATE_CONNECTING;
            msg.setData(new Bundle().putString("raeson", "Connect Thread Started"));
            msg.sendToTarget();

            // Start real work
            BluetoothAdapter.getDefaultAdapter ().cancelDiscovery();
            try {
                msg = mHandler.obtainMessage(MESSAGE_WARN);
                msg.setData(new Bundle().putString("msg", "Connect Thread: connecting"));
                msg.sendToTarget();
                mmSocket.connect();
                msg.setData(new Bundle().putString("msg", "Connect Thread: connection succeed"));
                msg.sendToTarget();
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                    msg = mHandler.obtainMessage(MESSAGE_WARN);
                    msg.setData(new Bundle().putString("msg", "Connect Thread: I/O exception occured"));
                    msg.sendToTarget();
                }
                catch (IOException closeException) { }
                return;
            }
            ConnectedThread connectedThread = ConnectedThread(mmSocket);
            connectedThread.start();
        }

        public void cancel() {
            try {
                mmSocket.close();
            }
            catch (IOException e) { }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream     mmInStream;
        private final OutputStream    mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            // Warn UI thread
            Message msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
            msg.arg1 = mCurPingState;
            msg.arg2 = PING_STATE_CONNECTED;
            msg.setData(new Bundle().putString("raeson", "Connected Thread Started"));
            msg.sendToTarget();

            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    msg = mHandler.obtainMessage(MESSAGE_WARN);
                    msg.setData(new Bundle().putString("msg", "Connected Thread: reading data ..."));
                    msg.sendToTarget();
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Check data are the ones expected
                    if (buffer.equals(mSentBuffer) && mSentBuffer.length == bytes) {
                        msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
                        msg.arg1 = mCurPingState;
                        msg.arg2 = PING_STATE_ACKNOWLEDGED;
                        msg.setData(new Bundle().putString("reason", "Connected Thread: reading data ..."));
                        msg.sendToTarget();
                    }
                    else
                    {
                        msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
                        msg.arg1 = mCurPingState;
                        switch (mCurPingState)
                        {
                            case PING_STATE_REQUESTED:
                                msg.arg2 = PING_STATE_FAILURE1;
                                break;
                            case PING_STATE_FAILURE1:
                                msg.arg2 = PING_STATE_FAILURE2;
                                break;
                            case PING_STATE_FAILURE2:
                                msg.arg2 = PING_STATE_FAILURE3;
                                break;
                            case PING_STATE_FAILURE3:
                                msg.arg2 = PING_STATE_ALARM;
                                break;
                            default:
                                msg.arg2 = PING_STATE_ALARM;
                                break;
                        }
                        msg.setData(new Bundle().putString("reason", "Connected Thread: Unexpected data read ..."));
                        msg.sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes)
        {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel()
        {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class PingThread extends Thread
    {
        private final ConnectedThread mmConnectedThread;

        public PingThread(ConnectedThread connectedThread)
        {
            mmConnectedThread = connectedThread;
        }

        public void run()
        {
            Message msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
            msg.arg1 = mCurPingState;
            msg.arg2 = PING_STATE_REQUESTED
        }
    }
}
