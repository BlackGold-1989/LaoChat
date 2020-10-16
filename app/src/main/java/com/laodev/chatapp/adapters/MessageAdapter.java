package com.laodev.chatapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.MainActivity;
import com.laodev.chatapp.interfaces.OnMessageItemClick;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.viewHolders.BaseMessageViewHolder;
import com.laodev.chatapp.viewHolders.MessageAttachmentAudioViewHolder;
import com.laodev.chatapp.viewHolders.MessageAttachmentContactViewHolder;
import com.laodev.chatapp.viewHolders.MessageAttachmentDocumentViewHolder;
import com.laodev.chatapp.viewHolders.MessageAttachmentImageViewHolder;
import com.laodev.chatapp.viewHolders.MessageAttachmentLocationViewHolder;
import com.laodev.chatapp.viewHolders.MessageAttachmentRecordingViewHolder;
import com.laodev.chatapp.viewHolders.MessageAttachmentVideoViewHolder;
import com.laodev.chatapp.viewHolders.MessageTextViewHolder;
import com.laodev.chatapp.viewHolders.MessageTypingViewHolder;

import java.util.ArrayList;
import java.util.HashMap;

public class MessageAdapter extends RecyclerView.Adapter<BaseMessageViewHolder> {

    private OnMessageItemClick itemClickListener;
    private MessageAttachmentRecordingViewHolder.RecordingViewInteractor recordingViewInteractor;
    private String myId;
    private Context context;
    private ArrayList<Message> messages;
    private View newMessage;
    private HashMap<String, User> myUsersNameInPhoneMap;

    private static final int MY = 0x00000000;
    public static final int OTHER = 0x0000100;

    public MessageAdapter(Context context, ArrayList<Message> messages, String myId, View newMessage) {
        this.context = context;
        this.messages = messages;
        this.myId = myId;
        this.newMessage = newMessage;
        Helper helper = new Helper(context);
        this.myUsersNameInPhoneMap = helper.getCacheMyUsers();

        if (context instanceof OnMessageItemClick) {
            this.itemClickListener = (OnMessageItemClick) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnUserGroupItemClick");
        }

        if (context instanceof MessageAttachmentRecordingViewHolder.RecordingViewInteractor) {
            this.recordingViewInteractor = (MessageAttachmentRecordingViewHolder.RecordingViewInteractor) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement RecordingViewInteractor");
        }
    }

    @NonNull
    @Override
    public BaseMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        viewType &= 0x00000FF;
        switch (viewType) {
            case AttachmentTypes.RECORDING:
                return new MessageAttachmentRecordingViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_recording, parent, false), itemClickListener, recordingViewInteractor, messages);
            case AttachmentTypes.AUDIO:
                return new MessageAttachmentAudioViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_audio, parent, false), itemClickListener, messages);
            case AttachmentTypes.CONTACT:
                return new MessageAttachmentContactViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_contact, parent, false), itemClickListener, messages);
            case AttachmentTypes.DOCUMENT:
                return new MessageAttachmentDocumentViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_document, parent, false), itemClickListener, messages);
            case AttachmentTypes.IMAGE:
                return new MessageAttachmentImageViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_image, parent, false), itemClickListener, messages);
            case AttachmentTypes.LOCATION:
                return new MessageAttachmentLocationViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_location, parent, false), itemClickListener, messages);
            case AttachmentTypes.VIDEO:
                return new MessageAttachmentVideoViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_video, parent, false), itemClickListener, messages);
            case AttachmentTypes.NONE_TYPING:
                return new MessageTypingViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_typing, parent, false));
            case AttachmentTypes.NONE_TEXT:
            default:
                return new MessageTextViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_text, parent, false), newMessage, itemClickListener, messages);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseMessageViewHolder holder, int position) {
        try {
            holder.setData(messages.get(position), position, myUsersNameInPhoneMap, MainActivity.myUsers);

            if (messages.get(position).isSelected())
                holder.parentLayout.setBackgroundColor(Color.parseColor("#d2b2e5"));
            else
                holder.parentLayout.setBackgroundColor(Color.parseColor("#00000000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemViewType(int position) {
        try {
            if (getItemCount() == 0) {
                return super.getItemViewType(position);
            } else {
                Message message = messages.get(position);
                int userType;
                if (message.getSenderId().equals(myId))
                    userType = MY;
                else
                    userType = OTHER;
                return message.getAttachmentType() | userType;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
