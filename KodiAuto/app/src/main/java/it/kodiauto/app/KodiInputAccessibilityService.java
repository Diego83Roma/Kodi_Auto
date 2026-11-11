package it.kodiauto.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

/** Receives Android Auto touch gestures and replays them on the phone's Kodi display. */
public final class KodiInputAccessibilityService extends AccessibilityService {
    private static volatile KodiInputAccessibilityService instance;

    static boolean isConnected() {
        return instance != null;
    }

    static boolean dispatch(Path path, long durationMs) {
        KodiInputAccessibilityService service = instance;
        if (service == null || path == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        long duration = Math.max(50L, Math.min(60000L, durationMs));
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0L, duration))
                .build();
        return service.dispatchGesture(gesture, null, null);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        android.util.Log.i("KodiAutoInput", "Accessibility input connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // The service is used only as an input injection endpoint.
    }

    @Override
    public void onInterrupt() {
        // No continuous feedback stream to interrupt.
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
