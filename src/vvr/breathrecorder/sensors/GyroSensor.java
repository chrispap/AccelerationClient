package vvr.breathrecorder.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GyroSensor implements SensorEventListener {

    public static final String TYPE = "gyro";
    private SensorListener     mListener;
    private SensorManager      mSensorManager;
    private Sensor             mGyroSensor;
    private float              ax, ay, az;

    public GyroSensor(Activity activity) {
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void register(SensorListener interestedActivity) {
        mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mListener = interestedActivity;
    }

    public void unregister() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        ax = event.values[0];
        ay = event.values[1];
        az = event.values[2];
        this.mListener.onValueChanged(TYPE, ax, ay, az);
    }

}
