package com.laodev.chatapp.viewHolders;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.interfaces.OnMessageItemClick;
import com.laodev.chatapp.models.Attachment;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.FileUtils;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.utils.MyFileProvider;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by mayank on 11/5/17.
 */

public class MessageAttachmentAudioViewHolder extends BaseMessageViewHolder {

    private TextView text;
    private TextView durationOrSize;
    private ProgressBar progressBar;
    private ImageView playPauseToggle;
    private Message message;
    private File file;
    private ImageView statusImg;
    private RelativeLayout statusLay;
    private TextView statusText;
    private ArrayList<Message> messages;
    private LinearLayout backGround;
    private ImageView user_image;

    public MessageAttachmentAudioViewHolder(View itemView, OnMessageItemClick itemClickListener, ArrayList<Message> messages) {
        super(itemView, itemClickListener, messages);
        text = itemView.findViewById(R.id.text);
        durationOrSize = itemView.findViewById(R.id.duration);
        progressBar = itemView.findViewById(R.id.progressBar);
        playPauseToggle = itemView.findViewById(R.id.playPauseToggle);
        statusImg = itemView.findViewById(R.id.statusImg);
        statusLay = itemView.findViewById(R.id.statusLay);
        statusText = itemView.findViewById(R.id.statusText);
        backGround = itemView.findViewById(R.id.backGround);
        user_image = itemView.findViewById(R.id.user_image);
        this.messages = messages;
        itemView.setOnClickListener(v -> {
            if (!Helper.CHAT_CAB)
                downloadFile();
            onItemClick(true);
        });

        itemView.setOnLongClickListener(v -> {
            onItemClick(false);
            return true;
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void setData(Message message, int position, HashMap<String, User> myUsers, ArrayList<User> myUsersList) {
        super.setData(message, position, myUsers, myUsersList);
        if (isMine()) {
            backGround.setBackgroundResource(R.drawable.shape_incoming_message);
            text.setTextColor(context.getColor(R.color.textColorWhite));
            durationOrSize.setTextColor(context.getColor(R.color.textColorWhite));
            senderName.setVisibility(View.GONE);
            senderName.setTextColor(context.getColor(R.color.textColorWhite));
            user_image.setVisibility(View.GONE);
        } else {
            backGround.setBackgroundResource(R.drawable.shape_outgoing_message);
//            senderName.setVisibility(View.VISIBLE);
            text.setTextColor(context.getColor(R.color.colorPrimary));
            senderName.setTextColor(context.getColor(R.color.textColor4));
            durationOrSize.setTextColor(context.getColor(R.color.colorPrimary));
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
        // cardView.setCardBackgroundColor(ContextCompat.getColor(context, message.isSelected() ? R.color.colorPrimary : R.color.colorBgLight));
        // ll.setBackgroundColor(message.isSelected() ? ContextCompat.getColor(context, R.color.colorPrimary) : isMine() ? Color.WHITE : ContextCompat.getColor(context, R.color.colorBgLight));
        Attachment attachment = message.getAttachment();
        this.message = message;

        boolean loading = message.getAttachment().getUrl().equals("loading");
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        playPauseToggle.setVisibility(loading ? View.GONE : View.VISIBLE);

        file = new File(Environment.getExternalStorageDirectory() + "/"
                +
                context.getString(R.string.app_name) + "/" + AttachmentTypes.getTypeName(message.getAttachmentType()) + (isMine() ? "/.sent/" : "")
                , message.getAttachment().getName());
        if (file.exists()) {
            Uri uri = Uri.fromFile(file);
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(context, uri);
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                int millis = Integer.parseInt(durationStr);
                durationOrSize.setText(TimeUnit.MILLISECONDS.toMinutes(millis) + ":" + TimeUnit.MILLISECONDS.toSeconds(millis));
                mmr.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            durationOrSize.setText(FileUtils.getReadableFileSize(attachment.getBytesCount()));
        text.setText(message.getAttachment().getName());

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
                            String Key = "&key=" + "AIzaSyCB7xTbA6NzO0V0JWtOGo0cFcrKP-9DTrY";
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

    private void downloadFile() {
        if (file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = MyFileProvider.getUriForFile(context,
                    context.getString(R.string.authority),
                    file);
            intent.setDataAndType(uri, Helper.getMimeType(context, uri)); //storage path is path of your vcf file and vFile is name of that file.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } else if (!isMine() && !message.getAttachment().getUrl().equals("loading")) {
            broadcastDownloadEvent();
        } else {
            Toast.makeText(context, "File unavailable", Toast.LENGTH_SHORT).show();
        }
    }
}
