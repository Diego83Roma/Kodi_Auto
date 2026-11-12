package it.kodiauto.app;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Surface;
/** Foreground MediaProjection service feeding the Android Auto SurfaceView. */
public final class KodiProjectionService extends Service {
 static final String EXTRA_RESULT_CODE="kodiauto.mirror.RESULT_CODE",EXTRA_RESULT_DATA="kodiauto.mirror.RESULT_DATA";
 private static final String CHANNEL="kodiauto_screen_mirror"; private static final int NOTIFICATION=7402;
 private static volatile KodiProjectionService instance; private final Handler handler=new Handler(Looper.getMainLooper());
 private MediaProjection projection; private MediaProjection.Callback callback; private VirtualDisplay display;
 private static volatile Surface surface; private static volatile int width,height,dpi;
 static boolean isRunning(){return instance!=null&&instance.projection!=null;}
 static void attachSurface(Surface s,int w,int h,int d){surface=s;width=w;height=h;dpi=d;KodiProjectionService x=instance;if(x!=null)x.handler.post(x::updateDisplay);}
 static void stop(Context c){c.stopService(new Intent(c,KodiProjectionService.class));}
 @Override public void onCreate(){super.onCreate();instance=this;createChannel();Notification n=notification();if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)startForeground(NOTIFICATION,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);else startForeground(NOTIFICATION,n);}
 @Override public int onStartCommand(Intent i,int flags,int id){if(i==null){stopSelf(id);return START_NOT_STICKY;}int code=i.getIntExtra(EXTRA_RESULT_CODE,0);Intent data;if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)data=i.getParcelableExtra(EXTRA_RESULT_DATA,Intent.class);else{ //noinspection deprecation
 data=i.getParcelableExtra(EXTRA_RESULT_DATA);}if(data==null){stopSelf(id);return START_NOT_STICKY;}try{MediaProjectionManager m=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);projection=m.getMediaProjection(code,data);if(projection==null)throw new IllegalStateException("MediaProjection non creata");callback=new MediaProjection.Callback(){@Override public void onStop(){stopSelf();}};projection.registerCallback(callback,handler);updateDisplay();}catch(Exception e){android.util.Log.e("KodiProjectionService","Avvio mirroring non riuscito",e);stopSelf(id);}return START_NOT_STICKY;}
 private void updateDisplay(){if(projection==null||surface==null||!surface.isValid()||width<=0||height<=0||dpi<=0)return;if(display==null){display=projection.createVirtualDisplay("Kodi Auto Screen Mirror",width,height,dpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,surface,null,handler);if(display==null){android.util.Log.e("KodiProjectionService","VirtualDisplay non creato");stopSelf();}}else{display.setSurface(surface);display.resize(width,height,dpi);}}
 private void createChannel(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){NotificationManager m=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);if(m!=null)m.createNotificationChannel(new NotificationChannel(CHANNEL,"Kodi Auto Screen Mirror",NotificationManager.IMPORTANCE_LOW));}}
 private Notification notification(){Notification.Builder b=Build.VERSION.SDK_INT>=Build.VERSION_CODES.O?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Kodi Auto").setContentText("Mirroring dello schermo attivo").setOngoing(true).build();}
 @Override public void onDestroy(){if(display!=null){display.release();display=null;}if(projection!=null){if(callback!=null)projection.unregisterCallback(callback);projection.stop();projection=null;}surface=null;width=height=dpi=0;if(instance==this)instance=null;super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}