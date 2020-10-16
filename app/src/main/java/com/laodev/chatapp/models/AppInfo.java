package com.laodev.chatapp.models;

import java.io.Serializable;

import io.realm.annotations.RealmClass;


public class AppInfo implements Serializable {

    public String app_name = "";
    public String built_date = "";
    public String version = "";
    public String version_code = "";

}