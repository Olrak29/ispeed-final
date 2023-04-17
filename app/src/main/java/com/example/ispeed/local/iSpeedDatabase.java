package com.example.ispeed.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.ispeed.Model.DisconnectedModel;
import com.example.ispeed.Model.FirebaseInternetDataModel;
import com.example.ispeed.Model.TrackInternetModel;

@Database(entities = {DisconnectedModel.class, FirebaseInternetDataModel.class, TrackInternetModel.class}, version = 2, exportSchema = false)
public abstract class iSpeedDatabase  extends RoomDatabase {
    public abstract DisconnectedDao disconnectedDao();
    public abstract FirebaseInternetDao firebaseInternetDao();
    public abstract TrackInternetDao trackInternetDao();
}