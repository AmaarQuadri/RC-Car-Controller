package com.gmail.amaarquardi.rccarcontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Amaar on 2017-06-12.
 */

public class ThrottleSliderView extends View {
    private static final Paint rectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    static {
        rectanglePaint.setColor(Color.DKGRAY);
        circlePaint.setColor(Color.BLUE);
    }

    private double yPos;
    private boolean center;
    private double canvasHeight;

    public ThrottleSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        center = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        canvasHeight = height;

        if (center) {
            yPos = canvasHeight / 2;
            center = false;
        }
        else yPos = Math.max(0.1 * canvasHeight, Math.min(0.9  * canvasHeight, yPos));
        canvas.drawRect(0.475f * width, 0.1f * height, 0.525f * width, 0.9f * height, rectanglePaint);
        canvas.drawCircle(0.5f * width, (float) yPos, 0.1f * height, circlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick();
            case MotionEvent.ACTION_MOVE:
                yPos = event.getY();
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                center = true;
                invalidate();
                return true;
            default: return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /**
     * @return A number from -1 to 1.
     */
    public double getThrottlePosition() {
        return (0.5 * canvasHeight - yPos) / (0.4 * canvasHeight);
    }

    public void resetThrottlePosition() {
        center = true;
        invalidate();
    }
}
