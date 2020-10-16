package com.laodev.chatapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.laodev.chatapp.models.Status;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.laodev.chatapp.R;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.Group;
import com.laodev.chatapp.models.LogCall;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.services.SinchService;
import com.laodev.chatapp.utils.AudioPlayer;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IncomingCallScreenActivity extends BaseActivity {
    static final String TAG = IncomingCallScreenActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSION_CALL = 951;
    private static final String CHANNEL_ID_USER_MISSCALL = "my_channel_04";

    private String[] recordPermissions = {Manifest.permission.VIBRATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private String mCallId;
    private AudioPlayer mAudioPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call_screen);

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        mAudioPlayer = new AudioPlayer(this);
        mAudioPlayer.playRingtone();

        Intent intent = getIntent();
        mCallId = intent.getStringExtra(SinchService.CALL_ID);

        findViewById(R.id.answerButton).setOnClickListener(mClickListener);
        findViewById(R.id.declineButton).setOnClickListener(mClickListener);
    }

    @SuppressLint("SetTextI18n")
    @Override
    void onSinchConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.addCallListener(new SinchCallListener());

            HashMap<String, User> myUsers = helper.getCacheMyUsers();
            if (myUsers != null && myUsers.containsKey(call.getRemoteUserId())) {
                user = myUsers.get(call.getRemoteUserId());
            }

            TextView remoteUser = findViewById(R.id.remoteUser);
            ImageView userImage2 = findViewById(R.id.userImage2);
            remoteUser.setText(user != null ? user.getNameToDisplay() : call.getRemoteUserId());
            if (user != null && !user.getImage().isEmpty()) {
                Picasso.get()
                        .load(user.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .into(userImage2);
            }
            else {
              userImage2.setBackgroundResource(R.drawable.ic_avatar);
            }
            TextView callingType = findViewById(R.id.txt_calling);
            callingType.setText(getResources().getString(R.string.app_name) + (call.getDetails().isVideoOffered() ? getString(R.string.incoming_video_calling) : getString(R.string.incoming_voice_calling)));
        } else {
            Log.e(TAG, "Started with invalid callId, aborting");
            finish();
        }
    }

    private void answerClicked() {
        mAudioPlayer.stopRingtone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            try {
                call.answer();
                startActivity(CallScreenActivity.newIntent(this, user, mCallId, "IN"));
                finish();
            } catch (Exception ignored) { }
        } else {
            finish();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            answerClicked();
        } else {
            Toast.makeText(this, "This application needs permission to use your microphone to function properly.", Toast.LENGTH_LONG).show();
        }
    }

    private void declineClicked() {
        mAudioPlayer.stopRingtone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();
    }

    private boolean recordPermissionsAvailable() {
        boolean available = true;
        for (String permission : recordPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                available = false;
                break;
            }
        }
        return available;
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended, cause: " + cause.toString());
            if (cause.toString().equals("CANCELED") || cause.toString().equals("DENIED")) {
                LogCall logCall = null;
                if (user == null) {
                    user = new User(call.getRemoteUserId(), call.getRemoteUserId(), getString(R.string.app_name), "");
                }

                rChatDb.beginTransaction();
                logCall = new LogCall(user, System.currentTimeMillis(), 0, call.getDetails().isVideoOffered(), cause.toString(), userMe.getId(), user.getId());
                rChatDb.copyToRealm(logCall);
                rChatDb.commitTransaction();

                if (cause.toString().equals("CANCELED")) {
                    notifyMisscall(logCall);
                }
            }
            mAudioPlayer.stopRingtone();
            finish();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }

    }

    private void notifyMisscall(LogCall logCall) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_USER_MISSCALL, "Dreams Chat misscall notification", NotificationManager.IMPORTANCE_DEFAULT);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_USER_MISSCALL);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSmallIcon(R.drawable.ic_logo_)
                .setContentTitle(logCall.getUser().getNameToDisplay())
                .setContentText("Gave you a miss call")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        int msgId = 0;
        try {
            msgId = Integer.parseInt(logCall.getUser().getId());
        } catch (NumberFormatException ex) {
            msgId = Integer.parseInt(logCall.getUser().getId().substring(logCall.getUser().getId().length() / 2));
        }
        if (notificationManager != null) {
            notificationManager.notify(msgId, notificationBuilder.build());
        }
    }

    private View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.answerButton:
                if (recordPermissionsAvailable()) {
                    answerClicked();
                } else {
                    ActivityCompat.requestPermissions(IncomingCallScreenActivity.this, recordPermissions, REQUEST_PERMISSION_CALL);
                }
                break;
            case R.id.declineButton:
                declineClicked();
                break;
        }
    };

    @Override
    void onSinchDisconnected() {

    }

    @Override
    void myUsersResult(ArrayList<User> myUsers) {

    }

    @Override
    void myContactsResult(ArrayList<Contact> myContacts) {

    }

    @Override
    void userAdded(User valueUser) {

    }

    @Override
    void groupAdded(Group valueGroup) {

    }

    @Override
    void userUpdated(User valueUser) {

    }

    @Override
    void groupUpdated(Group valueGroup) {

    }

    @Override
    void statusAdded(Status status) {

    }

    @Override
    void statusUpdated(Status status) {

    }
}
