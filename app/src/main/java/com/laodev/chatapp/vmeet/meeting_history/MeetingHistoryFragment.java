package com.laodev.chatapp.vmeet.meeting_history;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.laodev.chatapp.R;
import com.laodev.chatapp.interfaces.HomeIneractor;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Constants;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.vmeet.bean.MeetingHistory;
import com.laodev.chatapp.vmeet.firebase_db.DatabaseManager;
import com.laodev.chatapp.vmeet.meeting.MeetingActivity;
import com.laodev.chatapp.vmeet.utils.AppConstants;
import com.laodev.chatapp.vmeet.utils.SharedObjects;
import com.laodev.chatapp.vmeet.utils.SimpleDividerItemDecoration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MeetingHistoryFragment extends Fragment implements DatabaseManager.OnDatabaseDataChanged {

    private LinearLayout llError;
    private RecyclerView rvHistory;
    private TextView txtError;

    DatabaseManager databaseManager ;
    private ArrayList<MeetingHistory> arrMeetingHistory = new ArrayList<>();
    MeetingHistoryAdapter meetingHistoryAdapter;

    SharedObjects sharedObjects;

    private User userBean;

    String[] appPermissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private static final int PERMISSION_REQUEST_CODE = 10001;
    private static final int SETTINGS_REQUEST_CODE = 10002;

    public MeetingHistoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_meeting_history, container, false);

        userBean = Constants.gUserMe;
        initWithView(view);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        sharedObjects = new SharedObjects(getActivity());
        databaseManager = new DatabaseManager();
        databaseManager.setDatabaseManagerListener(this);

        getData();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkAppPermissions(appPermissions)) {
                requestAppPermissions(appPermissions);
            }
        }

        return view;
    }

    private void initWithView(View view) {
        llError = view.findViewById(R.id.llError);
        rvHistory = view.findViewById(R.id.rvHistory);
        txtError = view.findViewById(R.id.txtError);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void getData(){
        databaseManager.getMeetingHistoryByUser(userBean.getId());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setMeetingHistoryAdapter() {
        if (arrMeetingHistory.size() > 0) {
            Collections.sort(arrMeetingHistory, (arg0, arg1) -> {
                SimpleDateFormat format = new SimpleDateFormat(AppConstants.DateFormats.DATETIME_FORMAT_24, Locale.US);
                int compareResult = 0;
                try {
                    Date arg0Date = format.parse(arg0.getStartTime());
                    Date arg1Date = format.parse(arg1.getStartTime());
                    compareResult = arg1Date.compareTo(arg0Date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return compareResult;
            });

            meetingHistoryAdapter = new MeetingHistoryAdapter(arrMeetingHistory, getActivity());
            rvHistory.setAdapter(meetingHistoryAdapter);
            rvHistory.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

            meetingHistoryAdapter.setOnItemClickListener(new MeetingHistoryAdapter.OnItemClickListener() {
                @Override
                public void onItemClickListener(int position, MeetingHistory bean) {
                }

                @Override
                public void onDeleteClickListener(int position, MeetingHistory bean) {
                    databaseManager.deleteMeetingHistory(bean);
                }

                @Override
                public void onJoinClickListener(int position, MeetingHistory bean) {
                    AppConstants.MEETING_ID = bean.getMeeting_id();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkAppPermissions(appPermissions)) {
                            startActivity(new Intent(getActivity(), MeetingActivity.class));
                        } else {
                            requestAppPermissions(appPermissions);
                        }
                    } else {
                        startActivity(new Intent(getActivity(), MeetingActivity.class));
                    }

                }
            });

            rvHistory.setVisibility(View.VISIBLE);
            llError.setVisibility(View.GONE);
        } else {
            rvHistory.setVisibility(View.GONE);
            llError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDataChanged(String url, DataSnapshot dataSnapshot) {
        if (url.equalsIgnoreCase(AppConstants.Table.MEETING_HISTORY)){
            if (MeetingHistoryFragment.this.isVisible()){
                arrMeetingHistory = new ArrayList<>();
                arrMeetingHistory.addAll(databaseManager.getUserMeetingHistory());
                setMeetingHistoryAdapter();
            }
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        if (MeetingHistoryFragment.this.isVisible()) {
            arrMeetingHistory = new ArrayList<>();
            setMeetingHistoryAdapter();
        }
    }

    public boolean checkAppPermissions(String[] appPermissions) {
        //check which permissions are granted
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(getActivity(), perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        //Ask for non granted permissions
        if (!listPermissionsNeeded.isEmpty()) {
            return false;
        }
        // App has all permissions
        return true;
    }

    private void requestAppPermissions(String[] appPermissions) {
        ActivityCompat.requestPermissions(getActivity(), appPermissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                HashMap<String, Integer> permissionResults = new HashMap<>();
                int deniedCount = 0;

                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        permissionResults.put(permissions[i], grantResults[i]);
                        deniedCount++;
                    }
                }
                if (deniedCount == 0) {
                    Log.e("Permissions", "All permissions are granted!");
                    //invoke ur method
                } else {
                    //some permissions are denied
                    for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                        String permName = entry.getKey();
                        int permResult = entry.getValue();
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permName)) {
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
                            materialAlertDialogBuilder.setMessage(getString(R.string.permission_msg));
                            materialAlertDialogBuilder.setCancelable(false)
                                    .setNegativeButton(getString(android.R.string.no), (dialog, which) -> dialog.cancel())
                                    .setPositiveButton(getString(R.string.yes_grant_permission), (dialog, id) -> {
                                        dialog.cancel();
                                        if (!checkAppPermissions(appPermissions)) {
                                            requestAppPermissions(appPermissions);
                                        }
                                    });
                            materialAlertDialogBuilder.show();

                            break;
                        } else {//permission is denied and never asked is checked
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
                            materialAlertDialogBuilder.setMessage(getString(R.string.permission_msg_never_checked));
                            materialAlertDialogBuilder.setCancelable(false)
                                    .setNegativeButton(getString(android.R.string.no), (dialog, which) -> dialog.cancel())
                                    .setPositiveButton(getString(R.string.go_to_settings), (dialog, id) -> {
                                        dialog.cancel();
                                        openSettings();
                                    });
                            materialAlertDialogBuilder.show();

                            break;
                        }

                    }
                }

        }
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, SETTINGS_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case SETTINGS_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        if (checkAppPermissions(appPermissions)) {

                        } else {
                            requestAppPermissions(appPermissions);
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
