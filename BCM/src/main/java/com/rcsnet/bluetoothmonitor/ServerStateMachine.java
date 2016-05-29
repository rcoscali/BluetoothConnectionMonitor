package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by rcoscali on 26/05/16.
 */
public class ServerStateMachine
        extends StateMachine
{
    private BluetoothAdapter      mAdapter;
    private BluetoothServerSocket mmServerSocket;
    private Handler               mHandler;

    public Map<String,Object> mContext;

    public State initState = new State("Init")
    {
        @Override
        public void run()
        {
            try
            {
                mmServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(BluetoothClientServer.NAME,
                                                                  BluetoothClientServer.MY_UUID);
                sendTransitionEvent(mInitDoneTransition, "");
            }
            catch (InterruptedException ie)
            {
                try
                {
                    sendTransitionEvent(mInitFailedTransition,
                                        "Timeout while initializing: " + ie.getLocalizedMessage());
                } catch(InterruptedException ignored) {}
            }
            catch (IOException ioe)
            {
                try
                {
                    sendTransitionEvent(mInitFailedTransition,
                                        "I/O Exception occured: " + ioe.getLocalizedMessage());
                } catch(InterruptedException ignored) {}
            }
        }
    };

    public State stopState = new State("Stop")
    {
        @Override
        public void run()
        {
            mHandler.obtainMessage(BluetoothClientServer.MESSAGE_PING_THREAD_STOP).sendToTarget();
        }
    };

    public State listeningState = new State("Listening")
    {
        @Override
        public void run()
        {
            try
            {
                BluetoothSocket socket = mmServerSocket.accept();
                mConnectionTransition.getmUserData().put("socket", socket);
                sendTransitionEvent(mConnectionTransition, "");
            }
            catch (InterruptedException ie)
            {
                try
                {
                    sendTransitionEvent(mConnectionFailedTransition,
                                        "Timeout while listening for incoming connections: " + ie.getLocalizedMessage());
                } catch(InterruptedException ignored) {}
            }
            catch (IOException ioe)
            {
                try
                {
                    sendTransitionEvent(mConnectionFailedTransition,
                                        "I/O exception while listening for incoming connections: " + ioe.getLocalizedMessage());
                } catch(InterruptedException ignored) {}
            }
        }
    };

    public State receivingState = new State("Receiving")
    {
        @Override
        public void run()
        {
            try
            {
                BluetoothSocket socket =
                    (BluetoothSocket) getTrigerringEvent().getmUserData().get("socket");

                InputStream inStream = socket.getInputStream();
                OutputStream outStream = socket.getOutputStream();

                mConnectionTransition.getmUserData().put("outputStream", outStream);

                byte[] buffer = new byte[1024]; // buffer store for the stream
                int    bytes;                   // bytes returned from read()
                byte[] size = new byte[1];      // Buffer for reading size from stream
                int    sizeOfSize;
                int    bufSize;

                sizeOfSize = inStream.read(size);
                if (sizeOfSize != 1)
                    Log.v("ReceivingState", "Unexpected size size read");
                bytes = size[0];
                bufSize = inStream.read(buffer, 0, bytes);
                if (bufSize != bytes)
                    Log.v("ReceivingState", "Unexpected buffer size read");

                mConnectionTransition.getmUserByteData().put("size", size);
                mConnectionTransition.getmUserByteData().put("buffer", Arrays.copyOf(buffer, bytes));
                sendTransitionEvent(mReceivedTransition);
            }
            catch (InterruptedException ie)
            {
                try {
                    sendTransitionEvent(mReceiveFailedTransition,
                                        "Timeout while receiving data: " + ie.getLocalizedMessage());
                }
                catch (InterruptedException e) {}
            }
            catch (IOException ioe)
            {
                try {
                    sendTransitionEvent(mReceiveFailedTransition,
                                        "I/O exception while receiving data: " + ioe.getLocalizedMessage());
                }
                catch (InterruptedException e) {}
            }
        }
    };

    public State respondingState = new State("Responding") {
        @Override
        public
        void run()
        {
            try
            {
                OutputStream outStream = (OutputStream) getTrigerringEvent().getmUserData().get("outputStream");
                byte[] size = getTrigerringEvent().getmUserByteData().get("size");
                byte[] buffer = getTrigerringEvent().getmUserByteData().get("buffer");
                int bytes = (int)size[0];
                outStream.write(size);
                outStream.write(Arrays.copyOf(buffer, bytes));
                sendTransitionEvent(mRespondedTransition);
            }
            catch (InterruptedException ie)
            {
                try
                {
                    sendTransitionEvent(mResponseFailedTransition,
                                        "Timeout while responding: " + ie.getLocalizedMessage());
                }
                catch (InterruptedException ignored) {}
            }
            catch (IOException ioe)
            {
                try
                {
                    sendTransitionEvent(mResponseFailedTransition,
                                        "I/O exception while responding:" + ioe.getLocalizedMessage());
                }
                catch (InterruptedException ignored) {}
            }
        }
    };

    public State alarmState = new State("Alarm") {
        @Override
        public
        void run()
        {
            Intent intent = new Intent(
                mActivity.getApplicationContext(),
                ConnectionLostAlarm.class);
            mActivity.startActivity(intent);
        }
    };

    public class ErrorState
        extends State
    {
        private int mFailureNumber = 0;
        private final int mMaxFailureNumber;

        public ErrorState()
        {
            super("Error");
            mMaxFailureNumber = mActivity.getPingFailureNumber();
        }

        @Override
        public
        void run()
        {
            try
            {
                if (mFailureNumber < mMaxFailureNumber)
                    mFailureNumber++;
                else
                    sendTransitionEvent(mAlarmRestoreTransition);

                TransitionEvent trigger = getTrigerringEvent();
                if (trigger.equals(mConnectionFailedTransition))
                    sendTransitionEvent(mConnectionFailedRestoreTransition);
                else if (trigger.equals(mReceiveFailedTransition) ||
                         trigger.equals(mResponseFailedTransition))
                    sendTransitionEvent(mRcvRspFailedRestoreTransition);
            }
            catch (InterruptedException ignored)
            {
            }
        }
    }

    public ErrorState errorState = new ErrorState();

    // Nominal transitions
    public TransitionEvent mInitDoneTransition                = new TransitionEvent(this, initState, listeningState);
    public TransitionEvent mConnectionTransition              = new TransitionEvent(this, listeningState, receivingState);
    public TransitionEvent mReceivedTransition                = new TransitionEvent(this, receivingState, respondingState);
    public TransitionEvent mRespondedTransition               = new TransitionEvent(this, respondingState, receivingState);

    // Error Handling transitions
    public TransitionEvent mInitFailedTransition              = new TransitionEvent(this, initState, stopState);
    public TransitionEvent mConnectionFailedTransition        = new TransitionEvent(this, listeningState, errorState);
    public TransitionEvent mReceiveFailedTransition           = new TransitionEvent(this, receivingState, errorState);
    public TransitionEvent mResponseFailedTransition          = new TransitionEvent(this, respondingState, errorState);

    // Error Restoration transitions
    public TransitionEvent mConnectionFailedRestoreTransition = new TransitionEvent(this, errorState, listeningState);
    public TransitionEvent mRcvRspFailedRestoreTransition     = new TransitionEvent(this, errorState, receivingState);
    public TransitionEvent mAlarmRestoreTransition            = new TransitionEvent(this, errorState, alarmState);

    public ServerStateMachine(BluetoothClientServer activity, State initState, State stopState)
    {
        super(activity, initState, stopState);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = mActivity.mHandler;

        addTransition(mInitDoneTransition);
        addTransition(mConnectionTransition);
        addTransition(mReceivedTransition);
        addTransition(mRespondedTransition);
        addTransition(mInitFailedTransition);
        addTransition(mConnectionFailedTransition);
        addTransition(mReceiveFailedTransition);
        addTransition(mResponseFailedTransition);
        addTransition(mConnectionFailedRestoreTransition);
        addTransition(mRcvRspFailedRestoreTransition);
        addTransition(mAlarmRestoreTransition);
    }

    @Override
    public void run()
    {

    }
}
