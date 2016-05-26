package com.rcsnet.bluetoothmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.util.Map;

/**
 * Created by rcoscali on 26/05/16.
 */
public class ServerStateMachine
        extends StateMachine
{
    private BluetoothAdapter      mAdapter;
    private BluetoothClientServer mActivity;
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
                applyTransition();
            }
            catch (IOException ignored) { }
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
            }
            catch (IOException e)
            {
            }
        }
    };

    public State answeringState = new State("Answering")
    {
        @Override
        public void run()
        {

        }
    };

    public TransitionEvent mInitDoneTransition = new TransitionEvent(this, initState, listeningState);
    public TransitionEvent mConnectionTransition = new TransitionEvent(this, listeningState, receivingState);
    public TransitionEvent mReceivedTransition = new TransitionEvent(this, receivingState, answeringState);

    public TransitionEvent mInitFailedTransition = new TransitionEvent(this, initState, stopState);
    public TransitionEvent mListenFailedTransition = new TransitionEvent(this, listeningState, listeningState);
    public TransitionEvent mReceiveFailedTransition = new TransitionEvent(this, receivingState, listeningState);
    public TransitionEvent mAnswerFailedTransition = new TransitionEvent(this, answeringState, listeningState);

    public ServerStateMachine(BluetoothClientServer activity, State initState, State stopState)
    {
        super(initState, stopState);
        mActivity = activity;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = mActivity.mHandler;
        addTransition();
    }

    @Override
    public void run()
    {

    }
}
