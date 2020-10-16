package com.laodev.chatapp.viewHolders;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.interfaces.OnMessageItemClick;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.GeneralUtils;
import com.laodev.chatapp.utils.LinkTransformationMethod;
import com.squareup.picasso.Picasso;
import com.vanniktech.emoji.EmojiTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mayank on 11/5/17.
 */

public class MessageTextViewHolder extends BaseMessageViewHolder {
    private EmojiTextView text;
    private ImageView statusImg;
    private RelativeLayout statusLay;
    private TextView statusText;
    private ArrayList<Message> messages;
    private LinearLayout backGround;
    private ImageView user_image;

    private static int _4dpInPx = -1;


    public MessageTextViewHolder(View itemView, View newMessageView, OnMessageItemClick itemClickListener, ArrayList<Message> messages) {
        super(itemView, newMessageView, itemClickListener, messages);

        text = itemView.findViewById(R.id.text);
        statusImg = itemView.findViewById(R.id.statusImg);
        statusLay = itemView.findViewById(R.id.statusLay);
        statusText = itemView.findViewById(R.id.statusText);
        backGround = itemView.findViewById(R.id.backGround);
        user_image = itemView.findViewById(R.id.user_image);
        this.messages = messages;
        text.setTransformationMethod(new LinkTransformationMethod());
        text.setMovementMethod(LinkMovementMethod.getInstance());
        if (_4dpInPx == -1) _4dpInPx = GeneralUtils.dpToPx(itemView.getContext(), 4);
        itemView.setOnClickListener(v -> {

            onItemClick(true);
        });

        itemView.setOnLongClickListener(v -> {
            onItemClick(false);
            return true;
        });

        text.setOnLongClickListener(v -> {
            onItemClick(false);
            return true;
        });
    }

    @Override
    public void setData(Message message, int position, HashMap<String, User> myUsers, ArrayList<User> myUsersList) {
        super.setData(message, position, myUsers, myUsersList);

        if (isMine()) {
            backGround.setBackgroundResource(R.drawable.shape_incoming_message);
            senderName.setGravity(Gravity.END);
            senderName.setVisibility(View.GONE);
            text.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.START;
            text.setLayoutParams(lp);
            text.setTextColor(context.getColor(R.color.textColorWhite));
            senderName.setTextColor(context.getColor(R.color.textColor4));
            user_image.setVisibility(View.GONE);
        } else {
            backGround.setBackgroundResource(R.drawable.shape_outgoing_message);
            senderName.setGravity(Gravity.START);
            text.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            text.setTextColor(context.getColor(R.color.colorPrimary));
            senderName.setTextColor(context.getColor(R.color.textColor4));
            user_image.setVisibility(View.VISIBLE);

            BaseApplication.getUserRef().child(message.getSenderId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getImage() != null && !user.getImage().isEmpty()) {
                        Picasso.get()
                            .load(user.getImage())
                            .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                            .error(R.drawable.ic_avatar)
                            .centerInside()
                            .into(user_image);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                        .centerInside()
                        .into(user_image);
                }
            });
        }
        text.setText(message.getBody());
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
                            statusText.setText("Location");
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

    private void animateView(int position) {
        if (animate && position > lastPosition) {
            itemView.post(() -> {
                float originalX = cardView.getX();
                final float originalY = itemView.getY();
                int[] loc = new int[2];
                newMessageView.getLocationOnScreen(loc);
                cardView.setX(loc[0] / 2);
                itemView.setY(loc[1]);
                ValueAnimator radiusAnimator = new ValueAnimator();
                radiusAnimator.setFloatValues(80, _4dpInPx);
                radiusAnimator.setDuration(850);
                radiusAnimator.addUpdateListener(animation -> cardView.setRadius((Float) animation.getAnimatedValue()));
                radiusAnimator.start();
                cardView.animate().x(originalX).setDuration(900).setInterpolator(new DecelerateInterpolator()).start();
                itemView.animate().y(originalY - _4dpInPx).setDuration(750).setInterpolator(new DecelerateInterpolator()).start();
                new Handler().postDelayed(() -> itemView.animate().y(originalY + _4dpInPx).setDuration(250).setInterpolator(new DecelerateInterpolator()).start(), 750);
            });
            lastPosition = position;
            animate = false;
        }
    }

}
