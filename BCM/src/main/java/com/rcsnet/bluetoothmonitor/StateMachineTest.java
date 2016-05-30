package com.rcsnet.bluetoothmonitor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public
class StateMachineTest
    extends AppCompatActivity
{
    private static final int STATE_SWITCHED = 1;
    private static final int INIT_DONE      = 2;
    private static final int DO_INIT        = 3;
    private static final int COUNT_DOWN     = 4;

    //public MySurfaceView sv;
    //public SurfaceHolder sh;
    public Button        startbutton;
    public Button        state1button;
    public Button        state2button;
    public Button        state3button;
    public Button        state4button;
    public Button        state5button;
    public Button        state6button;
    public Button        state7button;
    public Button        state8button;

    public Button[]      stateButtons;

    private int mTimeBeforeOut;

    public Handler mHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public
        void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case DO_INIT:
                    startbutton.setEnabled(true);
                    break;

                case INIT_DONE:
                    startbutton.setEnabled(false);
                    break;

                case STATE_SWITCHED:
                    if (msg.arg1 >= 1 && msg.arg1 <= 8)
                        stateButtons[msg.arg1 -1].setEnabled(false);
                    if (msg.arg2 >= 1 && msg.arg2 <= 8)
                        stateButtons[msg.arg2 -1].setEnabled(true);
                    mTimeBeforeOut = 5;
                    statusText.setText("5");
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public
                        void run()
                        {
                            mTimeBeforeOut--;
                            statusText.setText(String.format(Locale.FRANCE, "%d", mTimeBeforeOut));
                            mHandler.obtainMessage(COUNT_DOWN, mTimeBeforeOut).sendToTarget();
                        }
                    },
                                         1000);
                    break;

                case COUNT_DOWN:
                    if (mTimeBeforeOut > 0)
                        mHandler.postDelayed(new Runnable() {
                                                 @Override
                                                 public
                                                 void run()
                                                 {
                                                     mTimeBeforeOut--;
                                                     statusText.setText(String.format(Locale.FRANCE, "%d", mTimeBeforeOut));
                                                     mHandler.obtainMessage(COUNT_DOWN, mTimeBeforeOut).sendToTarget();
                                                 }
                                             },
                                             1000);
                    break;
            }
        }
    };
    private TextView statusText;
    public MyStateMachine mMyStateMachine;

    public StateMachineTest(){
        mMyStateMachine = new MyStateMachine(this);
    }

    @Override
    protected
    void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state_machine_test);
        //sv = (MySurfaceView) findViewById(R.id.surfaceView);
        //sh = sv.getHolder();
        //sh.addCallback(sv);
        startbutton = (Button) findViewById(R.id.start_button);

        stateButtons = new Button[8];
        stateButtons[0] = state1button = (Button) findViewById(R.id.state1);
        stateButtons[1] = state2button = (Button) findViewById(R.id.state2);
        stateButtons[2] = state3button = (Button) findViewById(R.id.state3);
        stateButtons[3] = state4button = (Button) findViewById(R.id.state4);
        stateButtons[4] = state5button = (Button) findViewById(R.id.state5);
        stateButtons[5] = state6button = (Button) findViewById(R.id.state6);
        stateButtons[6] = state7button = (Button) findViewById(R.id.state7);
        stateButtons[7] = state8button = (Button) findViewById(R.id.state8);

        statusText = (TextView) findViewById(R.id.status_text);

        startbutton.setEnabled(true);
        state1button.setEnabled(false);
        state2button.setEnabled(false);
        state3button.setEnabled(false);
        state4button.setEnabled(false);
        state5button.setEnabled(false);
        state6button.setEnabled(false);
        state7button.setEnabled(false);
        state8button.setEnabled(false);
        startbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.tinit, "Init done");
                }
                catch (InterruptedException e) {
                    Log.v("Init", "Init -> state1 transition failed");
                }
            }
        });
        state1button.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.t2, "State1 done");
                }
                catch (InterruptedException e) {
                    Log.v("State1", "state1 -> state2 transition failed");
                }
            }
        });
        state2button.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.t3, "State2 done");
                }
                catch (InterruptedException e) {
                    Log.v("State2", "state2 -> state3 transition failed");
                }
            }
        });
        state3button.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.t4, "State3 done");
                }
                catch (InterruptedException e) {
                    Log.v("State3", "state3 -> state4 transition failed");
                }
            }
        });
        state4button.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.t5, "State4 done");
                }
                catch (InterruptedException e) {
                    Log.v("State4", "state4 -> state5 transition failed");
                }
            }
        });
        state5button.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.t6, "State5 done");
                }
                catch (InterruptedException e) {
                    Log.v("State5", "state5 -> state6 transition failed");
                }
            }
        });
        state6button.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.t7, "State6 done");
                }
                catch (InterruptedException e) {
                    Log.v("State6", "state6 -> state7 transition failed");
                }
            }
        });
        state7button.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.t8, "State7 done");
                }
                catch (InterruptedException e) {
                    Log.v("State7", "state7 -> state8 transition failed");
                }
            }
        });
        state8button.setOnClickListener(new View.OnClickListener() {
            @Override
            public
            void onClick(View v) {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.t1, "State8 done");
                }
                catch (InterruptedException e) {
                    Log.v("State8", "state8 -> state1 transition failed");
                }
            }
        });

        try
        {
            mMyStateMachine.init(5000);
            //mMyStateMachine.start();
        }
        catch (InterruptedException e) {
            Log.v("StateMachineTest", "Init failed");
        }
    }

    public class MyStateMachine
        extends StateMachine
    {

        public State initState = new State("INIT") {
            @Override
            public
            void run()
            {
                try {
                    mMyStateMachine.sendTransitionEvent(mMyStateMachine.tinit, "Init done");
                    mHandler.obtainMessage(STATE_SWITCHED, 0, 1).sendToTarget();
                }
                catch (InterruptedException e) {
                    Log.v("Init", "Init -> state1 transition failed");
                }
            }
        };

        public State stopState = new State("STOP") {
            @Override
            public
            void run()
            {

            }
        };

        public State state1 = new State("STATE1") {
            @Override
            public
            void run()
            {
                mHandler.obtainMessage(STATE_SWITCHED, 1, 2).sendToTarget();
            }
        };

        public State state2 = new State("STATE2") {
            @Override
            public
            void run()
            {
                mHandler.obtainMessage(STATE_SWITCHED, 2, 3).sendToTarget();
            }
        };

        public State state3 = new State("STATE3") {
            @Override
            public
            void run()
            {
                mHandler.obtainMessage(STATE_SWITCHED, 3, 4).sendToTarget();
            }
        };

        public State state4 = new State("STATE4") {
            @Override
            public
            void run()
            {
                mHandler.obtainMessage(STATE_SWITCHED, 4, 5).sendToTarget();
            }
        };

        public State state5 = new State("STATE5") {
            @Override
            public
            void run()
            {
                mHandler.obtainMessage(STATE_SWITCHED, 5, 6).sendToTarget();
            }
        };

        public State state6 = new State("STATE6") {
            @Override
            public
            void run()
            {
                mHandler.obtainMessage(STATE_SWITCHED, 6, 7).sendToTarget();
            }
        };

        public State state7 = new State("STATE7") {
            @Override
            public
            void run()
            {
                mHandler.obtainMessage(STATE_SWITCHED, 7, 8).sendToTarget();
            }
        };

        public State state8 = new State("STATE8") {
            @Override
            public
            void run()
            {
                mHandler.obtainMessage(STATE_SWITCHED, 8, 1).sendToTarget();
            }
        };

        TransitionEvent tinit = new TransitionEvent(this, initState, state1);

        TransitionEvent t1 = new TransitionEvent(this, state8, state1);
        TransitionEvent t2 = new TransitionEvent(this, state1, state2);
        TransitionEvent t3 = new TransitionEvent(this, state2, state3);
        TransitionEvent t4 = new TransitionEvent(this, state3, state4);
        TransitionEvent t5 = new TransitionEvent(this, state4, state5);
        TransitionEvent t6 = new TransitionEvent(this, state5, state6);
        TransitionEvent t7 = new TransitionEvent(this, state6, state7);
        TransitionEvent t8 = new TransitionEvent(this, state7, state8);

        TransitionEvent terr1 = new TransitionEvent(this, state1, stopState);
        TransitionEvent terr2 = new TransitionEvent(this, state2, stopState);
        TransitionEvent terr3 = new TransitionEvent(this, state3, stopState);
        TransitionEvent terr4 = new TransitionEvent(this, state4, stopState);
        TransitionEvent terr5 = new TransitionEvent(this, state5, stopState);
        TransitionEvent terr6 = new TransitionEvent(this, state6, stopState);
        TransitionEvent terr7 = new TransitionEvent(this, state7, stopState);
        TransitionEvent terr8 = new TransitionEvent(this, state8, stopState);

        public
        MyStateMachine(Activity activity) {
            super(activity);

            mInitState = initState;
            mStopState = stopState;

            initState.setExitAction(new Runnable() {
                @Override
                public
                void run()
                {
                    mHandler.obtainMessage(INIT_DONE).sendToTarget();
                }
            });

            addTransition(tinit);

            addTransition(t1);
            addTransition(t2);
            addTransition(t3);
            addTransition(t4);
            addTransition(t5);
            addTransition(t6);
            addTransition(t7);
            addTransition(t8);

            addTransition(terr1);
            addTransition(terr2);
            addTransition(terr3);
            addTransition(terr4);
            addTransition(terr5);
            addTransition(terr6);
            addTransition(terr7);
            addTransition(terr8);

            registerTransitionEventListener(initState);
            registerTransitionEventListener(stopState);
            registerTransitionEventListener(state1);
            registerTransitionEventListener(state2);
            registerTransitionEventListener(state3);
            registerTransitionEventListener(state4);
            registerTransitionEventListener(state5);
            registerTransitionEventListener(state6);
            registerTransitionEventListener(state7);
            registerTransitionEventListener(state8);

            mPingTimeout = 5000;
            mPingFailureNumber = 5;
        }

        @Override
        public
        void run()
        {
            Log.v("MyStateMachine", "run: State Machine started ");
            mHandler.obtainMessage(DO_INIT).sendToTarget();
        }
    }

    public static class MySurfaceView
        extends SurfaceView
        implements SurfaceHolder.Callback
    {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public
        MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
        {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public
        void surfaceCreated(SurfaceHolder holder)
        {
            Log.v("MySurfaceView", "surfaceCreated");
        }

        @Override
        public
        void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
        {
            Log.v("MySurfaceView", "surfaceChanged");
        }

        @Override
        public
        void surfaceDestroyed(SurfaceHolder holder)
        {
            Log.v("MySurfaceView", "surfaceDestroyed");
        }
    }
}
