package vvr.breathrecorder.sensors;

public class SensorData {

	public float x, y, z;
	public long timeStamp;

	public SensorData(float x, float y, float z, long timeStamp) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.timeStamp = timeStamp;
	}
	
	public SensorData() {
	    this(0.0f, 0.0f, 0.0f, 0L);
	}
	
	float get(int i) {
		if (i == 0) {
			return x;
		} else if (i == 1) {
			return y;
		} else {
			return z;
		}
	}
}
