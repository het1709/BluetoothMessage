package com.personal.het.bluetoothmessage;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bAdapter;
    private ArrayAdapter<String> bPairedAdapter;
    private ArrayAdapter<String> bDiscoveredAdapter;
    private ListView lvPairedDevices;
    private ListView lvDiscoveredDevices;
    private EditText txtMessage;
    private TextView msgNoPaired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        bPairedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        bDiscoveredAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
        lvDiscoveredDevices = (ListView) findViewById(R.id.lvDiscoveredDevices);
        msgNoPaired = (TextView) findViewById(R.id.msgNoPaired);
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        txtMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                txtMessage.setText("");
                return false;
            }
        });
        checkBTStatus();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bReceiver, filter);
        //getPairedDevices();
    }

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //When discovery finds a device
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //Get BluetoothDevice from Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                bDiscoveredAdapter.add(device.getName() + "\n" + device.getAddress());
                bDiscoveredAdapter.notifyDataSetChanged();
            }
            lvDiscoveredDevices.setAdapter(bDiscoveredAdapter);
        }
    };


    public void onStart(){
        super.onStart();
        msgNoPaired.setVisibility(View.INVISIBLE);
        bPairedAdapter.clear();
        bDiscoveredAdapter.clear();
        bPairedAdapter.notifyDataSetChanged();
    }

    public void onResume(){
        super.onResume();
        msgNoPaired.setVisibility(View.INVISIBLE);
        getPairedDevices();
    }

    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(bReceiver);
    }

    public void startDiscovery(){
        bAdapter.startDiscovery();
    }

    //Checks if the device supports Bluetooth and enables it
    public void checkBTStatus(){
        if(bAdapter == null){
            Context context = getApplicationContext();
            CharSequence text = "Bluetooth is not available in this device";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            this.finishAffinity();
        }
        if(!bAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    //Gets the Paired Devices
    public void getPairedDevices(){
        Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
        if(pairedDevices.size()>0) {
            for (BluetoothDevice device : pairedDevices) {
                bPairedAdapter.add(device.getName() + "\n" + device.getAddress());
            }
            lvPairedDevices.setAdapter(bPairedAdapter);
        } else {
            msgNoPaired.setVisibility(View.VISIBLE);
        }
    }
}
