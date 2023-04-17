package com.example.ispeed.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.ispeed.Model.DisconnectedModel;
import com.example.ispeed.Model.InternetDataModel;
import com.example.ispeed.Model.TimeAndSpeedModel;
import com.example.ispeed.R;
import com.example.ispeed.adapter.DashBoardDataAdapter;
import com.example.ispeed.local.iSpeedClient;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ViewDashboardContentActivity extends AppCompatActivity {

    BarChart bc_dashboard;
    RecyclerView rv_dashboard;
    String type = "";
    FirebaseFirestore db;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser firebaseUser;
    private static final String TAG = "ViewDashboardContentAct";
    ArrayList<InternetDataModel> mDataModel;

    String filterDay = "";
    String filterStartingHour = "";
    String filterEndingHour = "";

    TextView tv_db_label;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_dashboard_content);
        bc_dashboard = findViewById(R.id.bc_dashboard);
        rv_dashboard = findViewById(R.id.rv_dashboard);
        tv_db_label = findViewById(R.id.tv_db_label);
        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = mFirebaseAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        type = getIntent().getStringExtra("type");

        Log.d(TAG, "onCreate: " + type);

        if (type.equals("download")) {
            getDownloadSpeedData();
        } else if (type.equals("upload")) {
            getUploadSpeed();
        } else if (type.equals("disconnected")) {
            getDisconnectedCount();
        } else {
            getPingSpeed();
        }
    }

    public void getDownloadSpeedData() {
        rv_dashboard.setHasFixedSize(true);
        rv_dashboard.setLayoutManager(new LinearLayoutManager(ViewDashboardContentActivity.this));

        if (!getIntent().getStringExtra("date").equals("") &&
                !getIntent().getStringExtra("start").equals("")  &&
                !getIntent().getStringExtra("end").equals("")){
            filterDay = getIntent().getStringExtra("date");
            filterStartingHour = getIntent().getStringExtra("start");
            filterEndingHour = getIntent().getStringExtra("end");
            String  parsers = filterDay + " " + filterStartingHour + ":00";
            String  parserEnd = filterDay + " " + filterEndingHour + ":00";
            Date dateStart, dateEnd;
            try {
                dateStart = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parsers);
                dateEnd = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parserEnd);
                db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                        .whereEqualTo("user_id",firebaseUser.getUid())
                        .whereGreaterThanOrEqualTo("time",dateStart)
                        .whereLessThanOrEqualTo("time",dateEnd)
                        .orderBy("time", Query.Direction.ASCENDING)
                        .addSnapshotListener((value, error) -> {
                            mDataModel = new ArrayList<>();
                            ArrayList<TimeAndSpeedModel> mTimeAndSpeedModels = new ArrayList<>();
                            if (error != null){
                                Log.d(TAG, "onEvent: " + error.getMessage());
                            }else{
                                for (DocumentSnapshot document : value.getDocuments())
                                {
                                    InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                                    Log.d(TAG, "onEvent: " + dataModel.getDownLoadSpeed());
                                    mDataModel.add(dataModel);

                                }

                                ArrayList<BarEntry> barEntries = new ArrayList<>();
                                ArrayList<String> theTimes = new ArrayList<>();
                                barEntries.add(new BarEntry(0,0));
                                theTimes.add("");
                                Log.d(TAG, "onCreate: " + mDataModel.size());
                                for (int i = 0; i < mDataModel.size(); i++){
                                    Float val = Float.valueOf(mDataModel.get(i).getDownLoadSpeed());

                                    barEntries.add(new BarEntry(val,1+i));

                                    String date = mDataModel.get(i).getTime();
                                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                                    Date newDate = null;
                                    try {
                                        newDate = format.parse(date);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }

                                    format = new SimpleDateFormat("hh:mm a");
                                    String resultDate = format.format(newDate);

                                    theTimes.add(resultDate);
                                    TimeAndSpeedModel dataModel = new TimeAndSpeedModel();
                                    dataModel.setTime(resultDate);
                                    dataModel.setSpeed(String.valueOf(val) + "Mbps");
                                    mTimeAndSpeedModels.add(dataModel);

                                }

                                Date date = new Date();
                                @SuppressLint("SimpleDateFormat")
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                                tv_db_label.setText("Download Speed (" + simpleDateFormat.format(date) + ")" );

                                BarDataSet dataSet = new BarDataSet(barEntries, " Mbps");
                                BarData data = new BarData(theTimes,dataSet );
                                bc_dashboard.setData(data);
                                bc_dashboard.setTouchEnabled(true);
                                bc_dashboard.setDragEnabled(true);
                                bc_dashboard.setScaleEnabled(true);
                                // set the data and list of labels into chart
                                bc_dashboard.setDescription("");  // set the description
                                bc_dashboard.animateY(2000);

                                DashBoardDataAdapter dashBoardDataAdapter = new DashBoardDataAdapter( ViewDashboardContentActivity.this,mTimeAndSpeedModels);
                                rv_dashboard.setAdapter(dashBoardDataAdapter);
                            }


                        });
            }catch (Exception e){
                e.printStackTrace();
            }

        }else{
            Log.d(TAG, "getDownloadSpeedData:here ");
            db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                    .whereEqualTo("user_id",firebaseUser.getUid())
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, FirebaseFirestoreException error) {
                            mDataModel = new ArrayList<>();
                            ArrayList<TimeAndSpeedModel> mTimeAndSpeedModels = new ArrayList<>();
                            for (DocumentSnapshot document : value.getDocuments())
                            {
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

                                if (filterDay.equals("")){
                                    filterDay =  resultDate;
                                }

                                try {
                                    if (filterDay.equals(resultDate)) {
                                        mDataModel.add(dataModel);
                                    }
                                } catch (Exception e) { }
                            }

                            ArrayList<BarEntry> barEntries = new ArrayList<>();
                            ArrayList<String> theTimes = new ArrayList<>();
                            barEntries.add(new BarEntry(0,0));
                            theTimes.add("");
                            Log.d(TAG, "onCreate: " + mDataModel.size());
                            for (int i = 0; i < mDataModel.size(); i++){
                                Float val = Float.valueOf(mDataModel.get(i).getDownLoadSpeed());

                                barEntries.add(new BarEntry(val,1+i));
                                String date = mDataModel.get(i).getTime();
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                                Date newDate = null;
                                try {
                                    newDate = format.parse(date);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                format = new SimpleDateFormat("hh:mm a");
                                String resultDate = format.format(newDate);

                                theTimes.add(resultDate);
                                TimeAndSpeedModel dataModel = new TimeAndSpeedModel();
                                dataModel.setTime(resultDate);
                                dataModel.setSpeed(String.valueOf(val) + "Mbps");
                                mTimeAndSpeedModels.add(dataModel);

                            }

                            Date date = new Date();
                            @SuppressLint("SimpleDateFormat")
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                            tv_db_label.setText("Download Speed (" + simpleDateFormat.format(date) + ")" );

                            BarDataSet dataSet = new BarDataSet(barEntries, " Mbps");
                            BarData data = new BarData(theTimes,dataSet );
                            bc_dashboard.setData(data);
                            bc_dashboard.setTouchEnabled(true);
                            bc_dashboard.setDragEnabled(true);
                            bc_dashboard.setScaleEnabled(true);
                            // set the data and list of labels into chart
                            bc_dashboard.setDescription("");  // set the description
                            bc_dashboard.animateY(2000);

                            DashBoardDataAdapter dashBoardDataAdapter = new DashBoardDataAdapter( ViewDashboardContentActivity.this,mTimeAndSpeedModels);
                            rv_dashboard.setAdapter(dashBoardDataAdapter);

                        }
                    });

        }



    }


    public void getUploadSpeed(){
        rv_dashboard.setHasFixedSize(true);
        rv_dashboard.setLayoutManager(new LinearLayoutManager(ViewDashboardContentActivity.this));

        if (!getIntent().getStringExtra("date").equals("") &&
                !getIntent().getStringExtra("start").equals("")  &&
                !getIntent().getStringExtra("end").equals("")){

            filterDay = getIntent().getStringExtra("date");
            filterStartingHour = getIntent().getStringExtra("start");
            filterEndingHour = getIntent().getStringExtra("end");
            String  parsers = filterDay + " " + filterStartingHour + ":00";
            String  parserEnd = filterDay + " " + filterEndingHour + ":00";
            Date dateStart, dateEnd;

            try {
                dateStart = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parsers);
                dateEnd = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parserEnd);
                db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                    .whereEqualTo("user_id",firebaseUser.getUid())
                    .whereGreaterThanOrEqualTo("time",dateStart)
                    .whereLessThanOrEqualTo("time",dateEnd)
                    .orderBy("time", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {
                        mDataModel = new ArrayList<>();
                        ArrayList<TimeAndSpeedModel> mTimeAndSpeedModels = new ArrayList<>();

                        if (error != null){
                            Log.d(TAG, "onEvent: " + error.getMessage());
                        }else{
                            for (DocumentSnapshot document : value.getDocuments())
                            {
                                InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                                Log.d(TAG, "onEvent: " + dataModel.getDownLoadSpeed());
                                mDataModel.add(dataModel);

                            }
                            ArrayList<BarEntry> barEntriesUS = new ArrayList<>();
                            ArrayList<String> theTimes = new ArrayList<>();
                            barEntriesUS.add(new BarEntry(0,0));
                            theTimes.add("");

                            Log.d(TAG, "onCreate: " + mDataModel.size());
                            for (int i = 0; i < mDataModel.size(); i++){
                                Float valUS = Float.valueOf(mDataModel.get(i).getUploadSpeed());
                                barEntriesUS.add(new BarEntry(valUS,1+i));

                                String date = mDataModel.get(i).getTime();
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                                Date newDate = null;
                                try {
                                    newDate = format.parse(date);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                format = new SimpleDateFormat("hh:mm a");
                                String resultDate = format.format(newDate);

                                theTimes.add(resultDate);
                                TimeAndSpeedModel dataModel = new TimeAndSpeedModel();
                                dataModel.setTime(resultDate);
                                dataModel.setSpeed(String.valueOf(valUS) + "Mbps");
                                mTimeAndSpeedModels.add(dataModel);

                            }

                            Date date = new Date();
                            @SuppressLint("SimpleDateFormat")
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                            tv_db_label.setText("Upload Speed (" + simpleDateFormat.format(date) + ")" );

                            BarDataSet  dataSetUS = new BarDataSet(barEntriesUS, " Mbps");
                            BarData dataUS = new BarData(theTimes,dataSetUS );
                            bc_dashboard.setData(dataUS);
                            bc_dashboard.setTouchEnabled(true);
                            bc_dashboard.setDragEnabled(true);
                            bc_dashboard.setScaleEnabled(true);
                            // set the data and list of labels into chart
                            bc_dashboard.setDescription("");  // set the description
                            bc_dashboard.animateY(2000);

                            DashBoardDataAdapter dashBoardDataAdapter = new DashBoardDataAdapter( ViewDashboardContentActivity.this,mTimeAndSpeedModels);
                            rv_dashboard.setAdapter(dashBoardDataAdapter);
                        }
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
            .whereEqualTo("user_id",firebaseUser.getUid())
            .addSnapshotListener((value, error) -> {
                mDataModel = new ArrayList<>();
                ArrayList<TimeAndSpeedModel> mTimeAndSpeedModels = new ArrayList<>();
                for (DocumentSnapshot document : value.getDocuments())
                {
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

                    if (filterDay.equals("")){
                        filterDay =  resultDate;
                    }

                    try {
                        if (filterDay.equals(resultDate)) {
                            mDataModel.add(dataModel);
                        }
                    } catch (Exception e) { }

                }
                ArrayList<BarEntry> barEntriesUS = new ArrayList<>();
                ArrayList<String> theTimes = new ArrayList<>();
                barEntriesUS.add(new BarEntry(0,0));
                theTimes.add("");

                Log.d(TAG, "onCreate: " + mDataModel.size());
                for (int i = 0; i < mDataModel.size(); i++){
                    Float valUS = Float.valueOf(mDataModel.get(i).getUploadSpeed());
                    barEntriesUS.add(new BarEntry(valUS,1+i));

                    String date = mDataModel.get(i).getTime();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                    Date newDate = null;
                    try {
                        newDate = format.parse(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    format = new SimpleDateFormat("hh:mm a");
                    String resultDate = format.format(newDate);

                    theTimes.add(resultDate);
                    TimeAndSpeedModel dataModel = new TimeAndSpeedModel();
                    dataModel.setTime(resultDate);
                    dataModel.setSpeed(String.valueOf(valUS) + "Mbps");
                    mTimeAndSpeedModels.add(dataModel);

                }

                Date date = new Date();
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                tv_db_label.setText("Upload Speed (" + simpleDateFormat.format(date) + ")" );

                BarDataSet  dataSetUS = new BarDataSet(barEntriesUS, " Mbps");
                BarData dataUS = new BarData(theTimes,dataSetUS );
                bc_dashboard.setData(dataUS);
                bc_dashboard.setTouchEnabled(true);
                bc_dashboard.setDragEnabled(true);
                bc_dashboard.setScaleEnabled(true);
                // set the data and list of labels into chart
                bc_dashboard.setDescription("");  // set the description
                bc_dashboard.animateY(2000);

                DashBoardDataAdapter dashBoardDataAdapter = new DashBoardDataAdapter( ViewDashboardContentActivity.this,mTimeAndSpeedModels);
                rv_dashboard.setAdapter(dashBoardDataAdapter);


            });
        }
    }

    public void getPingSpeed(){
        rv_dashboard.setHasFixedSize(true);
        rv_dashboard.setLayoutManager(new LinearLayoutManager(ViewDashboardContentActivity.this));

        if (!getIntent().getStringExtra("date").equals("") &&
                !getIntent().getStringExtra("start").equals("")  &&
                !getIntent().getStringExtra("end").equals("")) {

            filterDay = getIntent().getStringExtra("date");
            filterStartingHour = getIntent().getStringExtra("start");
            filterEndingHour = getIntent().getStringExtra("end");
            String  parsers = filterDay + " " + filterStartingHour + ":00";
            String  parserEnd = filterDay + " " + filterEndingHour + ":00";
            Date dateStart, dateEnd;

            try {
                dateStart = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parsers);
                dateEnd = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parserEnd);
                db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                .whereEqualTo("user_id",firebaseUser.getUid())
                .whereGreaterThanOrEqualTo("time",dateStart)
                .whereLessThanOrEqualTo("time",dateEnd)
                .orderBy("time", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    mDataModel = new ArrayList<>();
                    ArrayList<TimeAndSpeedModel> mTimeAndSpeedModels = new ArrayList<>();
                    if (error != null) {
                        Log.d(TAG, "onEvent: " + error.getMessage());
                    } else {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                            Log.d(TAG, "onEvent: " + dataModel.getDownLoadSpeed());
                            mDataModel.add(dataModel);
                        }

                        ArrayList<BarEntry> barEntriesPing = new ArrayList<>();
                        ArrayList<String> theTimes = new ArrayList<>();

                        barEntriesPing.add(new BarEntry(0,0));
                        theTimes.add("");

                        Log.d(TAG, "onCreate: " + mDataModel.size());
                        for (int i = 0; i < mDataModel.size(); i++){

                            Float valPing = Float.valueOf(mDataModel.get(i).getPing());

                            barEntriesPing.add(new BarEntry(valPing,1+i));
                            String date = mDataModel.get(i).getTime();
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                            Date newDate = null;
                            try {
                                newDate = format.parse(date);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            format = new SimpleDateFormat("hh:mm a");
                            String resultDate = format.format(newDate);

                            theTimes.add(resultDate);

                            TimeAndSpeedModel dataModel = new TimeAndSpeedModel();
                            dataModel.setTime(resultDate);
                            dataModel.setSpeed((valPing) + " Ms");
                            mTimeAndSpeedModels.add(dataModel);
                        }

                        Date date =new Date();
                        @SuppressLint("SimpleDateFormat")
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                        tv_db_label.setText("Ping Speed (" + simpleDateFormat.format(date) + ")" );

                        BarDataSet  dataSetPing = new BarDataSet(barEntriesPing, "Ms");
                        BarData dataPing= new BarData(theTimes,dataSetPing );
                        bc_dashboard.setData(dataPing);
                        bc_dashboard.setTouchEnabled(true);
                        bc_dashboard.setDragEnabled(true);
                        bc_dashboard.setScaleEnabled(true);
                        // set the data and list of labels into chart
                        bc_dashboard.setDescription("");  // set the description
                        bc_dashboard.animateY(2000);

                        DashBoardDataAdapter dashBoardDataAdapter = new DashBoardDataAdapter( ViewDashboardContentActivity.this,mTimeAndSpeedModels);
                        rv_dashboard.setAdapter(dashBoardDataAdapter);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
            .whereEqualTo("user_id",firebaseUser.getUid())
            .addSnapshotListener((value, error) -> {
                mDataModel = new ArrayList<>();
                ArrayList<TimeAndSpeedModel> mTimeAndSpeedModels = new ArrayList<>();
                for (DocumentSnapshot document : value.getDocuments())
                {
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

                    if (filterDay.equals("")){
                        filterDay =  resultDate;
                    }

                    try {
                        if (filterDay.equals(resultDate)) {
                            mDataModel.add(dataModel);
                        }
                    } catch (Exception e) { }
                }

                ArrayList<BarEntry> barEntriesPing = new ArrayList<>();

                ArrayList<String> theTimes = new ArrayList<>();
                barEntriesPing.add(new BarEntry(0,0));
                theTimes.add("");

                Log.d(TAG, "onCreate: " + mDataModel.size());
                for (int i = 0; i < mDataModel.size(); i++){

                    Float valPing = Float.valueOf(mDataModel.get(i).getPing());

                    barEntriesPing.add(new BarEntry(valPing,1+i));
                    String date = mDataModel.get(i).getTime();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                    Date newDate = null;
                    try {
                        newDate = format.parse(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    format = new SimpleDateFormat("hh:mm a");
                    String resultDate = format.format(newDate);

                    theTimes.add(resultDate);


                    TimeAndSpeedModel dataModel = new TimeAndSpeedModel();
                    dataModel.setTime(resultDate);
                    dataModel.setSpeed((valPing) + " Ms");
                    mTimeAndSpeedModels.add(dataModel);
                }

                Date date = new Date();
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                tv_db_label.setText("Ping Speed (" + simpleDateFormat.format(date) + ")" );

                BarDataSet  dataSetPing = new BarDataSet(barEntriesPing, "Ms");
                BarData dataPing= new BarData(theTimes,dataSetPing );
                bc_dashboard.setData(dataPing);
                bc_dashboard.setTouchEnabled(true);
                bc_dashboard.setDragEnabled(true);
                bc_dashboard.setScaleEnabled(true);
                // set the data and list of labels into chart
                bc_dashboard.setDescription("");  // set the description
                bc_dashboard.animateY(2000);

                DashBoardDataAdapter dashBoardDataAdapter = new DashBoardDataAdapter( ViewDashboardContentActivity.this,mTimeAndSpeedModels);
                rv_dashboard.setAdapter(dashBoardDataAdapter);

            });
        }
    }

    private void getDisconnectedCount() {
        try {
            ArrayList<BarEntry> barEntriesDisconnect = new ArrayList<>();
            ArrayList<String> theTimes = new ArrayList<>();
            theTimes.add("");

            ArrayList<TimeAndSpeedModel> mTimeAndSpeedModels = new ArrayList<>();

            class GetTasks extends AsyncTask<Void, Void, List<DisconnectedModel>> {

                @Override
                protected List<DisconnectedModel> doInBackground(Void... voids) {
                    List<DisconnectedModel> taskList = iSpeedClient
                            .getInstance(getApplicationContext())
                            .getAppDatabase()
                            .disconnectedDao()
                            .getDisconnectedCount();
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

                        Date date = new Date();
                        @SuppressLint("SimpleDateFormat")
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                        tv_db_label.setText("Disconnected Count (" + simpleDateFormat.format(date) + ")" );

                        BarDataSet  dataSetPing = new BarDataSet(barEntriesDisconnect, "Ms");
                        BarData dataPing= new BarData(theTimes,dataSetPing );
                        bc_dashboard.setData(dataPing);
                        bc_dashboard.setTouchEnabled(true);
                        bc_dashboard.setDragEnabled(true);
                        bc_dashboard.setScaleEnabled(true);
                        // set the data and list of labels into chart
                        bc_dashboard.setDescription("");  // set the description
                        bc_dashboard.animateY(2000);

                        DashBoardDataAdapter dashBoardDataAdapter = new DashBoardDataAdapter( ViewDashboardContentActivity.this,mTimeAndSpeedModels);
                        rv_dashboard.setAdapter(dashBoardDataAdapter);
                    } catch (Exception e) { }
                }
            }

            GetTasks gt = new GetTasks();
            gt.execute();
        } catch (Exception e) {
            Log.d("GetException", e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}