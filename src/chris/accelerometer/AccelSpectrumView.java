package chris.accelerometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class AccelSpectrumView extends View implements AccelListener {

    /* For Drawing */
    private float       mWidth, mHeight;
    private Paint       mPaint;
    private float[]     mSpectrumLines;

    /* For Spectrum Calculation */
    private long        mDT      = 12 * 1000 * 1000;
    private long        mLastSampleTime;
    private int         mFFTSize = 80;
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

        /* Draw one vertical line at each frequency */
        mPaint.setColor(Color.GRAY);
        mPaint.setStrokeWidth(2);
        c.drawLines(mSpectrumLines, mPaint);

        /* Link the tops of every line
         *  to create the spectrum curve*/
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

    /* Drawing */
    private void updateSpectrumLines() {
        for (int i = 0; i < mFFTSize / 2; i++) {
            float x = (mWidth / (mFFTSize / 2)) * (i + 0.5f);
            float y = mHeight / 1000 * 8 * mBufSpectrum[i];

            mSpectrumLines[(i << 2) + 0] = x;
            mSpectrumLines[(i << 2) + 1] = mHeight;
            mSpectrumLines[(i << 2) + 2] = x;
            mSpectrumLines[(i << 2) + 3] = mHeight - y;
        }
    }

    /* Callbacks */
    public void onAccelChanged(float gx, float gy, float gz) {
        /* Avoid time interval inconsistencies */
        long now = System.nanoTime();
        int dN = (int) ((now - mLastSampleTime) / mDT);
        if (dN == 0) return;
        mLastSampleTime = mLastSampleTime + dN * mDT;

        /* Shift up the old acceleration values 
         * and store the new one */
        for (int i = 0; i < mFFTSize - dN; i++)
            mBufAccel[i] = mBufAccel[i + dN];
        mBufAccel[mFFTSize - 1] = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);

        /* Interpolate missed samples */
        if (dN < mFFTSize) {
            float dA = (mBufAccel[mFFTSize - 1] - mBufAccel[mFFTSize - dN - 1]) / dN;
            for (int i = 0; i < dN - 1; i++)
                mBufAccel[mFFTSize - dN + i] = mBufAccel[mFFTSize - dN - 1] + dA * (i + 1);
        }

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

        updateSpectrumLines();
        invalidate();
    }
}
