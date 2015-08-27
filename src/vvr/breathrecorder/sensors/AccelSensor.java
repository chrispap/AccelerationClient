package vvr.breathrecorder.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccelSensor implements SensorEventListener {

    public static final String TYPE = "accel";
    private SensorListener     mListener;
    private SensorManager      mSensorManager;
    private Sensor             mAccelSensor;

    public AccelSensor(Activity activity) {
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void register(SensorListener interestedActivity) {
        mSensorManager.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_FASTEST);
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
        this.mListener.onValueChanged(TYPE,
            event.values[0],
            event.values[1],
            event.values[2]);
    }

}
