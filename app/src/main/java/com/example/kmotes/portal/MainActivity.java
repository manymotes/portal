package com.example.kmotes.portal;


import android.Manifest;
import android.app.Activity;
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
import android.os.Looper;
import android.os.ParcelUuid;
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
import android.os.Looper;
import android.os.Handler;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String TAG = "BEACON_PROJECT";
    private ArrayList<String> beaconList;
    private ListView beaconListView;
    private ArrayAdapter<String> adapter;
    private BeaconManager beaconManager;
    int count = 0;
    private Handler mHandler;
    private boolean mScanning = true;
    private Boolean onOff = true;
    private Activity mainActivity;
    private Boolean scanning = false;
    private List<String> majorMinorQueue = new ArrayList<>();
    private BluetoothLeScannerCompat scanner;

    private int queueSize = 0;



    public class LooperThread extends Thread {
        public Handler handler;
        public void run(){
            Looper.prepare();
            handler = new Handler();
            //Looper.loop();

                 //if you want to stop thread you can create you own exception, throw it and catch. Example:
                 try
                 {
                       Looper.loop();
                 } catch(Exception e){
                    System.out.print("exception e caught");
                 }
////                 And now thread is stopping (and not handling runnables from handler.post()
////                 In other thread write this:
//                 thread.handler.post(new Runnable(){
//                          throw new MyException(); // And now exception will be caught and thread will be stopping.
//                 });

        }
    }





    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mainActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

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



        //start beacon code

        this.beaconList = new ArrayList<String>();
        this.beaconListView = (ListView) findViewById(R.id.listView);
        this.adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.beaconList);
        this.beaconListView.setAdapter(adapter);
        this.beaconManager = BeaconManager.getInstanceForApplication(this);
        this.beaconManager.getBeaconParsers().add(new BeaconParser(). setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        this.beaconManager.bind(this);
        beaconManager.setBackgroundScanPeriod(1000);
        beaconManager.setForegroundScanPeriod(1000);
        beaconManager.setBackgroundBetweenScanPeriod(1000);
        beaconManager.setForegroundBetweenScanPeriod(1000);

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

    @Override
    public void onBeaconServiceConnect() {
        this.beaconManager.setRangeNotifier(new RangeNotifier() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (onOff)
                {
                    if (beacons.size() > 0) {
                        majorMinorQueue.clear();
                        for (Iterator<Beacon> iterator = beacons.iterator(); iterator.hasNext(); ) {
                            Beacon tempBeacon = iterator.next();
                            if (tempBeacon.getId1().toString().equals("b9407f30-f5f8-466e-aff9-25556b57fd6e") || tempBeacon.getId1().toString().equals("b9407f30-f5f8-466e-aff9-25556b57fd6f")) {
                                //beaconList.add(tempBeacon.getId1().toString());
                                if (tempBeacon.getDistance() < 1.9) {
                                    count += 1;
                                    //beaconList.add(0, "OPEN: " + count);


                                        String tempMajorMinor = formatInt(tempBeacon.getId2().toString()) + formatInt(tempBeacon.getId3().toString());
                                        majorMinorQueue.add(tempMajorMinor);

                                }
                                else {
                                }
                            }
                        }

                        //write to pies here
                        writeToPi(majorMinorQueue);

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
    public void writeToPi(List<String> queue) {
                    if (scanner != null)
                    {
                        scanner.stopScan(mScanCallback);
                    }
                    scanner = BluetoothLeScannerCompat.getScanner();

                    ScanSettings settings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setReportDelay(1000)
                            .build();
                try {
                    scanner.startScan(uuidFilters(queue), settings, mScanCallback);

                    wait(1000);
                } catch (Exception e) {}


     //end writeToPI
    }

    private List<ScanFilter> uuidFilters(List<String> ids) {
        scanning = true;
        List<ScanFilter> toReturn = new ArrayList<>();

        for (int i  = 0; i < ids.size(); i++)
        {
            String toAdd = "D304DBD9-6FDC-4BF3-A617-E015" + ids.get(i);
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(UUID.fromString(toAdd))).build();
            toReturn.add(scanFilter);
        }
        return  toReturn;
    }
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // We scan with report delay > 0. This will never be called.
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (!results.isEmpty()) {
                for (int i = 0; i < results.size(); i++) {
                    ScanResult result = results.get(i);
                    BluetoothDevice device = result.getDevice();
                    String deviceAddress = device.getAddress();
                    // Device detected, we can automatically connect to it and stop the scan
                    device.connectGatt(getApplicationContext(), true,new BluetoothGattCallback() {

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
                                System.out.print("disconnect");
                                setContentView(R.layout.activity_main);
                                final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                                builder.setTitle("Bluetooth disconnected");
                                builder.setMessage("Please turn Bluetooth on and restart the app");
                                builder.setPositiveButton(android.R.string.ok, null);

                                builder.show();

                            }
                        }

                        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                            //Now we can start reading/writing characteristics
                                BluetoothGattService service = null;
                                BluetoothGattService tempService;
                                String serviceUsed = null;
                                for (int i = 0; i < majorMinorQueue.size(); i ++)
                                {
                                    String toAdd = "D304DBD9-6FDC-4BF3-A617-E015" + majorMinorQueue.get(i);
                                    tempService = gatt.getService(UUID.fromString(toAdd));
                                    if (tempService != null)
                                    {
                                        service = tempService;
                                        serviceUsed = toAdd;
                                    }
                                }
                                if (service == null) {
                                    System.out.println("service null");
                                }
                                else {
                                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("5099CBC8-A71F-4292-8158-BF4F25AE9948"));
                                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                                    if (characteristic == null) {
                                        System.out.println("characteristic null");
                                        return;
                                    }
                                    characteristic.setValue("GPIOON");
                                    gatt.writeCharacteristic(characteristic);
                                }



                        }
                    });
                    try {
                        wait(1000);
                    } catch (Exception e) {}
                }

            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            // Scan error
        }
    };

//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private void scanLeDevice(final boolean enable) {
//
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    invalidateOptionsMenu();
//                }
//            }, SCAN_PERIOD);
//
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        } else {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
//        invalidateOptionsMenu();
//    }


//    // Device scan callback.
//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//                private LooperThread thread = looperThread1;
//                {
//                    thread.start(); // start thread once
//                }
//                @Override
//                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
//                    if(thread.handler != null && !threadIsBusy)
//                        threadIsBusy = true;
//                        thread.handler.post(new Runnable() {
//                            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//                            @Override
//                            public void run() {
//                                // And now this code is running on seperate thread. (Not creating new)
//                                if (!isWriting) {
//                                    mLeDeviceListAdapter.addDevice(device);
//                                    mLeDeviceListAdapter.notifyDataSetChanged();
//                                }
//                                Log.e("LeScanCallback", Thread.currentThread().getName());//Prints Thread-xxxx
//                            }
//                        });
//                }
//            };


//    // Adapter for holding devices found through scanning.
//        private class LeDeviceListAdapter extends BaseAdapter {
//
//        private LayoutInflater mInflator;
//
//        public LeDeviceListAdapter() {
//            super();
//            mLeDevices = new ArrayList<BluetoothDevice>();
//        }
//
//        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//        public void addDevice(final BluetoothDevice device) {
//
//                if (device.getName() != null) {
//
//                    if (device.getName().equals("PORTAL")) {
//
//                        device.connectGatt(getApplicationContext(), true, mGattCallback);
//
//                    }
//                }
//
//        }





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
                System.out.print("disconnect");
                setContentView(R.layout.activity_main);
                final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                builder.setTitle("Bluetooth disconnected");
                builder.setMessage("Please turn Bluetooth on and restart the app");
                builder.setPositiveButton(android.R.string.ok, null);

                builder.show();

            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            //Now we can start reading/writing characteristics
            if (onOff) {

                    BluetoothGattService service = null;
                    BluetoothGattService tempService;
                    String serviceUsed = null;
                    for (int i = 0; i < majorMinorQueue.size(); i ++)
                    {
                        String toAdd = "D304DBD9-6FDC-4BF3-A617-E015" + majorMinorQueue.get(i);
                        tempService = gatt.getService(UUID.fromString(toAdd));
                        if (tempService != null)
                        {
                            service = tempService;
                            serviceUsed = toAdd;
                        }
                    }
                    if (service == null) {
                        System.out.println("service null");
                    }
                    else {
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("5099CBC8-A71F-4292-8158-BF4F25AE9948"));
                        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                        if (characteristic == null) {
                            System.out.println("characteristic null");
                            return;
                        }
                        characteristic.setValue("GPIOON");
                        gatt.writeCharacteristic(characteristic);
                    }



            }
        }
    };


    public static String formatInt (String n) {
        int addon = 4 - n.length();
        String hex = "";
        for (int i = 0; i < addon; i ++){
            hex += "0";
        }
        hex += n;

        return hex;
    }




}
//
//to do
//    increase ibeacon scan time by a lot and test, als warap around that with iswriting boolean.
//
//then handle two pis at once



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
