package fr.stephane.suivientrainement;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Velocity;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.OutcomeReceiver;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends android.app.Activity {
    private static final String APP_VERSION = "4.5";
    private static final int NAVY = Color.rgb(36, 52, 71);
    private static final int ORANGE = Color.rgb(240, 90, 40);
    private static final int SURFACE = Color.WHITE;
    private static final int BACKGROUND = Color.rgb(238,242,246);
    private static final int GREEN = Color.rgb(22,133,106);
    private static final int BLUE = Color.rgb(53,117,185);
    private static final int DONE = Color.rgb(221, 243, 228);
    private static final String PREFS = "gym_tracker";
    private static final String HISTORY = "history";
    private static final String BODY_WEIGHT = "body_weight";
    private static final String USER_NAME = "user_name";
    private static final String DEFAULT_REST = "default_rest";
    private static final String RUN_HISTORY = "running_history";
    private static final String TEMPLATES = "workout_templates";
    private static final int RUN_PERMISSION_REQUEST = 180;
    private static final int HEALTH_PERMISSION_REQUEST = 181;
    private static final int MATRIX_PERMISSION_REQUEST = 182;

    private final List<ExerciseEntry> current = new ArrayList<>();
    private LinearLayout root;
    private LinearLayout exerciseContainer;
    private TextView timerText;
    private TextView progressText;
    private CountDownTimer timer;
    private final Handler runningHandler=new Handler(Looper.getMainLooper());
    private TextView runningDistance,runningTime,runningPace,runningSpeed,runningSteps,runningMachine;
    private boolean runningScreen=false;
    private boolean pendingIndoor=false;
    private int pendingWatchRunIndex=-1;
    private int pendingWatchStrengthIndex=-1;
    private int activeExerciseIndex=0;
    private int editingHistoryIndex=-1;
    private String currentSessionName = "";
    private MatrixFtmsManager matrixFtms;
    // Musculation session timing/energy tracking (v4.2).
    private long sessionOriginalStartMs=0L;
    private long sessionActiveSegmentStartMs=0L;
    private long sessionAccumulatedSeconds=0L;

    private final Machine[] catalog = new Machine[] {
        new Machine("Chest Press", "Pectoraux", 20, 10, 3, 75),
        new Machine("Incline Chest Press", "Haut des pectoraux", 20, 10, 3, 75),
        new Machine("Pec Fly / Butterfly", "Pectoraux", 23, 20, 4, 60),
        new Machine("Vertical Bench Press", "Pectoraux", 20, 10, 3, 75),
        new Machine("Shoulder Press", "Épaules", 23, 25, 4, 75),
        new Machine("Lateral Raise", "Épaules", 18, 15, 3, 60),
        new Machine("Rear Delt / Reverse Fly", "Arrière des épaules", 18, 15, 3, 60),
        new Machine("Arm Curl", "Biceps", 23, 10, 3, 60),
        new Machine("Biceps Curl", "Biceps", 15, 10, 3, 60),
        new Machine("Triceps Extension", "Triceps", 32, 20, 3, 60),
        new Machine("Seated Triceps Press", "Triceps", 20, 12, 3, 60),
        new Machine("Lat Pulldown", "Dos", 32, 10, 3, 90),
        new Machine("Seated Row", "Dos", 32, 10, 3, 90),
        new Machine("Low Row", "Dos", 25, 10, 3, 90),
        new Machine("Back Extension", "Lombaires", 20, 12, 3, 60),
        new Machine("Abdominal Crunch", "Abdominaux", 30, 25, 4, 60),
        new Machine("Abdominal", "Abdominaux", 32, 25, 4, 60),
        new Machine("Torso Rotation", "Obliques", 23, 15, 3, 60),
        new Machine("Vertical Knee Raise", "Abdominaux", 0, 10, 3, 60),
        new Machine("Gainage", "Sangle abdominale", 0, 40, 3, 45),
        new Machine("Glute", "Fessiers", 20, 12, 3, 60),
        new Machine("Hip Abductor", "Fessiers / abducteurs", 20, 15, 3, 60),
        new Machine("Leg Press", "Quadriceps / fessiers", 40, 12, 3, 90),
        new Machine("Leg Extension", "Quadriceps", 20, 12, 3, 60),
        new Machine("Seated Leg Curl", "Ischio-jambiers", 20, 12, 3, 60),
        new Machine("Prone Leg Curl", "Ischio-jambiers", 15, 12, 3, 60),
        new Machine("Hip Adductor", "Adducteurs", 20, 15, 3, 60),
        new Machine("Calf Extension", "Mollets", 30, 15, 3, 60),
        new Machine("Rameur", "Échauffement / cardio", 0, 7, 1, 0),
        new Machine("Tapis de course", "Running", 0, 40, 1, 0)
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
            getWindow().setNavigationBarContrastEnforced(true);
        }
        matrixFtms=MatrixFtmsManager.get(this);matrixFtms.setListener(new MatrixFtmsManager.DataListener(){public void onStatus(String status){if(runningMachine!=null)runningMachine.setText(matrixLiveText());}public void onData(){if(runningScreen)updateRunningStats();}});
        showSplash();
    }

    @Override public void onBackPressed() {
        if(runningScreen){runningScreen=false;runningHandler.removeCallbacksAndMessages(null);showHome();return;}
        if (!currentSessionName.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Quitter la séance ?")
                .setMessage("Les saisies non enregistrées seront perdues.")
                .setNegativeButton("Continuer", null)
                .setPositiveButton("Quitter", (d,w) -> { stopTimer(); currentSessionName=""; showHome(); }).show();
        } else super.onBackPressed();
    }

    private void baseScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);
    }

    private void showSplash() {
        currentSessionName = ""; baseScreen(); root.setGravity(Gravity.CENTER); root.setBackgroundColor(NAVY);
        MachineIconView logo=new MachineIconView(this,"Dumbbell");
        logo.setBackgroundColor(Color.WHITE); root.addView(logo,new LinearLayout.LayoutParams(dp(112),dp(112)));
        TextView app=new TextView(this); app.setText("SUIVI ENTRAÎNEMENT"); app.setTextColor(Color.WHITE); app.setTextSize(24); app.setTypeface(Typeface.DEFAULT,Typeface.BOLD); app.setGravity(Gravity.CENTER); app.setPadding(dp(12),dp(24),dp(12),dp(4)); root.addView(app);
        TextView version=new TextView(this); version.setText("Version "+APP_VERSION); version.setTextColor(Color.rgb(220,225,230)); version.setTextSize(15); version.setGravity(Gravity.CENTER); root.addView(version);
        new Handler(Looper.getMainLooper()).postDelayed(this::openInitialScreen, 1300);
    }

    private void openInitialScreen(){ensureDefaultTemplates();SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);if(!p.contains(USER_NAME)&&!p.contains(BODY_WEIGHT))showSettings(true);else showHome();}

    private TextView title(String text, int sp) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextSize(sp); v.setTextColor(NAVY); v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setPadding(dp(20), dp(14), dp(20), dp(8));
        return v;
    }

    private Button action(String text) {
        Button b = new Button(this); b.setText(text); b.setTextSize(16); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); b.setBackground(rounded(NAVY,14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        p.setMargins(dp(20), dp(7), dp(20), dp(7)); b.setLayoutParams(p); return b;
    }

    private GradientDrawable rounded(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable outlined(int fill,int stroke,int radius){GradientDrawable g=rounded(fill,radius);g.setStroke(dp(1),stroke);return g;}

    private void showHome() {
        runningScreen=false;runningHandler.removeCallbacksAndMessages(null);currentSessionName = "";editingHistoryIndex=-1; current.clear(); stopTimer(); baseScreen();
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(20),dp(16),dp(20),dp(18));header.setBackgroundColor(NAVY);LinearLayout brandBox=new LinearLayout(this);brandBox.setOrientation(LinearLayout.VERTICAL);TextView brand=new TextView(this);brand.setText("MOVE");brand.setTextColor(ORANGE);brand.setTextSize(12);brand.setLetterSpacing(.18f);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brandBox.addView(brand);TextView brand2=new TextView(this);brand2.setText("Suivi Entraînement");brand2.setTextColor(Color.WHITE);brand2.setTextSize(20);brand2.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brandBox.addView(brand2);header.addView(brandBox,new LinearLayout.LayoutParams(0,dp(52),1));TextView avatar=new TextView(this);avatar.setText(initials(getUserName()));avatar.setGravity(Gravity.CENTER);avatar.setTextColor(Color.WHITE);avatar.setTypeface(Typeface.DEFAULT,Typeface.BOLD);avatar.setBackground(outlined(NAVY,Color.rgb(105,125,145),24));avatar.setOnClickListener(v->showSettings(false));header.addView(avatar,new LinearLayout.LayoutParams(dp(46),dp(46)));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(16),dp(16),dp(20));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));TextView hello=title("Bonjour "+getUserName(),26);hello.setPadding(0,0,0,0);content.addView(hello);TextView sub=new TextView(this);sub.setText("Prêt pour ta prochaine séance ?");sub.setTextColor(Color.GRAY);sub.setTextSize(14);sub.setPadding(0,dp(3),0,dp(14));content.addView(sub);
        int muscleCount=0,runCount=0;try{SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);muscleCount=new JSONArray(p.getString(HISTORY,"[]")).length();runCount=new JSONArray(p.getString(RUN_HISTORY,"[]")).length();}catch(Exception ignored){}LinearLayout metrics=new LinearLayout(this);metrics.setPadding(0,0,0,dp(12));addHomeMetric(metrics,"Poids",trim(getBodyWeight())+" kg");addHomeMetric(metrics,"Musculation",String.valueOf(muscleCount));addHomeMetric(metrics,"Running",String.valueOf(runCount));content.addView(metrics);
        TextView start=title("Choisir une activité",18);start.setPadding(0,dp(8),0,dp(8));content.addView(start);content.addView(homeSessionButton("Musculation","Choisir une séance enregistrée ou libre",ORANGE,v->showTemplateChooser()));LinearLayout tiles2=new LinearLayout(this);tiles2.setPadding(0,dp(10),0,0);tiles2.addView(homeTile(RunningService.active?"Reprendre":"Running outdoor",RunningService.active?"Séance en cours":"GPS et parcours","⌁",GREEN,v->{if(RunningService.active)showRunning();else prepareRunning(false);}),tileParams(true));tiles2.addView(homeTile("Running tapis","Distance et pas","▶",BLUE,v->prepareRunning(true)),tileParams(false));content.addView(tiles2);
        addBottomNav(0);
    }

    private LinearLayout.LayoutParams tileParams(boolean left){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(132),1);if(left)p.setMargins(0,0,dp(5),0);else p.setMargins(dp(5),0,0,0);return p;}
    private View homeTile(String name,String detail,String symbol,int color,View.OnClickListener listener){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(13),dp(14),dp(12));card.setBackground(rounded(SURFACE,18));card.setElevation(dp(2));TextView icon=new TextView(this);icon.setText(symbol);icon.setTextColor(color);icon.setTextSize(18);icon.setTypeface(Typeface.DEFAULT,Typeface.BOLD);icon.setGravity(Gravity.CENTER);icon.setBackground(rounded(Color.argb(24,Color.red(color),Color.green(color),Color.blue(color)),12));card.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));TextView n=new TextView(this);n.setText(name);n.setTextColor(NAVY);n.setTextSize(15);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);n.setPadding(0,dp(9),0,0);card.addView(n);TextView d=new TextView(this);d.setText(detail);d.setTextColor(Color.GRAY);d.setTextSize(11);card.addView(d);card.setOnClickListener(listener);return card;}

    private void addHomeMetric(LinearLayout row,String label,String value){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(dp(5),dp(10),dp(5),dp(10));box.setBackground(rounded(SURFACE,15));box.setElevation(dp(2));TextView l=new TextView(this);l.setText(label);l.setTextSize(11);l.setTextColor(Color.GRAY);l.setGravity(Gravity.CENTER);box.addView(l);TextView v=new TextView(this);v.setText(value);v.setTextSize(17);v.setTextColor(NAVY);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER);box.addView(v);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(72),1);p.setMargins(dp(4),0,dp(4),0);row.addView(box,p);}
    private View homeSessionButton(String name,String detail,int color,View.OnClickListener listener){LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(14),dp(9),dp(10),dp(9));card.setBackground(rounded(SURFACE,17));card.setElevation(dp(2));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(74));cp.setMargins(0,dp(5),0,dp(5));card.setLayoutParams(cp);TextView icon=new TextView(this);icon.setText(name.startsWith("Running")||name.startsWith("Reprendre")?"▶":"●");icon.setTextColor(color);icon.setTextSize(19);icon.setGravity(Gravity.CENTER);icon.setBackground(rounded(Color.argb(25,Color.red(color),Color.green(color),Color.blue(color)),13));card.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(dp(10),0,0,0);TextView n=new TextView(this);n.setText(name);n.setTextColor(NAVY);n.setTextSize(16);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);copy.addView(n);TextView d=new TextView(this);d.setText(detail);d.setTextColor(Color.GRAY);d.setTextSize(12);copy.addView(d);card.addView(copy,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));TextView arrow=new TextView(this);arrow.setText("›");arrow.setTextSize(28);arrow.setTextColor(Color.GRAY);card.addView(arrow,new LinearLayout.LayoutParams(dp(35),dp(44)));card.setOnClickListener(listener);return card;}
    private void addBottomNav(int selected){LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(6),dp(4),dp(6),dp(6));nav.setBackgroundColor(SURFACE);String[] labels={"⌂\nAccueil","↶\nHistorique","⌁\nProgression","⚙\nParamètres"};for(int i=0;i<labels.length;i++){final int index=i;TextView b=new TextView(this);b.setText(labels[i]);b.setGravity(Gravity.CENTER);b.setTextSize(11);b.setTextColor(i==selected?ORANGE:Color.GRAY);b.setTypeface(Typeface.DEFAULT,i==selected?Typeface.BOLD:Typeface.NORMAL);b.setBackgroundColor(SURFACE);b.setOnClickListener(v->{if(index==0)showHome();else if(index==1)showHistory();else if(index==2)showProgress();else showSettings(false);});nav.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));}root.addView(nav);}

    private void prepareRunning(boolean indoorMode){
        if(RunningService.active){showRunning();return;}
        pendingIndoor=indoorMode;List<String> missing=new ArrayList<>();if(!indoorMode&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if(Build.VERSION.SDK_INT>=29&&checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED)missing.add(Manifest.permission.ACTIVITY_RECOGNITION);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)missing.add(Manifest.permission.POST_NOTIFICATIONS);
        if(!missing.isEmpty()){requestPermissions(missing.toArray(new String[0]),RUN_PERMISSION_REQUEST);return;}startRunningService(indoorMode);
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==RUN_PERMISSION_REQUEST){if(pendingIndoor||checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)startRunningService(pendingIndoor);else Toast.makeText(this,"La localisation précise est nécessaire pour le running GPS",Toast.LENGTH_LONG).show();}
        if(requestCode==HEALTH_PERMISSION_REQUEST){
            if(pendingWatchStrengthIndex>=0){int index=pendingWatchStrengthIndex;pendingWatchStrengthIndex=-1;if(Build.VERSION.SDK_INT>=34&&checkSelfPermission("android.permission.health.READ_ACTIVE_CALORIES_BURNED")==PackageManager.PERMISSION_GRANTED)fetchStrengthWatchMetrics(index);else Toast.makeText(this,"Autorisation Health Connect refusée",Toast.LENGTH_LONG).show();}
            else if(pendingWatchRunIndex>=0){int index=pendingWatchRunIndex;pendingWatchRunIndex=-1;if(Build.VERSION.SDK_INT>=34&&checkSelfPermission("android.permission.health.READ_STEPS")==PackageManager.PERMISSION_GRANTED)fetchWatchMetrics(index);else Toast.makeText(this,"Autorisation Health Connect refusée",Toast.LENGTH_LONG).show();}
        }
        if(requestCode==MATRIX_PERMISSION_REQUEST)scanMatrixDevicesV45();
    }

    private void startRunningService(boolean indoorMode){if(indoorMode)matrixFtms.beginSession();Intent i=new Intent(this,RunningService.class);i.putExtra("action",indoorMode?"START_INDOOR":"START");if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);showRunning();}
    private void runningCommand(String command){Intent i=new Intent(this,RunningService.class);i.putExtra("action",command);startService(i);}
    private void adjustTreadmillSpeed(double delta){double speed=Math.max(1,Math.min(25,RunningService.treadmillSpeedKmh+delta));RunningService.treadmillSpeedKmh=speed;Intent i=new Intent(this,RunningService.class);i.putExtra("action","SET_SPEED");i.putExtra("speed",speed);startService(i);}

    private void showRunning(){
        runningHandler.removeCallbacksAndMessages(null);runningScreen=true;baseScreen();LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(NAVY);Button back=new Button(this);back.setText("‹");back.setTextColor(Color.WHITE);back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(NAVY));back.setTextSize(28);back.setOnClickListener(v->onBackPressed());bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));TextView h=title(RunningService.indoor?"Running sur tapis":"Running outdoor",24);h.setTextColor(Color.WHITE);h.setPadding(dp(8),0,0,0);bar.addView(h);root.addView(bar);
        TextView gps=new TextView(this);gps.setText(RunningService.indoor?(matrixFtms.isConnected()?"Matrix FTMS connecté • données machine en direct":"Mode tapis manuel • connexion Matrix disponible dans Paramètres"):"Suivi GPS actif • annonce vocale chaque kilomètre");gps.setTextColor(Color.WHITE);gps.setBackgroundColor(RunningService.indoor?Color.rgb(55,110,180):Color.rgb(28,110,63));gps.setGravity(Gravity.CENTER);gps.setPadding(dp(8),dp(12),dp(8),dp(12));root.addView(gps);if(RunningService.indoor&&matrixFtms.hasSavedDevice()&&!matrixFtms.isConnected())matrixFtms.connectSaved();
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.VERTICAL);stats.setPadding(dp(16),dp(16),dp(16),dp(8));
        runningDistance=new TextView(this);runningDistance.setText("DISTANCE\n0,00\nkilomètres");runningDistance.setTextColor(Color.WHITE);runningDistance.setTextSize(30);runningDistance.setTypeface(Typeface.DEFAULT,Typeface.BOLD);runningDistance.setGravity(Gravity.CENTER);runningDistance.setBackground(rounded(NAVY,20));runningDistance.setElevation(dp(3));stats.addView(runningDistance,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(170)));
        LinearLayout row1=new LinearLayout(this);row1.setPadding(0,dp(10),0,0);runningTime=runningStat(row1,"DURÉE","00:00:00");runningPace=runningStat(row1,"ALLURE MOYENNE","-- min/km");stats.addView(row1,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(98)));
        LinearLayout row2=new LinearLayout(this);row2.setPadding(0,dp(8),0,0);runningSpeed=runningStat(row2,"VITESSE MOYENNE","0,0 km/h");runningSteps=runningStat(row2,"PAS","0");stats.addView(row2,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(96)));runningMachine=new TextView(this);runningMachine.setText(matrixLiveText());runningMachine.setTextColor(NAVY);runningMachine.setTextSize(12);runningMachine.setGravity(Gravity.CENTER);runningMachine.setBackground(rounded(SURFACE,12));LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));mlp.setMargins(dp(4),dp(8),dp(4),0);stats.addView(runningMachine,mlp);root.addView(stats,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        if(RunningService.indoor){LinearLayout speedRow=new LinearLayout(this);speedRow.setGravity(Gravity.CENTER);speedRow.setPadding(dp(20),0,dp(20),dp(4));Button minus=new Button(this);minus.setText("− 0,5");minus.setTextColor(NAVY);minus.setBackground(rounded(SURFACE,13));minus.setOnClickListener(v->adjustTreadmillSpeed(-0.5));speedRow.addView(minus,new LinearLayout.LayoutParams(0,dp(52),1));TextView label=new TextView(this);label.setText("VITESSE\nTAPIS");label.setGravity(Gravity.CENTER);label.setTextColor(NAVY);label.setTextSize(12);label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);speedRow.addView(label,new LinearLayout.LayoutParams(0,dp(52),1));Button plus=new Button(this);plus.setText("+ 0,5");plus.setTextColor(Color.WHITE);plus.setBackground(rounded(BLUE,13));plus.setOnClickListener(v->adjustTreadmillSpeed(0.5));speedRow.addView(plus,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(speedRow);}
        LinearLayout controls=new LinearLayout(this);controls.setPadding(dp(16),dp(8),dp(16),dp(14));Button pause=new Button(this);pause.setText(RunningService.paused?"▶  Reprendre":"Ⅱ  Pause");pause.setAllCaps(false);pause.setTextColor(NAVY);pause.setBackground(rounded(SURFACE,14));pause.setOnClickListener(v->{runningCommand(RunningService.paused?"RESUME":"PAUSE");runningHandler.postDelayed(this::showRunning,150);});LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,dp(58),1);pp.setMargins(0,0,dp(6),0);controls.addView(pause,pp);Button stop=new Button(this);stop.setText("■  Terminer");stop.setAllCaps(false);stop.setTextColor(Color.WHITE);stop.setBackground(rounded(ORANGE,14));stop.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Terminer le running ?").setMessage("La sortie sera enregistrée dans l’historique.").setNegativeButton("Continuer",null).setPositiveButton("Enregistrer",(d,w)->{runningCommand("STOP");runningScreen=false;runningHandler.postDelayed(this::showHome,250);}).show());LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(58),1);sp.setMargins(dp(6),0,0,0);controls.addView(stop,sp);root.addView(controls);updateRunningStats();
    }

    private TextView runningStat(LinearLayout parent,String label,String value){TextView v=new TextView(this);v.setText(label+"\n"+value);v.setTextColor(NAVY);v.setTextSize(18);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER);v.setPadding(dp(6),dp(8),dp(6),dp(8));v.setBackground(rounded(SURFACE,16));v.setElevation(dp(2));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1);p.setMargins(dp(4),0,dp(4),0);parent.addView(v,p);return v;}
    private void updateRunningStats(){if(!runningScreen)return;double km=RunningService.distanceMeters/1000.0;long sec=RunningService.elapsedSeconds;double pace=km>0?(sec/60.0)/km:0;double speed=RunningService.indoor?RunningService.treadmillSpeedKmh:(sec>0?km/(sec/3600.0):0);runningDistance.setText("DISTANCE\n"+String.format(Locale.FRANCE,"%.2f",km)+"\nkilomètres");runningTime.setText("DURÉE\n"+String.format(Locale.FRANCE,"%02d:%02d:%02d",sec/3600,(sec%3600)/60,sec%60));runningPace.setText("ALLURE MOYENNE\n"+(pace>0?String.format(Locale.FRANCE,"%d:%02d min/km",(int)pace,(int)Math.round((pace-(int)pace)*60)):"-- min/km"));runningSpeed.setText((RunningService.indoor?"VITESSE TAPIS":"VITESSE MOYENNE")+"\n"+String.format(Locale.FRANCE,"%.1f km/h",speed));runningSteps.setText("PAS\n"+RunningService.steps);if(runningMachine!=null)runningMachine.setText(matrixLiveText());runningHandler.removeCallbacksAndMessages(null);runningHandler.postDelayed(this::updateRunningStats,1000);}
    private String matrixLiveText(){if(matrixFtms==null)return "Matrix FTMS : indisponible";if(!matrixFtms.isConnected())return "Matrix FTMS • "+matrixFtms.status;return String.format(Locale.FRANCE,"Matrix FTMS • %.0f tr/min • %.0f W • %.0f kcal • %d bpm",matrixFtms.cadence,matrixFtms.power,matrixFtms.calories,matrixFtms.heartRate);}

    private void startTemplate(String name, boolean a) {
        current.clear();activeExerciseIndex=0;editingHistoryIndex=-1;beginNewStrengthSession();
        String[] names = a
            ? new String[]{"Chest Press","Pec Fly / Butterfly","Shoulder Press","Lateral Raise","Triceps Extension","Abdominal Crunch","Abdominal","Arm Curl"}
            : new String[]{"Incline Chest Press","Lat Pulldown","Seated Row","Rear Delt / Reverse Fly","Arm Curl","Torso Rotation","Vertical Knee Raise"};
        for (String n : names) { ExerciseEntry e=new ExerciseEntry(find(n));applyLastPerformance(e);current.add(e); }
        showSession(name);
    }

    private void startFree() {
        current.clear();activeExerciseIndex=0;editingHistoryIndex=-1;beginNewStrengthSession(); showSession("Séance libre");
    }

    private void ensureDefaultTemplates(){SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);if(p.contains(TEMPLATES))return;try{JSONArray templates=new JSONArray();templates.put(templateJson("Séance A",new String[]{"Chest Press","Pec Fly / Butterfly","Shoulder Press","Lateral Raise","Triceps Extension","Abdominal Crunch","Abdominal","Arm Curl"}));templates.put(templateJson("Séance B",new String[]{"Incline Chest Press","Lat Pulldown","Seated Row","Rear Delt / Reverse Fly","Arm Curl","Torso Rotation","Vertical Knee Raise"}));p.edit().putString(TEMPLATES,templates.toString()).apply();}catch(JSONException ignored){}}
    private JSONObject templateJson(String name,String[] names)throws JSONException{JSONObject template=new JSONObject();template.put("name",name);JSONArray exercises=new JSONArray();for(String machineName:names){Machine m=find(machineName);JSONObject x=new JSONObject();x.put("name",m.name);x.put("group",m.group);x.put("weight",m.weight);x.put("reps",m.reps);x.put("targetSets",m.sets);x.put("note","");exercises.put(x);}template.put("exercises",exercises);return template;}
    private void showTemplateChooser(){ensureDefaultTemplates();ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(12),dp(6),dp(12),dp(12));scroll.addView(list);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Choisir une séance").setView(scroll).setNegativeButton("Annuler",null).create();try{JSONArray templates=new JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString(TEMPLATES,"[]"));for(int i=0;i<templates.length();i++){final int index=i;JSONObject template=templates.getJSONObject(i);String name=template.optString("name","Séance");int count=template.optJSONArray("exercises")==null?0:template.optJSONArray("exercises").length();Button row=new Button(this);row.setText(name+"\n"+count+" machines");row.setAllCaps(false);row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setOnClickListener(v->{dialog.dismiss();startSavedTemplate(index);});list.addView(row,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(66)));}}catch(JSONException ignored){}Button free=new Button(this);free.setText("+  Nouvelle séance libre");free.setAllCaps(false);free.setTextColor(Color.WHITE);free.setBackground(rounded(BLUE,12));free.setOnClickListener(v->{dialog.dismiss();startFree();});LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));fp.setMargins(0,dp(10),0,0);list.addView(free,fp);dialog.show();}
    private void startSavedTemplate(int index){try{JSONArray templates=new JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString(TEMPLATES,"[]"));if(index<0||index>=templates.length())return;JSONObject template=templates.getJSONObject(index);current.clear();JSONArray xs=template.optJSONArray("exercises");if(xs!=null)for(int i=0;i<xs.length();i++){JSONObject x=xs.getJSONObject(i);Machine known=findExact(x.optString("name"));ExerciseEntry e=new ExerciseEntry(known!=null?known:new Machine(x.optString("name","Machine"),x.optString("group","Autre"),0,10,3,getDefaultRest()));e.group=x.optString("group",e.group);e.weight=x.optDouble("weight",e.weight);e.reps=x.optInt("reps",e.reps);e.targetSets=Math.max(1,x.optInt("targetSets",e.targetSets));e.note=x.optString("note","");applyLastPerformance(e);current.add(e);}activeExerciseIndex=0;editingHistoryIndex=-1;beginNewStrengthSession();showSession(template.optString("name","Séance"));}catch(JSONException ex){Toast.makeText(this,"Modèle de séance illisible",Toast.LENGTH_LONG).show();}}
    private void saveCurrentAsTemplateDialog(){syncFields();if(current.isEmpty()){Toast.makeText(this,"Ajoute au moins une machine",Toast.LENGTH_SHORT).show();return;}EditText input=new EditText(this);input.setHint("Nom de la séance");input.setSingleLine(true);input.setPadding(dp(24),0,dp(24),0);if(!"Séance libre".equals(currentSessionName))input.setText(currentSessionName);new AlertDialog.Builder(this).setTitle("Enregistrer comme séance par défaut").setView(input).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{String name=input.getText().toString().trim();if(name.isEmpty()){Toast.makeText(this,"Donne un nom à la séance",Toast.LENGTH_LONG).show();return;}saveCurrentAsTemplate(name);}).show();}
    private void saveCurrentAsTemplate(String name){try{SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray templates=new JSONArray(p.getString(TEMPLATES,"[]"));JSONObject template=new JSONObject();template.put("name",name);JSONArray xs=new JSONArray();for(ExerciseEntry e:current){JSONObject x=new JSONObject();x.put("name",e.name);x.put("group",e.group);x.put("weight",e.weight);x.put("reps",e.reps);x.put("targetSets",e.targetSets);x.put("note",e.note);xs.put(x);}template.put("exercises",xs);int replace=-1;for(int i=0;i<templates.length();i++)if(name.equalsIgnoreCase(templates.getJSONObject(i).optString("name"))){replace=i;break;}if(replace>=0)templates.put(replace,template);else templates.put(template);p.edit().putString(TEMPLATES,templates.toString()).apply();currentSessionName=name;Toast.makeText(this,"Séance « "+name+" » enregistrée",Toast.LENGTH_LONG).show();}catch(JSONException ex){Toast.makeText(this,"Enregistrement du modèle impossible",Toast.LENGTH_LONG).show();}}

    private void beginNewStrengthSession(){
        long now=System.currentTimeMillis();
        sessionOriginalStartMs=now;
        sessionActiveSegmentStartMs=now;
        sessionAccumulatedSeconds=0L;
    }

    private long currentStrengthDurationSeconds(){
        long active=sessionActiveSegmentStartMs>0?Math.max(0L,(System.currentTimeMillis()-sessionActiveSegmentStartMs)/1000L):0L;
        return Math.max(0L,sessionAccumulatedSeconds+active);
    }

    private double estimateStrengthCalories(long durationSeconds,int exerciseCount,int totalSets){
        if(durationSeconds<=0)return 0;
        double minutes=durationSeconds/60.0;
        // Sans fréquence cardiaque, on utilise une estimation MET prudente.
        // Une séance dense (beaucoup de séries par heure) monte légèrement le MET.
        double setsPerHour=minutes>0?totalSets/(minutes/60.0):0;
        double met=setsPerHour>=28?5.5:setsPerHour>=20?5.0:4.5;
        if(exerciseCount<=2&&totalSets<=6)met=Math.min(met,4.0);
        return met*3.5*getBodyWeight()/200.0*minutes;
    }

    private String formatDuration(long seconds){
        long h=seconds/3600,m=(seconds%3600)/60,s=seconds%60;
        return h>0?String.format(Locale.FRANCE,"%d:%02d:%02d",h,m,s):String.format(Locale.FRANCE,"%d:%02d",m,s);
    }

    private long parseSessionDateMillis(String date){
        try{return new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.FRANCE).parse(date).getTime();}catch(Exception ignored){return System.currentTimeMillis();}
    }

    private void showSession(String name) {
        currentSessionName=name; baseScreen();
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(8),dp(8),dp(8),dp(4));bar.setBackgroundColor(NAVY);
        Button back=new Button(this); back.setText("‹");back.setTextColor(Color.WHITE);back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(NAVY)); back.setTextSize(28); back.setOnClickListener(v -> onBackPressed()); bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(52)));
        TextView t=title(name,22);t.setTextColor(Color.WHITE); t.setPadding(dp(8),0,0,0); bar.addView(t,new LinearLayout.LayoutParams(0,dp(52),1)); root.addView(bar);
        LinearLayout status=new LinearLayout(this);status.setPadding(dp(12),dp(10),dp(12),dp(6));status.setGravity(Gravity.CENTER_VERTICAL);progressText=new TextView(this); progressText.setTextSize(14); progressText.setTypeface(Typeface.DEFAULT,Typeface.BOLD); progressText.setTextColor(NAVY);progressText.setPadding(dp(12),0,dp(8),0);status.addView(progressText,new LinearLayout.LayoutParams(0,dp(46),1));timerText=new TextView(this); timerText.setText("Repos : prêt"); timerText.setTextSize(14); timerText.setTypeface(Typeface.DEFAULT,Typeface.BOLD); timerText.setTextColor(Color.WHITE); timerText.setGravity(Gravity.CENTER); timerText.setBackground(rounded(ORANGE,14));status.addView(timerText,new LinearLayout.LayoutParams(dp(132),dp(46)));root.addView(status);
        SwipeExerciseScrollView scroll=new SwipeExerciseScrollView(this); exerciseContainer=new LinearLayout(this); exerciseContainer.setOrientation(LinearLayout.VERTICAL); exerciseContainer.setPadding(dp(4),dp(4),dp(4),dp(8)); scroll.addView(exerciseContainer); root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        renderExercises();
        LinearLayout actions=new LinearLayout(this); actions.setPadding(dp(8),dp(4),dp(8),dp(8));
        Button add=new Button(this); add.setText("+ Machine"); add.setTextSize(11);add.setAllCaps(false);add.setTextColor(NAVY);add.setBackground(outlined(SURFACE,Color.rgb(210,217,224),14)); add.setOnClickListener(v -> chooseMachine());LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(54),1);ap.setMargins(0,0,dp(3),0);actions.addView(add,ap);
        Button model=new Button(this);model.setText("Modèle");model.setTextSize(11);model.setAllCaps(false);model.setTextColor(NAVY);model.setBackground(outlined(SURFACE,Color.rgb(210,217,224),14));model.setOnClickListener(v->saveCurrentAsTemplateDialog());LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,dp(54),1);mp.setMargins(dp(3),0,dp(3),0);actions.addView(model,mp);
        Button save=new Button(this); save.setText("✓ Séance");save.setTextSize(11); save.setAllCaps(false); save.setTextColor(Color.WHITE); save.setBackground(rounded(ORANGE,14)); save.setOnClickListener(v -> saveSession());LinearLayout.LayoutParams svp=new LinearLayout.LayoutParams(0,dp(54),1);svp.setMargins(dp(3),0,0,0);actions.addView(save,svp); root.addView(actions);
    }

    private void renderExercises() {
        exerciseContainer.removeAllViews();
        if (current.isEmpty()) {
            TextView empty=title("Ajoute une première machine",18); empty.setGravity(Gravity.CENTER); empty.setTextColor(Color.GRAY); exerciseContainer.addView(empty);
        } else {
            if(activeExerciseIndex<0)activeExerciseIndex=0;if(activeExerciseIndex>=current.size())activeExerciseIndex=current.size()-1;
            ExerciseEntry active=current.get(activeExerciseIndex);String activeZone=machineZone(active.name,active.group);
            TextView position=new TextView(this);position.setText(activeZone.toUpperCase(Locale.FRANCE)+"  •  MACHINE "+(activeExerciseIndex+1)+" SUR "+current.size());position.setTextColor(Color.GRAY);position.setTextSize(11);position.setLetterSpacing(.08f);position.setTypeface(Typeface.DEFAULT,Typeface.BOLD);position.setPadding(dp(4),dp(3),0,dp(8));exerciseContainer.addView(position);
            addExerciseCard(current.get(activeExerciseIndex),activeExerciseIndex);
            LinearLayout dots=new LinearLayout(this);dots.setGravity(Gravity.CENTER);dots.setPadding(0,dp(4),0,0);for(int i=0;i<current.size();i++){final int target=i;TextView dot=new TextView(this);dot.setText(current.get(i).done?"✓":"•");dot.setGravity(Gravity.CENTER);dot.setTextSize(current.get(i).done?12:20);dot.setTextColor(i==activeExerciseIndex?ORANGE:(current.get(i).done?GREEN:Color.LTGRAY));dot.setOnClickListener(v->{syncFields();activeExerciseIndex=target;renderExercises();});dots.addView(dot,new LinearLayout.LayoutParams(dp(30),dp(28)));}exerciseContainer.addView(dots);
        }
        updateProgress();
    }

    private void addExerciseCard(ExerciseEntry e, int index) {
        LinearLayout pager=new LinearLayout(this);pager.setGravity(Gravity.CENTER_VERTICAL);
        TextView left=pageArrow("‹",index>0);left.setOnClickListener(v->{if(index>0)swipeExercise(-1);});pager.addView(left,new LinearLayout.LayoutParams(dp(32),dp(90)));
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(12),dp(14),dp(12)); card.setBackground(rounded(e.done ? DONE : SURFACE,17));card.setElevation(dp(2));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1); cp.setMargins(0,dp(3),0,dp(3));
        ImageView photo=machinePhoto(e.name);photo.setContentDescription("Photo de la machine "+e.name+". Double appui pour terminer une série et lancer le repos.");final long[] lastTap={0};photo.setOnClickListener(v->{long now=System.currentTimeMillis();if(now-lastTap[0]<=450){lastTap[0]=0;if(e.done){e.done=false;renderExercises();}else startSeriesRest(e);}else lastTap[0]=now;});LinearLayout.LayoutParams photoParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(120));photoParams.setMargins(0,0,0,dp(5));card.addView(photo,photoParams);
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        String zone=machineZone(e.name,e.group);String detail=zone.equalsIgnoreCase(e.group)?zone:zone+"  •  "+e.group;
        String records=recordLabel(e,editingHistoryIndex);TextView name=new TextView(this); name.setText((records.isEmpty()?"":records+" ")+(e.done ? "✓ " : (index+1)+". ")+e.name+"\n"+detail); name.setTextSize(17); name.setTypeface(Typeface.DEFAULT,Typeface.BOLD); name.setTextColor(e.done ? Color.rgb(28,110,63) : NAVY); head.addView(name,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView status=new TextView(this);status.setText(e.done?"TERMINÉE":"EN COURS");status.setTextColor(e.done?GREEN:ORANGE);status.setTextSize(10);status.setGravity(Gravity.CENTER);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);status.setBackground(rounded(e.done?Color.rgb(225,245,235):Color.rgb(255,235,226),10));status.setOnClickListener(v->{syncFields();e.done=!e.done;renderExercises();});head.addView(status,new LinearLayout.LayoutParams(dp(76),dp(34)));
        TextView remove=new TextView(this); remove.setText("×");remove.setGravity(Gravity.CENTER);remove.setTextColor(Color.GRAY); remove.setTextSize(24);remove.setBackground(rounded(BACKGROUND,12)); remove.setOnClickListener(v -> { syncFields(); current.remove(e); renderExercises(); });LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(40),dp(40));rp.setMargins(dp(6),0,0,0);head.addView(remove,rp); card.addView(head);
        LinearLayout fields=new LinearLayout(this); fields.setGravity(Gravity.CENTER); fields.setPadding(0,dp(8),0,0);
        e.weightView=numberField("kg",e.weight,true); e.repsView=numberField("rép.",e.reps,false); e.setsView=numberField("séries",e.sets,false);
        fields.setOrientation(LinearLayout.VERTICAL);fields.addView(stepper("CHARGE",e.weightView,2.5,"kg"),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(58)));fields.addView(stepper("RÉPÉTITIONS",e.repsView,1,"rép."),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(58)));fields.addView(stepper("SÉRIES EFFECTUÉES  •  OBJECTIF "+e.targetSets,e.setsView,1,"séries"),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(58))); card.addView(fields);
        LinearLayout bottom=new LinearLayout(this); bottom.setGravity(Gravity.CENTER_VERTICAL);
        e.noteView=new EditText(this); e.noteView.setHint("Note ou réglage du siège"); e.noteView.setText(e.note); e.noteView.setTextSize(12);e.noteView.setSingleLine(true);e.noteView.setBackground(rounded(BACKGROUND,11));e.noteView.setPadding(dp(10),0,dp(8),0);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(42),1);np.setMargins(0,dp(5),0,0);bottom.addView(e.noteView,np);
        card.addView(bottom);Button finishMachine=new Button(this);finishMachine.setText(e.done?"Rouvrir la machine":"Terminer la machine");finishMachine.setAllCaps(false);finishMachine.setTextSize(12);finishMachine.setTextColor(e.done?NAVY:Color.WHITE);finishMachine.setBackground(e.done?outlined(SURFACE,GREEN,11):rounded(GREEN,11));finishMachine.setOnClickListener(v->{syncFields();e.done=!e.done;renderExercises();});LinearLayout.LayoutParams fmp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(42));fmp.setMargins(0,dp(6),0,0);card.addView(finishMachine,fmp);TextView hint=new TextView(this);hint.setText(e.done?"Machine terminée":"Double appui sur la photo : série terminée + repos "+getDefaultRest()+" s");hint.setTextColor(e.done?GREEN:ORANGE);hint.setTextSize(11);hint.setTypeface(Typeface.DEFAULT,Typeface.BOLD);hint.setGravity(Gravity.CENTER);hint.setPadding(0,dp(4),0,0);card.addView(hint,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(25)));
        pager.addView(card,cp);TextView right=pageArrow("›",index<current.size()-1);right.setOnClickListener(v->{if(index<current.size()-1)swipeExercise(1);});pager.addView(right,new LinearLayout.LayoutParams(dp(32),dp(90)));exerciseContainer.addView(pager);
    }

    private TextView pageArrow(String symbol,boolean enabled){TextView arrow=new TextView(this);arrow.setText(symbol);arrow.setTextSize(36);arrow.setGravity(Gravity.CENTER);arrow.setTextColor(enabled?ORANGE:Color.LTGRAY);arrow.setTypeface(Typeface.DEFAULT,Typeface.BOLD);arrow.setEnabled(enabled);return arrow;}

    private View stepper(String label,EditText field,double increment,String suffix){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(6),dp(8),dp(6));row.setBackground(rounded(BACKGROUND,12));TextView lab=new TextView(this);lab.setText(label);lab.setTextColor(Color.DKGRAY);lab.setTextSize(11);lab.setTypeface(Typeface.DEFAULT,Typeface.BOLD);row.addView(lab,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));TextView minus=stepButton("−");minus.setOnClickListener(v->changeField(field,-increment));row.addView(minus,new LinearLayout.LayoutParams(dp(42),dp(42)));field.setBackgroundColor(Color.TRANSPARENT);field.setTextColor(NAVY);field.setTypeface(Typeface.DEFAULT,Typeface.BOLD);row.addView(field,new LinearLayout.LayoutParams(dp(58),dp(46)));TextView unit=new TextView(this);unit.setText(suffix);unit.setTextColor(Color.GRAY);unit.setTextSize(11);unit.setGravity(Gravity.CENTER_VERTICAL);row.addView(unit,new LinearLayout.LayoutParams(dp(40),dp(42)));TextView plus=stepButton("+");plus.setOnClickListener(v->changeField(field,increment));row.addView(plus,new LinearLayout.LayoutParams(dp(42),dp(42)));return row;}
    private TextView stepButton(String symbol){TextView b=new TextView(this);b.setText(symbol);b.setGravity(Gravity.CENTER);b.setTextSize(23);b.setTextColor(NAVY);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(SURFACE,11));return b;}
    private void changeField(EditText field,double delta){double value=Math.max(0,parseDouble(field.getText().toString(),0)+delta);field.setText(delta==(long)delta&&value==(long)value?String.valueOf((long)value):trim(value));}

    private LinearLayout labeled(String label, EditText field) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(3),dp(3),dp(3),0);box.setBackground(rounded(BACKGROUND,11));
        TextView l=new TextView(this); l.setText(label); l.setTextSize(11); l.setGravity(Gravity.CENTER); l.setTextColor(Color.DKGRAY); box.addView(l); box.addView(field,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50))); return box;
    }

    private EditText numberField(String suffix,double value,boolean decimal) {
        EditText e=new EditText(this); e.setGravity(Gravity.CENTER); e.setTextSize(17); e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | (decimal?InputType.TYPE_NUMBER_FLAG_DECIMAL:0));
        e.setText(decimal ? trim(value) : String.valueOf((int)value)); e.setSelectAllOnFocus(true); return e;
    }

    private void chooseMachine() {
        syncFields();ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(8),dp(4),dp(8),dp(8));scroll.addView(list);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Ajouter une machine").setView(scroll).setNegativeButton("Annuler",null).create();String previousZone="";for(int i=0;i<catalog.length;i++){final int which=i;String zone=machineZone(catalog[i].name,catalog[i].group);if(!zone.equals(previousZone)){TextView header=new TextView(this);header.setText(zone.toUpperCase(Locale.FRANCE));header.setTextColor(ORANGE);header.setTextSize(13);header.setLetterSpacing(.09f);header.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.setPadding(dp(8),previousZone.isEmpty()?dp(8):dp(20),dp(8),dp(5));list.addView(header);previousZone=zone;}LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(7),dp(8),dp(7));row.setBackground(rounded(SURFACE,14));row.setElevation(dp(1));LinearLayout.LayoutParams rowp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(88));rowp.setMargins(0,dp(3),0,dp(3));row.setLayoutParams(rowp);ImageView image=machinePhoto(catalog[i].name);row.addView(image,new LinearLayout.LayoutParams(dp(92),dp(72)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(dp(12),0,0,0);TextView n=new TextView(this);n.setText(catalog[i].name);n.setTextColor(NAVY);n.setTextSize(16);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);copy.addView(n);TextView g=new TextView(this);g.setText(catalog[i].group);g.setTextColor(Color.GRAY);g.setTextSize(12);copy.addView(g);row.addView(copy,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));row.setOnClickListener(v->{ExerciseEntry e=new ExerciseEntry(catalog[which]);applyLastPerformance(e);current.add(e);activeExerciseIndex=current.size()-1;dialog.dismiss();renderExercises();});list.addView(row);}Button custom=new Button(this);custom.setText("+ Créer une machine personnalisée");custom.setAllCaps(false);custom.setOnClickListener(v->{dialog.dismiss();customMachine();});list.addView(custom,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(56)));dialog.show();
    }

    private ImageView machinePhoto(String machine){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);image.setAdjustViewBounds(true);image.setBackground(rounded(Color.WHITE,13));image.setImageResource(machinePhotoRes(machine));return image;}
    private int machinePhotoRes(String machine){String n=machine.toLowerCase(Locale.ROOT);if(n.contains("incline chest"))return R.drawable.machine_incline_chest_press;if(n.contains("vertical bench"))return R.drawable.machine_vertical_press;if(n.contains("chest press"))return R.drawable.machine_chest_press;if(n.contains("pec fly")||n.contains("rear delt"))return R.drawable.machine_pec_fly;if(n.contains("shoulder"))return R.drawable.machine_shoulder_press;if(n.contains("lateral"))return R.drawable.machine_lateral_raise;if(n.contains("seated triceps"))return R.drawable.machine_triceps_press;if(n.contains("triceps"))return R.drawable.machine_triceps;if(n.contains("biceps curl"))return R.drawable.machine_biceps_curl;if(n.contains("arm curl"))return R.drawable.machine_arm_curl;if(n.contains("pulldown"))return R.drawable.machine_lat_pulldown;if(n.contains("low row"))return R.drawable.machine_low_row;if(n.contains("seated row"))return R.drawable.machine_seated_row;if(n.contains("back extension"))return R.drawable.machine_back_extension;if(n.contains("abdominal"))return R.drawable.machine_abdominal;if(n.contains("rotation"))return R.drawable.machine_torso_rotation;if(n.contains("knee raise")||n.contains("gainage"))return R.drawable.machine_knee_raise;if(n.contains("hip abductor"))return R.drawable.machine_hip_abductor;if(n.contains("hip adductor"))return R.drawable.machine_hip_adductor;if(n.equals("glute")||n.contains("glute machine"))return R.drawable.machine_glute;if(n.contains("leg press"))return R.drawable.machine_leg_press;if(n.contains("leg extension"))return R.drawable.machine_leg_extension;if(n.contains("seated leg curl"))return R.drawable.machine_seated_leg_curl;if(n.contains("prone leg curl"))return R.drawable.machine_prone_leg_curl;if(n.contains("calf"))return R.drawable.machine_calf_extension;if(n.contains("rameur"))return R.drawable.machine_rower;if(n.contains("tapis"))return R.drawable.machine_treadmill;return R.drawable.machine_chest_press;}

    private String machineZone(String name,String group){String text=(name+" "+group).toLowerCase(Locale.ROOT);if(text.contains("chest")||text.contains("pec ")||text.contains("pector"))return "Pectoraux";if(text.contains("shoulder")||text.contains("lateral raise")||text.contains("rear delt")||text.contains("épaule"))return "Épaules";if(text.contains("triceps")||text.contains("biceps")||text.contains("arm curl"))return "Bras";if(text.contains("pulldown")||text.contains("row")||text.contains("back extension")||text.contains("dos")||text.contains("lombaire"))return "Dos";if(text.contains("abdominal")||text.contains("torso rotation")||text.contains("knee raise")||text.contains("gainage")||text.contains("oblique"))return "Abdominaux";if(text.contains("glute")||text.contains("hip abductor")||text.contains("fessier")||text.contains("abducteur"))return "Fessiers";if(text.contains("leg press")||text.contains("leg extension")||text.contains("leg curl")||text.contains("hip adductor")||text.contains("quadriceps")||text.contains("ischio")||text.contains("adducteur")||text.contains("cuisse"))return "Cuisses";if(text.contains("calf")||text.contains("mollet"))return "Mollets";if(text.contains("rameur")||text.contains("tapis")||text.contains("running")||text.contains("cardio"))return "Cardio";return "Autres";}

    private void customMachine() {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22),0,dp(22),0);
        EditText n=new EditText(this); n.setHint("Nom de la machine"); box.addView(n); EditText g=new EditText(this); g.setHint("Groupe musculaire"); box.addView(g);
        new AlertDialog.Builder(this).setTitle("Nouvelle machine").setView(box).setNegativeButton("Annuler",null).setPositiveButton("Ajouter",(d,w)->{
            String name=n.getText().toString().trim(); if(name.isEmpty()) name="Machine personnalisée"; String group=g.getText().toString().trim(); if(group.isEmpty()) group="Autre";
            ExerciseEntry e=new ExerciseEntry(new Machine(name,group,0,10,3,60));applyLastPerformance(e);current.add(e); renderExercises();
        }).show();
    }

    private void showMachinePhoto(String machine) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(6),dp(4),dp(6),0);
        TextView note=new TextView(this); note.setText("Photo officielle Matrix Fitness • connexion Internet nécessaire"); note.setTextSize(12); note.setTextColor(Color.DKGRAY); note.setGravity(Gravity.CENTER); note.setPadding(dp(4),dp(4),dp(4),dp(8)); box.addView(note);
        WebView web=new WebView(this); web.setBackgroundColor(Color.WHITE); web.getSettings().setJavaScriptEnabled(false); web.getSettings().setBuiltInZoomControls(true); web.getSettings().setDisplayZoomControls(false); web.setWebViewClient(new WebViewClient());
        box.addView(web,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(520)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(machine).setView(box).setNegativeButton("Fermer",null).create();
        dialog.setOnDismissListener(d -> { web.stopLoading(); web.destroy(); }); dialog.show(); web.loadUrl(matrixUrl(machine));
    }

    private String matrixUrl(String machine) {
        String n=machine.toLowerCase(Locale.ROOT);
        if(n.contains("incline chest")) return "https://cpo.matrixfitness.com/?s=incline+chest+press&post_type=product";
        if(n.contains("chest press")) return "https://cpo.matrixfitness.com/products/strength/ultra/matrix-ultra-g7-s13-02-converging-chest-press/";
        if(n.contains("pec fly")||n.contains("rear delt")) return "https://cpo.matrixfitness.com/products/strength/ultra/matrix-ultra-rear-delt-pec-fly-polarized-titanium-black-pads/";
        if(n.contains("shoulder")) return "https://cpo.matrixfitness.com/products/strength/aura/aura-converging-shoulder-press-lace-white-black-pads-copy/";
        if(n.contains("pulldown")) return "https://cpo.matrixfitness.com/products/strength/ultra/matrix-ultra-diverging-lat-pull-light-stack-lace-white-black-pads/";
        if(n.contains("abdominal crunch")) return "https://cpo.matrixfitness.com/products/strength/aura/aura-ab-crunch-lace-white-black-pads/";
        String query=machine.trim().replace(" / "," ").replace(" ","+");
        return "https://cpo.matrixfitness.com/?s="+query+"&post_type=product";
    }

    private void syncFields() {
        for(ExerciseEntry e:current) if(e.weightView!=null) {
            e.weight=parseDouble(e.weightView.getText().toString(),e.weight); e.reps=(int)parseDouble(e.repsView.getText().toString(),e.reps); e.sets=(int)parseDouble(e.setsView.getText().toString(),e.sets); e.note=e.noteView.getText().toString().trim();
        }
    }

    private void applyLastPerformance(ExerciseEntry entry) {
        try {
            JSONArray history=new JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString(HISTORY,"[]"));
            for(int i=history.length()-1;i>=0;i--){
                JSONArray exercises=history.getJSONObject(i).optJSONArray("exercises");if(exercises==null)continue;
                for(int j=0;j<exercises.length();j++){
                    JSONObject saved=exercises.getJSONObject(j);
                    if(entry.name.equals(saved.optString("name"))&&saved.optInt("sets",0)>0){
                        entry.weight=saved.optDouble("weight",entry.weight);
                        entry.reps=Math.max(1,saved.optInt("reps",entry.reps));
                        entry.targetSets=Math.max(1,saved.optInt("targetSets",saved.optInt("sets",entry.targetSets)));
                        return;
                    }
                }
            }
        } catch(JSONException ignored){}
    }

    private String recordLabel(ExerciseEntry entry,int ignoredHistoryIndex){
        if(entry.sets<=0)return "";
        boolean found=false;double bestWeight=-1;int bestReps=-1;
        try{JSONArray history=new JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString(HISTORY,"[]"));for(int i=0;i<history.length();i++){if(i==ignoredHistoryIndex)continue;JSONArray xs=history.getJSONObject(i).optJSONArray("exercises");if(xs==null)continue;for(int j=0;j<xs.length();j++){JSONObject x=xs.getJSONObject(j);if(entry.name.equalsIgnoreCase(x.optString("name"))&&x.optInt("sets",0)>0){found=true;bestWeight=Math.max(bestWeight,x.optDouble("weight",0));bestReps=Math.max(bestReps,x.optInt("reps",0));}}}}catch(JSONException ignored){}
        boolean weightRecord=entry.weight>0&&(!found||entry.weight>bestWeight);boolean repsRecord=!found||entry.reps>bestReps;if(!weightRecord&&!repsRecord)return "";return "🏆 "+(weightRecord&&repsRecord?"CHARGE/RÉP.":weightRecord?"CHARGE":"RÉP.");
    }

    private void updateProgress() {
        if (progressText == null) return;
        int done=0; for(ExerciseEntry e:current) if(e.done) done++;
        progressText.setText("Progression : "+done+" / "+current.size()+" machine"+(current.size()>1?"s":""));
        progressText.setTextColor(current.size()>0 && done==current.size() ? Color.rgb(28,110,63) : NAVY);
    }

    private void startTimer(int seconds) {
        stopTimer(); timer=new CountDownTimer(seconds*1000L,1000){ public void onTick(long ms){ timerText.setText("Repos  "+((ms+999)/1000)+" s"); } public void onFinish(){ timerText.setText("✓ Repos terminé"); timerText.setBackground(rounded(GREEN,14)); notifyRestFinished(); }}.start(); timerText.setBackground(rounded(ORANGE,14));
    }
    private void startSeriesRest(ExerciseEntry entry) {
        syncFields(); entry.sets++;if(entry.sets>=entry.targetSets)entry.done=true;if(entry.setsView!=null)entry.setsView.setText(String.valueOf(entry.sets)); startTimer(getDefaultRest());renderExercises();
        Toast.makeText(this,"Série "+entry.sets+" comptabilisée",Toast.LENGTH_SHORT).show();
    }
    private void notifyRestFinished() {
        try { Vibrator vibrator;
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){VibratorManager vm=(VibratorManager)getSystemService(Context.VIBRATOR_MANAGER_SERVICE);vibrator=vm.getDefaultVibrator();}
            else vibrator=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            if(vibrator!=null&&vibrator.hasVibrator())vibrator.vibrate(VibrationEffect.createOneShot(350,VibrationEffect.DEFAULT_AMPLITUDE));
        } catch(Exception ignored){}
        try { ToneGenerator tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,90);tone.startTone(ToneGenerator.TONE_PROP_BEEP2,450);new Handler(Looper.getMainLooper()).postDelayed(tone::release,700); } catch(Exception ignored){}
    }
    private void stopTimer(){ if(timer!=null){timer.cancel();timer=null;} }

    private void saveSession() {
        syncFields(); if(current.isEmpty()){Toast.makeText(this,"Ajoute au moins une machine",Toast.LENGTH_SHORT).show();return;}
        try {
            long endMs=System.currentTimeMillis();
            long durationSeconds=currentStrengthDurationSeconds();
            int totalSets=0;double totalVolume=0;int exerciseCount=0;
            JSONObject session=new JSONObject();
            long startMs=sessionOriginalStartMs>0?sessionOriginalStartMs:endMs;
            session.put("date",new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.FRANCE).format(new Date(startMs)));
            session.put("name",currentSessionName); session.put("bodyWeight",getBodyWeight());
            session.put("startEpochMs",startMs);session.put("endEpochMs",endMs);session.put("durationSeconds",durationSeconds);
            JSONArray exercises=new JSONArray(); for(ExerciseEntry e:current){if(e.sets<=0)continue;JSONObject x=new JSONObject();x.put("name",e.name);x.put("group",e.group);x.put("weight",e.weight);x.put("reps",e.reps);x.put("sets",e.sets);x.put("targetSets",e.targetSets);x.put("note",e.note);x.put("done",e.done);String record=recordLabel(e,editingHistoryIndex);x.put("weightRecord",record.contains("CHARGE"));x.put("repsRecord",record.contains("RÉP."));exercises.put(x);exerciseCount++;totalSets+=e.sets;totalVolume+=e.weight*e.reps*e.sets;}if(exercises.length()==0){Toast.makeText(this,"Aucune machine effectuée à enregistrer",Toast.LENGTH_LONG).show();return;} session.put("exercises",exercises);
            double calories=estimateStrengthCalories(durationSeconds,exerciseCount,totalSets);
            session.put("exerciseCount",exerciseCount);session.put("totalSets",totalSets);session.put("totalVolume",totalVolume);session.put("estimatedCalories",calories);session.put("caloriesSource","estimated");
            // Champs prévus pour une future récupération Health Connect / Galaxy Watch.
            session.put("heartAvg",JSONObject.NULL);session.put("heartMin",JSONObject.NULL);session.put("heartMax",JSONObject.NULL);
            SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE); JSONArray history=new JSONArray(p.getString(HISTORY,"[]"));if(editingHistoryIndex>=0&&editingHistoryIndex<history.length())history.put(editingHistoryIndex,session);else history.put(session); p.edit().putString(HISTORY,history.toString()).apply();
            Toast.makeText(this,(editingHistoryIndex>=0?"Séance mise à jour":"Séance enregistrée")+" • "+formatDuration(durationSeconds)+" • ~"+Math.round(calories)+" kcal",Toast.LENGTH_LONG).show();editingHistoryIndex=-1; currentSessionName="";sessionOriginalStartMs=0;sessionActiveSegmentStartMs=0;sessionAccumulatedSeconds=0; showHome();
        } catch(JSONException ex){ Toast.makeText(this,"Erreur d’enregistrement",Toast.LENGTH_LONG).show(); }
    }

    private void showHistory() {
        baseScreen(); LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(NAVY); Button back=new Button(this); back.setText("‹");back.setTextColor(Color.WHITE);back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(NAVY)); back.setTextSize(28); back.setOnClickListener(v->showHome()); bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56))); TextView h=title("Historique",24);h.setTextColor(Color.WHITE); h.setPadding(dp(8),0,0,0); bar.addView(h); root.addView(bar);
        ScrollView scroll=new ScrollView(this); LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(14),dp(5),dp(14),dp(20)); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        try { SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray history=new JSONArray(prefs.getString(HISTORY,"[]"));JSONArray runs=new JSONArray(prefs.getString(RUN_HISTORY,"[]"));
            if(history.length()==0&&runs.length()==0){TextView empty=title("Aucune séance enregistrée",17);empty.setTextColor(Color.GRAY);list.addView(empty);}
            if(runs.length()>0){list.addView(title("Running",20));for(int i=runs.length()-1;i>=0;i--)addRunningHistoryCard(list,runs.getJSONObject(i),i);}
            if(history.length()>0){list.addView(title("Musculation",20));for(int i=history.length()-1;i>=0;i--){JSONObject s=history.getJSONObject(i); addHistoryCard(list,s,i);}}
        } catch(JSONException ex){Toast.makeText(this,"Historique illisible",Toast.LENGTH_LONG).show();}
        Button clear=action("Effacer tout l’historique"); clear.setTextColor(Color.rgb(150,35,35)); clear.setBackground(outlined(SURFACE,Color.rgb(226,198,198),14)); clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Effacer l’historique ?").setMessage("Cette action est définitive.").setNegativeButton("Annuler",null).setPositiveButton("Effacer",(d,w)->{getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove(HISTORY).remove(RUN_HISTORY).apply();showHistory();}).show()); root.addView(clear);addBottomNav(1);
    }

    private void addRunningHistoryCard(LinearLayout list,JSONObject run,int index){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(rounded(SURFACE,17));card.setElevation(dp(2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(5),0,dp(5));card.setLayoutParams(lp);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);String mode="indoor".equals(run.optString("mode"))?"Tapis":"Outdoor";TextView head=new TextView(this);head.setText((run.optBoolean("isRecord")?"🏆 RECORD • ":"")+"Running "+mode+" • "+run.optString("date",""));head.setTextColor(run.optBoolean("isRecord")?ORANGE:NAVY);head.setTextSize(17);head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.addView(head,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));Button delete=new Button(this);delete.setText("Supprimer");delete.setTextSize(12);delete.setTextColor(Color.rgb(150,35,35));delete.setOnClickListener(v->deleteRunningEntry(index));header.addView(delete,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(44)));card.addView(header);
        double km=run.optDouble("distanceMeters",0)/1000.0;long sec=run.optLong("durationSeconds",0);double speed=sec>0?km/(sec/3600.0):0;double pace=km>0?(sec/60.0)/km:0;String stepType=run.optString("stepsSource");String source="watch".equals(stepType)?"montre":"estimated".equals(stepType)?"estimés":"téléphone";StringBuilder details=new StringBuilder(String.format(Locale.FRANCE,"%.2f km • %02d:%02d:%02d\nAllure %d:%02d min/km • %.1f km/h • %d pas (%s)",km,sec/3600,(sec%3600)/60,sec%60,(int)pace,(int)Math.round((pace-(int)pace)*60),speed,run.optInt("steps",0),source));if(run.has("machineSource"))details.append(String.format(Locale.FRANCE,"\nMatrix FTMS • %.0f tr/min • %.0f W • %.0f kcal • %d bpm",run.optDouble("machineCadence"),run.optDouble("machinePower"),run.optDouble("machineCalories"),run.optInt("machineHeartRate")));if(run.has("heartAvg")){details.append(String.format(Locale.FRANCE,"\nFC %d moy. • %d min. • %d max.",run.optInt("heartAvg"),run.optInt("heartMin"),run.optInt("heartMax")));}if(run.has("watchCalories"))details.append(String.format(Locale.FRANCE," • %.0f kcal%s",run.optDouble("watchCalories"),"estimated".equals(run.optString("caloriesSource"))?" (estimées)":" actives"));if(run.has("watchDistance"))details.append(String.format(Locale.FRANCE,"\nMontre : %.2f km • %.1f km/h moy.",run.optDouble("watchDistance")/1000.0,run.optDouble("watchSpeed")*3.6));TextView body=new TextView(this);body.setText(details);body.setTextColor(Color.DKGRAY);body.setTextSize(14);body.setPadding(0,dp(7),0,0);card.addView(body);
        JSONArray route=run.optJSONArray("route");if(route!=null&&route.length()>1){RouteMapView map=new RouteMapView(this,route);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(145));mp.setMargins(0,dp(10),0,dp(8));card.addView(map,mp);}if(run.has("startEpoch")){Button watch=new Button(this);watch.setText("Récupérer les données de la montre");watch.setAllCaps(false);watch.setTextColor(Color.WHITE);watch.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(55,110,180)));watch.setOnClickListener(v->requestWatchMetrics(index));card.addView(watch,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));}list.addView(card);
    }

    private void requestWatchMetrics(int index){
        if(Build.VERSION.SDK_INT<34){Toast.makeText(this,"Health Connect intégré nécessite Android 14 ou supérieur",Toast.LENGTH_LONG).show();return;}
        String[] permissions={"android.permission.health.READ_STEPS","android.permission.health.READ_HEART_RATE","android.permission.health.READ_ACTIVE_CALORIES_BURNED","android.permission.health.READ_DISTANCE","android.permission.health.READ_SPEED"};List<String> missing=new ArrayList<>();for(String p:permissions)if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED)missing.add(p);if(!missing.isEmpty()){pendingWatchStrengthIndex=-1;pendingWatchRunIndex=index;requestPermissions(missing.toArray(new String[0]),HEALTH_PERMISSION_REQUEST);return;}
        fetchWatchMetrics(index);
    }

    @SuppressWarnings({"rawtypes","unchecked"}) private void fetchWatchMetrics(int index){
        if(Build.VERSION.SDK_INT<34)return;
        try{SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray runs=new JSONArray(prefs.getString(RUN_HISTORY,"[]"));if(index<0||index>=runs.length())return;JSONObject run=runs.getJSONObject(index);long start=run.optLong("startEpoch",0),end=run.optLong("endEpoch",0);if(start<=0||end<=start){Toast.makeText(this,"Cette ancienne sortie ne possède pas les horaires nécessaires",Toast.LENGTH_LONG).show();return;}
            HealthConnectManager manager=getSystemService(HealthConnectManager.class);TimeInstantRangeFilter range=new TimeInstantRangeFilter.Builder().setStartTime(Instant.ofEpochMilli(start-120000)).setEndTime(Instant.ofEpochMilli(end+120000)).build();DataOrigin samsung=new DataOrigin.Builder().setPackageName("com.sec.android.app.shealth").build();AggregateRecordsRequest<Object> request=new AggregateRecordsRequest.Builder<Object>(range).addAggregationType(metric(StepsRecord.STEPS_COUNT_TOTAL)).addAggregationType(metric(HeartRateRecord.BPM_AVG)).addAggregationType(metric(HeartRateRecord.BPM_MIN)).addAggregationType(metric(HeartRateRecord.BPM_MAX)).addAggregationType(metric(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)).addAggregationType(metric(DistanceRecord.DISTANCE_TOTAL)).addAggregationType(metric(SpeedRecord.SPEED_AVG)).addDataOriginsFilter(samsung).build();Toast.makeText(this,"Lecture de Samsung Health…",Toast.LENGTH_SHORT).show();
            manager.aggregate(request,getMainExecutor(),new OutcomeReceiver<AggregateRecordsResponse<Object>,HealthConnectException>(){
                @Override public void onResult(AggregateRecordsResponse<Object> response){Long count=(Long)response.get(metric(StepsRecord.STEPS_COUNT_TOTAL));Long avg=(Long)response.get(metric(HeartRateRecord.BPM_AVG));Long min=(Long)response.get(metric(HeartRateRecord.BPM_MIN));Long max=(Long)response.get(metric(HeartRateRecord.BPM_MAX));Energy calories=(Energy)response.get(metric(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL));Length distance=(Length)response.get(metric(DistanceRecord.DISTANCE_TOTAL));Velocity watchSpeed=(Velocity)response.get(metric(SpeedRecord.SPEED_AVG));if(count==null&&avg==null&&calories==null&&distance==null){new AlertDialog.Builder(MainActivity.this).setTitle("Données non disponibles").setMessage("Ouvre Samsung Health, fais glisser l’accueil vers le bas pour synchroniser la montre, puis réessaie.").setPositiveButton("OK",null).show();return;}saveWatchMetrics(index,count,avg,min,max,calories,distance,watchSpeed);}
                @Override public void onError(HealthConnectException error){Toast.makeText(MainActivity.this,"Lecture Health Connect impossible",Toast.LENGTH_LONG).show();}
            });
        }catch(Exception ex){Toast.makeText(this,"Impossible de lire les pas de la montre",Toast.LENGTH_LONG).show();}
    }

    private void saveWatchMetrics(int index,Long count,Long avg,Long min,Long max,Energy calories,Length distance,Velocity speed){try{SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray old=new JSONArray(p.getString(RUN_HISTORY,"[]"));if(index<0||index>=old.length())return;JSONObject run=old.getJSONObject(index);double meters=run.optDouble("distanceMeters",0);long seconds=run.optLong("durationSeconds",0);long estimatedSteps=Math.round(meters/.78);boolean plausibleSteps=count!=null&&(meters<150||(count>=estimatedSteps*.55&&count<=estimatedSteps*1.6));if(plausibleSteps){run.put("steps",count);run.put("stepsSource","watch");}else if(meters>0){run.put("steps",estimatedSteps);run.put("stepsSource","estimated");}if(avg!=null)run.put("heartAvg",avg);if(min!=null)run.put("heartMin",min);if(max!=null)run.put("heartMax",max);double kcal=calories==null?0:calories.getInCalories();double minutes=Math.max(1,seconds/60.0);boolean plausibleCalories=kcal>=1&&kcal<=minutes*25;if(plausibleCalories){run.put("watchCalories",kcal);run.put("caloriesSource","watch");}else{double kmh=seconds>0?(meters/1000.0)/(seconds/3600.0):0;double met=kmh>=8?8.3:kmh>=6?6.0:4.0;double estimate=met*3.5*getBodyWeight()/200.0*minutes;run.put("watchCalories",estimate);run.put("caloriesSource","estimated");}if(distance!=null)run.put("watchDistance",distance.getInMeters());if(speed!=null)run.put("watchSpeed",speed.getInMetersPerSecond());p.edit().putString(RUN_HISTORY,old.toString()).apply();Toast.makeText(this,plausibleSteps&&plausibleCalories?"Données Samsung Health récupérées":"Données incohérentes remplacées par une estimation",Toast.LENGTH_LONG).show();showHistory();}catch(Exception ex){Toast.makeText(this,"Enregistrement impossible",Toast.LENGTH_LONG).show();}}

    private void requestStrengthWatchMetrics(int index){
        if(Build.VERSION.SDK_INT<34){Toast.makeText(this,"Health Connect intégré nécessite Android 14 ou supérieur",Toast.LENGTH_LONG).show();return;}
        String[] permissions={"android.permission.health.READ_HEART_RATE","android.permission.health.READ_ACTIVE_CALORIES_BURNED"};List<String> missing=new ArrayList<>();for(String p:permissions)if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED)missing.add(p);if(!missing.isEmpty()){pendingWatchRunIndex=-1;pendingWatchStrengthIndex=index;requestPermissions(missing.toArray(new String[0]),HEALTH_PERMISSION_REQUEST);return;}fetchStrengthWatchMetrics(index);
    }

    @SuppressWarnings({"rawtypes","unchecked"}) private void fetchStrengthWatchMetrics(int index){
        if(Build.VERSION.SDK_INT<34)return;
        try{SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray history=new JSONArray(prefs.getString(HISTORY,"[]"));if(index<0||index>=history.length())return;JSONObject session=history.getJSONObject(index);long start=session.optLong("startEpochMs",0),end=session.optLong("endEpochMs",0);if(start<=0||end<=start){Toast.makeText(this,"Cette ancienne séance ne possède pas les horaires nécessaires",Toast.LENGTH_LONG).show();return;}
            HealthConnectManager manager=getSystemService(HealthConnectManager.class);TimeInstantRangeFilter range=new TimeInstantRangeFilter.Builder().setStartTime(Instant.ofEpochMilli(start)).setEndTime(Instant.ofEpochMilli(end)).build();DataOrigin samsung=new DataOrigin.Builder().setPackageName("com.sec.android.app.shealth").build();AggregateRecordsRequest<Object> request=new AggregateRecordsRequest.Builder<Object>(range).addAggregationType(metric(HeartRateRecord.BPM_AVG)).addAggregationType(metric(HeartRateRecord.BPM_MIN)).addAggregationType(metric(HeartRateRecord.BPM_MAX)).addAggregationType(metric(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)).addDataOriginsFilter(samsung).build();Toast.makeText(this,"Lecture de Samsung Health…",Toast.LENGTH_SHORT).show();
            manager.aggregate(request,getMainExecutor(),new OutcomeReceiver<AggregateRecordsResponse<Object>,HealthConnectException>(){
                @Override public void onResult(AggregateRecordsResponse<Object> response){Long avg=(Long)response.get(metric(HeartRateRecord.BPM_AVG));Long min=(Long)response.get(metric(HeartRateRecord.BPM_MIN));Long max=(Long)response.get(metric(HeartRateRecord.BPM_MAX));Energy calories=(Energy)response.get(metric(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL));if(avg==null&&calories==null){new AlertDialog.Builder(MainActivity.this).setTitle("Données non disponibles").setMessage("Démarre une activité Musculation sur la montre pendant la séance, puis synchronise Samsung Health avant de réessayer.").setPositiveButton("OK",null).show();return;}saveStrengthWatchMetrics(index,avg,min,max,calories);}
                @Override public void onError(HealthConnectException error){Toast.makeText(MainActivity.this,"Lecture Health Connect impossible",Toast.LENGTH_LONG).show();}
            });
        }catch(Exception ex){Toast.makeText(this,"Impossible de lire les données de la montre",Toast.LENGTH_LONG).show();}
    }

    private void saveStrengthWatchMetrics(int index,Long avg,Long min,Long max,Energy calories){
        try{SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray history=new JSONArray(p.getString(HISTORY,"[]"));if(index<0||index>=history.length())return;JSONObject session=history.getJSONObject(index);long seconds=session.optLong("durationSeconds",0);double minutes=Math.max(1,seconds/60.0);double kcal=calories==null?0:calories.getInCalories();boolean plausibleCalories=kcal>=1&&kcal<=minutes*25;if(avg!=null)session.put("heartAvg",avg);if(min!=null)session.put("heartMin",min);if(max!=null)session.put("heartMax",max);if(plausibleCalories){session.put("watchCalories",kcal);session.put("caloriesSource","watch");}else session.put("caloriesSource","estimated");p.edit().putString(HISTORY,history.toString()).apply();Toast.makeText(this,plausibleCalories?"Calories Samsung Health récupérées":"Fréquence cardiaque récupérée ; estimation calorique conservée",Toast.LENGTH_LONG).show();showHistory();}catch(Exception ex){Toast.makeText(this,"Enregistrement impossible",Toast.LENGTH_LONG).show();}
    }
    @SuppressWarnings("unchecked") private static AggregationType<Object> metric(AggregationType<?> type){return (AggregationType<Object>)(AggregationType<?>)type;}

    private void deleteRunningEntry(int index){new AlertDialog.Builder(this).setTitle("Supprimer cette sortie ?").setMessage("Cette action est définitive.").setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->{try{SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray old=new JSONArray(p.getString(RUN_HISTORY,"[]"));JSONArray updated=new JSONArray();for(int i=0;i<old.length();i++)if(i!=index)updated.put(old.get(i));p.edit().putString(RUN_HISTORY,updated.toString()).apply();showHistory();}catch(Exception ex){Toast.makeText(this,"Suppression impossible",Toast.LENGTH_LONG).show();}}).show();}

    private void addHistoryCard(LinearLayout list, JSONObject s, int index) throws JSONException {
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(rounded(SURFACE,17));card.setElevation(dp(2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(5),0,dp(5));card.setLayoutParams(lp);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setOrientation(LinearLayout.HORIZONTAL);
        String bodyWeight=s.has("bodyWeight")?" • "+trim(s.optDouble("bodyWeight",0))+" kg":"";
        TextView head=new TextView(this);head.setText(s.getString("name")+" • "+s.getString("date")+bodyWeight);head.setTextColor(NAVY);head.setTextSize(17);head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.addView(head,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        if(isTodaySession(s.optString("date",""))){Button reopen=new Button(this);reopen.setText("Rouvrir");reopen.setTextSize(12);reopen.setAllCaps(false);reopen.setTextColor(Color.WHITE);reopen.setBackground(rounded(BLUE,11));reopen.setOnClickListener(v->reopenSession(index));header.addView(reopen,new LinearLayout.LayoutParams(dp(78),dp(42)));}Button delete=new Button(this);delete.setText("×");delete.setTextSize(20);delete.setTextColor(Color.rgb(150,35,35));delete.setContentDescription("Supprimer cette séance");delete.setBackground(rounded(Color.rgb(245,232,232),11));delete.setOnClickListener(v->confirmDeleteHistoryEntry(index,s.optString("name","Séance"),s.optString("date","")));LinearLayout.LayoutParams delp=new LinearLayout.LayoutParams(dp(44),dp(42));delp.setMargins(dp(6),0,0,0);header.addView(delete,delp);card.addView(header);
        JSONArray xs=s.getJSONArray("exercises");
        long duration=s.optLong("durationSeconds",0);int totalSets=s.optInt("totalSets",0);double totalVolume=s.optDouble("totalVolume",0);boolean watchCalories="watch".equals(s.optString("caloriesSource"))&&s.optDouble("watchCalories",0)>0;double kcal=watchCalories?s.optDouble("watchCalories",0):s.optDouble("estimatedCalories",0);
        if(totalSets<=0||totalVolume<=0){totalSets=0;totalVolume=0;for(int j=0;j<xs.length();j++){JSONObject x=xs.getJSONObject(j);int sets=x.optInt("sets",0);totalSets+=sets;totalVolume+=x.optDouble("weight",0)*x.optInt("reps",0)*sets;}}
        if(duration>0||totalSets>0){TextView metrics=new TextView(this);StringBuilder m=new StringBuilder();if(duration>0)m.append("Durée ").append(formatDuration(duration));if(kcal>0){if(m.length()>0)m.append("  •  ");if(!watchCalories)m.append("~");m.append(Math.round(kcal)).append(" kcal ").append(watchCalories?"(montre)":"(estimées)");}if(totalSets>0){if(m.length()>0)m.append("  •  ");m.append(totalSets).append(" séries");}if(totalVolume>0){if(m.length()>0)m.append("\n");m.append("Volume total ").append(String.format(Locale.FRANCE,"%.0f kg",totalVolume));}if(s.has("heartAvg")&&!s.isNull("heartAvg")){m.append("\nFC ").append(s.optInt("heartAvg")).append(" moy.");if(s.has("heartMin")&&!s.isNull("heartMin"))m.append(" • ").append(s.optInt("heartMin")).append(" min.");if(s.has("heartMax")&&!s.isNull("heartMax"))m.append(" • ").append(s.optInt("heartMax")).append(" max.");}metrics.setText(m.toString());metrics.setTextSize(13);metrics.setTextColor(BLUE);metrics.setTypeface(Typeface.DEFAULT,Typeface.BOLD);metrics.setPadding(0,dp(7),0,dp(2));card.addView(metrics);}
        StringBuilder summary=new StringBuilder();for(int j=0;j<xs.length();j++){JSONObject x=xs.getJSONObject(j);if(j>0)summary.append("\n");if(x.optBoolean("weightRecord")||x.optBoolean("repsRecord"))summary.append("🏆 ");summary.append(x.optBoolean("done",true)?"✓ ":"○ ").append(x.getString("name")).append("  ").append(trim(x.getDouble("weight"))).append(" kg • ").append(x.getInt("sets")).append(" × ").append(x.getInt("reps"));}
        TextView body=new TextView(this);body.setText(summary);body.setTextSize(14);body.setTextColor(Color.DKGRAY);body.setPadding(0,dp(7),0,0);card.addView(body);if(s.optLong("startEpochMs",0)>0&&s.optLong("endEpochMs",0)>s.optLong("startEpochMs",0)){Button watch=new Button(this);watch.setText(watchCalories?"Actualiser les données de la montre":"Récupérer les données de la montre");watch.setAllCaps(false);watch.setTextColor(Color.WHITE);watch.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(55,110,180)));watch.setOnClickListener(v->requestStrengthWatchMetrics(index));card.addView(watch,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));}list.addView(card);
    }

    private void confirmDeleteHistoryEntry(int index, String name, String date) {
        String label=name+(date.isEmpty()?"":" du "+date);
        new AlertDialog.Builder(this).setTitle("Supprimer cette séance ?").setMessage(label+" sera définitivement supprimée.").setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->deleteHistoryEntry(index)).show();
    }

    private boolean isTodaySession(String date){String today=new SimpleDateFormat("dd/MM/yyyy",Locale.FRANCE).format(new Date());return date!=null&&date.startsWith(today);}
    private void loadTemplateExercisesByName(String name){try{JSONArray templates=new JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString(TEMPLATES,"[]"));for(int i=0;i<templates.length();i++){JSONObject template=templates.getJSONObject(i);if(!name.equalsIgnoreCase(template.optString("name")))continue;JSONArray xs=template.optJSONArray("exercises");if(xs!=null)for(int j=0;j<xs.length();j++){JSONObject x=xs.getJSONObject(j);Machine known=findExact(x.optString("name"));ExerciseEntry e=new ExerciseEntry(known!=null?known:new Machine(x.optString("name","Machine"),x.optString("group","Autre"),0,10,3,getDefaultRest()));e.group=x.optString("group",e.group);e.weight=x.optDouble("weight",e.weight);e.reps=x.optInt("reps",e.reps);e.targetSets=Math.max(1,x.optInt("targetSets",e.targetSets));e.note=x.optString("note","");current.add(e);}return;}}catch(JSONException ignored){}}
    private void reopenSession(int index){try{SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray history=new JSONArray(p.getString(HISTORY,"[]"));if(index<0||index>=history.length())return;JSONObject saved=history.getJSONObject(index);if(!isTodaySession(saved.optString("date",""))){Toast.makeText(this,"Seules les séances du jour peuvent être rouvertes",Toast.LENGTH_LONG).show();return;}String name=saved.optString("name","Séance libre");current.clear();loadTemplateExercisesByName(name);
        JSONArray xs=saved.optJSONArray("exercises");if(xs!=null)for(int i=0;i<xs.length();i++){JSONObject x=xs.getJSONObject(i);String exerciseName=x.optString("name","Machine");ExerciseEntry entry=null;for(ExerciseEntry candidate:current)if(candidate.name.equals(exerciseName)){entry=candidate;break;}if(entry==null){Machine known=findExact(exerciseName);entry=new ExerciseEntry(known!=null?known:new Machine(exerciseName,x.optString("group","Autre"),x.optDouble("weight",0),x.optInt("reps",10),Math.max(1,x.optInt("sets",3)),getDefaultRest()));current.add(entry);}entry.weight=x.optDouble("weight",entry.weight);entry.reps=x.optInt("reps",entry.reps);entry.sets=x.optInt("sets",0);entry.note=x.optString("note","");entry.done=x.optBoolean("done",entry.sets>=entry.targetSets);}
        activeExerciseIndex=0;for(int i=0;i<current.size();i++)if(!current.get(i).done){activeExerciseIndex=i;break;}editingHistoryIndex=index;sessionOriginalStartMs=saved.optLong("startEpochMs",parseSessionDateMillis(saved.optString("date","")));sessionAccumulatedSeconds=saved.optLong("durationSeconds",0);sessionActiveSegmentStartMs=System.currentTimeMillis();showSession(name);Toast.makeText(this,"Séance rouverte : durée précédente conservée",Toast.LENGTH_LONG).show();}catch(Exception ex){Toast.makeText(this,"Impossible de rouvrir cette séance",Toast.LENGTH_LONG).show();}}

    private void deleteHistoryEntry(int index) {
        try {
            SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);JSONArray history=new JSONArray(p.getString(HISTORY,"[]"));
            if(index<0||index>=history.length())return;
            JSONArray updated=new JSONArray();for(int i=0;i<history.length();i++)if(i!=index)updated.put(history.get(i));
            p.edit().putString(HISTORY,updated.toString()).apply();Toast.makeText(this,"Séance supprimée",Toast.LENGTH_SHORT).show();showHistory();
        } catch(JSONException ex){Toast.makeText(this,"Impossible de supprimer la séance",Toast.LENGTH_LONG).show();}
    }

    private double getBodyWeight() {
        return Double.longBitsToDouble(getSharedPreferences(PREFS,MODE_PRIVATE).getLong(BODY_WEIGHT,Double.doubleToLongBits(73.0)));
    }

    private String getUserName(){String value=getSharedPreferences(PREFS,MODE_PRIVATE).getString(USER_NAME,"Stéphane").trim();return value.isEmpty()?"Sportif":value;}
    private int getDefaultRest(){return getSharedPreferences(PREFS,MODE_PRIVATE).getInt(DEFAULT_REST,60);}
    private String initials(String name){String[] parts=name.trim().split("\\s+");StringBuilder r=new StringBuilder();for(String p:parts)if(!p.isEmpty()&&r.length()<2)r.append(Character.toUpperCase(p.charAt(0)));return r.length()==0?"?":r.toString();}

    private void showSettings(boolean firstLaunch){currentSessionName="";baseScreen();LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(NAVY);if(!firstLaunch){Button back=new Button(this);back.setText("‹");back.setTextSize(28);back.setTextColor(Color.WHITE);back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(NAVY));back.setOnClickListener(v->showHome());bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));}TextView h=title(firstLaunch?"Bienvenue":"Paramètres",24);h.setTextColor(Color.WHITE);h.setPadding(dp(18),0,0,0);bar.addView(h);root.addView(bar);
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(20),dp(18),dp(28));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));TextView intro=new TextView(this);intro.setText(firstLaunch?"Configure ton profil avant la première séance.":"Ces valeurs seront utilisées pour les prochaines séances.");intro.setTextColor(Color.GRAY);intro.setTextSize(14);intro.setPadding(0,0,0,dp(18));content.addView(intro);
        EditText name=new EditText(this);name.setHint("Prénom ou nom");name.setText(firstLaunch?"":getUserName());name.setSingleLine(true);content.addView(settingsField("UTILISATEUR",name));EditText weight=numberField("kg",getBodyWeight(),true);content.addView(settingsField("POIDS ACTUEL (KG)",weight));EditText rest=numberField("secondes",getDefaultRest(),false);content.addView(settingsField("TEMPS DE REPOS PAR DÉFAUT (SECONDES)",rest));content.addView(matrixSettingsCard());TextView help=new TextView(this);help.setText("Le nom n’est pas récupéré automatiquement par Android. Chaque installation possède son propre profil, enregistré uniquement sur le téléphone.");help.setTextColor(Color.GRAY);help.setTextSize(13);help.setPadding(dp(4),dp(8),dp(4),dp(18));content.addView(help);Button save=action(firstLaunch?"Créer mon profil":"Enregistrer les paramètres");save.setBackground(rounded(ORANGE,14));save.setOnClickListener(v->{String user=name.getText().toString().trim();double kg=parseDouble(weight.getText().toString(),73);int seconds=(int)parseDouble(rest.getText().toString(),60);if(user.isEmpty()){Toast.makeText(this,"Saisis un prénom ou un nom",Toast.LENGTH_LONG).show();return;}if(kg<30||kg>300){Toast.makeText(this,"Poids attendu entre 30 et 300 kg",Toast.LENGTH_LONG).show();return;}if(seconds<15||seconds>300){Toast.makeText(this,"Repos attendu entre 15 et 300 secondes",Toast.LENGTH_LONG).show();return;}getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(USER_NAME,user).putLong(BODY_WEIGHT,Double.doubleToLongBits(kg)).putInt(DEFAULT_REST,seconds).apply();showHome();});content.addView(save);if(!firstLaunch)addBottomNav(3);
    }
    private View settingsField(String label,EditText input){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(10),dp(14),dp(8));box.setBackground(rounded(SURFACE,15));box.setElevation(dp(2));TextView l=new TextView(this);l.setText(label);l.setTextColor(Color.GRAY);l.setTextSize(11);l.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(l);input.setTextColor(NAVY);input.setTextSize(18);input.setBackgroundColor(Color.TRANSPARENT);box.addView(input,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(82));p.setMargins(0,0,0,dp(12));box.setLayoutParams(p);return box;}
    private View matrixSettingsCard(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(12),dp(14),dp(12));box.setBackground(rounded(SURFACE,15));box.setElevation(dp(2));TextView title=new TextView(this);title.setText("MACHINE MATRIX BLUETOOTH");title.setTextColor(NAVY);title.setTextSize(13);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(title);TextView state=new TextView(this);String saved=matrixFtms.savedName();state.setText(saved.isEmpty()?"Aucune machine enregistrée":saved+" • "+matrixFtms.status);state.setTextColor(Color.GRAY);state.setTextSize(13);state.setPadding(0,dp(5),0,dp(10));box.addView(state);LinearLayout buttons=new LinearLayout(this);Button scan=new Button(this);scan.setText("Rechercher");scan.setAllCaps(false);scan.setTextColor(Color.WHITE);scan.setBackground(rounded(BLUE,12));scan.setOnClickListener(v->requestMatrixScan());buttons.addView(scan,new LinearLayout.LayoutParams(0,dp(48),1));Button forget=new Button(this);forget.setText("Oublier");forget.setAllCaps(false);forget.setEnabled(matrixFtms.hasSavedDevice());forget.setOnClickListener(v->{matrixFtms.forget();showSettings(false);});buttons.addView(forget,new LinearLayout.LayoutParams(0,dp(48),1));box.addView(buttons);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(142));p.setMargins(0,0,0,dp(12));box.setLayoutParams(p);return box;}
    private void requestMatrixScan(){List<String> missing=new ArrayList<>();if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission("android.permission.BLUETOOTH_SCAN")!=PackageManager.PERMISSION_GRANTED)missing.add("android.permission.BLUETOOTH_SCAN");if(checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED)missing.add("android.permission.BLUETOOTH_CONNECT");}else if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)missing.add(Manifest.permission.ACCESS_FINE_LOCATION);if(!missing.isEmpty()){requestPermissions(missing.toArray(new String[0]),MATRIX_PERMISSION_REQUEST);return;}scanMatrixDevicesV45();}
    private void scanMatrixDevices(){if(Build.VERSION.SDK_INT>=31&&(checkSelfPermission("android.permission.BLUETOOTH_SCAN")!=PackageManager.PERMISSION_GRANTED||checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED))return;LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setPadding(dp(10),dp(6),dp(10),dp(10));LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);TextView searching=new TextView(this);searching.setText("Recherche de tous les périphériques pendant 10 secondes…");searching.setGravity(Gravity.CENTER);searching.setPadding(dp(8),dp(18),dp(8),dp(18));list.addView(searching);ScrollView scroll=new ScrollView(this);scroll.addView(list);outer.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));Button close=new Button(this);close.setText("Fermer");close.setAllCaps(false);outer.addView(close,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Périphériques Bluetooth à proximité").setView(outer).create();close.setOnClickListener(v->{matrixFtms.stopScan();dialog.dismiss();});dialog.setOnCancelListener(d->matrixFtms.stopScan());dialog.setOnDismissListener(d->matrixFtms.stopScan());dialog.show();dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,dp(560));final Map<String,BleCandidate> devices=new LinkedHashMap<>();matrixFtms.scan(new MatrixFtmsManager.ScanListener(){public void onDevice(BluetoothDevice device,int rssi){runOnUiThread(()->{if(!dialog.isShowing())return;devices.put(device.getAddress(),new BleCandidate(device,rssi));renderCandidates(list,dialog,devices);});}public void onFinished(){runOnUiThread(()->{if(!dialog.isShowing())return;if(devices.isEmpty()){list.removeAllViews();searching.setText("Aucun périphérique détecté. Vérifie le Bluetooth puis relance la recherche.");list.addView(searching);}});}});}
    private void renderCandidates(LinearLayout list,AlertDialog dialog,Map<String,BleCandidate> devices){list.removeAllViews();TextView info=new TextView(this);info.setText("Touchez une machine pour lancer l’appairage • tri par proximité");info.setTextColor(Color.GRAY);info.setTextSize(12);info.setGravity(Gravity.CENTER);info.setPadding(dp(5),dp(5),dp(5),dp(10));list.addView(info);List<BleCandidate> ordered=new ArrayList<>(devices.values());Collections.sort(ordered,(a,b)->Integer.compare(b.rssi,a.rssi));for(BleCandidate candidate:ordered){BluetoothDevice device=candidate.device;String name;try{name=device.getName();}catch(SecurityException e){name=null;}if(name==null||name.trim().isEmpty())name="Périphérique sans nom";final String deviceName=name;String address=device.getAddress();String identifier=address.length()>5?address.substring(address.length()-5):address;String bondState=matrixFtms.bondStateLabel(device);Button row=new Button(this);row.setText(deviceName+"\n"+proximityLabel(candidate.rssi)+" • "+candidate.rssi+" dBm • "+bondState+" • ID …"+identifier);row.setAllCaps(false);row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setOnClickListener(v->{matrixFtms.stopScan();row.setEnabled(false);row.setText(deviceName+"\nAppairage en cours…");matrixFtms.pairAndConnect(device,new MatrixFtmsManager.PairListener(){public void onPairedAndConnecting(){runOnUiThread(()->{Toast.makeText(MainActivity.this,"Appairage terminé. Connexion à "+deviceName+"…",Toast.LENGTH_LONG).show();dialog.dismiss();});}public void onAlreadyPaired(){runOnUiThread(()->{Toast.makeText(MainActivity.this,"Machine déjà appairée. Connexion à "+deviceName+"…",Toast.LENGTH_LONG).show();dialog.dismiss();});}public void onPairingFailed(String message){runOnUiThread(()->{row.setEnabled(true);row.setText(deviceName+"\n"+proximityLabel(candidate.rssi)+" • "+candidate.rssi+" dBm • appairage échoué");Toast.makeText(MainActivity.this,message,Toast.LENGTH_LONG).show();});}});});list.addView(row,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(78)));}}
    private String proximityLabel(int rssi){if(rssi>=-55)return "Très proche";if(rssi>=-68)return "Proche";return "Éloignée";}

    private void scanMatrixDevicesV45(){
        if(Build.VERSION.SDK_INT>=31&&(checkSelfPermission("android.permission.BLUETOOTH_SCAN")!=PackageManager.PERMISSION_GRANTED||checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED))return;
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(10),dp(8),dp(10),dp(14));TextView searching=new TextView(this);searching.setText("Recherche de tous les périphériques pendant 10 secondes…");searching.setGravity(Gravity.CENTER);searching.setPadding(dp(8),dp(24),dp(8),dp(24));list.addView(searching);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(list);scroll.setMinimumHeight(dp(480));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Périphériques Bluetooth à proximité").setView(scroll).setNegativeButton("Fermer",(d,w)->matrixFtms.stopScan()).create();dialog.setCanceledOnTouchOutside(false);dialog.setOnCancelListener(d->matrixFtms.stopScan());dialog.setOnDismissListener(d->matrixFtms.stopScan());dialog.show();
        final Map<String,BleCandidate> devices=new LinkedHashMap<>();matrixFtms.scan(new MatrixFtmsManager.ScanListener(){public void onDevice(BluetoothDevice device,int rssi){runOnUiThread(()->{if(!dialog.isShowing())return;devices.put(device.getAddress(),new BleCandidate(device,rssi));renderCandidatesV45(list,dialog,devices);});}public void onFinished(){runOnUiThread(()->{if(!dialog.isShowing())return;if(devices.isEmpty()){list.removeAllViews();searching.setText("Aucun périphérique détecté. Vérifie le Bluetooth puis relance la recherche.");list.addView(searching);}});}});
    }

    private void renderCandidatesV45(LinearLayout list,AlertDialog dialog,Map<String,BleCandidate> devices){
        list.removeAllViews();TextView info=new TextView(this);info.setText("Appuie sur APPARIER • appareils classés par proximité");info.setTextColor(Color.GRAY);info.setTextSize(12);info.setGravity(Gravity.CENTER);info.setPadding(dp(5),dp(5),dp(5),dp(10));list.addView(info);List<BleCandidate> ordered=new ArrayList<>(devices.values());Collections.sort(ordered,(a,b)->Integer.compare(b.rssi,a.rssi));for(BleCandidate candidate:ordered){BluetoothDevice device=candidate.device;String rawName;try{rawName=device.getName();}catch(SecurityException e){rawName=null;}if(rawName==null||rawName.trim().isEmpty())rawName="Périphérique sans nom";final String deviceName=rawName;String address=device.getAddress();String identifier=address.length()>5?address.substring(address.length()-5):address;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(7),dp(6),dp(7));row.setBackground(rounded(BACKGROUND,12));LinearLayout.LayoutParams rowp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(82));rowp.setMargins(0,dp(3),0,dp(3));TextView details=new TextView(this);details.setText(deviceName+"\n"+proximityLabel(candidate.rssi)+" • "+candidate.rssi+" dBm • "+matrixFtms.bondStateLabel(device)+" • ID …"+identifier);details.setTextColor(NAVY);details.setTextSize(13);details.setGravity(Gravity.CENTER_VERTICAL);row.addView(details,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));Button pair=new Button(this);pair.setText("Appairer");pair.setAllCaps(false);pair.setTextSize(11);pair.setTextColor(Color.WHITE);pair.setBackground(rounded(BLUE,10));pair.setOnClickListener(v->pairMatrixCandidate(candidate,deviceName,details,pair,dialog));row.setOnClickListener(v->pair.performClick());row.addView(pair,new LinearLayout.LayoutParams(dp(88),dp(48)));list.addView(row,rowp);}
    }

    private void pairMatrixCandidate(BleCandidate candidate,String deviceName,TextView details,Button pair,AlertDialog dialog){
        matrixFtms.stopScan();pair.setEnabled(false);pair.setText("Patiente…");details.setText(deviceName+"\nAppairage Android en cours…");matrixFtms.pairAndConnect(candidate.device,new MatrixFtmsManager.PairListener(){public void onPairedAndConnecting(){runOnUiThread(()->{Toast.makeText(MainActivity.this,"Appairage terminé. Connexion à "+deviceName+"…",Toast.LENGTH_LONG).show();dialog.dismiss();});}public void onAlreadyPaired(){runOnUiThread(()->{Toast.makeText(MainActivity.this,"Connexion à "+deviceName+"…",Toast.LENGTH_LONG).show();dialog.dismiss();});}public void onPairingFailed(String message){runOnUiThread(()->{pair.setEnabled(true);pair.setText("Réessayer");details.setText(deviceName+"\nAppairage échoué • "+candidate.rssi+" dBm");Toast.makeText(MainActivity.this,message,Toast.LENGTH_LONG).show();});}});
    }

    private void editBodyWeight() {
        EditText input=numberField("kg",getBodyWeight(),true); input.setHint("Poids en kg"); input.setPadding(dp(24),0,dp(24),0);
        new AlertDialog.Builder(this).setTitle("Mon poids actuel").setMessage("Ce poids sera associé aux prochaines séances enregistrées.").setView(input)
            .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
                double value=parseDouble(input.getText().toString(),getBodyWeight());
                if(value<30||value>300){Toast.makeText(this,"Saisis un poids entre 30 et 300 kg",Toast.LENGTH_LONG).show();return;}
                getSharedPreferences(PREFS,MODE_PRIVATE).edit().putLong(BODY_WEIGHT,Double.doubleToLongBits(value)).apply(); showHome();
            }).show();
    }

    private void showProgress() {
        baseScreen(); LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(NAVY);
        Button back=new Button(this); back.setText("‹");back.setTextColor(Color.WHITE);back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(NAVY)); back.setTextSize(28); back.setOnClickListener(v->showHome()); bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView h=title("Mes évolutions",24);h.setTextColor(Color.WHITE); h.setPadding(dp(8),0,0,0); bar.addView(h); root.addView(bar);
        ScrollView scroll=new ScrollView(this); LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(14),0,dp(14),dp(24)); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        try {
            JSONArray history=new JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString(HISTORY,"[]"));
            content.addView(title("Évolution du poids",20));
            List<Double> weights=new ArrayList<>(); List<String> weightDates=new ArrayList<>();
            for(int i=0;i<history.length();i++){JSONObject s=history.getJSONObject(i);if(s.has("bodyWeight")){weights.add(s.getDouble("bodyWeight"));weightDates.add(shortDate(s.optString("date","")));}}
            content.addView(new ProgressChartView(this,"Poids",weights,weightDates,"kg",ORANGE),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(230)));
            TextView machineTitle=title("Progression par machine",20); machineTitle.setPadding(dp(20),dp(22),dp(20),dp(8)); content.addView(machineTitle);
            Set<String> names=new LinkedHashSet<>(); for(int i=0;i<history.length();i++){JSONArray xs=history.getJSONObject(i).optJSONArray("exercises");if(xs!=null)for(int j=0;j<xs.length();j++){JSONObject x=xs.getJSONObject(j);if(x.optInt("sets",0)>0)names.add(x.optString("name",""));}}
            Button choose=action(names.isEmpty()?"Aucune machine enregistrée":"Choisir une machine"); choose.setEnabled(!names.isEmpty()); choose.setOnClickListener(v->chooseProgressMachine(history,names)); content.addView(choose);
            TextView help=new TextView(this); help.setText("Les anciennes séances restent compatibles. La courbe de poids commencera dès la prochaine séance enregistrée."); help.setTextSize(13); help.setTextColor(Color.DKGRAY); help.setPadding(dp(20),dp(10),dp(20),0); content.addView(help);
        } catch(JSONException ex){Toast.makeText(this,"Impossible de lire les évolutions",Toast.LENGTH_LONG).show();}addBottomNav(2);
    }

    private void chooseProgressMachine(JSONArray history, Set<String> names) {
        String[] items=names.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Machine à analyser").setItems(items,(d,which)->showMachineProgress(history,items[which])).setNegativeButton("Annuler",null).show();
    }

    private void showMachineProgress(JSONArray history,String machine) {
        LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(8),0,dp(8),dp(8));
        List<Double> weights=new ArrayList<>(),reps=new ArrayList<>(),sets=new ArrayList<>(),volumes=new ArrayList<>(); List<String> dates=new ArrayList<>();
        try { for(int i=0;i<history.length();i++){JSONObject s=history.getJSONObject(i);JSONArray xs=s.optJSONArray("exercises");if(xs==null)continue;for(int j=0;j<xs.length();j++){JSONObject x=xs.getJSONObject(j);if(machine.equals(x.optString("name"))&&x.optInt("sets",0)>0){double w=x.optDouble("weight",0);double r=x.optDouble("reps",0);double se=x.optDouble("sets",0);weights.add(w);reps.add(r);sets.add(se);volumes.add(w*r*se);dates.add(shortDate(s.optString("date","")));break;}}} } catch(JSONException ignored){}
        content.addView(new ProgressChartView(this,"Charge",weights,dates,"kg",ORANGE),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(205)));
        content.addView(new ProgressChartView(this,"Répétitions",reps,dates,"",Color.rgb(28,110,63)),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(205)));
        content.addView(new ProgressChartView(this,"Séries",sets,dates,"",Color.rgb(55,110,180)),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(205)));
        content.addView(new ProgressChartView(this,"Volume total",volumes,dates,"kg",Color.rgb(130,75,165)),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(205)));
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        new AlertDialog.Builder(this).setTitle(machine).setView(scroll).setNegativeButton("Fermer",null).show();
    }

    private String shortDate(String date){return date.length()>=5?date.substring(0,5):date;}

    private void swipeExercise(int direction){if(current.isEmpty())return;syncFields();int target=activeExerciseIndex+direction;if(target<0||target>=current.size()){Toast.makeText(this,target<0?"Première machine":"Dernière machine",Toast.LENGTH_SHORT).show();return;}activeExerciseIndex=target;renderExercises();}

    private class SwipeExerciseScrollView extends ScrollView {
        private float downX,downY;private boolean horizontal;
        SwipeExerciseScrollView(Context context){super(context);setFillViewport(true);}
        @Override public boolean onInterceptTouchEvent(MotionEvent event){switch(event.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=event.getX();downY=event.getY();horizontal=false;super.onInterceptTouchEvent(event);return false;case MotionEvent.ACTION_MOVE:float dx=Math.abs(event.getX()-downX),dy=Math.abs(event.getY()-downY);if(dx>dp(24)&&dx>dy*1.35f){horizontal=true;return true;}break;}return super.onInterceptTouchEvent(event);}
        @Override public boolean onTouchEvent(MotionEvent event){if(horizontal){if(event.getActionMasked()==MotionEvent.ACTION_UP){float delta=event.getX()-downX;if(Math.abs(delta)>dp(65))swipeExercise(delta<0?1:-1);horizontal=false;}else if(event.getActionMasked()==MotionEvent.ACTION_CANCEL)horizontal=false;return true;}return super.onTouchEvent(event);}
    }

    private Machine find(String name){for(Machine m:catalog)if(m.name.equals(name))return m;return catalog[0];}
    private Machine findExact(String name){for(Machine m:catalog)if(m.name.equals(name))return m;return null;}
    private double parseDouble(String s,double fallback){try{return Double.parseDouble(s.replace(',','.'));}catch(Exception e){return fallback;}}
    private String trim(double v){return v==(long)v?String.valueOf((long)v):String.format(Locale.FRANCE,"%.1f",v);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

    private static class Machine {String name,group;double weight;int reps,sets,rest;Machine(String n,String g,double w,int r,int s,int t){name=n;group=g;weight=w;reps=r;sets=s;rest=t;}}
    private static class BleCandidate {BluetoothDevice device;int rssi;BleCandidate(BluetoothDevice d,int signal){device=d;rssi=signal;}}
    private static class ExerciseEntry {String name,group,note="";double weight;int reps,sets,rest,targetSets;boolean done=false;EditText weightView,repsView,setsView,noteView;ExerciseEntry(Machine m){name=m.name;group=m.group;weight=m.weight;reps=m.reps;targetSets=m.sets;sets=0;rest=60;}}

    private static class ProgressChartView extends View {
        private final List<Double> values; private final List<String> labels; private final String title,unit; private final int color; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        ProgressChartView(Context c,String t,List<Double> v,List<String> l,String u,int lineColor){super(c);title=t;values=v;labels=l;unit=u;color=lineColor;GradientDrawable bg=new GradientDrawable();bg.setColor(SURFACE);bg.setCornerRadius(18*getResources().getDisplayMetrics().density);setBackground(bg);setElevation(2*getResources().getDisplayMetrics().density);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(16*getResources().getDisplayMetrics().scaledDensity);p.setColor(NAVY);c.drawText(title,dpLocal(12),dpLocal(25),p);
            if(values.isEmpty()){p.setTypeface(Typeface.DEFAULT);p.setTextSize(14*getResources().getDisplayMetrics().scaledDensity);p.setColor(Color.GRAY);c.drawText("Pas encore de données",dpLocal(12),h/2,p);return;}
            double min=values.get(0),max=values.get(0);for(double v:values){min=Math.min(min,v);max=Math.max(max,v);}double pad=Math.max((max-min)*.15,Math.max(1,max*.04));min=Math.max(0,min-pad);max+=pad;if(max<=min)max=min+1;
            float left=dpLocal(42),right=w-dpLocal(14),top=dpLocal(48),bottom=h-dpLocal(37);p.setStrokeWidth(dpLocal(1));p.setColor(Color.rgb(205,211,217));for(int i=0;i<4;i++){float y=top+(bottom-top)*i/3f;c.drawLine(left,y,right,y,p);}
            p.setColor(color);p.setStrokeWidth(dpLocal(3));p.setStyle(Paint.Style.STROKE);float prevX=0,prevY=0;for(int i=0;i<values.size();i++){float x=values.size()==1?(left+right)/2:left+(right-left)*i/(values.size()-1f);float y=(float)(bottom-(values.get(i)-min)/(max-min)*(bottom-top));if(i>0)c.drawLine(prevX,prevY,x,y,p);prevX=x;prevY=y;}p.setStyle(Paint.Style.FILL);
            p.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);p.setTypeface(Typeface.DEFAULT);for(int i=0;i<values.size();i++){float x=values.size()==1?(left+right)/2:left+(right-left)*i/(values.size()-1f);float y=(float)(bottom-(values.get(i)-min)/(max-min)*(bottom-top));p.setColor(color);c.drawCircle(x,y,dpLocal(4),p);if(values.size()<=8||i==0||i==values.size()-1){String val=format(values.get(i))+unit;p.setColor(NAVY);c.drawText(val,Math.max(left,Math.min(x-dpLocal(10),right-dpLocal(28))),y-dpLocal(7),p);String lab=i<labels.size()?labels.get(i):"";p.setColor(Color.DKGRAY);c.drawText(lab,Math.max(left,Math.min(x-dpLocal(11),right-dpLocal(25))),bottom+dpLocal(19),p);}}
        }
        private float dpLocal(int v){return v*getResources().getDisplayMetrics().density;}
        private String format(double v){return v==(long)v?String.valueOf((long)v):String.format(Locale.FRANCE,"%.1f",v);}
    }

    private static class RouteMapView extends View {
        private final List<double[]> points=new ArrayList<>();private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        RouteMapView(Context context,JSONArray route){super(context);setContentDescription("Aperçu du parcours GPS");for(int i=0;i<route.length();i++)try{JSONArray pair=route.getJSONArray(i);points.add(new double[]{pair.getDouble(0),pair.getDouble(1)});}catch(Exception ignored){}}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();paint.setColor(Color.rgb(232,237,241));paint.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(0,0,w,h),dpv(15),dpv(15),paint);paint.setColor(Color.rgb(210,219,226));paint.setStrokeWidth(dpv(1));for(int i=1;i<5;i++){float x=w*i/5f;c.drawLine(x,0,x,h,paint);}for(int i=1;i<4;i++){float y=h*i/4f;c.drawLine(0,y,w,y,paint);}if(points.size()<2)return;double minLat=points.get(0)[0],maxLat=minLat,minLon=points.get(0)[1],maxLon=minLon;for(double[] p:points){minLat=Math.min(minLat,p[0]);maxLat=Math.max(maxLat,p[0]);minLon=Math.min(minLon,p[1]);maxLon=Math.max(maxLon,p[1]);}double latRange=Math.max(maxLat-minLat,.00001),lonRange=Math.max(maxLon-minLon,.00001);float pad=dpv(18);Path path=new Path();for(int i=0;i<points.size();i++){double[] p=points.get(i);float x=(float)(pad+(p[1]-minLon)/lonRange*(w-2*pad));float y=(float)(h-pad-(p[0]-minLat)/latRange*(h-2*pad));if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}paint.setColor(BLUE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dpv(4));paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);c.drawPath(path,paint);double[] first=points.get(0),last=points.get(points.size()-1);drawMarker(c,first,minLat,latRange,minLon,lonRange,w,h,pad,GREEN);drawMarker(c,last,minLat,latRange,minLon,lonRange,w,h,pad,ORANGE);}
        private void drawMarker(Canvas c,double[] p,double minLat,double latRange,double minLon,double lonRange,float w,float h,float pad,int color){float x=(float)(pad+(p[1]-minLon)/lonRange*(w-2*pad));float y=(float)(h-pad-(p[0]-minLat)/latRange*(h-2*pad));paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);c.drawCircle(x,y,dpv(7),paint);paint.setColor(color);c.drawCircle(x,y,dpv(5),paint);}
        private float dpv(int v){return v*getResources().getDisplayMetrics().density;}
    }

    private static class MachineIconView extends View {
        private final String machine;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        MachineIconView(Context context,String name){super(context);machine=name;setPadding(3,3,3,3);}
        private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){p.setColor(color);p.setStrokeWidth(width);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x1,y1,x2,y2,p);}
        private void circle(Canvas c,float x,float y,float r,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),sx=w/100f,sy=h/80f;c.save();c.scale(sx,sy);
            p.setColor(Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(2,2,98,78),9,9,p);p.setColor(Color.rgb(210,216,222));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRoundRect(new RectF(2,2,98,78),9,9,p);
            int frame=Color.rgb(80,94,108), person=ORANGE; String n=machine.toLowerCase(Locale.ROOT);
            if(n.contains("tapis")){line(c,12,62,88,62,frame,5);line(c,75,60,86,28,frame,4);circle(c,46,22,6,person);line(c,46,29,49,46,person,5);line(c,49,35,62,42,person,4);line(c,49,46,38,59,person,5);line(c,49,46,61,58,person,5);
            }else if(n.contains("rameur")){line(c,12,61,88,61,frame,5);line(c,30,54,48,40,frame,4);circle(c,51,24,6,person);line(c,50,31,46,46,person,5);line(c,46,38,67,32,person,4);line(c,67,32,75,20,frame,3);line(c,46,46,28,56,person,5);
            }else if(n.contains("pulldown")){line(c,18,14,18,66,frame,6);line(c,18,15,82,15,frame,4);line(c,30,20,70,20,frame,4);circle(c,50,33,6,person);line(c,50,40,50,55,person,5);line(c,50,42,34,23,person,4);line(c,50,42,66,23,person,4);line(c,36,61,64,61,frame,6);
            }else if(n.contains("seated row")){line(c,15,17,15,65,frame,6);line(c,15,28,48,42,frame,3);circle(c,58,27,6,person);line(c,57,34,52,51,person,5);line(c,54,39,34,37,person,4);line(c,52,51,38,62,person,5);line(c,47,61,78,61,frame,6);
            }else if(n.contains("knee raise")){line(c,18,14,18,66,frame,5);line(c,18,18,78,18,frame,4);line(c,70,18,70,48,frame,4);circle(c,55,28,6,person);line(c,55,35,55,49,person,5);line(c,55,39,70,34,person,4);line(c,55,49,43,58,person,5);line(c,43,58,54,63,person,5);
            }else if(n.contains("gainage")){circle(c,25,44,6,person);line(c,31,45,66,48,person,6);line(c,66,48,84,61,person,5);line(c,37,47,27,61,person,5);line(c,10,64,90,64,frame,3);
            }else if(n.contains("abdominal")){line(c,22,60,78,60,frame,6);line(c,37,57,47,40,frame,5);circle(c,53,25,6,person);line(c,52,32,47,48,person,6);line(c,51,37,68,45,person,4);line(c,68,45,76,31,frame,3);
            }else if(n.contains("rotation")){line(c,28,63,72,63,frame,6);circle(c,50,25,6,person);line(c,50,32,50,53,person,6);line(c,50,39,31,34,person,4);line(c,50,39,69,34,person,4);line(c,31,34,23,27,frame,3);line(c,69,34,77,27,frame,3);
            }else if(n.contains("curl")){line(c,25,64,75,64,frame,6);circle(c,50,23,6,person);line(c,50,30,50,49,person,6);line(c,49,37,35,48,person,4);line(c,35,48,29,38,person,4);line(c,51,37,65,48,person,4);line(c,65,48,71,38,person,4);
            }else if(n.contains("triceps")){line(c,18,15,18,66,frame,6);line(c,18,16,70,16,frame,3);line(c,70,16,70,34,frame,2);circle(c,52,31,6,person);line(c,52,38,52,57,person,6);line(c,52,40,65,35,person,4);line(c,65,35,70,48,person,4);line(c,40,64,66,64,frame,5);
            }else if(n.contains("lateral")||n.contains("rear delt")||n.contains("fly")){line(c,18,14,18,66,frame,6);line(c,82,14,82,66,frame,6);circle(c,50,28,6,person);line(c,50,35,50,55,person,6);line(c,50,40,25,31,person,4);line(c,50,40,75,31,person,4);line(c,38,63,62,63,frame,6);
            }else if(n.contains("shoulder")){line(c,18,14,18,66,frame,6);line(c,82,14,82,66,frame,6);circle(c,50,32,6,person);line(c,50,39,50,57,person,6);line(c,50,43,34,27,person,4);line(c,34,27,34,17,person,4);line(c,50,43,66,27,person,4);line(c,66,27,66,17,person,4);
            }else if(n.contains("chest")){line(c,18,14,18,66,frame,6);line(c,82,14,82,66,frame,6);line(c,35,63,65,63,frame,6);circle(c,50,28,6,person);line(c,50,35,50,55,person,6);line(c,50,40,34,43,person,4);line(c,34,43,25,36,person,4);line(c,50,40,66,43,person,4);line(c,66,43,75,36,person,4);
            }else{line(c,15,37,85,37,frame,7);line(c,15,25,15,49,frame,6);line(c,85,25,85,49,frame,6);line(c,8,28,8,46,person,6);line(c,92,28,92,46,person,6);}
            c.restore();
        }
    }
}
