package it.kodiauto.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility input bridge for the Android Auto mirror.
 * It intentionally does not inspect Kodi's window content.
 */
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
        long duration = Math.max(1L, Math.min(
                GestureDescription.getMaxGestureDuration(), durationMs));
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(new Path(path), 0L, duration))
                .build();
        return service.dispatchGesture(gesture, null, null);
    }

    static boolean dispatchBack() {
        KodiInputAccessibilityService service = instance;
        return service != null && service.performGlobalAction(GLOBAL_ACTION_BACK);
    }

    static boolean dispatchHome() {
        KodiInputAccessibilityService service = instance;
        return service != null && service.performGlobalAction(GLOBAL_ACTION_HOME);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        android.util.Log.i("KodiAutoInput", "Accessibility input connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Input-only service: no window text or content is collected.
    }

    @Override
    public void onInterrupt() {
        // No continuous event stream to interrupt.
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }
}