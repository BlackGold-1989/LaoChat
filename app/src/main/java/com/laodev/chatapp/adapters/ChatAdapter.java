package com.laodev.chatapp.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.interfaces.ContextualModeInteractor;
import com.laodev.chatapp.interfaces.OnUserGroupItemClick;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Chat;
import com.laodev.chatapp.models.Group;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Helper;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import io.realm.RealmList;

/**
 * Created by a_man on 5/10/2017.
 */

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<Chat> dataList;
    private OnUserGroupItemClick itemClickListener;
    private ContextualModeInteractor contextualModeInteractor;
    private int selectedCount = 0;
    private String userId, from;
    private ImageView groupImg1, groupImg2, groupImg3, groupImg4;
    private TextView groupImgCount;
    protected Helper helper;
    private HashMap<String, User> myUsersNameInPhoneMap;

    public ChatAdapter(Context context, ArrayList<Chat> dataList, String userId, String from) {
        this.context = context;
        this.dataList = dataList;
        this.userId = userId;
        this.from = from;
        this.helper = new Helper(context);
        this.myUsersNameInPhoneMap = helper.getCacheMyUsers();
        if (context instanceof OnUserGroupItemClick) {
            this.itemClickListener = (OnUserGroupItemClick) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnUserGroupItemClick");
        }

        if (context instanceof ContextualModeInteractor) {
            this.contextualModeInteractor = (ContextualModeInteractor) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ContextualModeInteractor");
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.setData(dataList.get(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView status, name, lastMessage, time, msgCount, msgCountGroup;
        private ImageView image, myUserImageOnline, img, readImg;


        MyViewHolder(View itemView) {
            super(itemView);
            status = itemView.findViewById(R.id.emotion);
            name = itemView.findViewById(R.id.user_name);
            time = itemView.findViewById(R.id.time);
            readImg = itemView.findViewById(R.id.readImg);
            lastMessage = itemView.findViewById(R.id.message);
            image = itemView.findViewById(R.id.user_image);
            msgCount = itemView.findViewById(R.id.msgCount);
            msgCountGroup = itemView.findViewById(R.id.msgCountGroup);
            img = itemView.findViewById(R.id.img);
            RelativeLayout user_details_container = itemView.findViewById(R.id.user_details_container);
            myUserImageOnline = itemView.findViewById(R.id.user_image_online);

            groupImg1 = itemView.findViewById(R.id.groupImg1);
            groupImg2 = itemView.findViewById(R.id.groupImg2);
            groupImg3 = itemView.findViewById(R.id.groupImg3);
            groupImg4 = itemView.findViewById(R.id.groupImg4);
            groupImgCount = itemView.findViewById(R.id.groupImgCount);

            user_details_container.setOnClickListener(v -> {
                if (contextualModeInteractor.isContextualMode()) {
                    toggleSelection(dataList.get(getAdapterPosition()), getAdapterPosition());
                } else {
                    int pos = getAdapterPosition();
                    if (pos != -1) {
                        Chat chat = dataList.get(pos);
                        if (chat.getUser() != null)
                            itemClickListener.OnUserClick(chat.getUser(), pos, image);
                        else if (chat.getGroup() != null)
                            itemClickListener.OnGroupClick(chat.getGroup(), pos, image);
                    }
                }
            });
            user_details_container.setOnLongClickListener(view -> {
                contextualModeInteractor.enableContextualMode();
                toggleSelection(dataList.get(getAdapterPosition()), getAdapterPosition());
                return true;
            });
        }

        private void showDialog(final String image, String profileName) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View dialogView = li.inflate(R.layout.dialog_image, null);
            ImageView profileImg = dialogView.findViewById(R.id.profileImg);
            TextView name = dialogView.findViewById(R.id.name);
            name.setText(profileName);
            if (!image.isEmpty()) {
                Picasso.get()
                        .load(image)
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .into(profileImg);
            } else {
                Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .tag(this)
                        .error(R.drawable.ic_avatar)
                        .placeholder(R.drawable.ic_avatar)
                        .into(profileImg);
            }
            builder.setView(dialogView).create().show();
        }

        private void setData(final Chat chat) {
            final User chatUser = chat.getUser();
            final Group chatGroup = chat.getGroup();
            RealmList<Message> message = chat.getMessages();
            if (chatUser != null && chatUser.getImage() != null && !chatUser.getImage().equalsIgnoreCase("")) {
                if (chatUser.getBlockedUsersIds() != null && !chatUser.getBlockedUsersIds().contains(userId))
                    Picasso.get()
                            .load(chatUser.getImage())
                            .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                            .centerInside()
                            .into(image);
                else
                    Picasso.get()
                            .load(R.drawable.ic_avatar)
                            .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                            .centerInside()
                            .into(image);

            } else if (chatGroup != null && chatGroup.getImage() != null &&
                    !chatGroup.getImage().equalsIgnoreCase("")) {
                Picasso.get()
                        .load(chatGroup.getImage())
                        .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                        .centerInside()
                        .into(image);
            } else {
                Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                        .centerInside()
                        .into(image);
            }

            image.setOnClickListener(v -> {
                String profileName = name.getText().toString();
                if (chatUser != null && chatUser.getImage() != null && !chatUser.getImage().equalsIgnoreCase("")) {
                    if (chatUser.getBlockedUsersIds() != null && !chatUser.getBlockedUsersIds().contains(userId))
                        showDialog(chatUser.getImage(), profileName);
                } else if (chatGroup != null && chatGroup.getImage() != null && !chatGroup.getImage().equalsIgnoreCase("")) {
                    showDialog(chatGroup.getImage(), profileName);
                }
            });

            name.setText(chatUser != null ? chatUser.getNameToDisplay() : chatGroup.getName());
            if (from.equalsIgnoreCase("group")
                    && !chat.isRead()
                    && chatGroup.getUserIds().contains(userId)
                    && !chatGroup.getGrpExitUserIds().contains(userId)) {
                msgCountGroup.setVisibility(View.VISIBLE);
            } else {
                msgCountGroup.setVisibility(View.GONE);
            }

            status.setText(chatUser != null ? chatUser.getStatus() : chatGroup.getStatus());
            if (chat.getTimeUpdated() == 0) {
                time.setVisibility(View.GONE);
            } else {
                time.setVisibility(View.VISIBLE);
                time.setText(Helper.getChatFormattedDate(chat.getTimeUpdated()));
            }
            if (chatUser != null) {
                lastMessage.setText(chat.getLastMessage());
            } else {
                if (message.size() > 0) {
                    lastMessage.setText(chat.getLastMessage());
                } else {
                    try {
                        lastMessage.setText(context.getString(R.string.create_on) + Helper.getDateTime(chatGroup.getDate()));
                    } catch (Exception e) {
                        lastMessage.setText("");
                    }
                }
            }

            lastMessage.setTextColor(ContextCompat.getColor(context, !chat.isRead() ? R.color.textColorPrimary : R.color.textColorSecondary));

            try {
                if (chatUser != null && chatUser.isOnline()) {
                    myUserImageOnline.setVisibility(View.VISIBLE);
                    lastMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, chatUser.isOnline() ? R.drawable.ring_green : 0, 0);
                } else {
                    myUserImageOnline.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (message != null && message.size() > 0) {
                    if (message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.AUDIO && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_audiotrack_gray);
                        lastMessage.setText(context.getString(R.string.audio));
                    } else if (message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.RECORDING && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_audiotrack_gray);
                        lastMessage.setText(context.getString(R.string.recording));
                    } else if (message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.VIDEO && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_videocam_gray);
                        lastMessage.setText(context.getString(R.string.video));
                    } else if (message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.IMAGE && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_wallpaper_gray);
                        lastMessage.setText(context.getString(R.string.image));
                    } else if (message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.CONTACT && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_contact_gray);
                        lastMessage.setText(context.getString(R.string.contact));
                    } else if (message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.LOCATION && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_location_gray);
                        lastMessage.setText(context.getString(R.string.location));
                    } else if (message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.DOCUMENT && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_insert_gray);
                        lastMessage.setText(context.getString(R.string.document));
                    } else if (message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.NONE_TEXT && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.GONE);
                    }
                    try {
                        if (!from.equalsIgnoreCase("group")) {
                            if (!message.get(message.size() - 1).getRecipientId().equalsIgnoreCase(userId)
                                    && message.get(message.size() - 1).getDelete().isEmpty()) {
                                readImg.setVisibility(View.VISIBLE);
                                readImg.setBackgroundResource(message.get(message.size() - 1).isSent()
                                        ? (message.get(message.size() - 1).isDelivered()
                                        ? (message.get(message.size() - 1).isReadMsg()
                                        ? R.drawable.ic_done_all_blue : R.drawable.ic_done_all_black)
                                        : R.drawable.ic_done_black) : R.drawable.ic_waiting);
                            } else {
                                readImg.setVisibility(View.GONE);
                            }
                            groupImg1.setVisibility(View.GONE);
                            groupImg2.setVisibility(View.GONE);
                            groupImg3.setVisibility(View.GONE);
                            groupImg4.setVisibility(View.GONE);
                            groupImgCount.setVisibility(View.GONE);
                        } else {
                            readImg.setVisibility(View.GONE);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (chatUser != null) {
                        msgCountGroup.setVisibility(View.GONE);
                        int count = 0;
                        for (int i = 0; i < message.size(); i++) {
                            if (!message.get(i).isReadMsg() && !message.get(i).getSenderId().equalsIgnoreCase(userId)
                                    && !message.get(i).isBlocked())
                                count++;
                        }

                        if (count > 99) {
                            msgCount.setVisibility(View.VISIBLE);
                            msgCount.setText("+99");
                        } else if (count > 0) {
                            msgCount.setVisibility(View.VISIBLE);
                            msgCount.setText("" + count);
                        } else {
                            msgCount.setVisibility(View.GONE);
                        }
                    }
                }
                if (from.equalsIgnoreCase("group") && chatGroup != null) {
                    loadImages(chatGroup);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int count = 0;

    private void isCheckUserData(RealmList<String> userIds, GroupChatInfoCheckListener callback) {
        count = 0;
        for (String userid: userIds) {
            if (userid == null) continue;
            User user = myUsersNameInPhoneMap.get(userid);
            if (user == null) {
                BaseApplication.getUserRef().child(userid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User userModel = snapshot.getValue(User.class);
                        if (userModel != null) {
                            myUsersNameInPhoneMap.put(userModel.getId(), userModel);
                        }
                        count++;
                        if (count == userIds.size()) {
                            callback.onSuccessGetAllUserInfos();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        count++;
                        if (count == userIds.size()) {
                            callback.onSuccessGetAllUserInfos();
                        }
                    }
                });
            } else {
                count++;
                if (count == userIds.size()) {
                    callback.onSuccessGetAllUserInfos();
                }
            }
        }
    }

    private interface GroupChatInfoCheckListener {
        void onSuccessGetAllUserInfos();
    }

    private void loadImages(Group chatGroup) {
        RealmList<String> userIds = new RealmList<>();

        for (String ids : chatGroup.getUserIds()) {
            if (!chatGroup.getGrpExitUserIds().contains(ids))
                userIds.add(ids);
        }

        isCheckUserData(userIds, () -> {
            if (userIds.size() > 4) {
                groupImg1.setVisibility(View.VISIBLE);
                groupImg2.setVisibility(View.VISIBLE);
                groupImg3.setVisibility(View.VISIBLE);
                groupImg4.setVisibility(View.VISIBLE);
                groupImgCount.setVisibility(View.VISIBLE);
                groupImgCount.setText(String.valueOf(userIds.size()));

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg1);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(1)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(1)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg2);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(2)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(2)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg3);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(3)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(3)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg4);
            } else if (userIds.size() == 4) {
                groupImg1.setVisibility(View.VISIBLE);
                groupImg2.setVisibility(View.VISIBLE);
                groupImg3.setVisibility(View.VISIBLE);
                groupImg4.setVisibility(View.VISIBLE);
                groupImgCount.setVisibility(View.GONE);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg1);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(1)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(1)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg2);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(2)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(2)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg3);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(3)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(3)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg4);
            } else if (userIds.size() == 3) {
                groupImg1.setVisibility(View.VISIBLE);
                groupImg2.setVisibility(View.VISIBLE);
                groupImg3.setVisibility(View.VISIBLE);
                groupImg4.setVisibility(View.GONE);
                groupImgCount.setVisibility(View.GONE);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg1);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(1)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(1)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg2);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(2)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(2)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg3);
            } else if (userIds.size() == 2) {
                groupImg1.setVisibility(View.VISIBLE);
                groupImg2.setVisibility(View.VISIBLE);
                groupImg3.setVisibility(View.GONE);
                groupImg4.setVisibility(View.GONE);
                groupImgCount.setVisibility(View.GONE);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg1);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(1)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(1)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg2);

            } else if (userIds.size() == 1) {
                groupImg1.setVisibility(View.VISIBLE);
                groupImg2.setVisibility(View.GONE);
                groupImg3.setVisibility(View.GONE);
                groupImg4.setVisibility(View.GONE);
                groupImgCount.setVisibility(View.GONE);

                Picasso.get()
                        .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                                : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(groupImg1);
            }
        });
    }

    private void toggleSelection(Chat chat, int position) {
        chat.setSelected(!chat.isSelected());
        notifyItemChanged(position);

        if (chat.isSelected())
            selectedCount++;
        else
            selectedCount--;

        contextualModeInteractor.updateSelectedCount(selectedCount);
    }

    public void disableContextualMode() {
        selectedCount = 0;
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).isSelected()) {
                dataList.get(i).setSelected(false);
                notifyItemChanged(i);
            }
        }
    }

}
