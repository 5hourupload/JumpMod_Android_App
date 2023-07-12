package hci.jumpmod;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    BluetoothGatt mGatt;
    BluetoothGattService service;
    BluetoothGattCharacteristic voltageCharacteristic;
    BluetoothGattCharacteristic positionCharacteristic;
    BluetoothGattCharacteristic commandCharacteristic;
    BluetoothGattCharacteristic triggerCharacteristic;

    TextView debugTextView;
    TextView voltage;
    TextView position;
    Button calibrate;

    BluetoothDevice device;

    String reading = "voltage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_ADMIN, android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},1);
        }

        NumberPicker picker = findViewById(R.id.number_picker);
        String[] data = new String[]{"Jump Higher", "Pulled Higher", "Pulled Lower", "Land Harder", "Land Softer"};
        picker.setMinValue(0);
        picker.setMaxValue(data.length-1);
        picker.setDisplayedValues(data);
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (commandCharacteristic == null) return;

                if (newVal == 0) commandCharacteristic.setValue("x,1");
                if (newVal == 1) commandCharacteristic.setValue("x,2");
                if (newVal == 2) commandCharacteristic.setValue("x,3");
                if (newVal == 3) commandCharacteristic.setValue("x,4");
                if (newVal == 4) commandCharacteristic.setValue("x,5");

                mGatt.writeCharacteristic(commandCharacteristic);

            }
        });


        debugTextView = findViewById(R.id.debug);
        voltage = findViewById(R.id.voltage);
        position = findViewById(R.id.position);
        TextView sendCommand = findViewById(R.id.send_command);
        EditText commandTextField = findViewById(R.id.command);
        sendCommand.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if (commandCharacteristic == null) return;

                commandCharacteristic.setValue(commandTextField.getText().toString());
                mGatt.writeCharacteristic(commandCharacteristic);
                System.out.println("Sending: " + commandTextField.getText().toString());
            }
        });


        Button rescan = findViewById(R.id.rescan);
        rescan.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if(!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();
                }
                mBluetoothAdapter.startDiscovery();
            }
        });
        calibrate = findViewById(R.id.calibrate);
        calibrate.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if (commandCharacteristic == null) return;

                commandCharacteristic.setValue("c");
                mGatt.writeCharacteristic(commandCharacteristic);
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

            @SuppressLint("MissingPermission")
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                gatt.discoverServices();
                reading = "voltage";

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            debugTextView.setText("Connected");
                        }
                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mGatt = device.connectGatt(getApplicationContext(), false, this, TRANSPORT_LE);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            debugTextView.setText("Not Connected");
                        }
                    });
                }
            }

            @Override
            public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
                super.onCharacteristicRead(gatt, characteristic, value, status);
                System.out.println("READ!!!");
            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
                super.onCharacteristicChanged(gatt, characteristic, value);
                System.out.println("Changed!!!");
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                final List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService s : services)
                {
                    String uuid = s.getUuid().toString();
                    Log.i("bluetooth", "Service Found: " + uuid);

                    if (uuid.equals("4fafc201-1fb5-459e-8fcc-c5c9c331914b"))
                    {
                        service = s;
                        for (BluetoothGattCharacteristic mCharacteristic : s.getCharacteristics()) {
                            Log.i("idk", "Found Characteristic: " + mCharacteristic.getUuid().toString());
                            if (mCharacteristic.getUuid().toString().equals("9c5a211f-d400-490b-9465-52f7a417d19c"))
                            {
                                voltageCharacteristic = mCharacteristic;
                                System.out.println("Voltage characteristic established");
                            }
                            if (mCharacteristic.getUuid().toString().equals("3deefe33-d8f5-45a8-a24c-8fec01486b03"))
                            {
                                positionCharacteristic = mCharacteristic;
                                System.out.println("Position characteristic established");
                            }
                            if (mCharacteristic.getUuid().toString().equals("1fee8942-0f64-11ed-861d-0242ac120002"))
                            {
                                triggerCharacteristic = mCharacteristic;
                                System.out.println("Trigger characteristic established");
                            }
                            if (mCharacteristic.getUuid().toString().equals("beb5483e-36e1-4688-b7f5-ea07361b26a8"))
                            {
                                commandCharacteristic = mCharacteristic;
                                System.out.println("Command characteristic established");
                            }
                        }
                    }
                }
            }
        };


        mBluetoothAdapter.startDiscovery();
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Finding devices
                if (BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    if (foundDevice.getAddress().equals("45:99:23:A4:82:7A"))
                    {
                        device = foundDevice;

                        System.out.println("FOUND:" + device.getAddress());
//                        mBluetoothAdapter.cancelDiscovery();
                        boolean bonded = false;
                        try {
                            bonded = createBond(device);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        if (bonded)
                            mGatt = device.connectGatt(getApplicationContext(), false, bluetoothGattCallback,TRANSPORT_LE);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);


        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @SuppressLint("MissingPermission")
            public void run() {


                if (reading.equals("voltage"))
                {
                    if (mGatt != null && voltageCharacteristic != null)
                    {
                        mGatt.readCharacteristic(voltageCharacteristic);
                        String vChara = voltageCharacteristic.getStringValue(0);
                        voltage.setText("Voltage: " + vChara);

                        if (vChara != null)
                            reading = "position";
                    }

                    handler.postDelayed(this, 1000);
                }
                if (reading.equals("position"))
                {
                    if (mGatt != null && positionCharacteristic != null)
                    {
                        mGatt.readCharacteristic(positionCharacteristic);
                        String pChara = positionCharacteristic.getStringValue(0);
                        position.setText("Position: " + pChara);
                    }
                    handler.postDelayed(this, 200);
                }
            }
        };
        runnable.run();

    }
    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }
}