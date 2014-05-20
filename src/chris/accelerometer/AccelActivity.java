package chris.accelerometer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class AccelActivity extends Activity implements AccelerometerListener {

    boolean                  mRunning;
    Button                   mBtnStartStop;
    TextView                 mTxtEditIp;
    AccelSensor            mAccelSensor;
    AccelSender      mAccelSender;
    AccelView         mAccelView;
    AccelSpectrumView mAccelSpectrumView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        /* Find views and set listeners. */
        mAccelView = (AccelView) (findViewById(R.id.view_accel));
        mAccelSpectrumView = (AccelSpectrumView) (findViewById(R.id.view_spectrum));
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

        mAccelSensor = new AccelSensor(this);
        mAccelSender = new AccelSender(mTxtEditIp.getText().toString());
        mRunning = true;
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
        mAccelSensor.register(this);
        mAccelSender.startSending();
        mRunning = true;
    }

    private void stop() {
        mAccelSensor.unregister();
        mAccelSender.stopSending();
        mRunning = false;
    }

    @Override
    public void onAccelerationChanged(float gx, float gy, float gz) {
        mAccelView.onAccelerationChanged(gx, gy, gz);
        mAccelSpectrumView.onAccelerationChanged(gx, gy, gz);
        mAccelSender.onAccelerationChanged(gx, gy, gz);
    }

}
