package com.rcsnet.bluetoothmonitor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class TimeoutTestActivity
        extends AppCompatActivity
{

    private static final String TAG = "TimeoutTest";

    private static final int MAIN_MESSAGE    = 1;
    private static final int TIMEOUT_MESSAGE = 2;
    private static final int COUNTDOWN       = 3;

    private SeekBar  mmTimeoutTimeSeekBarView;
    private SeekBar  mmMainTimeSeekBarView;
    private TextView mmTimeoutTimeText;
    private TextView mmMainTimeText;
    private Button   mmStartButton;
    private Button   mmStopButton;
    private int mmMainTime    = 1;
    private int mmTimeoutTime = 1;
    private TextView    mmTimeoutThreadStatusText;
    private TextView    mmMainThreadStatusText;
    private TextView    mmCountdownText;
    private ProgressBar mmCountdownProgress;
    private int         mmNSeconds;
    private Handler     mmHandler;

    private Thread mainThread;
    private Thread timeoutThread;
    private volatile boolean mmCancelling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeout_test);

        mmCountdownText = (TextView) findViewById(R.id.countdown_text);
        mmCountdownProgress = (ProgressBar) findViewById(R.id.countdown_progress);
        mmCountdownProgress.setProgress(0);
        mmCountdownProgress.setIndeterminate(false);
        mmTimeoutTimeSeekBarView = (SeekBar) findViewById(R.id.timeout_time_seek);
        mmTimeoutTimeSeekBarView.setMax(99);
        mmTimeoutTimeText = (TextView) findViewById(R.id.timeout_time);
        mmTimeoutThreadStatusText = (TextView) findViewById(R.id.timeout_thread_status);
        mmMainTimeSeekBarView = (SeekBar) findViewById(R.id.main_time_seek);
        mmMainTimeSeekBarView.setMax(99);
        mmMainTimeText = (TextView) findViewById(R.id.main_time);
        mmMainThreadStatusText = (TextView) findViewById(R.id.main_thread_status);
        mmStopButton = (Button) findViewById(R.id.stop_button);
        mmStartButton = (Button) findViewById(R.id.start_button);


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

        mmHandler = new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                case COUNTDOWN:
                {
                    Log.v(TAG, "Entering synchronized code in COUNTDOWN processing of handler main loop");
                    synchronized (mmHandler)
                    {
                        Log.v(TAG, "Synchronized code entered in COUNTDOWN processing of handler main loop");
                        if (!mmCancelling)
                        {
                            mmCountdownText.setText(String.format(Locale.FRANCE, "%d (elapsed %d)",
                                                                  mmNSeconds,
                                                                  Math.max(mmMainTime, mmTimeoutTime) - mmNSeconds));
                            double progress = 100.0 * mmNSeconds / Math.max(mmMainTime, mmTimeoutTime);
                            Log.v(TAG, String.format(Locale.FRANCE, "Progress = %d", (int) progress));
                            mmCountdownProgress.setProgress((int) progress);
                        }
                        else
                        {
                            mmCountdownText.setText("Canceled");
                            mmMainThreadStatusText.setText("...");
                            mmTimeoutThreadStatusText.setText("...");
                        }

                        if (mmNSeconds > 0)
                        {
                            mmNSeconds--;
                            rearm();
                        }
                    }
                    break;
                }
                case MAIN_MESSAGE:
                    mmMainThreadStatusText.setText(msg.getData().getString("msg"));
                    break;

                case TIMEOUT_MESSAGE:
                    mmTimeoutThreadStatusText.setText(msg.getData().getString("msg"));
                    break;

                default:
                }

            }
        };

        mmStopButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.v(TAG, "Entering synchronized code in stop button click handler");
                synchronized (mmHandler)
                {
                    Log.v(TAG, "Synchronized code entered in stop button click handler");
                    if (timeoutThread != null)
                    {
                        timeoutThread.interrupt();
                        try
                        {
                            timeoutThread.join();
                            Log.v(TAG, "timeoutThread joined in stop button click handler");
                        }
                        catch (InterruptedException ignored) {}
                        timeoutThread = null;
                    }
                    if (mainThread != null)
                    {
                        mmCancelling = true;
                        mainThread.interrupt();
                        try
                        {
                            mainThread.join();
                            Log.v(TAG, "mainThread joined in stop button click handler");
                        }
                        catch (InterruptedException ignored) {}
                        mainThread = null;
                    }
                    mmStartButton.setEnabled(true);
                    mmStopButton.setEnabled(false);
                    mmCountdownText.setText("Stopped");
                    mmNSeconds = 0;
                    Log.v(TAG, "Notifying at exit of synchronized section of code in stop button click handler");
                    mmHandler.notifyAll();
                }
            }
        });

        mmStartButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mmCancelling = false;
                mmNSeconds = Math.max(mmMainTime, mmTimeoutTime);
                mmStartButton.setEnabled(false);
                mmStopButton.setEnabled(true);
                mmCountdownText.setText(String.format(Locale.FRANCE, "%d", mmNSeconds));
                mmCountdownProgress.setProgress(0);
                mmHandler.obtainMessage(COUNTDOWN).sendToTarget();
                mainThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.v(TAG, "Main thread: started");
                        try
                        {
                            Message msg  = mmHandler.obtainMessage(MAIN_MESSAGE);
                            Bundle  data = new Bundle();
                            data.putString("msg", "Started");
                            msg.setData(data);
                            msg.sendToTarget();
                            synchronized (this)
                            {
                                Log.v(TAG, "Main thread: about to wait " + (mmMainTime * 1000) + " ms");
                                wait(mmMainTime * 1000);
                                Log.v(TAG, "Wait reached its end in main thread");
                                msg = mmHandler.obtainMessage(MAIN_MESSAGE);
                                data = new Bundle();
                                data.putString("msg", "Terminated");
                                msg.setData(data);
                                msg.sendToTarget();
                            }
                        }
                        catch (InterruptedException ie)
                        {
                            Log.v(TAG, "Cancelling main thread");
                            if (!mmCancelling)
                            {
                                Message msg  = mmHandler.obtainMessage(MAIN_MESSAGE);
                                Bundle  data = new Bundle();
                                data.putString("msg", "Interrupted");
                                msg.setData(data);
                                msg.sendToTarget();
                            }
                        }
                    }
                });
                mainThread.start();
                timeoutThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Message msg  = mmHandler.obtainMessage(TIMEOUT_MESSAGE);
                            Bundle  data = new Bundle();
                            data.putString("msg", "Started");
                            msg.setData(data);
                            msg.sendToTarget();
                            synchronized (this)
                            {
                                Log.v(TAG, "About to wait " + mmTimeoutTime + " s in timeout thread");
                                wait(mmTimeoutTime * 1000);
                                Log.v(TAG, "Wait reached its end in timeout thread");
                                if (mainThread != null)
                                    mainThread.interrupt();
                            }
                            msg = mmHandler.obtainMessage(TIMEOUT_MESSAGE);
                            data = new Bundle();
                            data.putString("msg", "Terminated");
                            msg.setData(data);
                            msg.sendToTarget();
                        }
                        catch (InterruptedException ignored)
                        {
                            Log.v(TAG, "Cancelling timeout thread");
                            Message msg  = mmHandler.obtainMessage(TIMEOUT_MESSAGE);
                            Bundle  data = new Bundle();
                            data.putString("msg", "Canceled");
                            msg.setData(data);
                            msg.sendToTarget();
                        }
                    }
                });
                timeoutThread.start();
            }
        });
    }

    private void rearm()
    {
        if (mmHandler != null && mmNSeconds > 0)
            mmHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mmHandler.obtainMessage(COUNTDOWN).sendToTarget();
                }
            }, 1000);
    }
}
