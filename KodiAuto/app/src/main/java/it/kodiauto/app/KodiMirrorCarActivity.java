package it.kodiauto.app;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
/** Experimental full-screen Android Auto mirror surface. */
public final class KodiMirrorCarActivity extends CarActivity implements SurfaceHolder.Callback {
 private SurfaceView mirrorSurface;
 @Override public void onCreate(Bundle savedInstanceState){
  super.onCreate(savedInstanceState); setIgnoreConfigChanges(0xFFFFFFFF);
  CarUiController ui=getCarUiController(); ui.getStatusBarController().hideAppHeader(); ui.getMenuController().hideMenuButton();
  mirrorSurface=new SurfaceView(this); mirrorSurface.getHolder().addCallback(this); setContentView(mirrorSurface);
  KodiProjectionActivity.request(getApplicationContext());
 }
 @Override public void onResume(){super.onResume(); if(mirrorSurface!=null&&mirrorSurface.getHolder().getSurface().isValid()) attach(mirrorSurface.getHolder().getSurface(),mirrorSurface.getWidth(),mirrorSurface.getHeight());}
 @Override public void surfaceCreated(SurfaceHolder h){attach(h.getSurface(),h.getSurfaceFrame().width(),h.getSurfaceFrame().height());}
 @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int z){attach(h.getSurface(),w,z);}
 private void attach(Surface s,int w,int h){KodiProjectionService.attachSurface(s,w,h,getResources().getDisplayMetrics().densityDpi);}
 @Override public void surfaceDestroyed(SurfaceHolder h){KodiProjectionService.attachSurface(null,0,0,0);}
 @Override public void onDestroy(){KodiProjectionService.stop(this);super.onDestroy();}
}