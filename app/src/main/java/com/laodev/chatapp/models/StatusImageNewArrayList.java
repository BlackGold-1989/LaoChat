package com.laodev.chatapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.annotations.Ignore;


public class StatusImageNewArrayList implements Parcelable {
    private String body, senderName, senderId, id;
    private long date;
    private boolean delivered = false, sent = false;
    private
    @AttachmentTypes.AttachmentType
    int attachmentType;
    private AttachmentArrayList attachment;

    @Ignore
    private boolean selected;

    public StatusImageNewArrayList(int attachmentType) {
        this.attachmentType = attachmentType;
        this.senderId = "";
    }

    private StatusImageNewArrayList(Parcel in) {
        body = in.readString();
        senderName = in.readString();
        senderId = in.readString();
        id = in.readString();
        date = in.readLong();
        delivered = in.readByte() != 0;
        sent = in.readByte() != 0;
        attachmentType = in.readInt();
        attachment = in.readParcelable(Attachment.class.getClassLoader());
        selected = in.readByte() != 0;
    }

    public static final Creator<StatusImageNewArrayList> CREATOR = new Creator<StatusImageNewArrayList>() {
        @Override
        public StatusImageNewArrayList createFromParcel(Parcel in) {
            return new StatusImageNewArrayList(in);
        }

        @Override
        public StatusImageNewArrayList[] newArray(int size) {
            return new StatusImageNewArrayList[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatusImageNewArrayList)) return false;

        StatusImageNewArrayList message = (StatusImageNewArrayList) o;

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

    public AttachmentArrayList getAttachment() {
        return attachment;
    }

    public void setAttachment(AttachmentArrayList attachment) {
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

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(body);
        parcel.writeString(senderName);
        parcel.writeString(senderId);
        parcel.writeString(id);
        parcel.writeLong(date);
        parcel.writeByte((byte) (delivered ? 1 : 0));
        parcel.writeByte((byte) (sent ? 1 : 0));
        parcel.writeInt(attachmentType);
        parcel.writeParcelable(attachment, i);
        parcel.writeByte((byte) (selected ? 1 : 0));
    }
}