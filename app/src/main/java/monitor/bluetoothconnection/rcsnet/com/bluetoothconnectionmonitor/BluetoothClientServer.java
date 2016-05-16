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
import java.util.regex.Matcher;

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

    // Ping timeout (millis)
    public static final int PING_TIMEOUT = 1000;
    // Ping period (millis)
    public static final int PING_REQUEST_PERIOD = 1000;

    public int mCurPingState;

    public static final String PING_STATE_NAMES[] =
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
    };

    private final static int MESSAGE_STATE_TRANSITION = 1;
    private final static int MESSAGE_WARN = 2;
    private final static int MESSAGE_DATAOUT = 3;
    private final static int MESSAGE_DATAIN = 4;
    private final static int MESSAGE_ERROR = 5;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage (Message msg) {
            switch (msg.what)
            {
                case MESSAGE_STATE_TRANSITION: {
                    int state_from = msg.arg1;
                    int state_to = msg.arg2;
                    if (mCurPingState == state_from) {
                        mCurPingState = state_to;
                        sendStateMessage(msg.getData().getString("reason"));
                    }
                    break;
                }
                case MESSAGE_WARN: {
                    sendStateMessage(msg.getData().getString("msg"));
                    break;
                }
                case MESSAGE_DATAOUT: {
                    int outbytes = msg.arg1;
                    String outdata = msg.getData().getString("dataout");
                    sendStateMessage(String.format("%d bytes of data sent: '%s'", outbytes, outdata));
                    break;
                }
                case MESSAGE_DATAIN: {
                    int inbytes = msg.arg1;
                    String indata = msg.getData().getString("datain");
                    sendStateMessage(String.format("%d bytes of data received: '%s'", inbytes, indata));
                    break;
                }
                case MESSAGE_ERROR: {
                    sendStateMessage(msg.getData().getString("msg"));
                    mConnectThread.interrupt();
                    try {
                        mConnectThread.join(200);
                    } catch (InterruptedException e) {
                    }
                    mAcceptThread.interrupt();
                    try {
                        mAcceptThread.join(200);
                    } catch (InterruptedException e) {
                    }
                    break;
                }
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
            Bundle data = new Bundle();
            data.putString("msg", "Accept Thread Started");
            msg.setData(data);
            msg.sendToTarget();

            // Start real work
            BluetoothSocket socket = null;
            while (true)
            {
                try
                {
                    // Signal UI thread we are about to accept incoming data
                    msg = mHandler.obtainMessage(MESSAGE_WARN);
                    data = new Bundle();
                    data.putString("msg", "Accept Thread: Accepting data ...");
                    msg.setData(data);
                    msg.sendToTarget();

                    // Wait for incoming data ...
                    socket = mmServerSocket.accept();

                    // Some data arrived, tell UI
                    msg = mHandler.obtainMessage(MESSAGE_WARN);
                    data = new Bundle();
                    data.putString("msg", "Accept Thread: Data received ...");
                    msg.setData(data);
                    msg.sendToTarget();
                }
                catch (IOException e)
                {
                    msg = mHandler.obtainMessage(MESSAGE_WARN);
                    data = new Bundle();
                    data.putString("msg", "Accept Thread: I/O exception occured ...");
                    msg.setData(data);
                    msg.sendToTarget();
                    break;
                }

                if (socket != null) {
                    ConnectedThread connectedThread = new ConnectedThread(socket, null);
                    connectedThread.start();
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
        private PingThread mmPingThread;

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
            Bundle data = new Bundle();
            msg.arg1 = mCurPingState;
            msg.arg2 = PING_STATE_CONNECTING;
            data.putString("raeson", "Connect Thread Started");
            msg.setData(data);
            msg.sendToTarget();

            // Start real work
            BluetoothAdapter.getDefaultAdapter ().cancelDiscovery();
            try
            {
                // Signal UI thread we are connecting
                msg = mHandler.obtainMessage(MESSAGE_WARN);
                data = new Bundle();
                data.putString("msg", "Connect Thread: connecting");
                msg.setData(data);
                msg.sendToTarget();

                // Connection on going
                mmSocket.connect();

                // Connection succeed, signal UI thread
                data = new Bundle();
                data.putString("msg", "Connect Thread: connection succeed");
                msg.setData(data);
                msg.sendToTarget();

                // and now, send ping request
                mmPingThread = new PingThread(mmSocket);
                mmPingThread.start();
            }
            catch (IOException connectException) {
                try {
                    // Signal UI thread we are not able to connect
                    msg = mHandler.obtainMessage(MESSAGE_ERROR);
                    data = new Bundle();
                    data.putString("msg", "Connect Thread: I/O exception occured");
                    msg.setData(data);
                    msg.sendToTarget();
                    // Then try to close socket
                    mmSocket.close();
                }
                catch (IOException closeException)
                { }
            }
        }

        public void cancel() {
            mmPingThread.cancel();
            try {
                mmPingThread.join();
            } catch (InterruptedException e) {
            }
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
        private final byte[] mSentBuffer;

        public ConnectedThread(BluetoothSocket socket, byte[] sentBuffer) {
            mmSocket = socket;
            mSentBuffer = sentBuffer;
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

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            mSentBuffer = null;
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
            Bundle data = new Bundle();
            msg.arg1 = mCurPingState;
            msg.arg2 = PING_STATE_CONNECTED;
            data.putString("raeson", "Connected Thread Started");
            msg.setData(data);
            msg.sendToTarget();

            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
                try {

                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Tell UI about data received
                    msg = mHandler.obtainMessage(MESSAGE_DATAIN);
                    data = new Bundle();
                    msg.arg1 = bytes;
                    data.putString("datain", new String(buffer));
                    msg.setData(data);
                    msg.sendToTarget();


                    // Check data are the ones expected
                    // when we are in the context of a client
                    if (mSentBuffer != null) {
                        if (buffer.equals(mSentBuffer) && mSentBuffer.length == bytes) {
                            msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
                            data = new Bundle();
                            msg.arg1 = mCurPingState;
                            msg.arg2 = PING_STATE_ACKNOWLEDGED;
                            data.putString("reason", "Connected Thread: reading data ...");
                            msg.setData(data);
                            msg.sendToTarget();
                        } else {
                            msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
                            data = new Bundle();
                            msg.arg1 = mCurPingState;
                            switch (mCurPingState) {
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
                            data.putString("reason", "Connected Thread: Unexpected data read ...");
                            msg.setData(data);
                            msg.sendToTarget();
                        }
                    }
                    // We are a server, just send back the data as a ping response
                    else
                    {
                        try {
                            mmOutStream.write(buffer);
                        } catch (IOException e) {}
                    }
                } catch (IOException e)
                {
                    msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
                    msg.arg1 = mCurPingState;
                    switch (mCurPingState) {
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
                    data.putString("reason", "Connected Thread: I/O exception while reading ...");
                    msg.setData(data);
                    msg.sendToTarget();
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

    private class TimeoutThread extends Thread
    {
        private int mMillis;
        private Thread mToTimeout;

        public TimeoutThread(int millis, Thread toTimeout)
        {
            mMillis = millis;
            mToTimeout = toTimeout;
        }

        public void run()
        {
            try {
                wait(mMillis);
            }
            catch(InterruptedException ie) {}
            mToTimeout.interrupt();
        }
    }

    private class PingThread extends Thread
    {
        private final BluetoothSocket mSocket;
        private ConnectedThread mmConnectedThread;
        private TimeoutThread mmTimeoutThread;
        private volatile boolean mNotEnd;

        public PingThread(BluetoothSocket socket)
        {
            mSocket = socket;
            mNotEnd = true;
        }

        public void cancel()
        {
            mNotEnd = false;
            mmConnectedThread.interrupt();
            interrupt();
        }

        public void run()
        {
            while (mNotEnd) {
                try {
                    Message msg = mHandler.obtainMessage(MESSAGE_STATE_TRANSITION);
                    Bundle data = new Bundle();
                    msg.arg1 = mCurPingState;
                    msg.arg2 = PING_STATE_REQUESTED;
                    data.putString("reason", "Connected Thread: sending ping data");
                    msg.setData(data);

                    int bytes = 2 + (int) (Math.random() * 14.0);
                    byte[] buffer = new byte[bytes];
                    for (int n = 0; n < bytes; n++) {
                        buffer[n] = (byte) ((Math.random() * ('z' - 'A')) + 'A');
                    }

                    mmConnectedThread = new ConnectedThread(mSocket, buffer);
                    mmTimeoutThread = new TimeoutThread(PING_TIMEOUT, mmConnectedThread);
                    mmConnectedThread.start();

                    mmConnectedThread.write(buffer);

                    // Tell main UI thread about data we sent in ping request
                    msg = mHandler.obtainMessage(MESSAGE_DATAOUT);
                    data = new Bundle();
                    msg.arg1 = bytes;
                    data.putString("dataout", new String(buffer));
                    msg.setData(data);

                    // Start timeout thread
                    mmTimeoutThread.start();

                    // Then wait either response received or timeout ends
                    // This is handled in ConnectedThread
                    try {
                        mmConnectedThread.join();
                    } catch (InterruptedException ie) {
                    }
                    try {
                        mmTimeoutThread.join();
                    } catch (InterruptedException ie) {
                    }

                    // Then wait for next ping request
                    wait(PING_REQUEST_PERIOD);
                } catch (InterruptedException ie) {}
            }

            mmConnectedThread.cancel();
        }
    }
}
