package com.laodev.chatapp.vmeet.schedule;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.laodev.chatapp.vmeet.bean.Schedule;
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

import static android.content.Context.CLIPBOARD_SERVICE;


public class ScheduleFragment extends Fragment implements DatabaseManager.OnDatabaseDataChanged, View.OnClickListener {

    private LinearLayout llError;
    private RecyclerView rvEvents;

    private ArrayList<Schedule> arrSchedule = new ArrayList<>();

    private DatabaseManager databaseManager ;

    private String[] appPermissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private static final int PERMISSION_REQUEST_CODE = 10001;
    private static final int SETTINGS_REQUEST_CODE = 10002;

    private User userBean;

    public ScheduleFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vmeet_schedule, container, false);

        initWithView(view);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        userBean = Constants.gUserMe;
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
        rvEvents = view.findViewById(R.id.rvEvents);
        ImageView imgAdd = view.findViewById(R.id.imgAdd);
        imgAdd.setOnClickListener(this);
        TextView txtError = view.findViewById(R.id.txtError);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void getData(){
        databaseManager.getScheduleByUser(userBean.getId());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setScheduleAdapter() {
        if (arrSchedule.size() > 0) {
            Collections.sort(arrSchedule, (arg0, arg1) -> {
                SimpleDateFormat format = new SimpleDateFormat(AppConstants.DateFormats.DATE_FORMAT_DASH, Locale.US);
                int compareResult = 0;
                try {
                    Date arg0Date = format.parse(arg0.getDate());
                    Date arg1Date = format.parse(arg1.getDate());
                    compareResult = arg1Date.compareTo(arg0Date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return compareResult;
            });

            ScheduleAdapter scheduleAdapter = new ScheduleAdapter(arrSchedule, getActivity());
            rvEvents.setAdapter(scheduleAdapter);
            rvEvents.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

            scheduleAdapter.setOnItemClickListener(new ScheduleAdapter.OnItemClickListener() {
                @Override
                public void onItemClickListener(int position, Schedule bean) {
                    startActivity(new Intent(getActivity(),ScheduleMeetingActivity.class)
                    .putExtra(AppConstants.INTENT_BEAN,bean));
                }

                @Override
                public void onDeleteClickListener(int position, Schedule bean) {
                    databaseManager.deleteSchedule(bean);
                }

                @Override
                public void onStartClickListener(int position, Schedule bean) {

                    AppConstants.MEETING_ID = bean.getMeeetingId();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkAppPermissions(appPermissions)) {
                            showMeetingShareDialog();
                        } else {
                            requestAppPermissions(appPermissions);
                        }
                    } else {
                        showMeetingShareDialog();
                    }
                }
            });
            rvEvents.setVisibility(View.VISIBLE);
            llError.setVisibility(View.GONE);
        } else {
            rvEvents.setVisibility(View.GONE);
            llError.setVisibility(View.VISIBLE);
        }
    }

    private void showMeetingShareDialog() {
        final Dialog dialogDate = new Dialog(getActivity());
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogDate.setContentView(R.layout.dialog_meeting_share);
        dialogDate.setCancelable(true);

        Window window = dialogDate.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        wlp.dimAmount = 0.8f;
        window.setAttributes(wlp);
        dialogDate.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView txtMeetingURL = dialogDate.findViewById(R.id.txtMeetingURL);
        ImageView imgCopy = dialogDate.findViewById(R.id.imgCopy);
        txtMeetingURL.setText(AppConstants.MEETING_ID);
        ImageView imgShare = dialogDate.findViewById(R.id.imgShare);
        imgShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Vmeet Link");
            intent.putExtra(Intent.EXTRA_TEXT, txtMeetingURL.getText().toString());
            getActivity().startActivity(Intent.createChooser(intent, "Share Via"));
        });


        imgCopy.setOnClickListener(v -> {
            ClipboardManager myClipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
            ClipData myClip;
            myClip = ClipData.newPlainText("text", txtMeetingURL.getText().toString());
            myClipboard.setPrimaryClip(myClip);
            Toast.makeText(getActivity(), "Link copied", Toast.LENGTH_SHORT).show();
        });

        txtMeetingURL.setOnClickListener(v -> {
            ClipboardManager myClipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
            ClipData myClip;
            myClip = ClipData.newPlainText("text", txtMeetingURL.getText().toString());
            myClipboard.setPrimaryClip(myClip);
            Toast.makeText(getActivity(), "Link copied", Toast.LENGTH_SHORT).show();
        });

        Button btnContinue = dialogDate.findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            dialogDate.dismiss();
            startActivity(new Intent(getActivity(), MeetingActivity.class));
        });

        if (!dialogDate.isShowing()) {
            dialogDate.show();
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.imgAdd) {
            startActivity(new Intent(getActivity(), ScheduleMeetingActivity.class));
        }
    }

    @Override
    public void onDataChanged(String url, DataSnapshot dataSnapshot) {
        if (url.equalsIgnoreCase(AppConstants.Table.SCHEDULE)){
            if (ScheduleFragment.this.isVisible()){
                arrSchedule = new ArrayList<>();
                arrSchedule.addAll(databaseManager.getUserSchedule());
                setScheduleAdapter();
            }
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        if (ScheduleFragment.this.isVisible()) {
            arrSchedule = new ArrayList<>();
            setScheduleAdapter();
        }
    }

    private boolean checkAppPermissions(String[] appPermissions) {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(getActivity(), perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }
        return listPermissionsNeeded.isEmpty();
    }

    private void requestAppPermissions(String[] appPermissions) {
        ActivityCompat.requestPermissions(getActivity(), appPermissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }
            if (deniedCount > 0) {
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
                    } else {
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
        if (requestCode == SETTINGS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                //
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
