package com.rcsnet.bluetoothmonitor;

import android.util.Log;

/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 * Created by cohen on 20/05/2016.
 */
public class TimeoutThread
        extends ConnectionManagement
{
    private static final String TAG = "TimeoutThread";
    private int    mMillis;
    private int    mNanos;
    private Thread mToTimeout;

    // Constructor with milliseconds & nanoseconds
    public TimeoutThread(BluetoothClientServer activity,
                         int millis,
                         int nanos,
                         Thread toTimeout)
    {
        super(activity);
        mMillis = millis;
        mNanos = nanos;
        mToTimeout = toTimeout;
    }

    // Milliseconds only constructor
    public TimeoutThread(BluetoothClientServer activity,
                         int millis,
                         Thread toTimeout)
    {
        this(activity, millis, 0, toTimeout);
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
                Log.v(TAG, "Waiting " + mMillis + "ms and " + mNanos + " ns before interrupting thread " + mToTimeout);
                wait(mMillis, mNanos);
                Log.v(TAG, "Timeout runs out of time: interrupting ...");
                mToTimeout.interrupt();
            }
        }
        catch (InterruptedException ignored)
        {
            Log.v(TAG, "Timeout canceled");
        }
    }
}
