package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;

import java.util.Arrays;

/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 * Created by cohen on 20/05/2016.
 */
public class PingThread
        extends ConnectionManagement
{
    private final static String TAG = "PingThread";

    private final    BluetoothSocket mSocket;
    private          ConnectedThread mmConnectedThread;
    private          TimeoutThread   mmTimeoutThread;
    public  final    Object          mMonitor;

    public PingThread(BluetoothClientServer activity,
                      BluetoothSocket socket)
    {
        super(activity);
        mSocket = socket;
        mMonitor = new Object();
    }

    @Override
    public
    void cancel()
    {
        mmConnectedThread.interrupt();
        try
        {
            mmConnectedThread.join();
        }
        catch(InterruptedException ignored)
        {
        }
        mmTimeoutThread.interrupt();
        try
        {
            mmTimeoutThread.join();
        }
        catch(InterruptedException ignored)
        {
        }
        super.cancel();
    }

    @Override
    public
    void run()
    {
        while (mNotEnd)
        {
            int    bytes  = 2 + (int) (Math.random() * 14.0);
            byte[] buffer = new byte[bytes+1];
            buffer[0] = (byte)bytes;
            for (int n = 1; n <= bytes; n++)
            {
                buffer[n] = (byte) ((Math.random() * ('z' - 'A')) + 'A');
            }

            mmConnectedThread = new ConnectedThread(mActivity,
                                                    mSocket,
                                                    Arrays.copyOfRange(buffer, 1, buffer.length),
                                                    this);
            mmTimeoutThread = new TimeoutThread(mActivity,
                                                BluetoothClientServer.PING_TIMEOUT,
                                                mmConnectedThread);

            synchronized (this)
            {
                mmConnectedThread.start();
                this.notify();
            }
            mmConnectedThread.write(buffer);

            Message msg  = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
            Bundle data = new Bundle();
            msg.arg1 = mActivity.getState();
            msg.arg2 = mActivity.setState(false);
            data.putString("reason", mResources.getString(R.string.ping_thread_send_request));
            msg.setData(data);

            // Tell main UI thread about data we sent in ping request
            msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_DATAOUT);
            data = new Bundle();
            msg.arg1 = bytes;
            data.putString("dataout", new String(Arrays.copyOfRange(buffer, 1, buffer.length)));
            msg.setData(data);

            try
            {
                mmConnectedThread.join();
            }
            catch (InterruptedException ignored)
            {
            }
            mmTimeoutThread.cancel();
            try
            {
                mmTimeoutThread.join();
            }
            catch (InterruptedException ignored)
            {
            }

            try
            {
                synchronized (this)
                {
                    wait(1000);
                }
            }
            catch (InterruptedException ignored)
            {
            }

            if (mActivity.getState() == BluetoothClientServer.PING_STATE_ALARM) {
                msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                data = new Bundle();
                msg.arg1 = mActivity.getState();
                msg.arg2 = mActivity.setState(false);
                data.putString("reason", mResources.getString(R.string.alarm_launched));
                msg.setData(data);
                mNotEnd = false;
            }
        }

        mmConnectedThread.cancel();
        mHandler.obtainMessage(BluetoothClientServer.MESSAGE_PING_THREAD_STOP).sendToTarget();
    }
}