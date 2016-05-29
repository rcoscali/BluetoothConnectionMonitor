/**
 * Copyright (c) 2016 Rémi Cohen-Scali - All Rights Reserved
 * Created by rcoscali on 26/05/16.
 */
package com.rcsnet.bluetoothmonitor;

import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * State Machine mechanics implementation
 * Created by rcoscali on 26/05/16.
 */
public class StateMachine
        extends Thread
{
    private static final String TAG = "StateMachine";

    protected final BluetoothClientServer mActivity;
    protected final List<TransitionEvent> mTransitions;
    protected State mCurrentState = null;
    protected final State mInitState;
    protected final State mStopState;
    protected final List<TransitionEventListener> listeners = new ArrayList<>(20);

    public StateMachine(BluetoothClientServer activity, State initState, State stopState)
    {
        mActivity = activity;
        mTransitions = new ArrayList<>(10);
        mInitState = initState;
        mStopState = stopState;
    }

    public void init(int millis)
            throws InterruptedException
    {
        mCurrentState = mInitState;
        mCurrentState.enter(millis);
    }

    public TransitionEventListener registerTransitionEventListener(TransitionEventListener lsnr)
    {
        synchronized (listeners) {
            if (!listeners.contains(lsnr)) listeners.add(lsnr);
        }
        return lsnr;
    }

    public void unregisterTransitionEventListener(TransitionEventListener lsnr)
    {
        synchronized (listeners) {
            if (listeners.contains(lsnr)) listeners.remove(lsnr);
        }
    }

    public void sendTransitionEvent(TransitionEvent e)
        throws InterruptedException
    {
        sendTransitionEvent(e, "");
    }

    public void sendTransitionEvent(TransitionEvent e, String msg)
        throws InterruptedException
    {
        List<TransitionEventListener> candidates = new ArrayList<>(5);
        synchronized (listeners) {
            for (TransitionEventListener lsnr : listeners) {
                if (lsnr.onTransitionEventReceived(e))
                    candidates.add(lsnr);
            }
            if (candidates.isEmpty())
                throw new RuntimeException("Unknown transition");
            if (candidates.size() > 1)
                throw new RuntimeException("Ambiguous transition");
        }
        applyTransition(e, mActivity.getPingTimeout());
    }

    public void addTransition(TransitionEvent transitionEvent)
    {
        for (TransitionEvent t : mTransitions)
            if (t.origin().equals(transitionEvent.origin()) &&
                t.target().equals(transitionEvent.target()))
                throw new RuntimeException("Transition with same origin & target already exists");
        mTransitions.add(transitionEvent);
    }

    public State applyTransition(TransitionEvent transitionEvent, int millis)
            throws InterruptedException
    {
        if (mCurrentState.equals(mStopState))
            throw new RuntimeException("StateMachine reached its Stop state");
        if (mCurrentState.isOriginOf(transitionEvent))
        {
            mCurrentState.exit(millis);
            mCurrentState = transitionEvent.target();
            mCurrentState.setTrigerringEvent(transitionEvent);
            mCurrentState.enter(millis);
        }
        return mCurrentState;
    }

    public void dump()
    {
        List<State> states = new ArrayList<>(20);
        Log.v(TAG, "StateMachine: " + toString());
        Log.v(TAG, "    Transitions list: ");
        for (TransitionEvent t : mTransitions)
        {
            Log.v(TAG, "        " + t.toString());
            if (!states.contains(t.origin()))
                states.add(t.origin());
            if (!states.contains(t.target()))
                states.add(t.target());
        }
        Log.v(TAG, "    States list: ");
        for (State s : states)
        {
            Log.v(TAG, "        " + s.toString());
        }
    }

    public interface TransitionEventListener
    {
        boolean onTransitionEventReceived(TransitionEvent event);
    }

    public static class TransitionEvent
            extends EventObject
    {
        private static final String TAG = "StateMachine.TransitionEvent";

        private final State               mFrom;
        private final State               mTo;
        private final Map<String, Object> mUserData;
        private final Map<String, byte[]> mUserByteData;

        public TransitionEvent(Object src, State from, State to)
        {
            super(src);
            mFrom = from;
            mTo = to;
            mUserData = new ArrayMap<>(10);
            mUserByteData = new ArrayMap<>(10);
            mFrom.addAsOriginOf(this);
            mTo.addAsTargetOf(this);
        }

        public State origin()
        {
            return mFrom;
        }

        public State target()
        {
            return mTo;
        }

        @Override
        public String toString()
        {
            String event = super.toString();
            return "Transition " + event + " from " + mFrom.toString() + " to " + mTo.toString();
        }

        public
        Map<String, Object> getmUserData()
        {
            return mUserData;
        }

        public
        Map<String, byte[]> getmUserByteData()
        {
            return mUserByteData;
        }
    }

    public static abstract class State
            implements Runnable, StateMachine.TransitionEventListener
    {
        private static final String TAG = "StateMachine.State";

        private final String mName;

        private Runnable mExitAction = null;

        private List<StateMachine.TransitionEvent> mIsOriginOf;
        private List<StateMachine.TransitionEvent> mIsTargetOf;
        private TransitionEvent mTrigerringEvent = null;

        public State(String name)
        {
            mIsOriginOf = new ArrayList<>(10);
            mIsTargetOf = new ArrayList<>(10);
            mName = name;
        }

        @Override
        public
        boolean onTransitionEventReceived(TransitionEvent event)
        {
            if (isOriginOf(event))
            {
                mTrigerringEvent = event;
                event.target().setTrigerringEvent(event);
            }
            return isOriginOf(event);
        }

        @Override
        public String toString()
        {
            return mName + "{" + super.toString() + "}";
        }

        public boolean isOriginOf(TransitionEvent transition)
        {
            return transition.origin().equals(this);
        }

        public boolean isTargetOf(TransitionEvent transition)
        {
            return transition.target().equals(this);
        }

        public void addAsOriginOf(TransitionEvent transition)
        {
            mIsOriginOf.add(transition);
        }

        public void addAsTargetOf(TransitionEvent transition)
        {
            mIsTargetOf.add(transition);
        }

        public void enter(int millis)
                throws InterruptedException
        {
            Thread thread = new Thread(this);
            thread.start();
            thread.join(millis);
        }

        public void exit(int millis)
                throws InterruptedException
        {
            if (mExitAction != null)
            {
                Thread thread = new Thread(mExitAction);
                thread.start();
                thread.join(millis);
            }
        }

        public
        void setTrigerringEvent(TransitionEvent trigerringEvent)
        {
            this.mTrigerringEvent = trigerringEvent;
        }

        public
        TransitionEvent getTrigerringEvent()
        {
            return this.mTrigerringEvent;
        }

        @Override
        public abstract void run();

    }

}
