package com.hankyjcheng.qrcodescanner.activity.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * UI representation of the QR Code target box that the user can use to aim at the QR Code
 */
public class FocusSquareView extends View {

    public static final String TAG = FocusSquareView.class.getSimpleName();

    private Paint bodyPaint;
    private RectF rect;

    public FocusSquareView(Context context) {
        super(context);
        init();
    }

    public FocusSquareView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FocusSquareView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        Log.d(TAG, "Size: " + width + ", " + height);
        int bodyWidth = width / 30;
        int bodyLength = height / 3;

        rect.set(0, 0, bodyLength, bodyWidth);
        canvas.drawRect(rect, bodyPaint);
        rect.set(0, 0, bodyWidth, bodyLength);
        canvas.drawRect(rect, bodyPaint);

        rect.set(0, height - bodyLength, bodyWidth, height);
        canvas.drawRect(rect, bodyPaint);
        rect.set(0, height - bodyWidth, bodyLength, height);
        canvas.drawRect(rect, bodyPaint);

        rect.set(width - bodyLength, 0, width, bodyWidth);
        canvas.drawRect(rect, bodyPaint);
        rect.set(width - bodyWidth, 0, width, bodyLength);
        canvas.drawRect(rect, bodyPaint);

        rect.set(width - bodyLength, height - bodyWidth, width, height);
        canvas.drawRect(rect, bodyPaint);
        rect.set(width - bodyWidth, height - bodyLength, width, height);
        canvas.drawRect(rect, bodyPaint);
    }

    private void init() {
        rect = new RectF();
        bodyPaint = new Paint();
        bodyPaint.setColor(Color.parseColor("#FFFFFF"));
        bodyPaint.setStyle(Paint.Style.FILL);
        bodyPaint.setAntiAlias(true);
    }

}