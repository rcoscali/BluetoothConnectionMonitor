package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothAdapter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Arrays;

/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
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
        mMe.interrupt();
        try
        {
            mMe.join();
        }
        catch (InterruptedException ignored)
        {
            Log.v(TAG, "Thread canceled !!");
        }
    }

    public
    void sendTransition(int msgResId, boolean err)
    {
        Message msg  = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
        Bundle  data = new Bundle();
        msg.arg1 = mActivity.getState();
        msg.arg2 = mActivity.setState(err);
        data.putString("reason", mResources.getString(msgResId));
        msg.setData(data);
        msg.sendToTarget();
    }

    public
    void sendMessage(int msgResId)
    {
        Message msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_WARN);
        Bundle data = new Bundle();
        data.putString("msg", mResources.getString(msgResId));
        msg.setData(data);
        msg.sendToTarget();
    }

    public
    void sendError(int msgResId)
    {
        Message msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_ERROR);
        Bundle data = new Bundle();
        data.putString("msg", mResources.getString(msgResId));
        msg.setData(data);
        msg.sendToTarget();
    }

    public
    void sendDataInOutMessage(boolean in, byte[] buffer, int bytes)
    {
        Message msg = mHandler.obtainMessage(in ? BluetoothClientServer.MESSAGE_DATAIN : BluetoothClientServer.MESSAGE_DATAOUT);
        Bundle data = new Bundle();
        msg.arg1 = bytes;
        data.putString(in ? "datain" : "dataout", new String(Arrays.copyOf(buffer, bytes)));
        msg.setData(data);
        msg.sendToTarget();
    }
}





