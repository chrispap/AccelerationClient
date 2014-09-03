package chris.accelerometer;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Chronometer;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class AccelActivity extends Activity implements AccelListener {

    public static final String SERVER_URL = "http://test.papapaulou.gr/tremor_data_insert";
    public static final String LOGTAG     = "accel";
    public static final int    FFT_SIZE   = 5000;
    public static final int    DT         = 10;

    /* UI */
    private boolean            mRunning;
    private Chronometer        mChronometer;
    private AccelSensor        mAccelSensor;
    private AccelView          mAccelView;
    private AccelSpectrumView  mAccelSpectrumView;

    /* Spectrum Calculation */
    private int                mCount;
    private long               mTime;
    private long[]             mBuf_Time;
    private double[]           mBuf_AccelX;
    private double[]           mBuf_AccelY;
    private double[]           mBuf_AccelZ;
    private double[]           mBuf_Spectrum;
    private DoubleFFT_1D       mFFT;

    private void allocBuffers() {
        mBuf_Time = new long[FFT_SIZE];
        mBuf_AccelX = new double[FFT_SIZE];
        mBuf_AccelY = new double[FFT_SIZE];
        mBuf_AccelZ = new double[FFT_SIZE];
        mBuf_Spectrum = new double[FFT_SIZE / 2];
        mFFT = new DoubleFFT_1D(FFT_SIZE);
    }

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
        mAccelView = (AccelView) findViewById(R.id.view_accel);
        mAccelSpectrumView = (AccelSpectrumView) findViewById(R.id.view_spectrum);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);

        /*-*/
        allocBuffers();
        mAccelSensor = new AccelSensor(this);
        mRunning = false;
    }

    protected void onResume() {
        super.onResume();
        mAccelSensor.register(this);
        if (mRunning) {
            startMeasurement();
        }
    }

    protected void onPause() {
        super.onPause();
        mAccelSensor.unregister();
        boolean wasRunning = mRunning;
        if (mRunning) stopMeasurement();
        mRunning = wasRunning;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return true;

        if (mRunning)
        { // OnStop
            stopMeasurement();
            onStopMeasurement();
        }
        else
        { // OnStart
            startMeasurement();
        }

        return true;
    }

    private void startMeasurement() {
        for (int i = 0; i < FFT_SIZE; i++) {
            mBuf_AccelX[i] = -1;
            mBuf_AccelY[i] = -1;
            mBuf_AccelZ[i] = -1;
            mBuf_Time[i] = -1;
        }

        mAccelView.setBackgroundResource(R.color.Olive);
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
        mCount = 0;
        mRunning = true;
    }

    private void stopMeasurement() {
        mChronometer.stop();
        mAccelView.setBackgroundResource(R.color.RosyBrown);
        mRunning = false;
    }

    private void onStopMeasurement() {
        // Format data
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("time:ax:ay:az \n");
        for (int i = 0; i < FFT_SIZE; i++) {
            sbuf.append(mBuf_Time[i]/* - mBuf_Time[0]*/).append('\t').
                    append(mBuf_AccelX[i]).append('\t').
                    append(mBuf_AccelY[i]).append('\t').
                    append(mBuf_AccelZ[i]).append('\t').
                    append('\n');
        }

        // Distribute data
        String data = sbuf.toString();
        storeDataLocally(data);
        sendDataToServer(data);
        mAccelView.setBackgroundResource(R.color.mainSurfaceBgColor);
    }

    private void sendDataToServer(String data) {
        try {
            AccelSender.sendPost(data, SERVER_URL);
        } catch (Exception exc) {
            Log.e(LOGTAG + ".send", exc.getMessage());
        }

    }

    private void storeDataLocally(String data) {
        if (!Utils.isExternalStorageWritable()) return;
        Utils.writeToSDFile(data, "data", "txt");
    }

    @Override
    public void onAccelChanged(float ax, float ay, float az) {
        if (!mRunning) return;

        if (mCount >= FFT_SIZE) {
            stopMeasurement();
            onStopMeasurement();
            return;
        }

        addAccelVal(ax, ay, az);
        mCount++;
        //mAccelView.onAccelChanged(ax, ay, az);
        //mAccelSpectrumView.updateSpectrum(mBuf_Spectrum);
    }

    private void addAccelVal(float ax, float ay, float az) {
        mTime = System.currentTimeMillis();

        mBuf_Time[mCount] = mTime;
        mBuf_AccelX[mCount] = ax;
        mBuf_AccelY[mCount] = ay;
        mBuf_AccelZ[mCount] = az;
        
        // Shift up old acceleration values ...
        /*for (int i = 0; i < FFT_SIZE - 1; i++) {
            mBuf_Time[i] = mBuf_Time[i + 1];
            mBuf_AccelX[i] = mBuf_AccelX[i + 1];
            mBuf_AccelY[i] = mBuf_AccelY[i + 1];
            mBuf_AccelZ[i] = mBuf_AccelZ[i + 1];
        }*/

        // ... and store the new one
        /*mBuf_Time[FFT_SIZE - 1] = mTime;
        mBuf_AccelX[FFT_SIZE - 1] = ax;
        mBuf_AccelY[FFT_SIZE - 1] = ay;
        mBuf_AccelZ[FFT_SIZE - 1] = az;*/
    }

}
