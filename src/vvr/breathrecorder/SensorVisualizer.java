package vvr.breathrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import vvr.breathrecorder.sensors.SensorListener;

public class SensorVisualizer extends View implements SensorListener {

    private float  mAx, mAy;
    private int    mWidth, mHeight, mSize;
    private PointF mMark, mCenter;
    private Paint  mPaint;

    private void init() {
        mMark = new PointF();
        mCenter = new PointF();
        mPaint = new Paint();
    }

    /* 3 Constructors */
    public SensorVisualizer(Context context) {
        super(context);
        init();
    }

    public SensorVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SensorVisualizer(Context context, AttributeSet attrs, int defStyle) {
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

        mMark.x = mWidth / 2 - (int) (mAx * mWidth * 0.1) / 2;
        mMark.y = mHeight / 2 + (int) (mAy * mHeight * 0.1) / 2;

        c.drawCircle(mMark.x, mMark.y, 10, mPaint);

        mPaint.setStrokeWidth(3);
        c.drawLine(mCenter.x, mCenter.y, mMark.x, mMark.y, mPaint);
    }

    @Override
    public void onValueChanged(String tytpe, float ax, float ay, float az) {
        mAx = ax;
        mAy = ay;
        invalidate();
    }

}
