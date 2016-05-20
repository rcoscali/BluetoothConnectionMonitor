package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothAdapter;
import android.content.res.Resources;
import android.os.Handler;

/**
 * Created by cohen on 20/05/2016.
 */
public abstract class ConnectionManagement
        extends Thread
{
    // Android Log TAG
    protected static         String                TAG = "ConnectionManagement";

    // Parent activity
    protected final          BluetoothClientServer mActivity;
    // Boolean flag for run loop control
    protected volatile       boolean               mNotEnd;
    // This thread
    protected final          Thread                mMe;

    protected final          BluetoothAdapter      mAdapter;
    protected final          Handler               mHandler;
    protected final          Resources             mResources;

    ConnectionManagement (BluetoothClientServer activity)
    {
        mActivity = activity;
        mHandler = activity.mHandler;
        mResources = activity.getResources();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mNotEnd = true;
        mMe = this;
    }

    @Override
    public abstract
    void run();

    public
    void cancel()
    {
        mNotEnd = false;
        interrupt();
        try
        {
            mMe.join();
        }
        catch (InterruptedException ie)
        {
        }
    }
}





