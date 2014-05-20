package chris.accelerometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class AccelSpectrumView extends View implements AccelListener {

    /* Drawing */
    private double       mWidth, mHeight;
    private Paint        mPaint;
    private float[]      mSpectrumLines;

    /* Spectrum Calculation */
    private long         mT       = 0;
    private long         mDT      = 10;
    private int          mFFTSize = 64;
    private double[]     mBufAccel, mBufFFT, mBufSpectrum;
    private DoubleFFT_1D mFFT;

    private void allocBuffers() {
        mBufAccel = new double[mFFTSize];
        mBufFFT = new double[mFFTSize];
        mBufSpectrum = new double[mFFTSize / 2];
        mFFT = new DoubleFFT_1D(mFFTSize);

        mSpectrumLines = new float[mFFTSize * 2];
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
            double x = (mWidth / (mFFTSize / 2)) * (i + 0.5f);
            double y = mHeight / 800.0 * 5.0 * mBufSpectrum[i];

            mSpectrumLines[(i << 2) + 0] = (float) x;
            mSpectrumLines[(i << 2) + 1] = (float) mHeight;
            mSpectrumLines[(i << 2) + 2] = (float) x;
            mSpectrumLines[(i << 2) + 3] = (float) (mHeight - y);
        }
    }

    /* Callbacks */
    public void onAccelChanged(float gx, float gy, float gz) {
        /* Enforce regular intervals */
        int dN = (int) ((System.nanoTime() - mT * 1000000L) / (mDT * 1000000L));
        if (dN == 0) return;
        mT += (dN * mDT);

        /* Shift up the old acceleration values 
         * and store the new one */
        for (int i = 0; i < mFFTSize - dN; i++)
            mBufAccel[i] = mBufAccel[i + dN];
        mBufAccel[mFFTSize - 1] = (float) Math.sqrt((gx * gx) + (gy * gy) + (gz * gz));

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

        updateSpectrumLines();
        invalidate();
    }

}
