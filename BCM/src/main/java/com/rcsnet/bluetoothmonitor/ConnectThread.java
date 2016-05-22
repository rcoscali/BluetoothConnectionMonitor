package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import java.io.IOException;

/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 * Created by cohen on 20/05/2016.
 */
public class ConnectThread
        extends ConnectionManagement
{
    private static final String TAG = "ConnectThread";

    private final BluetoothSocket       mmSocket;
    private final BluetoothDevice       mmDevice;
    private       PingThread            mmPingThread;

    public ConnectThread(BluetoothClientServer activity, BluetoothDevice device)
    {
        super(activity);
        BluetoothSocket tmp = null;
        mmDevice = device;
        try
        {
            tmp = mmDevice.createRfcommSocketToServiceRecord(BluetoothClientServer.MY_UUID);
        }
        catch (IOException ignored) { }
        mmSocket = tmp;
    }

    @Override
    public
    void run()
    {
        // Warn UI thread
        Message msg  = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
        Bundle data = new Bundle();
        msg.arg1 = mActivity.getState();
        msg.arg2 = BluetoothClientServer.PING_STATE_CONNECTING;
        data.putString("reason", mResources.getString(R.string.connect_thread_started));
        msg.setData(data);
        msg.sendToTarget();

        // Start real work
        mAdapter.cancelDiscovery();
        try
        {
            // Signal UI thread we are connecting
            msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_WARN);
            data = new Bundle();
            data.putString("msg", mResources.getString(R.string.connect_thread_connecting));
            msg.setData(data);
            msg.sendToTarget();

            // Connection on going
            mmSocket.connect();

            // Connection succeed, signal UI thread
            msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_WARN);
            data = new Bundle();
            data.putString("msg", mResources.getString(R.string.connect_thread_connection_succeed));
            msg.setData(data);
            msg.sendToTarget();

            // Warn UI thread and changed state to connected
            msg  = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
            data = new Bundle();
            msg.arg1 = mActivity.getState();
            msg.arg2 = mActivity.setState(false);
            data.putString("reason", mResources.getString(R.string.connected_thread_started));
            msg.setData(data);
            msg.sendToTarget();

            // and now, send ping request
            mmPingThread = new PingThread(mActivity, mmSocket);
            mmPingThread.start();
        }
        catch (IOException connectException)
        {
            try
            {
                // Signal UI thread we are not able to connect
                msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_ERROR);
                data = new Bundle();
                data.putString("msg", mResources.getString(R.string.connect_thread_io_exception));
                msg.setData(data);
                msg.sendToTarget();
                // Then try to close socket
                mmSocket.close();

                // Warn UI thread and changed state to connected
                msg  = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                data = new Bundle();
                msg.arg1 = mActivity.getState();
                msg.arg2 = mActivity.setState(true);
                data.putString("reason", mResources.getString(R.string.connected_thread_started));
                msg.setData(data);
                msg.sendToTarget();
            }
            catch (IOException ignored)
            {
            }
        }
    }

    @Override
    public
    void cancel()
    {
        mmPingThread.cancel();
        try
        {
            mmPingThread.join();
        }
        catch (InterruptedException ignored)
        {
        }
        try
        {
            mmSocket.close();
        }
        catch (IOException ignored) { }
        super.cancel();
    }

}
