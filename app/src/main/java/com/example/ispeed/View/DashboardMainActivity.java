package com.example.ispeed.View;

import static com.example.ispeed.service.MyService.isTimerStopped;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ispeed.FunctionMethod.FunctionMethod;
import com.example.ispeed.Model.DisconnectedModel;
import com.example.ispeed.Model.FirebaseInternetDataModel;
import com.example.ispeed.Model.InternetDataModel;
import com.example.ispeed.R;
import com.example.ispeed.local.iSpeedClient;
import com.example.ispeed.service.MyService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import es.dmoral.toasty.Toasty;

public class DashboardMainActivity extends AppCompatActivity {
    BarChart bc_downloadSpeed,bc_ping,bc_uploadSpeed, bc_disconnected;
    ArrayList<InternetDataModel> mDataModel;
    FirebaseFirestore db;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser firebaseUser;
    TextView tv_ds_label,tv_us_label,tv_ping_label, tv_disconnected_label;
    CardView cv_uploadSpeed,cv_downloadSpeed,cv_ping, cv_disconnected;
    private static final String TAG = "DashboardMainActivity";
    Button btn_settings,btn_export;

    String filterDay = "";
    String filterStartingHour = "";
    String filterEndingHour = "";
    int filterFrequency = 0;

    Button btn_startTracking,btn_stopTracking;

    String currentLocation;
    Double latitude;
    Double longitude;

    FunctionMethod functionMethod;
    GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION = 1;
    Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_main);

        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = mFirebaseAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        bc_ping = findViewById(R.id.bc_ping);
        bc_disconnected = findViewById(R.id.bc_disconnected);
        bc_uploadSpeed = findViewById(R.id.bc_uploadSpeed);
        bc_downloadSpeed = findViewById(R.id.bc_downloadSpeed);
        tv_ds_label = findViewById(R.id.tv_ds_label);
        tv_ping_label = findViewById(R.id.tv_ping_label);
        tv_disconnected_label = findViewById(R.id.tv_disconnected_label);
        tv_us_label = findViewById(R.id.tv_us_label);
        btn_settings = findViewById(R.id.btn_settings);
        btn_export = findViewById(R.id.btn_export);

        cv_downloadSpeed = findViewById(R.id.cv_downloadSpeed);
        cv_ping = findViewById(R.id.cv_ping);
        cv_uploadSpeed = findViewById(R.id.cv_uploadSpeed);
        cv_disconnected = findViewById(R.id.cv_disconnected);

        btn_startTracking = findViewById(R.id.btn_startTracking);
        btn_stopTracking = findViewById(R.id.btn_stopTracking);

        functionMethod = new FunctionMethod();
        if(googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) { }
                    @Override
                    public void onConnectionSuspended(int i) {
                        googleApiClient.connect();
                    }
                })
                .addOnConnectionFailedListener(connectionResult -> { }).build();
            googleApiClient.connect();
        }
        db = FirebaseFirestore.getInstance();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
        .addOnSuccessListener(this, location -> {
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                Log.d(TAG, "onSuccess: " + location.getLatitude());
                try {
                    if (ActivityCompat.checkSelfPermission(
                            getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions( (Activity) getApplicationContext() , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                    } else {
                        double lat = location.getLatitude();
                        double longi = location.getLongitude();

                        Geocoder geo = new Geocoder(getBaseContext(), Locale.getDefault());
                        List<Address> addresses = geo.getFromLocation(lat, longi, 1);

                        currentLocation = addresses.get(0).getLocality();
                        latitude = lat;
                        longitude = longi;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        btn_startTracking.setOnClickListener(view -> {
            //date, latitude, longitude, starthr, endhr, frequency
            Intent startServiceIntent = new Intent(this, MyService.class);
            startServiceIntent.putExtra("loc", currentLocation);
            startServiceIntent.putExtra("lat", latitude);
            startServiceIntent.putExtra("longi", longitude);
            startServiceIntent.putExtra("date", filterDay);
            startServiceIntent.putExtra("start", filterStartingHour);
            startServiceIntent.putExtra("end", filterEndingHour);
            startServiceIntent.putExtra("frequency", filterFrequency);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startServiceIntent);
            } else {
                startService(startServiceIntent);
            }
        });

        btn_stopTracking.setOnClickListener(view -> {
            Intent stopServiceIntent = new Intent(this, MyService.class);
            stopServiceIntent.putExtra("loc",currentLocation);
            stopService(stopServiceIntent);
        });

        btn_settings.setOnClickListener(view -> {
            Intent serviceIntent = new Intent(this, SettingsActivity.class);
            serviceIntent.putExtra("date", filterDay);
            serviceIntent.putExtra("start", filterStartingHour);
            serviceIntent.putExtra("end", filterEndingHour);
            serviceIntent.putExtra("frequency", filterFrequency);
            startActivity(serviceIntent);
        });

        btn_export.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ExportActivity.class);
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);
        });

        bc_downloadSpeed.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ViewDashboardContentActivity.class);
            intent.putExtra("type", "download");
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);
        });


        bc_uploadSpeed.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ViewDashboardContentActivity.class);
            intent.putExtra("type", "upload");
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);

        });

        bc_ping.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ViewDashboardContentActivity.class);
            intent.putExtra("type", "ping");
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);
        });

        bc_disconnected.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ViewDashboardContentActivity.class);
            intent.putExtra("type", "disconnected");
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);
        });
//        DashboardInterface dashboardInterface = new DashboardInterface();
        dataWatcher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((getIntent().getStringExtra("date") !=null &&
                getIntent().getStringExtra("start") !=null &&
                getIntent().getStringExtra("end") !=null)
        ){
            filterDay = getIntent().getStringExtra("date");
            filterStartingHour = getIntent().getStringExtra("start");
            filterEndingHour = getIntent().getStringExtra("end");
            filterFrequency = getIntent().getIntExtra("frequency", 0);

            Log.d(TAG, "filterDay: " + filterDay);
            Log.d(TAG, "filterStartingHour: " + filterStartingHour);
            Log.d(TAG, "filterEndingHour: " + filterEndingHour);
            Log.d(TAG, "filterFrequency: " + filterFrequency);
            getDataWithFilters();
        } else {
            getData();
        }
    }

    public void getData(){
        if (!isTimerStopped) {
            try {
                ArrayList<BarEntry> barEntries = new ArrayList<>();
                ArrayList<BarEntry> barEntriesUS = new ArrayList<>();
                ArrayList<BarEntry> barEntriesPing = new ArrayList<>();

                ArrayList<String> theTimes = new ArrayList<>();
                barEntriesUS.add(new BarEntry(0,0));
                barEntries.add(new BarEntry(0,0));
                barEntriesPing.add(new BarEntry(0,0));
                theTimes.add("");

                class GetTasks extends AsyncTask<Void, Void, List<FirebaseInternetDataModel>> {

                    @Override
                    protected List<FirebaseInternetDataModel> doInBackground(Void... voids) {
                        List<FirebaseInternetDataModel> taskList = null;
                        try {
                            taskList= iSpeedClient
                                    .getInstance(getApplicationContext())
                                    .getAppDatabase()
                                    .firebaseInternetDao()
                                    .getFBInternetCount();
                        } catch (Exception e) {
                            Log.e("ERROR", e.getMessage());
                        }
                        return taskList;
                    }

                    @Override
                    protected void onPostExecute(List<FirebaseInternetDataModel> result) {
                        super.onPostExecute(result);

                        try {
                            for (int i = 0; i < result.size(); i++) {
                                String uploadSpeed = result.get(i).getUploadSpeed();
                                String downloadSpeed = result.get(i).getDownLoadSpeed();
                                String ping = result.get(i).getPing();


                                Float val = Float.valueOf(downloadSpeed);
                                Float valUS = Float.valueOf(uploadSpeed);
                                Float valPing = Float.valueOf(ping);

                                barEntries.add(new BarEntry(val, 1 + i));
                                barEntriesUS.add(new BarEntry(valUS, 1 + i));
                                barEntriesPing.add(new BarEntry(valPing, 1 + i));
                                String date = result.get(i).getTime();

                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                                Date newDate = format.parse(date);

                                format = new SimpleDateFormat("hh:mm a");
                                String resultDate = format.format(newDate);

                                theTimes.add(resultDate);
                            }

                            @SuppressLint("SimpleDateFormat")
                            String date = new SimpleDateFormat("MMMM dd, yyyy").format(new Date());

                            //Download Speed Chart
                            setChartData(tv_ds_label, bc_downloadSpeed, barEntries, theTimes, "Mbps", "Download Speed (" + date + ")" );

                            //Upload Speed Chart
                            setChartData(tv_us_label, bc_uploadSpeed, barEntriesUS, theTimes, "Mbps", "Upload Speed (" + date + ")" );

                            //Ping Chart
                            setChartData(tv_ping_label, bc_ping, barEntriesPing, theTimes, "Ms", "Ping (" + date + ")" );

                        } catch (Exception e) {
                            Log.e("ERROR", e.getMessage());
                        }
                    }
                }

                GetTasks gt = new GetTasks();
                gt.execute();
            } catch (Exception e) {
                Log.d("GetException", e.getMessage());
            }
        } else {
            db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
            .whereEqualTo("user_id",firebaseUser.getUid())
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener((value, error) -> {
                mDataModel = new ArrayList<>();
                if (error != null) {
                    Log.d(TAG, "onEvent: " + error.getMessage());
                } else {
                    for (DocumentSnapshot document : value.getDocuments()) {
                        InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                        Date newDate = null;
                        try {
                            newDate = format.parse(dataModel.getTime());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        format = new SimpleDateFormat("dd/MM/yyyy");
                        String resultDate = format.format(newDate);

                        if (filterDay.equals("")) filterDay = resultDate;

                        try {
                            if (filterDay.equals(resultDate))
                                mDataModel.add(dataModel);
                        } catch (Exception e) { }
                    }

                    ArrayList<BarEntry> barEntries = new ArrayList<>();
                    ArrayList<BarEntry> barEntriesUS = new ArrayList<>();
                    ArrayList<BarEntry> barEntriesPing = new ArrayList<>();

                    ArrayList<String> theTimes = new ArrayList<>();
                    barEntriesUS.add(new BarEntry(0,0));
                    barEntries.add(new BarEntry(0,0));
                    barEntriesPing.add(new BarEntry(0,0));
                    theTimes.add("");

                    try {
                        List<FirebaseInternetDataModel> taskList = iSpeedClient
                                .getInstance(getApplicationContext())
                                .getAppDatabase()
                                .firebaseInternetDao()
                                .getFBInternetCount();

                        for (int i = 0; i < taskList.size(); i++) {
                            String uploadSpeed = taskList.get(i).getUploadSpeed();
                            String downloadSpeed = taskList.get(i).getDownLoadSpeed();
                            String ping = taskList.get(i).getPing();

                            Float val = Float.valueOf(downloadSpeed);
                            Float valUS = Float.valueOf(uploadSpeed);
                            Float valPing = Float.valueOf(ping);

                            barEntries.add(new BarEntry(val, 1 + i));
                            barEntriesUS.add(new BarEntry(valUS, 1 + i));
                            barEntriesPing.add(new BarEntry(valPing, 1 + i));
                            String date = taskList.get(i).getTime();

                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                            Date newDate = format.parse(date);

                            format = new SimpleDateFormat("hh:mm a");
                            String resultDate = format.format(newDate);

                            theTimes.add(resultDate);
                        }
                    } catch (Exception e) { }

                    @SuppressLint("SimpleDateFormat")
                    String date = new SimpleDateFormat("MMMM dd, yyyy").format(new Date());

                    //Download Speed Chart
                    setChartData(tv_ds_label, bc_downloadSpeed, barEntries, theTimes, "Mbps", "Download Speed (" + date + ")" );

                    //Upload Speed Chart
                    setChartData(tv_us_label, bc_uploadSpeed, barEntriesUS, theTimes, "Mbps", "Upload Speed (" + date + ")" );

                    //Ping Chart
                    setChartData(tv_ping_label, bc_ping, barEntriesPing, theTimes, "Ms", "Ping (" + date + ")" );
                }
            });
        }

        //Disconnected Chart
        getDisconnectedCount();
    }

    private void getDisconnectedCount() {
        try {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");

            ArrayList<BarEntry> barEntriesDisconnect = new ArrayList<>();
            ArrayList<String> theTimes = new ArrayList<>();
            theTimes.add("");

            class GetTasks extends AsyncTask<Void, Void, List<DisconnectedModel>> {

                @Override
                protected List<DisconnectedModel> doInBackground(Void... voids) {
                    List<DisconnectedModel> taskList = new ArrayList<>();
                    try {
                        taskList = iSpeedClient
                                .getInstance(getApplicationContext())
                                .getAppDatabase()
                                .disconnectedDao()
                                .getDisconnectedCount();
                    } catch (Exception e) { }
                    return taskList;
                }

                @Override
                protected void onPostExecute(List<DisconnectedModel> result) {
                    super.onPostExecute(result);

                    try {
                        for (int i = 0; i < result.size(); i++) {
                            Float val = Float.valueOf(result.get(i).getDisconnectedCount());

                            barEntriesDisconnect.add(new BarEntry(val, 1 + i));
                            String date = result.get(i).getDate();
                            @SuppressLint("SimpleDateFormat")
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                            Date objDate = dateFormat.parse(date);
                            //Expected date format
                            SimpleDateFormat dateFormat2 = new SimpleDateFormat("HH:mm aa");

                            String finalDate = dateFormat2.format(objDate);
                            theTimes.add(finalDate);
                        }

                        //Disconnected Chart
                        setChartData(tv_disconnected_label, bc_disconnected, barEntriesDisconnect, theTimes, "Count", "Disconnected Count (" + simpleDateFormat.format(date) + ")");
                    } catch (Exception e) { }
                }
            }

            GetTasks gt = new GetTasks();
            gt.execute();
        } catch (Exception e) {
            Log.d("GetException", e.getMessage());
        }
    }

    private void setChartData(
        TextView tvLabel,
        BarChart barChart,
        ArrayList<BarEntry> barEntries,
        ArrayList<String> theTimes, String label,
        String labelText
    ) {
        // set the data and list of labels into chart
        BarDataSet dataSet = new BarDataSet(barEntries, label);
        BarData data = new BarData(theTimes, dataSet);
        barChart.setData(data);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        // set the data and list of labels into chart
        barChart.setDescription("");  // set the description
        barChart.animateY(2000);

        tvLabel.setText(labelText);
    }

    public void getDataWithFilters(){
        try {

            Date dateStart = null, dateEnd = null;
            String  parsers = filterDay + " " + filterStartingHour + ":00";
            String  parserEnd = filterDay + " " + filterEndingHour + ":00";

            try {
                dateStart = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parsers);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                dateEnd = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parserEnd);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                    .whereEqualTo("user_id",firebaseUser.getUid())
                    .whereGreaterThanOrEqualTo("time",dateStart)
                    .whereLessThanOrEqualTo("time",dateEnd)
                    .orderBy("time", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {
                        mDataModel = new ArrayList<>();

                        if (error != null) {
                            Log.d(TAG, "onEvent: " + error.getMessage());
                        } else {
                            for (DocumentSnapshot document : value.getDocuments()) {
                                InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                                Date newDate = null;
                                try {
                                    newDate = format.parse(dataModel.getTime());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                format = new SimpleDateFormat("dd/MM/yyyy");
                                String resultDate = format.format(newDate);

                                if (filterDay.equals("")) filterDay = resultDate;
                                Log.d(TAG, "onEvent: " + filterDay );
                                Log.d(TAG, "onEvent: " + resultDate);
                                mDataModel.add(dataModel);
                            }

                            ArrayList<BarEntry> barEntries = new ArrayList<>();
                            ArrayList<BarEntry> barEntriesUS = new ArrayList<>();
                            ArrayList<BarEntry> barEntriesPing = new ArrayList<>();

                            ArrayList<String> theTimes = new ArrayList<>();
                            barEntriesUS.add(new BarEntry(0,0));
                            barEntries.add(new BarEntry(0,0));
                            barEntriesPing.add(new BarEntry(0,0));
                            theTimes.add("");

                            List<FirebaseInternetDataModel> taskList = iSpeedClient
                                    .getInstance(getApplicationContext())
                                    .getAppDatabase()
                                    .firebaseInternetDao()
                                    .getFBInternetCount();

                            for (int i = 0; i < taskList.size(); i++){

                                String uploadSpeed = taskList.get(i).getUploadSpeed();
                                String downloadSpeed = taskList.get(i).getDownLoadSpeed();
                                String ping = taskList.get(i).getPing();

                                Float val = Float.valueOf(downloadSpeed);
                                Float valUS = Float.valueOf(uploadSpeed);
                                Float valPing = Float.valueOf(ping);

                                barEntries.add(new BarEntry(val,1+i));
                                barEntriesUS.add(new BarEntry(valUS,1+i));
                                barEntriesPing.add(new BarEntry(valPing,1+i));
                                String date = taskList.get(i).getTime();

                                @SuppressLint("SimpleDateFormat")
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                                Date objDate = null;
                                try {
                                    objDate = dateFormat.parse(date);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                //Expected date format
                                SimpleDateFormat dateFormat2 = new SimpleDateFormat("HH:mm aa");

                                String finalDate = dateFormat2.format(objDate);

                                theTimes.add(finalDate);
                            }

                            try {
                                String date = mDataModel.get(0).getTime();
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                                Date newDate = null;
                                try {
                                    newDate = format.parse(date);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                format = new SimpleDateFormat("MMMM dd, yyyy");
                                String resultDate = format.format(newDate);

                                tv_ds_label.setText("Download Speed (" + resultDate + ")");
                                tv_ping_label.setText("Ping (" + resultDate + ")");
                                tv_us_label.setText("Upload Speed (" + resultDate + ")");
                            } catch (Exception e) { }

                            BarDataSet  dataSet = new BarDataSet(barEntries, "Mbps");
                            BarData data = new BarData(theTimes,dataSet );
                            bc_downloadSpeed.setData(data);
                            bc_downloadSpeed.setTouchEnabled(true);
                            bc_downloadSpeed.setDragEnabled(true);
                            bc_downloadSpeed.setScaleEnabled(true);
                            // set the data and list of labels into chart
                            bc_downloadSpeed.setDescription("");  // set the description
                            bc_downloadSpeed.animateY(2000);

                            BarDataSet  dataSetUS = new BarDataSet(barEntriesUS, "Mbps");
                            BarData dataUS = new BarData(theTimes,dataSetUS );
                            bc_uploadSpeed.setData(dataUS);
                            bc_uploadSpeed.setTouchEnabled(true);
                            bc_uploadSpeed.setDragEnabled(true);
                            bc_uploadSpeed.setScaleEnabled(true);
                            // set the data and list of labels into chart
                            bc_uploadSpeed.setDescription("");  // set the description
                            bc_uploadSpeed.animateY(2000);

                            BarDataSet  dataSetPing = new BarDataSet(barEntriesPing, "Ms");
                            BarData dataPing= new BarData(theTimes,dataSetPing );
                            bc_ping.setData(dataPing);
                            bc_ping.setTouchEnabled(true);
                            bc_ping.setDragEnabled(true);
                            bc_ping.setScaleEnabled(true);
                            // set the data and list of labels into chart
                            bc_ping.setDescription("");  // set the description
                            bc_ping.animateY(2000);
                        }
                    });

            //Disconnected Chart
            getDisconnectedCount();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity( new Intent(getBaseContext(),HomeActivity.class));
    }

    public void dataWatcher() {
        try {
            Log.d("WATCHER", "HEHE" + new Date().toString());
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d("WATCHER", "START WATCHING...");
                    if (functionMethod.haveNetworkConnected(getApplication())) {
                        getData();
                    } else {
                        Toasty.error(getApplicationContext(), "Tracking Stops. Check internet Connection", Toast.LENGTH_LONG).show();
                        onDestroy();
                    }
                }
            }, new Date(), 30000);
        } catch (Exception e) { }
    }

    @Override
    protected void onDestroy() {
        timer.cancel();
        super.onDestroy();
    }

    //    public void onRefresh() {
//        Log.d(TAG, "FIRING ONREFRESH");
//        getData();
//    }
}