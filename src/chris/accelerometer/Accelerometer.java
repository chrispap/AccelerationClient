package chris.accelerometer;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Accelerometer implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float mAccelorometerMaxValue;

    private AccelerometerListener listeningActivity;

    private float gx, gy, gz;

    public Accelerometer(Activity activity) {
	mSensorManager = (SensorManager) activity
		.getSystemService(Context.SENSOR_SERVICE);
	mAccelerometer = mSensorManager
		.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	mAccelorometerMaxValue = mAccelerometer.getMaximumRange();
    }

    public void register(AccelerometerListener interestedActivity) {
	mSensorManager.registerListener(this, mAccelerometer,
		SensorManager.SENSOR_DELAY_GAME);
	listeningActivity = interestedActivity;
    }

    public void unregister() {
	mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
	// TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
	mAccelorometerMaxValue = Math.max(mAccelorometerMaxValue,
		event.values[0]);
	mAccelorometerMaxValue = Math.max(mAccelorometerMaxValue,
		event.values[1]);
	mAccelorometerMaxValue = Math.max(mAccelorometerMaxValue,
		event.values[2]);

	gx = event.values[0] / (mAccelorometerMaxValue);
	gy = event.values[1] / (mAccelorometerMaxValue);
	gz = event.values[2] / (mAccelorometerMaxValue);

	Log.println(Log.DEBUG, "accel",
		"ACCEL-DATA-START-------------------------------");
	// +"\nGx: "+Float.toString(gx)
	// +"\nGy: "+Float.toString(gy)
	// +"\nGz: "+Float.toString(gz)
	// +"\n---------------------------------ACCEL-DATA-END");

	this.listeningActivity.onAccelerationChanged(gx, gy, gz);

    }

}
