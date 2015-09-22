package com.xjtu.zsy.motionsensor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public class MainActivity extends Activity implements View.OnClickListener,ServiceConnection {
    private MyService.Binder binder;
//    private File sensorFile;
    private TextView tvAccelerator;
    private TextView tvGyroscope;
    private TextView tvGravity;
    private TextView tvMagnetic;
    private TextView tvGPS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnBindService).setOnClickListener(this);
        findViewById(R.id.btnUnBindService).setOnClickListener(this);

        tvAccelerator = (TextView)findViewById(R.id.tvAccelerator);
        tvGravity = (TextView) findViewById(R.id.tvGravity);
        tvGyroscope = (TextView) findViewById(R.id.tvGyroscope);
        tvMagnetic = (TextView) findViewById(R.id.tvMagnetic);
        tvGPS = (TextView) findViewById(R.id.tvGPS);

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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder= (MyService.Binder) service;
        binder.getSerivce().setCallback(new MyService.Callback() {
            @Override
            public void onDataChange(String tv, String data) {
                Message msg = new Message();
                Bundle b = new Bundle();
                b.putString("tv", tv);
                b.putString("data", data);
                msg.setData(b);
                handler.sendMessage(msg);
            }
        });
        Location location =binder.getSerivce().mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        binder.getSerivce().recordGPS(location);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnBindService:
                bindService(new Intent(this,MyService.class),this, Context.BIND_AUTO_CREATE);
                break;
            case R.id.btnUnBindService:
                unbindService(this);
                break;
        }
    }
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String tv = msg.getData().getString("tv");
            String value = msg.getData().getString("data");
            if (tv.equals(getResources().getString(R.string.tvAccelerator))){
                tvAccelerator.setText(value);
            }else if(tv.equals(getResources().getString(R.string.tvGyroscope))){
                tvGyroscope.setText(value);
            }else if(tv.equals(getResources().getString(R.string.tvGravity))){
                tvGravity.setText(value);
            }else if(tv.equals(getResources().getString(R.string.tvMagnetic))){
                tvMagnetic.setText(value);
            }else if(tv.equals(getResources().getString(R.string.tvGPS))){
                tvGPS.setText(value);
            }
        }
    };
}
