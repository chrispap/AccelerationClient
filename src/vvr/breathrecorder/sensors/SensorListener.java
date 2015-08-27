package vvr.breathrecorder.sensors;

public interface SensorListener {

    public void onValueChanged(String type, float x, float y, float z);

}
