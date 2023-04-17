package com.example.ispeed.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ispeed.BuildConfig;
import com.example.ispeed.FunctionMethod.FunctionMethod;
import com.example.ispeed.Model.DisconnectedModel;
import com.example.ispeed.Model.InternetDataModel;
import com.example.ispeed.Model.SignUpModel;
import com.example.ispeed.Model.TimeAndSpeedModel;
import com.example.ispeed.R;
import com.example.ispeed.adapter.DashBoardDataAdapter;
import com.example.ispeed.local.iSpeedClient;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import es.dmoral.toasty.Toasty;

public class ExportActivity extends AppCompatActivity {
    BarChart bc_downloadSpeed,bc_ping,bc_uploadSpeed, bc_disconnected;
    ArrayList<InternetDataModel> mDataModel;
    FirebaseFirestore db;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser firebaseUser;
    TextView tv_ds_label,tv_us_label,tv_ping_label,tv_export_name,tv_export_email,tv_export_date, tv_disconnected_label;
    CardView cv_uploadSpeed,cv_downloadSpeed,cv_ping, cv_disconnected;
    private static final String TAG = "DashboardMainActivity";
    Button btn_settings,btn_export;
    String filterDay = "";

    String filterStartingHour = "";
    String filterEndingHour = "";

    FunctionMethod method;
    AppCompatButton shareButton;

    Bitmap bitmap;
    ScrollView contentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = mFirebaseAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        bc_ping = findViewById(R.id.bc_ping);
        bc_uploadSpeed = findViewById(R.id.bc_uploadSpeed);
        bc_downloadSpeed = findViewById(R.id.bc_downloadSpeed);
        bc_disconnected = findViewById(R.id.bc_disconnected);

        tv_ds_label = findViewById(R.id.tv_ds_label);
        tv_ping_label = findViewById(R.id.tv_ping_label);
        tv_us_label = findViewById(R.id.tv_us_label);
        tv_export_name = findViewById(R.id.tv_export_name);
        tv_export_email = findViewById(R.id.tv_export_email);
        tv_export_date = findViewById(R.id.tv_export_date);
        tv_disconnected_label = findViewById(R.id.tv_disconnected_label);

        btn_settings = findViewById(R.id.btn_settings);
        btn_export = findViewById(R.id.btn_export);

        cv_downloadSpeed = findViewById(R.id.cv_downloadSpeed);
        cv_ping = findViewById(R.id.cv_ping);
        cv_uploadSpeed = findViewById(R.id.cv_uploadSpeed);
        cv_disconnected = findViewById(R.id.cv_disconnected);

        shareButton = findViewById(R.id.buttonShare);
        contentView = findViewById(R.id.contentView);

        method = new FunctionMethod();
        method.takeScreenShot(this);
        verifyStoragePermission(this);

        getUserInfo();

        if (!getIntent().getStringExtra("date").equals("") &&
                !getIntent().getStringExtra("start") .equals("") &&
                !getIntent().getStringExtra("end") .equals("")){
            filterDay = getIntent().getStringExtra("date");
            filterStartingHour = getIntent().getStringExtra("start");
            filterEndingHour = getIntent().getStringExtra("end");
            getDataWithFilters();
        } else getData();

        new Handler().postDelayed(() -> captureScreen(), 3000);

        setupOnClicked();
    }

    private void setupOnClicked() {
        shareButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, getImageUri(this, bitmap));
            startActivity(Intent.createChooser(intent, "Share"));
        });
    }

    public void getData(){
        db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                .whereEqualTo("user_id",firebaseUser.getUid())
                .addSnapshotListener((value, error) -> {
                    mDataModel = new ArrayList<>();
                    for (DocumentSnapshot document : value.getDocuments())
                    {
                        InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                        try {
                            if (filterDay.equals("")) filterDay = formatter.format(new Date());
                            if (filterDay.equals(formatter.format(dataModel.getTime())))
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

                    for (int i = 0; i < mDataModel.size(); i++){
                        Float val = Float.valueOf(mDataModel.get(i).getDownLoadSpeed());
                        Float valUS = Float.valueOf(mDataModel.get(i).getUploadSpeed());
                        Float valPing = Float.valueOf(mDataModel.get(i).getPing());

                        barEntries.add(new BarEntry(val,1+i));
                        barEntriesUS.add(new BarEntry(valUS,1+i));
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

                    }

                    Date date = new Date();
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                    tv_export_date.setText("Date: "+ simpleDateFormat.format(date));
                    tv_ds_label.setText("Download Speed (" + simpleDateFormat.format(date) + ")" );
                    tv_ping_label.setText("Ping (" + simpleDateFormat.format(date) + ")" );
                    tv_us_label.setText("Upload Speed (" + simpleDateFormat.format(date) + ")" );


                    BarDataSet dataSet = new BarDataSet(barEntries, "Mbps");
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

                    getDisconnectedCount();
                });
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
                        tv_disconnected_label.setText("Disconnected Count (" + simpleDateFormat.format(date) + ")" );

                        BarDataSet  dataSetDisconnected = new BarDataSet(barEntriesDisconnect, "Count");
                        BarData dataDisconnected= new BarData(theTimes,dataSetDisconnected );
                        bc_disconnected.setData(dataDisconnected);
                        bc_disconnected.setTouchEnabled(true);
                        bc_disconnected.setDragEnabled(true);
                        bc_disconnected.setScaleEnabled(true);
                        // set the data and list of labels into chart
                        bc_disconnected.setDescription("");  // set the description
                        bc_disconnected.animateY(2000);
                    } catch (Exception e) { }
                }
            }

            GetTasks gt = new GetTasks();
            gt.execute();
        } catch (Exception e) {
            Log.d("GetException", e.getMessage());
        }
    }

    public void getDataWithFilters(){
        Timestamp timestamp;
        Date dateStart, dateEnd;
        String  parsers = filterDay + " " + filterStartingHour + ":00";
        String  parserEnd = filterDay + " " + filterEndingHour + ":00";
        try {
            dateStart = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parsers);
            dateEnd = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parserEnd);
            db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
            .whereEqualTo("user_id",firebaseUser.getUid())
            .whereGreaterThanOrEqualTo("time",dateStart)
            .whereLessThanOrEqualTo("time",dateEnd)
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable  QuerySnapshot value, FirebaseFirestoreException error) {
                    mDataModel = new ArrayList<>();

                    if (error != null){
                        Log.d(TAG, "onEvent: " + error.getMessage());
                    }else{
                        for (DocumentSnapshot document : value.getDocuments())
                        {
                            InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

                            if (filterDay.equals("")){
                                filterDay =  formatter.format(new Date());

                            }


                            Log.d(TAG, "onEvent: " + filterDay );
                            Log.d(TAG, "onEvent: " + formatter.format(dataModel.getTime()));
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

                        for (int i = 0; i < mDataModel.size(); i++){
                            Float val = Float.valueOf(mDataModel.get(i).getDownLoadSpeed());
                            Float valUS = Float.valueOf(mDataModel.get(i).getUploadSpeed());
                            Float valPing = Float.valueOf(mDataModel.get(i).getPing());

                            barEntries.add(new BarEntry(val,1+i));
                            barEntriesUS.add(new BarEntry(valUS,1+i));
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

                        }

                        Date date = new Date();
                        @SuppressLint("SimpleDateFormat")
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");

                        tv_export_date.setText("Date: " + simpleDateFormat.format(date));
                        tv_ds_label.setText("Download Speed (" + simpleDateFormat.format(date) + ")" );
                        tv_ping_label.setText("Ping (" + simpleDateFormat.format(date) + ")" );
                        tv_us_label.setText("Upload Speed (" + simpleDateFormat.format(date) + ")" );
                        tv_disconnected_label.setText("Disconnected Count (" + simpleDateFormat.format(date) + ")" );


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

                        getDisconnectedCount();
                    }
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void getUserInfo(){
        db.collection(getString(R.string.COLLECTION_USER_INFORMATION))
        .whereEqualTo("user_id", firebaseUser.getUid())
        .get()
        .addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                SignUpModel signUpModel = querySnapshot.getDocuments().get(0).toObject(SignUpModel.class);

                tv_export_name.setText("Name: " + signUpModel.getFirstname() + " " + signUpModel.getLastname());
                tv_export_email.setText("Email: " +  firebaseUser.getEmail());
            }
        });
    }

    public File captureScreen() {
        try {
            String dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));

            String path = dirPath + "/" + "SCREEN" + System.currentTimeMillis() + ".png";
            bitmap = getBitmapFromView(contentView, contentView.getChildAt(0).getHeight(), contentView.getChildAt(0).getWidth());

            File imageFile = new File(path);

            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);

            int quality = 100;

            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            Toasty.info(this, "Check Exported Image on your gallery.", Toast.LENGTH_LONG).show();

            if (bitmap != null) {
                shareButton.setVisibility(View.VISIBLE);
            }

            Log.d(TAG, "captureScreen: " + imageFile);
            return imageFile;


        } catch (FileNotFoundException e) {
            e.printStackTrace();

            Log.d(TAG, "captureScreen: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "captureScreen: " + e.getMessage());
        }
        return  null;
    }

    private Bitmap getBitmapFromView(View view, int height, int width) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return bitmap;
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "iSpeed", null);
        return Uri.parse(path);
    }

    private void openScreenshot(File imageFile) {
        Uri uri=
        FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                BuildConfig.APPLICATION_ID + ".provider", imageFile);
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_VIEW);
//
//        Uri uri=
//                FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
//                        BuildConfig.APPLICATION_ID + ".provider", imageFile);
////        Uri uri = Uri.fromFile(imageFile);
//        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

//    // verifying if storage permission is given or not
//    public static void verifystoragepermissions(Activity activity) {
//
//        int permissions = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//
//        // If storage permission is not given then request for External Storage Permission
//        if (permissions != PackageManager.PERMISSION_GRANTED) {
//
//        }
//    }

    private static final int  REQUEST_EXTERNAL_STORAGE = 1;

    private String[] PERMISION_STORAGE = {

        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    public void verifyStoragePermission(Activity activity){
        int permission = ActivityCompat.checkSelfPermission(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity,
            PERMISION_STORAGE,
            REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}