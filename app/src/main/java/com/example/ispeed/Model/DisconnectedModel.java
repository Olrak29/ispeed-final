package com.example.ispeed.Model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "disconnected", indices = @Index(value = {"date"}, unique = true))
public class DisconnectedModel implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "disconnectedCount")
    private String disconnectedCount;

    @ColumnInfo(name = "date")
    private String date;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisconnectedCount() {
        return disconnectedCount;
    }

    public void setDisconnectedCount(String disconnectedCount) {
        this.disconnectedCount = disconnectedCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}