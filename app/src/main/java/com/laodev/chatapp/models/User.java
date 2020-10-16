package com.laodev.chatapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.RealmClass;

/**
 * Created by a_man on 5/4/2017.
 */

@RealmClass
public class User implements Parcelable, RealmModel {
    @Ignore
    private boolean online;
    @Exclude
    @Ignore
    private String nameInPhone;
    @Ignore
    private boolean typing;

    @Ignore
    @Exclude
    private boolean selected;

    private String id, name, status, image;

    private String wallpaper;

    private long timestamp;

    public RealmList<solochat> solochat = new RealmList<>();
    private RealmList<String> blockedUsersIds = new RealmList<>();

    public User() {
    }

    protected User(Parcel in) {
        online = in.readByte() != 0;
        nameInPhone = in.readString();
        typing = in.readByte() != 0;
        selected = in.readByte() != 0;
        id = in.readString();
        name = in.readString();
        status = in.readString();
        image = in.readString();
        wallpaper = in.readString();
        timestamp = in.readLong();
        ArrayList<String> blockedUsersIds = in.createStringArrayList();
        this.blockedUsersIds = new RealmList<>();
        this.blockedUsersIds.addAll(blockedUsersIds);
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public User(String id, String name, String status, String image) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.online = false;
        this.image = image;
        this.typing = false;
    }

    public User(String id, String name, String status, String image, String wallpaper) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.online = false;
        this.image = image;
        this.typing = false;
        this.wallpaper = wallpaper;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return id.equals(user.id);
    }

    public String getNameInPhone() {
        return nameInPhone;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }

    public void setNameInPhone(String nameInPhone) {
        this.nameInPhone = nameInPhone;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOnline() {
        return online;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public String getNameToDisplay() {
        return (this.nameInPhone != null) ? this.nameInPhone : this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(String wallpaper) {
        this.wallpaper = wallpaper;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timestamp = timeStamp;
    }

    public RealmList<solochat> getSolochat() {
        return solochat;
    }

    public void setSolochat(ArrayList<solochat> solochat) {
        this.solochat = new RealmList<>();
        this.solochat.addAll(solochat);
    }

    public RealmList<String> getBlockedUsersIds() {
        return blockedUsersIds;
    }

    public void setBlockedUsersIds(ArrayList<String> blockedUsersIds) {
        this.blockedUsersIds.addAll(blockedUsersIds);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (online ? 1 : 0));
        parcel.writeString(nameInPhone);
        parcel.writeByte((byte) (typing ? 1 : 0));
        parcel.writeByte((byte) (selected ? 1 : 0));
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(status);
        parcel.writeString(image);
        parcel.writeString(wallpaper);
        parcel.writeLong(timestamp);
        ArrayList<String> blockedIds = new ArrayList<>();
        if (this.blockedUsersIds != null) {
            blockedIds.addAll(this.blockedUsersIds);
            parcel.writeStringList(blockedIds);
        }
    }

    public static boolean validate(User user) {
        return user != null && user.getId() != null && user.getName() != null && user.getStatus() != null;
    }

    public boolean isFoundNewUser(String s) {
        return s.length() > 8 && id.contains(s);
    }

}