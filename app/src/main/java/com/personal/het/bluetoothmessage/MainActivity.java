package com.personal.het.bluetoothmessage;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bAdapter;
    private ArrayAdapter<String> bArrayAdapter;
    private ListView lvPairedDevices;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        bArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
        textView = (TextView) findViewById(R.id.textView);

    }

    public void onStart(){
        super.onStart();
        checkBTStatus();
        getPairedDevices();
    }

    //Checks is the device supports Bluetooth and enables it
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

    public void getPairedDevices(){
        Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
        if(pairedDevices.size()>0) {
            for (BluetoothDevice device : pairedDevices) {
                bArrayAdapter.add(device.getName() + "/n" + device.getAddress());
            }
            lvPairedDevices.setAdapter(bArrayAdapter);
        } else {
            textView.setText(R.string.noPairedDevices);
        }
    }
}
