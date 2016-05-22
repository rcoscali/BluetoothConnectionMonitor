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
