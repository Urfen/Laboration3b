package se.arvidbodkth.laboration3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button buttonStart, buttonStop;
    private ScrollView scrollView;
    private TextView dataView;
    private EditText editText;
    private DataSender model;
    private File dataFile;

    private ArrayList<String> dataArray;

    public static final int REQUEST_ENABLE_BT = 42;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice noninDevice = null;
    private BluetoothIOTask bluetoothIOTask;

    private File file;
    FileWriter fileWriter;
    BufferedWriter bufferedWriter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editText = (EditText) findViewById(R.id.editText);
        dataView = (TextView) findViewById(R.id.textView);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        dataView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("This device do not support Bluetooth");
            this.finish();
        }

        //The data array.
        dataArray = new ArrayList<>();

        //Create a public file.
        file = new File(Environment.getExternalStorageDirectory(), "data.txt");

    }


    @Override
    protected void onStart() {
        super.onStart();
        dataView.setText(R.string.data);
        initBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothIOTask.cancel(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothIOTask.cancel(true);
        // TODO: stop ongoing BT communication
    }

    public void onPollButtonClicked(View view) {

        if (noninDevice != null) {
            bluetoothIOTask = new BluetoothIOTask(this, noninDevice, getApplicationContext());
            bluetoothIOTask.execute();
        } else {
            showToast("No Nonin sensor found");
        }
    }

    public void onStopButtonClicked(View view) {
        bluetoothIOTask.cancel(true);

        writeToFile();

        System.out.println(file.getPath() +" Can write: "+ file.canWrite());

        TcpTask tcpTask = new TcpTask();

        StringBuilder dataString = new StringBuilder();

        for (int i = 0; i < dataArray.size(); i++) {
            dataString.append(dataArray.get(i));
        }

        tcpTask.execute(dataString.toString());

    }

    protected void displayData(CharSequence data) {
        String splitData = data.toString();
        String[] splitedData = splitData.split(";");

        dataView.append(splitedData[0] + "\n");
        dataArray.add(splitedData[1] + ";");
    }


    public void writeToFile() {
        showToast(" " + dataArray.size());
        if (dataArray.size() > 0) {

            try {
                //open output outpus stream
                fileWriter = new FileWriter(file);
                bufferedWriter = new BufferedWriter(fileWriter);

                for (int i = 0; i < dataArray.size(); i++) {
                    bufferedWriter.write(dataArray.get(i));
                }

                showToast("Saved to file.");

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if(bufferedWriter != null){
                    try {
                        bufferedWriter.close();
                        fileWriter.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private void initBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            getNoninDevice();
        }
    }

    // callback for BluetoothAdapter.ACTION_REQUEST_ENABLE (called via
    // initBluetooth)
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent result) {
        super.onActivityResult(requestCode, resultCode, result);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (bluetoothAdapter.isEnabled()) {
                getNoninDevice();
            } else {
                showToast("Bluetooth is turned off.");
            }
        }
    }

    private void getNoninDevice() {
        noninDevice = null;
        Set<BluetoothDevice> pairedBTDevices = bluetoothAdapter
                .getBondedDevices();
        if (pairedBTDevices.size() > 0) {
            // the last Nonin device, if any, will be selected...
            for (BluetoothDevice device : pairedBTDevices) {
                String name = device.getName();
                if (name.contains("Nonin")) {
                    noninDevice = device;
                    showToast("Paired device: " + name);
                    return;
                }
            }
        }
        if (noninDevice == null) {
            showToast("No paired Nonin devices found!\r\n"
                    + "Please pair a Nonin BT device with this device.");
        }
    }

    public void showToast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}

