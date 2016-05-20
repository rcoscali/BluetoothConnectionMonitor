package com.rcsnet.bluetoothmonitor;

import android.util.Log;

/**
 * Created by cohen on 20/05/2016.
 */
public class TimeoutThread
        extends ConnectionManagement
{
    private static final String TAG = "TimeoutThread";
    private int    mMillis;
    private Thread mToTimeout;

    public TimeoutThread(BluetoothClientServer activity,
                         int millis,
                         Thread toTimeout)
    {
        super(activity);
        mMillis = millis;
        mToTimeout = toTimeout;
    }

    @Override
    public
    void run()
    {
        Log.v(TAG, "Running TimeOut Thread");
        try
        {
            synchronized (this)
            {
                wait(mMillis);
            }
        }
        catch (InterruptedException ignored) {}
        mToTimeout.interrupt();
    }
}
