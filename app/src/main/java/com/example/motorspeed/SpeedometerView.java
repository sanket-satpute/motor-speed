package com.example.motorspeed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class SpeedometerView extends View {

    private Paint speedometerPaint;
    private RectF speedometerRect;
    private Paint needlePaint;
    private Paint textPaint;
    private float currentValue;

    private int targetProgress;
    private int targetScore;
    private long animationDuration;
    private long startTime;

    public SpeedometerView(Context context) {
        super(context);
        init();
    }

    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        speedometerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        speedometerPaint.setStyle(Paint.Style.STROKE);
        speedometerPaint.setStrokeWidth(20);

        speedometerRect = new RectF(100, 100, 400, 400);

        Shader shader = new SweepGradient(
                speedometerRect.centerX(),
                speedometerRect.centerY(),
                new int[]{Color.GREEN, Color.YELLOW, Color.RED},
                null);

        Matrix gradientMatrix = new Matrix();
        shader.getLocalMatrix(gradientMatrix);
        gradientMatrix.preRotate(135, speedometerRect.centerX(), speedometerRect.centerY());
        shader.setLocalMatrix(gradientMatrix);

        speedometerPaint.setShader(shader);

        needlePaint = new Paint();
        needlePaint.setColor(Color.RED);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeWidth(5);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(20);
        textPaint.setTextAlign(Paint.Align.CENTER);

        currentValue = 0; // Initial value

        targetProgress = 0;
        targetScore = 0;
        animationDuration = 1000; // Animation duration in milliseconds
        startTime = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(speedometerRect, 135, 270, false, speedometerPaint);

        drawSpeedLabels(canvas);

        float angle = 135 + (currentValue / 100) * 270;
        float x = (float) (250 + 150 * Math.cos(Math.toRadians(angle)));
        float y = (float) (250 + 150 * Math.sin(Math.toRadians(angle)));
        canvas.drawLine(250, 250, x, y, needlePaint);
    }

    public void setCurrentValue(float value) {
        if (value < 0) {
            currentValue = 0;
        } else if (value > 100) {
            currentValue = 100;
        } else {
            currentValue = value;
        }
        invalidate(); // Trigger redraw
    }

    private void drawSpeedLabels(Canvas canvas) {
        for (int i = 0; i <= 100; i += 10) {
            float angle = 135 + (i / 100f) * 270;
            float x = (float) (250 + 180 * Math.cos(Math.toRadians(angle)));
            float y = (float) (250 + 180 * Math.sin(Math.toRadians(angle)));
            canvas.drawText(String.valueOf(i), x, y, textPaint);
        }
    }

    public void updateScore(int progress, int score) {
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }

        if (currentValue != progress) {
            targetProgress = progress;
            targetScore = score;
            startTime = System.currentTimeMillis();
            invalidate(); // Trigger redraw
        }
    }

    private float interpolateValue(float startValue, float endValue, float progress) {
        return startValue + (endValue - startValue) * progress;
    }

    public float getCurrentValue() {
        return currentValue;
    }
}
