package com.laodev.chatapp.models;

import io.realm.RealmModel;
import io.realm.annotations.RealmClass;

@RealmClass
public class solochat implements RealmModel {

    private String phoneNo;

    private long timeStamp;

    public solochat() {
    }

    public solochat(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

}