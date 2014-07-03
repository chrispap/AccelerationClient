package chris.accelerometer;

import java.util.HashMap;

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

    public static final String SERVER_URL = "http://upat.notremor.eu/tremor_data_insert";
    public static final String LOGTAG = "accel";

    private boolean            mRunning;
    private Chronometer        mChronometer;
    private AccelSensor        mAccelSensor;
    private AccelSender        mAccelSender;
    private AccelView          mAccelView;
    private AccelSpectrumView  mAccelSpectrumView;

    /* Spectrum Calculation */
    private long               mT         = 0;
    private long               mDT        = 10;
    private int                mFFTSize   = 1000;
    private double[]           mBufAccel, mBufFFT, mBufSpectrum;
    private DoubleFFT_1D       mFFT;

    private void allocBuffers() {
        mBufAccel = new double[mFFTSize];
        mBufFFT = new double[mFFTSize];
        mBufSpectrum = new double[mFFTSize / 2];
        mFFT = new DoubleFFT_1D(mFFTSize);
    }

    /* Android */
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
        mAccelSender = new AccelSender("IP");
        mRunning = false;
    }

    protected void onResume() {
        super.onResume();
        mAccelSensor.register(this);
        if (mRunning) {
            start();
        }
    }

    protected void onPause() {
        super.onPause();
        mAccelSensor.unregister();
        boolean wasRunning = mRunning;
        if (mRunning) stop();
        mRunning = wasRunning;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if (mRunning) {
                Log.i(LOGTAG, "Stopped");

                mChronometer.stop();
                
                stop();
                // storeDataLocally();
                sendSomething();
            }
            else {
                Log.i(LOGTAG, "Started");
                
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
                
                start();
            }

        }

        return true;
    }

    /* UI */
    private void start() {
        // mAccelSender.startSending();
        mRunning = true;
    }

    private void stop() {
        mAccelSender.stopSending();
        mRunning = false;
    }

    private void sendSomething() {
        HashMap<String, String> values = new HashMap<String, String>();

        StringBuffer sbuf = new StringBuffer();
        for (int i = 0; i < mFFTSize; i++) {
            sbuf.append(mBufAccel[i]).append('\n');
        }

        try {
            AccelSender.sendPost(sbuf.toString(), SERVER_URL);
            Log.i(LOGTAG+".send", "Sent " +sbuf.length()+" characters");
        } catch (Exception exc) {
            Log.e(LOGTAG+".send", exc.getMessage());
        }

    }

    private void storeDataLocally() {
        if (!Utils.isExternalStorageWritable()) return;
        Utils.writeToSDFile(mBufAccel, "data.txt");
    }

    /* Callbacks */
    @Override
    public void onAccelChanged(float ax, float ay, float az) {
        addAccelVal(ax, ay, az);
        mAccelView.onAccelChanged(ax, ay, az);
        mAccelSpectrumView.updateSpectrum(mBufSpectrum);
        // mAccelSender.onAccelChanged(gx, gy, gz);
    }

    private void addAccelVal(float ax, float ay, float az) {
        /* Enforce regular intervals */
        int dN = (int) ((System.nanoTime() - mT * 1000000L) / (mDT * 1000000L));
        if (dN == 0) return;
        mT += (dN * mDT);

        /*
         * Shift up the old acceleration values and store the new one
         */
        for (int i = 0; i < mFFTSize - dN; i++)
            mBufAccel[i] = mBufAccel[i + dN];
        mBufAccel[mFFTSize - 1] = (float) Math.sqrt((ax * ax) + (ay * ay) + (az * az));

        /* Interpolate missed samples */
        if (dN < mFFTSize) {
            double dA = (mBufAccel[mFFTSize - 1] - mBufAccel[mFFTSize - dN - 1]) / dN;
            for (int i = 0; i < dN - 1; i++)
                mBufAccel[mFFTSize - dN + i] = mBufAccel[mFFTSize - dN - 1] + dA * (i + 1);
        }

//        /* Perform FFT and update Spectrum */
//        for (int i = 0; i < mFFTSize; i++)
//            mBufFFT[i] = mBufAccel[i];
//        mFFT.realForward(mBufFFT);
//
//        for (int i = 0; i < mFFTSize / 2; i++) {
//            final double aRe = mBufFFT[(i << 1)];
//            final double aIm = mBufFFT[(i << 1) + 1];
//            final double a = Math.sqrt(aRe * aRe + aIm * aIm);
//            mBufSpectrum[i] = a;
//        }
//
//        mBufSpectrum[0] = 0; // Depress DC component.
    }

}
