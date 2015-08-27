package vvr.breathrecorder;

import java.util.Vector;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Chronometer;
import vvr.breathrecorder.sensors.AccelSensor;
import vvr.breathrecorder.sensors.GyroSensor;
import vvr.breathrecorder.sensors.SensorData;
import vvr.breathrecorder.sensors.SensorListener;

public class AccelActivity extends Activity implements SensorListener {

    public static final String SERVER_URL  = "http://upat.notremor.eu/tremor_data_insert";
    public static final String LOGTAG      = "accel";
    public static final int    BUFFER_SIZE = 5000;

    /* UI */
    private boolean     mRunning;
    private View        mLayoutTop;
    private Chronometer mChronometer;
    private AccelSensor mAccelSensor;
    private GyroSensor mGyroSensor;

    /* Spectrum Calculation */
    Vector<SensorData> mBuff_Accel;
    Vector<SensorData> mBuff_Gyro;
    long mt0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        /* Disable strict mode */
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        /* Find views and set listeners. */
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mLayoutTop = findViewById(R.id.layout_top);
        mLayoutTop.setBackgroundResource(R.color.mainSurfaceBgColor);

        /* Init */
        mBuff_Accel = new Vector<SensorData>();
        mBuff_Gyro = new Vector<SensorData>();
        mAccelSensor = new AccelSensor(this);
        mGyroSensor = new GyroSensor(this);
        mRunning = false;
    }

    protected void onResume() {
        super.onResume();
        mAccelSensor.register(this);
        mGyroSensor.register(this);
        if (mRunning) {
            startMeasurement();
        }
    }

    protected void onPause() {
        super.onPause();
        mAccelSensor.unregister();
        mGyroSensor.unregister();
        boolean wasRunning = mRunning;
        if (mRunning) stopMeasurement();
        mRunning = wasRunning;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return true;

        if (mRunning) // stop 
        {
            stopMeasurement();
            onStopMeasurement();
        } else // start
        {
            startMeasurement();
        }

        return true;
    }

    private void startMeasurement() {
        // Clear buffers
        mBuff_Accel.clear();
        mBuff_Gyro.clear();
        mt0 = SystemClock.elapsedRealtime();

        // Set gui
        mLayoutTop.setBackgroundResource(R.color.lime);
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
        mRunning = true;
    }

    private void stopMeasurement() {
        mChronometer.stop();
        mLayoutTop.setBackgroundResource(R.color.RosyBrown);
        mRunning = false;
    }

    private void onStopMeasurement() {
        // Format data
        StringBuffer sbuf;
        String data;
        
        // Store accel data
        sbuf = new StringBuffer();
        for (final SensorData sd : mBuff_Accel) {
            sbuf.append(sd.timeStamp).append('\t').
            append(sd.x).append('\t').
            append(sd.y).append('\t').
            append(sd.z).append('\t').
            append('\n');
        }

        data = sbuf.toString();
        storeDataLocally(data, AccelSensor.TYPE, "txt");
        sendDataToServer(data, AccelSensor.TYPE, "txt");

        // Store gyro data
        sbuf = new StringBuffer();
        for (final SensorData sd : mBuff_Gyro) {
            sbuf.append((float)(sd.timeStamp - mt0)/1000).append('\t').
            append(sd.x).append('\t').
            append(sd.y).append('\t').
            append(sd.z).append('\t').
            append('\n');
        }

        data = sbuf.toString();
        storeDataLocally(data, GyroSensor.TYPE, "txt");
        sendDataToServer(data, GyroSensor.TYPE, "txt");
        
        mLayoutTop.setBackgroundResource(R.color.mainSurfaceBgColor);
    }

    private void sendDataToServer(String data, String filename, String extension) {
        try {
            AccelSender.sendPost(data, SERVER_URL + "/" + filename + "." + extension);
        } catch (Exception exc) {
            Log.e(LOGTAG + ".send", exc.getMessage());
        }

    }

    private void storeDataLocally(String data, String filename, String extension) {
        if (!Utils.isExternalStorageWritable()){
            Log.w(LOGTAG, "Cannot write to External Storage");
            return;
        }
        Utils.writeToSDFile(data, filename, extension);
    }

    @Override
    public void onValueChanged(String type, float x, float y, float z) {
        if (!mRunning)
            return;

        // Create sensor data object
        SensorData sd = new SensorData(x, y, z, SystemClock.elapsedRealtime());

        // Pick appropriate buffer
        Vector<SensorData> buf = null;
        switch (type) {
        case AccelSensor.TYPE:
            buf = mBuff_Accel;
            break;
        case GyroSensor.TYPE:
            buf = mBuff_Gyro;
            break;
        default:
            break;
        }

        // Append new sensor measurement
        buf.add(sd);
    }

}
