package it.kodiauto.app;

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;

/** Kodi's real phone display projected onto the Android Auto surface. */
public final class KodiMirrorCarActivity extends CarActivity implements SurfaceHolder.Callback {
    private SurfaceView mirrorSurface;
    private Path touchPath;
    private long touchDownTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setIgnoreConfigChanges(0xFFFFFFFF);

        CarUiController ui = getCarUiController();
        ui.getStatusBarController().hideAppHeader();
        ui.getMenuController().hideMenuButton();

        FrameLayout root = new FrameLayout(this);
        mirrorSurface = new SurfaceView(this);
        mirrorSurface.getHolder().addCallback(this);
        mirrorSurface.setOnTouchListener((view, event) -> forwardTouch(view, event));
        root.addView(mirrorSurface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Fermata-style navigation controls: Back and Home are sent to the phone,
        // while the rest of the surface remains a direct touch area for Kodi.
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER);
        toolbar.setPadding(8, 8, 8, 8);
        toolbar.setBackgroundColor(Color.argb(175, 12, 18, 14));
        toolbar.addView(makeToolButton("⌂", true));
        toolbar.addView(makeToolButton("‹", false));
        FrameLayout.LayoutParams toolbarParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        toolbarParams.topMargin = 14;
        toolbarParams.rightMargin = 14;
        root.addView(toolbar, toolbarParams);

        setContentView(root);

        if (!KodiInputAccessibilityService.isConnected()) {
            android.widget.Toast.makeText(this,
                    "Per controllare Kodi dall'auto, abilita 'Kodi Auto Input' nelle impostazioni di Accessibilità del telefono.",
                    android.widget.Toast.LENGTH_LONG).show();
        }
        KodiProjectionActivity.request(getApplicationContext());
    }

    private TextView makeToolButton(String label, boolean home) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(25);
        button.setGravity(Gravity.CENTER);
        button.setPadding(18, 8, 18, 8);
        button.setBackgroundColor(Color.argb(210, 28, 42, 32));
        button.setContentDescription(home ? "Schermata Home del telefono" : "Indietro in Kodi");
        button.setOnClickListener(v -> {
            boolean sent = home
                    ? KodiInputAccessibilityService.dispatchHome()
                    : KodiInputAccessibilityService.dispatchBack();
            if (!sent) {
                android.widget.Toast.makeText(this,
                        "Abilita Kodi Auto Input nelle impostazioni di Accessibilità.",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        return button;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mirrorSurface != null && mirrorSurface.getHolder().getSurface().isValid()) {
            attach(mirrorSurface.getHolder().getSurface(),
                    mirrorSurface.getWidth(), mirrorSurface.getHeight());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        attach(holder.getSurface(), holder.getSurfaceFrame().width(),
                holder.getSurfaceFrame().height());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        attach(holder.getSurface(), width, height);
    }

    private void attach(Surface surface, int width, int height) {
        KodiProjectionService.attachSurface(surface, width, height,
                getResources().getDisplayMetrics().densityDpi);
    }

    private boolean forwardTouch(android.view.View view, MotionEvent event) {
        int action = event.getActionMasked();
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (manager == null || view.getWidth() <= 0 || view.getHeight() <= 0) return true;

        Point phone = new Point();
        manager.getDefaultDisplay().getRealSize(phone);
        if (phone.x <= 0 || phone.y <= 0) return true;

        // Match the fit-center scaling used by screen mirroring. This avoids
        // sending touches to the wrong place when phone and car aspect ratios differ.
        float scale = Math.min(view.getWidth() / (float) phone.x,
                view.getHeight() / (float) phone.y);
        float offsetX = (view.getWidth() - phone.x * scale) / 2f;
        float offsetY = (view.getHeight() - phone.y * scale) / 2f;
        float x = (event.getX() - offsetX) / scale;
        float y = (event.getY() - offsetY) / scale;
        x = Math.max(0f, Math.min(phone.x - 1f, x));
        y = Math.max(0f, Math.min(phone.y - 1f, y));

        if (action == MotionEvent.ACTION_DOWN) {
            touchDownTime = event.getEventTime();
            touchPath = new Path();
            touchPath.moveTo(x, y);
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (touchPath != null) touchPath.lineTo(x, y);
        } else if (action == MotionEvent.ACTION_UP) {
            if (touchPath != null) {
                touchPath.lineTo(x, y);
                long duration = Math.max(50L,
                        Math.min(5000L, event.getEventTime() - touchDownTime));
                KodiInputAccessibilityService.dispatch(touchPath, duration);
            }
            touchPath = null;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            touchPath = null;
        }
        return true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        touchPath = null;
        KodiProjectionService.attachSurface(null, 0, 0, 0);
    }

    @Override
    public void onBackPressed() {
        if (!KodiInputAccessibilityService.dispatchBack()) super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        KodiProjectionService.stop(this);
        super.onDestroy();
    }
}