package com.example.ispeed.local;

import android.content.Context;

import androidx.room.Room;

public class iSpeedClient {
    private final Context mCtx;
    private static iSpeedClient mInstance;

    //our app database object
    private iSpeedDatabase appDatabase;

    private iSpeedClient(Context mCtx) {
        this.mCtx = mCtx;

        //creating the app database with Room database builder
        //MyToDos is the name of the database
        appDatabase = Room.databaseBuilder(mCtx, iSpeedDatabase.class, "iSpeed")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
    }

    public static synchronized iSpeedClient getInstance(Context mCtx) {
        if (mInstance == null) {
            mInstance = new iSpeedClient(mCtx);
        }
        return mInstance;
    }

    public iSpeedDatabase getAppDatabase() {
        return appDatabase;
    }
}
