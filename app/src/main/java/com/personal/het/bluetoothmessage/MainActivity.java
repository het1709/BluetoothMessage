package com.personal.het.bluetoothmessage;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bAdapter;
    private BluetoothDevice bDevice;
    private ArrayAdapter<String> bPairedAdapter;
    private ArrayAdapter<String> bDiscoveredAdapter;
    private ListView lvPairedDevices;
    private ListView lvDiscoveredDevices;
    private EditText txtMessage;
    private TextView msgNoPaired;
    private TextView msgNoDiscovered;
    private TextView msgReceived;
    private boolean deviceSelected = false;

    private final UUID myUUID = UUID.fromString("f6ea2b4b-386d-4bcf-96e7-e3a4d2501a13");
    private final int MESSAGE_READ = 1;

    private Handler bHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == MESSAGE_READ){
                String message = msg.obj.toString();
                msgReceived.setText(message);
            }
        }
    };
    //private BroadcastReceiver bReceiver;

    //Sets up a BroadcastReceiver to handle various Bluetooth related Intents
    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //When discovery finds a device
            //Toast.makeText(getApplicationContext(), action, Toast.LENGTH_SHORT).show();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //Get BluetoothDevice from Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    Toast.makeText(getApplicationContext(), device.getName(), Toast.LENGTH_SHORT).show();
                    bDiscoveredAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Toast.makeText(getApplicationContext(), "Discovery has started successfully", Toast.LENGTH_SHORT).show();
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Toast.makeText(getApplicationContext(), "Discovery has ended", Toast.LENGTH_SHORT).show();
                if(bDiscoveredAdapter.isEmpty()) {
                    msgNoDiscovered.setVisibility(View.VISIBLE);
                }
            }
        }
    };


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
        msgNoDiscovered = (TextView) findViewById(R.id.msgNoDiscovered);
        msgReceived = (TextView) findViewById(R.id.msgReceived);
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        lvDiscoveredDevices.setAdapter(bDiscoveredAdapter);


        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bReceiver, filter);

        txtMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                txtMessage.setText("");
                return false;
            }
        });

        lvPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String device = bPairedAdapter.getItem(position);
                String deviceName =  device.substring(0, (device.length()) - 18);
                String address = device.substring(deviceName.length()+1, device.length());
                bDevice = bAdapter.getRemoteDevice(address);
                deviceSelected = true;
                Toast.makeText(getApplicationContext(), address, Toast.LENGTH_SHORT).show();
            }
        });
        checkBTStatus();
    }

    public void onStart(){
        super.onStart();
        msgNoPaired.setVisibility(View.INVISIBLE);
        msgNoDiscovered.setVisibility(View.INVISIBLE);
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
        if(bAdapter != null){
            bAdapter.cancelDiscovery();
        }
        unregisterReceiver(bReceiver);
    }

    //Starts discovery
    public void discover(View view){
        bDiscoveredAdapter.clear();
        bAdapter.cancelDiscovery();
        bAdapter.startDiscovery();
    }

    //Implement Server side connection thread
    public void connectAsServer(View view){
        AcceptThread server = new AcceptThread();
        server.start();
    }

    //Implement Client side connection thread
    public void connectAsClient(View view){
        if(!deviceSelected){
            Toast.makeText(getApplicationContext(), "Please select a device first", Toast.LENGTH_SHORT).show();
        } else {
            ConnectThread client = new ConnectThread(bDevice);
            client.start();
            deviceSelected = false;
        }
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

    /**Gets the Paired Devices*/
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

    /**Server side thread. NOTE: Server searched for the connection initiated by Client*/
    private class AcceptThread extends Thread{
        private final BluetoothServerSocket bServerSocket;

        public AcceptThread(){
            BluetoothServerSocket temp = null;
            try{
                temp = bAdapter.listenUsingRfcommWithServiceRecord("BluetoothMessage", myUUID);
            }catch(IOException e){}
            bServerSocket = temp;
        }

        public void run(){
            BluetoothSocket socket = null;
            while(true){
                try{
                    socket = bServerSocket.accept();
                }catch (IOException e){
                    break;
                }
            }
            if(socket != null){
                //Manage connection in a separate thread (the ConnectedThread)
                /*manageConnectedSocket(socket);
                mmServerSocket.close();
                break;*/
                ConnectedThread connectedThread = new ConnectedThread(socket);
                byte[] message = txtMessage.getText().toString().getBytes();
                connectedThread.write(message);
                try{
                    bServerSocket.close();
                }catch(IOException e){}
            }

        }

        //Cancels the listening socket and forces thread to close
        public void cancel(){
            try{
                bServerSocket.close();
            }catch (IOException e){}
        }
    }

    /**Client side thread. NOTE: Client initiates connection*/
    private class ConnectThread extends Thread{
        private final BluetoothSocket bSocket;
        private final BluetoothDevice bDevice;

        public ConnectThread(BluetoothDevice device){
            BluetoothSocket temp = null;
            bDevice = device;
            try{
                temp = bDevice.createRfcommSocketToServiceRecord(myUUID);
            }catch(IOException e){}
            bSocket = temp;
        }

        public void run(){
            bAdapter.cancelDiscovery();
            try{
                bSocket.connect();
            }catch(IOException connectException){
                try{
                    bSocket.close();
                }catch(IOException closeException){}
                return;
            }
            ConnectedThread connectedThread = new ConnectedThread(bSocket);
            connectedThread.start();
            //Manage the connection by implementing a separate thread (the ConnectedThread)
            //manageConnectedSocket(bSocket);
        }

        public void cancel(){
            try{
                bSocket.close();
            }catch(IOException e){}
        }
    }

    //Thread used to manage the connection
    private class ConnectedThread extends Thread{
        private final BluetoothSocket bSocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket){
            bSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;
            try{
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e){}
            inStream = tempIn;
            outStream = tempOut;
        }

        //Receive data. Used by client.
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes; //bytes returned from read();
            while(true){
                try{
                    bytes = inStream.read(buffer);
                    bHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch(IOException e){
                    break;
                }
            }
        }

        //Call from Main activity to send data
        public void write(byte[] bytes){
            try{
                outStream.write(bytes);
            }catch(IOException e){
                Toast.makeText(getApplicationContext(), "Message couldn't be sent", Toast.LENGTH_SHORT).show();
                cancel();
            }
        }

        public void cancel(){
            try{
                bSocket.close();
            }catch (IOException e){}
        }

    }
}