package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

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
        mmPingThread = new PingThread(mActivity, mmSocket);
    }

    public
    void setPingRequestMinSize(int sz)
    {
        mmPingThread.setmMinCharNr(sz);
    }

    public
    void setPingRequestMaxSize(int sz)
    {
        mmPingThread.setmMaxCharNr(sz);
    }

    @Override
    public
    void run()
    {
        // Warn UI thread
        sendTransition(BluetoothClientServer.PING_STATE_NONE,
                       BluetoothClientServer.PING_STATE_NONE,
                       R.string.connect_thread_started,
                       false);

        // Start real work
        mAdapter.cancelDiscovery();
        try
        {
            // Signal UI thread we are connecting
            sendMessage(R.string.connect_thread_connecting);

            // Connection on going
            mmSocket.connect();

            // Connection succeed, signal UI thread
            sendMessage(R.string.connect_thread_connection_succeed);

            // Warn UI thread and changed state to connected
            sendTransition(BluetoothClientServer.PING_STATE_NONE,
                           BluetoothClientServer.PING_STATE_NONE,
                           R.string.connected_thread_started,
                           false);

            // and now, send ping request
            mmPingThread.start();
        }
        catch (IOException connectException)
        {
            try
            {
                // Signal UI thread we are not able to connect
                sendError(R.string.connect_thread_io_exception);

                // Then try to close socket
                mmSocket.close();
            }
            catch (IOException ignored)
            {
            }
            // Warn UI thread and changed state to connected
            sendTransition(BluetoothClientServer.PING_STATE_NONE,
                           BluetoothClientServer.PING_STATE_NONE,
                           R.string.connected_thread_started,
                           true);
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
