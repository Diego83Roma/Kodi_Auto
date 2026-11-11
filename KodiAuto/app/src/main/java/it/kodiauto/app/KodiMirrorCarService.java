package it.kodiauto.app;
import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;
public final class KodiMirrorCarService extends CarActivityService {
 @Override public Class<? extends CarActivity> getCarActivity(){return KodiMirrorCarActivity.class;}
 @Override public void onCreate(){super.onCreate();}
 @Override public void onDestroy(){KodiProjectionService.stop(this);super.onDestroy();}
}