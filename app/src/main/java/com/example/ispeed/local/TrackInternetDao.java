package com.example.ispeed.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.ispeed.Model.TrackInternetModel;
import java.util.List;

@Dao
public interface TrackInternetDao {

    @Query("SELECT * FROM trackInternet ORDER BY trackDate desc")
    List<TrackInternetModel> getTrackInternetData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TrackInternetModel trackInternetModel);
}
