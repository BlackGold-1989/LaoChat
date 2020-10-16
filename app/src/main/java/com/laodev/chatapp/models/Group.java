package com.laodev.chatapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;

/**
 * Created by a_man on 30-12-2017.
 */

@RealmClass
public class Group implements Parcelable, RealmModel {
    private String id, name, status, image, admin;
    private RealmList<String> userIds;
    private RealmList<String> grpExitUserIds;
    private long date;

    public Group() {
    }

    protected Group(Parcel in) {
        id = in.readString();
        admin = in.readString();
        name = in.readString();
        status = in.readString();
        image = in.readString();
        ArrayList<String> userIdsToParse = in.createStringArrayList();
        userIds = new RealmList<>();
        userIds.addAll(userIdsToParse);
        date = in.readLong();
        ArrayList<String> exitUserIds = in.createStringArrayList();
        grpExitUserIds = new RealmList<>();
        grpExitUserIds.addAll(exitUserIds);
    }

    public static final Creator<Group> CREATOR = new Creator<Group>() {
        @Override
        public Group createFromParcel(Parcel in) {
            return new Group(in);
        }

        @Override
        public Group[] newArray(int size) {
            return new Group[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Group group = (Group) o;

        return id.equals(group.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public RealmList<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(ArrayList<String> userIds) {
        this.userIds = new RealmList<>();
        this.userIds.addAll(userIds);
    }

    public RealmList<String> getGrpExitUserIds() {
        return grpExitUserIds;
    }

    public void setGrpExitUserIds(ArrayList<String> ExitUserIds) {
        this.grpExitUserIds = new RealmList<>();
        grpExitUserIds.addAll(ExitUserIds);
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(admin);
        parcel.writeString(name);
        parcel.writeString(status);
        parcel.writeString(image);
        ArrayList<String> userIdsToParse = new ArrayList<>();
        userIdsToParse.addAll(this.userIds);
        parcel.writeStringList(userIdsToParse);

        parcel.writeLong(date);
        ArrayList<String> exitGrpUserIds = new ArrayList<>();
        if (grpExitUserIds != null) {
            exitGrpUserIds.addAll(this.grpExitUserIds);
            parcel.writeStringList(exitGrpUserIds);
        }
    }

    public static boolean validate(Group group) {
        return group != null && group.getId() != null && group.getName() != null && group.getStatus() != null && group.getUserIds() != null;
    }
}
