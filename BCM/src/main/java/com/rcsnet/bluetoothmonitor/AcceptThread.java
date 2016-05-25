/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 * Created by cohen on 20/05/2016.
 */
package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

/**
 * Thread providing an accept on a socket for a server
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
        mCanceling = false;
        // Warn UI thread
        sendMessage(R.string.accept_thread_started);

        // Start real work
        BluetoothSocket socket;
        while (mNotEnd)
        {
            try
            {
                sendTransition(BluetoothClientServer.PING_STATE_INIT,
                               BluetoothClientServer.PING_STATE_LISTENING,
                               R.string.accept_thread_accepting_data,
                               false);

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
                    if (!mCanceling)
                    {
                        // Transition for server
                        sendTransition(BluetoothClientServer.PING_STATE_LISTENING,
                                       BluetoothClientServer.PING_STATE_NONE,
                                       R.string.accept_thread_timeout,
                                       true);
                    }
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
