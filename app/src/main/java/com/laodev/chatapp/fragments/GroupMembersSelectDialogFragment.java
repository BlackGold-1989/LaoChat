package com.laodev.chatapp.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.laodev.chatapp.R;
import com.laodev.chatapp.adapters.GroupNewParticipantsAdapter;
import com.laodev.chatapp.interfaces.UserGroupSelectionDismissListener;
import com.laodev.chatapp.models.User;

import java.util.ArrayList;

/**
 * Created by a_man on 31-12-2017.
 */

public class GroupMembersSelectDialogFragment extends BaseFullDialogFragment implements GroupNewParticipantsAdapter.ParticipantClickListener {

    private ArrayList<User> selectedUsers, myUsers;
    private User userMe;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_select_members, container);
        RecyclerView participants = view.findViewById(R.id.participants);
        RecyclerView myUsersRecycler = view.findViewById(R.id.myUsers);

        participants.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        myUsersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        GroupNewParticipantsAdapter selectedParticipantsAdapter = new GroupNewParticipantsAdapter(this, selectedUsers, userMe);
        participants.setAdapter(selectedParticipantsAdapter);
        myUsersRecycler.setAdapter(new GroupNewParticipantsAdapter(this, myUsers, selectedParticipantsAdapter, userMe));

        view.findViewById(R.id.back).setOnClickListener(view1 -> dismiss());
        view.findViewById(R.id.done).setOnClickListener(view12 -> dismiss());

        return view;
    }

    public static GroupMembersSelectDialogFragment newInstance(UserGroupSelectionDismissListener dismissListener,
                                                               ArrayList<User> selectedUsers, ArrayList<User> myUsers, User loggedInUser) {
        GroupMembersSelectDialogFragment fragment = new GroupMembersSelectDialogFragment();
        fragment.selectedUsers = selectedUsers;
        fragment.myUsers = myUsers;
        fragment.userMe = loggedInUser;
        fragment.dismissListener = dismissListener;
        return fragment;
    }

    @Override
    public void onParticipantClick(int pos, User participant) {

    }
}
