package com.laodev.chatapp.adapters;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.CallScreenActivity;
import com.laodev.chatapp.activities.MainActivity;
import com.laodev.chatapp.interfaces.OnUserGroupItemClick;
import com.laodev.chatapp.models.LogCall;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Helper;
import com.sinch.android.rtc.calling.Call;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

public class LogCallAdapter extends RecyclerView.Adapter<LogCallAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<LogCall> dataList;
    private ArrayList<User> myUsers;
    private User userMe, user;
    private FragmentManager manager;
    private Helper helper;
    private String[] permissionsSinch = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_PHONE_STATE};

    public LogCallAdapter(Context context, ArrayList<LogCall> dataList, ArrayList<User> myUsers,
                          User loggedInUser, FragmentManager manager, Helper helper) {
        this.context = context;
        this.dataList = dataList;
        this.userMe = loggedInUser;
        this.myUsers = myUsers;
        this.manager = manager;
        this.helper = helper;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_item_log_call, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.setData(dataList.get(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView userImage, aCallLogImg, callTypeImg;
        private TextView time, duration, userName;

        MyViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.userImage);
            time = itemView.findViewById(R.id.time);
            duration = itemView.findViewById(R.id.duration);
            userName = itemView.findViewById(R.id.userName);
            aCallLogImg = itemView.findViewById(R.id.img_calllog);
            callTypeImg = itemView.findViewById(R.id.callTypeImg);
        }

        public void setData(final LogCall logCall) {
            if (myUsers.size() != 0) {
                for (int i = 0; i < myUsers.size(); i++) {
                    if (myUsers.get(i).getId().equalsIgnoreCase(logCall.getUserId()))
                        user = myUsers.get(i);
                }
                if (user.getImage() != null && !user.getImage().isEmpty())
                    if (user.getBlockedUsersIds() != null && !user.getBlockedUsersIds().contains(MainActivity.userId))
                        Picasso.get()
                                .load(user.getImage())
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage);
                    else
                        Picasso.get()
                                .load(R.drawable.ic_avatar)
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage);
                else
                    Picasso.get()
                            .load(R.drawable.ic_avatar)
                            .tag(this)
                            .error(R.drawable.ic_avatar)
                            .placeholder(R.drawable.ic_avatar)
                            .into(userImage);
            }

            userName.setText(logCall.getUser().getNameToDisplay());
            time.setText(Helper.getDateTime(logCall.getTimeUpdated()));
            if (logCall.getStatus().equalsIgnoreCase("CANCELED")) {
                aCallLogImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_missed_24dp));
            } else if (logCall.getStatus().equalsIgnoreCase("DENIED")) {
                aCallLogImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_missed_24dp));
            } else if (logCall.getStatus().equalsIgnoreCase("IN")) {
                aCallLogImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_in));
            } else if (logCall.getStatus().equalsIgnoreCase("OUT")) {
                aCallLogImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_out));
            }

            duration.setText(formatTimespan(logCall.getTimeDuration()));

            if (logCall.isVideo()) {
                callTypeImg.setBackgroundResource(R.drawable.ic_videocam_white_24dp);
            } else {
                callTypeImg.setBackgroundResource(R.drawable.ic_call_white_24dp);
            }

            callTypeImg.setOnClickListener(v -> {
                if (logCall.isVideo()) {
                    makeCall(true, logCall.getUserId(), user);
                } else {
                    makeCall(false, logCall.getUserId(), user);
                }
            });
        }

        private String formatTimespan(int totalSeconds) {
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private void makeCall(boolean b, String userId, User user) {
        if (user != null && userMe != null && userMe.getBlockedUsersIds() != null
                && userMe.getBlockedUsersIds().contains(user.getId())) {
            Helper.unBlockAlert(user.getNameToDisplay(), userMe, context,
                    helper, user.getId(), manager);
        } else
            placeCall(b, userId);
    }

    private void placeCall(boolean isVideoCall, String userId) {
        if (permissionsAvailable(permissionsSinch)) {
            try {
                Call call = isVideoCall ? ((MainActivity) context).getSinchRef().callUserVideo(userId)
                        : ((MainActivity) context).getSinchRef().callUser(userId);
                if (call == null) {
                    // Service failed for some reason, show a Toast and abort
                    Toast.makeText(context, "Service is not started. Try stopping the service and starting it again before placing a call.", Toast.LENGTH_LONG).show();
                    return;
                }
                String callId = call.getCallId();

                for (User user : MainActivity.myUsers) {
                    if (user != null && user.getId() != null && user.getId().equalsIgnoreCase(userId)) {
                        context.startActivity(CallScreenActivity.newIntent(context, user, callId, "OUT"));
                    }
                }

            } catch (Exception ignored) { }
        } else {
            ActivityCompat.requestPermissions((Activity) context, permissionsSinch, 69);
        }
    }

    private boolean permissionsAvailable(String[] permissions) {
        boolean granted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        return granted;
    }
}
