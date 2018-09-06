package com.example.kmotes.portal;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String TAG = "BEACON_PROJECT";
    public static final String mDeviceAddress = "B8:27:EB:42:D6:B5";
    private static final int REQUEST_ENABLE_BT = 1;
    private ArrayList<String> beaconList;
    private ListView beaconListView;
    private ArrayAdapter<String> adapter;
    private BeaconManager beaconManager;
    int count = 0;
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Handler mHandler;
    private boolean mScanning = true;
    // Stops scanning after 1 seconds.
    private static final long SCAN_PERIOD = 5900;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<BluetoothDevice> mLeDevices;
    private String majorMinor;
    private BluetoothGatt bluetoothGatt;
    private Boolean onOff = true;
    private Boolean inRange = false;





    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        SwitchCompat onOffSwitch = findViewById(R.id.on_off_switch);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v("Switch State=", ""+isChecked);
                if (isChecked) {
                    ((TextView)findViewById(R.id.status)).setText("Active");
                    onOff = true;
                }
                else {
                    ((TextView)findViewById(R.id.status)).setText("Off");
                    onOff = false;
                }
            }

        });

        mLeDeviceListAdapter = new LeDeviceListAdapter();


        //start beacon code

        this.beaconList = new ArrayList<String>();
        this.beaconListView = (ListView) findViewById(R.id.listView);
        this.adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.beaconList);
        this.beaconListView.setAdapter(adapter);
        this.beaconManager = BeaconManager.getInstanceForApplication(this);
        this.beaconManager.getBeaconParsers().add(new BeaconParser(). setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        this.beaconManager.bind(this);
        beaconManager.setBackgroundScanPeriod(1);
        beaconManager.setForegroundScanPeriod(1);
        beaconManager.setBackgroundBetweenScanPeriod(5900);
        beaconManager.setForegroundBetweenScanPeriod(5900);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    }
                });
                builder.show();
            }
        }


        //end beacon code
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.beaconManager.unbind(this);
    }

    public void searchIbeacon() {

    }

    @Override
    public void onBeaconServiceConnect() {
        this.beaconManager.setRangeNotifier(new RangeNotifier() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (onOff)
                {
                    if (beacons.size() > 0) {
                        //beaconList.clear();
                        for (Iterator<Beacon> iterator = beacons.iterator(); iterator.hasNext(); ) {
                            Beacon tempBeacon = iterator.next();
                            if (tempBeacon.getId1().toString().equals("b9407f30-f5f8-466e-aff9-25556b57fd6e") || tempBeacon.getId1().toString().equals("b9407f30-f5f8-466e-aff9-25556b57fd6f")) {
                                //beaconList.add(tempBeacon.getId1().toString());
                                if (tempBeacon.getDistance() < 1.1) {
                                    count += 1;
                                    //beaconList.add(0, "OPEN: " + count);
                                    inRange = true;
                                    writeToPi(tempBeacon.getId2().toString(), tempBeacon.getId3().toString());
                                }
                                else {
                                    inRange = false;
                                }
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        });
        try {
            this.beaconManager.startRangingBeaconsInRegion(new Region("MyRegionId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }





    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void writeToPi(String major, String minor) {

        major = intToBinary(Integer.valueOf(major), 4).toString();
        minor = intToBinary(Integer.valueOf(minor), 4).toString();
        majorMinor = "D304DBD9-6FDC-4BF3-A617-E015" + major + minor;
      //final  UUID serviceUUID = UUID.fromString(majorMinor);
     //   UUID characteristicUUID = UUID.fromString("5099CBC8-A71F-4292-8158-BF4F25AE9948");


//
//
//
//
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//
//
//        if (mBluetoothLeService != null) {
//            mBluetoothLeService.writeCustomCharacteristic(0xAA);
//        }
//


            scanLeDevice(true);



     //end writeToPI
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final boolean enable) {

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                        @Override
                        public void run() {

                                    mLeDeviceListAdapter.addDevice(device);
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                }


                    });
                }
            };

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {

        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void addDevice(final BluetoothDevice device) {

                if (device.getName() != null) {

                    if (device.getName().equals("PORTAL")) {

                        device.connectGatt(getApplicationContext(), true, mGattCallback);

                    }
                }

        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {


            return null;
        }
    }


    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //Connection established
            if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_CONNECTED) {

                //Discover services
                gatt.discoverServices();

            } else if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_DISCONNECTED) {

                //Handle a disconnect event

            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            //Now we can start reading/writing characteristics
            if (onOff && inRange) {

            BluetoothGattService service = gatt.getService(UUID.fromString(majorMinor));
            if (service == null) {
                System.out.println("service null"); return;
            }


            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("5099CBC8-A71F-4292-8158-BF4F25AE9948"));
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            if (characteristic == null) {
                System.out.println("characteristic null"); return;
            }
            characteristic.setValue("0xAA");


                    gatt.writeCharacteristic(characteristic);
                    //mBluetoothLeService.writeCustomCharacteristic(0xAA);
//                    try {
//                        TimeUnit.MILLISECONDS.sleep(1643);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }


        }
    };


    public static String intToBinary (int n, int numOfBits) {
        String binary = "";
        for(int i = 0; i < numOfBits; ++i, n/=2) {
            switch (n % 2) {
                case 0:
                    binary = "0" + binary;
                    break;
                case 1:
                    binary = "1" + binary;
                    break;
            }
        }

        return binary;
    }




}



/*
distnace notes

3 inchase away is 0.0031161370689476646 mDistance

10 ft away is 1.9411248985355725

20 feet away is 2.0334158400692743

id1: b9407f30-f5f8-466e-aff9-25556b57fd6e id2: 0 id3: 0
beacon@5008

id1: b9407f30-f5f8-466e-aff9-25556b57fd6f id2: 0 id3: 0
beacon@5069
mBluetoothAddress B8:27:EB:42:D6:B5

 */





//scan for ibeacon, get the major and minor vlaues

// and major and minor to the uuid

// let serviceUUID = [CBUUID(string: "D304DBD9-6FDC-4BF3-A617-E015" + majorValue + minorValue)]

// look for characteristic id

// peripheral.discoverCharacteristics([CBUUID.init(string :"5099CBC8-A71F-4292-8158-BF4F25AE9948"), CBUUID.init(string :"5099CBC8-A71F-4292-8158-BF4F25AE9949")], for: peripheral.services![0])

//then turn on that characteristic

//   peripheral.writeValue("GPIO_ON".data(using: .utf8)!, for: service.characteristics![0], type: .withoutResponse)

// 5-10 feet open is optimal

//look at power value to find distance

//pi uses near setting for ibeacon to turn (far intermedieat and near)

//phone will look
