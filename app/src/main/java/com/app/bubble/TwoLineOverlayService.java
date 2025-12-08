package com.app.bubble;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TwoLineOverlayService extends Service {

    private WindowManager windowManager;

    // Window 1: The Lines (Full Screen)
    private View linesView;
    private WindowManager.LayoutParams linesParams;
    private View lineTop, lineBottom;
    private ImageView handleTop, handleBottom;
    private TextView helperText;

    // Window 2: The Controls (Bottom Only)
    private View controlsView;
    private WindowManager.LayoutParams controlsParams;
    private Button btnAction;
    private ImageView btnClose;

    private int currentState = 0; // 0=Set Green, 1=Scrolling, 2=Set Red
    private int screenHeight;

    // For Touch Logic
    private View activeDragView = null;
    private View activeHandleView = null;
    private float initialTouchY, initialViewY;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        screenHeight = getResources().getDisplayMetrics().heightPixels;

        setupWindows();
    }

    private void setupWindows() {
        int type = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        LayoutInflater inflater = LayoutInflater.from(this);

        // --- SETUP LINES WINDOW (Full Screen) ---
        linesView = inflater.inflate(R.layout.layout_two_line_overlay, null);
        linesView.findViewById(R.id.btn_action).setVisibility(View.GONE);
        linesView.findViewById(R.id.btn_close).setVisibility(View.GONE);

        lineTop = linesView.findViewById(R.id.line_top);
        handleTop = linesView.findViewById(R.id.handle_top);
        lineBottom = linesView.findViewById(R.id.line_bottom);
        handleBottom = linesView.findViewById(R.id.handle_bottom);
        helperText = linesView.findViewById(R.id.helper_text);

        linesParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                // FIX: REMOVED FLAG_SECURE so the screenshot is not black
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, 
                PixelFormat.TRANSLUCENT
        );
        windowManager.addView(linesView, linesParams);

        // --- SETUP CONTROLS WINDOW (Bottom) ---
        controlsView = inflater.inflate(R.layout.layout_two_line_overlay, null);
        controlsView.findViewById(R.id.line_top).setVisibility(View.GONE);
        controlsView.findViewById(R.id.handle_top).setVisibility(View.GONE);
        controlsView.findViewById(R.id.line_bottom).setVisibility(View.GONE);
        controlsView.findViewById(R.id.handle_bottom).setVisibility(View.GONE);
        controlsView.findViewById(R.id.helper_text).setVisibility(View.GONE);

        btnAction = controlsView.findViewById(R.id.btn_action);
        btnClose = controlsView.findViewById(R.id.btn_close);

        controlsParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                // FIX: REMOVED FLAG_SECURE here as well
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, 
                PixelFormat.TRANSLUCENT
        );
        controlsParams.gravity = Gravity.BOTTOM | Gravity.END;
        controlsParams.y = 50; // Margin bottom
        windowManager.addView(controlsView, controlsParams);

        setupTouchListeners();
        setupControlLogic();
    }

    private void setupControlLogic() {
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalScrollService.stopScroll();
                FloatingTranslatorService service = FloatingTranslatorService.getInstance();
                if (service != null) service.stopBurstCapture();
                stopSelf();
            }
        });

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState == 0) {
                    // STATE: GREEN LINE SET -> START SCROLLING
                    currentState = 1;

                    handleTop.setVisibility(View.GONE);
                    helperText.setText("Scroll to end of text.\nClick STOP when done.");

                    btnAction.setText("STOP SCROLL");
                    btnAction.setBackgroundColor(0xFFFF0000); // Red

                    // Make Lines Window "Pass Through" so user can scroll browser
                    // FIX: REMOVED FLAG_SECURE
                    linesParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | 
                                      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    windowManager.updateViewLayout(linesView, linesParams);

                    FloatingTranslatorService service = FloatingTranslatorService.getInstance();
                    if (service != null) service.startBurstCapture();

                } else if (currentState == 1) {
                    // STATE: SCROLLING DONE -> SHOW RED LINE
                    currentState = 2;

                    FloatingTranslatorService service = FloatingTranslatorService.getInstance();
                    if (service != null) service.stopBurstCapture();

                    // Make Lines Window Touchable again
                    // FIX: REMOVED FLAG_SECURE
                    linesParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                                      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                    windowManager.updateViewLayout(linesView, linesParams);

                    lineBottom.setVisibility(View.VISIBLE);
                    handleBottom.setVisibility(View.VISIBLE);

                    helperText.setText("Place Red Line at END of text.");
                    btnAction.setText("COPY");
                    btnAction.setBackgroundColor(0xFF4CAF50); // Green

                } else {
                    // STATE: RED LINE SET -> COPY
                    finishCopy();
                }
            }
        });
    }

    private void finishCopy() {
        int[] topLocation = new int[2];
        lineTop.getLocationOnScreen(topLocation);
        int[] bottomLocation = new int[2];
        lineBottom.getLocationOnScreen(bottomLocation);

        int topY = topLocation[1];
        int bottomY = bottomLocation[1];

        if (topY >= bottomY) {
            Toast.makeText(this, "Top line must be above bottom line", Toast.LENGTH_SHORT).show();
            return;
        }

        Rect selectionRect = new Rect(0, topY, getResources().getDisplayMetrics().widthPixels, bottomY);

        Intent intent = new Intent(this, FloatingTranslatorService.class);
        intent.putExtra("RECT", selectionRect);
        intent.putExtra("COPY_TO_CLIPBOARD", true);
        startService(intent);

        Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show();
        stopSelf();
    }

    private void setupTouchListeners() {
        // "Fat Finger" Logic: Touch anywhere on the screen to drag the lines
        linesView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Ignore touches during Scroll Mode
                if (currentState == 1) return false;

                float rawY = event.getRawY();

                // Sensitivity threshold (how close you need to be to grab a line)
                int threshold = 150; 

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        float distTop = Math.abs(rawY - getCenterY(lineTop));
                        float distBottom = Math.abs(rawY - getCenterY(lineBottom));

                        // Decide which line to grab
                        if (currentState == 0) {
                            // Only Top line is active
                            if (distTop < threshold) {
                                activeDragView = lineTop;
                                activeHandleView = handleTop;
                                initialTouchY = rawY;
                                initialViewY = lineTop.getY();
                                return true;
                            }
                        } else if (currentState == 2) {
                            // Both lines active, grab the closest one
                            if (distTop < threshold && distTop < distBottom) {
                                activeDragView = lineTop;
                                activeHandleView = handleTop;
                                initialTouchY = rawY;
                                initialViewY = lineTop.getY();
                                return true;
                            } else if (distBottom < threshold) {
                                activeDragView = lineBottom;
                                activeHandleView = handleBottom;
                                initialTouchY = rawY;
                                initialViewY = lineBottom.getY();
                                return true;
                            }
                        }
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        if (activeDragView != null) {
                            float dY = rawY - initialTouchY;
                            float newY = initialViewY + dY;

                            // Clamp to screen bounds
                            if (newY < 0) newY = 0;
                            if (newY > screenHeight - activeDragView.getHeight()) newY = screenHeight - activeDragView.getHeight();

                            activeDragView.setY(newY);
                            // Also move the handle if it's separate in the layout
                            if (activeHandleView != null) {
                                // Assuming handle is centered or aligned; just sync Y roughly or rely on layout
                                // Ideally the layout moves handles with lines, but if not:
                                activeHandleView.setY(newY); 
                            }
                            return true;
                        }
                        return false;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        activeDragView = null;
                        activeHandleView = null;
                        return true;
                }
                return false;
            }
        });
    }

    private float getCenterY(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location[1] + (view.getHeight() / 2f);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (linesView != null) windowManager.removeView(linesView);
        if (controlsView != null) windowManager.removeView(controlsView);
    }
}