package com.laodev.chatapp.activities;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.adapters.ForwardUserAdapter;
import com.laodev.chatapp.models.Attachment;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.CheckableUser;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.BannerUtil;
import com.laodev.chatapp.utils.FileUtils;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.utils.MimeTypes;
import com.laodev.chatapp.utils.StringUtil;
import com.laodev.chatapp.views.SelectedUserView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.laodev.chatapp.vmeet.utils.SharedObjects.getContext;

public class ForwardActivity extends AppCompatActivity {

    private Toolbar toolbarForward;
    private FloatingActionButton fabSend;
    private View content;
    private RecyclerView ryc_friends;
    private HorizontalScrollView hsv_select;
    private LinearLayout llt_select;

    private List<CheckableUser> allFriendUsers = new ArrayList<>();
    private List<CheckableUser> friendUsers = new ArrayList<>();
    private ForwardUserAdapter forwardUserAdapter;
    private String searchKey = "";
    private Helper helper;
    private User userMe;
    private int messageCount = 0;

    private ForwardUserAdapter.ForwardUserAdapterListener userAdapterListener = this::refreshSelectedView;
    private SelectedUserView.SelectedUserViewListener userViewListener = new SelectedUserView.SelectedUserViewListener() {
        @Override
        public void onClickCloseButton() {
            refreshSelectedView();
            forwardUserAdapter.notifyDataSetChanged();
        }
    };

    private void refreshSelectedView() {
        List<CheckableUser> userList = new ArrayList<>();
        for (CheckableUser user: friendUsers) {
            if (user.isCheck()) {
                userList.add(user);
            }
        }
        if (userList.size() > 0) {
            hsv_select.setVisibility(View.VISIBLE);
            YoYo.with(Techniques.FadeIn)
                    .duration(300)
                    .repeat(0)
                    .playOn(findViewById(R.id.hsv_select));
            llt_select.removeAllViews();
            for (CheckableUser user: userList) {
                SelectedUserView userView = new SelectedUserView(this);
                userView.setUser(user);
                userView.setSelectedUserViewListener(userViewListener);
                llt_select.addView(userView);
            }
        } else {
            hsv_select.setVisibility(View.GONE);
            YoYo.with(Techniques.FadeOut)
                    .duration(300)
                    .repeat(0)
                    .playOn(findViewById(R.id.hsv_select));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_forward);

        helper = new Helper(this);

        initView();

        setSupportActionBar(toolbarForward);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbarForward.setNavigationOnClickListener(v -> onBackPressed());
        }

        fabSend.show();
        fabSend.setOnClickListener(view -> {
            List<CheckableUser> forwardUsers = new ArrayList<>();
            for (CheckableUser user: friendUsers) {
                if (user.isCheck()) {
                    forwardUsers.add(user);
                }
            }
            if (forwardUsers.size() == 0) {
                return;
            }
            handleIncomingShareToChat(forwardUsers);
        });
    }

    private void initView() {
        content = findViewById(R.id.content);

        toolbarForward = findViewById(R.id.toolbar);
        fabSend = findViewById(R.id.fab_send);
        ryc_friends = findViewById(R.id.ryc_users);
        ryc_friends.setLayoutManager(new LinearLayoutManager(this));
        forwardUserAdapter = new ForwardUserAdapter(this, friendUsers, searchKey);
        forwardUserAdapter.setForwardUserAdapterListener(userAdapterListener);
        ryc_friends.setAdapter(forwardUserAdapter);
        hsv_select = findViewById(R.id.hsv_select);
        llt_select = findViewById(R.id.llt_select);

        initData();
    }

    private String getUid() {
        userMe = helper.getLoggedInUser();
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

            List<User> myUsers = new ArrayList<>();
            BaseApplication.getUserRef().addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);
                        myUsers.add(user);
                    }
                    List<User> finalUserList = new ArrayList<>();
                    for (Contact savedContact : new ArrayList<>(myContacts)) {
                        for (User user : myUsers) {
                            if (user != null && user.getId() != null && !user.getId().equals(getUid())) {
                                if (Helper.contactMatches(user.getId(), savedContact.getPhoneNumber())) {
                                    user.setNameInPhone(savedContact.getName());
                                    finalUserList.add(user);
                                    break;
                                }
                            }
                        }
                    }

                    for (User user: finalUserList) {
                        CheckableUser checkableUser = new CheckableUser(user);
                        allFriendUsers.add(checkableUser);
                    }
                    sortArrayForUsers("");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    BannerUtil.onShowErrorAlertEvent(content, databaseError.getMessage(), 2000);
                }
            });
        }
    }

    private void sortArrayForUsers(String newText) {
        friendUsers.clear();
        for (CheckableUser checkableUser: allFriendUsers) {
            if (checkableUser.search(newText)) {
                friendUsers.add(checkableUser);
            }
        }
        Collections.sort(friendUsers, (entry1, entry2) -> entry1.getUser().getName().compareTo(entry2.getUser().getName()));
        searchKey = newText;
        forwardUserAdapter = new ForwardUserAdapter(this, friendUsers, searchKey);
        forwardUserAdapter.setForwardUserAdapterListener(userAdapterListener);
        ryc_friends.setAdapter(forwardUserAdapter);
    }

    private void handleIncomingShareToChat(List<CheckableUser> selectedUsers) {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.equals(MimeTypes.TEXT_PLAIN)) {
                handleTextShare(selectedUsers);
            } else if (type.startsWith(MimeTypes.IMAGE)) {
                handleImageShare(selectedUsers);
            } else if (type.startsWith(MimeTypes.VIDEO)) {
                handleVideoShare(selectedUsers);
            } else if (type.startsWith(MimeTypes.CONTACT)) {
                handleContactShare(selectedUsers);
            } else if (type.startsWith(MimeTypes.AUDIO)) {
                Toast.makeText(this, "Unsupport file yet.", Toast.LENGTH_SHORT).show();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Toast.makeText(this, "Unsupport file yet.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleContactShare(List<CheckableUser> selectedUsers) {
        Uri contactsData = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        Attachment attachment = new Attachment();
        attachment.setData(getContactAsVcard(contactsData));
        for (CheckableUser user: selectedUsers) {
            sendMessage("", user.getUser().getId(), AttachmentTypes.CONTACT, attachment, () -> {
                messageCount++;
                if (messageCount == selectedUsers.size()) {
                    finish();
                }
            });
        }
    }

    public String getContactAsVcard(Uri uri) {
        ContentResolver cr = getContentResolver();
        InputStream stream = null;
        try {
            stream = cr.openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        StringBuffer fileContent = new StringBuffer("");
        int ch;
        try {
            while ((ch = stream.read()) != -1)
                fileContent.append((char) ch);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new String(fileContent);
    }

    private void handleTextShare(List<CheckableUser> selectedUsers) {
        messageCount = 0;
        String sharedText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        for (CheckableUser user: selectedUsers) {
            sendMessage(sharedText, user.getUser().getId(), AttachmentTypes.NONE_TEXT, null, () -> {
                messageCount++;
                if (messageCount == selectedUsers.size()) {
                    finish();
                }
            });
        }
    }

    private void handleImageShare(List<CheckableUser> selectedUsers) {
        try {
            messageCount = 0;

            Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            final File fileToUpload = new File(imageUri.getPath());
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            ProgressDialog progressDialog = ProgressDialog.show(this, "", getString(R.string.updated));
            onUploadImageToFirebase(bitmap, new UploadImageCallback() {
                @Override
                public void onSuccessUploadCallback(String url, String fileName) {
                    progressDialog.dismiss();

                    Attachment attachment = new Attachment();
                    attachment.setName(fileName);
                    attachment.setBytesCount(fileToUpload.length());
                    attachment.setUrl(url);

                    checkAndCopy("/" + getString(R.string.app_name) + "/" +
                            AttachmentTypes.getTypeName(AttachmentTypes.IMAGE) + "/Sent/", fileToUpload);

                    for (CheckableUser user: selectedUsers) {
                        sendMessage(null, user.getUser().getId(), AttachmentTypes.IMAGE, attachment, () -> {
                            messageCount++;
                            if (messageCount == selectedUsers.size()) {
                                finish();
                            }
                        });
                    }
                }

                @Override
                public void onFailureUploadCallback(String error) {
                    progressDialog.dismiss();

                    Toast.makeText(ForwardActivity.this, error, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleVideoShare(List<CheckableUser> selectedUsers) {
        messageCount = 0;

        Uri videoPath = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);

        if (videoPath != null) {
            final File fileToUpload = new File(videoPath.getPath());

            setProgressBarIndeterminateVisibility(true);
            setProgressBarVisibility(true);

            String fileName = StringUtil.getUploadVideoName();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference videoRef = storageRef.child("/Video/" + fileName);
            UploadTask uploadTask = videoRef.putFile(videoPath);
            uploadTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    setProgressBarIndeterminateVisibility(false);
                    setProgressBarVisibility(false);
                    Toast.makeText(getContext(), "Upload Complete", Toast.LENGTH_SHORT).show();

                    checkAndCopy("/" + getString(R.string.app_name) + "/" +
                            AttachmentTypes.getTypeName(AttachmentTypes.VIDEO) + "/Sent/", fileToUpload);
                    videoRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String url = uri.toString();

                                Attachment attachment = new Attachment();
                                attachment.setName(fileName);
                                attachment.setBytesCount(fileToUpload.length());
                                attachment.setUrl(url);

                                for (CheckableUser user: selectedUsers) {
                                    sendMessage(null, user.getUser().getId(), AttachmentTypes.VIDEO, attachment, () -> {
                                        messageCount++;
                                        if (messageCount == selectedUsers.size()) {
                                            finish();
                                        }
                                    });
                                }
                            });
                }
            }).addOnProgressListener(taskSnapshot -> setProgress((int) (taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount() * 100)));
        }
    }

    private void checkAndCopy(String directory, File source) {
        //Create and copy file content
        File file = new File(Environment.getExternalStorageDirectory(), directory);
        boolean dirExists = file.exists();
        if (!dirExists)
            dirExists = file.mkdirs();
        if (dirExists) {
            try {
                file = new File(Environment.getExternalStorageDirectory() +
                        directory, Uri.fromFile(source).getLastPathSegment());
                boolean fileExists = file.exists();
                if (!fileExists)
                    fileExists = file.createNewFile();
                if (fileExists && file.length() == 0) {
                    FileUtils.copyFile(source, file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onUploadImageToFirebase(Bitmap bitmap, final UploadImageCallback callback) {
        String filename = StringUtil.getUploadImageName();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference childRef = storageRef.child(filename);

        String storagePath = getString(R.string.app_name) + "/Image/" + filename;
        final StorageReference mountainImagesRef = storageRef.child(storagePath);

        childRef.getName().equals(mountainImagesRef.getName());    // true
        childRef.getPath().equals(mountainImagesRef.getPath());    // false

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = mountainImagesRef.putBytes(data);
        uploadTask.addOnSuccessListener(taskSnapshot -> mountainImagesRef.getDownloadUrl()
                .addOnSuccessListener(downloadPhotoUrl -> {
                    String url = downloadPhotoUrl.toString();
                    callback.onSuccessUploadCallback(url, filename);
                }))
                .addOnFailureListener(e -> callback.onFailureUploadCallback(e.getMessage()));
    }

    public interface UploadImageCallback {
        void onSuccessUploadCallback(String url, String fileName);
        void onFailureUploadCallback(String error);
    }

    private void sendMessage(String messageBody
            , String userid
            , @AttachmentTypes.AttachmentType int attachmentType
            , Attachment attachment
            , ForwardSenderListener listener) {
        BaseApplication.getChatRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    String chatID = dataSnapshot.getKey();
                    if (chatID.contains(userMe.getId()) && chatID.contains(userid)) {
                        //Create message object
                        Message message = new Message();
                        message.setAttachmentType(attachmentType);
                        if (attachmentType != AttachmentTypes.NONE_TEXT)
                            message.setAttachment(attachment);

                        //Add messages in chat child
                        String messageId = BaseApplication.getChatRef().push().getKey();
                        if (messageId != null) {
                            if (messageBody != null)
                                message.setBody(messageBody);
                            message.setDate(System.currentTimeMillis());
                            message.setSenderId(userMe.getId());
                            message.setSenderName(userMe.getName());
                            message.setSent(true);
                            message.setDelivered(false);
                            message.setRecipientId(userid);
                            message.setId(messageId);
                            message.setReplyId("0");
                            message.setDelete("");

                            BaseApplication.getChatRef().child(chatID).child(message.getId()).setValue(message);
                        }
                        break;
                    }
                }
                listener.onSendMessageToUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onSendMessageToUsers();
            }
        });
    }

    public interface ForwardSenderListener {
        void onSendMessageToUsers();
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
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                sortArrayForUsers(newText);
                return false;
            }

        });

        searchView.setOnCloseListener(() -> {
            sortArrayForUsers("");
            return false;
        });

        return super.onCreateOptionsMenu(menu);
    }

}
