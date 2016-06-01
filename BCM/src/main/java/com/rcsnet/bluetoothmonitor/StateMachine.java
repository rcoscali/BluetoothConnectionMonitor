/**
 * Copyright (c) 2016 Rémi Cohen-Scali - All Rights Reserved
 * Created by rcoscali on 26/05/16.
 */
package com.rcsnet.bluetoothmonitor;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * Finite State Machine mechanics implementation
 *
 * Created by rcoscali on 26/05/16.
 */
public class StateMachine
        extends Thread
{
    private static final String TAG = "StateMachine";

    /**
     *
     */
    protected final Activity mActivity;
    protected final List<TransitionEvent> mTransitions;
    protected State mCurrentState = null;
    protected State mInitState;
    protected State mStopState;
    protected final List<TransitionEventListener> listeners = new ArrayList<>(20);
    protected int mPingTimeout;
    protected int mPingFailureNumber;

    /**
     * State machine constructor
     *
     * @param activity the parent activity
     */
    public
    StateMachine(Activity activity)
    {
        mActivity = activity;
        mTransitions = new ArrayList<>(10);
    }

    /**
     * Affectation to the init state field
     */
    public
    void setInitState(State initState)
    {
        mInitState = initState;
    }

    /**
     * Affectation to the stop state field
     * @param stopState
     */
    public
    void setStopState(State stopState)
    {
        mStopState = stopState;
    }

    /**
     * The init state actions
     *
     * @param millis
     * @throws InterruptedException
     */
    public
    void init(int millis)
            throws InterruptedException
    {
        mCurrentState = mInitState;
        mCurrentState.enter(millis);
    }

    /**
     * registerTransitionEventListener
     *
     * @param lsnr the listener to add
     * @return the lsnr or null is something wrong occured
     */
    public
    TransitionEventListener registerTransitionEventListener(TransitionEventListener lsnr)
    {
        synchronized (listeners) {
            if (!listeners.contains(lsnr))
                listeners.add(lsnr);
        }
        return lsnr;
    }

    /**
     * unregisterTransitionEventListener
     *
     * @param lsnr the listener to remove
     */
    public
    void unregisterTransitionEventListener(TransitionEventListener lsnr)
    {
        synchronized (listeners) {
            if (listeners.contains(lsnr))
                listeners.remove(lsnr);
        }
    }

    /**
     * Send a transition event
     *
     * @param e The event to send to states
     * @throws InterruptedException In case time provided for executing new state action
     *                              too long
     */
    public
    void sendTransitionEvent(TransitionEvent e)
        throws InterruptedException
    {
        sendTransitionEvent(e, "Sending event " + e.origin());
    }

    /**
     * Send a transition event
     *
     * @param e The event to send to states
     * @throws InterruptedException In case time provided for executing new state action
     *                              too long
     */
    public
    void sendTransitionEvent(TransitionEvent e, String msg)
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
        applyTransition(e, mPingTimeout);
    }

    /**
     * Add transition to the specified state
     *
     * @param transitionEvent
     */
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

    /**
     * ===========================================================================================
     * TransitionEvent listener interface
     *
     * ===========================================================================================
     */
    public
    interface TransitionEventListener
    {
        boolean onTransitionEventReceived(TransitionEvent event);
    }

    /**
     * ===========================================================================================
     * TransitionEvent interface
     *
     * The event triggering the
     * ===========================================================================================
     */
    public static
    class TransitionEvent
            extends EventObject
    {
        private static final String TAG = "StateMachine.TransitionEvent";

        private final String              mName;
        private final State               mFrom;
        private final State               mTo;
        private final Map<String, Object> mUserData;
        private final Map<String, byte[]> mUserByteData;

        public
        TransitionEvent(String name, Object src, State from, State to)
        {
            super(src);
            mFrom = from;
            mTo = to;
            mUserData = new ArrayMap<>(10);
            mUserByteData = new ArrayMap<>(10);
            mFrom.addAsOriginOf(this);
            mTo.addAsTargetOf(this);
            mName = name;
        }

        public
        TransitionEvent(Object src, State from, State to)
        {
            this("", src, from, to);
        }

        public
        TransitionEvent(State from, State to)
        {
            this("", null, from, to);
        }

        public
        State origin()
        {
            return mFrom;
        }

        public
        State target()
        {
            return mTo;
        }

        @Override
        public
        String toString()
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

    /**
     * ===========================================================================================
     * State class
     *
     * A State is a machine concept to which is attached an action. Machine can change state
     * when a specific event occurs. This event allows machine to switch to another state and to
     * execute some other actions. Actions are attached to either state entering or state exiting.
     * Machine can only be in a unique state.
     * ===========================================================================================
     */
    public static
    abstract class State
        implements Runnable, StateMachine.TransitionEventListener
    {
        private static final String TAG = "StateMachine.State";

        private final String mName;

        private Runnable mExitAction = null;

        private List<StateMachine.TransitionEvent> mIsOriginOf;
        private List<StateMachine.TransitionEvent> mIsTargetOf;
        private TransitionEvent mTrigerringEvent = null;

        public
        State(String name)
        {
            mIsOriginOf = new ArrayList<>(10);
            mIsTargetOf = new ArrayList<>(10);
            mName = name;
        }

        public
        void setExitAction(Runnable exitAction)
        {
            mExitAction = exitAction;
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
        public
        String toString()
        {
            return mName + "{" + super.toString() + "}";
        }

        public
        boolean isOriginOf(TransitionEvent transition)
        {
            return transition.origin().equals(this);
        }

        public
        boolean isTargetOf(TransitionEvent transition)
        {
            return transition.target().equals(this);
        }

        public
        void addAsOriginOf(TransitionEvent transition)
        {
            mIsOriginOf.add(transition);
        }

        public
        void addAsTargetOf(TransitionEvent transition)
        {
            mIsTargetOf.add(transition);
        }

        /**
         * Method called when a state is entered
         * @param millis
         * @throws InterruptedException Raised if something wrong occurs (including timeout)
         */
        public
        void enter(int millis)
            throws InterruptedException
        {
            Thread thread = new Thread(this);
            thread.start();
            thread.join(millis);
        }

        /**
         * Method called when a state is exited
         * @param millis
         * @throws InterruptedException Raised if something wrong occurs (including timeout)
         */
        public
        void exit(int millis)
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

        /**
         * Unimplemented Runnable method
         */
        @Override
        public abstract
        void run();

    }

}
