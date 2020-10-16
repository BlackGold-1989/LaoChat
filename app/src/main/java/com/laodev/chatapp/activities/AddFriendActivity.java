package com.laodev.chatapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.adapters.UnFriendAdapter;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.BannerUtil;
import com.laodev.chatapp.utils.Helper;

import java.util.ArrayList;
import java.util.List;

public class AddFriendActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADD_FRIEND = 99;

    private View content;
    private RecyclerView ryc_unfriends;
    private TextView lbl_no_found;

    private Helper helper;
    private List<User> unFriendUserList = new ArrayList<>();
    private List<User> showUsers = new ArrayList<>();

    private UnFriendAdapter.UnFriendAdapterListener adapterListener = user -> {
        Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
        contactIntent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        contactIntent
                .putExtra(ContactsContract.Intents.Insert.NAME, "Unknown Name")
                .putExtra(ContactsContract.Intents.Insert.PHONE, user.getId());
        startActivityForResult(contactIntent, REQUEST_CODE_ADD_FRIEND);
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        helper = new Helper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        content = findViewById(R.id.content);
        ryc_unfriends = findViewById(R.id.ryc_users);
        ryc_unfriends.setLayoutManager(new LinearLayoutManager(this));
        lbl_no_found = findViewById(R.id.lbl_no_find);

        initData();
    }

    private String getUid() {
        User userMe = helper.getLoggedInUser();
        if (userMe != null)
            return userMe.getId() != null ? userMe.getId() : userMe.getNameInPhone();

        return null;
    }

    private void initData() {
        if (getUid() != null) {
            List<Contact> myContacts = new ArrayList<>();
            Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (cursor != null && !cursor.isClosed()) {
                cursor.getCount();
                while (cursor.moveToNext()) {
                    int hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    if (hasPhoneNumber == 1) {
                        String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("\\s+", "");
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
                        if (Patterns.PHONE.matcher(number).matches()) {
                            boolean hasPlus = String.valueOf(number.charAt(0)).equals("+");
                            number = number.replaceAll("[\\D]", "");
                            if (hasPlus) {
                                number = "+" + number;
                            }
                            Contact contact = new Contact(number, name);
                            if (!myContacts.contains(contact))
                                myContacts.add(contact);
                        }
                    }
                }
                cursor.close();
            }

            BaseApplication.getUserRef().addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<User> myUsers = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);
                        myUsers.add(user);
                    }

                    unFriendUserList.clear();
                    for (User user : myUsers) {
                        boolean flag = true;
                        for (Contact savedContact : new ArrayList<>(myContacts)) {
                            if (user != null && user.getId() != null && !user.getId().equals(getUid())) {
                                if (Helper.contactMatches(user.getId(), savedContact.getPhoneNumber())) {
                                    flag = false;
                                    break;
                                }
                            }
                        }
                        if (flag) unFriendUserList.add(user);
                    }

                    sortUserBySearchKey("");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    BannerUtil.onShowErrorAlertEvent(content, databaseError.getMessage(), 2000);
                }
            });
        } else {
            BannerUtil.onShowErrorAlertEvent(content, R.string.server_error, 2000);
        }
    }

    private void sortUserBySearchKey(String s) {
        showUsers.clear();
        if (s.isEmpty()) {
            lbl_no_found.setVisibility(View.VISIBLE);
            return;
        }
        for (User user: unFriendUserList) {
            if (user.isFoundNewUser(s)) {
                showUsers.add(user);
            }
        }
        if (showUsers.size() > 0) {
            lbl_no_found.setVisibility(View.GONE);
        } else {
            UnFriendAdapter unFriendAdapter = new UnFriendAdapter(this, showUsers);
            unFriendAdapter.setUnFriendAdapterListener(adapterListener);
            ryc_unfriends.setAdapter(unFriendAdapter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_forward, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setInputType(InputType.TYPE_CLASS_NUMBER);
        searchView.setQueryHint(getString(R.string.phone_number));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                sortUserBySearchKey(newText);
                return false;
            }

        });

        searchView.setOnCloseListener(() -> {
            sortUserBySearchKey("");
            return false;
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE_ADD_FRIEND) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Added Contact", Toast.LENGTH_SHORT).show();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Cancelled Added Contact", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
