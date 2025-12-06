package com.app.bubble;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.widget.Button;

/**
 * This service acts as a custom keyboard extension.
 * Instead of typing keys, it provides a shortcut to launch the Two-Line Copy overlay.
 */
public class BubbleKeyboardService extends InputMethodService {

    @Override
    public View onCreateInputView() {
        // Inflate the custom keyboard layout
        // We will create 'layout_keyboard_view.xml' in the resources next.
        View keyboardView = getLayoutInflater().inflate(R.layout.layout_keyboard_view, null);

        // Find the "Start Multi-Page Copy" button
        Button startCopyButton = keyboardView.findViewById(R.id.button_start_copy);

        if (startCopyButton != null) {
            startCopyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 1. Hide the keyboard so the screen content is fully visible
                    requestHideSelf(0);

                    // 2. Launch the Two-Line Overlay Service to draw the lines
                    Intent intent = new Intent(BubbleKeyboardService.this, TwoLineOverlayService.class);
                    // We need to start it as a Foreground Service typically, but since
                    // we are interacting directly, startService works if the app has permission.
                    startService(intent);
                }
            });
        }

        return keyboardView;
    }
}