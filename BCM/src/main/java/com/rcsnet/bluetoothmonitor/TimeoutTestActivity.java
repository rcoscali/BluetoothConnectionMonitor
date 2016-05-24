package com.rcsnet.bluetoothmonitor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class TimeoutTestActivity
        extends AppCompatActivity
{

    private static final int MAIN_MESSAGE    = 1;
    private static final int TIMEOUT_MESSAGE = 2;

    private SeekBar  mmTimeoutTimeSeekBarView;
    private SeekBar  mmMainTimeSeekBarView;
    private TextView mmTimeoutTimeText;
    private TextView mmMainTimeText;
    private Button   mmStartButton;
    private int      mmMainTime;
    private int      mmTimeoutTime;
    private TextView mmTimeoutThreadStatusText;
    private TextView mmMainThreadStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeout_test);

        mmTimeoutTimeSeekBarView = (SeekBar) findViewById(R.id.timeout_time_seek);
        mmTimeoutTimeSeekBarView.setMax(99);
        mmTimeoutTimeText = (TextView) findViewById(R.id.timeout_time);
        mmTimeoutThreadStatusText = (TextView) findViewById(R.id.main_thread_status);
        mmTimeoutTimeSeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                mmTimeoutTime = i + 1;
                mmTimeoutTimeText.setText(String.format(Locale.FRANCE, "%d s", i + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        mmMainTimeSeekBarView = (SeekBar) findViewById(R.id.main_time_seek);
        mmMainTimeSeekBarView.setMax(99);
        mmMainTimeText = (TextView) findViewById(R.id.main_time);
        mmMainThreadStatusText = (TextView) findViewById(R.id.main_thread_status);
        mmMainTimeSeekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                mmMainTime = i + 1;
                mmMainTimeText.setText(String.format(Locale.FRANCE, "%d s", i + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        final Handler handler = new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                case MAIN_MESSAGE:
                {
                    String str = msg.getData().getString("msg");
                    mmMainThreadStatusText.setText(str);
                    break;
                }

                case TIMEOUT_MESSAGE:
                {
                    String str = msg.getData().getString("msg");
                    mmTimeoutThreadStatusText.setText(str);
                    break;
                }

                default:
                }

            }
        };

        mmStartButton = (Button) findViewById(R.id.start_button);
        mmStartButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final Thread mainThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Message msg  = handler.obtainMessage(MAIN_MESSAGE);
                            Bundle  data = new Bundle();
                            data.putString("msg", "Started");
                            msg.setData(data);
                            msg.sendToTarget();
                            synchronized (this)
                            {
                                wait(mmMainTime);
                                msg = handler.obtainMessage(MAIN_MESSAGE);
                                data = new Bundle();
                                data.putString("msg", "Terminated");
                                msg.setData(data);
                                msg.sendToTarget();
                            }
                        }
                        catch (InterruptedException ie)
                        {
                            Message msg  = handler.obtainMessage(MAIN_MESSAGE);
                            Bundle  data = new Bundle();
                            data.putString("msg", "Interrupted");
                            msg.setData(data);
                            msg.sendToTarget();
                        }
                    }
                });
                mainThread.start();
                Thread timeoutThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Message msg  = handler.obtainMessage(TIMEOUT_MESSAGE);
                            Bundle  data = new Bundle();
                            data.putString("msg", "Started");
                            msg.setData(data);
                            msg.sendToTarget();
                            synchronized (this)
                            {
                                wait(mmTimeoutTime);
                                mainThread.interrupt();
                            }
                        }
                        catch (InterruptedException ignored)
                        {

                        }
                        Message msg  = handler.obtainMessage(TIMEOUT_MESSAGE);
                        Bundle  data = new Bundle();
                        data.putString("msg", "Terminated");
                        msg.setData(data);
                        msg.sendToTarget();
                    }
                });
                timeoutThread.start();
            }
        });
    }
}
