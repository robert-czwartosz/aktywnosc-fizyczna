package org.deeplearning4j.examples.aktywnosc_fiz;

public class SensorRead {
    public long Timestamp;
    public float AccelX;
    public float AccelY;
    public float AccelZ;
    public float Accel;
    public String Aktywnosc;
    public SensorRead(long timestamp, float accelX, float accelY, float accelZ, float accel, String Akt){
        Timestamp = timestamp;
        AccelX = accelX;
        AccelY = accelY;
        AccelZ = accelZ;
        Accel = accel;
        Aktywnosc = Akt;
    }
}
