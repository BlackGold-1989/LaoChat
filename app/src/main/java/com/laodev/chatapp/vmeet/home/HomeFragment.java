package com.laodev.chatapp.vmeet.home;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.R;
import com.laodev.chatapp.interfaces.HomeIneractor;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Constants;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.vmeet.bean.MeetingHistory;
import com.laodev.chatapp.vmeet.firebase_db.DatabaseManager;
import com.laodev.chatapp.vmeet.meeting.MeetingActivity;
import com.laodev.chatapp.vmeet.meeting_history.MeetingHistoryAdapter;
import com.laodev.chatapp.vmeet.schedule.ScheduleMeetingActivity;
import com.laodev.chatapp.vmeet.utils.AppConstants;
import com.laodev.chatapp.vmeet.utils.SharedObjects;
import com.laodev.chatapp.vmeet.utils.SimpleDividerItemDecoration;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

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

public class HomeFragment extends Fragment implements DatabaseManager.OnDatabaseDataChanged, View.OnClickListener {

    private ArrayList<MeetingHistory> arrMeetingHistory = new ArrayList<>();
    private DatabaseManager databaseManager ;
    private DatabaseReference databaseReferenceMeetingHistory;
    private User userBean;

    private TextView txtUserName;
    private CircularImageView imgUser;
    private LinearLayout llError;
    private RecyclerView rvHistory;

    private String[] appPermissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private static final int PERMISSION_REQUEST_CODE = 10001;
    private static final int SETTINGS_REQUEST_CODE = 10002;


    public HomeFragment() {
        userBean = Constants.gUserMe;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        view = inflater.inflate(R.layout.fragment_vmeet_home, container, false);

        initWithView(view);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        databaseManager = new DatabaseManager();
        databaseManager.setDatabaseManagerListener(this);
        setUserData();

        databaseReferenceMeetingHistory = FirebaseDatabase.getInstance().getReference(Helper.REF_DATA).child(AppConstants.Table.MEETING_HISTORY);

        rvHistory.setNestedScrollingEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkAppPermissions(appPermissions)) {
                requestAppPermissions(appPermissions);
            }
        }

        return view;
    }

    private void initWithView(View view) {
        txtUserName = view.findViewById(R.id.txtUserName);
        imgUser = view.findViewById(R.id.imgUser);
        LinearLayout llJoin = view.findViewById(R.id.llJoin);
        llJoin.setOnClickListener(this);
        LinearLayout llSchedule = view.findViewById(R.id.llSchedule);
        llSchedule.setOnClickListener(this);
        LinearLayout llNewMeeting = view.findViewById(R.id.llNewMeeting);
        llNewMeeting.setOnClickListener(this);
        llError = view.findViewById(R.id.llError);
        rvHistory = view.findViewById(R.id.rvHistory);
    }

    private boolean isMeetingExist = false;

    private void checkMeetingExists(final String meeting_id) {
        isMeetingExist = false;
        Query query = databaseReferenceMeetingHistory.orderByChild("meeting_id").equalTo(meeting_id);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(MeetingHistory.class).getMeeting_id().equals(meeting_id)) {
                            isMeetingExist = true;
                        }
                    }
                } else {
                    isMeetingExist = false;
                }
            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
                isMeetingExist = false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void setUserData() {
        if (userBean != null) {
            if (!TextUtils.isEmpty(userBean.getImage())) {
                Picasso.get().load(userBean.getImage())
                        .error(R.drawable.ic_avatar).into(imgUser);
            } else {
                imgUser.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_avatar));
            }

            if (!TextUtils.isEmpty(userBean.getName())) {
                txtUserName.setText("Hi, " + userBean.getName());
            } else {
                txtUserName.setText("Hi, ");
            }
            databaseManager.getMeetingHistoryByUser(userBean.getId());
        } else {
            txtUserName.setText("Hi, ");
            imgUser.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_avatar));
        }
    }

    @Override
    public void onDataChanged(String url, DataSnapshot dataSnapshot) {
        if (url.equalsIgnoreCase(AppConstants.Table.MEETING_HISTORY)){
            if (HomeFragment.this.isVisible()){
                arrMeetingHistory = new ArrayList<>();
                if (databaseManager.getUserMeetingHistory().size() > 0){
                    for (int i = 0; i < databaseManager.getUserMeetingHistory().size(); i++) {
                        MeetingHistory bean = databaseManager.getUserMeetingHistory().get(i);
                        if (!TextUtils.isEmpty(bean.getStartTime())){
                            String date = SharedObjects.convertDateFormat(bean.getStartTime()
                                    ,AppConstants.DateFormats.DATETIME_FORMAT_24,AppConstants.DateFormats.DATE_FORMAT_DD_MMM_YYYY);
                            if (date.equalsIgnoreCase(SharedObjects.getTodaysDate(AppConstants.DateFormats.DATE_FORMAT_DD_MMM_YYYY))){
                                arrMeetingHistory.add(bean);
                            }
                        }
                    }
                }
                setMeetingHistoryAdapter();
            }
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        arrMeetingHistory = new ArrayList<>();
        setMeetingHistoryAdapter();
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

            MeetingHistoryAdapter meetingHistoryAdapter = new MeetingHistoryAdapter(arrMeetingHistory, getActivity());
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

    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.llJoin:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkAppPermissions(appPermissions)) {
                        showMeetingCodeDialog();
                    } else {
                        requestAppPermissions(appPermissions);
                    }
                } else {
                    showMeetingCodeDialog();
                }
                break;
            case R.id.llNewMeeting:
                AppConstants.MEETING_ID = AppConstants.getMeetingCode();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkAppPermissions(appPermissions)) {
                        showMeetingShareDialog();
                    } else {
                        requestAppPermissions(appPermissions);
                    }
                } else {
                    showMeetingShareDialog();
                }
                break;
            case R.id.llSchedule:
                startActivity(new Intent(getActivity(), ScheduleMeetingActivity.class));
                break;
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

        imgShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Vmeet Link");
            intent.putExtra(Intent.EXTRA_TEXT, txtMeetingURL.getText().toString());
            getActivity().startActivity(Intent.createChooser(intent, "Share Via"));
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

    private TextInputLayout inputLayoutCode, inputLayoutName;
    private TextInputEditText edtCode, edtName;

    private void showMeetingCodeDialog() {
        final Dialog dialogDate = new Dialog(getActivity());
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogDate.setContentView(R.layout.dialog_meeting_code);
        dialogDate.setCancelable(true);

        Window window = dialogDate.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        wlp.dimAmount = 0.8f;
        window.setAttributes(wlp);
        dialogDate.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        inputLayoutCode = dialogDate.findViewById(R.id.inputLayoutCode);
        inputLayoutName = dialogDate.findViewById(R.id.inputLayoutName);
        edtCode = dialogDate.findViewById(R.id.edtCode);
        edtName = dialogDate.findViewById(R.id.edtName);

        inputLayoutName.setEnabled(false);
        edtName.setEnabled(false);

        edtName.setText(userBean.getName());

        edtCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutCode.setErrorEnabled(false);
                inputLayoutCode.setError("");
                if (charSequence.length() == 11){
                    checkMeetingExists(charSequence.toString());
                }else{
                    isMeetingExist = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutName.setErrorEnabled(false);
                inputLayoutName.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        Button btnAdd = dialogDate.findViewById(R.id.btnAdd);
        Button btnCancel = dialogDate.findViewById(R.id.btnCancel);

        btnAdd.setOnClickListener(v -> {
            if (TextUtils.isEmpty(edtCode.getText().toString().trim())) {
                inputLayoutCode.setErrorEnabled(true);
                inputLayoutCode.setError(getString(R.string.errMeetingCode));
                return;
            }
            if (edtCode.getText().toString().length() < 11) {
                inputLayoutCode.setErrorEnabled(true);
                inputLayoutCode.setError(getString(R.string.errMeetingCodeInValid));
                return;
            }
            if (!isMeetingExist){
                Toast.makeText(getContext(), R.string.meeting_not_exist, Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(edtName.getText().toString().trim())) {
                inputLayoutName.setErrorEnabled(true);
                inputLayoutName.setError(getString(R.string.err_name));
                return;
            }
            AppConstants.MEETING_ID = edtCode.getText().toString().trim();
            AppConstants.NAME = edtName.getText().toString().trim();

            dialogDate.dismiss();

            startActivity(new Intent(getActivity(), MeetingActivity.class));
        });

        btnCancel.setOnClickListener(view -> dialogDate.dismiss());

        if (!dialogDate.isShowing()) {
            dialogDate.show();
        }
    }

    private boolean checkAppPermissions(String[] appPermissions) {
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
        switch (requestCode) {
            case SETTINGS_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    //
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
