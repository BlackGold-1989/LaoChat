package com.laodev.chatapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;


public class AttachmentArrayList implements Parcelable {
    private String name, data;
    private ArrayList<StatusImageList> urlList;
    private long bytesCount;

    public AttachmentArrayList() {
    }

    private AttachmentArrayList(Parcel in) {
        name = in.readString();
        data = in.readString();
        bytesCount = in.readLong();
    }

    public static final Creator<AttachmentArrayList> CREATOR = new Creator<AttachmentArrayList>() {
        @Override
        public AttachmentArrayList createFromParcel(Parcel in) {
            return new AttachmentArrayList(in);
        }

        @Override
        public AttachmentArrayList[] newArray(int size) {
            return new AttachmentArrayList[size];
        }
    };

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public ArrayList<StatusImageList> getUrlList() {
        return urlList;
    }

    public void setUrlList(ArrayList<StatusImageList> urlList) {
        this.urlList = urlList;
    }

    public long getBytesCount() {
        return bytesCount;
    }

    public void setBytesCount(long bytesCount) {
        this.bytesCount = bytesCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(data);
        dest.writeList(urlList);
        dest.writeLong(bytesCount);
    }
}

