package com.example.ispeed.View.Dialog;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.ispeed.BuildConfig;
import com.example.ispeed.Model.InternetDataModel;
import com.example.ispeed.R;
import com.example.ispeed.View.ExportActivity;
import com.example.ispeed.View.HomeActivity;
import com.example.ispeed.View.SignUpActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import es.dmoral.toasty.Toasty;

public class ViewSaveData extends AppCompatDialogFragment {
    FirebaseFirestore db;
    TextView tv_time,tv_loc,tv_ping,tv_download,tv_upload,tv_isp,tv_stablitiy;
    Button btn_ok,btn_cancel;

    InternetDataModel dataModel;

    private static final String TAG = "ViewUserInfoDialog";
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_save_data_dialog,null);

        btn_ok = view.findViewById(R.id.btn_ok);
        btn_cancel = view.findViewById(R.id.btn_cancel);
        tv_time = view.findViewById(R.id.tv_time);
        tv_loc = view.findViewById(R.id.tv_loc);
        tv_ping = view.findViewById(R.id.tv_ping);
        tv_download = view.findViewById(R.id.tv_download);
        tv_upload = view.findViewById(R.id.tv_upload);
        tv_isp = view.findViewById(R.id.tv_isp);
        tv_stablitiy = view.findViewById(R.id.tv_stablitiy);
        db = FirebaseFirestore.getInstance();
        builder.setView(view);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa");

        tv_time.setText( "Time Recorded: " +formatter.format(date));
        tv_loc.setText("Location: " + dataModel.getLocation());
        tv_ping.setText( "Ping: " + dataModel.getPing() + "Ms");
        tv_download.setText("Download: " + dataModel.getDownLoadSpeed() + "Mbps");
        tv_upload.setText("Upload: " + dataModel.getUploadSpeed() + "Mbps");
        tv_isp.setText("ISP: " + dataModel.getIsp());
        try {
            if(Double.parseDouble(dataModel.getPing()) >= 20){
                tv_stablitiy.setText("Stability: Stable");
                dataModel.setStability(true);
            }else{
                tv_stablitiy.setText("Stability: Unstable");
                dataModel.setStability(false);

            }
        } catch (NumberFormatException nfe) {
            // Handle the condition when str is not a number.
        }

        btn_ok.setOnClickListener(view1 -> db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                .add(dataModel)
                .addOnSuccessListener(documentReference -> {
                    captureScreen(getDialog().getWindow().getDecorView().getRootView());
                    Toasty.success(getActivity(), "Saved successfully", Toast.LENGTH_LONG).show();

                    startActivity(new Intent(getActivity(), HomeActivity.class));
                    dismiss();
                }));
        btn_cancel.setOnClickListener(view12 -> dismiss());
        return builder.create();
    }

    public ViewSaveData( InternetDataModel dataModel ) {
        this.dataModel = dataModel;
    }

    public File captureScreen(View view) {
        Date date = new Date();

        SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy");
        CharSequence sequence = formatter.format(date);
        try {

            String dirPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
            File fileDir = new File(dirPath);

            if (!fileDir.exists()) {
                boolean mkDir = fileDir.mkdir();
            }

            String path = dirPath + "/" + "SCREEN"
                    + System.currentTimeMillis() + ".png";


            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());

            view.setDrawingCacheEnabled(false);

            File imageFile = new File(path);

            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);

            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

//            openScreenshot(imageFile);


            Toasty.info(getActivity(),
                    "Check Exported Image on your gallery.", Toast.LENGTH_LONG)
                    .show();
            Log.d(TAG, "captureScreen: " + imageFile.toString());
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
