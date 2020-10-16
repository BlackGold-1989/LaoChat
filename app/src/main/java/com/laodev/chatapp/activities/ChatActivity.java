package com.laodev.chatapp.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.adapters.MessageAdapter;
import com.laodev.chatapp.interfaces.OnMessageItemClick;
import com.laodev.chatapp.models.Attachment;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Chat;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.DownloadFileEvent;
import com.laodev.chatapp.models.Group;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.Status;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.ConfirmationDialogFragment;
import com.laodev.chatapp.utils.DownloadUtil;
import com.laodev.chatapp.utils.FileUtils;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.utils.KeyboardUtil;
import com.laodev.chatapp.viewHolders.BaseMessageViewHolder;
import com.laodev.chatapp.viewHolders.MessageAttachmentRecordingViewHolder;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iceteck.silicompressorr.SiliCompressor;
import com.kbeanie.multipicker.api.AudioPicker;
import com.kbeanie.multipicker.api.CameraImagePicker;
import com.kbeanie.multipicker.api.FilePicker;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.VideoPicker;
import com.kbeanie.multipicker.api.callbacks.AudioPickerCallback;
import com.kbeanie.multipicker.api.callbacks.FilePickerCallback;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.callbacks.VideoPickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenAudio;
import com.kbeanie.multipicker.api.entity.ChosenFile;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.kbeanie.multipicker.api.entity.ChosenVideo;
import com.sinch.android.rtc.calling.Call;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.listeners.OnEmojiPopupShownListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;

import static com.laodev.chatapp.utils.Helper.GROUP_PREFIX;

public class ChatActivity extends BaseActivity implements OnMessageItemClick,
        MessageAttachmentRecordingViewHolder.RecordingViewInteractor, View.OnClickListener, ImagePickerCallback,
        FilePickerCallback, AudioPickerCallback, VideoPickerCallback {

    private static final int REQUEST_CODE_CONTACT = 1;
    private static final int REQUEST_PLACE_PICKER = 2;
    private static final int REQUEST_CODE_PLAY_SERVICES = 3;
    private static final int REQUEST_CODE_UPDATE_USER = 753;
    private static final int REQUEST_CODE_UPDATE_GROUP = 357;
    private static final int REQUEST_PERMISSION_RECORD = 159;

    private static String EXTRA_DATA_GROUP = "extradatagroup";
    private static String EXTRA_DATA_USER = "extradatauser";
    private static String EXTRA_DATA_LIST = "extradatalist";
    private static String DELETE_TAG = "deletetag";

    private MessageAdapter messageAdapter;
    private ArrayList<Message> dataList = new ArrayList<>();
    private RealmResults<Chat> queryResult;
    private String chatChild, userOrGroupId;
    private int countSelected = 0;

    private Handler recordWaitHandler, recordTimerHandler;
    private Runnable recordRunnable, recordTimerRunnable;
    private MediaRecorder mRecorder = null;
    private String recordFilePath;
    private float displayWidth;
    private boolean callIsVideo;

    private ArrayList<Integer> adapterPositions = new ArrayList<>();

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private String currentlyPlaying = "";

    private Toolbar toolbar;
    private RelativeLayout toolbarContent;
    private TextView selectedCount, status, userName;
    private RecyclerView recyclerView;
    private EmojiEditText newMessage;
    private ImageView usersImage, addAttachment, sendMessage, attachment_emoji;
    private LinearLayout rootView, myAttachmentLLY;
    private ImageView callAudio, callVideo;

    private EmojiPopup emojIcon;

    private String pickerPath;
    private ImagePicker imagePicker;
    private CameraImagePicker cameraPicker;
    private FilePicker filePicker;
    private AudioPicker audioPicker;
    private VideoPicker videoPicker;
    private RelativeLayout replyLay;
    private TextView replyName;
    private ImageView replyImg;
    private ImageView closeReply;
    private HashMap<String, User> myUsersNameInPhoneMap;
    private String replyId = "0";
    private TextView userStatus;

    String senderIdDelete = "";
    String recipientIdDelete = "";
    String bodyDelete = "";
    String msgID = "", newMsgID = "";
    long dateDelete;
    private boolean delete = false;
    private ImageView camera;

    //Download complete listener
    private BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null)
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                    if (adapterPositions.size() > 0 && messageAdapter != null)
                        for (int pos : adapterPositions)
                            if (pos != -1)
                                messageAdapter.notifyItemChanged(pos);
                    adapterPositions.clear();
                }
        }
    };

    //Download event listener
    private BroadcastReceiver downloadEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DownloadFileEvent downloadFileEvent = intent.getParcelableExtra("data");
            if (downloadFileEvent != null) {
                downloadFile(downloadFileEvent);
            }
        }
    };

    @Override
    void myUsersResult(ArrayList<User> myUsers) {

    }

    @Override
    void myContactsResult(ArrayList<Contact> myContacts) {

    }

    @Override
    void userAdded(User valueUser) {
        //do nothing
    }

    @Override
    void groupAdded(Group valueGroup) {
        //do nothing
    }

    @Override
    void userUpdated(User valueUser) {
        if (user != null && user.getId().equals(valueUser.getId())) {
            valueUser.setNameInPhone(user.getNameInPhone());
            user = valueUser;

            status.setText(user.getStatus());
            status.setSelected(true);
            showTyping(user.isTyping());//Show typing
            int existingPos = MainActivity.myUsers.indexOf(valueUser);
            if (existingPos != -1) {
                MainActivity.myUsers.set(existingPos, valueUser);
                helper.setCacheMyUsers(MainActivity.myUsers);
                messageAdapter.notifyDataSetChanged();
            }
        } else if (userMe.getId().equalsIgnoreCase(valueUser.getId())) {
            helper.setLoggedInUser(valueUser);
        } else {
            int existingPos = MainActivity.myUsers.indexOf(valueUser);
            if (existingPos != -1) {
                valueUser.setNameInPhone(MainActivity.myUsers.get(existingPos).getNameToDisplay());
                MainActivity.myUsers.set(existingPos, valueUser);
                helper.setCacheMyUsers(MainActivity.myUsers);
            }
        }
    }

    @Override
    void groupUpdated(Group valueGroup) {
        if (group != null && group.getId().equals(valueGroup.getId())) {
            group = valueGroup;
            checkIfChatAllowed();
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
        callAudio.setClickable(true);
        callVideo.setClickable(true);
    }

    @Override
    void onSinchDisconnected() {
        callAudio.setClickable(false);
        callVideo.setClickable(false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);
        // new CommentKeyBoardFix(this);
        this.helper = new Helper(ChatActivity.this);
        this.myUsersNameInPhoneMap = helper.getCacheMyUsers();
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_DATA_USER)) {
            user = intent.getParcelableExtra(EXTRA_DATA_USER);
            Helper.CURRENT_CHAT_ID = user.getId();
        } else if (intent.hasExtra(EXTRA_DATA_GROUP)) {
            group = intent.getParcelableExtra(EXTRA_DATA_GROUP);
            Helper.CURRENT_CHAT_ID = group.getId();
        } else {
            finish();//temporary fix
        }

        initUi();

        //set basic user info
        String nameText = null, statusText = null, imageUrl = null;
        if (user != null) {
            if (helper.getCacheMyUsers().containsKey(user.getNameToDisplay())) {
                nameText = helper.getCacheMyUsers().get(user.getNameToDisplay()).getNameToDisplay();
            } else {
                nameText = user.getNameToDisplay();
            }
            statusText = user.getStatus();
            imageUrl = user.getImage();
            if (imageUrl != null && !imageUrl.isEmpty() && user.getBlockedUsersIds() != null
                    && !user.getBlockedUsersIds().contains(userMe.getId()))
                Picasso.get()
                        .load(imageUrl)
                        .tag(this)
                        .error(R.drawable.ic_avatar)
                        .placeholder(R.drawable.ic_avatar)
                        .into(usersImage);
            else
                Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .tag(this)
                        .error(R.drawable.ic_avatar)
                        .placeholder(R.drawable.ic_avatar)
                        .into(usersImage);
        } else if (group != null) {
            nameText = group.getName();
            statusText = group.getStatus();
            imageUrl = group.getImage();
        }
        userName.setText(nameText);
        status.setText(statusText);
        userName.setSelected(true);
        status.setSelected(true);
        if (imageUrl != null && !imageUrl.isEmpty())
            Picasso.get()
                    .load(imageUrl)
                    .tag(this)
                    .error(R.drawable.ic_avatar)
                    .placeholder(R.drawable.ic_avatar)
                    .into(usersImage);
        else
            usersImage.setBackgroundResource(R.drawable.ic_avatar);


        callAudio.setClickable(false);
        callVideo.setClickable(false);

        animateToolbarViews();

        userOrGroupId = user != null ? user.getId() : group.getId();

        //setup recycler view
        messageAdapter = new MessageAdapter(this, dataList, userMe.getId(), newMessage);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                }, 100);
            }
        });

        emojIcon = EmojiPopup.Builder.fromRootView(rootView).setOnEmojiPopupShownListener(() -> {
            if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
                myAttachmentLLY.setVisibility(View.GONE);
                // addAttachment.animate().setDuration(400).rotationBy(-45).start();
            }
        }).build(newMessage);

        displayWidth = Helper.getDisplayWidth(this);

        mediaPlayer.setOnCompletionListener(mediaPlayer -> notifyRecordingPlaybackCompletion());

        //Query out chat from existing chats whose owner is logged in user and the user is selected user
        RealmQuery<Chat> query = Helper.getChat(rChatDb, userMe.getId(), userOrGroupId);//rChatDb.where(Chat.class).equalTo("myId", userMe.getId()).equalTo("userId", user.getId());
        queryResult = query.findAll();
        queryResult.addChangeListener(realmChangeListener);//register change listener
        Chat prevChat = query.findFirst();
        //Add all messages from queried chat into recycler view

        chatChild = user != null ? getChatChild(prevChat) : group.getId();
        //Group Chat
        if (user == null) {
            if (prevChat != null) {
                for (int i = 0; i < prevChat.getMessages().size(); i++) {
                    if (prevChat.getMessages().get(i) != null &&
                            prevChat.getMessages().get(i).getUserIds().contains(userMe.getId())) {
                        this.dataList.add(prevChat.getMessages().get(i));
                    }
                }
                for (int i = 0; i < dataList.size(); i++) {
                    if (!dataList.get(i).getSenderId().equalsIgnoreCase(userMe.getId()) && !dataList.get(i).isReadMsg() && dataList.get(i).getId() != null)
                        chatRef.child(chatChild).child(dataList.get(i).getId()).child("readMsg").setValue(true);
                }
                messageAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
            }
        }

        registerUserUpdates();
        checkAndForward();
        initSwipe();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initUi() {
        toolbar = findViewById(R.id.chatToolbar);
        toolbarContent = findViewById(R.id.chatToolbarContent);
        selectedCount = findViewById(R.id.selectedCount);
        TableLayout addAttachmentLayout = findViewById(R.id.add_attachment_layout);
        usersImage = findViewById(R.id.users_image);
        status = findViewById(R.id.emotion);
        userName = findViewById(R.id.user_name);
        recyclerView = findViewById(R.id.recycler_view);
        newMessage = findViewById(R.id.new_message);
        addAttachment = findViewById(R.id.add_attachment);
        sendMessage = findViewById(R.id.send);
        LinearLayout sendContainer = findViewById(R.id.sendContainer);
        myAttachmentLLY = findViewById(R.id.layout_chat_attachment_LLY);
        rootView = findViewById(R.id.rootView);
        attachment_emoji = findViewById(R.id.attachment_emoji);
        callAudio = findViewById(R.id.callAudio);
        callVideo = findViewById(R.id.callVideo);
        replyLay = findViewById(R.id.replyLay);
        replyName = findViewById(R.id.replyName);
        replyImg = findViewById(R.id.replyImg);
        closeReply = findViewById(R.id.closeReply);
        userStatus = findViewById(R.id.user_status);
        camera = findViewById(R.id.camera);

        callAudio.setVisibility(user != null && group == null ? View.VISIBLE : View.GONE);
        callVideo.setVisibility(user != null && group == null ? View.VISIBLE : View.GONE);

        setSupportActionBar(toolbar);
        addAttachment.setOnClickListener(this);
        toolbarContent.setOnClickListener(this);
        attachment_emoji.setOnClickListener(this);
        sendMessage.setOnClickListener(this);
        callAudio.setOnClickListener(this);
        callVideo.setOnClickListener(this);
        findViewById(R.id.back_button).setOnClickListener(this);
        findViewById(R.id.attachment_video).setOnClickListener(this);
        findViewById(R.id.attachment_contact).setOnClickListener(this);
        findViewById(R.id.camera).setOnClickListener(this);
        findViewById(R.id.attachment_gallery).setOnClickListener(this);
        findViewById(R.id.attachment_audio).setOnClickListener(this);
        findViewById(R.id.attachment_location).setOnClickListener(this);
        findViewById(R.id.attachment_document).setOnClickListener(this);
        newMessage.setOnTouchListener((v, event) -> {
            if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
                myAttachmentLLY.setVisibility(View.GONE);
            }
            return false;
        });
        sendMessage.setOnTouchListener(voiceMessageListener);
        //Wallpaper
        if (userMe != null && userMe.getWallpaper() != null && !userMe.getWallpaper().isEmpty()) {
            String url;
            if (userMe.getWallpaper().isEmpty()) {
                url = "a";
            } else {
                url = userMe.getWallpaper();
            }
            Picasso.get().load(url).error(R.drawable.chat_background)
                    .placeholder(R.drawable.chat_background).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    rootView.setBackground(new BitmapDrawable(bitmap));
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
        }
    }

    private View.OnTouchListener voiceMessageListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (newMessage.getText().toString().trim().isEmpty()) {
                        if (recordWaitHandler == null)
                            recordWaitHandler = new Handler();
                        recordRunnable = () -> {
                            if (user != null) {
                                userMe = helper.getLoggedInUser();
                                if (userMe.getBlockedUsersIds() != null
                                        && !userMe.getBlockedUsersIds().contains(user.getId())) {
                                    recordingStart();
                                } else {
                                    FragmentManager manager = getSupportFragmentManager();
                                    Fragment frag = manager.findFragmentByTag(DELETE_TAG);
                                    if (frag != null) {
                                        manager.beginTransaction().remove(frag).commit();
                                    }

                                    Helper.unBlockAlert(user.getNameToDisplay(), userMe, ChatActivity.this,
                                            helper, user.getId(), manager);
                                }
                            } else {
                                recordingStart();
                            }
                        };
                        recordWaitHandler.postDelayed(recordRunnable, 600);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mRecorder != null && newMessage.getText().toString().trim().isEmpty()) {
                        if (Math.abs(event.getX()) / displayWidth > 0.35f) {
                            recordingStop(false);
                            Toast.makeText(ChatActivity.this, "Recording cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (recordWaitHandler != null && newMessage.getText().toString().trim().isEmpty())
                        recordWaitHandler.removeCallbacks(recordRunnable);
                    if (mRecorder != null && newMessage.getText().toString().trim().isEmpty()) {
                        recordingStop(true);
                    }
                    break;
            }
            return false;
        }
    };

    private void recordingStop(boolean send) {
        try {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        } catch (Exception ex) {
            mRecorder = null;
        }
        KeyboardUtil.getInstance(this).closeKeyboard();
        recordTimerStop();
        if (send) {
            newFileUploadTask(recordFilePath, AttachmentTypes.RECORDING, null);
        } else {
            new File(recordFilePath).delete();
        }
    }

    private void recordingStart() {
        if (recordPermissionsAvailable()) {
            File recordFile = new File(Environment.getExternalStorageDirectory(), "/" + getString(R.string.app_name) + "/" + AttachmentTypes.getTypeName(AttachmentTypes.RECORDING) + "/.sent/");
            boolean dirExists = recordFile.exists();
            if (!dirExists)
                dirExists = recordFile.mkdirs();
            if (dirExists) {
                try {
                    recordFile = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name) + "/" + AttachmentTypes.getTypeName(AttachmentTypes.RECORDING) + "/.sent/", System.currentTimeMillis() + ".mp3");
                    if (!recordFile.exists())
                        recordFile.createNewFile();
                    recordFilePath = recordFile.getAbsolutePath();
                    mRecorder = new MediaRecorder();
                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mRecorder.setOutputFile(recordFilePath);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mRecorder.prepare();
                    mRecorder.start();
                    recordTimerStart(System.currentTimeMillis());
                } catch (IOException | IllegalStateException e) {
                    e.printStackTrace();
                    mRecorder = null;
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, permissionsRecord, REQUEST_PERMISSION_RECORD);
        }
    }

    private void recordTimerStart(final long currentTimeMillis) {
        Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();
        recordTimerRunnable = new Runnable() {
            public void run() {
                Long elapsedTime = System.currentTimeMillis() - currentTimeMillis;
                newMessage.setHint(Helper.timeFormater(elapsedTime) + " (Slide left to cancel)");
                recordTimerHandler.postDelayed(this, 1000);
            }
        };
        if (recordTimerHandler == null)
            recordTimerHandler = new Handler();
        recordTimerHandler.post(recordTimerRunnable);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(100);
    }

    private void recordTimerStop() {
        recordTimerHandler.removeCallbacks(recordTimerRunnable);
        newMessage.setHint("Type your message");
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(100);
    }

    private boolean recordPermissionsAvailable() {
        boolean available = true;
        for (String permission : permissionsRecord) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                available = false;
                break;
            }
        }
        return available;
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadEventReceiver, new IntentFilter(Helper.BROADCAST_DOWNLOAD_EVENT));

        userMe = helper.getLoggedInUser();
        if (user != null) {
            DatabaseReference databaseReference = usersRef.child(user.getId());
            databaseReference.addValueEventListener(new ValueEventListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User users = dataSnapshot.getValue(User.class);
                    userMe = helper.getLoggedInUser();
                    if (users != null) {
                        if (users.isOnline()) {
                            if (users.getTimeStamp() != 0 && users.getBlockedUsersIds() != null
                                    && !users.getBlockedUsersIds().contains(userMe.getId()) && userMe.getBlockedUsersIds() != null
                                    && !userMe.getBlockedUsersIds().contains(users.getId()))
                                userStatus.setText("Online");
                            else
                                userStatus.setText("");
                        } else {
                            if (users.getTimeStamp() != 0 && users.getBlockedUsersIds() != null
                                    && !users.getBlockedUsersIds().contains(userMe.getId()) && userMe.getBlockedUsersIds() != null
                                    && !userMe.getBlockedUsersIds().contains(users.getId()))
                                userStatus.setText("last seen " + Helper.getTimeAgoLastSeen(users.getTimeStamp(), ChatActivity.this));
                            else
                                userStatus.setText("");
                        }

                        if (users.getImage() != null && !users.getImage().isEmpty() && users.getBlockedUsersIds() != null
                                && !users.getBlockedUsersIds().contains(userMe.getId()))
                            Picasso.get()
                                    .load(users.getImage())
                                    .tag(this)
                                    .error(R.drawable.ic_avatar)
                                    .placeholder(R.drawable.ic_avatar)
                                    .into(usersImage);
                        else
                            Picasso.get()
                                    .load(R.drawable.ic_avatar)
                                    .tag(this)
                                    .error(R.drawable.ic_avatar)
                                    .placeholder(R.drawable.ic_avatar)
                                    .into(usersImage);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            userStatus.setText("tap here for group info");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(downloadCompleteReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadEventReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaPlayer.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Helper.CHAT_CAB)
            undoSelectionPrepared();
        queryResult.removeChangeListener(realmChangeListener);
        Helper.CURRENT_CHAT_ID = null;
        markAllReadForThisUser();

        if (delete) {
            final Chat chat;
            if (user == null) {
                chat = rChatDb.where(Chat.class).equalTo(chatChild.startsWith(GROUP_PREFIX) ? "groupId" : "userId", chatChild).findFirst();
            } else {
                String userOrGroupId = userMe.getId().equals(senderIdDelete)
                        ? recipientIdDelete : senderIdDelete;
                chat = Helper.getChat(rChatDb, userMe.getId(), userOrGroupId).findFirst();
            }
            if (chat != null) {
                rChatDb.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        RealmList<Message> realmList = chat.getMessages();
                        RealmList<Message> realmList1 = new RealmList<>();
                        for (int i = 0; i < chat.getMessages().size(); i++) {
                            if (user == null && chat.getMessages().get(i).getUserIds() != null
                                    && chat.getMessages().get(i).getUserIds().contains(userMe.getId())) {
                                realmList1.add(chat.getMessages().get(i));
                            } else if (user != null && chat.getMessages().get(i) != null && !chat.getMessages().get(i).getDelete()
                                    .equalsIgnoreCase(MainActivity.userId)) {
                                realmList1.add(chat.getMessages().get(i));
                            }
                        }
                        if (realmList1.size() == 0) {
                            chat.setLastMessage("");
                            chat.setTimeUpdated(0);
                            //                            RealmObject.deleteFromRealm(chat);
                        } else {
                            chat.setLastMessage(realmList1.get(realmList1.size() - 1).getBody());
                            chat.setTimeUpdated(realmList1.get(realmList1.size() - 1).getDate());
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (Helper.CHAT_CAB)
            undoSelectionPrepared();
        else {
            KeyboardUtil.getInstance(this).closeKeyboard();
            finishAfterTransition();
        }
    }

    private void markAllReadForThisUser() {
        Chat thisChat = Helper.getChat(rChatDb, userMe.getId(), userOrGroupId).findFirst();
        if (thisChat != null) {
            rChatDb.beginTransaction();
            thisChat.setRead(true);
            rChatDb.commitTransaction();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_copy:
                StringBuilder stringBuilder = new StringBuilder("");
                for (Message message : dataList) {//Get all selected messages in a String
                    if (message.isSelected() && !TextUtils.isEmpty(message.getBody())) {
                        stringBuilder.append(message.getBody());
                    }
                }
                //Add String in clipboard
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", stringBuilder.toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Messages copied", Toast.LENGTH_SHORT).show();
                undoSelectionPrepared();
                break;
            case R.id.action_delete:
                FragmentManager manager = getSupportFragmentManager();
                Fragment frag = manager.findFragmentByTag(DELETE_TAG);
                if (frag != null) {
                    manager.beginTransaction().remove(frag).commit();
                }

                ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment.newInstance("Delete messages",
                        "Continue deleting selected messages?",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //ArrayList<String> idsToDelete = new ArrayList<>();
                                for (final Message msg : new ArrayList<>(dataList)) {//Delete all selected messages
                                    if (msg.isSelected()) {
                                        try {
                                            if (group != null) {
                                                if (msg.getUserIds() != null) {
                                                    ArrayList<String> deleteUSerIds = new ArrayList<>();
                                                    deleteUSerIds.addAll(msg.getUserIds());
                                                    if (deleteUSerIds.contains(userMe.getId())) {
                                                        deleteUSerIds.remove(userMe.getId());
                                                    }
                                                    chatRef.child(chatChild).child(msg.getId()).child("userIds").setValue(deleteUSerIds);
                                                    senderIdDelete = msg.getSenderId();
                                                    recipientIdDelete = msg.getRecipientId();
                                                    msgID = chatChild;
                                                    bodyDelete = msg.getBody();
                                                    dateDelete = msg.getDate();
                                                    delete = true;
                                                    Helper.deleteMessageFromRealm(rChatDb, msg.getId());
//                                                Helper.updateMessageFromRealm(rChatDb, msg);
                                                }
                                            } else {
                                                if (msg.getDelete() != null && msg.getDelete().isEmpty()) {
                                                    chatRef.child(chatChild).child(msg.getId()).child("delete").setValue(userMe.getId());
                                                    senderIdDelete = msg.getSenderId();
                                                    recipientIdDelete = msg.getRecipientId();
                                                    msgID = msg.getId();
                                                    bodyDelete = msg.getBody();
                                                    dateDelete = msg.getDate();
                                                    delete = true;
                                                    Helper.deleteMessageFromRealm(rChatDb, msg.getId());
//                                                    Helper.updateMessageFromRealm(rChatDb, msg);
                                                } else if (msg.getDelete() != null && !msg.getDelete().isEmpty()) {
//                                                    deleteGroup = true;
                                                    chatRef.child(chatChild).child(msg.getId()).removeValue();
                                                    Helper.deleteMessageFromRealm(rChatDb, msg.getId());
//                                                    Helper.updateMessageFromRealm(rChatDb, msg);
                                                }
                                            }
                                        } catch (DatabaseException ignored) {

                                        }
                                    }
                                }
                               /* for (String idToCompare : idsToDelete) {
                                    Helper.deleteMessageFromRealm(rChatDb, idToCompare);
                                }*/
                                toolbar.getMenu().clear();
                                selectedCount.setVisibility(View.GONE);
                                toolbarContent.setVisibility(View.VISIBLE);
                                Helper.CHAT_CAB = false;
                            }
                        },
                        view -> undoSelectionPrepared());
                confirmationDialogFragment.show(manager, DELETE_TAG);
                break;
            case R.id.action_forward:
                ArrayList<Message> forwardList = new ArrayList<>();
                for (
                        Message msg : dataList)
                    if (msg.isSelected())
                        forwardList.add(rChatDb.copyFromRealm(msg));
                Intent resultIntent = new Intent();
                resultIntent.putParcelableArrayListExtra("FORWARD_LIST", forwardList);

                setResult(Activity.RESULT_OK, resultIntent);

                finish();
                break;
        }
        return true;
    }

    private void registerUserUpdates() {
        newMessage.addTextChangedListener(new TextWatcher() {
            CountDownTimer timer = null;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendMessage.setImageDrawable(ContextCompat.getDrawable(ChatActivity.this, s.length() == 0 ? R.drawable.ic_keyboard_voice_24dp : R.drawable.ic_send));
                if (user != null) {
                    if (timer != null) {
                        timer.cancel();
                        usersRef.child(userMe.getId()).child("typing").setValue(true);
                    }
                    timer = new CountDownTimer(1500, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                            usersRef.child(userMe.getId()).child("typing").setValue(false);
                        }
                    }.start();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private boolean checkIfChatAllowed() {
        boolean allowed = false;
        if (group == null)
            return true;
        if (group.getGrpExitUserIds() == null) {
            allowed = true;
        } else if (group.getGrpExitUserIds() != null && !group.getGrpExitUserIds().contains(userMe.getId())) {
            allowed = true;
        }

        if (!group.getName().equalsIgnoreCase(userName.getText().toString())) {
            userName.setText(group.getName());
        }

        if (!allowed) {
            // sendContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_gray));
            newMessage.setText("");
            newMessage.setHint("You were removed from this group");
            newMessage.setEnabled(false);
            addAttachment.setClickable(false);
            sendMessage.setClickable(false);
            attachment_emoji.setClickable(false);
            camera.setClickable(false);
            sendMessage.setOnTouchListener(null);
        } else {
            //  sendContainer.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
            newMessage.setText("");
            newMessage.setHint("Type your message");
            newMessage.setEnabled(true);
            addAttachment.setClickable(true);
            sendMessage.setClickable(true);
            attachment_emoji.setClickable(true);
            camera.setClickable(true);
            sendMessage.setOnTouchListener(voiceMessageListener);
        }
        return allowed;
    }

    private void checkAndForward() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_DATA_LIST) && checkIfChatAllowed()) {
            ArrayList<Message> toForward = intent.getParcelableArrayListExtra(EXTRA_DATA_LIST);
            if (!toForward.isEmpty()) {
                for (Message msg : toForward) {
                    chatChild = userMe.getId() + "-" + user.getId();
                    BaseApplication.getChatRef().addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!dataSnapshot.hasChild(chatChild)) {
                                chatChild = user.getId() + "-" + userMe.getId();
                            }
                            sendMessage(msg.getBody(), msg.getAttachmentType(), msg.getAttachment());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
        }

    }

    private void showTyping(boolean typing) {
        userMe = helper.getLoggedInUser();
        if (user != null) {
            if (user.getBlockedUsersIds() != null
                    && !user.getBlockedUsersIds().contains(userMe.getId()) && userMe.getBlockedUsersIds() != null
                    && !userMe.getBlockedUsersIds().contains(user.getId())) {
                disableTyping(typing);
            }
        } else {
            disableTyping(typing);
        }
    }

    private void disableTyping(boolean typing) {
        if (dataList != null && dataList.size() > 0 && RealmObject.isValid(dataList.get(dataList.size() - 1))) {
            boolean lastIsTyping = dataList.get(dataList.size() - 1).getAttachmentType() == AttachmentTypes.NONE_TYPING;
            if (typing && !lastIsTyping) {//if last message is not Typing
                dataList.add(new Message(AttachmentTypes.NONE_TYPING));
                messageAdapter.notifyItemInserted(dataList.size() - 1);
                recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
            } else if (lastIsTyping && dataList.size() > 0) {//If last is typing and there is a message in list
                dataList.remove(dataList.size() - 1);
                messageAdapter.notifyItemRemoved(dataList.size());
            }
        }
    }

    private void animateToolbarViews() {
        Animation emotionAnimation = AnimationUtils.makeInChildBottomAnimation(this);
        emotionAnimation.setDuration(400);
        status.startAnimation(emotionAnimation);
        Animation nameAnimation = AnimationUtils.makeInChildBottomAnimation(this);
        nameAnimation.setDuration(420);
        userName.startAnimation(nameAnimation);
    }

    private RealmChangeListener<RealmResults<Chat>> realmChangeListener = new RealmChangeListener<RealmResults<Chat>>() {
        @Override
        public void onChange(RealmResults<Chat> element) {
            if (element.get(0) != null|| element.get(0).getMessages() != null && element.isValid() && element.size() > 0) {
                RealmList<Message> updatedList = element.get(0).getMessages();//updated list of messages
                if (updatedList != null && updatedList.size() > 0) {
                    if (updatedList.size() < dataList.size()) {//if updated items after deletion
                        dataList.clear();
                        // dataList.addAll(element.get(0).getMessages());
                        for (int i = 0; i < element.get(0).getMessages().size(); i++) {
                            //element.get(0).getMessages().get(i).getGrpDeletedMsgIds() != null
                            //&& element.get(0).getMessages().get(i).getGrpDeletedMsgIds().contains(userMe.getId())&&
                            if (element.get(0).getMessages().get(i).getUserIds() != null
                                    && element.get(0).getMessages().get(i).getUserIds().contains(userMe.getId())) {
                                dataList.add(element.get(0).getMessages().get(i));
                            } else if (element.get(0).getMessages().get(i) != null
                                    && !element.get(0).getMessages().get(i).getDelete()
                                    .equalsIgnoreCase(MainActivity.userId)) {
                                if (element.get(0).getMessages().get(i).isBlocked()
                                        && element.get(0).getMessages().get(i).getSenderId().equalsIgnoreCase(userMe.getId()))
                                    dataList.add(element.get(0).getMessages().get(i));
                                else if (!element.get(0).getMessages().get(i).isBlocked())
                                    dataList.add(element.get(0).getMessages().get(i));
                            }
                        }
                        messageAdapter.notifyDataSetChanged();
                    } else { // either new or updated message items
                        try {
                            showTyping(false);//hide typing indicator

                            dataList.clear();
//                            dataList.addAll(updatedList);
//                            messageAdapter.notifyDataSetChanged();

                            for (int i = 0; i < element.get(0).getMessages().size(); i++) {
                                //element.get(0).getMessages().get(i).getGrpDeletedMsgIds() != null
                                //&& element.get(0).getMessages().get(i).getGrpDeletedMsgIds().contains(userMe.getId())&&
                                if (user == null && element.get(0).getMessages().get(i).getUserIds() != null
                                        && element.get(0).getMessages().get(i).getUserIds().contains(userMe.getId())) {
                                    dataList.add(element.get(0).getMessages().get(i));
                                } else if (user != null && element.get(0).getMessages().get(i) != null
                                        && !element.get(0).getMessages().get(i).getDelete()
                                        .equalsIgnoreCase(MainActivity.userId)) {
                                    if (element.get(0).getMessages().get(i).isBlocked()
                                            && element.get(0).getMessages().get(i).getSenderId().equalsIgnoreCase(userMe.getId()))
                                        dataList.add(element.get(0).getMessages().get(i));
                                    else if (!element.get(0).getMessages().get(i).isBlocked())
                                        dataList.add(element.get(0).getMessages().get(i));
                                }
                            }
                            messageAdapter.notifyDataSetChanged();
                            for (int i = 0; i < dataList.size(); i++) {
                                if (dataList.get(i).getRecipientId().equalsIgnoreCase(userMe.getId()) &&
                                        !dataList.get(i).isReadMsg() && dataList.get(i).isDelivered()
                                        && dataList.get(i).getId() != null)
                                    chatRef.child(chatChild).child(dataList.get(i).getId()).child("readMsg").setValue(true);
                            }
                            recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);//scroll to latest message
                        } catch (Exception e) {
                            e.printStackTrace();
                            messageAdapter.notifyDataSetChanged();
                        }
                    }
                } else if (updatedList.size() == 0) {
                    dataList.clear();
                    messageAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_button:
                Helper.closeKeyboard(this, view);
                onBackPressed();
                break;
            case R.id.add_attachment:
                Helper.closeKeyboard(this, view);
                if (user != null) {
                    userMe = helper.getLoggedInUser();
                    if (userMe.getBlockedUsersIds() != null
                            && !userMe.getBlockedUsersIds().contains(user.getId())) {
                        if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
                            myAttachmentLLY.setVisibility(View.GONE);
                            //  addAttachment.animate().setDuration(400).rotationBy(-45).start();
                        } else {
                            myAttachmentLLY.setVisibility(View.VISIBLE);
                            //  addAttachment.animate().setDuration(400).rotationBy(45).start();
                            emojIcon.dismiss();
                        }
                    } else {
                        FragmentManager manager = getSupportFragmentManager();
                        Fragment frag = manager.findFragmentByTag(DELETE_TAG);
                        if (frag != null) {
                            manager.beginTransaction().remove(frag).commit();
                        }

                        Helper.unBlockAlert(user.getNameToDisplay(), userMe, ChatActivity.this,
                                helper, user.getId(), manager);
                    }
                } else {
                    if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
                        myAttachmentLLY.setVisibility(View.GONE);
                        //  addAttachment.animate().setDuration(400).rotationBy(-45).start();
                    } else {
                        myAttachmentLLY.setVisibility(View.VISIBLE);
                        //   addAttachment.animate().setDuration(400).rotationBy(45).start();
                        emojIcon.dismiss();
                    }
                }
                break;
            case R.id.send:
                if (!TextUtils.isEmpty(newMessage.getText().toString().trim()) && user != null) {
                    userMe = helper.getLoggedInUser();
                    if (userMe.getBlockedUsersIds() != null
                            && !userMe.getBlockedUsersIds().contains(user.getId())) {
                        sendMessage(newMessage.getText().toString(), AttachmentTypes.NONE_TEXT, null);
                        newMessage.setText("");
                    } else {
                        FragmentManager manager = getSupportFragmentManager();
                        Fragment frag = manager.findFragmentByTag(DELETE_TAG);
                        if (frag != null) {
                            manager.beginTransaction().remove(frag).commit();
                        }

                        Helper.unBlockAlert(user.getNameToDisplay(), userMe, ChatActivity.this,
                                helper, user.getId(), manager);
                    }
                } else if (!TextUtils.isEmpty(newMessage.getText().toString().trim())) {
                    sendMessage(newMessage.getText().toString(), AttachmentTypes.NONE_TEXT, null);
                    newMessage.setText("");
                }
                break;
            case R.id.chatToolbarContent:
                if (toolbarContent.getVisibility() == View.VISIBLE) {
                    if (user != null)
                        startActivityForResult(ChatDetailActivity.newIntent(this, user), REQUEST_CODE_UPDATE_USER);
                    else if (group != null)
                        startActivityForResult(ChatDetailActivity.newIntent(this, group), REQUEST_CODE_UPDATE_GROUP);
                }
                break;
            case R.id.attachment_contact:
                openContactPicker();
                break;
            case R.id.attachment_emoji:
                emojIcon.toggle();
                break;
            case R.id.attachment_gallery:
                openImagePick();
                break;
            case R.id.camera:
                if (user != null) {
                    userMe = helper.getLoggedInUser();
                    if (userMe.getBlockedUsersIds() != null
                            && !userMe.getBlockedUsersIds().contains(user.getId())) {
                        openImageClick();
                    } else {
                        FragmentManager manager = getSupportFragmentManager();
                        Fragment frag = manager.findFragmentByTag(DELETE_TAG);
                        if (frag != null) {
                            manager.beginTransaction().remove(frag).commit();
                        }

                        Helper.unBlockAlert(user.getNameToDisplay(), userMe, ChatActivity.this,
                                helper, user.getId(), manager);
                    }
                } else {
                    openImageClick();
                }

                /*AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setMessage("Get image from");
                alertDialog.setPositiveButton("Camera", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        openImageClick();
                    }
                });
                alertDialog.setNegativeButton("Gallery", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        openImagePick();
                    }
                });
                alertDialog.create().show();*/
                break;
            case R.id.attachment_audio:
                openAudioPicker();
                break;
            case R.id.attachment_location:
                openPlacePicker();
                break;
            case R.id.attachment_video:
                openVideoPicker();
                break;
            case R.id.attachment_document:
                openDocumentPicker();
                break;
            case R.id.callVideo:
                callIsVideo = true;
                makeCall();
                break;
            case R.id.callAudio:
                callIsVideo = false;
                makeCall();
                break;
        }
    }

    private void makeCall() {
        userMe = helper.getLoggedInUser();
        if (user != null && userMe != null && userMe.getBlockedUsersIds() != null
                && userMe.getBlockedUsersIds().contains(user.getId())) {

            FragmentManager manager = getSupportFragmentManager();
            Fragment frag = manager.findFragmentByTag(DELETE_TAG);
            if (frag != null) {
                manager.beginTransaction().remove(frag).commit();
            }
            Helper.unBlockAlert(user.getNameToDisplay(), userMe, ChatActivity.this,
                    helper, user.getId(), manager);
        } else
            placeCall();
    }

    private void placeCall() {
        if (permissionsAvailable(permissionsSinch)) {
            try {
                Call call = callIsVideo ? getSinchServiceInterface().callUserVideo(user.getId()) : getSinchServiceInterface().callUser(user.getId());
                if (call == null) {
                    // Service failed for some reason, show a Toast and abort
                    Toast.makeText(this, "Service is not started. Try stopping the service and starting it again before placing a call.", Toast.LENGTH_LONG).show();
                    return;
                }
                String callId = call.getCallId();
                startActivity(CallScreenActivity.newIntent(this, user, callId, "OUT"));
            } catch (Exception e) {
                Log.e("CHECK", e.getMessage());
                //ActivityCompat.requestPermissions(this, new String[]{e.getRequiredPermission()}, 0);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissionsSinch, 69);
        }
    }

    private void openDetails() {
        if (toolbarContent.getVisibility() == View.VISIBLE) {
            if (user != null)
                startActivityForResult(ChatDetailActivity.newIntent(this, user), REQUEST_CODE_UPDATE_USER);
            else if (group != null)
                startActivityForResult(ChatDetailActivity.newIntent(this, group), REQUEST_CODE_UPDATE_GROUP);
        }
    }

    private void prepareMessage(String body, int attachmentType, Attachment attachment) {
        Message message = new Message();
        message.setAttachmentType(attachmentType);
        message.setAttachment(attachment);
        message.setBody(body);
        message.setDate(System.currentTimeMillis());
        message.setSenderId(userMe.getId());
        message.setSenderName(userMe.getName());
        message.setSent(false);
        message.setDelivered(false);
        message.setRecipientId(userOrGroupId);
//        message.setId(chatRef.child(chatChild).push().getKey());
        message.setId(attachment.getUrl() + attachment.getBytesCount() + attachment.getName());
        if (group != null && group.getUserIds() != null) {
            ArrayList<String> userIds = new ArrayList<>();
            for (String user : group.getUserIds()) {
                if (group.getUserIds() != null && group.getUserIds().contains(user))
                    userIds.add(user);
            }
            message.setUserIds(userIds);
        }
        if (user != null && user.getBlockedUsersIds() != null
                && user.getBlockedUsersIds().contains(userMe.getId())) {
            message.setBlocked(true);
        }


        Helper.deleteMessageFromRealm(rChatDb, message.getId());

        //Loading attachment message
        newMsgID = message.getId();
        String userId = message.getRecipientId();
        String myId = message.getSenderId();
        Chat chat = Helper.getChat(rChatDb, myId, userId).findFirst();//rChatDb.where(Chat.class).equalTo("myId", myId).equalTo("userId", userId).findFirst();
        boolean commitNow = false;
        if (!rChatDb.isInTransaction()) {
            rChatDb.beginTransaction();
            commitNow = true;
        }
        if (chat == null) {
            chat = rChatDb.createObject(Chat.class);
            chat.setMessages(new RealmList<Message>(rChatDb.copyToRealm(message)));
            chat.setLastMessage(message.getBody());
            chat.setMyId(myId);
            chat.setTimeUpdated(message.getDate());
            if (user != null) {
                chat.setUser(rChatDb.copyToRealm(user));
                chat.setUserId(userId);
            } else {
                chat.setGroupId(group.getId());
                chat.setGroup(rChatDb.copyToRealm(group));
            }
        }
        chat.setTimeUpdated(message.getDate());
        chat.getMessages().add(message);
        chat.setLastMessage(message.getBody());
        if (commitNow) {
            rChatDb.commitTransaction();
        }

    }

    private void sendMessage(String messageBody,
                             @AttachmentTypes.AttachmentType int attachmentType, Attachment attachment) {
        //Create message object
        Message message = new Message();
        message.setAttachmentType(attachmentType);
        if (attachmentType != AttachmentTypes.NONE_TEXT)
            message.setAttachment(attachment);
        else
            BaseMessageViewHolder.animate = true;
        message.setBody(messageBody);
        message.setDate(System.currentTimeMillis());
        message.setSenderId(userMe.getId());
        message.setSenderName(userMe.getName());
        message.setSent(true);
        message.setDelivered(false);
        message.setRecipientId(userOrGroupId);
        message.setId(chatRef.child(chatChild).push().getKey());
        message.setReplyId(replyId);
        message.setDelete("");

        if (user != null && user.getBlockedUsersIds() != null
                && user.getBlockedUsersIds().contains(userMe.getId())) {
            message.setBlocked(true);
        }

        if (group != null && group.getUserIds() != null) {
            ArrayList<String> userIds = new ArrayList<>();
            for (String user : group.getUserIds()) {
                if (group.getGrpExitUserIds() == null) {
                    userIds.add(user);
                } else if (group.getGrpExitUserIds() != null && !group.getGrpExitUserIds().contains(user))
                    userIds.add(user);
            }
            message.setUserIds(userIds);
        }
        //Add messages in chat child
        chatRef.child(chatChild).child(message.getId()).setValue(message);
        replyLay.setVisibility(View.GONE);
        replyId = "0";
        KeyboardUtil.getInstance(this).closeKeyboard();
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

    private void openPlacePicker() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), REQUEST_PLACE_PICKER);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            googleApiAvailability.showErrorDialogFragment(this, googleApiAvailability.isGooglePlayServicesAvailable(this), REQUEST_CODE_PLAY_SERVICES);
            e.printStackTrace();
        }
    }

    void openContactPicker() {
        if (permissionsAvailable(permissionsContact)) {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(contactPickerIntent, REQUEST_CODE_CONTACT);
        } else {
            ActivityCompat.requestPermissions(this, permissionsContact, 14);
        }
    }

    void openAudioPicker() {
        if (permissionsAvailable(permissionsStorage)) {
            audioPicker = new AudioPicker(this);
            audioPicker.setAudioPickerCallback(this);
            audioPicker.pickAudio();
        } else {
            ActivityCompat.requestPermissions(this, permissionsStorage, 25);
        }
    }

    public void openImagePick() {
        if (permissionsAvailable(permissionsStorage)) {
            imagePicker = new ImagePicker(this);
            imagePicker.shouldGenerateMetadata(true);
            imagePicker.shouldGenerateThumbnails(true);
            imagePicker.setImagePickerCallback(this);
            imagePicker.pickImage();
        } else {
            ActivityCompat.requestPermissions(this, permissionsStorage, 36);
        }
    }

    void openImageClick() {
        if (permissionsAvailable(permissionsCamera)) {
            cameraPicker = new CameraImagePicker(this);
            cameraPicker.shouldGenerateMetadata(true);
            cameraPicker.shouldGenerateThumbnails(true);
            cameraPicker.setImagePickerCallback(this);
            pickerPath = cameraPicker.pickImage();
        } else {
            ActivityCompat.requestPermissions(this, permissionsCamera, 47);
        }
    }

    public void openDocumentPicker() {
        if (permissionsAvailable(permissionsStorage)) {
            filePicker = new FilePicker(this);
            filePicker.setFilePickerCallback(this);
            filePicker.setMimeType("application/pdf");
            filePicker.pickFile();
        } else {
            ActivityCompat.requestPermissions(this, permissionsStorage, 58);
        }
    }

    private void openVideoPicker() {
        if (permissionsAvailable(permissionsStorage)) {
            videoPicker = new VideoPicker(this);
            videoPicker.shouldGenerateMetadata(true);
            videoPicker.shouldGeneratePreviewImages(true);
            videoPicker.setVideoPickerCallback(this);
            videoPicker.pickVideo();
        } else {
            ActivityCompat.requestPermissions(this, permissionsStorage, 41);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 14:
                if (permissionsAvailable(permissions))
                    openContactPicker();
                break;
            case 25:
                if (permissionsAvailable(permissions))
                    openAudioPicker();
                break;
            case 36:
                if (permissionsAvailable(permissions))
                    openImagePick();
                break;
            case 47:
                if (permissionsAvailable(permissions))
                    openImageClick();
                break;
            case 58:
                if (permissionsAvailable(permissions))
                    openDocumentPicker();
                break;
            case 69:
                if (permissionsAvailable(permissions))
                    placeCall();
                break;
            case 41:
                if (permissionsAvailable(permissions))
                    openVideoPicker();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case Picker.PICK_IMAGE_DEVICE:
                    if (imagePicker == null) {
                        imagePicker = new ImagePicker(this);
                        imagePicker.setImagePickerCallback(this);
                    }
                    imagePicker.submit(data);
                    break;
                case Picker.PICK_IMAGE_CAMERA:
                    if (cameraPicker == null) {
                        cameraPicker = new CameraImagePicker(this);
                        cameraPicker.setImagePickerCallback(this);
                        cameraPicker.reinitialize(pickerPath);
                    }
                    cameraPicker.submit(data);
                    break;
                case Picker.PICK_VIDEO_DEVICE:
                    if (videoPicker == null) {
                        videoPicker = new VideoPicker(this);
                        videoPicker.setVideoPickerCallback(this);
                    }
                    videoPicker.submit(data);
                    break;
                case Picker.PICK_FILE:
                    filePicker.submit(data);
                    break;
                case Picker.PICK_AUDIO:
                    audioPicker.submit(data);
                    break;
            }
        }
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_UPDATE_USER:
                    user = data.getParcelableExtra(EXTRA_DATA_USER);
                    userUpdated(user);
                    break;
                case REQUEST_CODE_UPDATE_GROUP:
                    group = data.getParcelableExtra(EXTRA_DATA_GROUP);
                    groupUpdated(group);
                    if (group.getImage() != null && !group.getImage().isEmpty())
                        Picasso.get()
                                .load(group.getImage())
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(usersImage);
                    else
                        usersImage.setBackgroundResource(R.drawable.ic_avatar);
                    break;
                case REQUEST_CODE_CONTACT:
                    getSendVCard(data.getData());
                    break;
                case REQUEST_PLACE_PICKER:
                    Place place = PlacePicker.getPlace(this, data);
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("address", place.getAddress().toString());
                        jsonObject.put("latitude", place.getLatLng().latitude);
                        jsonObject.put("longitude", place.getLatLng().longitude);
                        Attachment attachment = new Attachment();
                        attachment.setData(jsonObject.toString());
                        sendMessage(null, AttachmentTypes.LOCATION, attachment);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case REQUEST_CODE_PLAY_SERVICES:
                    openPlacePicker();
                    break;
            }
        }
    }

    private void getSendVCard(Uri contactsData) {
        @SuppressLint("StaticFieldLeak") AsyncTask<Cursor, Void, File> task = new AsyncTask<Cursor, Void, File>() {
            String vCardData;

            @Override
            protected File doInBackground(Cursor... params) {
                Cursor cursor = params[0];
                File toSend = new File(Environment.getExternalStorageDirectory(), "/" + getString(R.string.app_name) + "/Contact/.sent/");
                if (cursor != null && !cursor.isClosed()) {
                    cursor.getCount();
                    if (cursor.moveToFirst()) {
                        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
                        try {
                            AssetFileDescriptor assetFileDescriptor = getContentResolver().openAssetFileDescriptor(uri, "r");
                            if (assetFileDescriptor != null) {
                                FileInputStream inputStream = assetFileDescriptor.createInputStream();
                                boolean dirExists = toSend.exists();
                                if (!dirExists)
                                    dirExists = toSend.mkdirs();
                                if (dirExists) {
                                    try {
                                        toSend = new File(Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name) + "/Contact/.sent/", name + ".vcf");
                                        boolean fileExists = toSend.exists();
                                        if (!fileExists)
                                            fileExists = toSend.createNewFile();
                                        if (fileExists) {
                                            OutputStream stream = new BufferedOutputStream(new FileOutputStream(toSend, false));
                                            byte[] buffer = readAsByteArray(inputStream);
                                            vCardData = new String(buffer);
                                            stream.write(buffer);
                                            stream.close();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } catch (FileNotFoundException e) {
                            Log.e(ChatActivity.class.getSimpleName(), "Vcard for the Contact " + lookupKey + " not found", e);
                        } catch (IOException e) {
                            Log.e(ChatActivity.class.getSimpleName(), "Problem creating stream from the assetFileDescriptor.", e);
                        } finally {
                            cursor.close();
                        }
                    }
                }
                return toSend;
            }

            @Override
            protected void onPostExecute(File f) {
                super.onPostExecute(f);
                if (f != null && !TextUtils.isEmpty(vCardData)) {
                    Attachment attachment = new Attachment();
                    attachment.setData(vCardData);
                    newFileUploadTask(f.getAbsolutePath(), AttachmentTypes.CONTACT, attachment);
                }
            }
        };
        task.execute(getContentResolver().query(contactsData, null, null, null, null));
    }

    public byte[] readAsByteArray(InputStream ios) throws IOException {
        ByteArrayOutputStream ous = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        } finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        return ous.toByteArray();
    }

    private void uploadImage(String filePath) {
        newFileUploadTask(filePath, AttachmentTypes.IMAGE, null);
    }

    private void uploadThumbnail(final String filePath) {
        Toast.makeText(this, "Just a moment..", Toast.LENGTH_LONG).show();
        File file = new File(filePath);
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(getString(R.string.app_name)).child("video").child("thumbnail").child(file.getName() + ".jpg");
        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //If thumbnail exists
                Attachment attachment = new Attachment();
                attachment.setData(uri.toString());
                newFileUploadTask(filePath, AttachmentTypes.VIDEO, attachment);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                @SuppressLint("StaticFieldLeak") AsyncTask<String, Void, Bitmap> thumbnailTask = new AsyncTask<String, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(String... params) {
                        //Create thumbnail
                        return ThumbnailUtils.createVideoThumbnail(params[0], MediaStore.Video.Thumbnails.MINI_KIND);
                    }

                    @SuppressLint("WrongThread")
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        if (bitmap != null) {
                            //Upload thumbnail and then upload video
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            byte[] data = baos.toByteArray();
                            UploadTask uploadTask = storageReference.putBytes(data);
                            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                    if (!task.isSuccessful()) {
                                        throw task.getException();
                                    }
                                    // Continue with the task to get the download URL
                                    return storageReference.getDownloadUrl();
                                }
                            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        Uri downloadUri = task.getResult();
                                        Attachment attachment = new Attachment();
                                        attachment.setData(downloadUri.toString());
                                        newFileUploadTask(filePath, AttachmentTypes.VIDEO, attachment);
                                    } else {
                                        newFileUploadTask(filePath, AttachmentTypes.VIDEO, null);
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    newFileUploadTask(filePath, AttachmentTypes.VIDEO, null);
                                }
                            });
                        } else
                            newFileUploadTask(filePath, AttachmentTypes.VIDEO, null);
                    }
                };
                thumbnailTask.execute(filePath);
            }
        });
    }

    private void newFileUploadTask(String filePath,
                                   @AttachmentTypes.AttachmentType final int attachmentType, final Attachment attachment) {
        if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
            myAttachmentLLY.setVisibility(View.GONE);
            //  addAttachment.animate().setDuration(400).rotationBy(-45).start();
        }

        final File fileToUpload = new File(filePath);
        final String fileName = Uri.fromFile(fileToUpload).getLastPathSegment();

        Attachment preSendAttachment = attachment;//Create/Update attachment
        if (preSendAttachment == null) preSendAttachment = new Attachment();
        preSendAttachment.setName(fileName);
        preSendAttachment.setBytesCount(fileToUpload.length());
        preSendAttachment.setUrl("loading");
        prepareMessage(null, attachmentType, preSendAttachment);

        checkAndCopy("/" + getString(R.string.app_name) + "/" +
                AttachmentTypes.getTypeName(attachmentType) + "/.sent/", fileToUpload);//Make a copy

        Intent intent = new Intent(Helper.UPLOAD_AND_SEND);
        intent.putExtra("attachment", attachment);
        if (group != null) {
            intent.putExtra("chatDataGroup", group);
        }
        intent.putExtra("userIds", attachment);
        intent.putExtra("attachment_type", attachmentType);
        intent.putExtra("attachment_file_path", filePath);
        intent.putExtra("attachment_file_path", filePath);
        intent.putExtra("attachment_recipient_id", userOrGroupId);
        intent.putExtra("attachment_chat_child", chatChild);
        intent.putExtra("attachment_reply_id", replyId);
        intent.putExtra("new_msg_id", newMsgID);
        intent.putExtra("statusUrl", "");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        replyLay.setVisibility(View.GONE);
        replyId = "0";
        KeyboardUtil.getInstance(this).closeKeyboard();
    }

    public void downloadFile(DownloadFileEvent downloadFileEvent) {
        if (permissionsAvailable(permissionsStorage)) {
            new DownloadUtil().checkAndLoad(this, downloadFileEvent);
            adapterPositions.add(downloadFileEvent.getPosition());
        } else {
            ActivityCompat.requestPermissions(this, permissionsStorage, 47);
        }
    }

    @Override
    public void OnMessageClick(Message message, int position) {
        if (Helper.CHAT_CAB && RealmObject.isValid(message)) {
            message.setSelected(!message.isSelected());//Toggle message selection
            messageAdapter.notifyItemChanged(position);//Notify changes

            if (message.isSelected())
                countSelected++;
            else
                countSelected--;

            selectedCount.setText(String.valueOf(countSelected));//Update count
            if (countSelected == 0)
                undoSelectionPrepared();//If count is zero then reset selection
        }
    }

    @Override
    public void OnMessageLongClick(Message message, int position) {
        if (!Helper.CHAT_CAB && RealmObject.isValid(message)) {//Prepare selection if not in selection mode
            prepareToSelect();
            message.setSelected(true);
            messageAdapter.notifyItemChanged(position);
            countSelected++;
            selectedCount.setText(String.valueOf(countSelected));
        }
    }

    private void prepareToSelect() {
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_chat_cab);
        getSupportActionBar().setTitle("");
        selectedCount.setText("1");
        selectedCount.setVisibility(View.VISIBLE);
        toolbarContent.setVisibility(View.GONE);
        Helper.CHAT_CAB = true;
    }

    private void undoSelectionPrepared() {
        for (Message msg : dataList) {
            msg.setSelected(false);
        }
        countSelected = 0;
        messageAdapter.notifyDataSetChanged();
        toolbar.getMenu().clear();
        selectedCount.setVisibility(View.GONE);
        toolbarContent.setVisibility(View.VISIBLE);
        Helper.CHAT_CAB = false;
    }

    public static Intent newIntent(Context context, ArrayList<Message> forwardMessages, User
            user) {
        //intent contains user to chat with and message forward list if any.
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_DATA_USER, user);
        //intent.removeExtra(EXTRA_DATA_GROUP);
        if (forwardMessages == null)
            forwardMessages = new ArrayList<>();
        intent.putParcelableArrayListExtra(EXTRA_DATA_LIST, forwardMessages);
        return intent;
    }

    public static Intent newIntent(Context
                                           context, ArrayList<Message> forwardMessages, Group group) {
        //intent contains user to chat with and message forward list if any.
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_DATA_GROUP, group);
//        intent.putExtra(EXTRA_DATA_GROUP1, group);
        //intent.removeExtra(EXTRA_DATA_USER);
        if (forwardMessages == null)
            forwardMessages = new ArrayList<>();
        intent.putParcelableArrayListExtra(EXTRA_DATA_LIST, forwardMessages);
        return intent;
    }

    @Override
    public boolean isRecordingPlaying(String fileName) {
        return isMediaPlayerPlaying() && currentlyPlaying.equals(fileName);
    }

    private boolean isMediaPlayerPlaying() {
        try {
            return mediaPlayer.isPlaying();
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    @Override
    public void playRecording(File file, String fileName, int position) {
        if (recordPermissionsAvailable()) {
            if (isMediaPlayerPlaying()) {
                mediaPlayer.stop();
                notifyRecordingPlaybackCompletion();
                if (!fileName.equals(currentlyPlaying)) {
                    if (startPlayback(file)) {
                        currentlyPlaying = fileName;
                        messageAdapter.notifyItemChanged(position);
                    }
                }
            } else {
                if (startPlayback(file)) {
                    currentlyPlaying = fileName;
                    messageAdapter.notifyItemChanged(position);
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, permissionsRecord, REQUEST_PERMISSION_RECORD);
        }
    }

    private boolean startPlayback(File file) {
        boolean started = true;
        resetMediaPlayer();
        try {
            FileInputStream is = new FileInputStream(file);
            FileDescriptor fd = is.getFD();
            mediaPlayer.setDataSource(fd);
            is.close();
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            started = false;
        }
        return started;
    }

    private void resetMediaPlayer() {
        try {
            mediaPlayer.reset();
        } catch (IllegalStateException ex) {
            mediaPlayer = new MediaPlayer();
        }
    }

    private void notifyRecordingPlaybackCompletion() {
        if (recyclerView != null && messageAdapter != null) {
            int total = dataList.size();
            for (int i = total - 1; i >= 0; i--) {
                /*if (dataList.get(i).getAttachment() != null
                        &&
                        dataList.get(i).getAttachment().getName().equals(currentlyPlaying)) {
                    messageAdapter.notifyItemChanged(i);
                    break;
                }*/
                if (dataList.get(i).getAttachment() != null) {
                    if (dataList.get(i).getAttachment().getName().contains(".wav")) {
                        if (dataList.get(i).getAttachment().getName().replace(".wav", ".mp3").equals(currentlyPlaying)) {
                            messageAdapter.notifyItemChanged(i);
                            break;
                        }
                    } else {
                        if (dataList.get(i).getAttachment().getName().equals(currentlyPlaying)) {
                            messageAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }


            }

        }
    }

    @Override
    public void onVideosChosen(List<ChosenVideo> list) {
        if (list != null && !list.isEmpty()) {
            if (list.get(0).getSize() < 16777216)
                uploadThumbnail(Uri.parse(list.get(0).getOriginalPath()).getPath());
            else
                Toast.makeText(this, "Maximum limit is 16 MB", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAudiosChosen(List<ChosenAudio> list) {
        if (list != null && !list.isEmpty())
            newFileUploadTask(Uri.parse(list.get(0).getOriginalPath()).getPath(), AttachmentTypes.AUDIO, null);
    }

    @Override
    public void onFilesChosen(List<ChosenFile> list) {
        if (list != null && !list.isEmpty())
            newFileUploadTask(Uri.parse(list.get(0).getOriginalPath()).getPath(),
                    AttachmentTypes.DOCUMENT, null);
    }

    @Override
    public void onImagesChosen(List<ChosenImage> list) {
        if (list != null && !list.isEmpty()) {
            Uri originalFileUri = Uri.parse(list.get(0).getOriginalPath());
            File tempFile = new File(getCacheDir(), originalFileUri.getLastPathSegment());
            try {
                uploadImage(SiliCompressor.with(this).compress(originalFileUri.toString(), tempFile));
            } catch (Exception ex) {
                uploadImage(originalFileUri.getPath());
            }
        }
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("picker_path", pickerPath);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        if (savedInstanceState.containsKey("picker_path")) {
            pickerPath = savedInstanceState.getString("picker_path");
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void initSwipe() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int i) {
                replyLay.setVisibility(View.VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        messageAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                }, 500);
                String nameToDisplay = "";
                if (myUsersNameInPhoneMap != null && myUsersNameInPhoneMap
                        .containsKey(dataList.get(viewHolder.getAdapterPosition()).getSenderId())) {
                    nameToDisplay = myUsersNameInPhoneMap.get(dataList.get(viewHolder.getAdapterPosition())
                            .getSenderId()).getNameToDisplay();
                }
                replyId = dataList.get(viewHolder.getAdapterPosition()).getId();
                replyName.setText(nameToDisplay);
                replyImg.setBackgroundResource(0);
                replyImg.setVisibility(View.VISIBLE);
                if (dataList.get(viewHolder.getAdapterPosition()).getAttachmentType() == AttachmentTypes.AUDIO) {
                    Picasso.get()
                            .load(R.drawable.ic_audiotrack_24dp)
                            .tag(ChatActivity.this)
                            .placeholder(R.drawable.ic_audiotrack_24dp)
                            .into(replyImg);
                } else if (dataList.get(viewHolder.getAdapterPosition()).getAttachmentType() == AttachmentTypes.RECORDING) {
                    Picasso.get()
                            .load(R.drawable.ic_audiotrack_24dp)
                            .tag(ChatActivity.this)
                            .placeholder(R.drawable.ic_audiotrack_24dp)
                            .into(replyImg);
                } else if (dataList.get(viewHolder.getAdapterPosition()).getAttachmentType() == AttachmentTypes.VIDEO) {
                    if (dataList.get(viewHolder.getAdapterPosition()).getAttachment().getData() != null)
                        Picasso.get()
                                .load(dataList.get(viewHolder.getAdapterPosition()).getAttachment().getData())
                                .tag(ChatActivity.this)
                                .placeholder(R.drawable.ic_placeholder)
                                .into(replyImg);
                    else
                        replyImg.setBackgroundResource(R.drawable.ic_placeholder);
                } else if (dataList.get(viewHolder.getAdapterPosition()).getAttachmentType() == AttachmentTypes.IMAGE) {
                    if (dataList.get(viewHolder.getAdapterPosition()).getAttachment().getUrl() != null)
                        Picasso.get()
                                .load(dataList.get(viewHolder.getAdapterPosition()).getAttachment().getUrl())
                                .tag(ChatActivity.this)
                                .placeholder(R.drawable.ic_placeholder)
                                .into(replyImg);
                    else
                        replyImg.setBackgroundResource(R.drawable.ic_placeholder);
                } else if (dataList.get(viewHolder.getAdapterPosition()).getAttachmentType() == AttachmentTypes.CONTACT) {
                    Picasso.get()
                            .load(R.drawable.ic_person_black_24dp)
                            .tag(ChatActivity.this)
                            .placeholder(R.drawable.ic_person_black_24dp)
                            .into(replyImg);
                } else if (dataList.get(viewHolder.getAdapterPosition()).getAttachmentType() == AttachmentTypes.LOCATION) {
                    try {
                        String staticMap = "https://maps.googleapis.com/maps/api/staticmap?center=%s,%s&zoom=16&size=512x512&format=png";
                        String Key = "&key="+"AIzaSyCB7xTbA6NzO0V0JWtOGo0cFcrKP-9DTrY";
                        String latitude, longitude;
                        JSONObject placeData = new JSONObject(dataList.get(viewHolder.getAdapterPosition()).getAttachment().getData());
                        replyName.setText(nameToDisplay + "\n" + placeData.getString("address"));
                        latitude = placeData.getString("latitude");
                        longitude = placeData.getString("longitude");
                        Picasso.get()
                                .load(String.format(staticMap, latitude, longitude) + Key)
                                .tag(ChatActivity.this)
                                .into(replyImg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (dataList.get(viewHolder.getAdapterPosition()).getAttachmentType() == AttachmentTypes.DOCUMENT) {
                    Picasso.get()
                            .load(R.drawable.ic_insert_64dp)
                            .tag(ChatActivity.this)
                            .placeholder(R.drawable.ic_insert_64dp)
                            .into(replyImg);
                    replyName.setText(nameToDisplay + "\n" + dataList.get(viewHolder.getAdapterPosition()).getAttachment().getName());
                } else if (dataList.get(viewHolder.getAdapterPosition()).getAttachmentType() == AttachmentTypes.NONE_TEXT) {
                    replyName.setText(nameToDisplay + "\n" + dataList.get(viewHolder.getAdapterPosition()).getBody());
                    replyImg.setVisibility(View.GONE);
                }

                closeReply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        replyLay.setVisibility(View.GONE);
                        replyId = "0";
                    }
                });
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView
                    recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX / 4, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public MessageAdapter getMessageAdapter() {
        return messageAdapter;
    }

    public void moveToPosition(int pos) {
        recyclerView.getLayoutManager().scrollToPosition(pos);
        setBackgroundColor(pos);
    }

    public void setBackgroundColor(int position) {
        for (int i = 0; i < dataList.size(); i++) {
            View view = recyclerView.getLayoutManager().findViewByPosition(i);
            try {
                if (view != null) {
                    if (i == position)
                        view.setBackgroundColor(Color.parseColor("#504cbee7"));
                    else
                        view.setBackgroundColor(Color.WHITE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setRead(Chat prevChat) {
        if (prevChat != null) {
            for (int i = 0; i < prevChat.getMessages().size(); i++) {
                if (prevChat.getMessages().get(i) != null &&
                        !prevChat.getMessages().get(i).getDelete().equalsIgnoreCase(MainActivity.userId)) {
                    if (prevChat.getMessages().get(i).isBlocked()
                            && prevChat.getMessages().get(i).getSenderId().equalsIgnoreCase(userMe.getId()))
                        this.dataList.add(prevChat.getMessages().get(i));
                    else if (!prevChat.getMessages().get(i).isBlocked())
                        this.dataList.add(prevChat.getMessages().get(i));
                }
            }
            for (int i = 0; i < dataList.size(); i++) {
                if (!dataList.get(i).getSenderId().equalsIgnoreCase(userMe.getId()) && dataList.get(i).isDelivered()
                        && !dataList.get(i).isReadMsg() && dataList.get(i).getId() != null)
                    chatRef.child(chatChild).child(dataList.get(i).getId()).child("readMsg").setValue(true);
            }
            messageAdapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    private String getChatChild(final Chat prevChat) {
        chatChild = userMe.getId() + "-" + user.getId();
        BaseApplication.getChatRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(chatChild)) {
                    chatChild = user.getId() + "-" + userMe.getId();
                }
                setRead(prevChat);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return chatChild;
    }
}
