package com.laodev.chatapp.models;

import io.realm.RealmModel;
import io.realm.annotations.RealmClass;

@RealmClass
public class LogCall implements RealmModel {
    private User user;
    private long timeUpdated;
    private int timeDurationSeconds;
    private String status, myId,userId;
    private boolean isVideo;

    public LogCall() {
    }

    public LogCall(User user, long timeUpdated, int timeDurationSeconds, boolean isVideo, String status, String myId, String userId) {
        this.user = user;
        this.timeUpdated = timeUpdated;
        this.timeDurationSeconds = timeDurationSeconds;
        this.isVideo = isVideo;
        this.status = status;
        this.myId = myId;
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public long getTimeUpdated() {
        return timeUpdated;
    }

    public int getTimeDuration() {
        return timeDurationSeconds;
    }

    public int getTimeDurationSeconds() {
        return timeDurationSeconds;
    }

    public String getMyId() {
        return myId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public String getStatus() {
        return status;
    }
}
