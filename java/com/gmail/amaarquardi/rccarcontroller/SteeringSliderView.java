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

public class SteeringSliderView extends View {
    private static final Paint rectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    static {
        rectanglePaint.setColor(Color.DKGRAY);
        circlePaint.setColor(Color.BLUE);
    }

    private double xPos;
    private boolean center;
    private double canvasWidth;
    private double canvasHeight;

    public SteeringSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        center = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        canvasWidth = width;
        canvasHeight = height;

        if (center) {
            xPos = canvasWidth / 2;
            center = false;
        }
        else xPos = Math.max(0.1 * height, Math.min(width - 0.1 * height, xPos));
        //middle 80% of width, middle 5% of height
        canvas.drawRect(0.1f * height, 0.9f * height - 0.0125f * width, width - 0.1f * height, 0.9f * height + 0.0125f * width, rectanglePaint);
        //20% of height
        canvas.drawCircle((float) xPos, 0.9f * height, 0.1f * height, circlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick();
            case MotionEvent.ACTION_MOVE:
                xPos = event.getX();
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
    public double getSteeringAngle() {
        double semiWidth = 0.5 * canvasWidth;
        return (xPos - semiWidth) / (semiWidth - 0.1 * canvasHeight);
    }
}
