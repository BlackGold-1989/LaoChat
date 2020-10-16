package com.laodev.chatapp.vmeet.firebase_db;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.utils.Constants;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.utils.SharedPreferenceHelper;
import com.laodev.chatapp.vmeet.bean.MeetingHistory;
import com.laodev.chatapp.vmeet.bean.Schedule;
import com.laodev.chatapp.vmeet.utils.AppConstants;

import java.util.ArrayList;


public class DatabaseManager {

    private static final String TAG = DatabaseManager.class.getSimpleName();

    private DatabaseReference databaseMeetingHistory;
    private DatabaseReference databaseSchedule;

    private OnDatabaseDataChanged mDatabaseListener;
    private OnUserAddedListener onUserAddedListener;
    private OnUserListener onUserListener;
    private OnUserPasswordListener onUserPasswordListener;
    private OnScheduleListener onScheduleListener;
    private OnMeetingHistoryListener onMeetingHistoryListener;
    private OnUserDeleteListener onUserDeleteListener;

    private ArrayList<MeetingHistory> arrMeetingHistory = new ArrayList<>();
    private ArrayList<Schedule> arrSchedule = new ArrayList<>();
    private ArrayList<UserInfo> arrUsers = new ArrayList<>();

    private UserInfo userBean = null;

    public DatabaseManager() {
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

        databaseMeetingHistory = mDatabase.getReference(Helper.REF_DATA).child(AppConstants.Table.MEETING_HISTORY);
        databaseMeetingHistory.keepSynced(true);

        databaseSchedule = mDatabase.getReference(Helper.REF_DATA).child(AppConstants.Table.SCHEDULE);
        databaseSchedule.keepSynced(true);
    }

    public void initUsers() {
        BaseApplication.getUserRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    arrUsers = new ArrayList<>();
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        UserInfo customer = postSnapshot.getValue(UserInfo.class);
                        if (customer != null) {
                            arrUsers.add(customer);
                        }
                    }
                    if (mDatabaseListener != null) {
                        mDatabaseListener.onDataChanged(AppConstants.USER_INFO, dataSnapshot);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
                if (mDatabaseListener != null) {
                    mDatabaseListener.onCancelled(databaseError);
                }
            }
        });
    }

    public ArrayList<UserInfo> getUsers() {
        return arrUsers;
    }

    public UserInfo getCurrentUser() {
        return userBean;
    }

    public void addUser(UserInfo bean) {
        BaseApplication.getUserRef().child(bean.getUid()).setValue(bean).addOnSuccessListener(aVoid -> {
            if (onUserAddedListener != null) {
                onUserAddedListener.onSuccess();
            }
        }).addOnFailureListener(e -> {
            if (onUserAddedListener != null) {
                onUserAddedListener.onFail();
            }
        });
    }

    public void updateUser(UserInfo bean) {
        DatabaseReference db = BaseApplication.getUserRef().child(bean.getUid());
        db.setValue(bean).addOnSuccessListener(aVoid -> {
            if (onUserAddedListener != null) {
                onUserAddedListener.onSuccess();
            }
        }).addOnFailureListener(e -> {
            if (onUserAddedListener != null) {
                onUserAddedListener.onFail();
            }
        });
    }

    public void getUser(final String id) {
        if (id.equals(Constants.gUserMe.getId())) {
            userBean = FirebaseAuth.getInstance().getCurrentUser();
            if (onUserListener != null) {
                onUserListener.onUserFound();
            }
            return;
        }
        Query query = BaseApplication.getUserRef().orderByChild("id").equalTo(id);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(UserInfo.class).getUid().equals(id)) {
                            userBean = postSnapshot.getValue(UserInfo.class);
                        }
                    }
                }
                if (onUserListener != null) {
                    onUserListener.onUserFound();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (onUserListener != null) {
                    onUserListener.onUserNotFound();
                }
            }
        });
    }

    public void updateUserPassword(UserInfo bean) {
        DatabaseReference db = BaseApplication.getUserRef().child(bean.getUid());
        db.setValue(bean).addOnSuccessListener(aVoid -> {
            if (onUserPasswordListener!= null) {
                onUserPasswordListener.onPasswordUpdateSuccess();
            }
        }).addOnFailureListener(e -> {
            if (onUserPasswordListener != null) {
                onUserPasswordListener.onPasswordUpdateFail();
            }
        });
    }

    public void deleteUser(UserInfo bean) {
        BaseApplication.getUserRef().child(bean.getUid()).removeValue((databaseError, databaseReference) -> {
            if (databaseError != null) {
                if (onUserDeleteListener != null) {
                    onUserDeleteListener.onUserDeleteFail();
                }
            } else {
                if (onUserDeleteListener != null) {
                    onUserDeleteListener.onUserDeleteSuccess();
                }
            }
        });
    }

    public void getScheduleByUser(final String id) {
        Query query = databaseSchedule.orderByChild("userId").equalTo(id);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                arrSchedule = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(Schedule.class).getUserId().equals(id)) {
                            Schedule products = postSnapshot.getValue(Schedule.class);
                            if (products != null) {
                                arrSchedule.add(products);
                            }
                        }
                    }
                }
                if (mDatabaseListener != null) {
                    mDatabaseListener.onDataChanged(AppConstants.Table.SCHEDULE, dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (mDatabaseListener != null) {
                    mDatabaseListener.onCancelled(databaseError);
                }
            }
        });
    }

    public ArrayList<Schedule> getUserSchedule() {
        return arrSchedule;
    }

    public void addSchedule(Schedule bean) {
        String id = databaseSchedule.push().getKey();
        bean.setId(id);
        databaseSchedule.child(bean.getId()).setValue(bean).addOnSuccessListener(aVoid -> {
            if (onScheduleListener != null) {
                onScheduleListener.onAddSuccess();
            }
        }).addOnFailureListener(e -> {
            if (onScheduleListener != null) {
                onScheduleListener.onAddFail();
            }
        });
    }

    public void updateSchedule(Schedule bean) {
        DatabaseReference db = databaseSchedule.child(bean.getId());
        db.setValue(bean).addOnSuccessListener(aVoid -> {
            if (onScheduleListener != null) {
                onScheduleListener.onUpdateSuccess();
            }
        }).addOnFailureListener(e -> {
            if (onScheduleListener != null) {
                onScheduleListener.onUpdateFail();
            }
        });
    }

    public void deleteSchedule(Schedule bean) {
        databaseSchedule.child(bean.getId()).removeValue((databaseError, databaseReference) -> {
            if (databaseError != null) {
                if (onScheduleListener != null) {
                    onScheduleListener.onDeleteFail();
                }
            } else {
                if (onScheduleListener != null) {
                    onScheduleListener.onDeleteSuccess();
                }
            }
        });
    }

    public void addMeetingHistory(MeetingHistory bean) {
        databaseMeetingHistory.child(bean.getId()).setValue(bean).addOnSuccessListener(aVoid -> {
            if (onMeetingHistoryListener != null) {
                onMeetingHistoryListener.onAddSuccess();
            }
        }).addOnFailureListener(e -> {
            if (onMeetingHistoryListener != null) {
                onMeetingHistoryListener.onAddFail();
            }
        });
    }

    public String getKeyForMeetingHistory() {
        return  databaseMeetingHistory.push().getKey();
    }

    public void updateMeetingHistory(MeetingHistory bean) {
        DatabaseReference db = databaseMeetingHistory.child(bean.getId());
        db.setValue(bean).addOnSuccessListener(aVoid -> {
            if (onMeetingHistoryListener != null) {
                onMeetingHistoryListener.onUpdateSuccess();
            }
        }).addOnFailureListener(e -> {
            if (onMeetingHistoryListener != null) {
                onMeetingHistoryListener.onUpdateFail();
            }
        });
    }

    public void getMeetingHistoryByUser(final String userId) {
        Query query = databaseMeetingHistory.orderByChild("userId").equalTo(userId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                arrMeetingHistory = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(MeetingHistory.class).getUserId().equals(userId)) {
                            MeetingHistory meetingHistory = postSnapshot.getValue(MeetingHistory.class);
                            if (meetingHistory != null) {
                                arrMeetingHistory.add(meetingHistory);
                            }
                        }
                    }
                }
                if (mDatabaseListener != null) {
                    mDatabaseListener.onDataChanged(AppConstants.Table.MEETING_HISTORY, dataSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (mDatabaseListener != null) {
                    mDatabaseListener.onCancelled(databaseError);
                }
            }
        });
    }

    public ArrayList<MeetingHistory> getUserMeetingHistory() {
        return arrMeetingHistory;
    }

    public void deleteMeetingHistory(MeetingHistory bean) {
        databaseMeetingHistory.child(bean.getId()).removeValue((databaseError, databaseReference) -> {
            if (databaseError != null) {
                if (onMeetingHistoryListener != null) {
                    onMeetingHistoryListener.onDeleteFail();
                }
            } else {
                if (onMeetingHistoryListener != null) {
                    onMeetingHistoryListener.onDeleteSuccess();
                }
            }
        });
    }

    public interface OnUserListener {
        void onUserFound();
        void onUserNotFound();
    }

    public interface OnDatabaseDataChanged {
        void onDataChanged(String url, DataSnapshot dataSnapshot);
        void onCancelled(DatabaseError error);
    }

    public interface OnUserAddedListener {
        void onSuccess();
        void onFail();
    }

    public interface OnUserDeleteListener {
        void onUserDeleteSuccess();
        void onUserDeleteFail();
    }

    public void setOnUserAddedListener(OnUserAddedListener listener) {
        onUserAddedListener = listener;
    }

    public void setDatabaseManagerListener(OnDatabaseDataChanged listener) {
        mDatabaseListener = listener;
    }

    public interface OnUserPasswordListener {
        void onPasswordUpdateSuccess();
        void onPasswordUpdateFail();
    }

    public interface OnScheduleListener {
        void onAddSuccess();
        void onUpdateSuccess();
        void onDeleteSuccess();
        void onAddFail();
        void onUpdateFail();
        void onDeleteFail();
    }

    public interface OnMeetingHistoryListener {
        void onAddSuccess();
        void onUpdateSuccess();
        void onDeleteSuccess();
        void onAddFail();
        void onUpdateFail();
        void onDeleteFail();
    }

    public void setOnUserPasswordListener(OnUserPasswordListener onUserPasswordListener) {
        this.onUserPasswordListener = onUserPasswordListener;
    }

    public OnUserListener getOnUserListener() {
        return onUserListener;
    }

    public void setOnUserListener(OnUserListener onUserListener) {
        this.onUserListener = onUserListener;
    }

    public OnScheduleListener getOnScheduleListener() {
        return onScheduleListener;
    }

    public void setOnScheduleListener(OnScheduleListener onScheduleListener) {
        this.onScheduleListener = onScheduleListener;
    }

}
