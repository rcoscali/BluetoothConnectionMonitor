package com.rcsnet.bluetoothmonitor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Main activity of this bluetooth connection monitoring application
 * It displays available bluetooth devices and allow the user to
 * launch a ping client for a selected device.
 * It also allows to launch a ping server.
 */
public class BluetoothConnect
        extends AppCompatActivity
{
    private static final int     REQUEST_CODE_ENABLE_BLUETOOTH = 0;
    private              boolean mScanningDevices              = false;
    public static final  String  PREFS_NAME                    = "BluetoothConnectMonitorPreferences";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private SharedPreferences    mSettings;

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
        if (mViewPager != null)
            mViewPager.setAdapter(mSectionsPagerAdapter);

        // Preferences
        mSettings = getPreferences(MODE_PRIVATE);
        mSettings.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s)
            {

            }
        });
    }

    /**
     * Callback called when a started activity return a result
     * If bluetooth device not available, finish the main activity
     *
     * @param requestCode The request code used fat activity start
     * @param resultCode  The result returned by the activity
     * @param data        The intent used for activity launch
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_ENABLE_BLUETOOTH)
            return;
        if (resultCode != RESULT_OK)
            finish();
    }

    /**
     * Callback called for creation of the menu bar menu items
     *
     * @param menu The menu to be populated
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth_connect, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem itemRestartScan = menu.findItem(R.id.action_restart_scan);
        MenuItem itemStopScan    = menu.findItem(R.id.action_stop_scan);

        if (mScanningDevices)
        {
            itemRestartScan.setEnabled(true);
            itemStopScan.setEnabled(true);
            MenuItemCompat.setShowAsAction(itemRestartScan, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            MenuItemCompat.setShowAsAction(itemStopScan, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        }
        else
        {
            itemRestartScan.setEnabled(true);
            itemStopScan.setEnabled(false);
            MenuItemCompat.setShowAsAction(itemRestartScan, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
            MenuItemCompat.setShowAsAction(itemStopScan, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Callback called when a menu bar option item is selected
     *
     * @param item The menu bar menu item selected
     * @return Superclass method result if not handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId())
        {
        case R.id.action_launch_server:
        {
            Intent intent = new Intent(
                    getApplicationContext(),
                    BluetoothClientServer.class);
            intent.putExtra("device", "");
            intent.putExtra("local", true);
            startActivity(intent);
        }
        break;

        case R.id.action_settings:
        {
            Intent intent = new Intent(getApplicationContext(), BluetoothMonitorSettings.class);
            startActivity(intent);
        }
        break;

        case R.id.action_restart_scan:
            BluetoothAdapter.getDefaultAdapter().startDiscovery();
            mScanningDevices = true;
            break;

        case R.id.action_stop_scan:
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            mScanningDevices = false;
            break;

        case R.id.action_quit:
            finish();
            break;

        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void setScanningDevices(boolean scanningDevices)
    {
        this.mScanningDevices = scanningDevices;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment
            extends Fragment
    {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Request code used for Bluetooth device activation
         * Requires BLUETOOTH_ADMIN permission
         */
        private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 0;

        private int                mSectionNumber;
        private View               mRootView;
        private TextView           mTextView;
        private ListView           mListView;
        private ProgressBar        mProgressBar;
        private BluetoothAdapter   mBluetoothAdapter;
        private DeviceArrayAdapter mArrayAdapter;

        public PlaceholderFragment()
        {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         *
         * @param sectionNumber page number to instanciate
         * @return Newly instanciated fragment
         */
        public static PlaceholderFragment newInstance(int sectionNumber)
        {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle              args     = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        /**
         * Create the view for this fragment
         *
         * @param inflater           The inflater used for parsing the layout resource
         * @param container          The container in which this view is embedded
         * @param savedInstanceState A bundle for the saved state
         * @return view created for the fragment
         */
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container,
                                 Bundle savedInstanceState)
        {
            mSectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            mRootView = inflater.inflate(R.layout.fragment_bluetooth_connect, container, false);
            mTextView = (TextView) mRootView.findViewById(R.id.section_label);
            mListView = (ListView) mRootView.findViewById(R.id.section_list);
            mProgressBar = (ProgressBar) mRootView.findViewById(R.id.progressBar);
            mArrayAdapter = new DeviceArrayAdapter(this.getActivity());
            mListView.setAdapter(mArrayAdapter);
            registerForContextMenu(mListView);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    BluetoothDevice device = mArrayAdapter.getItem(position);
                    Log.v("BluetoothMonitor",
                          "Device selected: " + device.getName());
                    Intent intent = new Intent(getActivity().getApplicationContext(), BluetoothClientServer.class);
                    intent.putExtra("device", device);
                    intent.putExtra("local", false);
                    startActivity(intent);
                }
            });
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null)
            {
                Toast.makeText(this.getActivity().getApplicationContext(), getResources().getString(R.string.no_bluetooth_device),
                               Toast.LENGTH_SHORT
                              ).show();
                mTextView.setText(getResources().getString(R.string.no_bluetooth_device));
            }
            else
            {
                mTextView.setText(getResources().getString(R.string.section_format,
                                            mSectionNumber));
                if (!mBluetoothAdapter.isEnabled())
                {
                    Intent enableBlueTooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBlueTooth, REQUEST_CODE_ENABLE_BLUETOOTH);
                }
                else
                {
                    ((DeviceArrayAdapter) mListView.getAdapter()).clear();
                    addBluetoothKnownDevices();
                    addBluetoothNewDevices();
                }
            }

            return mRootView;
        }

        @Override
        public boolean onContextItemSelected(MenuItem item)
        {
            AdapterView.AdapterContextMenuInfo cmi    = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            BluetoothDevice                    device = mArrayAdapter.getItem(cmi.position);
            switch (item.getItemId())
            {
            case R.id.action_infos:
                break;
            case R.id.action_ping_device:
                Intent intent = new Intent(
                        getActivity().getApplicationContext(),
                        BluetoothClientServer.class);
                intent.putExtra("device", device);
                intent.putExtra("local", false);
                startActivity(intent);
                break;
            }
            return super.onContextItemSelected(item);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu,
                                        View v,
                                        ContextMenu.ContextMenuInfo menuInfo)
        {
            super.onCreateContextMenu(menu, v, menuInfo);
            getActivity().getMenuInflater().inflate(R.menu.device_menu,
                                                    menu);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data)
        {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode != REQUEST_CODE_ENABLE_BLUETOOTH)
                return;
            if (resultCode == 0)
                this.getActivity().finish();
            else
            {
                ((DeviceArrayAdapter) mListView.getAdapter()).clear();
                addBluetoothKnownDevices();
                addBluetoothNewDevices();
            }
        }

        private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                {
                    mProgressBar.setVisibility(View.VISIBLE);
                    getActivity().invalidateOptionsMenu();
                    ((BluetoothConnect) getActivity()).setScanningDevices(true);
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                {
                    mProgressBar.setVisibility(View.GONE);
                    getActivity().invalidateOptionsMenu();
                    ((BluetoothConnect) getActivity()).setScanningDevices(false);
                }
                else if (BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    mArrayAdapter.add((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                }
            }
        };

        private void addBluetoothNewDevices()
        {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            getContext().registerReceiver(mBluetoothReceiver, filter);
            mBluetoothAdapter.startDiscovery();
            getActivity().invalidateOptionsMenu();
            ((BluetoothConnect) getActivity()).setScanningDevices(true);
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();
            mBluetoothAdapter.cancelDiscovery();
            getActivity().invalidateOptionsMenu();
            ((BluetoothConnect) getActivity()).setScanningDevices(false);
            getContext().unregisterReceiver(mBluetoothReceiver);
        }

        private void addBluetoothKnownDevices()
        {
            mArrayAdapter.clear();
            mArrayAdapter.addAll(mBluetoothAdapter.getBondedDevices());
        }
    }

    public static class DeviceArrayAdapter
            extends BaseAdapter
    {
        private final Activity              mActivity;
        private final Context               mContext;
        private final List<BluetoothDevice> mDevices;
        private final MenuInflater          mMenuInflater;

        public static class ViewHolder
        {
            // I added a generic return type to reduce the casting noise in client code
            @SuppressWarnings("unchecked")
            public static <T extends View> T get(View view, int id)
            {
                SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
                if (viewHolder == null)
                {
                    viewHolder = new SparseArray<>();
                    view.setTag(viewHolder);
                }
                View childView = viewHolder.get(id);
                if (childView == null)
                {
                    childView = view.findViewById(id);
                    viewHolder.put(id, childView);
                }
                return (T) childView;
            }
        }

        public DeviceArrayAdapter(Context context)
        {
            mContext = context;
            mActivity = (Activity) context;
            mMenuInflater = mActivity.getMenuInflater();
            mDevices = new ArrayList<>();

            notifyDataSetChanged();
        }

        public DeviceArrayAdapter(Context context, BluetoothDevice[] devices)
        {
            this(context);

            Collections.addAll(mDevices, devices);
            notifyDataSetChanged();
        }

        public void add(BluetoothDevice device)
        {
            mDevices.add(device);
            notifyDataSetChanged();
        }

        public void addAll(Collection<? extends BluetoothDevice> devices)
        {
            mDevices.addAll(devices);
            notifyDataSetChanged();
        }

        public void clear()
        {
            mDevices.clear();
            notifyDataSetChanged();
        }

        public int getCount()
        {
            return mDevices.size();
        }

        // getItem(int) in Adapter returns Object but we can override
        // it to BananaPhone thanks to Java return type covariance
        @Override
        public BluetoothDevice getItem(int position)
        {
            return mDevices.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return mDevices.get(position).hashCode();
        }

        @Override
        public boolean isEnabled(int position)
        {
            return true;
        }

        @Override
        public boolean areAllItemsEnabled()
        {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {

            if (convertView == null)
            {
                convertView = LayoutInflater.from(mContext)
                                            .inflate(R.layout.rowitem, parent, false);
            }

            ImageView imgView  = ViewHolder.get(convertView, R.id.row_item_icon);
            TextView  nameView = ViewHolder.get(convertView, R.id.row_item_text);
            TextView  addrView = ViewHolder.get(convertView, R.id.row_item_address);

            BluetoothDevice device = getItem(position);
            nameView.setText(device.getName() == null
                             ? "Unknown" /* mContext.getApplicationContext().getResources().getString(R.string.unknown_device) */
                             : device.getName());
            addrView.setText(device.getAddress());
            if (device.getBluetoothClass().hasService(BluetoothClass.Service.AUDIO))
                imgView.setImageResource(R.drawable.bluetooth_class_audio);
            else if (device.getBluetoothClass().hasService(BluetoothClass.Service.CAPTURE))
                imgView.setImageResource(R.drawable.bluetooth_class_capture);
            else if (device.getBluetoothClass().hasService(BluetoothClass.Service.NETWORKING))
                imgView.setImageResource(R.drawable.bluetooth_class_networking);
            else if (device.getBluetoothClass().hasService(BluetoothClass.Service.OBJECT_TRANSFER))
                imgView.setImageResource(R.drawable.bluetooth_class_object_transfer);
            else if (device.getBluetoothClass().hasService(BluetoothClass.Service.POSITIONING))
                imgView.setImageResource(R.drawable.bluetooth_class_positioning);
            else if (device.getBluetoothClass().hasService(BluetoothClass.Service.RENDER))
                imgView.setImageResource(R.drawable.bluetooth_class_render);
            else if (device.getBluetoothClass().hasService(BluetoothClass.Service.TELEPHONY))
                imgView.setImageResource(R.drawable.bluetooth_class_telephony);

            return convertView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter
            extends FragmentPagerAdapter
    {
        private final int STATE_START            = 1;
        private final int STATE_DEVICE_SELECTED  = 2;
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
