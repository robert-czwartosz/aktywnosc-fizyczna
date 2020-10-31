package org.deeplearning4j.examples.aktywnosc_fiz;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;


import static android.content.ContentValues.TAG;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper sInstance;

    // Database Info
    private static final String DATABASE_NAME = "SensorDatabase.db";
    private static final int DATABASE_VERSION = 6;

    // Table Names
    private static final String TABLE_SENSOR = "Pomiary";
    private static final String TABLE_AKTYWNOSCI = "Aktywnosci";

    // Sensor Table Columns
    private static final String KEY_SENSOR_ID = "idRead";
    private static final String KEY_SENSOR_TIME = "Timestamp";
    private static final String KEY_SENSOR_ACCX = "PrzyspieszenieX";
    private static final String KEY_SENSOR_ACCY = "PrzyspieszenieY";
    private static final String KEY_SENSOR_ACCZ = "PrzyspieszenieZ";
    private static final String KEY_SENSOR_ACC = "Przyspieszenie";
    private static final String KEY_SENSOR_AKT = "Aktywnosc";

    // Aktywnosci Table Columns
    private static final String KEY_AKTYWNOSCI_ID = "idAkt";
    private static final String KEY_AKTYWNOSCI_DATA  = "Data";
    private static final String KEY_AKTYWNOSCI_AKT  = "Aktywnosc";


    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
    public static synchronized DatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Constructor should be private to prevent direct instantiation.
     * Make a call to the static method "getInstance()" instead.
     */
    private DatabaseHelper(Context context) {
        super(context, Environment.getExternalStorageDirectory().getPath() + "/AktywnoscFizycznaDB/" + DATABASE_NAME, null, DATABASE_VERSION);
    }


    // Called when the database is created for the FIRST time.
    // If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SENSOR_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SENSOR +
                "(" +
                KEY_SENSOR_ID + " INTEGER PRIMARY KEY autoincrement," + // Define a primary key
                KEY_SENSOR_TIME + " INTEGER," +
                KEY_SENSOR_ACCX + " NUMBER(10,2)," +
                KEY_SENSOR_ACCY + " NUMBER(10,2)," +
                KEY_SENSOR_ACCZ + " NUMBER(10,2)," +
                KEY_SENSOR_ACC + " NUMBER(10,2)," +
                KEY_SENSOR_AKT + " VARCHAR(50)" +
                ")";
        String CREATE_AKTYWNOSCI_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_AKTYWNOSCI +
                "(" +
                KEY_AKTYWNOSCI_ID + " INTEGER PRIMARY KEY autoincrement," + // Define a primary key
                KEY_AKTYWNOSCI_DATA + " VARCHAR(50)," +
                KEY_AKTYWNOSCI_AKT + " VARCHAR(50)" +
                ")";

        db.execSQL(CREATE_SENSOR_TABLE);
        db.execSQL(CREATE_AKTYWNOSCI_TABLE);
    }

    // Called when the database needs to be upgraded.
    // This method will only be called if a database already exists on disk with the same DATABASE_NAME,
    // but the DATABASE_VERSION is different than the version of the database that exists on disk.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // Simplest implementation is to drop all old tables and recreate them
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AKTYWNOSCI);
            onCreate(db);
        }
    }

    public long addAkt(String data, String Aktywnosc){
        SQLiteDatabase db = getWritableDatabase();
        long aktId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_AKTYWNOSCI_DATA, data);
            values.put(KEY_AKTYWNOSCI_AKT, Aktywnosc);

            aktId = db.insertOrThrow(TABLE_AKTYWNOSCI, null, values);
            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add activity");
        } finally {
            db.endTransaction();
        }
        return aktId;
    }

    public long addRead(SensorRead read) {
        // The database connection is cached so it's not expensive to call getWriteableDatabase() multiple times.
        SQLiteDatabase db = getWritableDatabase();
        long readId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_SENSOR_TIME, read.Timestamp);
            values.put(KEY_SENSOR_ACCX, read.AccelX);
            values.put(KEY_SENSOR_ACCY, read.AccelY);
            values.put(KEY_SENSOR_ACCZ, read.AccelZ);
            values.put(KEY_SENSOR_ACC, read.Accel);
            values.put(KEY_SENSOR_AKT, read.Aktywnosc);

            readId = db.insertOrThrow(TABLE_SENSOR, null, values);
            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add read");
        } finally {
            db.endTransaction();
        }
        return readId;
    }

    // truncate table Pomiary
    public void truncPomiary(){
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {

            db.execSQL("DELETE FROM " + TABLE_SENSOR + ";");
            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.d(TAG, "Error while trying to truncate");
        } finally {
            db.endTransaction();
        }
    }

    // truncate table Aktywnosci
    public void truncAktywnosci(){
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {

            db.execSQL("DELETE FROM " + TABLE_AKTYWNOSCI + ";");
            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.d(TAG, "Error while trying to truncate");
        } finally {
            db.endTransaction();
        }
    }
}
