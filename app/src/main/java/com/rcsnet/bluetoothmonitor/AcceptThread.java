package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;

/**
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
            tmp = mAdapter.listenUsingRfcommWithServiceRecord(mActivity.NAME,
                                                              mActivity.MY_UUID);
        }
        catch (IOException ignored) { }
        mmServerSocket = tmp;
    }

    @Override
    public void run()
    {
        // Warn UI thread
        Message msg  = mHandler.obtainMessage(mActivity.MESSAGE_WARN);
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
                // Signal UI thread we are about to accept incoming data
                msg = mHandler.obtainMessage(mActivity.MESSAGE_WARN);
                data = new Bundle();
                data.putString("msg", mResources.getString(R.string.accept_thread_accepting_data));
                msg.setData(data);
                msg.sendToTarget();

                // Wait for incoming data ...
                socket = mmServerSocket.accept();

                // Some data arrived, tell UI
                msg = mHandler.obtainMessage(mActivity.MESSAGE_WARN);
                data = new Bundle();
                data.putString("msg", mResources.getString(R.string.accept_thread_data_received));
                msg.setData(data);
                msg.sendToTarget();
            }
            catch (IOException e)
            {
                msg = mHandler.obtainMessage(mActivity.MESSAGE_WARN);
                data = new Bundle();
                data.putString("msg", mResources.getString(R.string.accept_thread_io_exception));
                msg.setData(data);
                msg.sendToTarget();
                break;
            }

            if (socket != null)
            {
                ConnectedThread connectedThread = new ConnectedThread(mActivity, socket);
                connectedThread.start();
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
