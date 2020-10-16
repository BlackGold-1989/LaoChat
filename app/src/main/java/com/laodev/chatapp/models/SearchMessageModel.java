package com.laodev.chatapp.models;

public class SearchMessageModel {

    private String profileImg;
    private String name;
    private String lastMessage;
    private String messageId;
    private long time;
    @AttachmentTypes.AttachmentType
    private int attachmentType;
    private User user;
    private Group group;


    public SearchMessageModel(String profileImg, String name, String lastMessage, String messageId, long time, int attachmentType,User user, Group group) {
        this.profileImg = profileImg;
        this.name = name;
        this.lastMessage = lastMessage;
        this.messageId = messageId;
        this.time = time;
        this.attachmentType = attachmentType;
        this.user = user;
        this.group = group;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getProfileImg() {
        return profileImg;
    }

    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(int attachmentType) {
        this.attachmentType = attachmentType;
    }
}
