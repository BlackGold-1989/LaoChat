package com.laodev.chatapp;

import android.app.Application;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import android.content.Context;
import android.util.Log;

import com.laodev.chatapp.models.User;
import com.laodev.chatapp.receivers.ConnectivityReceiver;
import com.laodev.chatapp.utils.Helper;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;

public class BaseApplication extends Application implements LifecycleObserver {

    private static FirebaseDatabase firebaseDatabase;
    private static DatabaseReference userRef, chatRef, groupsRef, statusRef, appRef;
    protected Helper helper;
    protected User userMe;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        ConnectivityReceiver.init(this);
        EmojiManager.install(new GoogleEmojiProvider());
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        //  MobileAds.initialize(this, "Statusr-app-id");
        init();
    }

    private void init() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        appRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_APP);
        userRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_USER);
        chatRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_CHAT);
        groupsRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_GROUP);
        statusRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_STATUS_NEW);

        AndroidAudioConverter.load(this, new ILoadCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(Exception error) {
                error.printStackTrace();
            }
        });

        final FFmpeg ffmpeg = FFmpeg.getInstance(getApplicationContext());
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onSuccess() {

                }
            });
        } catch (FFmpegNotSupportedException ignored) {

        }
    }

    public static DatabaseReference getAppRef() {
        if (appRef == null) {
            appRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_APP);
            appRef.keepSynced(true);
        }
        return appRef;
    }

    public static DatabaseReference getUserRef() {
        if (userRef == null) {
            userRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_USER);
            userRef.keepSynced(true);
        }
        return userRef;
    }

    public static DatabaseReference getChatRef() {
        if (chatRef == null) {
            chatRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_CHAT);
            chatRef.keepSynced(true);
        }
        return chatRef;
    }

    public static DatabaseReference getGroupRef() {
        if (groupsRef == null) {
            groupsRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_GROUP);
            groupsRef.keepSynced(true);
        }
        return groupsRef;
    }

    public static DatabaseReference getStatusRef() {
        if (statusRef == null) {
            statusRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_STATUS_NEW);
            statusRef.keepSynced(true);
        }
        return statusRef;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onAppBackgrounded() {
        markOnline(false);
        Helper.CURRENT_CHAT_ID = null;
        Log.d("MyApp", "App in background");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onAppForegrounded() {
        markOnline(true);
        Log.d("MyApp", "App in foreground");
    }

    private void markOnline(boolean b) {
        helper = new Helper(this);
        userMe = helper.getLoggedInUser();
        if (userMe != null && userMe.getId() != null) {
            getUserRef().child(userMe.getId()).child("timeStamp").setValue(System.currentTimeMillis());
            getUserRef().child(userMe.getId()).child("online").setValue(b);
        }
    }

}
