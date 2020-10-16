package com.laodev.chatapp.viewHolders;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.ImageViewerActivity;
import com.laodev.chatapp.interfaces.OnMessageItemClick;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Helper;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mayank on 11/5/17.
 */

public class MessageAttachmentImageViewHolder extends BaseMessageViewHolder {

    private ImageView image;
    private ProgressBar myProgressBar;
    private ImageView statusImg;
    private RelativeLayout statusLay;
    private TextView statusText;
    private ArrayList<Message> messages;
    private LinearLayout backGround;
    private ImageView user_image;

    public MessageAttachmentImageViewHolder(View itemView, OnMessageItemClick itemClickListener, ArrayList<Message> messages) {
        super(itemView, itemClickListener, messages);
        image = itemView.findViewById(R.id.image);
        myProgressBar = itemView.findViewById(R.id.progressBar);
        statusImg = itemView.findViewById(R.id.statusImg);
        statusLay = itemView.findViewById(R.id.statusLay);
        statusText = itemView.findViewById(R.id.statusText);
        backGround = itemView.findViewById(R.id.backGround);
        user_image = itemView.findViewById(R.id.user_image);
        this.messages = messages;

        itemView.setOnClickListener(v -> onItemClick(true));

        itemView.setOnLongClickListener(v -> {
            onItemClick(false);
            return true;
        });

        image.setOnLongClickListener(v -> {
            onItemClick(false);
            return true;
        });
    }

    @Override
    public void setData(final Message message, int position, HashMap<String, User> myUsers, ArrayList<User> myUsersList) {
        super.setData(message, position, myUsers, myUsersList);

        if (isMine()) {
            backGround.setBackgroundResource(R.drawable.shape_incoming_message);
            senderName.setVisibility(View.GONE);
            senderName.setTextColor(context.getColor(R.color.textColorWhite));
            user_image.setVisibility(View.GONE);
        } else {
            backGround.setBackgroundResource(R.drawable.shape_outgoing_message);
            senderName.setTextColor(context.getColor(R.color.textColor4));
            user_image.setVisibility(View.VISIBLE);
            try {
                if (myUsers.get(message.getSenderId()) == null) {
                    BaseApplication.getUserRef().child(message.getSenderId()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User user = snapshot.getValue(User.class);
                            myUsers.put(message.getSenderId(), user);
                            Picasso.get()
                                    .load(myUsers.get(message.getSenderId()).getImage())
                                    .tag(context)
                                    .placeholder(R.drawable.ic_avatar)
                                    .error(R.drawable.ic_avatar)
                                    .into(user_image);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Picasso.get()
                                    .load(error.getMessage())
                                    .tag(context)
                                    .placeholder(R.drawable.ic_avatar)
                                    .error(R.drawable.ic_avatar)
                                    .into(user_image);
                        }
                    });
                } else {
                    Picasso.get()
                            .load(myUsers.get(message.getSenderId()).getImage())
                            .tag(context)
                            .placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.ic_avatar)
                            .into(user_image);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Picasso.get()
                .load(message.getAttachment().getUrl())
                .tag(context)
                .placeholder(R.drawable.ic_placeholder)
                .into(image, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        myProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        myProgressBar.setVisibility(View.VISIBLE);
                    }
                });

        image.setOnClickListener(v -> {
            if (!Helper.CHAT_CAB)
                context.startActivity(ImageViewerActivity.newInstance(context, message.getAttachment().getUrl()));
        });

        if (message.getStatusUrl() != null && !message.getStatusUrl().isEmpty()) {
            statusLay.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(message.getStatusUrl())
                    .tag(context)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(statusImg);
            statusText.setText("Status");
        } else if (message.getReplyId() != null && !message.getReplyId().equalsIgnoreCase("0")) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getId() != null &&
                        messages.get(i).getId().equalsIgnoreCase(message.getReplyId())) {
                    statusLay.setVisibility(View.VISIBLE);
                    Message message1 = messages.get(i);
                    if (message1.getAttachmentType() == AttachmentTypes.AUDIO) {
                        Picasso.get()
                                .load(R.drawable.ic_audiotrack_24dp)
                                .tag(context)
                                .placeholder(R.drawable.ic_audiotrack_24dp)
                                .into(statusImg);
                        statusText.setText("Audio");
                    } else if (message1.getAttachmentType() == AttachmentTypes.RECORDING) {
                        Picasso.get()
                                .load(R.drawable.ic_audiotrack_24dp)
                                .tag(context)
                                .placeholder(R.drawable.ic_audiotrack_24dp)
                                .into(statusImg);
                        statusText.setText("Recording");
                    } else if (message1.getAttachmentType() == AttachmentTypes.VIDEO) {
                        if (message1.getAttachment().getData() != null) {
                            Picasso.get()
                                    .load(message1.getAttachment().getData())
                                    .tag(context)
                                    .placeholder(R.drawable.ic_placeholder)
                                    .into(statusImg);
                            statusText.setText("Video");
                        } else
                            statusImg.setBackgroundResource(R.drawable.ic_placeholder);
                        //replyName.setText(message1.getAttachment().getName());
                    } else if (message1.getAttachmentType() == AttachmentTypes.IMAGE) {
                        if (message1.getAttachment().getUrl() != null) {
                            Picasso.get()
                                    .load(message1.getAttachment().getUrl())
                                    .tag(context)
                                    .placeholder(R.drawable.ic_placeholder)
                                    .into(statusImg);
                            statusText.setText("Image");
                        } else
                            statusImg.setBackgroundResource(R.drawable.ic_placeholder);
                    } else if (message1.getAttachmentType() == AttachmentTypes.CONTACT) {
                        Picasso.get()
                                .load(R.drawable.ic_person_black_24dp)
                                .tag(context)
                                .placeholder(R.drawable.ic_person_black_24dp)
                                .into(statusImg);
                        statusText.setText("Contact");
                    } else if (message1.getAttachmentType() == AttachmentTypes.LOCATION) {
                        try {
                            String staticMap = "https://maps.googleapis.com/maps/api/staticmap?center=%s,%s&zoom=16&size=512x512&format=png";
                            String Key = "&key="+"AIzaSyCB7xTbA6NzO0V0JWtOGo0cFcrKP-9DTrY";
                            String latitude, longitude;
                            JSONObject placeData = new JSONObject(message1.getAttachment().getData());
                            statusText.setText(placeData.getString("address"));
                            latitude = placeData.getString("latitude");
                            longitude = placeData.getString("longitude");
                            Picasso.get()
                                    .load(String.format(staticMap, latitude, longitude) + Key)
                                    .tag(context)
                                    .into(statusImg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (message1.getAttachmentType() == AttachmentTypes.DOCUMENT) {
                        Picasso.get()
                                .load(R.drawable.ic_insert_64dp)
                                .tag(context)
                                .placeholder(R.drawable.ic_insert_64dp)
                                .into(statusImg);
                        statusText.setText("Document");
                    } else if (message1.getAttachmentType() == AttachmentTypes.NONE_TEXT) {
                        statusText.setText(message1.getBody());
                        statusImg.setVisibility(View.GONE);
                    }
                }
            }
        } else {
            statusLay.setVisibility(View.GONE);
        }

    }
}
