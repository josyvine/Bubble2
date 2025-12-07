package com.app.bubble;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class DebugActivity extends Activity {

    // STATIC variables to share data from Service to Activity.
    // We use static because Bitmaps are too big to pass through Intents.
    public static Bitmap sLastBitmap = null;
    public static Rect sLastRect = null;
    public static String sRawText = "";
    public static String sFilteredText = "";
    public static String sErrorLog = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        TextView console = findViewById(R.id.debug_console);
        ImageView imageView = findViewById(R.id.debug_image);

        // 1. Build the Log Report
        StringBuilder log = new StringBuilder();
        log.append("=== DEBUG REPORT ===\n");
        
        if (sErrorLog != null && !sErrorLog.isEmpty()) {
            log.append("!! ERRORS FOUND !!\n").append(sErrorLog).append("\n\n");
        }
        
        if (sLastRect != null) {
            log.append("Crop Lines: Green Y=").append(sLastRect.top)
               .append(" | Red Y=").append(sLastRect.bottom).append("\n");
        }
        
        if (sLastBitmap != null) {
            log.append("Image Size: W=").append(sLastBitmap.getWidth())
               .append(" x H=").append(sLastBitmap.getHeight()).append("\n");
        } else {
            log.append("Image: NULL (Capture failed)\n");
        }

        log.append("----------------------------\n");
        log.append("FILTERED RESULT (Final Output):\n[").append(sFilteredText).append("]\n");
        log.append("----------------------------\n");
        log.append("RAW OCR TEXT (Before Filter):\n").append(sRawText);
        
        console.setText(log.toString());

        // 2. Draw Lines on the Bitmap to visualize the mismatch
        if (sLastBitmap != null && sLastRect != null) {
            try {
                // Create a mutable copy of the bitmap so we can draw on it
                Bitmap mutableBitmap = sLastBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);
                Paint paint = new Paint();
                paint.setStrokeWidth(15); // Thick lines
                
                // Draw Green Line (Start)
                paint.setColor(Color.GREEN);
                canvas.drawLine(0, sLastRect.top, mutableBitmap.getWidth(), sLastRect.top, paint);

                // Draw Red Line (End)
                paint.setColor(Color.RED);
                canvas.drawLine(0, sLastRect.bottom, mutableBitmap.getWidth(), sLastRect.bottom, paint);

                imageView.setImageBitmap(mutableBitmap);
            } catch (Exception e) {
                console.append("\n\nError drawing lines: " + e.getMessage());
                imageView.setImageBitmap(sLastBitmap);
            }
        }
    }
}