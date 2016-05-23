package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;

import java.io.IOException;

/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 * Created by cohen on 20/05/2016.
 */
public class AcceptThread
        extends ConnectionManagement
{
    // Server for the bluetooth socket
    private final BluetoothServerSocket mmServerSocket;

    public AcceptThread(BluetoothClientServer activity)
    {
        super(activity);
        BluetoothServerSocket tmp = null;
        try
        {
            tmp = mAdapter.listenUsingRfcommWithServiceRecord(BluetoothClientServer.NAME,
                                                              BluetoothClientServer.MY_UUID);
        }
        catch (IOException ignored) { }
        mmServerSocket = tmp;
    }

    @Override
    public void run()
    {
        // Starting timeout
        mTimeoutThread.start();

        // Warn UI thread
        Message msg  = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_WARN);
        Bundle data = new Bundle();
        data.putString("msg", mResources.getString(R.string.accept_thread_started));
        msg.setData(data);
        msg.sendToTarget();

        // Start real work
        BluetoothSocket socket;
        while (mNotEnd)
        {
            try
            {
                msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                data = new Bundle();
                msg.arg1 = mActivity.getState();
                msg.arg2 = mActivity.setState(false);
                data.putString("reason", mResources.getString(R.string.accept_thread_accepting_data));
                msg.setData(data);
                msg.sendToTarget();

                // Wait for incoming data ...
                socket = mmServerSocket.accept();

                // Some data arrived, tell UI
                msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_WARN);
                data = new Bundle();
                data.putString("msg", mResources.getString(R.string.accept_thread_data_received));
                msg.setData(data);
                msg.sendToTarget();
            }
            catch (IOException e)
            {
                msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_WARN);
                data = new Bundle();
                data.putString("msg", mResources.getString(R.string.accept_thread_io_exception));
                msg.setData(data);
                msg.sendToTarget();
                break;
            }

            ConnectedThread connectedThread;

            if (socket != null)
            {
                connectedThread = new ConnectedThread(mActivity, socket);
                connectedThread.start();
                try
                {
                    connectedThread.join();
                }
                catch (InterruptedException ie)
                {
                    // Transition for server
                    // Server Transition
                    msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                    data = new Bundle();
                    msg.arg1 = mActivity.getState();
                    msg.arg2 = mActivity.setState(true);
                    data.putString("reason", mResources.getString(R.string.accept_thread_timeout));
                    msg.setData(data);
                    msg.sendToTarget();
                }
            }
        }
    }

    @Override
    public void cancel()
    {
        try
        {
            mmServerSocket.close();
        }
        catch (IOException ignored) {}
        super.cancel();
    }
}
