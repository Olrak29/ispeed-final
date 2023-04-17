package com.example.ispeed.View.Dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.example.ispeed.R;
import java.util.Calendar;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener{

    private final static int TIME_PICKER_INTERVAL = 5;
    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        return new TimePickerDialog(getActivity(), (timePicker, hourOfDay, minute1) -> {
            TextView tv = getActivity().findViewById(R.id.tv_startinghour);
            boolean isPM = (hourOfDay >= 12);
            tv.setText(String.format("%02d:%02d %s", (hourOfDay == 12 || hourOfDay == 0) ? 12 : hourOfDay % 12, minute1, isPM ? "PM" : "AM"));
        }, hour, minute,false);
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) { }


}
