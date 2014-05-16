package chris.accelerometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class AccelerometerCanvas extends View implements AccelerometerListener {

    private int mWidth, mHeight, mSize;
    private PointF mMark, mCenter;
    private Paint  mPaint;
    float          mGx, mGy, mGz;

    final int      n = 1024*4;
    float[]        a;
    FloatFFT_1D    fft;

    private void init() {
        mMark = new PointF();
        mCenter = new PointF();
        mPaint = new Paint();

        a = new float[n];
        fft = new FloatFFT_1D(n);

        for (int i = 0; i < n; i++)
            a[i] = (float) Math.cos((125.663706144 * 0.015) * i);
        // cos(2π*20*t)
        // f=20Hz
        // f_sample = 67Hz
        // t_sample = 15ms

        fft.realForward(a);

        for (int i = 0; i < n; i+=2)
            Log.i("accel.fft", "FFT[i] = " + Math.sqrt(a[i]*a[i]  + a[i+1]*a[i+1] ));
        
    }

    /* 3 Constructors */
    public AccelerometerCanvas(Context context) {
        super(context);
        init();
    }

    public AccelerometerCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccelerometerCanvas(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /* Android */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        mSize = Math.min(mWidth, mHeight);
        mCenter.x = mWidth / 2;
        mCenter.y = mHeight / 2;
        this.setMinimumHeight((int) mSize);
        this.setMinimumWidth((int) mSize);
        setMeasuredDimension(mWidth, mHeight);
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        mPaint.setColor(Color.RED);

        mMark.x = mWidth / 2 - (int) (mGx * mWidth * 0.1) / 2;
        mMark.y = mHeight / 2 + (int) (mGy * mHeight * 0.1) / 2;

        c.drawCircle(mMark.x, mMark.y, 10, mPaint);

        mPaint.setStrokeWidth(3);
        c.drawLine(mCenter.x, mCenter.y, mMark.x, mMark.y, mPaint);
    }

    @Override
    public void onAccelerationChanged(float gx, float gy, float gz) {
        mGx = gx;
        mGy = gy;
        mGz = gz;
        invalidate();
    }

}
