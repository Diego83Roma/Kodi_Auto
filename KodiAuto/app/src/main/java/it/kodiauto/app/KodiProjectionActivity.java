package it.kodiauto.app;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
/** Phone-side user consent flow; no root or accessibility permission is required for video capture. */
public final class KodiProjectionActivity extends Activity {
 private static final int REQUEST_CAPTURE=7401;
 private static volatile boolean requestPending;
 static void request(Context c){if(requestPending||KodiProjectionService.isRunning())return;requestPending=true;Intent i=new Intent(c,KodiProjectionActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(i);}
 @Override protected void onCreate(Bundle b){super.onCreate(b);MediaProjectionManager m=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);if(m==null){requestPending=false;Toast.makeText(this,"MediaProjection non disponibile",Toast.LENGTH_LONG).show();finish();return;}startActivityForResult(m.createScreenCaptureIntent(),REQUEST_CAPTURE);}
 @Override protected void onActivityResult(int r,int code,Intent data){super.onActivityResult(r,code,data);if(r!=REQUEST_CAPTURE)return;requestPending=false;if(code!=RESULT_OK||data==null){Toast.makeText(this,"Autorizzazione mirroring non concessa",Toast.LENGTH_LONG).show();finish();return;}Intent s=new Intent(this,KodiProjectionService.class);s.putExtra(KodiProjectionService.EXTRA_RESULT_CODE,code);s.putExtra(KodiProjectionService.EXTRA_RESULT_DATA,data);if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)startForegroundService(s);else startService(s);Intent launch=getPackageManager().getLaunchIntentForPackage(getPackageName());if(launch!=null){launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(launch);}finish();}
 @Override protected void onDestroy(){requestPending=false;super.onDestroy();}
}