package com.rcsnet.bluetoothmonitor;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

public
class StateMachineTest
    extends AppCompatActivity
{
    public MySurfaceView sv;
    public SurfaceHolder sh;
    public Button        state1;
    public Button        state2;
    public Button        state3;
    public Button        state4;
    public Button        state5;
    public Button        state6;
    public Button        state7;
    public Button        state8;

    @Override
    protected
    void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state_machine_test);
        sv = (MySurfaceView) findViewById(R.id.surfaceView);
        sh = sv.getHolder();
        sh.addCallback(sv);
        state1 = (Button) findViewById(R.id.state1);
        state2 = (Button) findViewById(R.id.state2);
        state3 = (Button) findViewById(R.id.state3);
        state4 = (Button) findViewById(R.id.state4);
        state5 = (Button) findViewById(R.id.state5);
        state6 = (Button) findViewById(R.id.state6);
        state7 = (Button) findViewById(R.id.state7);
        state8 = (Button) findViewById(R.id.state8);
    }

    public class MyStateMachine
    extends StateMachine
    {

        public
        MyStateMachine(BluetoothClientServer activity, State initState, State stopState) {
            super(activity, initState, stopState);
        }
    }

    public class MySurfaceView
    extends SurfaceView
    implements SurfaceHolder.Callback
    {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public
        MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public
        void surfaceCreated(SurfaceHolder holder)
        {

        }

        @Override
        public
        void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
        {

        }

        @Override
        public
        void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}
