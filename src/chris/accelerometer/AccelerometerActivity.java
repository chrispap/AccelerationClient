package chris.accelerometer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AccelerometerActivity extends Activity implements AccelerometerListener {

    /* Instance Variables */
    AccelerometerView mainSurface;
    TextView ipEditor;
    Button startButton;
    Button stopButton;
    Accelerometer accel;
    Sender sender;

    boolean running = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        mainSurface = (AccelerometerView) (findViewById(R.id.mainSurface));
        ipEditor = (TextView) (findViewById(R.id.IPeditor));

        accel = new Accelerometer(this);
        sender = new Sender(ipEditor.getText().toString());

        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                start();
            }
        });

        stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                stop();
            }
        });

    }

    protected void onResume() {
        super.onResume();
        start();
    }

    protected void onPause() {
        super.onPause();
        stop();
    }

    private String makeMessage(float gx, float gy, float gz) {
        String message = new String("");
        message = message + Float.toString(gx) + ":" + message + Float.toString(gy) + ":" + message
                + Float.toString(gz);
        return message;
    }

    @Override
    public void onAccelerationChanged(float gx, float gy, float gz) {
        mainSurface.Gx = gx;
        mainSurface.Gy = gy;
        mainSurface.Gz = gz;
        mainSurface.invalidate();

        String msg = makeMessage(gx, gy, gz);
        sender.putDataToBuffer(msg);
    }

    protected void start() {
        if (!running) {
            accel.register(this);
            sender.startSending();
            startButton.setText(R.string.startButtonLabel_2);
        } else {
            sender.changeIP(ipEditor.getText().toString());
        }
        running = true;
    }

    protected void stop() {
        sender.stopSending();
        accel.unregister();
        running = false;
        startButton.setText(R.string.startButtonLabel_1);
    }

}
