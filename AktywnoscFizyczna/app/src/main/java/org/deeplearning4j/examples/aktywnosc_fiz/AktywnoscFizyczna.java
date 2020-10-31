package org.deeplearning4j.examples.aktywnosc_fiz;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;


import android.content.Context;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

public class AktywnoscFizyczna extends AppCompatActivity{

    PowerManager.WakeLock wakeLock;

    private SensorActivity mSensorActivity;
    private SensorManager mSensorManager;

    SharedPreferences pref = null;
    SharedPreferences.Editor editor = null;

    Handler readSens;
    Runnable readS;
    Handler checkAct;
    Runnable checkA;

    MultiLayerNetwork net = null;
    INDArray output;
    Button wyczysc;
    Button wyczyscDB;
    ListView aktywnosci;
    TextView listLabel;

    float accel_x, accel_y, accel_z, accel;
    long Time;
    String Aktywnosc = "Brak aktywności";
    String Aktywnosc0 = "";
    String data;

    ArrayList<String> aktywnosciList;
    ArrayList<String> dateList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aktywnosc_fizyczna);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        if(!wakeLock.isHeld())
            wakeLock.acquire();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-mm-yyyy HH:mm:ss", Locale.getDefault());
        //String currentTime0 = dateFormat.format(new Date());
        //String currentTime = dateFormat.format(new Date());


        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        listLabel = findViewById(R.id.listLabel);
        wyczysc = findViewById(R.id.wyczysc);
        wyczyscDB = findViewById(R.id.wyczyscDB);
        aktywnosci = findViewById(R.id.aktywnosci);

        aktywnosciList = new ArrayList<String>();
        dateList = new ArrayList<String>();
        try {
            aktywnosciList = (ArrayList<String>) ObjectSerializer.deserialize(pref.getString("Aktywnosci", ObjectSerializer.serialize(new ArrayList<String>())));
            dateList = (ArrayList<String>) ObjectSerializer.deserialize(pref.getString("Daty", ObjectSerializer.serialize(new ArrayList<String>())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        CustomAdapter customAdapter = new CustomAdapter();
        //aktywnosciAdapter = new ArrayAdapter(this, R.layout.list_view_layout,  aktywnosciList);
        aktywnosci.setAdapter(customAdapter);

        wyczysc.setOnClickListener(view -> {
            aktywnosciList.clear();
            dateList.clear();
            customAdapter.notifyDataSetChanged();
        });

        wyczyscDB.setOnClickListener(view -> {
            aktywnosciList.clear();
            dateList.clear();
            databaseHelper.truncAktywnosci();
            customAdapter.notifyDataSetChanged();
        });

        mSensorActivity = new SensorActivity(this);

        final int TimeSerieLen = 60;
        final int nIn = 2;
        float[][][] pomiary = new float[1][nIn][TimeSerieLen];
        Dictionary labelDict = new Hashtable();

        AsyncTask.execute(() -> {

            for(int i=0;i<TimeSerieLen;i++) {
                pomiary[0][0][i] = 10;
                pomiary[0][1][i] = 0;
            }

            InputStream inputStream = getResources().openRawResource(R.raw.trained_seq_model);
            try {
                net = ModelSerializer.restoreMultiLayerNetwork(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                InputStream raw = getResources().openRawResource(R.raw.label_dict);
                InputStreamReader inputreader = new InputStreamReader(raw);
                BufferedReader br = new BufferedReader(inputreader);

                String line="12", label;
                int labelCode;
                while ((line = br.readLine()) != null){
                    System.out.println(line);
                    if(line.split(":").length > 1) {
                        label = line.split(":")[0];
                        labelCode = Integer.parseInt(line.split(":")[1]);
                        labelDict.put(labelCode, label);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            mSensorActivity.start();
            readSens.postDelayed(readS,1000);
            checkAct.postDelayed(checkA,2000);
        });


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

                    for(int i=0;i<TimeSerieLen-1;i++) {
                        pomiary[0][0][i] = pomiary[0][0][i+1];
                        pomiary[0][1][i] = pomiary[0][1][i+1];
                    }
                    pomiary[0][1][TimeSerieLen-1] = accel - pomiary[0][0][TimeSerieLen-1];
                    pomiary[0][0][TimeSerieLen-1] = accel;

                }
                readSens.postDelayed(this, 50);
            }
        };


        checkAct = new Handler();
        checkA = new Runnable() {
            @Override
            public void run() {
                if(net != null) {
                    output = net.output(Nd4j.create(pomiary));
                    Aktywnosc = labelDict.get(output.argMax(1).getInt(1, TimeSerieLen - 1)).toString();
                    if (Aktywnosc != Aktywnosc0) {
                        data = dateFormat.format(new Date());
                        databaseHelper.addAkt(data, Aktywnosc);
                        aktywnosciList.add(0, Aktywnosc);
                        dateList.add(0, data);
                        customAdapter.notifyDataSetChanged();
                        Aktywnosc0 = Aktywnosc;
                        //aktywnosc.setText("Aktywność\n" + labelDict.get(output.argMax(1).getInt(1, TimeSerieLen - 1)));
                    }
                }
                checkAct.postDelayed(this, 1000);
            }
        };

    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            editor.putString("Aktywnosci", ObjectSerializer.serialize(aktywnosciList));
            editor.putString("Daty", ObjectSerializer.serialize(dateList));
        } catch (IOException e) {
            e.printStackTrace();
        }


        editor.commit();

    }//End of onPause

    @Override
    protected void onDestroy(){
        super.onDestroy();
        wakeLock.release();
        mSensorActivity.stop();
        readSens.removeCallbacks(readS);
        checkAct.removeCallbacks(checkA);
    }
    @Override
    protected void onResume() {
        super.onResume();
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

    class CustomAdapter extends BaseAdapter{


        @Override
        public int getCount(){
            return  aktywnosciList.size();
        }

        @Override
        public Object getItem(int i){
            return null;
        }

        @Override
        public long getItemId(int i){
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup){

            view = getLayoutInflater().inflate(R.layout.list_view_layout, null);
            if(aktywnosciList.size() > i && dateList.size() > i){


                TextView textView_date = view.findViewById(R.id.textView_date);
                TextView textView_act = view.findViewById(R.id.textView_act);

                textView_date.setText(dateList.get(i));
                textView_act.setText(aktywnosciList.get(i));
            }
            return view;

        }
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

