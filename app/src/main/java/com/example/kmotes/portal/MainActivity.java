package com.example.kmotes.portal;


import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String TAG = "BEACON_PROJECT";
    private ArrayList<String> beaconList;
    private ListView beaconListView;
    private ArrayAdapter<String> adapter;
    private BeaconManager beaconManager;
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SwitchCompat onOffSwitch = findViewById(R.id.on_off_switch);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v("Switch State=", ""+isChecked);
                if (isChecked) { ((TextView)findViewById(R.id.status)).setText("Active"); }
                else { ((TextView)findViewById(R.id.status)).setText("Off"); }
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
        beaconManager.setBackgroundScanPeriod(200);
        beaconManager.setForegroundScanPeriod(200);
        beaconManager.setBackgroundBetweenScanPeriod(200);
        beaconManager.setForegroundBetweenScanPeriod(200);

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
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    //beaconList.clear();
                    for(Iterator<Beacon> iterator = beacons.iterator(); iterator.hasNext();) {
                        Beacon tempBeacon = iterator.next();
                        if (tempBeacon.getId1().toString().equals("b9407f30-f5f8-466e-aff9-25556b57fd6e") || tempBeacon.getId1().toString().equals("b9407f30-f5f8-466e-aff9-25556b57fd6f")) {
                            //beaconList.add(tempBeacon.getId1().toString());
                            if (tempBeacon.getDistance() < 1.9411248985355725)
                            {
                                count += 1;
                                beaconList.add(0, "OPEN: " + count);
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
        });
        try {
            this.beaconManager.startRangingBeaconsInRegion(new Region("MyRegionId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
