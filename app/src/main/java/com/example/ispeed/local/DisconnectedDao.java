package com.example.ispeed.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.ispeed.Model.DisconnectedModel;
import java.util.List;

@Dao
public interface DisconnectedDao {

    @Query("SELECT * FROM disconnected")
    List<DisconnectedModel> getDisconnectedCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DisconnectedModel disconnectedModel);

    @Query("UPDATE disconnected SET disconnectedCount= :count WHERE date =:date")
    int updateDisconnectCount(String count, String date);

    @Query("SELECT disconnectedCount FROM disconnected WHERE date =:date")
    String getSpecificCount(String date);

    @Query("SELECT * FROM disconnected WHERE date = :date")
    int isDataExist(String date);
}
