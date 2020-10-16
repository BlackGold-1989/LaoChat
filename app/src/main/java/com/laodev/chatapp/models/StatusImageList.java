package com.laodev.chatapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.RealmModel;

import io.realm.annotations.RealmClass;

@RealmClass
public class StatusImageList implements Parcelable, RealmModel {

    private String url;
    private long uploadTime;
    private boolean expiry;

    public StatusImageList() {

    }

    private StatusImageList(Parcel in) {
        url = in.readString();
        uploadTime = in.readLong();
        expiry = in.readByte() != 0;
    }

    public static final Creator<StatusImageList> CREATOR = new Creator<StatusImageList>() {
        @Override
        public StatusImageList createFromParcel(Parcel in) {
            return new StatusImageList(in);
        }

        @Override
        public StatusImageList[] newArray(int size) {
            return new StatusImageList[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeLong(uploadTime);
        dest.writeByte((byte) (expiry ? 1 : 0));
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(long uploadTime) {
        this.uploadTime = uploadTime;
    }

    public boolean isExpiry() {
        return expiry;
    }

    public void setExpiry(boolean expiry) {
        this.expiry = expiry;
    }
}
