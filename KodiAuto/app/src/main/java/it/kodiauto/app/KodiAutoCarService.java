package it.kodiauto.app;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

/** Android Auto custom projection entry, following Fermata's CarActivityService pattern. */
public final class KodiAutoCarService extends CarActivityService {
    @Override
    public Class<? extends CarActivity> getCarActivity() {
        return KodiAutoCarActivity.class;
    }
}
