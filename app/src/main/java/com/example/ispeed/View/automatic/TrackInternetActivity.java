package com.example.ispeed.View.automatic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ispeed.Model.TrackInternetModel;
import com.example.ispeed.R;
import com.example.ispeed.View.Dialog.CustomTimePickerDialog;
import com.example.ispeed.databinding.ActivityTrackInternetBinding;
import com.example.ispeed.local.iSpeedClient;
import com.example.ispeed.service.MyService;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import es.dmoral.toasty.Toasty;

public class TrackInternetActivity extends AppCompatActivity {

    private ActivityTrackInternetBinding binding;
    private static final String TAG = "TrackInternetActivity";
    private TrackStatusAdapter adapter;
    private List<TrackInternetModel> trackList = new ArrayList<>();
    private int year, month, day;

    private int selectedPosition;
    private final String[] arraySpinner = new String[] {
        "Choose here", "15 Mins", "30 Mins", "Hour"
    };

    //Location Components
    private Double latitude;
    private Double longitude;
    private String currentLocation;
    private GoogleApiClient googleApiClient;
    private static final int REQUEST_LOCATION = 1;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String eventStr) {
        getTrackInternetItems();
    };

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBinding();
    }

    private void setupBinding() {
        binding = ActivityTrackInternetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);

        //Setup Components
        setupSpinner();
        setupGoogleClient();
        setupObserveLocation();
        setupOnClickedListener();
        setupTrackInternetList();

        //Setup Domain Components
        getTrackInternetItems();
    }

    private void setupOnClickedListener() {
        binding.etGetDate.setOnClickListener(v -> showDateDialog(binding.etGetDate));
        binding.tvStartinghour.setOnClickListener(v -> showTimeDialog(binding.tvStartinghour));
        binding.tvEndinghour.setOnClickListener(v -> showTimeDialog(binding.tvEndinghour));
        binding.btnStartTracking.setOnClickListener(v -> startBackgroundTracking());
        binding.btnStopTracking.setOnClickListener(v -> stopBackgroundTracking());
        binding.btnExport.setOnClickListener(v -> {
            captureScreen();
        });
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_spinner_list, arraySpinner);
        adapter.setDropDownViewResource(R.layout.item_spinner_list);
        binding.spnrFrequency.setAdapter(adapter);
        binding.spnrFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                selectedPosition = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
    }

    private void setupGoogleClient() {
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
    }

    private void setupObserveLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
        .addOnSuccessListener(this, location -> {
            // Get last known location. In some rare situations this can be null.
            if (location != null) {
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
    }

    private void setupTrackInternetList() {
        binding.internetStatusList.setLayoutManager(new LinearLayoutManager(this));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getTrackInternetItems() {

        List<TrackInternetModel> top10Status = new ArrayList<TrackInternetModel>();
        trackList = iSpeedClient
        .getInstance(getApplicationContext())
        .getAppDatabase()
        .trackInternetDao()
        .getTrackInternetData();

        trackList.add(0, null);
        top10Status = trackList;

        Log.e(TAG, "TRACKLIST: " + trackList.size());

        if(trackList.size() > 10){
            top10Status = trackList.stream().limit(11).collect(Collectors.toList());
        }
        adapter = new TrackStatusAdapter(top10Status, this);
        binding.internetStatusList.setAdapter(adapter);
    }

    private void startBackgroundTracking() {
        //date, latitude, longitude, starthr, endhr, frequency
        Intent intent = new Intent(this, MyService.class);
        intent.putExtra("lat", latitude);
        intent.putExtra("longi", longitude);
        intent.putExtra("loc", currentLocation);
        intent.putExtra("frequency", selectedPosition);
        intent.putExtra("date", binding.etGetDate.getText().toString());
        intent.putExtra("end", binding.tvEndinghour.getText().toString());
        intent.putExtra("start", binding.tvStartinghour.getText().toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private void stopBackgroundTracking() {
        Intent stopServiceIntent = new Intent(this, MyService.class);
        stopServiceIntent.putExtra("loc", currentLocation);
        stopService(stopServiceIntent);
    }

    private void showDateDialog(TextView textView) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            month = month + 1;
            String date =  day + "/" + month +"/"+ year;
            try {
                textView.setText(convertDateWord(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimeDialog(TextView timeText) {
        @SuppressLint("DefaultLocale") CustomTimePickerDialog timePickerDialog = new CustomTimePickerDialog(this, (view, hourOfDay, minute) -> {
            boolean isPM = (hourOfDay >= 12);
            timeText.setText(String.format("%02d:%02d %s", (hourOfDay == 12 || hourOfDay == 0) ? 12 : hourOfDay % 12, minute, isPM ? "PM" : "AM"));
        }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                CustomTimePickerDialog.getRoundedMinute(Calendar.getInstance().get(Calendar.MINUTE) + CustomTimePickerDialog.TIME_PICKER_INTERVAL),
                false);
        timePickerDialog.show();
    }

    @SuppressLint("SimpleDateFormat")
    private String convertDateWord(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyy");
        Date newDate = format.parse(date);

        format = new SimpleDateFormat("MMMM dd, yyyy");
        return format.format(newDate);
    }

    public enum TimerEnum {
        START,
        END
    }

    public File captureScreen() {
        try {
            Bitmap bitmap;
            String dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));

            String path = dirPath + "/" + "SCREEN" + System.currentTimeMillis() + ".png";
            bitmap = getScreenshotFromRecyclerView(binding.internetStatusList); //getBitmapFromView(contentView, contentView.getChildAt(0).getHeight(), contentView.getChildAt(0).getWidth());

            File imageFile = new File(path);

            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);

            int quality = 100;

            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            Toasty.info(this, "Check Exported Image on your gallery.", Toast.LENGTH_LONG).show();
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


    public Bitmap getScreenshotFromRecyclerView(RecyclerView view) {
        RecyclerView.Adapter adapter = view.getAdapter();
        Bitmap bigBitmap = null;
        if (adapter != null) {
            int size = adapter.getItemCount();
            int height = 0;
            Paint paint = new Paint();
            int iHeight = 0;
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;
            LruCache<String, Bitmap> bitmaCache = new LruCache<>(cacheSize);
            for (int i = 0; i < size; i++) {
                RecyclerView.ViewHolder holder = adapter.createViewHolder(view, adapter.getItemViewType(i));
                adapter.onBindViewHolder(holder, i);
                holder.itemView.measure(View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                holder.itemView.layout(0, 0, holder.itemView.getMeasuredWidth(), holder.itemView.getMeasuredHeight());
                holder.itemView.setDrawingCacheEnabled(true);
                holder.itemView.buildDrawingCache();
                Bitmap drawingCache = holder.itemView.getDrawingCache();
                if (drawingCache != null) {

                    bitmaCache.put(String.valueOf(i), drawingCache);
                }
//                holder.itemView.setDrawingCacheEnabled(false);
//                holder.itemView.destroyDrawingCache();
                height += holder.itemView.getMeasuredHeight();
            }

            bigBitmap = Bitmap.createBitmap(view.getMeasuredWidth(), height, Bitmap.Config.ARGB_8888);
            Canvas bigCanvas = new Canvas(bigBitmap);
            bigCanvas.drawColor(Color.WHITE);

            for (int i = 0; i < size; i++) {
                Bitmap bitmap = bitmaCache.get(String.valueOf(i));
                bigCanvas.drawBitmap(bitmap, 0f, iHeight, paint);
                iHeight += bitmap.getHeight();
                bitmap.recycle();
            }
        }
        return bigBitmap;
    }
    private void storeImage(Bitmap image) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = timeStamp + ".jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.TITLE, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put(MediaStore.Images.Media.DESCRIPTION, "captured image");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                final String relativeLocation = Environment.DIRECTORY_DCIM + File.separator + "Camera" + File.separator;
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation);
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            } else {
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                File file = new File(directory, "Camera/" + fileName);
                values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                image.compress(Bitmap.CompressFormat.JPEG, 100, output);

            }
            image.recycle();
            Toast.makeText(this,"Image saved!",Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            image.recycle();
            Toast.makeText(this,"Error occurred when saving.",Toast.LENGTH_SHORT).show();
        }
    }

    public static Bitmap combineBitmaps(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp2.getWidth(), bmp1.getHeight()+ bmp2.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, 0,0, null);
        canvas.drawBitmap(bmp2, 0, bmp1.getHeight(), null);
        if (bmp1 != null && !bmp1.isRecycled()) {
            bmp1.recycle();
        }
        if (bmp2 != null && !bmp2.isRecycled()) {
            bmp2.recycle();
        }
        return bmOverlay;
    }

    public void askForPermissions(Bitmap image) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
            storeImage(image);
        }
    }
}