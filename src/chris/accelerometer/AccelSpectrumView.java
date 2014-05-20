package chris.accelerometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class AccelSpectrumView extends View implements AccelerometerListener {

    /* For Drawing */
    private float       mWidth, mHeight;
    private Paint       mPaint;
    private float[]     mSpectrumLines;

    /* For Spectrum calculation */
    private long        mDT      = 10;
    private long        mLastSampleTime;
    private int         mFFTSize = 64;
    private int         mSampleCounter;
    private float[]     mBufAccel, mBufFFT, mBufSpectrum;
    private FloatFFT_1D mFFT;

    private void allocBuffers() {
        mBufAccel = new float[mFFTSize];
        mBufFFT = new float[mFFTSize];
        mBufSpectrum = new float[mFFTSize / 2];
        mSpectrumLines = new float[mFFTSize * 2];
        mFFT = new FloatFFT_1D(mFFTSize);

    }

    private void init() {
        allocBuffers();
        mPaint = new Paint();
    }

    /* 3 Constructors */
    public AccelSpectrumView(Context context) {
        super(context);
        init();
    }

    public AccelSpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccelSpectrumView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /* Logic */
    private void updateFreqLines() {
        for (int i = 0; i < mFFTSize / 2; i++) {
            float x = (mWidth / (mFFTSize / 2)) * (i + 0.5f);
            float y = (mBufSpectrum[i] * 8);

            mSpectrumLines[(i << 2) + 0] = x;
            mSpectrumLines[(i << 2) + 1] = mHeight;
            mSpectrumLines[(i << 2) + 2] = x;
            mSpectrumLines[(i << 2) + 3] = mHeight - y;
        }
    }

    public void onAccelerationChanged(float gx, float gy, float gz) {
        long now = SystemClock.elapsedRealtime();
        int n = (int) Math.floor((now - mLastSampleTime) / mDT);
        if (n == 0) return;
        mLastSampleTime = mLastSampleTime + n * mDT;

        /* Shift up the old acceleration values 
         * and store the new one */
        for (int i = 0; i < mFFTSize - n; i++)
            mBufAccel[i] = mBufAccel[i + n];
        mBufAccel[mFFTSize - 1] = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);

        if (n < mFFTSize) {
            float dA = (mBufAccel[mFFTSize - 1] - mBufAccel[mFFTSize - n - 1]) / n;
            for (int i = 0; i < n - 1; i++)
                mBufAccel[mFFTSize - n + i] = mBufAccel[mFFTSize - n - 1] + dA * (i + 1);
        }

        mSampleCounter += n;

        /* Perform FFT and update Spectrum */
        for (int i = 0; i < mFFTSize; i++)
            mBufFFT[i] = mBufAccel[i];
        mFFT.realForward(mBufFFT);

        for (int i = 0; i < mFFTSize / 2; i++) {
            final float aRe = mBufFFT[(i << 1)];
            final float aIm = mBufFFT[(i << 1) + 1];
            final float a = (float) Math.sqrt(aRe * aRe + aIm * aIm);
            mBufSpectrum[i] = a;
        }

        mBufSpectrum[0] = 0; //Depress DC component.

        updateFreqLines();
        invalidate();
    }

    /* Android */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        int minSize = (int) Math.min(mWidth, mHeight);
        this.setMinimumHeight(minSize);
        this.setMinimumWidth(minSize);
        setMeasuredDimension((int) mWidth, (int) mHeight);
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (mSampleCounter < mFFTSize) return;

        /* Draw one line at each frequency */
        mPaint.setColor(Color.LTGRAY);
        mPaint.setStrokeWidth(2);
        c.drawLines(mSpectrumLines, mPaint);

        /* Link the tops of every line */
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(4);
        for (int i = 0; i < mFFTSize / 2 - 1; i++) {
            c.drawLine(
                mSpectrumLines[(i << 2) + 2],
                mSpectrumLines[(i << 2) + 3],
                mSpectrumLines[(i << 2) + 6],
                mSpectrumLines[(i << 2) + 7],
                mPaint);
        }

    }

}
