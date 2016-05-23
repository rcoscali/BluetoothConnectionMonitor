package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

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
        // Warn UI thread
        sendMessage(R.string.accept_thread_started);

        // Start real work
        BluetoothSocket socket;
        while (mNotEnd)
        {
            try
            {
                sendTransition(R.string.accept_thread_accepting_data, false);

                // Wait for incoming data ...
                socket = mmServerSocket.accept();

                // Some data arrived, tell UI
                sendMessage(R.string.accept_thread_data_received);
            }
            catch (IOException e)
            {
                sendMessage(R.string.accept_thread_io_exception);
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
                    sendTransition(R.string.accept_thread_timeout, true);
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
