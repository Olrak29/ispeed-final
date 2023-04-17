package com.example.ispeed.View.Dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.example.ispeed.R;
import java.util.Calendar;

public class TimePickerFragmentEnding extends DialogFragment implements TimePickerDialog.OnTimeSetListener{

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        return new TimePickerDialog(getActivity(), (timePicker, hourOfDay, minutes) -> {
            TextView tv = getActivity().findViewById(R.id.tv_endinghour);
            boolean isPM = (hourOfDay >= 12);
            tv.setText(String.format("%02d:%02d %s", (hourOfDay == 12 || hourOfDay == 0) ? 12 : hourOfDay % 12, minutes, isPM ? "PM" : "AM"));
        }, hour, minute,false);
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) { }
}
