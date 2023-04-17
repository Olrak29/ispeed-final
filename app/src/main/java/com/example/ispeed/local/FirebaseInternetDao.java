package com.example.ispeed.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.ispeed.Model.FirebaseInternetDataModel;
import java.util.List;

@Dao
public interface FirebaseInternetDao {
    @Query("SELECT * FROM internetDataModel")
    List<FirebaseInternetDataModel> getFBInternetCount();


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FirebaseInternetDataModel fbInternetModel);

}
