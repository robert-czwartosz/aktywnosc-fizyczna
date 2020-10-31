package org.deeplearning4j.examples.aktywnosc_fiz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

public class Pomiary extends AppCompatActivity implements AdapterView.OnItemSelectedListener, Switch.OnCheckedChangeListener{

    PowerManager.WakeLock wakeLock;

    private SensorActivity mSensorActivity;
    private SensorManager mSensorManager;

    SharedPreferences pref = null;
    SharedPreferences.Editor editor = null;

    Handler readSens;
    Runnable readS;

    TextView acc;

    Button resetButton;
    Switch startStop;
    Spinner dropdown;
    ArrayAdapter<String> adapter;

    float accel_x, accel_y, accel_z, accel;
    long Time;
    String Aktywnosc = "Brak aktywności";

    //SQLiteDatabase db;
    String myDbPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomiary);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        if(!wakeLock.isHeld())
            wakeLock.acquire();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        acc = findViewById(R.id.acc);

        startStop = findViewById(R.id.startStop);
        startStop.setTextOff("Zbieranie pomiarów wyłączone");
        startStop.setTextOn("Zbieranie pomiarów włączone");
        startStop.setChecked(pref.getBoolean("startedSensor", false));
        resetButton = findViewById(R.id.reset);

        String directDB = Environment.getExternalStorageDirectory().getPath() + "/AktywnoscFizycznaDB/";
        final File newFile = new File(directDB);
        if(!newFile.exists()){
            newFile.mkdir();
        }
        //acc.setText(directDB );
        // Get singleton instance of database
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);

        mSensorActivity = new SensorActivity(this);

        //get the spinner from the xml.
        dropdown = findViewById(R.id.aktywnoscSpin);
        //create a list of items for the spinner.
        String[] items = new String[]{"Brak aktywności", "Chodzenie", "Bieganie", "Jazda na rowerze"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(this);

        readSens = new Handler();
        readS = new Runnable() {
            @Override
            public void run() {
                if(mSensorActivity!=null)
                {
                    Time = mSensorActivity.Timestamp;
                    accel_x = mSensorActivity.acc[0];
                    accel_y = mSensorActivity.acc[1];
                    accel_z = mSensorActivity.acc[2];
                    accel = (float) Math.sqrt(accel_x*accel_x + accel_y*accel_y + accel_z*accel_z);
                    //acc.setText(accel_x + "\n" + accel_y + "\n" + accel_z + "\n" + accel);
                    acc.setText("Przyspieszenie\n" + accel);
                    // Add sensor read to the database
                    databaseHelper.addRead(new SensorRead(Time, accel_x, accel_y, accel_z, accel, Aktywnosc));
                }
                readSens.postDelayed(this, 50);
            }
        };
        //readSens.postDelayed(readS, 1000);
        startStop.setOnCheckedChangeListener(this);

        resetButton.setOnClickListener(view -> {
            databaseHelper.truncPomiary();
        });

        startStop.setChecked(pref.getBoolean("startedSensor", false));
        if(startStop.isChecked()) {
            startStop.setText("Zbieranie pomiarów włączone");
            mSensorActivity.start();
            readSens.postDelayed(readS, 100);
        }
        else{
            startStop.setText("Zbieranie pomiarów wyłączone");
            mSensorActivity.stop();
            readSens.removeCallbacks(readS);
        }

    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        editor.putBoolean("startedSensor", startStop.isChecked()); // Storing boolean - true/false
        editor.commit();
        if(isChecked) {
            startStop.setText("Zbieranie pomiarów włączone");
            mSensorActivity.start();
            readSens.postDelayed(readS, 100);
        }
        else{
            startStop.setText("Zbieranie pomiarów wyłączone");
            mSensorActivity.stop();
            readSens.removeCallbacks(readS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        editor.putBoolean("startedSensor", startStop.isChecked()); // Storing boolean - true/false
        editor.putString("Aktywnosc", Aktywnosc); // Storing string
        editor.commit(); // commit changes


    }//End of onPause
    @Override
    protected void onDestroy(){
        super.onDestroy();
        wakeLock.release();
        startStop.setText("Zbieranie pomiarów wyłączone");
        mSensorActivity.stop();
        readSens.removeCallbacks(readS);
        Log.d("SENSORR", "OFF");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Aktywnosc = pref.getString("Aktywnosc", "Brak aktywności"); // getting String
        int spinnerPosition = adapter.getPosition(Aktywnosc);
        dropdown.setSelection(spinnerPosition);

        if(!wakeLock.isHeld())
            wakeLock.acquire();

    }//End of onResume

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        Aktywnosc = (String) parent.getItemAtPosition(position);

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public class SensorActivity implements SensorEventListener {

        private Sensor mAccelerometer;
        private long Timestamp;
        float[] acc;

        SensorActivity(Context context) {
            super();
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            //isAccelerometerSupported = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            acc = new float[3];

        }

        void stop(){
            mSensorManager.unregisterListener(this);
        }

        void start(){
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        @Override
        public void onSensorChanged(SensorEvent event) {

            Timestamp = System.currentTimeMillis();

            switch(event.sensor.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                    acc = event.values.clone();
                    break;
            }

        }


    }

}

