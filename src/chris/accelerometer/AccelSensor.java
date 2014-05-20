package chris.accelerometer;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccelSensor implements SensorEventListener {

    private AccelListener mListener;
    private SensorManager mSensorManager;
    private Sensor        mAccelSensor;
    private float         gx, gy, gz;

    public AccelSensor(Activity activity) {
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void register(AccelListener interestedActivity) {
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
        gx = event.values[0];
        gy = event.values[1];
        gz = event.values[2];
        this.mListener.onAccelChanged(gx, gy, gz);
    }

}

interface AccelListener {

    public void onAccelChanged(float x, float y, float z);

}
