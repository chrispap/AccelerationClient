package chris.accelerometer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class AccelActivity extends Activity implements AccelListener {

    private final String      USER_AGENT = "Mozilla/5.0";
    private final String      SERVER_URL = "http://test.papapaulou.gr/tremor_data_insert";

    private boolean           mRunning;
    private Button            mBtnStartStop;
    private AccelSensor       mAccelSensor;
    private AccelSender       mAccelSender;
    private AccelView         mAccelView;
    private AccelSpectrumView mAccelSpectrumView;

    /* Spectrum Calculation */
    private long              mT         = 0;
    private long              mDT        = 10;
    private int               mFFTSize   = 64;
    private double[]          mBufAccel, mBufFFT, mBufSpectrum;
    private DoubleFFT_1D      mFFT;

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
        mAccelView = (AccelView) (findViewById(R.id.view_accel));
        mAccelSpectrumView = (AccelSpectrumView) (findViewById(R.id.view_spectrum));
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

        /* */
        allocBuffers();
        mAccelSensor = new AccelSensor(this);
        mAccelSender = new AccelSender("IP");
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            HashMap<String, String> values = new HashMap<String, String>();
            values.put("name", "Chris");
            values.put("tremor_data", "1,2,3,4,5,6");

            try {
                sendPost(values);
                Log.i("accel.send", "+++");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        return true;
    }

    /* UI */
    private void start() {
        mAccelSensor.register(this);
        //mAccelSender.startSending();
        mRunning = true;
    }

    private void stop() {
        mAccelSensor.unregister();
        mAccelSender.stopSending();
        mRunning = false;
    }

    /* Callbacks */
    @Override
    public void onAccelChanged(float ax, float ay, float az) {
        addAccelVal(ax, ay, az);
        mAccelView.onAccelChanged(ax, ay, az);
        mAccelSpectrumView.updateSpectrum(mBufSpectrum);

        //mAccelSender.onAccelChanged(gx, gy, gz);
    }

    private void addAccelVal(float ax, float ay, float az) {
        /* Enforce regular intervals */
        int dN = (int) ((System.nanoTime() - mT * 1000000L) / (mDT * 1000000L));
        if (dN == 0) return;
        mT += (dN * mDT);

        /* Shift up the old acceleration values 
         * and store the new one */
        for (int i = 0; i < mFFTSize - dN; i++)
            mBufAccel[i] = mBufAccel[i + dN];
        mBufAccel[mFFTSize - 1] = (float) Math.sqrt((ax * ax) + (ay * ay) + (az * az));

        /* Interpolate missed samples */
        if (dN < mFFTSize) {
            double dA = (mBufAccel[mFFTSize - 1] - mBufAccel[mFFTSize - dN - 1]) / dN;
            for (int i = 0; i < dN - 1; i++)
                mBufAccel[mFFTSize - dN + i] = mBufAccel[mFFTSize - dN - 1] + dA * (i + 1);
        }

        /* Perform FFT and update Spectrum */
        for (int i = 0; i < mFFTSize; i++)
            mBufFFT[i] = mBufAccel[i];
        mFFT.realForward(mBufFFT);

        for (int i = 0; i < mFFTSize / 2; i++) {
            final double aRe = mBufFFT[(i << 1)];
            final double aIm = mBufFFT[(i << 1) + 1];
            final double a = Math.sqrt(aRe * aRe + aIm * aIm);
            mBufSpectrum[i] = a;
        }

        mBufSpectrum[0] = 0; //Depress DC component.
    }

    /* Net */
    private int sendPost(Map<String, String> dataMap) throws Exception {

        URL obj = new URL(SERVER_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        /* Add reuqest header */
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        /* Construct the post data */
        StringBuffer post_data = new StringBuffer();
        for (Map.Entry<String, String> entry : dataMap.entrySet())
            post_data.append(entry.getKey() + "=" + entry.getValue() + "&");
        if (post_data.length() > 0) post_data.setLength(post_data.length() - 1);

        /* Send post request */
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(post_data.toString());
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(
            new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return responseCode;
    }

}
