package com.laodev.chatapp.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.laodev.chatapp.R;
import com.laodev.chatapp.adapters.MenuUsersRecyclerAdapter;
import com.laodev.chatapp.interfaces.UserGroupSelectionDismissListener;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.Helper;

import java.util.ArrayList;

/**
 * Created by a_man on 01-12-2017.
 */

public class UserSelectDialogFragment extends BaseFullDialogFragment {

    private EditText query;
    private RecyclerView usersRecycler;
    private ArrayList<User> myUsers;
    private Helper helper;

    public UserSelectDialogFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_select, container);
        TextView heading = view.findViewById(R.id.heading);
        query = view.findViewById(R.id.searchQuery);
        usersRecycler = view.findViewById(R.id.usersRecycler);
        helper = new Helper(getActivity());
        view.findViewById(R.id.close).setOnClickListener(view1 -> dismiss());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        usersRecycler.setAdapter(new MenuUsersRecyclerAdapter(getActivity(), myUsers, helper.getLoggedInUser()));
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (usersRecycler.getAdapter() instanceof MenuUsersRecyclerAdapter) {
                    ((MenuUsersRecyclerAdapter) usersRecycler.getAdapter()).getFilter().filter(editable.toString());
                }
            }
        });
    }

    public void refreshUsers(int pos) {
        if (pos == -1) {
            query.setText("");
            usersRecycler.getAdapter().notifyDataSetChanged();
        } else {
            usersRecycler.getAdapter().notifyItemChanged(pos);
        }
    }

    public static UserSelectDialogFragment newInstance(Context context, ArrayList<User> myUsers) {
        UserSelectDialogFragment dialogFragment = new UserSelectDialogFragment();
        dialogFragment.myUsers = myUsers;
        if (context instanceof UserGroupSelectionDismissListener) {
            dialogFragment.dismissListener = (UserGroupSelectionDismissListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement UserGroupSelectionDismissListener");
        }
        return dialogFragment;
    }

}
