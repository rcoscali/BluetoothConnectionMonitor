package com.rcsnet.bluetoothmonitor;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
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
                           BluetoothSocket socket,
                           byte[] sentBuffer,
                           Object monitor)
    {
        super(activity);

        mmSocket = socket;
        mSentBuffer = sentBuffer;
        mMonitor = monitor;

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
        mTimeoutThread = null;
    }

    public ConnectedThread(BluetoothClientServer activity,
                           BluetoothSocket socket)
    {
        this(activity, socket, null, new Object());
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public
    void run()
    {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int    bytes; // bytes returned from read()
        byte[] size;

        synchronized(mMonitor)
        {
            size = new byte[1];
        }
        // Keep listening to the InputStream until an exception occurs
        try
        {
            while (mNotEnd)
            {
                // Read from the InputStream
                int sizesize = mmInStream.read(size);
                bytes = size[0];
                int bufsize = mmInStream.read(buffer, 0, bytes);

                byte[] bufferReceived = Arrays.copyOf(buffer, bytes);
                Message msg;
                Bundle data;

                if (!isInterrupted() && mSentBuffer == null)
                {
                    // Transition for server
                    // Server Transition
                    msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                    data = new Bundle();
                    msg.arg1 = mActivity.getState();
                    msg.arg2 = mActivity.setState(false);
                    data.putString("reason", mResources.getString(R.string.connected_thread_data_received));
                    msg.setData(data);
                    msg.sendToTarget();

                    mTimeoutThread = new TimeoutThread(mActivity, 1000, this);
                    mTimeoutThread.start();
                }

                if (!isInterrupted())
                {
                    // Tell UI about data received
                    msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_DATAIN);
                    data = new Bundle();
                    msg.arg1 = bytes;
                    data.putString("datain", new String(bufferReceived));
                    msg.setData(data);
                    msg.sendToTarget();
                }

                // Check data are the ones expected
                // when we are in the context of a client
                if (mSentBuffer != null) {

                    if (Arrays.equals(bufferReceived, mSentBuffer)) {
                        msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                        data = new Bundle();
                        msg.arg1 = mActivity.getState();
                        msg.arg2 = mActivity.setState(false);
                        data.putString("reason", mResources.getString(R.string.connected_thread_data_acknowledge));
                        msg.setData(data);
                        msg.sendToTarget();
                    } else {
                        msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                        data = new Bundle();
                        msg.arg1 = mActivity.getState();
                        msg.arg2 = mActivity.setState(true);
                        data.putString("reason", mResources.getString(R.string.connected_thread_invalid_data_received));
                        msg.setData(data);
                        msg.sendToTarget();
                    }
                    mNotEnd = false;
                }
                // We are a server, just send back the data as a ping response
                else {
                    try
                    {
                        mTimeoutThread.cancel();
                        if (!isInterrupted())
                        {
                            try
                            {
                                mTimeoutThread.join();
                            }
                            catch (InterruptedException ignored)
                            {
                            }
                        }
                        else
                        {
                            msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                            data = new Bundle();
                            msg.arg1 = mActivity.getState();
                            msg.arg2 = mActivity.setState(true);
                            data.putString("reason", mResources.getString(R.string.connected_thread_data_timeout));
                            msg.setData(data);
                            msg.sendToTarget();
                            mNotEnd = true;
                            break;
                        }

                        mmOutStream.write(size);
                        mmOutStream.write(Arrays.copyOf(buffer, bytes));

                        // Server Transition
                        msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                        data = new Bundle();
                        msg.arg1 = mActivity.getState();
                        msg.arg2 = mActivity.setState(false);
                        data.putString("reason", mResources.getString(R.string.connected_thread_data_acknowledge));
                        msg.setData(data);
                        msg.sendToTarget();

                        // Tell UI about data received
                        msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_DATAOUT);
                        data = new Bundle();
                        msg.arg1 = bytes;
                        data.putString("dataout", new String(Arrays.copyOf(buffer, bytes)));
                        msg.setData(data);
                        msg.sendToTarget();

                    }
                    catch (IOException e)
                    {
                        mTimeoutThread.cancel();
                        try
                        {
                            mTimeoutThread.join();
                        }
                        catch (InterruptedException ignored) {
                        }

                        mNotEnd = false;

                        msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
                        data = new Bundle();
                        msg.arg1 = mActivity.getState();
                        msg.arg2 = mActivity.setState(true);
                        data.putString("reason", mResources.getString(R.string.connected_thread_io_exception));
                        msg.setData(data);
                        msg.sendToTarget();
                    }
                }
            }
        }
        catch (IOException e)
        {
            Message msg = mHandler.obtainMessage(BluetoothClientServer.MESSAGE_STATE_TRANSITION);
            Bundle data = new Bundle();
            msg.arg1 = mActivity.getState();
            msg.arg2 = mActivity.setState(true);
            data.putString("reason", mResources.getString(R.string.connected_thread_io_exception));
            msg.setData(data);
            msg.sendToTarget();
        }
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

    /* Call this from the main activity to shutdown the connection */
    @Override
    public
    void cancel()
    {
        try
        {
            mmSocket.close();
        }
        catch (IOException ignored) { }
        super.cancel();
    }
}
