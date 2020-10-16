package com.laodev.chatapp.interfaces;

import android.view.View;

import com.laodev.chatapp.models.Group;
import com.laodev.chatapp.models.User;


public interface OnUserGroupItemClick {
    void OnUserClick(User user, int position, View userImage);
    void OnGroupClick(Group group, int position, View userImage);
}
