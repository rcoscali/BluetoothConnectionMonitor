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
    private final Thread mToTimeout;
    private       int    mMillis;
    private       int    mNanos;

    // Milliseconds only constructor
    public TimeoutThread(BluetoothClientServer activity,
                         int millis,
                         Thread toTimeout)
    {
        this(activity, millis, 0, toTimeout);
    }

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

    @Override
    public
    void run()
    {
        Log.v(TAG, "Running TimeOut Thread");
        try
        {
            synchronized (mToTimeout)
            {
                Log.v(TAG, "Waiting " + mMillis + "ms and " + mNanos + " ns before interrupting thread " + mToTimeout);
                wait(mMillis, mNanos);
                Log.v(TAG, "Timeout runs out of time: interrupting ...");
                //mToTimeout.interrupt();
            }
        }
        catch (InterruptedException ignored)
        {
            Log.v(TAG, "Timeout canceled");
        }
    }
}
