package com.example.ispeed.View.automatic;

import static com.example.ispeed.Model.TrackInternetModel.TrackStatusEnum.CONNECTED;
import static com.example.ispeed.Model.TrackInternetModel.TrackStatusEnum.DISCONNECTED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ispeed.Model.FirebaseInternetDataModel;
import com.example.ispeed.Model.TrackInternetModel;
import com.example.ispeed.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrackStatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<TrackInternetModel> dataList;
    private Context context;
    private int viewStatusItem = 0;
    private int viewDetailsItem = 1;
    SharedPreferences sharedpreferences;

    public TrackStatusAdapter(List<TrackInternetModel> dataList, Context context) {
        this.dataList = dataList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if(viewType == viewStatusItem){
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_internet_list_item, parent, false);
            return new ViewStatusHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.share_activity_header, parent, false);
            return new ViewStatusHeader(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final TrackInternetModel data = dataList.get(position);

        if(holder instanceof ViewStatusHolder){
            ViewStatusHolder vsholder = (ViewStatusHolder) holder;
            vsholder.trackTime.setText(convertDateWord(data.getTrackDate()));
            if (data.getTrackStatus().equals(CONNECTED.getStatus())) {
                vsholder.trackStatus.setTextColor(ContextCompat.getColor(context,R.color.lightGreen));
            } else {
                vsholder.trackStatus.setTextColor(ContextCompat.getColor(context,R.color.lightRed));
            }
            vsholder.trackStatus.setText(data.getTrackStatus());
        } else {
            ViewStatusHeader vsheader = (ViewStatusHeader) holder;
            sharedpreferences = context.getSharedPreferences(context.getString(R.string.USERPREF), Context.MODE_PRIVATE);
            vsheader.nameVal.setText(sharedpreferences.getAll().get(context.getString(R.string.FIRSTNAME)).toString() + " " + sharedpreferences.getAll().get(context.getString(R.string.LASTNAME)).toString());
            //Get date today

            String currDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
            vsheader.dateVal.setText(currDate);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
          return viewDetailsItem;
        } else {
          return viewStatusItem;
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class ViewStatusHolder extends RecyclerView.ViewHolder {
        TextView trackTime;
        TextView trackStatus;
        public ViewStatusHolder(@NonNull View itemView) {
            super(itemView);
            trackTime = itemView.findViewById(R.id.track_time);
            trackStatus = itemView.findViewById(R.id.track_status);
        }
    }

    public class ViewStatusHeader extends RecyclerView.ViewHolder {
        TextView nameVal;
        TextView dateVal;
        public ViewStatusHeader (@NonNull View itemView) {
            super(itemView);
            nameVal = itemView.findViewById(R.id.nameVal);
            dateVal = itemView.findViewById(R.id.dateVal);
        }
    }

    private String convertDateWord(String trackDate) {
        try {
            // Get date from string
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            Date date = null;
            try {
                date = dateFormatter.parse(trackDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // Get time from date
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a");
            return timeFormatter.format(date);
        } catch (Exception e) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        }
    }
}