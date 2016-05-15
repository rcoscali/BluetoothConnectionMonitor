package monitor.bluetoothconnection.rcsnet.com.bluetoothconnectionmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothConnect extends AppCompatActivity
{
    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 0;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Snackbar.make(view,
                                      "Restart scan",
                                      Snackbar.LENGTH_LONG).setAction("ActionRestartScan", null).show();
                        Snackbar.make(view,
                                      "Stop scan",
                                      Snackbar.LENGTH_LONG).setAction("ActionStopScan", null).show();
                    }
                }
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_ENABLE_BLUETOOTH)
            return;
        if (resultCode != RESULT_OK)
            finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth_connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }
        else if (id == R.id.action_restart_scan)
        {
            BluetoothAdapter.getDefaultAdapter ().startDiscovery ();
            return true;
        }
        else if (id == R.id.action_stop_scan)
        {
            BluetoothAdapter.getDefaultAdapter ().cancelDiscovery ();
            return true;
        }
        else if (id == R.id.action_quit)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
    {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 0;

        private int                   mSectionNumber;
        private View                  mRootView;
        private TextView              mTextView;
        private ListView              mListView;
        private ProgressBar           mProgressBar;
        private BluetoothAdapter      mBluetoothAdapter;
        private List<BluetoothDevice> mDeviceList;

        public PlaceholderFragment()
        {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber)
        {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container,
                                 Bundle savedInstanceState)
        {
            mSectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            Log.v("BluetoothMonitor",
                  "Fragment - onCreateView: " + getString(R.string.section_format, mSectionNumber));
            mRootView         = inflater.inflate(R.layout.fragment_bluetooth_connect, container, false);
            mTextView = (TextView) mRootView.findViewById(R.id.section_label);
            mListView = (ListView) mRootView.findViewById(R.id.section_list);
            mProgressBar = (ProgressBar) mRootView.findViewById (R.id.progressBar);
            mListView.setAdapter (new ArrayAdapter<String> (this.getActivity (),
                                                            R.layout.rowitem,
                                                            R.id.row_item_text));
            mListView.setOnItemClickListener (new AdapterView.OnItemClickListener ()
            {
                @Override
                public
                void onItemClick (AdapterView<?> parent, View view, int position, long id)
                {
                    BluetoothDevice device = mDeviceList.get (position);
                    Log.v("BluetoothMonitor",
                          "Device selected: " + device.getName ());
                    Intent intent = new Intent (getActivity ().getApplicationContext (), BluetoothClientServer.class);
                    intent.putExtra("device", device);
                    startActivity (intent);
                }
            });
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter ();
            if (mBluetoothAdapter == null)
            {
                Toast.makeText (this.getActivity ().getApplicationContext (), "Pas de Bluetooth",
                                Toast.LENGTH_SHORT
                               ).show ();
                mTextView.setText("No bluetooth device");
            }
            else
            {
                mTextView.setText(getString(R.string.section_format,
                                           mSectionNumber));
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBlueTooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBlueTooth, REQUEST_CODE_ENABLE_BLUETOOTH);
                }
                else
                {
                    ((ArrayAdapter<String>)mListView.getAdapter ()).clear ();
                    addBluetoothKnownDevices ();
                    addBluetoothNewDevices ();
                }
            }

            return mRootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode != REQUEST_CODE_ENABLE_BLUETOOTH)
                return;
            if (resultCode == 0)
                this.getActivity ().finish ();
            else
            {
                ((ArrayAdapter<String>)mListView.getAdapter ()).clear ();
                addBluetoothKnownDevices ();
                addBluetoothNewDevices ();
            }
        }

        private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
            public void onReceive (Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                {
                    mProgressBar.setVisibility (View.VISIBLE);
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                {
                    mProgressBar.setVisibility (View.GONE);
                }
                else if (BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDeviceList.add (device);
                    String item = device.getName () + " " + device.getAddress ();
                    Log.v("BluetoothMonitor", "Adding new device item: " + item);
                    ((ArrayAdapter<String>)mListView.getAdapter ()).add(item);
                }
            }
        };

        private
        void addBluetoothNewDevices ()
        {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            getContext().registerReceiver(mBluetoothReceiver, filter);
            mBluetoothAdapter.startDiscovery();
        }

        @Override
        public
        void onDestroy() {
            super.onDestroy();
            mBluetoothAdapter.cancelDiscovery();
            getContext().unregisterReceiver(mBluetoothReceiver);
        }

        private
        void addBluetoothKnownDevices()
        {
            mDeviceList = new ArrayList<> ();
            Set<BluetoothDevice> deviceList = mBluetoothAdapter.getBondedDevices ();
            for (BluetoothDevice device : deviceList) {
                mDeviceList.add (device);
                String item = device.getName () + " " + device.getAddress ();
                Log.v("BluetoothMonitor", "Adding known device item: " + item);
                ((ArrayAdapter<String>)mListView.getAdapter ()).add(item);
            }
        }

        @Override
        public void onStart()
        {
            super.onStart ();
            Log.v("BluetoothMonitor", "Fragment - onStart: section number #" + mSectionNumber);
        }

        @Override
        public void onResume()
        {
            super.onResume();
            Log.v("BluetoothMonitor", "Fragment - onResume: section number #" + mSectionNumber);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private final int STATE_START = 1;
        private final int STATE_DEVICE_SELECTED = 2;
        private final int STATE_DEvICE_CONNECTED = 3;
        private int mState;

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
            mState = STATE_START;
        }

        @Override
        public Fragment getItem(int position)
        {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount()
        {
            // Show 3 total pages.
            switch (mState)
            {
            case STATE_START:
                return 1;
            case STATE_DEVICE_SELECTED:
                return 2;
            case STATE_DEvICE_CONNECTED:
                return 3;
            default:
                return 1;
            }
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0:
                    return "Check Bluetooth Device";
                case 1:
                    return "List Reachable Devices";
                case 2:
                    return "Connect And Monitor";
            }
            return null;
        }
    }
}
