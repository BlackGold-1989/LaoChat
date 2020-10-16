package com.laodev.chatapp.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.laodev.chatapp.R;
import com.laodev.chatapp.adapters.CallListAdapter;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.Group;
import com.laodev.chatapp.models.Status;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.services.FetchMyUsersService;
import com.laodev.chatapp.utils.Constants;
import com.laodev.chatapp.utils.Helper;
import com.sinch.android.rtc.calling.Call;

import java.util.ArrayList;

public class CallListActivity extends BaseActivity {
    private ArrayList<User> myUsers = new ArrayList<>();
    private SwipeRefreshLayout swipeMenuRecyclerView;
    private SearchView searchView;
    private RecyclerView callListRecyclerView;
    private CallListAdapter callListAdapter;
    private ImageView back_button;
    private TextView title;
    private Helper helper;


    @Override
    void myUsersResult(ArrayList<User> myUsers) {
        helper.setCacheMyUsers(myUsers);
        this.myUsers.clear();
        this.myUsers.addAll(myUsers);
        try {
            callListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        swipeMenuRecyclerView.setRefreshing(false);
    }

    @Override
    void myContactsResult(ArrayList<Contact> myContacts) {

    }

    @Override
    void userAdded(User valueUser) {

    }

    @Override
    void groupAdded(Group valueGroup) {

    }

    @Override
    void userUpdated(User valueUser) {
        if (userMe.getId().equalsIgnoreCase(valueUser.getId())) {
            valueUser.setNameInPhone(helper.getLoggedInUser().getNameInPhone());
            helper.setLoggedInUser(valueUser);
            callListAdapter = new CallListAdapter(CallListActivity.this, MainActivity.myUsers);
            callListRecyclerView.setAdapter(callListAdapter);
        } else {
            int existingPos = MainActivity.myUsers.indexOf(valueUser);
            if (existingPos != -1) {
                valueUser.setNameInPhone(MainActivity.myUsers.get(existingPos).getNameInPhone());
                MainActivity.myUsers.set(existingPos, valueUser);
                helper.setCacheMyUsers(MainActivity.myUsers);
                callListAdapter = new CallListAdapter(CallListActivity.this, MainActivity.myUsers);
                callListRecyclerView.setAdapter(callListAdapter);
            }
        }
    }

    @Override
    void groupUpdated(Group valueGroup) {

    }

    @Override
    void statusAdded(Status status) {

    }

    @Override
    void statusUpdated(Status status) {

    }

    @Override
    void onSinchConnected() {
    }

    @Override
    void onSinchDisconnected() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_list);

        uiInit();

    }

    private void uiInit() {
        helper = new Helper(CallListActivity.this);
        swipeMenuRecyclerView = findViewById(R.id.callListSwipeRefresh);
        searchView = findViewById(R.id.searchView);
        back_button = findViewById(R.id.back_button);
        title = findViewById(R.id.title);
        callListRecyclerView = findViewById(R.id.callListRecyclerView);

        searchView.setIconified(true);
        ImageView searchIcon = searchView.findViewById(R.id.search_button);
        searchIcon.setImageDrawable(ContextCompat.getDrawable(CallListActivity.this, R.drawable.ic_search_white));
        SearchView.SearchAutoComplete searchAutoComplete =
                searchView.findViewById(R.id.search_src_text);
        searchAutoComplete.setHintTextColor(getColor(android.R.color.white));
        searchAutoComplete.setTextColor(getColor(android.R.color.white));

        callListAdapter = new CallListAdapter(CallListActivity.this, myUsers);
        callListRecyclerView.setAdapter(callListAdapter);
        fetchContacts();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                callListAdapter.getFilter().filter(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                callListAdapter.getFilter().filter(s);
                return false;
            }
        });

        searchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                back_button.setVisibility(View.GONE);
                title.setVisibility(View.GONE);
            } else {
                back_button.setVisibility(View.VISIBLE);
                title.setVisibility(View.VISIBLE);
                searchView.setIconified(true);
            }
        });

        back_button.setOnClickListener(view -> finish());
    }

    private void fetchContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            if (!FetchMyUsersService.STARTED) {
                if (!swipeMenuRecyclerView.isRefreshing())
                    swipeMenuRecyclerView.setRefreshing(true);
                FetchMyUsersService.startMyUsersService(CallListActivity.this, userMe.getId(), "");
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                    Constants.CONTACTS_REQUEST_CODE);
        }
    }

    public void makeCall(boolean b, User user) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment frag = manager.findFragmentByTag("DELETE_TAG");
        if (frag != null) {
            manager.beginTransaction().remove(frag).commit();
        }
        if (user != null && userMe != null && userMe.getBlockedUsersIds() != null
                && userMe.getBlockedUsersIds().contains(user.getId())) {
            Helper.unBlockAlert(user.getNameToDisplay(), userMe, CallListActivity.this,
                    helper, user.getId(), manager);
        } else
            placeCall(b, user);
    }

    private void placeCall(boolean callIsVideo, User user) {
        if (permissionsAvailable(permissionsSinch)) {
            try {
                Call call = callIsVideo ? getSinchServiceInterface().callUserVideo(user.getId())
                        : getSinchServiceInterface().callUser(user.getId());
                if (call == null) {
                    // Service failed for some reason, show a Toast and abort
                    Toast.makeText(this, "Service is not started." +
                            " Try stopping the service and starting it again before placing a call.", Toast.LENGTH_LONG).show();
                    return;
                }
                String callId = call.getCallId();
                startActivity(CallScreenActivity.newIntent(this, user, callId, "OUT"));
            } catch (Exception ignored) { }
        } else {
            ActivityCompat.requestPermissions(this, permissionsSinch, 69);
        }
    }
}
