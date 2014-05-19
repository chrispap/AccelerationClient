package chris.accelerometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class AccelerationSpectrumView extends View implements AccelerometerListener {

    private float       mWidth, mHeight;
    private Paint       mPaint;
    private int         mSampleCounter;
    private FloatFFT_1D mFFT;
    private final int   mFFTSize = 128;
    private float[]     mBufAccel, mBufFFT, mSpectrum, mSpectrumLines;

    private void allocBuffers() {
        mBufAccel = new float[mFFTSize];
        mBufFFT = new float[mFFTSize];
        mSpectrum = new float[mFFTSize / 2];
        mSpectrumLines = new float[mFFTSize * 2];
        mFFT = new FloatFFT_1D(mFFTSize);

    }

    private void init() {
        allocBuffers();
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

    /* Logic */
    private void updateFreqLines() {
        for (int i = 0; i < mFFTSize / 2; i++)
        {
            float x = mWidth / mFFTSize * 2 * i;
            float y = mSpectrum[i] * 20;

            mSpectrumLines[(i << 2) + 0] = x;
            mSpectrumLines[(i << 2) + 1] = mHeight;
            mSpectrumLines[(i << 2) + 2] = x;
            mSpectrumLines[(i << 2) + 3] = mHeight - y;
        }
    }

    public void onAccelerationChanged(float gx, float gy, float gz) {
        /* Shift up the old acceleration values 
         * and store the new one*/
        for (int i = 0; i < mFFTSize - 1; i++)
            mBufAccel[i] = mBufAccel[i + 1];
        mBufAccel[mFFTSize - 1] = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
        mSampleCounter++;

        /* Perform FFT and update Spectrum */
        for (int i = 0; i < mFFTSize; i++)
            mBufFFT[i] = mBufAccel[i];
        mFFT.realForward(mBufFFT);
        mBufFFT[0] = 0; // Depress DC component.

        for (int i = 0; i < mFFTSize / 2; i++) {
            final float aRe = mBufFFT[(i << 1)];
            final float aIm = mBufFFT[(i << 1) + 1];
            final float a = (float) Math.sqrt(aRe * aRe + aIm * aIm);

            mSpectrum[i] = a;
        }

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
