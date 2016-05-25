package com.rcsnet.bluetoothmonitor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 * Created by cohen on 20/05/2016.
 */
public class ConnectedThread
        extends ConnectionManagement
{
    private static final String TAG = "ConnectedThread";

    private final BluetoothSocket mmSocket;
    private final InputStream     mmInStream;
    private final OutputStream    mmOutStream;
    private final byte[]          mSentBuffer;
    private final Object          mMonitor;
    private       TimeoutThread   mTimeoutThread;

    public ConnectedThread(BluetoothClientServer activity,
                           BluetoothSocket socket)
    {
        // Call default constructor
        this(activity, socket, null, new Object());
    }

    public ConnectedThread(BluetoothClientServer activity,
                           BluetoothSocket socket,
                           byte[] sentBuffer,
                           Object monitor)
    {
        // Call superclass constructor
        super(activity);

        // Init fields
        mmSocket = socket;
        mSentBuffer = sentBuffer;
        mMonitor = monitor;
        mTimeoutThread = null;

        // Init final streams using temp var
        InputStream  tmpIn  = null;
        OutputStream tmpOut = null;
        try
        {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        }
        catch (IOException ignored)
        {
        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @SuppressLint("Assert")
    @Override
    public
    void run()
    {
        // Keep listening to the InputStream until an exception occurs
        try
        {
            byte[] buffer;  // buffer store for the stream
            int    bytes;   // bytes returned from read()
            byte[] size;    // Buffer for reading size from stream
            int    sizeOfSize;
            int    bufSize;
            byte[] bufferReceived;

            synchronized(mMonitor)
            {
                buffer = new byte[1024];
                size = new byte[1];
            }
            while (mNotEnd)
            {
                mTimeoutThread = new TimeoutThread(mActivity, 1000, this);
                mTimeoutThread.start();

                // Read from the InputStream
                sizeOfSize = mmInStream.read(size);
                assert (sizeOfSize == 1);
                bytes = size[0];
                bufSize = mmInStream.read(buffer, 0, bytes);
                assert (bufSize == bytes);

                // Thread interrupted & joined in cancel
                mTimeoutThread.cancel();

                bufferReceived = Arrays.copyOf(buffer, bytes);

                if (!isInterrupted())
                {
                    if (mSentBuffer == null)
                        // Transition for server
                        sendTransition(BluetoothClientServer.PING_STATE_LISTENING,
                                       BluetoothClientServer.PING_STATE_ACKNOWLEDGED,
                                       R.string.connected_thread_data_received,
                                       false);

                    // Tell UI about data received
                    sendDataInOutMessage(true, bufferReceived, bytes);
                }
                else
                {
                    sendTransition(BluetoothClientServer.PING_STATE_REQUESTED,
                                   BluetoothClientServer.PING_STATE_NONE,
                                   R.string.connected_thread_data_timeout,
                                   true);
                    break;
                }

                // Check data are the ones expected
                // when we are in the context of a client
                if (mSentBuffer != null)
                {
                    if (Arrays.equals(bufferReceived, mSentBuffer))
                        sendTransition(BluetoothClientServer.PING_STATE_NONE,
                                       BluetoothClientServer.PING_STATE_ACKNOWLEDGED,
                                       R.string.connected_thread_data_acknowledge,
                                       false);
                    else
                        sendTransition(BluetoothClientServer.PING_STATE_REQUESTED,
                                       BluetoothClientServer.PING_STATE_NONE,
                                       R.string.connected_thread_invalid_data_received,
                                       true);

                    mNotEnd = false;  // Client stop
                }
                // We are a server, just send back the data as a ping response
                else
                {
                    mNotEnd = true;  // If server go on loop
                    try
                    {
                        mmOutStream.write(size);
                        mmOutStream.write(Arrays.copyOf(buffer, bytes));

                        // Server Transition & Tell UI about data received
                        sendTransition(BluetoothClientServer.PING_STATE_REQUESTED,
                                       BluetoothClientServer.PING_STATE_ACKNOWLEDGED,
                                       R.string.connected_thread_data_acknowledge,
                                       false);
                        sendDataInOutMessage(false, buffer, bytes);
                    }
                    catch (IOException e)
                    {
                        mNotEnd = false;
                        sendTransition(BluetoothClientServer.PING_STATE_REQUESTED,
                                       BluetoothClientServer.PING_STATE_NONE,
                                       R.string.connected_thread_io_exception,
                                       true);
                    }
                }
            }
        }
        catch (IOException e)
        {
            sendTransition(BluetoothClientServer.PING_STATE_NONE,
                           BluetoothClientServer.PING_STATE_NONE,
                           R.string.connected_thread_io_exception,
                           true);
        }
    }

    /* Call this from the main activity to shutdown the connection */
    @Override
    public void cancel()
    {
        mCanceling = true;
        try
        {
            mmSocket.close();
        }
        catch (IOException ignored) { }
        super.cancel();
    }

    /* Call this from the main activity to send data to the remote device */
    public
    void write(byte[] bytes)
    {
        try
        {
            mmOutStream.write(bytes);
        }
        catch (IOException ignored) { }
    }
}
