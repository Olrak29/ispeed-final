package com.example.ispeed.service;

import static com.example.ispeed.Model.TrackInternetModel.TrackStatusEnum.DISCONNECTED;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.ispeed.GetSpeedTestHostsHandler;
import com.example.ispeed.Model.DisconnectedModel;
import com.example.ispeed.Model.TrackInternetModel;
import com.example.ispeed.R;
import com.example.ispeed.local.iSpeedClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import es.dmoral.toasty.Toasty;

public class NetworkChangeReceiver extends BroadcastReceiver {

    Context context;
    TrackInternetModel trackInternetModel;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onReceive(Context context, Intent intent) {
        try {
            //Init Components
            setupComponents(context);

            if (!isOnline(context)) {
                Date date = new Date();
                SimpleDateFormat dateFormatWithZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
                String currentDate = dateFormatWithZone.format(date);

                trackInternetModel.setTrackDate(currentDate);
                trackInternetModel.setTrackStatus(DISCONNECTED.getStatus());
                saveTrackDisconnect(context, trackInternetModel);
                createNotification(context, "You have been disconnected to the internet.");
            } else {
                createNotification(context, "Internet has been restored, you can start tracking your internet.");
                Toasty.info(context, "Internet has been restored, you can start tracking your internet.", Toast.LENGTH_LONG).show();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setupComponents(Context context) {
        this.context = context;
        this.trackInternetModel = new TrackInternetModel("", "");
    }

    private void saveTrackDisconnect(Context context, TrackInternetModel trackInternetModel) {
        try {
            @SuppressLint("StaticFieldLeak")
            class SaveTrackDisconnect extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        //adding to database
                        iSpeedClient.getInstance(context).getAppDatabase()
                        .trackInternetDao()
                        .insert(trackInternetModel);

                        EventBus.getDefault().post("trigger");
                    } catch (Exception e) { }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                }
            }
            SaveTrackDisconnect st = new SaveTrackDisconnect();
            st.execute();
        } catch (Exception e) { }
    }

    private boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            //should check null because in airplane mode it will be null
            return (netInfo != null && netInfo.isConnected());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotification(Context context, String text) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        NotificationChannel channel = new NotificationChannel("CHANNEL_ID", "name", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("description");
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "CHANNEL_ID")
                .setSmallIcon(R.drawable.ispeedlogo)
                .setPriority(1)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(text);
        notificationManager.notify(1, builder.build());
    }
}
