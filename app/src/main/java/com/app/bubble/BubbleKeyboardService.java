package com.app.bubble;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

/**
 * A fully functional QWERTY keyboard with a special button to launch the Bubble Copy tool.
 */
public class BubbleKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard keyboard;

    // Custom Key Code defined in qwerty.xml for the Copy button
    private static final int CODE_LAUNCH_COPY = -10;

    @Override
    public View onCreateInputView() {
        // Inflate the layout containing the KeyboardView
        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.layout_real_keyboard, null);
        
        // Load the QWERTY layout definition
        keyboard = new Keyboard(this, R.xml.qwerty);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        
        return kv;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                // Handle Backspace
                ic.deleteSurroundingText(1, 0);
                break;
                
            case Keyboard.KEYCODE_SHIFT:
                // Simple shift toggle logic
                keyboard.setShifted(!keyboard.isShifted());
                kv.invalidateAllKeys();
                break;
                
            case Keyboard.KEYCODE_DONE:
            case -4: // Enter
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
                
            case CODE_LAUNCH_COPY:
                // *** THE SPECIAL FEATURE ***
                // 1. Hide the keyboard
                requestHideSelf(0);
                
                // 2. Launch the Two-Line Overlay Service
                Intent intent = new Intent(BubbleKeyboardService.this, TwoLineOverlayService.class);
                startService(intent);
                break;
                
            default:
                // Handle standard characters
                char code = (char) primaryCode;
                if (Character.isLetter(code) && keyboard.isShifted()) {
                    code = Character.toUpperCase(code);
                }
                ic.commitText(String.valueOf(code), 1);
        }
    }

    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}