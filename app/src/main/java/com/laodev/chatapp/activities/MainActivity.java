package com.laodev.chatapp.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.laodev.chatapp.R;
import com.laodev.chatapp.adapters.MenuUsersRecyclerAdapter;
import com.laodev.chatapp.adapters.ViewPagerAdapter;
import com.laodev.chatapp.fragments.GroupCreateDialogFragment;
import com.laodev.chatapp.fragments.MyCallsFragment;
import com.laodev.chatapp.fragments.MyGroupsFragment;
import com.laodev.chatapp.fragments.MyStatusFragmentNew;
import com.laodev.chatapp.fragments.MyUsersFragment;
import com.laodev.chatapp.fragments.OptionsFragment;
import com.laodev.chatapp.fragments.UserSelectDialogFragment;
import com.laodev.chatapp.interfaces.ContextualModeInteractor;
import com.laodev.chatapp.interfaces.OnUserGroupItemClick;
import com.laodev.chatapp.interfaces.UserGroupSelectionDismissListener;
import com.laodev.chatapp.interfaces.HomeIneractor;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.Group;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.Status;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.services.FetchMyUsersService;
import com.laodev.chatapp.services.SinchService;
import com.laodev.chatapp.utils.Constants;
import com.laodev.chatapp.utils.GeneralUtils;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.views.SwipeControlViewPager;
import com.laodev.chatapp.vmeet.VmeetMainActivity;
import com.mxn.soul.flowingdrawer_core.ElasticDrawer;
import com.mxn.soul.flowingdrawer_core.FlowingDrawer;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends BaseActivity implements HomeIneractor, OnUserGroupItemClick, View.OnClickListener, ContextualModeInteractor, UserGroupSelectionDismissListener {
    private static final int REQUEST_CODE_CHAT_FORWARD = 99;
    private static String USER_SELECT_TAG = "userselectdialog";
    private static String OPTIONS_MORE = "optionsmore";
    private static String GROUP_CREATE_TAG = "groupcreatedialog";

    private ImageView usersImage;
    private ImageView backImage;
    private RecyclerView menuRecyclerView;
    private SwipeRefreshLayout swipeMenuRecyclerView;
    private FlowingDrawer drawerLayout;
    private EditText searchContact;
    private TextView invite, selectedCount;
    private RelativeLayout toolbarContainer, cabContainer;

    private TabLayout tabLayout;
    private SwipeControlViewPager viewPager;

    private FloatingActionButton floatingActionButton;

    private MenuUsersRecyclerAdapter menuUsersRecyclerAdapter;
    private ArrayList<Contact> contactsData = new ArrayList<>();
    public static ArrayList<User> myUsers = new ArrayList<>();
    private ArrayList<Group> myGroups = new ArrayList<>();
    private ArrayList<Message> messageForwardList = new ArrayList<>();
    private UserSelectDialogFragment userSelectDialogFragment;
    private ViewPagerAdapter adapter;
    public static String userId;
    private ProgressDialog dialog;

    public DatabaseReference getDatabaseRef() {
        return statusRef;
    }

    public DatabaseReference getUserDatabaseRef() {
        return usersRef;
    }

    public SinchService.SinchServiceInterface getSinchRef() {
        return getSinchServiceInterface();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
        userId = userMe.getId();
        setupMenu();

        //If its a url then load it, else Make a text drawable of user's name
        setProfileImage(usersImage);
        usersImage.setOnClickListener(this);
        backImage.setOnClickListener(this);
        invite.setOnClickListener(this);
        findViewById(R.id.action_delete).setOnClickListener(this);
        floatingActionButton.setOnClickListener(this);
        floatingActionButton.setVisibility(View.VISIBLE);

        setupViewPager();
        fetchContacts();
        markOnline(true);
    }

    private void initUi() {
        usersImage = findViewById(R.id.users_image);
        ImageView img_meeting = findViewById(R.id.img_meeting);
        img_meeting.setOnClickListener(v -> {
            Constants.gUserMe = userMe;
            GeneralUtils.showOtherActivity(MainActivity.this, VmeetMainActivity.class, 0);
        });
        menuRecyclerView = findViewById(R.id.menu_recycler_view);
        swipeMenuRecyclerView = findViewById(R.id.menu_recycler_view_swipe_refresh);
        drawerLayout = findViewById(R.id.drawer_layout);
        searchContact = findViewById(R.id.searchContact);
        invite = findViewById(R.id.invite);
        toolbarContainer = findViewById(R.id.toolbarContainer);
        cabContainer = findViewById(R.id.cabContainer);
        selectedCount = findViewById(R.id.selectedCount);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        floatingActionButton = findViewById(R.id.addConversation);
        backImage = findViewById(R.id.back_button);
        drawerLayout.setTouchMode(ElasticDrawer.TOUCH_MODE_BEZEL);
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Syncing. . .");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(() -> {
            if (dialog != null && dialog.isShowing())
                dialog.dismiss();
        }, 4000);
    }

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new MyUsersFragment(MainActivity.this), getString(R.string.title_chats));
        adapter.addFrag(new MyGroupsFragment(MainActivity.this), getString(R.string.group));
        adapter.addFrag(new MyStatusFragmentNew(), getString(R.string.status));
        adapter.addFrag(new MyCallsFragment(), getString(R.string.title_calls));
        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 2)
                    floatingActionButton.hide();
                else
                    floatingActionButton.show();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

    }

    private void setupMenu() {
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuUsersRecyclerAdapter = new MenuUsersRecyclerAdapter(this, myUsers, helper.getLoggedInUser());
        menuRecyclerView.setAdapter(menuUsersRecyclerAdapter);
        swipeMenuRecyclerView.setColorSchemeResources(R.color.colorAccent);
        swipeMenuRecyclerView.setOnRefreshListener(this::fetchContacts);
        searchContact.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                menuUsersRecyclerAdapter.getFilter().filter(editable.toString());
            }
        });
    }

    private void setProfileImage(ImageView imageView) {
        if (userMe != null)
            if (userMe.getImage() != null && !userMe.getImage().isEmpty()) {
                Picasso.get()
                        .load(userMe.getImage())
                        .tag(this)
                        .error(R.drawable.ic_avatar)
                        .placeholder(R.drawable.ic_avatar)
                        .into(imageView);
            } else if (group != null && group.getImage() != null && !group.getImage().isEmpty()) {
                Picasso.get()
                        .load(group.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .into(imageView);
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.CONTACTS_REQUEST_CODE) {
            fetchContacts();
        }
    }

    private void fetchContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            if (!FetchMyUsersService.STARTED) {
                if (!swipeMenuRecyclerView.isRefreshing())
                    swipeMenuRecyclerView.setRefreshing(true);
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser != null) {
                    firebaseUser.getIdToken(true).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String idToken = task.getResult().getToken();
                            FetchMyUsersService.startMyUsersService(MainActivity.this, userMe.getId(), idToken);
                        }
                    });
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, Constants.CONTACTS_REQUEST_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        markOnline(false);
    }

    @Override
    public void onBackPressed() {
        if (ElasticDrawer.STATE_CLOSED != drawerLayout.getDrawerState()) {
            drawerLayout.closeMenu(true);
        } else if (isContextualMode()) {
            disableContextualMode();
        } else if (viewPager.getCurrentItem() != 0) {
            viewPager.post(() -> viewPager.setCurrentItem(0));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHAT_FORWARD) {
            if (resultCode == Activity.RESULT_OK) {
                //show forward dialog to choose users
                messageForwardList.clear();
                ArrayList<Message> temp = data.getParcelableArrayListExtra("FORWARD_LIST");
                messageForwardList.addAll(temp);
                userSelectDialogFragment = UserSelectDialogFragment.newInstance(this, myUsers);
                FragmentManager manager = getSupportFragmentManager();
                Fragment frag = manager.findFragmentByTag(USER_SELECT_TAG);
                if (frag != null) {
                    manager.beginTransaction().remove(frag).commit();
                }
                userSelectDialogFragment.show(manager, USER_SELECT_TAG);
            }
        }
    }

    private void sortMyGroupsByName() {
        Collections.sort(myGroups, (group1, group2) -> group1.getName().compareToIgnoreCase(group2.getName()));
    }

    private void sortMyUsersByName() {
        Collections.sort(myUsers, (user1, user2) -> user1.getNameToDisplay().compareToIgnoreCase(user2.getNameToDisplay()));
    }

    @Override
    void userAdded(User value) {
        if (value.getId().equals(userMe.getId())) {
        }
        else if (helper.getCacheMyUsers() != null && helper.getCacheMyUsers().containsKey(value.getId())) {
            value.setNameInPhone(helper.getCacheMyUsers().get(value.getId()).getNameToDisplay());
            addUser(value);
        } else {
            for (Contact savedContact : contactsData) {
                if (Helper.contactMatches(value.getId(), savedContact.getPhoneNumber())) {
                    value.setNameInPhone(savedContact.getName());
                    addUser(value);
                    helper.setCacheMyUsers(myUsers);
                    break;
                }
            }
        }
    }

    @Override
    void groupAdded(Group group) {
        if (!myGroups.contains(group)) {
            myGroups.add(group);
            sortMyGroupsByName();
        }
    }

    @Override
    void userUpdated(User value) {
        if (value.getId().equals(userMe.getId())) {
            userMe = value;
            setProfileImage(usersImage);
        } else if (helper.getCacheMyUsers() != null && helper.getCacheMyUsers().containsKey(value.getId())) {
            value.setNameInPhone(helper.getCacheMyUsers().get(value.getId()).getNameToDisplay());
            updateUser(value);
        } else {
            for (Contact savedContact : contactsData) {
                if (Helper.contactMatches(value.getId(), savedContact.getPhoneNumber())) {
                    value.setNameInPhone(savedContact.getName());
                    updateUser(value);
                    helper.setCacheMyUsers(myUsers);
                    break;
                }
            }
        }
    }

    private void updateUser(User value) {
        int existingPos = myUsers.indexOf(value);
        if (existingPos != -1) {
            myUsers.set(existingPos, value);
            menuUsersRecyclerAdapter.notifyItemChanged(existingPos);
            refreshUsers(existingPos);
        }
    }

    @Override
    void groupUpdated(Group group) {
        int existingPos = myGroups.indexOf(group);
        if (existingPos != -1) {
            myGroups.set(existingPos, group);
            //menuUsersRecyclerAdapter.notifyItemChanged(existingPos);
            //refreshUsers(existingPos);
        }
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

    private void addUser(User value) {
        if (!myUsers.contains(value)) {
            myUsers.add(value);
            sortMyUsersByName();
            menuUsersRecyclerAdapter.notifyDataSetChanged();
            refreshUsers(-1);
        }
    }


    @Override
    public void OnUserClick(final User user, int position, View userImage) {
        if (ElasticDrawer.STATE_CLOSED != drawerLayout.getDrawerState()) {
            drawerLayout.closeMenu(true);
        }
        if (userImage == null) {
            userImage = usersImage;
        }
        Intent intent = ChatActivity.newIntent(this, messageForwardList, user);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                userImage, "backImage");
        startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD, options.toBundle());

        if (userSelectDialogFragment != null)
            userSelectDialogFragment.dismiss();
    }

    @Override
    public void OnGroupClick(Group group, int position, View userImage) {
        Intent intent = ChatActivity.newIntent(this, messageForwardList, group);
        if (userImage == null) {
            userImage = usersImage;
        }
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, userImage, "backImage");
        startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD, options.toBundle());

        if (userSelectDialogFragment != null)
            userSelectDialogFragment.dismiss();
    }

    private void refreshUsers(int pos) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(USER_SELECT_TAG);
        if (frag != null) {
            userSelectDialogFragment.refreshUsers(pos);
        }
    }

    private void markOnline(boolean b) {
        //Mark online boolean as b in firebase
        usersRef.child(userMe.getId()).child("timeStamp").setValue(System.currentTimeMillis());
        usersRef.child(userMe.getId()).child("online").setValue(b);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                drawerLayout.openMenu(true);
                break;
            case R.id.addConversation:
                switch (viewPager.getCurrentItem()) {
                    case 0:
//                        drawerLayout.openMenu(true);
                        Intent callIntent = new Intent(MainActivity.this, ContactActivity.class);
                        startActivity(callIntent);
                        break;
                    case 1:
                        for (int i = 0; i < myUsers.size(); i++) {
                            myUsers.get(i).setSelected(false);
                        }
                        GroupCreateDialogFragment.newInstance(this, userMe, myUsers)
                                .show(getSupportFragmentManager(), GROUP_CREATE_TAG);
                        break;
                    case 3:
//                        drawerLayout.openMenu(true);
                        Intent aCallIntent = new Intent(MainActivity.this, CallListActivity.class);
                        startActivity(aCallIntent);
                        break;
                }
                break;
            case R.id.users_image:
                if (userMe != null)
                    OptionsFragment.newInstance(getSinchServiceInterface()).show(getSupportFragmentManager(), OPTIONS_MORE);
                break;
            case R.id.invite:
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invitation_title));
                    shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.invitation_text), getPackageName()));
                    startActivity(Intent.createChooser(shareIntent, "Share using.."));
                } catch (Exception ignored) {
                }
                break;
            case R.id.action_delete:
                if (viewPager.getCurrentItem() == 0) {
                    ((MyUsersFragment) adapter.getItem(0)).deleteSelectedChats();
                } else if (viewPager.getCurrentItem() == 1) {
                    ((MyGroupsFragment) adapter.getItem(1)).deleteSelectedChats();
                }
                break;
        }
    }

    @Override
    public void onUserGroupSelectDialogDismiss() {
        messageForwardList.clear();
    }

    @Override
    public void selectionDismissed() {
        //do nothing..
    }

    @Override
    public void myUsersResult(ArrayList<User> myUsers) {
        if (helper == null) {
            helper = new Helper(this);
        }
        helper.setCacheMyUsers(myUsers);
        MainActivity.myUsers.clear();
        MainActivity.myUsers.addAll(myUsers);
        refreshUsers(-1);
        menuUsersRecyclerAdapter.notifyDataSetChanged();
        swipeMenuRecyclerView.setRefreshing(false);
    }

    @Override
    public void myContactsResult(ArrayList<Contact> myContacts) {
        contactsData.clear();
        contactsData.addAll(myContacts);
        MyUsersFragment myUsersFragment = ((MyUsersFragment) adapter.getItem(0));
        myUsersFragment.setUserNamesAsInPhone();
        MyCallsFragment myCallsFragment = ((MyCallsFragment) adapter.getItem(3));
        myCallsFragment.setUserNamesAsInPhone();
    }

    public void disableContextualMode() {
        cabContainer.setVisibility(View.GONE);
        toolbarContainer.setVisibility(View.VISIBLE);
        ((MyUsersFragment) adapter.getItem(0)).disableContextualMode();
        ((MyGroupsFragment) adapter.getItem(1)).disableContextualMode();
        viewPager.setSwipeAble(true);
    }

    @Override
    public void enableContextualMode() {
        cabContainer.setVisibility(View.VISIBLE);
        toolbarContainer.setVisibility(View.GONE);
        viewPager.setSwipeAble(false);
    }

    @Override
    public boolean isContextualMode() {
        return cabContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public void updateSelectedCount(int count) {
        if (count > 0) {
            selectedCount.setText(String.format(Locale.getDefault(), "%d selected", count));
        } else {
            disableContextualMode();
        }
    }

    @Override
    public User getUserMe() {
        return userMe;
    }

    @Override
    public ArrayList<Contact> getLocalContacts() {
        return contactsData;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
