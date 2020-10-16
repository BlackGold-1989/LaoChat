package com.laodev.chatapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.RealmClass;

public class StatusNewArrayList implements Parcelable {

    private StatusNewArrayList(Parcel in) {
        lastMessage = in.readString();
        myId = in.readString();
        userId = in.readString();
        groupId = in.readString();
        timeUpdated = in.readLong();
        user = in.readParcelable(User.class.getClassLoader());
        group = in.readParcelable(Group.class.getClassLoader());
        read = in.readByte() != 0;
        selected = in.readByte() != 0;
    }

    public static final Creator<StatusNewArrayList> CREATOR = new Creator<StatusNewArrayList>() {
        @Override
        public StatusNewArrayList createFromParcel(Parcel in) {
            return new StatusNewArrayList(in);
        }

        @Override
        public StatusNewArrayList[] newArray(int size) {
            return new StatusNewArrayList[size];
        }
    };

    public ArrayList<StatusImageNewArrayList> getStatusImages() {
        return statusImages;
    }

    public void setStatusImages(ArrayList<StatusImageNewArrayList> statusImages) {
        this.statusImages = statusImages;
    }

    private ArrayList<StatusImageNewArrayList> statusImages;
    private String lastMessage, myId, userId, groupId;
    private long timeUpdated;
    private User user;
    private Group group;
    private boolean read;

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    @Ignore
    private boolean selected;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public StatusNewArrayList() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getMyId() {
        return myId;
    }

    public void setMyId(String myId) {
        this.myId = myId;
    }

    public long getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(long timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(lastMessage);
        parcel.writeString(myId);
        parcel.writeString(userId);
        parcel.writeString(groupId);
    }

    public static boolean validate(StatusNewArrayList status) {
        return status != null && status.getMyId() != null && status.getStatusImages() != null;
    }
}