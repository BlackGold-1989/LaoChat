package com.laodev.chatapp.vmeet.meeting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;

import com.facebook.react.modules.core.PermissionListener;
import com.laodev.chatapp.interfaces.HomeIneractor;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Constants;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.vmeet.bean.MeetingHistory;
import com.laodev.chatapp.vmeet.firebase_db.DatabaseManager;
import com.laodev.chatapp.vmeet.utils.AppConstants;
import com.laodev.chatapp.vmeet.utils.SharedObjects;

import org.jitsi.meet.sdk.JitsiMeetActivityDelegate;
import org.jitsi.meet.sdk.JitsiMeetActivityInterface;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.jitsi.meet.sdk.JitsiMeetView;
import org.jitsi.meet.sdk.JitsiMeetViewListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class MeetingActivity extends FragmentActivity implements JitsiMeetActivityInterface {

    private JitsiMeetView view;
    SharedObjects sharedObjects ;

    DatabaseManager mDatabaseManager;
    MeetingHistory meetingHistory = null;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        JitsiMeetActivityDelegate.onActivityResult(
                this, requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        JitsiMeetActivityDelegate.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new JitsiMeetView(this);

        sharedObjects = new SharedObjects(MeetingActivity.this);
        mDatabaseManager = new DatabaseManager();

        User userBean = Constants.gUserMe;
        meetingHistory = new MeetingHistory();
        meetingHistory.setId(mDatabaseManager.getKeyForMeetingHistory());
        meetingHistory.setUserId(userBean.getId());
        meetingHistory.setMeeting_id(AppConstants.MEETING_ID);

        JitsiMeetUserInfo jitsiMeetUserInfo = new JitsiMeetUserInfo();
        jitsiMeetUserInfo.setDisplayName(userBean.getName());
        try {
            if (!TextUtils.isEmpty(userBean.getImage())){
                jitsiMeetUserInfo.setAvatar(new URL(userBean.getImage()));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        JitsiMeetConferenceOptions options = null;
        try {
            options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(AppConstants.MEETING_ID)
                    .setUserInfo(jitsiMeetUserInfo)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("pip.enabled", true)
                    .setWelcomePageEnabled(false)
                    .build();

            if (meetingHistory != null) {
                meetingHistory.setSubject(options.getSubject());
            }
            view.join(options);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        view.setListener(new JitsiMeetViewListener() {
            @Override
            public void onConferenceJoined(Map<String, Object> map) {
                if (meetingHistory != null){
                    meetingHistory.setStartTime(SharedObjects.getTodaysDate(AppConstants.DateFormats.DATETIME_FORMAT_24));
                    meetingHistory.setEndTime("");
                    saveMeetingDetails();
                }
            }

            @Override
            public void onConferenceTerminated(Map<String, Object> map) {
                if (meetingHistory != null) {
                    if (TextUtils.isEmpty(meetingHistory.getStartTime())){
                        meetingHistory.setStartTime(SharedObjects.getTodaysDate(AppConstants.DateFormats.DATETIME_FORMAT_24));
                    }
                    meetingHistory.setEndTime(SharedObjects.getTodaysDate(AppConstants.DateFormats.DATETIME_FORMAT_24));
                    updateMeetingDetails();
                }
                onBackPressed();
            }

            @Override
            public void onConferenceWillJoin(Map<String, Object> map) {
            }
        });

        setContentView(view);
    }

    private void saveMeetingDetails() {
        mDatabaseManager.addMeetingHistory(meetingHistory);
    }
    private void updateMeetingDetails() {
        mDatabaseManager.updateMeetingHistory(meetingHistory);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        view.dispose();
        view = null;

        JitsiMeetActivityDelegate.onHostDestroy(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        JitsiMeetActivityDelegate.onNewIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode,
            final String[] permissions,
            final int[] grantResults) {
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        JitsiMeetActivityDelegate.onHostResume(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        JitsiMeetActivityDelegate.onHostPause(this);
    }

    @Override
    public void requestPermissions(String[] strings, int i, PermissionListener permissionListener) {
    }

}