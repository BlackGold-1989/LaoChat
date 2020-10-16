package com.laodev.chatapp.models;

import java.io.Serializable;

public class CheckableUser implements Serializable {
    private User user;
    private boolean isCheck;

    public CheckableUser(User user) {
        this.user = user;
        isCheck = false;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public boolean search(String newText) {
        if (newText.isEmpty()) {
            return true;
        }
        if (user.getNameInPhone().toLowerCase().contains(newText.toLowerCase())) {
            return true;
        }
        if (user.getStatus().toLowerCase().contains(newText.toLowerCase())) {
            return true;
        }
        return user.getId().contains("+") && user.getId().contains(newText);
    }

}