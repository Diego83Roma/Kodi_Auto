package it.kodiauto.app;

import android.util.Log;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

/** Android Auto projection service with explicit lifecycle handling, aligned with Fermata. */
public final class KodiAutoCarService extends CarActivityService {
    private static final String TAG = "KodiAutoCarService";

    @Override
    public Class<? extends CarActivity> getCarActivity() {
        return KodiAutoCarActivity.class;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Car projection service created");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Car projection service destroyed");
        try {
            KodiAutoCarActivity.onCarServiceDestroyed();
        } finally {
            super.onDestroy();
        }
    }
}
