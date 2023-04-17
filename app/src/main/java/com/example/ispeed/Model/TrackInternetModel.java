package com.example.ispeed.Model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "trackInternet", indices = @Index(value = {"trackDate"}, unique = true))
public class TrackInternetModel implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "trackStatus")
    private String trackStatus;

    @ColumnInfo(name = "trackDate")
    private String trackDate;

    public TrackInternetModel(String trackStatus, String trackDate) {
        this.trackStatus = trackStatus;
        this.trackDate = trackDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTrackStatus() {
        return trackStatus;
    }

    public void setTrackStatus(String trackStatus) {
        this.trackStatus = trackStatus;
    }

    public String getTrackDate() {
        return trackDate;
    }

    public void setTrackDate(String trackDate) {
        this.trackDate = trackDate;
    }

    public enum TrackStatusEnum {

        CONNECTED("Connected"),
        DISCONNECTED("Disconnected");

        private String status;

        TrackStatusEnum(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}