package com.laodev.chatapp.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.laodev.chatapp.R;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.Group;
import com.laodev.chatapp.models.LogCall;
import com.laodev.chatapp.models.Status;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.services.SinchService;
import com.laodev.chatapp.utils.AudioPlayer;
import com.laodev.chatapp.utils.OnDragTouchListener;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallState;
import com.sinch.android.rtc.video.VideoCallListener;
import com.sinch.android.rtc.video.VideoController;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CallScreenActivity extends BaseActivity implements SensorEventListener {

    private final String TAG = CallScreenActivity.class.getSimpleName();
    static final String ADDED_LISTENER = "addedListener";
    private static String EXTRA_DATA_USER = "extradatauser";
    private static String EXTRA_DATA_IN_OR_OUT = "extradatainorout";

    private AudioPlayer mAudioPlayer;
    private Timer mTimer;
    private UpdateCallDurationTask mDurationTask;

    private String mCallId, inOrOut;
    private boolean mAddedListener;
    private boolean mLocalVideoViewAdded;
    private boolean mRemoteVideoViewAdded;
    private boolean isVideo;
    private boolean isMute;
    private boolean isSpeaker;
    private boolean logSaved;
    private int mCallDurationSecond = 0;

    private TextView mCallDuration, mCallState, mCallerName, myTxtCalling;
    private ImageView userImage1, userImage2, switchVideo, switchMic, switchVolume;
    private View tintBlue;
    private RelativeLayout localVideo, remoteVideo;
    private LinearLayout mySwitchCameraLLY;

    private SensorManager mSensorManager;
    private Sensor mProximity;
    private PowerManager.WakeLock wlOff = null, wlOn = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (wlOff != null && wlOff.isHeld()) {
                wlOff.release();
            } else if (wlOn != null && wlOn.isHeld()) {
                wlOn.release();
            }
        } catch (RuntimeException ignored) { }
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float distance = sensorEvent.values[0];
        if (!isVideo && !isSpeaker) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (distance < 4) {
                if (wlOn != null && wlOn.isHeld()) {
                    wlOn.release();
                }
                if (pm != null) {
                    if (wlOff == null)
                        wlOff = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "tag");
                    if (!wlOff.isHeld()) wlOff.acquire(10*60*1000L /*10 minutes*/);
                }
            } else {
                if (wlOff != null && wlOff.isHeld()) {
                    wlOff.release();
                }
                if (pm != null) {
                    if (wlOn == null)
                        wlOn = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
                    if (!wlOn.isHeld()) wlOn.acquire(10*60*1000L /*10 minutes*/);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private class UpdateCallDurationTask extends TimerTask {
        @Override
        public void run() {
            CallScreenActivity.this.runOnUiThread(CallScreenActivity.this::updateCallDuration);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_screen);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        Intent intent = getIntent();
        user = intent.getParcelableExtra(EXTRA_DATA_USER);
        mCallId = intent.getStringExtra(SinchService.CALL_ID);
        inOrOut = intent.getStringExtra(EXTRA_DATA_IN_OR_OUT);

        mAudioPlayer = new AudioPlayer(this);
        mCallDuration = findViewById(R.id.callDuration);
        mCallerName = findViewById(R.id.remoteUser);
        mCallState = findViewById(R.id.callState);
        userImage1 = findViewById(R.id.userImage1);
        userImage2 = findViewById(R.id.userImage2);
        myTxtCalling = findViewById(R.id.txt_calling);
        tintBlue = findViewById(R.id.tintBlue);
        localVideo = findViewById(R.id.localVideo);
        remoteVideo = findViewById(R.id.remoteVideo);
        switchVideo = findViewById(R.id.switchVideo);
        switchMic = findViewById(R.id.switchMic);
        switchVolume = findViewById(R.id.switchVolume);
        mySwitchCameraLLY = findViewById(R.id.switchVideo_LLY);
        findViewById(R.id.hangupButton).setOnClickListener(v -> endCall());

        switchMic.setOnClickListener(view -> {
            isMute = !isMute;
            setMuteUnmute();
        });
        switchVolume.setOnClickListener(view -> {
            isSpeaker = !isSpeaker;
            enableSpeaker(isSpeaker);
        });
        switchVideo.setClickable(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDurationTask.cancel();
        mTimer.cancel();
        mSensorManager.unregisterListener(this);
        removeVideoViews();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer = new Timer();
        mDurationTask = new UpdateCallDurationTask();
        mTimer.schedule(mDurationTask, 0, 500);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        updateUI();
    }

    @Override
    public void onBackPressed() {
        // User should exit activity by ending call, not by going back.
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();
        Call call = getSinchServiceInterface().getCall(mCallId);

        if (call != null) {
            if (user == null) {
                user = new User(call.getRemoteUserId(), call.getRemoteUserId(), getString(R.string.app_name), "");
            }
            call.hangup();
        }
        saveLog();
        finish();
    }

    private void saveLog() {
        if (!logSaved) {
            rChatDb.beginTransaction();
            rChatDb.copyToRealm(new LogCall(user, System.currentTimeMillis(), mCallDurationSecond,
                    isVideo, inOrOut, userMe.getId(), user.getId()));
            rChatDb.commitTransaction();
            logSaved = true;
        }
    }

    private String formatTimespan(int totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void updateCallDuration() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            mCallDurationSecond = call.getDetails().getDuration();
            mCallDuration.setText(formatTimespan(call.getDetails().getDuration()));
        }
    }

    @Override
    void onSinchConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (!mAddedListener) {
                call.addCallListener(new SinchCallListener());
                mAddedListener = true;
            }
            mCallerName.setText(user != null ? user.getNameToDisplay() : call.getRemoteUserId());
            mCallState.setText(call.getState().toString());
            if (user != null) {
                if (user.getImage() != null && !user.getImage().isEmpty()) {
                    if (user.getBlockedUsersIds() != null
                            && !user.getBlockedUsersIds().contains(userMe.getId()))
                        Picasso.get()
                                .load(user.getImage())
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage1);
                    else
                        Picasso.get()
                                .load(R.drawable.ic_avatar)
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage1);

                    Picasso.get()
                            .load(user.getImage())
                            .tag(this)
                            .error(R.drawable.ic_avatar)
                            .placeholder(R.drawable.ic_avatar)
                            .into(userImage2);
                } else {
                    userImage1.setBackgroundResource(R.drawable.ic_avatar);
                    userImage2.setBackgroundResource(R.drawable.ic_avatar);
                }
            }
        } else {
            finish();
        }
        updateUI();
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            mCallerName.setText(user != null ? user.getNameToDisplay() : call.getRemoteUserId());
            mCallState.setText(call.getState().toString());
            isVideo = call.getDetails().isVideoOffered();
            if (isVideo) {
                addLocalView();
                if (call.getState() == CallState.ESTABLISHED) {
                    addRemoteView();
                }
                mySwitchCameraLLY.setVisibility(View.VISIBLE);
            } else {
                mySwitchCameraLLY.setVisibility(View.GONE);
            }

            myTxtCalling.setText(getResources().getString(R.string.app_name) + (isVideo ? getString(R.string.video_calling) : getString(R.string.voice_calling)));
            tintBlue.setVisibility(isVideo ? View.GONE : View.VISIBLE);
            localVideo.setVisibility(!isVideo ? View.GONE : View.VISIBLE);
        }
    }

    private void removeVideoViews() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            remoteVideo.removeView(vc.getRemoteView());

            localVideo.removeView(vc.getLocalView());
            mLocalVideoViewAdded = false;
            mRemoteVideoViewAdded = false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addLocalView() {
        if (mLocalVideoViewAdded || getSinchServiceInterface() == null) {
            return; //early
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            localVideo.addView(vc.getLocalView());
            switchVideo.setOnClickListener(v -> vc.toggleCaptureDevicePosition());
            mLocalVideoViewAdded = true;
            localVideo.setOnTouchListener(new OnDragTouchListener(localVideo, remoteVideo));
        }
    }

    private void addRemoteView() {
        if (mRemoteVideoViewAdded || getSinchServiceInterface() == null) {
            return; //early
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            RelativeLayout view = findViewById(R.id.remoteVideo);
            view.addView(vc.getRemoteView());
            mRemoteVideoViewAdded = true;
        }
    }

    private void enableSpeaker(boolean enable) {
        AudioController audioController = getSinchServiceInterface().getAudioController();
        if (enable)
            audioController.enableSpeaker();
        else
            audioController.disableSpeaker();
        switchVolume.setImageDrawable(ContextCompat.getDrawable(this, isSpeaker ? R.drawable.ic_speaker : R.drawable.ic_speaker_off));
    }

    private void setMuteUnmute() {
        AudioController audioController = getSinchServiceInterface().getAudioController();
        if (isMute) {
            audioController.mute();
            switchMic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
        } else {
            audioController.unmute();
            switchMic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_on));
        }
    }

    @Override
    void onSinchDisconnected() {

    }

    private class SinchCallListener implements VideoCallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended. Reason: " + cause.toString());
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            String endMsg = call.getDetails().toString();
            Log.d(TAG, "Call ended. Reason: " + endMsg);
            if (user == null) {
                user = new User(call.getRemoteUserId(), call.getRemoteUserId(), getString(R.string.app_name), "");
            }
            endCall();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
            mAudioPlayer.stopProgressTone();
            mCallState.setText(call.getState().toString());
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            isSpeaker = call.getDetails().isVideoOffered();
            enableSpeaker(isSpeaker);
            switchVideo.setClickable(call.getDetails().isVideoOffered());
            if (call.getDetails().isVideoOffered()) {
                mySwitchCameraLLY.setVisibility(View.VISIBLE);
            } else {
                mySwitchCameraLLY.setVisibility(View.GONE);
            }
            switchVideo.setAlpha(call.getDetails().isVideoOffered() ? 1f : 0.4f);
            userImage1.setVisibility(isVideo ? View.GONE : View.VISIBLE);
            Log.d(TAG, "Call offered video: " + call.getDetails().isVideoOffered());
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
            mAudioPlayer.playProgressTone();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }

        @Override
        public void onVideoTrackAdded(Call call) {
            Log.d(TAG, "Video track added");
            addRemoteView();
        }

        @Override
        public void onVideoTrackPaused(Call call) {

        }

        @Override
        public void onVideoTrackResumed(Call call) {

        }

    }

    public static Intent newIntent(Context context, User user, String callId, String inOrOut) {
        Intent intent = new Intent(context, CallScreenActivity.class);
        intent.putExtra(EXTRA_DATA_USER, user);
        intent.putExtra(EXTRA_DATA_IN_OR_OUT, inOrOut);
        intent.putExtra(SinchService.CALL_ID, callId);
        return intent;
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
