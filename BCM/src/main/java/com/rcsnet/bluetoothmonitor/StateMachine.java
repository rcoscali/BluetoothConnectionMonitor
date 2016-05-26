/**
 * Copyright (c) 2016 Rémi Cohen-Scali - All Rights Reserved
 * Created by rcoscali on 26/05/16.
 */
package com.rcsnet.bluetoothmonitor;

import android.util.Log;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * State Machine mechanics implementation
 * Created by rcoscali on 26/05/16.
 */
public class StateMachine
        extends Thread
{
    private static final String TAG = "StateMachine";

    private List<TransitionEvent> mTransitions;
    private State mCurrentState = null;
    private State mInitState    = null;
    private State mStopState    = null;

    public StateMachine(State initState, State stopState)
    {
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

    public static class TransitionEvent
            extends EventObject
    {
        private static final String TAG = "StateMachine.TransitionEvent";

        private final State mFrom;
        private final State mTo;

        public TransitionEvent(Object src, State from, State to)
        {
            super(src);
            mFrom = from;
            mTo = to;
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
    }

    public static abstract class State
            implements Runnable
    {
        private static final String TAG = "StateMachine.State";

        private final String mName;
        private Runnable mExitAction = null;

        private List<TransitionEvent> mIsOriginOf;
        private List<TransitionEvent> mIsTargetOf;

        public State(String name)
        {
            mIsOriginOf = new ArrayList<>(10);
            mIsTargetOf = new ArrayList<>(10);
            mName = name;
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

        @Override
        public abstract void run();
    }

}
