package it.kodiauto.app;
import android.os.Bundle;
import android.graphics.Path;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
/** Experimental full-screen Android Auto mirror surface. */
public final class KodiMirrorCarActivity extends CarActivity implements SurfaceHolder.Callback {
 private SurfaceView mirrorSurface;
 private Path touchPath;
 private long touchDownTime;
 @Override public void onCreate(Bundle savedInstanceState){
  super.onCreate(savedInstanceState); setIgnoreConfigChanges(0xFFFFFFFF);
  CarUiController ui=getCarUiController(); ui.getStatusBarController().hideAppHeader(); ui.getMenuController().hideMenuButton();
  mirrorSurface=new SurfaceView(this);
  mirrorSurface.getHolder().addCallback(this);
  mirrorSurface.setOnTouchListener((view,event)->forwardTouch(view,event));
  setContentView(mirrorSurface);
  if(!KodiInputAccessibilityService.isConnected())
   android.widget.Toast.makeText(this,"Per controllare Kodi dall'auto, abilita 'Kodi Auto Input' nelle impostazioni di Accessibilità del telefono.",android.widget.Toast.LENGTH_LONG).show();
  KodiProjectionActivity.request(getApplicationContext());
 }
 @Override public void onResume(){super.onResume(); if(mirrorSurface!=null&&mirrorSurface.getHolder().getSurface().isValid()) attach(mirrorSurface.getHolder().getSurface(),mirrorSurface.getWidth(),mirrorSurface.getHeight());}
 @Override public void surfaceCreated(SurfaceHolder h){attach(h.getSurface(),h.getSurfaceFrame().width(),h.getSurfaceFrame().height());}
 @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int z){attach(h.getSurface(),w,z);}
 private void attach(Surface s,int w,int h){KodiProjectionService.attachSurface(s,w,h,getResources().getDisplayMetrics().densityDpi);}
 private boolean forwardTouch(android.view.View view,MotionEvent event){
  int action=event.getActionMasked();
  WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  if(wm==null||view.getWidth()<=0||view.getHeight()<=0)return true;
  Point size=new Point(); wm.getDefaultDisplay().getRealSize(size);
  float x=event.getX()*size.x/view.getWidth();
  float y=event.getY()*size.y/view.getHeight();
  if(action==MotionEvent.ACTION_DOWN){
   touchDownTime=event.getEventTime(); touchPath=new Path(); touchPath.moveTo(x,y);
  }else if(action==MotionEvent.ACTION_MOVE){
   if(touchPath!=null)touchPath.lineTo(x,y);
  }else if(action==MotionEvent.ACTION_UP){
   if(touchPath!=null){
    touchPath.lineTo(x,y);
    long duration=Math.max(50L,Math.min(5000L,event.getEventTime()-touchDownTime));
    KodiInputAccessibilityService.dispatch(touchPath,duration);
   }
   touchPath=null;
  }else if(action==MotionEvent.ACTION_CANCEL){touchPath=null;}
  return true;
 }
 @Override public void surfaceDestroyed(SurfaceHolder h){KodiProjectionService.attachSurface(null,0,0,0);}
 @Override public void onDestroy(){KodiProjectionService.stop(this);super.onDestroy();}
}