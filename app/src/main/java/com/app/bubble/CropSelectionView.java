package com.app.bubble;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

public class CropSelectionView extends View {

    private Paint paint;
    private Paint borderPaint;
    private float startX, startY, endX, endY;
    private RectF selectionRect = new RectF();

    private Handler autoCloseHandler = new Handler(Looper.getMainLooper());
    private Runnable autoCloseRunnable;
    private long timeoutDuration; // This will now be loaded from SharedPreferences

    public CropSelectionView(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.argb(100, 0, 100, 255)); // Transparent blue
        paint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.BLUE);
        borderPaint.setStrokeWidth(5f);
        borderPaint.setStyle(Paint.Style.STROKE);

        // *** MODIFICATION START ***
        // Read the user-configured timer duration from SharedPreferences.
        // We use the constants defined in SettingsActivity for consistency.
        SharedPreferences prefs = getContext().getSharedPreferences(
            SettingsActivity.PREFS_NAME, 
            Context.MODE_PRIVATE
        );
        // Load the saved duration, defaulting to 5000ms (5 seconds) if not found.
        timeoutDuration = prefs.getLong(SettingsActivity.KEY_TIMER_DURATION, 5000L);
        // *** MODIFICATION END ***

        autoCloseRunnable = new Runnable() {
            @Override
            public void run() {
                // Tell the service that selection is finished
                FloatingTranslatorService service = (FloatingTranslatorService) getContext();
                Rect finalRect = new Rect(
                    (int) selectionRect.left,
                    (int) selectionRect.top,
                    (int) selectionRect.right,
                    (int) selectionRect.bottom
                );
                service.onCropFinished(finalRect);
            }
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        selectionRect.set(
            Math.min(startX, endX),
            Math.min(startY, endY),
            Math.max(startX, endX),
            Math.max(startY, endY)
        );
        // Draw the blue fill
        canvas.drawRect(selectionRect, paint);
        // Draw the border
        canvas.drawRect(selectionRect, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                endX = startX;
                endY = startY;
                resetAutoCloseTimer();
                invalidate(); // Redraw the view
                return true;
            case MotionEvent.ACTION_MOVE:
                endX = event.getRawX();
                endY = event.getRawY();
                resetAutoCloseTimer();
                invalidate(); // Redraw the view
                return true;
            case MotionEvent.ACTION_UP:
                // The timer will fire after the timeout duration with no more movement
                return true;
        }
        return false;
    }

    private void resetAutoCloseTimer() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable);
        autoCloseHandler.postDelayed(autoCloseRunnable, timeoutDuration);
    }
}

