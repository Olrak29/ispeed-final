package com.example.ispeed.service;

import static com.example.ispeed.Model.TrackInternetModel.TrackStatusEnum.CONNECTED;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import com.example.ispeed.FunctionMethod.FunctionMethod;
import com.example.ispeed.Model.FirebaseInternetDataModel;
import com.example.ispeed.Model.InternetDataModel;
import com.example.ispeed.Model.TrackInternetModel;
import com.example.ispeed.View.DashboardMainActivity;
import com.example.ispeed.View.HomeActivity;
import com.example.ispeed.local.iSpeedClient;
import com.example.ispeed.test.HttpDownloadTest;
import com.example.ispeed.test.HttpUploadTest;
import com.example.ispeed.test.PingTest;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import android.app.Service;
import android.os.IBinder;
import com.example.ispeed.GetSpeedTestHostsHandler;
import com.example.ispeed.R;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import es.dmoral.toasty.Toasty;

public class MyService extends Service {
    DashboardMainActivity main;
    private static final String TAG = "MyService";
    GetSpeedTestHostsHandler getSpeedTestHostsHandler = null;
    HashSet<String> tempBlackList;
    InternetDataModel dataModel;
    final DecimalFormat dec = new DecimalFormat("#.##");
    private FirebaseInternetDataModel fbDataModel = new FirebaseInternetDataModel();
    String currentLocation;
    Double latitude;
    Double longitude;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser firebaseUser;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    FunctionMethod functionMethod = new FunctionMethod();
    FirebaseFirestore db;
    int delay = 10000;
    int period = 180000;
    int freq30Sec = 30000;
    int freq1Min = 60000; //60000 //30000
    int freq3Min = 180000;
    int freq15Min = 900000;
    int freq30Min = 1800000;
    int freq1Hr = 3600000;

    Timer timer = new Timer();
    Timer timerKiller = new Timer();

    public static boolean isTrackingStarted = false;

    //Filter var
    String filterDateStr = "";
    Date filterDate = new Date();
    String filterStartingHour = "";
    String filterEndingHour = "";
    int filterFrequency = 0;

    public static boolean isTimerStopped = true;

    private PowerManager.WakeLock wakeLock;
    private int allowed_time = 30;
    private static final int ONE_MINUTE = 60000;

    public MyService() { }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        main = new DashboardMainActivity();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
        .setSmallIcon(R.drawable.ispeedlogo)
        .setPriority(1)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setContentTitle("Tracking Internet")
        .build();

        startForeground(1, notification);
        sharedpreferences = getSharedPreferences(getString(R.string.USERPREF), Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setSslEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PeriSecure:MyWakeLock");

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            wakeLock.acquire((long) allowed_time * ONE_MINUTE);
        } catch (Exception e) { }

        if (intent.getStringExtra("date") != null) {
            filterDateStr = intent.getStringExtra("date");
        }

        if (intent.getStringExtra("start") != null) {
            filterStartingHour = intent.getStringExtra("start");
        }

        final String dateWStartingHr = filterDateStr+ "-" + filterStartingHour;

        if (intent.getStringExtra("end") != null) {
            filterEndingHour = intent.getStringExtra("end");
        }

        final String dateWEndingHr = filterDateStr+ "-" + filterEndingHour;

        if (intent.getIntExtra("frequency",0) != 0) {
            filterFrequency = intent.getIntExtra("frequency", 0);
        }

        if (intent.getStringExtra("loc") != null) {
            currentLocation = intent.getStringExtra("loc");
        }

        if (intent.hasExtra("lat")) {
            latitude = intent.getDoubleExtra("lat", 0.0);
        }

        if (intent.hasExtra("longi")) {
            longitude = intent.getDoubleExtra("longi", 0.0);
        }

        //0 - Choose here, 1 - 15Mins, 2 - 30Mins, 3 - Hourly
        Log.d(TAG, "onStartCommand: filterDay - " + filterDateStr);
        Log.d(TAG, "onStartCommand: filterStartingHour - " + filterStartingHour);
        Log.d(TAG, "onStartCommand: filterEndingHour - " + filterEndingHour);
        Log.d(TAG, "onStartCommand: filterFrequency - " + filterFrequency);

        if(isStartDatePast(dateWStartingHr)) {
            Toasty.error(getApplicationContext(), "Set date is in the past.", Toast.LENGTH_LONG).show();
        } else {
            //Check if current date is not after the past date
            long frequency = frequencyConverter(filterFrequency);
            SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy-hh:mm a");
            //Create a timer
            try {
                Toasty.info(getApplicationContext(), "Tracking on background...", Toast.LENGTH_LONG).show();
                Date startDate = format.parse(dateWStartingHr);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.d(TAG, "TIMER STARTS....");
                        if (functionMethod.haveNetworkConnected(getApplication())) {
                            isTrackingStarted = true;
                            isTimerStopped = false;
                            trackInternet();
                        } else {
                            Toasty.error(getApplicationContext(), "Tracking Stops. Check internet Connection", Toast.LENGTH_LONG).show();
                            onDestroy();
                        }
                    }
                }, startDate, frequency);
            } catch (IllegalStateException | ParseException e) {
                Log.d(TAG, "Exception: " + e.getMessage());
                onDestroy();
            }

            //Create a killer for the timer
            try {
                Date startDate = format.parse(dateWStartingHr);
                Date endDate = format.parse(dateWEndingHr);

                if(endDate.before(startDate)) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(endDate);
                    c.add(Calendar.DATE, 1);
                    endDate = c.getTime();
                }
                final Handler handler = new Handler();
                TimerTask timertask = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(() -> {
                            try {
                                cancelTimer();
                                //Toasty.error(getApplicationContext(), "Tracking and watcher Stops", Toast.LENGTH_LONG).show();
                                //This toast gets and error "Can't toast on a thread that has not called Looper.prepare()" so comment out for now
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                            }
                        });
                    }
                };
                timerKiller = new Timer();
                timerKiller.schedule(timertask, endDate);
            } catch (ParseException e) {
                Log.d(TAG, "Exception: " + e.getMessage());
                onDestroy();
            }
        }

        return START_STICKY;
    }

    public long frequencyConverter(int freq) {
        //return freq1Min;
        if (freq == 1) {
            return freq15Min;
        }else if(freq == 2) {
            return freq30Min;
        }else {
            return freq1Hr;
        }
    }

    public boolean isStartDatePast(String date) {
        SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy-hh:mm a");
        try {
            Date startDate = format.parse(date);
            Date currDate = new Date();
            if (startDate.before(currDate)) {
                return true;
            } else {
                return false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void cancelTimer() {
        Log.d(TAG, "Killing both timer");

        //Timer
        timer.cancel();
        isTrackingStarted = false;
        isTimerStopped = true;
        editor.putBoolean("isStart", false);

        //Killer timer
        timerKiller.cancel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimer();
        Toasty.error(getApplicationContext(), "Tracking and watcher Stops", Toast.LENGTH_LONG).show();
        wakeLock.release();
    }

    public void trackInternet() {
        Log.d(TAG, "Start tracking....");
        tempBlackList = new HashSet<>();
        dataModel = new InternetDataModel();
        mFirebaseAuth = FirebaseAuth.getInstance();
        //Restart test icin eger baglanti koparsa

        //Restart test icin eger baglanti koparsa
        if (getSpeedTestHostsHandler == null) {
            getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
            getSpeedTestHostsHandler.start();
        }

        //Get egcodes.speedtest hosts
        int timeCount = 600; //1min
        while (!getSpeedTestHostsHandler.isFinished()) {
            timeCount--;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.d(TAG, "run: " + e.getMessage());
            }
            if (timeCount <= 0) {
                getSpeedTestHostsHandler = null;
                return;
            }
        }

        //Find closest server
        HashMap<Integer, String> mapKey = getSpeedTestHostsHandler.getMapKey();
        HashMap<Integer, List<String>> mapValue = getSpeedTestHostsHandler.getMapValue();
        double selfLat = getSpeedTestHostsHandler.getSelfLat();
        double selfLon = getSpeedTestHostsHandler.getSelfLon();
        double tmp = 19349458;
        double dist = 0.0;
        int findServerIndex = 0;

        for (int index : mapKey.keySet()) {
            if (tempBlackList.contains(mapValue.get(index).get(5))) {
                continue;
            }
            Location source = new Location("Source");
            source.setLatitude(selfLat);
            source.setLongitude(selfLon);

            List<String> ls = mapValue.get(index);
            Location dest = new Location("Dest");
            dest.setLatitude(Double.parseDouble(ls.get(0)));
            dest.setLongitude(Double.parseDouble(ls.get(1)));

            double distance = source.distanceTo(dest);
            if (tmp > distance) {
                tmp = distance;
                dist = distance;
                findServerIndex = index;
            }
        }

        String testAddr = mapKey.get(findServerIndex).replace("http://", "https://");
        final List<String> info = mapValue.get(findServerIndex);

        if (info == null) return;

        //Reset value, graphics
        final List<Double> pingRateList = new ArrayList<>();
        final List<Double> downloadRateList = new ArrayList<>();
        final List<Double> uploadRateList = new ArrayList<>();
        Boolean pingTestStarted = false;
        Boolean pingTestFinished = false;
        Boolean downloadTestStarted = false;
        Boolean downloadTestFinished = false;
        Boolean uploadTestStarted = false;
        Boolean uploadTestFinished = false;

        //Init Test
        final PingTest pingTest = new PingTest(info.get(6).replace(":8080", ""), 3);
        final HttpDownloadTest downloadTest = new HttpDownloadTest(testAddr.replace(testAddr.split("/")[testAddr.split("/").length - 1], ""));
        final HttpUploadTest uploadTest = new HttpUploadTest(testAddr);

        //Tests
        while (true) {
            if (!pingTestStarted) {
                pingTest.start();
                pingTestStarted = true;
            }
            if (pingTestFinished && !downloadTestStarted) {
                downloadTest.start();
                downloadTestStarted = true;
            }
            if (downloadTestFinished && !uploadTestStarted) {
                uploadTest.start();
                uploadTestStarted = true;
            }
            Log.d("PING", dec.format(pingTest.getAvgRtt()));
            //Ping Test
            if (pingTestFinished) {
                //Failure
                if (pingTest.getAvgRtt() == 0) {
                    System.out.println("Ping error...");
                } else {
                    //Success
                    dataModel.setPing(dec.format(pingTest.getAvgRtt()));
                    fbDataModel.setPing(dec.format(pingTest.getAvgRtt()));
                }
            } else {
                pingRateList.add(pingTest.getInstantRtt());
                dataModel.setPing(dec.format(pingTest.getAvgRtt()));
                fbDataModel.setPing(dec.format(pingTest.getAvgRtt()));
            }

            //Download Test
            if (pingTestFinished) {
                if (downloadTestFinished) {
                    //Failure
                    if (downloadTest.getFinalDownloadRate() == 0) {
                        System.out.println("Download error...");
                    } else {
                        //Success
                        dataModel.setDownLoadSpeed(dec.format(downloadTest.getFinalDownloadRate()));
                        fbDataModel.setDownLoadSpeed(dec.format(downloadTest.getFinalDownloadRate()));
                    }
                } else {
                    //Calc position
                    double downloadRate = downloadTest.getInstantDownloadRate();
                    downloadRateList.add(downloadRate);
                }
            }

            //Upload Test
            if (downloadTestFinished) {
                if (uploadTestFinished) {
                    //Failure
                    if (uploadTest.getFinalUploadRate() == 0) {
                        System.out.println("Upload error...");
                    } else {
                        //Success
                        dataModel.setUploadSpeed(dec.format(uploadTest.getFinalUploadRate()));
                        fbDataModel.setUploadSpeed(dec.format(uploadTest.getFinalUploadRate()));
                    }
                } else {
                    //Calc position
                    double uploadRate = uploadTest.getInstantUploadRate();
                    uploadRateList.add(uploadRate);

                    dataModel.setUploadSpeed(dec.format(uploadTest.getFinalUploadRate()));
                    fbDataModel.setUploadSpeed(dec.format(uploadTest.getFinalUploadRate()));
                    //Update chart
                }
            }

            if (pingTestFinished && downloadTestFinished && uploadTest.isFinished()) break;

            if (pingTest.isFinished()) {
                pingTestFinished = true;
            }
            if (downloadTest.isFinished()) {
                downloadTestFinished = true;
            }
            if (uploadTest.isFinished()) {
                uploadTestFinished = true;
            }

            if (pingTestStarted && !pingTestFinished) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Log.d(TAG, "run: " + e.getMessage());
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.d(TAG, "run: " + e.getMessage());
                }
            }
        }

        firebaseUser = mFirebaseAuth.getCurrentUser();
        dataModel.setUserName(sharedpreferences.getAll().get(getString(R.string.FIRSTNAME)).toString() + " "
        + sharedpreferences.getAll().get(getString(R.string.LASTNAME)).toString());
        dataModel.setIsp(getSpeedTestHostsHandler.getIspName());
        dataModel.setLocation(currentLocation);
        dataModel.setLatitude(latitude);
        dataModel.setLongitude(longitude);
        dataModel.setTime(null);
        dataModel.setUser_id(firebaseUser.getUid());

        //adding to database
        fbDataModel.setUserName(sharedpreferences.getAll().get(getString(R.string.FIRSTNAME)).toString() + " "
                + sharedpreferences.getAll().get(getString(R.string.LASTNAME)).toString());
        fbDataModel.setIsp(getSpeedTestHostsHandler.getIspName());
        fbDataModel.setLocation(currentLocation);
        fbDataModel.setLatitude(latitude);
        fbDataModel.setLongitude(longitude);
        fbDataModel.setUser_id(firebaseUser.getUid());

        SimpleDateFormat dateFormatWithZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
        String currentDate = dateFormatWithZone.format(new Date());
        fbDataModel.setTime(currentDate);

        Log.e(TAG, "Inserting data to room....");
        iSpeedClient.getInstance(getApplicationContext()).getAppDatabase()
        .trackInternetDao()
        .insert(new TrackInternetModel(CONNECTED.getStatus(), currentDate));

        EventBus.getDefault().post("trigger");
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "my_service_channelid";
        String channelName = "My Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }
}