package chris.accelerometer;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class AccelerometerActivity extends Activity implements AccelerometerListener {

    boolean             mRunning;
    Button              mBtnStartStop;
    TextView            mTxtEditIp;
    Accelerometer       mAccel;
    AccelerometerCanvas mAccelCanvas;
    AccelerometerSender mAccelSender;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        mRunning = true;

        mAccelCanvas = (AccelerometerCanvas) (findViewById(R.id.canvas_accel));
        mTxtEditIp = (TextView) (findViewById(R.id.txtedit_ip));
        mBtnStartStop = (Button) findViewById(R.id.btn_start_stop);
        mBtnStartStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mRunning) {
                    stop();
                    mBtnStartStop.setText(R.string.label_start);
                }
                else {
                    start();
                    mBtnStartStop.setText(R.string.label_stop);
                }
            }
        });
        ((Button) findViewById(R.id.btn_change_ip)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAccelSender.changeIP(mTxtEditIp.getText().toString());
            }
        });

        mAccel = new Accelerometer(this);
        mAccelSender = new AccelerometerSender(mTxtEditIp.getText().toString());
    }

    protected void onResume() {
        super.onResume();
        if (mRunning) {
            start();
            mBtnStartStop.setText(R.string.label_stop);
        }
    }

    protected void onPause() {
        super.onPause();
        boolean wasRunning = mRunning;
        stop();
        mRunning = wasRunning;
    }

    private void start() {
        mAccel.register(this);
        //mAccelSender.startSending();
        mRunning = true;
    }

    private void stop() {
        mAccelSender.stopSending();
        mAccel.unregister();
        mRunning = false;
    }

    @Override
    public void onAccelerationChanged(float gx, float gy, float gz) {
        mAccelCanvas.onAccelerationChanged(gx, gy, gz);
        // mAccelSender.putDataToBuffer(String.format(Locale.US, "%5.3f:%5.3f:%5.3f", gx, gy, gz));
    }

}
