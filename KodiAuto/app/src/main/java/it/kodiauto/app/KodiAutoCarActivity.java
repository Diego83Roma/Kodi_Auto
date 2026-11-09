package it.kodiauto.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.common.util.concurrent.ListenableFuture;

import android.content.ComponentName;

/**
 * Car-safe Kodi Auto control surface. Playback remains inside Kodi; buttons
 * control the same MediaLibrarySession exposed to Android Auto's media browser.
 */
public final class KodiAutoCarActivity extends CarActivity {
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private TextView title;
    private TextView subtitle;
    private TextView state;

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onMediaMetadataChanged(MediaMetadata metadata) {
            render();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            render();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            render();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setIgnoreConfigChanges(0xFFFFFFFF);
        CarUiController ui = getCarUiController();
        ui.getStatusBarController().hideAppHeader();
        ui.getMenuController().hideMenuButton();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(36, 28, 36, 28);
        root.setBackgroundColor(Color.rgb(7, 14, 10));

        TextView brand = new TextView(this);
        brand.setText("KODI AUTO");
        brand.setTextColor(Color.rgb(0, 255, 65));
        brand.setTextSize(22);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        title = new TextView(this);
        title.setText("Collegamento a Kodi");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = 28;
        root.addView(title, titleParams);

        subtitle = new TextView(this);
        subtitle.setText("Apri la libreria multimediale per scegliere musica e contenuti.");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        state = new TextView(this);
        state.setText("Connessione in corso…");
        state.setTextColor(Color.rgb(0, 255, 65));
        state.setTextSize(13);
        state.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stateParams.topMargin = 20;
        root.addView(state, stateParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsParams.topMargin = 30;

        Button previous = new Button(this);
        previous.setText("⏮");
        previous.setOnClickListener(v -> {
            if (controller != null) controller.seekToPreviousMediaItem();
        });
        controls.addView(previous);

        Button playPause = new Button(this);
        playPause.setText("▶ / ❚❚");
        playPause.setOnClickListener(v -> {
            if (controller == null) return;
            if (controller.isPlaying()) controller.pause();
            else controller.play();
        });
        controls.addView(playPause);

        Button next = new Button(this);
        next.setText("⏭");
        next.setOnClickListener(v -> {
            if (controller != null) controller.seekToNextMediaItem();
        });
        controls.addView(next);

        root.addView(controls, controlsParams);
        setContentView(root);
        connectMediaSession();
    }

    private void connectMediaSession() {
        SessionToken token = new SessionToken(
                this, new ComponentName(this, KodiAutoMediaLibraryService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(listener);
                render();
            } catch (Exception error) {
                state.setText("Servizio multimediale non disponibile");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void render() {
        if (controller == null) return;
        MediaMetadata metadata = controller.getMediaMetadata();
        CharSequence name = metadata.title;
        CharSequence artist = metadata.artist;
        title.setText(name == null ? "Nessun contenuto in riproduzione" : name);
        subtitle.setText(artist == null ? "Libreria Kodi Auto" : artist);
        state.setText(controller.isPlaying() ? "IN RIPRODUZIONE" : "IN PAUSA");
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.removeListener(listener);
            controller.release();
            controller = null;
        }
        if (controllerFuture != null && !controllerFuture.isDone()) {
            MediaController.releaseFuture(controllerFuture);
        }
        super.onDestroy();
    }
}
