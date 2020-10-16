package com.laodev.chatapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;

@RealmClass
public class AttachmentList implements Parcelable, RealmModel {
    private String name, data;
    private RealmList<StatusImageList> urlList;
    private long bytesCount;

    public AttachmentList() {
    }

    protected AttachmentList(Parcel in) {
        name = in.readString();
        data = in.readString();
        bytesCount = in.readLong();
    }

    public static final Creator<AttachmentList> CREATOR = new Creator<AttachmentList>() {
        @Override
        public AttachmentList createFromParcel(Parcel in) {
            return new AttachmentList(in);
        }

        @Override
        public AttachmentList[] newArray(int size) {
            return new AttachmentList[size];
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

    public RealmList<StatusImageList> getUrlList() {
        return urlList;
    }

    public void setUrlList(RealmList<StatusImageList> urlList) {
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

