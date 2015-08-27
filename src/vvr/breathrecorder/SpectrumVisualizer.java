package vvr.breathrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.view.View;

public class SpectrumVisualizer extends View {

    /* Drawing */
    private float   mWidth, mHeight;
    private float[] mSpectrumLines;
    private int     mSignalPower;
    private long    mTimeLastUpdate;
    private Paint   mPaint;

    private void init() {
        mPaint = new Paint();
        mSpectrumLines = new float[0];
    }

    /* 3 Constructors */
    public SpectrumVisualizer(Context context) {
        super(context);
        init();
    }

    public SpectrumVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpectrumVisualizer(Context context, AttributeSet attrs, int defStyle) {
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

        /*Draw numerical indication of signal instant power*/
        mPaint.setTextSize(150);
        mPaint.setColor(Color.GRAY);
        mPaint.setTextAlign(Align.RIGHT);
        c.drawText("" + mSignalPower, mWidth - 40, 150, mPaint);

        /* Draw one vertical line at each frequency */
        mPaint.setColor(Color.GRAY);
        mPaint.setStrokeWidth(2);
        c.drawLines(mSpectrumLines, mPaint);

        /* Link the tops of every line
         *  to create the spectrum curve*/
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(4);
        for (int i = 0; i < mSpectrumLines.length / 4 - 1; i++) {
            c.drawLine(
                mSpectrumLines[(i << 2) + 2],
                mSpectrumLines[(i << 2) + 3],
                mSpectrumLines[(i << 2) + 6],
                mSpectrumLines[(i << 2) + 7],
                mPaint);
        }

    }

    /* Drawing */
    public void updateSpectrum(double[] spectrumBuf) {
        if (mSpectrumLines.length != spectrumBuf.length)
            mSpectrumLines = new float[spectrumBuf.length * 4];

        double power = 0;

        for (int i = 0; i < spectrumBuf.length; i++) {
            power += spectrumBuf[i];

            double x = (mWidth / spectrumBuf.length) * (i + 0.5f);
            double y = mHeight / 800.0 * 5.0 * spectrumBuf[i];

            mSpectrumLines[(i << 2) + 0] = (float) x;
            mSpectrumLines[(i << 2) + 1] = (float) mHeight;
            mSpectrumLines[(i << 2) + 2] = (float) x;
            mSpectrumLines[(i << 2) + 3] = (float) (mHeight - y);
        }

        long now = System.currentTimeMillis();
        long dt = now - mTimeLastUpdate;
        if (dt > 200) {
            mSignalPower = (int) power;
            mTimeLastUpdate = now;
        }

        invalidate();
    }

}
