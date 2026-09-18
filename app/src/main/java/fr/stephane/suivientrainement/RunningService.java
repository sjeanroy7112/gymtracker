package fr.stephane.suivientrainement;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunningService extends Service implements LocationListener, SensorEventListener, TextToSpeech.OnInitListener {
    public static volatile boolean active=false, paused=false;
    public static volatile boolean indoor=false;
    public static volatile double treadmillSpeedKmh=8.0;
    public static volatile double distanceMeters=0;
    public static volatile long elapsedSeconds=0;
    public static volatile int steps=0;
    public static volatile boolean ftmsConnected=false;
    public static volatile double machineCadence=0,machinePower=0,machineCalories=0;
    public static volatile int machineHeartRate=0;
    private static final String CHANNEL="running_tracking";
    private static final int NOTIFICATION_ID=180;
    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Location lastLocation;
    private float initialSteps=-1;
    private long startedAt, pausedAt, totalPaused;
    private int announcedKm=0;
    private long lastKilometerElapsedSeconds=0;
    private TextToSpeech tts;
    private final List<double[]> routePoints=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){public void run(){if(active){if(indoor&&!paused&&!ftmsConnected)distanceMeters+=treadmillSpeedKmh/3.6;updateElapsed();checkKilometer();if(elapsedSeconds%5==0)updateNotification();handler.postDelayed(this,1000);}}};

    @Override public void onCreate(){super.onCreate();createChannel();tts=new TextToSpeech(this,this);}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?"START":intent.getStringExtra("action");
        if("STOP".equals(action)){finishRun();return START_NOT_STICKY;}
        if("PAUSE".equals(action)){pauseRun();return START_NOT_STICKY;}
        if("RESUME".equals(action)){resumeRun();return START_NOT_STICKY;}
        if("SET_SPEED".equals(action)){treadmillSpeedKmh=Math.max(1,Math.min(25,intent.getDoubleExtra("speed",treadmillSpeedKmh)));updateNotification();return START_NOT_STICKY;}
        if(!active)startRun("START_INDOOR".equals(action));return START_NOT_STICKY;
    }

    private void startRun(boolean indoorMode){
        active=true;paused=false;indoor=indoorMode;treadmillSpeedKmh=8.0;distanceMeters=0;elapsedSeconds=0;steps=0;machineCadence=0;machinePower=0;machineCalories=0;machineHeartRate=0;startedAt=System.currentTimeMillis();totalPaused=0;announcedKm=0;lastKilometerElapsedSeconds=0;lastLocation=null;initialSteps=-1;routePoints.clear();
        Notification first=notification(indoor?"Vitesse du tapis : 8,0 km/h":"Recherche du signal GPS…");
        if(Build.VERSION.SDK_INT>=29){int type=indoor?(Build.VERSION.SDK_INT>=34?ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH:ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE):ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;startForeground(NOTIFICATION_ID,first,type);}else startForeground(NOTIFICATION_ID,first);
        locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if(!indoor)try{locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,3,this);}catch(SecurityException ignored){}
        Sensor step=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);if(step!=null)sensorManager.registerListener(this,step,SensorManager.SENSOR_DELAY_NORMAL);
        handler.post(ticker);
    }

    private void pauseRun(){if(!active||paused)return;paused=true;pausedAt=System.currentTimeMillis();lastLocation=null;updateNotification();}
    private void resumeRun(){if(!active||!paused)return;totalPaused+=System.currentTimeMillis()-pausedAt;paused=false;lastLocation=null;updateNotification();}

    private void finishRun(){
        if(!active){stopSelf();return;}updateElapsed();
        try{long endedAt=System.currentTimeMillis();JSONObject run=new JSONObject();run.put("date",new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.FRANCE).format(new Date()));run.put("startEpoch",startedAt);run.put("endEpoch",endedAt);run.put("distanceMeters",distanceMeters);run.put("durationSeconds",elapsedSeconds);run.put("steps",steps);run.put("stepsSource","phone");
            run.put("mode",indoor?"indoor":"outdoor");run.put("treadmillSpeed",treadmillSpeedKmh);if(ftmsConnected){run.put("machineSource","Matrix FTMS");run.put("machineCadence",machineCadence);run.put("machinePower",machinePower);run.put("machineCalories",machineCalories);run.put("machineHeartRate",machineHeartRate);}if(!indoor&&routePoints.size()>1){JSONArray route=new JSONArray();int stride=Math.max(1,routePoints.size()/500);for(int i=0;i<routePoints.size();i+=stride){double[] point=routePoints.get(i);JSONArray pair=new JSONArray();pair.put(point[0]);pair.put(point[1]);route.put(pair);}double[] last=routePoints.get(routePoints.size()-1);JSONArray endPair=new JSONArray();endPair.put(last[0]);endPair.put(last[1]);route.put(endPair);run.put("route",route);}SharedPreferences p=getSharedPreferences("gym_tracker",MODE_PRIVATE);JSONArray runs=new JSONArray(p.getString("running_history","[]"));if(isOutdoorCourseRecord(run,runs))run.put("isRecord",true);runs.put(run);p.edit().putString("running_history",runs.toString()).apply();
        }catch(Exception ignored){}
        active=false;paused=false;indoor=false;stopTracking();stopForeground(true);stopSelf();
    }

    private boolean isOutdoorCourseRecord(JSONObject current,JSONArray previous){
        if(!"outdoor".equals(current.optString("mode")))return false;double currentDistance=current.optDouble("distanceMeters",0);long currentTime=current.optLong("durationSeconds",0);JSONArray currentRoute=current.optJSONArray("route");if(currentDistance<500||currentTime<=0||currentRoute==null||currentRoute.length()<5)return false;double currentSpeed=currentDistance/currentTime;boolean matched=false;double bestSpeed=0;for(int i=0;i<previous.length();i++){JSONObject old=previous.optJSONObject(i);if(old==null||!"outdoor".equals(old.optString("mode")))continue;double oldDistance=old.optDouble("distanceMeters",0);long oldTime=old.optLong("durationSeconds",0);JSONArray oldRoute=old.optJSONArray("route");if(oldTime<=0||oldRoute==null||Math.abs(oldDistance-currentDistance)/currentDistance>.10||!sameRoute(currentRoute,oldRoute))continue;matched=true;bestSpeed=Math.max(bestSpeed,oldDistance/oldTime);}return matched&&currentSpeed>bestSpeed*1.005;
    }

    private boolean sameRoute(JSONArray a,JSONArray b){
        if(a.length()<5||b.length()<5)return false;double direct=0,reverse=0;int samples=9;for(int i=0;i<samples;i++){int ai=Math.round(i*(a.length()-1f)/(samples-1));int bi=Math.round(i*(b.length()-1f)/(samples-1));int bri=(b.length()-1)-bi;JSONArray pa=a.optJSONArray(ai),pb=b.optJSONArray(bi),pbr=b.optJSONArray(bri);if(pa==null||pb==null||pbr==null)return false;direct+=geoDistance(pa.optDouble(0),pa.optDouble(1),pb.optDouble(0),pb.optDouble(1));reverse+=geoDistance(pa.optDouble(0),pa.optDouble(1),pbr.optDouble(0),pbr.optDouble(1));}return Math.min(direct,reverse)/samples<=200;
    }

    private double geoDistance(double lat1,double lon1,double lat2,double lon2){double earth=6371000.0;double p1=Math.toRadians(lat1),p2=Math.toRadians(lat2),dp=Math.toRadians(lat2-lat1),dl=Math.toRadians(lon2-lon1);double h=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);return 2*earth*Math.atan2(Math.sqrt(h),Math.sqrt(1-h));}

    private void stopTracking(){handler.removeCallbacks(ticker);if(locationManager!=null)locationManager.removeUpdates(this);if(sensorManager!=null)sensorManager.unregisterListener(this);if(tts!=null){tts.stop();tts.shutdown();tts=null;}}
    @Override public void onDestroy(){if(active){active=false;paused=false;}stopTracking();super.onDestroy();}

    @Override public void onLocationChanged(Location location){
        if(!active||paused||!location.hasAccuracy()||location.getAccuracy()>40)return;
        if(lastLocation!=null){float delta=lastLocation.distanceTo(location);if(delta>=1&&delta<100){distanceMeters+=delta;checkKilometer();}}
        lastLocation=location;if(routePoints.size()<5000)routePoints.add(new double[]{location.getLatitude(),location.getLongitude()});updateElapsed();updateNotification();
    }
    @Override public void onProviderEnabled(String p){}
    @Override public void onProviderDisabled(String p){}
    @Override public void onStatusChanged(String p,int s,Bundle b){}

    private void updateElapsed(){if(active){long end=paused?pausedAt:System.currentTimeMillis();elapsedSeconds=Math.max(0,(end-startedAt-totalPaused)/1000);}}
    private void checkKilometer(){int km=(int)(distanceMeters/1000);if(km<=announcedKm)return;updateElapsed();long lastKmSeconds=Math.max(1,elapsedSeconds-lastKilometerElapsedSeconds);announcedKm=km;lastKilometerElapsedSeconds=elapsedSeconds;double lastKmSpeed=3600.0/lastKmSeconds;double averagePaceSeconds=elapsedSeconds/(double)km;double averageSpeed=elapsedSeconds>0?km/(elapsedSeconds/3600.0):0;
        String[] encouragements={"Bravo, continue comme ça !","Très bon rythme !","Bien joué, garde cette allure !","Super, kilomètre validé !"};String encouragement=encouragements[(km-1)%encouragements.length];String message=encouragement+" "+km+" kilomètre"+(km>1?"s":"")+" parcouru"+(km>1?"s":"")+". Vitesse sur le dernier kilomètre, "+String.format(Locale.FRANCE,"%.1f",lastKmSpeed)+" kilomètres heure. Vitesse moyenne, "+String.format(Locale.FRANCE,"%.1f",averageSpeed)+" kilomètres heure. Dernier kilomètre en "+formatSpokenDuration(lastKmSeconds)+". Allure moyenne, "+formatSpokenDuration(Math.round(averagePaceSeconds))+" par kilomètre.";
        if(tts!=null)tts.speak(message,TextToSpeech.QUEUE_FLUSH,null,"km"+km);vibrate();
    }

    private String formatSpokenDuration(long seconds){long minutes=seconds/60;long remaining=seconds%60;return minutes+" minute"+(minutes>1?"s":"")+" "+remaining+" seconde"+(remaining>1?"s":"");}

    private void vibrate(){try{Vibrator v;if(Build.VERSION.SDK_INT>=31)v=((VibratorManager)getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();else v=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);if(v!=null)v.vibrate(VibrationEffect.createOneShot(300,VibrationEffect.DEFAULT_AMPLITUDE));}catch(Exception ignored){}}
    @Override public void onSensorChanged(SensorEvent event){if(initialSteps<0)initialSteps=event.values[0];steps=Math.max(0,Math.round(event.values[0]-initialSteps));}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    @Override public void onInit(int status){if(status==TextToSpeech.SUCCESS&&tts!=null)tts.setLanguage(Locale.FRANCE);}

    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Suivi du running",NotificationManager.IMPORTANCE_LOW);c.setDescription("Suivi GPS pendant une sortie running");((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    private Notification notification(String text){Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);String label=indoor?"Running sur tapis":"Running outdoor";return new Notification.Builder(this,CHANNEL).setSmallIcon(fr.stephane.suivientrainement.R.drawable.ic_launcher).setContentTitle(paused?label+" en pause":label+" en cours").setContentText(text).setContentIntent(pi).setOngoing(true).setColor(Color.rgb(240,90,40)).build();}
    private void updateNotification(){updateElapsed();String text=String.format(Locale.FRANCE,"%.2f km • %s%s",distanceMeters/1000.0,formatDuration(elapsedSeconds),indoor?String.format(Locale.FRANCE," • %.1f km/h",treadmillSpeedKmh):"");((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,notification(text));}
    private String formatDuration(long sec){return String.format(Locale.FRANCE,"%02d:%02d:%02d",sec/3600,(sec%3600)/60,sec%60);}
    @Override public IBinder onBind(Intent intent){return null;}
}
