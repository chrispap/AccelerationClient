package chris.accelerometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class AccelerationSpectrumView extends View implements AccelerometerListener {

    private int   mWidth, mHeight, mSize;
    private Paint mPaint;
    int           mSampleCounter;
    long          mLastSampleTime;

    FloatFFT_1D   mFFT;
    final int     mFFTSize = 64;
    float[]       mBufAccel, mBufFFT, mSpectrum, mSpectrumLines;

    private void init() {
        mBufAccel = new float[mFFTSize];
        mBufFFT = new float[mFFTSize];
        mSpectrum = new float[mFFTSize / 2];
        mSpectrumLines = new float[mFFTSize * 2];
        mFFT = new FloatFFT_1D(mFFTSize);

        mPaint = new Paint();
    }

    /* 3 Constructors */
    public AccelerationSpectrumView(Context context) {
        super(context);
        init();
    }

    public AccelerationSpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccelerationSpectrumView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /* Android */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        mSize = Math.min(mWidth, mHeight);
        this.setMinimumHeight((int) mSize);
        this.setMinimumWidth((int) mSize);
        setMeasuredDimension(mWidth, mHeight);
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(mWidth / mFFTSize);
        c.drawLines(mSpectrumLines, mPaint);
    }

    private void updateFreqLines() {
        for (int i = 0; i < mFFTSize / 2; i++) {
            float x = (float) mWidth / mFFTSize * 2 * i;
            float y = mSpectrum[i];
            // Line start
            mSpectrumLines[(i << 2) + 0] = x;
            mSpectrumLines[(i << 2) + 1] = mHeight;
            // Line end
            mSpectrumLines[(i << 2) + 2] = x;
            mSpectrumLines[(i << 2) + 3] = mHeight - y;
        }
    }

    @Override
    public void onAccelerationChanged(float gx, float gy, float gz) {
        long millis = SystemClock.elapsedRealtime();

        if (millis - mLastSampleTime < 5) return;
        mLastSampleTime = millis;

        /* Shift up the old acceleration values */
        for (int i = 0; i < mFFTSize - 1; i++)
            mBufAccel[i] = mBufAccel[i + 1];

        /* Store the new acceleration value */
        mBufAccel[mFFTSize - 1] = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);

        ++mSampleCounter;

        /* Perform FFT and update Spectrum */
        for (int i = 0; i < mFFTSize; i++)
            mBufFFT[i] = mBufAccel[i];

        mFFT.realForward(mBufFFT);

        mBufFFT[0] = 0; // Depress DC component.

        for (int i = 0; i < mFFTSize / 2; i++) {
            final float a_re = mBufFFT[(i << 1)]; // Real 
            final float a_im = mBufFFT[(i << 1) + 1]; // Imaginary
            mSpectrum[i] = 20.0f * (float) Math.sqrt(a_re * a_re + a_im * a_im);
        }

        updateFreqLines();

        invalidate();
    }

}
