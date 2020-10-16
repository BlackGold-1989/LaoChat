package com.laodev.chatapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.RealmClass;


@RealmClass
public class Message implements Parcelable, RealmModel {
    private String body, senderName, senderId, recipientId, id, statusUrl;
    private long date;
    private boolean delivered = false, sent = false;
    private
    @AttachmentTypes.AttachmentType
    int attachmentType;
    private Attachment attachment;
    private boolean readMsg = false;
    private String replyId;
    private String delete = "";
    private RealmList<String> userIds;
    private RealmList<String> grpDeletedMsgIds;
    private boolean isBlocked = false;

    @Ignore
    private boolean selected;

    public Message() {
    }

    public Message(int attachmentType) {
        this.attachmentType = attachmentType;
        this.senderId = "";
    }

    protected Message(Parcel in) {
        body = in.readString();
        senderName = in.readString();
        senderId = in.readString();
        recipientId = in.readString();
        id = in.readString();
        statusUrl = in.readString();
        replyId = in.readString();
        date = in.readLong();
        delivered = in.readByte() != 0;
        sent = in.readByte() != 0;
        attachmentType = in.readInt();
        attachment = in.readParcelable(Attachment.class.getClassLoader());
        selected = in.readByte() != 0;
        readMsg = in.readByte() != 0;
        delete = in.readString();
        ArrayList<String> userIds1 = in.createStringArrayList();
        userIds = new RealmList<>();
        userIds.addAll(userIds1);
        isBlocked = in.readByte() != 0;

    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;

        Message message = (Message) o;

        return getId() != null ? getId().equals(message.getId()) : message.getId() == null;

    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    @AttachmentTypes.AttachmentType
    public int getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(int attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isReadMsg() {
        return readMsg;
    }

    public void setReadMsg(boolean readMsg) {
        this.readMsg = readMsg;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }

    public String getReplyId() {
        return replyId;
    }

    public void setReplyId(String replyId) {
        this.replyId = replyId;
    }

    public String getDelete() {
        return delete;
    }

    public void setDelete(String delete) {
        this.delete = delete;
    }

    public RealmList<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(ArrayList<String> userIds) {
        this.userIds = new RealmList<>();
        this.userIds.addAll(userIds);
    }

    public RealmList<String> getGrpDeletedMsgIds() {
        return grpDeletedMsgIds;
    }

    public void setGrpDeletedMsgIds(ArrayList<String> grpDeletedMsgIds) {
        this.grpDeletedMsgIds = new RealmList<>();
        this.grpDeletedMsgIds.addAll(grpDeletedMsgIds);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(body);
        parcel.writeString(senderName);
        parcel.writeString(senderId);
        parcel.writeString(recipientId);
        parcel.writeString(id);
        parcel.writeString(statusUrl);
        parcel.writeString(replyId);
        parcel.writeLong(date);
        parcel.writeByte((byte) (delivered ? 1 : 0));
        parcel.writeByte((byte) (sent ? 1 : 0));
        parcel.writeInt(attachmentType);
        parcel.writeParcelable(attachment, i);
        parcel.writeByte((byte) (selected ? 1 : 0));
        parcel.writeByte((byte) (readMsg ? 1 : 0));
        parcel.writeString(delete);
        ArrayList<String> userIds = new ArrayList<>();
        if (userIds != null) {
            userIds.addAll(this.userIds);
            parcel.writeStringList(userIds);
        }
        parcel.writeByte((byte) (isBlocked ? 1 : 0));
    }

    public static boolean validate(Message message) {
        return message != null && message.getId() != null;
    }
    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
}