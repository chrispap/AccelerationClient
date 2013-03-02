package chris.accelerometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class AccelerometerView extends View {

    /* View dimensions */
    int width, height, size;

    /* View components */
    PointF mark, center;
    final float markDiameter = 50;

    /* Accelometer Values */
    float Gx = 0, Gy = 0, Gz = 0;

    /* 3-CONSTRUCTORS */
    public AccelerometerView(Context context) {
        super(context);
        Log.d("chris", "1st constructor of GestureView");
    }

    public AccelerometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d("chris", "2nd constructor of GestureView");
    }

    public AccelerometerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.d("chris", "3rd constructor of GestureView");
    }

    /* ANDROID METHODS */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = View.MeasureSpec.getSize(widthMeasureSpec);
        height = View.MeasureSpec.getSize(heightMeasureSpec);
        size = Math.min(width, height);
        center = new PointF(width / 2, height / 2);
        this.setMinimumHeight((int) size);
        this.setMinimumWidth((int) size);
        setMeasuredDimension(width, height);
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        Paint p = new Paint();
        p.setColor(Color.RED);
        mark = new PointF(width / 2 - (int) (Gx * (width)) / 2, height / 2 + (int) (Gy * (height)) / 2);

        c.drawCircle(mark.x, mark.y, (2.0f - Gz) * markDiameter / 2.0f, p);
        p.setStrokeWidth(3);
        c.drawLine(center.x, center.y, mark.x, mark.y, p);

    }

}
