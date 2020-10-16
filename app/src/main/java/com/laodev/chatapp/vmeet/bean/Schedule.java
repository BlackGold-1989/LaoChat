package com.laodev.chatapp.vmeet.bean;

import java.io.Serializable;

public class Schedule implements Serializable {

    String id;
    String title;
    String date;
    String startTime;
    String duration;
    String userId;
    String meeetingId;

    public Schedule() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getMeeetingId() {
        return meeetingId;
    }

    public void setMeeetingId(String meeetingId) {
        this.meeetingId = meeetingId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
