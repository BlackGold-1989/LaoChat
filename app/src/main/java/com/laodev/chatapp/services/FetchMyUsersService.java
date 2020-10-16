package com.laodev.chatapp.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Helper;

import java.util.ArrayList;
import java.util.Collections;

public class FetchMyUsersService extends IntentService {
    private static String EXTRA_PARAM1 = "my_id";
    private ArrayList<Contact> myContacts;
    private ArrayList<User> myUsers, finalUserList;
    private String myId;
    public static boolean STARTED = false;

    public FetchMyUsersService() {
        super("FetchMyUsersService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    public static void startMyUsersService(Context context, String myId, String idToken) {
        Intent intent = new Intent(context, FetchMyUsersService.class);
        intent.putExtra(EXTRA_PARAM1, myId);
        String EXTRA_PARAM2 = "token";
        intent.putExtra(EXTRA_PARAM2, idToken);
        try {
            context.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        STARTED = true;
        myId = intent.getStringExtra(EXTRA_PARAM1);
        getApplicationContext().getContentResolver().registerContentObserver
                (ContactsContract.Contacts.CONTENT_URI, true, new MyContentObserver());
        fetchMyContacts();
        broadcastMyContacts();
        STARTED = false;
    }

    private void broadcastMyUsers() {
        if (this.finalUserList != null) {
            Intent intent = new Intent(Helper.BROADCAST_MY_USERS);
            intent.putParcelableArrayListExtra("data", this.finalUserList);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.sendBroadcast(intent);
        }
    }

    private void broadcastMyContacts() {
        if (this.myContacts != null) {
            Intent intent = new Intent(Helper.BROADCAST_MY_CONTACTS);
            intent.putParcelableArrayListExtra("data", this.myContacts);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.sendBroadcast(intent);
        }
    }

    private void fetchMyContacts() {
        myContacts = new ArrayList<>();
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (cursor != null && !cursor.isClosed()) {
            cursor.getCount();
            while (cursor.moveToNext()) {
                int hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (hasPhoneNumber == 1) {
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("\\s+", "");
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
                    if (Patterns.PHONE.matcher(number).matches()) {
                        boolean hasPlus = String.valueOf(number.charAt(0)).equals("+");
                        number = number.replaceAll("[\\D]", "");
                        if (hasPlus) {
                            number = "+" + number;
                        }
                        Contact contact = new Contact(number, name);
                        if (!myContacts.contains(contact))
                            myContacts.add(contact);
                    }
                }
            }
            cursor.close();
        }
        registerUserUpdates();
    }


    private void startMyOwnForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = getPackageName();
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_logo_)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        }
    }


    private void registerUserUpdates() {
        myUsers = new ArrayList<>();
        BaseApplication.getUserRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    myUsers.add(user);
                }
                finalUserList = new ArrayList<>();
                for (Contact savedContact : new ArrayList<>(myContacts)) {
                    for (User user : myUsers) {
                        if (user != null && user.getId() != null && !user.getId().equals(myId)) {
                            if (Helper.contactMatches(user.getId(), savedContact.getPhoneNumber())) {
                                user.setNameInPhone(savedContact.getName());
                                finalUserList.add(user);
                                break;
                            }
                        }
                    }
                }
                Collections.sort(finalUserList, (user1, user2) -> user1.getNameToDisplay().compareToIgnoreCase(user2.getNameToDisplay()));
                broadcastMyUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private class MyContentObserver extends ContentObserver {
        MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d("", "A change has happened");
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Log.d("", uri.toString());
            fetchMyContacts();
            broadcastMyContacts();
        }
    }

}
