package com.example.ispeed.service;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.ispeed.FunctionMethod.FunctionMethod;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import com.example.ispeed.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class AutoMaticTrackingActivity extends AppCompatActivity {

    String currentLocation;
    private static final int REQUEST_LOCATION = 1;

    FunctionMethod functionMethod;
    GoogleApiClient googleApiClient;
    FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    Button btn_startTracking,btn_stopTracking;
    private static final String TAG = "AutoMaticTrackingActivi";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_matic_tracking);

        btn_stopTracking = findViewById(R.id.btn_stopTracking);
        btn_startTracking = findViewById(R.id.btn_startTracking);

        functionMethod = new FunctionMethod();
        if(googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {  }

                @Override
                public void onConnectionSuspended(int i) {
                    googleApiClient.connect();
                }
            })
            .addOnConnectionFailedListener(connectionResult -> { }).build();
            googleApiClient.connect();
        }

        db = FirebaseFirestore.getInstance();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        btn_startTracking.setOnClickListener(view -> {
            Intent startServiceIntent = new Intent(AutoMaticTrackingActivity.this, MyService.class);
            startServiceIntent.putExtra("loc",currentLocation);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startServiceIntent);
            } else {
                startService(startServiceIntent);
            }
        });

        btn_stopTracking.setOnClickListener(view -> {
            Intent stopServiceIntent = new Intent(AutoMaticTrackingActivity.this, MyService.class);
            stopServiceIntent.putExtra("loc",currentLocation);
            stopService(stopServiceIntent);
        });
    }
}