package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothSocket;

import java.util.Arrays;

/**
 * Copyright (C) 2016 - Rémi Cohen-Scali. All rights reserved.
 * Created by cohen on 20/05/2016.
 */
public class PingThread
        extends ConnectionManagement
{
    private final static String TAG = "PingThread";

    private static final int MIN_CHAR_NR = 2;
    private static final int MAX_CHAR_NR = 16;
    public final  Object          mMonitor;
    private final BluetoothSocket mSocket;
    private       ConnectedThread mmConnectedThread;
    private       TimeoutThread   mmTimeoutThread;
    private       int             mMinCharNr;
    private       int             mMaxCharNr;

    public PingThread(BluetoothClientServer activity,
                      BluetoothSocket socket)
    {
        super(activity);
        mSocket = socket;
        mMonitor = new Object();
        mMinCharNr = MIN_CHAR_NR;
        mMaxCharNr = MAX_CHAR_NR;
        mmTimeoutThread = null;
    }

    public void setmMaxCharNr(int mMaxCharNr)
    {
        this.mMaxCharNr = mMaxCharNr;
    }

    public void setmMinCharNr(int mMinCharNr)
    {
        this.mMinCharNr = mMinCharNr;
    }

    @Override
    public void run()
    {
        while (mNotEnd)
        {
            int    bytes  = randomNrOfBytes();
            byte[] buffer = new byte[bytes + 1];
            // First byte transmitted is size of buffer
            buffer[0] = (byte) bytes;
            // Then fill with random bytes
            fillRandomBytes(buffer, bytes);

            mmConnectedThread = new ConnectedThread(mActivity,
                                                    mSocket,
                                                    Arrays.copyOfRange(buffer, 1, buffer.length),
                                                    this);
            if (mmTimeoutThread == null)
                mmTimeoutThread = new TimeoutThread(mActivity,
                                                    mActivity.getPingTimeout(),
                                                    mmConnectedThread);
            mmTimeoutThread.start();
            synchronized (mMonitor)
            {
                mmConnectedThread.start();
            }
            mmConnectedThread.write(buffer);
            mmTimeoutThread.cancel();

            sendTransition(BluetoothClientServer.PING_STATE_NONE,
                           BluetoothClientServer.PING_STATE_NONE,
                           R.string.ping_thread_send_request,
                           false);

            // Tell main UI thread about data we sent in ping request
            sendDataInOutMessage(false, Arrays.copyOfRange(buffer, 1, buffer.length), bytes);

            try
            {
                mmConnectedThread.join();
            }
            catch (InterruptedException ignored)
            {
            }

            try
            {
                synchronized (mMonitor)
                {
                    wait(mActivity.getPingFrequency());
                }
            }
            catch (InterruptedException ignored)
            {
            }

            if (mActivity.getState() == BluetoothClientServer.PING_STATE_ALARM)
            {
                sendTransition(BluetoothClientServer.PING_STATE_NONE,
                               BluetoothClientServer.PING_STATE_NONE,
                               R.string.alarm_launched,
                               false);
                mNotEnd = false;
            }
        }

        mmConnectedThread.cancel();
        mHandler.obtainMessage(BluetoothClientServer.MESSAGE_PING_THREAD_STOP).sendToTarget();
    }

    @Override
    public
    void cancel()
    {
        mCanceling = true;
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

    private
    int randomNrOfBytes()
    {
        return MIN_CHAR_NR + (int)(Math.random() * (float)(mMaxCharNr - mMinCharNr));
    }

    private
    void fillRandomBytes(byte[] buf, int bytes)
    {
        for (int n = 1; n <= bytes; n++)
        {
            buf[n] = (byte) ((Math.random() * ('z' - 'A')) + 'A');
        }
    }
}
