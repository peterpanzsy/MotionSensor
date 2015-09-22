package com.xjtu.zsy.motionsensor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyService extends Service implements SensorEventListener {
    private String data = "default info";
    private SensorManager sensorManager;
    public LocationManager mLocationManager;
    private LocationListener locationListener;
    private File accFile;
    private File gyroFile;
    private File gravityFile;
    private File magneticFile;
    private File gpsFile;
    private FileOutputStream accFos;
    private FileOutputStream gyroFos;
    private FileOutputStream gravityFos;
    private FileOutputStream magneticFos;
    private FileOutputStream gpsFos;
    private OutputStreamWriter accOsw;
    private OutputStreamWriter gyroOsw;
    private OutputStreamWriter gravityOsw;
    private OutputStreamWriter magneticOsw;
    private OutputStreamWriter gpsOsw;
    private float[] gravity = new float[3];
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new Binder();
    }

    public class Binder extends android.os.Binder{
        public void setData(String data){
            MyService.this.data = data;
        }
        public MyService getSerivce(){
            return MyService.this;
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        data = intent.getStringExtra("data");
        return super.onStartCommand(intent, flags, startId);
    }

    //service只创建一次
    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        createFile();
//        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        recordGPS(location);
        startGPSRequest();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            accOsw.flush();
            accOsw.close();
            accFos.close();
            gyroOsw.flush();
            gyroOsw.close();
            gyroFos.close();
            gravityOsw.flush();
            gravityOsw.close();
            gravityFos.close();
            magneticOsw.flush();
            magneticOsw.close();
            magneticFos.close();
            gpsOsw.flush();
            gpsOsw.close();
            gpsFos.close();
        }catch (Exception e){

        }
        sensorManager.unregisterListener(this);//停止传感器监听
        mLocationManager.removeUpdates(locationListener);//停止GPS实时捕获
    }

    private Callback callback = null;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public Callback getCallback() {
        return callback;
    }

    public static interface Callback{
        void onDataChange(String key, String data);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd:HH:mm:ss:SSS");
        String time = df.format(new Date());
        try {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    final float alpha = (float) 0.8;
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
                    //String accelerator = "accelerated velocity \n"+"X:"+(event.values[0] - gravity[0])+"\nY:"+(event.values[1]-gravity[1])+"\nZ:"+(event.values[2] - gravity[2]);
                    String accelerator = time + " " + (event.values[0]) + " " + (event.values[1]) + " " + (event.values[2] + "\n");
                    if(callback!=null){
                        callback.onDataChange(getResources().getString(R.string.tvAccelerator),accelerator);
                    }
                    accOsw.write(accelerator);
                    break;
                case Sensor.TYPE_GRAVITY:
                    gravity[0] = event.values[0];
                    gravity[1] = event.values[1];
                    gravity[2] = event.values[2];
                    String gravity = time + " " + event.values[0] + " " + event.values[1] + " " + event.values[2]  + "\n";
                    if(callback!=null){
                        callback.onDataChange(getResources().getString(R.string.tvGravity),gravity);
                    }
                    gravityOsw.write(gravity);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    String gyroscope = time + " " + event.values[0] + " " + event.values[1] + " " + event.values[2]  + "\n";
                    if(callback!=null){
                        callback.onDataChange(getResources().getString(R.string.tvGyroscope),gyroscope);
                    }
                    gyroOsw.write(gyroscope);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    String magnetic =time + " " + event.values[0] + " " + event.values[1] + " " + event.values[2]  + "\n";
                    if(callback!=null){
                        callback.onDataChange(getResources().getString(R.string.tvMagnetic),magnetic);
                    }
                    magneticOsw.write(magnetic);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void startGPSRequest(){
         locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                recordGPS(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                recordGPS(mLocationManager.getLastKnownLocation(provider));
            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, locationListener);
    }
    public void recordGPS(Location newLocation){
        if(newLocation != null){
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd:HH:mm:ss:SSS");
            String time = df.format(new Date());
            String gps = time+" "+newLocation.getLongitude()+" "+newLocation.getLatitude()+" "+newLocation.getAltitude()+" "
                    +newLocation.getSpeed()+" "+newLocation.getBearing()+"\n";
            try {
                if(callback!=null){
                    callback.onDataChange(getResources().getString(R.string.tvGPS),gps);
                }
                gpsOsw.write(gps);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public  void createFile(){
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String time = df.format(new Date());
        File sdCard = Environment.getExternalStorageDirectory();
        //sensorFile = new File(sdCard,"sensor.txt");
        File dir = new File(sdCard.getPath()+"/motionSensor/"+time);
        if(!dir.exists()){
            dir.mkdirs();
        }
        accFile = new File(dir,"ACCELEROMETER.txt");
        gyroFile = new File(dir,"GYROSCOPE.txt");
        gravityFile = new File(dir,"GRAVITY.txt");
        magneticFile = new File(dir,"MAGNETIC.txt");
        gpsFile = new File(dir,"GPS.txt");
        if(!sdCard.exists()){
            Toast.makeText(getApplicationContext(), "sdcard doesn't exist !", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            accFile.createNewFile();
            accFos = new FileOutputStream(accFile);
            accOsw = new OutputStreamWriter(accFos);
            accOsw.write("TIME X Y Z\n");
            gyroFile.createNewFile();
            gyroFos = new FileOutputStream(gyroFile);
            gyroOsw = new OutputStreamWriter(gyroFos);
            gyroOsw.write("TIME X Y Z\n");
            gravityFile.createNewFile();
            gravityFos = new FileOutputStream(gravityFile);
            gravityOsw = new OutputStreamWriter(gravityFos);
            gravityOsw.write("TIME X Y Z\n");
            magneticFile.createNewFile();
            magneticFos = new FileOutputStream(magneticFile);
            magneticOsw = new OutputStreamWriter(magneticFos);
            magneticOsw.write("TIME X Y Z\n");
            gpsFile.createNewFile();
            gpsFos = new FileOutputStream(gpsFile);
            gpsOsw = new OutputStreamWriter(gpsFos);
            gpsOsw.write("TIME Longitude Latitude Altitude Speed Bearing\n");
            Toast.makeText(getApplicationContext(),"files created!",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
