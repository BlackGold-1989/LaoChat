package com.laodev.chatapp.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.models.Attachment;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Chat;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.Group;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.Status;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.status.CircleView;
import com.laodev.chatapp.status.StoryStatusView;
import com.laodev.chatapp.status.glideProgressBar.DelayBitmapTransformation;
import com.laodev.chatapp.status.glideProgressBar.LoggingListener;
import com.laodev.chatapp.status.glideProgressBar.ProgressTarget;
import com.laodev.chatapp.utils.FileUtils;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.viewHolders.BaseMessageViewHolder;
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
import com.squareup.picasso.Picasso;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.listeners.OnEmojiPopupShownListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.realm.RealmList;
import io.realm.RealmQuery;

public class StatusStoriesActivity extends BaseActivity implements StoryStatusView.UserInteractionListener,
        View.OnClickListener, ImagePickerCallback, FilePickerCallback, AudioPickerCallback, VideoPickerCallback {
    private static final int REQUEST_PERMISSION_RECORD = 159;
    private static final int REQUEST_PLACE_PICKER = 2;
    private static final int REQUEST_CODE_CONTACT = 1;
    private static final int REQUEST_CODE_PLAY_SERVICES = 3;
    public static final String STATUS_RESOURCES_KEY = "statusStoriesResources";
    public static final String STATUS_DURATION_KEY = "statusStoriesDuration";
    public static final String IS_IMMERSIVE_KEY = "isImmersive";
    public static final String IS_CACHING_ENABLED_KEY = "isCaching";
    public static final String IS_TEXT_PROGRESS_ENABLED_KEY = "isText";
    public static final String USER_NAME = "userName";
    public static final String URL = "url";
    public static final String RECIPIENT_ID = "recipientId";
    public static final String fromPositionStr = "fromPosition";
    public static final int fromPosition = 0;
    public static boolean FROM = false;

    private static StoryStatusView storyStatusView;
    private ImageView image;
    private int counter = 0;

    private String[] statusResources;
    private boolean isImmersive = true;
    private boolean isCaching = true;
    private static boolean isTextEnabled = true;
    private ProgressTarget<String, Bitmap> target;
    private ImageView addAttachment;
    private ImageView sendMessage;
    private EmojiPopup emojIcon;
    private LinearLayout myAttachmentLLY, sendContainer;
    private EmojiEditText newMessage;
    private Handler recordWaitHandler, recordTimerHandler;
    private Runnable recordRunnable, recordTimerRunnable;
    private float displayWidth;
    private String recordFilePath, userOrGroupId, chatChild;
    private MediaRecorder mRecorder = null;
    private VideoPicker videoPicker;
    private ImagePicker imagePicker;
    private CameraImagePicker cameraPicker;
    private String pickerPath;
    private AudioPicker audioPicker;
    private FilePicker filePicker;
    private LinearLayout statusReply;
    String newMsgID = "";
    private User statusUser;

    @Override
    void myUsersResult(ArrayList<User> myUsers) {

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_stories);

        statusResources = getIntent().getStringArrayExtra(STATUS_RESOURCES_KEY);
        //    private long[] statusResourcesDuration;
        long statusDuration = getIntent().getLongExtra(STATUS_DURATION_KEY, 5000L);
        isImmersive = getIntent().getBooleanExtra(IS_IMMERSIVE_KEY, true);
        isCaching = getIntent().getBooleanExtra(IS_CACHING_ENABLED_KEY, true);
        isTextEnabled = getIntent().getBooleanExtra(IS_TEXT_PROGRESS_ENABLED_KEY, true);

        ProgressBar imageProgressBar = findViewById(R.id.imageProgressBar);
        TextView textView = findViewById(R.id.textView);
        image = findViewById(R.id.image);
        CircleView userImage = findViewById(R.id.userImage);
        TextView userName = findViewById(R.id.userName);

        ImageView attachment_emoji = findViewById(R.id.attachment_emoji);
        RelativeLayout rootView = findViewById(R.id.rootView);
        myAttachmentLLY = findViewById(R.id.layout_chat_attachment_LLY);
        addAttachment = findViewById(R.id.add_attachment);
        sendContainer = findViewById(R.id.sendContainer);
        statusReply = findViewById(R.id.statusReply);

        if (!getIntent().getExtras().getString(URL).isEmpty()) {
            Picasso.get()
                    .load(getIntent().getExtras().getString(URL))
                    .tag(this)
                    .placeholder(R.drawable.ic_avatar)
                    .into(userImage);
        } else {
            Picasso.get()
                    .load(R.drawable.ic_avatar)
                    .tag(this)
                    .placeholder(R.drawable.ic_avatar)
                    .into(userImage);
        }

        userName.setText(getIntent().getExtras().getString(USER_NAME));

        storyStatusView = findViewById(R.id.storiesStatus);
        storyStatusView.setStoriesCount(statusResources.length);
        storyStatusView.setStoryDuration(statusDuration);
        newMessage = findViewById(R.id.new_message);
        sendMessage = findViewById(R.id.send);

        if (getIntent().getExtras().getString(USER_NAME).equalsIgnoreCase("My Status")) {
            statusReply.setVisibility(View.GONE);
        } else {
            statusReply.setVisibility(View.VISIBLE);
        }

        if (FROM)
            statusReply.setVisibility(View.GONE);
        else
            statusReply.setVisibility(View.VISIBLE);


        findViewById(R.id.attachment_video).setOnClickListener(this);
        findViewById(R.id.attachment_contact).setOnClickListener(this);
        findViewById(R.id.attachment_gallery).setOnClickListener(this);
        findViewById(R.id.camera).setOnClickListener(this);
        findViewById(R.id.attachment_audio).setOnClickListener(this);
        findViewById(R.id.attachment_location).setOnClickListener(this);
        findViewById(R.id.attachment_document).setOnClickListener(this);
        findViewById(R.id.add_attachment).setOnClickListener(this);

        userOrGroupId = getIntent().getExtras().getString(RECIPIENT_ID);
        BaseApplication.getUserRef().child(userOrGroupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                statusUser = dataSnapshot.getValue(User.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        chatChild = getChatChild();

        newMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                storyStatusView.pause();
        });

        // or
        // statusView.setStoriesCountWithDurations(statusResourcesDuration);
        storyStatusView.setUserInteractionListener(this);
        //     storyStatusView.playStories();
        target = new MyProgressTarget<>(new BitmapImageViewTarget(image), imageProgressBar, textView);
        image.setOnClickListener(v -> storyStatusView.skip());
        displayWidth = Helper.getDisplayWidth(this);
        storyStatusView.pause();
        target.setModel(statusResources[counter]);
        Glide.with(image.getContext())
                .load(target.getModel())
                .asBitmap()
                .crossFade()
                .skipMemoryCache(!isCaching)
                .diskCacheStrategy(isCaching ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .transform(new CenterCrop(image.getContext()), new DelayBitmapTransformation(1000))
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target,
                                                   boolean isFromMemoryCache, boolean isFirstResource) {
                        storyStatusView.playStories();
                        return false;
                    }
                })
                .into(target);
//.listener(new LoggingListener<String, Bitmap>())
        // bind reverse view
        findViewById(R.id.reverse).setOnClickListener(v -> storyStatusView.reverse());

        // bind skip view
        findViewById(R.id.skip).setOnClickListener(v -> storyStatusView.skip());

        findViewById(R.id.actions).setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                storyStatusView.pause();
            } else {
                storyStatusView.resume();
                if (sendContainer.getVisibility() == View.VISIBLE)
                    sendContainer.setVisibility(View.GONE);
                if (myAttachmentLLY.getVisibility() == View.VISIBLE)
                    myAttachmentLLY.setVisibility(View.GONE);

                if (getIntent().getExtras().getString(USER_NAME).equalsIgnoreCase("My Status")) {
                    statusReply.setVisibility(View.GONE);
                } else {
                    statusReply.setVisibility(View.VISIBLE);
                }
                emojIcon.dismiss();
            }
            return true;
        });

        statusReply.setOnClickListener(v -> {
            storyStatusView.pause();
            if (statusReply.getVisibility() == View.VISIBLE) {
                statusReply.setVisibility(View.GONE);
            }
            sendContainer.setVisibility(View.VISIBLE);
        });

        emojIcon = EmojiPopup.Builder.fromRootView(rootView).setOnEmojiPopupShownListener(() -> {
            if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
                myAttachmentLLY.setVisibility(View.GONE);
                addAttachment.animate().setDuration(400).rotationBy(-45).start();
            }
        }).build(newMessage);

        newMessage.setOnTouchListener((v, event) -> {
            if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
                myAttachmentLLY.setVisibility(View.GONE);
                addAttachment.animate().setDuration(400).rotationBy(-45).start();
            }
            return false;
        });
        sendMessage.setOnTouchListener(voiceMessageListener);

        attachment_emoji.setOnClickListener(v -> emojIcon.toggle());

        sendMessage.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(newMessage.getText().toString().trim()) && statusUser != null) {
                userMe = helper.getLoggedInUser();
                if (userMe.getBlockedUsersIds() != null
                        && !userMe.getBlockedUsersIds().contains(statusUser.getId())) {
                    sendContainer.setVisibility(View.GONE);
                    statusReply.setVisibility(View.VISIBLE);
                    hideKeyboardFrom(StatusStoriesActivity.this, v);
                    sendMessage(newMessage.getText().toString(), AttachmentTypes.NONE_TEXT, null);
                    newMessage.setText("");
                    storyStatusView.resume();
                } else {
                    FragmentManager manager = getSupportFragmentManager();
                    Fragment frag = manager.findFragmentByTag("DELETE_TAG");
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }

                    Helper.unBlockAlert(getIntent().getExtras().getString(USER_NAME), userMe,
                            StatusStoriesActivity.this,
                            helper, statusUser.getId(), manager);
                }
            } else if (!TextUtils.isEmpty(newMessage.getText().toString().trim())) {
                sendContainer.setVisibility(View.GONE);
                statusReply.setVisibility(View.VISIBLE);
                hideKeyboardFrom(StatusStoriesActivity.this, v);
                sendMessage(newMessage.getText().toString(), AttachmentTypes.NONE_TEXT, null);
                newMessage.setText("");
                storyStatusView.resume();
            }
        });

        registerUserUpdates();

        findViewById(R.id.back).setOnClickListener(v -> {
            storyStatusView.pause();
            finish();
        });
    }

    private void registerUserUpdates() {
        newMessage.addTextChangedListener(new TextWatcher() {
            CountDownTimer timer = null;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendMessage.setImageDrawable(ContextCompat.getDrawable(StatusStoriesActivity.this,
                        s.length() == 0 ? R.drawable.ic_keyboard_voice_24dp : R.drawable.ic_send));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private View.OnTouchListener voiceMessageListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", "touched down");
                    if (newMessage.getText().toString().trim().isEmpty()) {
                        if (recordWaitHandler == null)
                            recordWaitHandler = new Handler();
                        recordRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (statusUser != null) {
                                    userMe = helper.getLoggedInUser();
                                    if (userMe.getBlockedUsersIds() != null
                                            && !userMe.getBlockedUsersIds().contains(statusUser.getId())) {
                                        recordingStart();
                                    } else {
                                        FragmentManager manager = getSupportFragmentManager();
                                        Fragment frag = manager.findFragmentByTag("DELETE_TAG");
                                        if (frag != null) {
                                            manager.beginTransaction().remove(frag).commit();
                                        }

                                        Helper.unBlockAlert(getIntent().getExtras().getString(USER_NAME),
                                                userMe, StatusStoriesActivity.this,
                                                helper, statusUser.getId(), manager);
                                    }
                                } else {
                                    recordingStart();
                                }
                            }
                        };
                        recordWaitHandler.postDelayed(recordRunnable, 600);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i("TAG", "moving: (" + displayWidth + ", " + x + ")");
                    if (mRecorder != null && newMessage.getText().toString().trim().isEmpty()) {
                        if (Math.abs(event.getX()) / displayWidth > 0.35f) {
                            recordingStop(false);
                            hideKeyboardFrom(StatusStoriesActivity.this, v);
                            Toast.makeText(StatusStoriesActivity.this, "Recording cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", "touched up");
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

    private void sendMessage(String messageBody, @AttachmentTypes.AttachmentType int attachmentType,
                             Attachment attachment) {
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
        message.setStatusUrl(statusResources[counter]);

        //Add messages in chat child
        chatRef.child(chatChild).child(message.getId()).setValue(message);
    }


    private void recordingStart() {
        if (recordPermissionsAvailable()) {
            File recordFile = new File(Environment.getExternalStorageDirectory(),
                    "/" + getString(R.string.app_name) + "/" + AttachmentTypes.getTypeName(AttachmentTypes.RECORDING) + "/.sent/");
            boolean dirExists = recordFile.exists();
            if (!dirExists)
                dirExists = recordFile.mkdirs();
            if (dirExists) {
                try {
                    recordFile = new File(Environment.getExternalStorageDirectory()
                            + "/" + getString(R.string.app_name) + "/" + AttachmentTypes.getTypeName(AttachmentTypes.RECORDING)
                            + "/.sent/", System.currentTimeMillis() + ".mp3");
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
                long elapsedTime = System.currentTimeMillis() - currentTimeMillis;
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
    public void onNext() {
        storyStatusView.pause();
        ++counter;
        target.setModel(statusResources[counter]);
        Glide.with(image.getContext())
                .load(target.getModel())
                .asBitmap()
                .crossFade()
                .centerCrop()
                .skipMemoryCache(!isCaching)
                .diskCacheStrategy(isCaching ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .transform(new CenterCrop(image.getContext()), new DelayBitmapTransformation(1000))
                .listener(new LoggingListener<String, Bitmap>())
                .into(target);
    }


    private void recordingStop(boolean send) {
        try {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        } catch (IllegalStateException ex) {
            mRecorder = null;
        }
        sendContainer.setVisibility(View.GONE);
        recordTimerStop();
        if (send) {
            newFileUploadTask(recordFilePath, AttachmentTypes.RECORDING, null);
        } else {
            new File(recordFilePath).delete();
        }
    }

    private void recordTimerStop() {
        recordTimerHandler.removeCallbacks(recordTimerRunnable);
        newMessage.setHint("Type your message");
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(100);
    }

    private void newFileUploadTask(String filePath,
                                   @AttachmentTypes.AttachmentType final int attachmentType, final Attachment attachment) {
        if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
            myAttachmentLLY.setVisibility(View.GONE);
            //  addAttachment.animate().setDuration(400).rotationBy(-45).start();
        }
        if (sendContainer.getVisibility() == View.VISIBLE)
            sendContainer.setVisibility(View.GONE);

        if (statusReply.getVisibility() == View.GONE) {
            statusReply.setVisibility(View.VISIBLE);
        }
        storyStatusView.resume();
        final File fileToUpload = new File(filePath);
        final String fileName = Uri.fromFile(fileToUpload).getLastPathSegment();

        Attachment preSendAttachment = attachment;//Create/Update attachment
        if (preSendAttachment == null) preSendAttachment = new Attachment();
        preSendAttachment.setName(fileName);
        preSendAttachment.setBytesCount(fileToUpload.length());
        preSendAttachment.setUrl("loading");
        prepareMessage(null, attachmentType, preSendAttachment);

        checkAndCopy("/" + getString(R.string.app_name) + "/" + AttachmentTypes.getTypeName(attachmentType) + "/.sent/", fileToUpload);//Make a copy

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
        intent.putExtra("attachment_reply_id", "0");
        intent.putExtra("new_msg_id", newMsgID);
        intent.putExtra("statusUrl", statusResources[counter]);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        //hideKeyboard();
    }

    private void checkAndCopy(String directory, File source) {
        //Create and copy file content
        File file = new File(Environment.getExternalStorageDirectory(), directory);
        boolean dirExists = file.exists();
        if (!dirExists)
            dirExists = file.mkdirs();
        if (dirExists) {
            try {
                file = new File(Environment.getExternalStorageDirectory() + directory,
                        Uri.fromFile(source).getLastPathSegment());
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

    private void prepareMessage(String body, int attachmentType, Attachment attachment) {
        try {
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
            message.setId(attachment.getUrl() + attachment.getBytesCount() + attachment.getName());
            message.setStatusUrl(statusResources[counter]);
            Helper.deleteMessageFromRealm(rChatDb, message.getId());

            //Loading attachment message
            newMsgID = message.getId();
            String userId = message.getRecipientId();
            String myId = message.getSenderId();
            RealmQuery<Chat> realmQuery = rChatDb.where(Chat.class).equalTo("myId", myId)
                    .equalTo("userId", userId);
            Chat chat = realmQuery.findFirst();
            rChatDb.beginTransaction();
            if (chat == null) {
                chat = rChatDb.createObject(Chat.class);
                chat.setMessages(new RealmList<Message>());
                chat.setLastMessage(message.getBody());
                chat.setMyId(myId);
                chat.setTimeUpdated(message.getDate());

                for (User user : MainActivity.myUsers) {
                    if (user != null && user.getId() != null && user.getId().equalsIgnoreCase(userOrGroupId)) {
                        chat.setUser(rChatDb.copyToRealm(user));
                        chat.setUserId(userId);
                    }
                }
            }
            chat.setTimeUpdated(message.getDate());
            chat.getMessages().add(message);
            chat.setLastMessage(message.getBody());
            rChatDb.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrev() {

        if (counter - 1 < 0) return;
        storyStatusView.pause();
        --counter;
        target.setModel(statusResources[counter]);
        Glide.with(image.getContext())
                .load(target.getModel())
                .asBitmap()
                .centerCrop()
                .crossFade()
                .skipMemoryCache(!isCaching)
                .diskCacheStrategy(isCaching ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .transform(new CenterCrop(image.getContext()), new DelayBitmapTransformation(1000))
                .listener(new LoggingListener<String, Bitmap>())
                .into(target);

        //.transform(new CenterCrop(image.getContext()), new DelayBitmapTransformation(1000))
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (isImmersive) {
            if (hasFocus) {
                getWindow().getDecorView()
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }

    @Override
    public void onComplete() {
        finish();
    }

    @Override
    protected void onDestroy() {
        // Very important !
        storyStatusView.destroy();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.attachment_video:
                openVideoPicker();
                break;
            case R.id.attachment_contact:
                openContactPicker();
                break;
            case R.id.attachment_gallery:
                openImagePick();
                break;
            case R.id.camera:
                openImageClick();
                break;
            case R.id.attachment_audio:
                openAudioPicker();
                break;
            case R.id.attachment_location:
                openPlacePicker();
                break;
            case R.id.attachment_document:
                openDocumentPicker();
                break;
            case R.id.add_attachment:
                Helper.closeKeyboard(this, v);
                if (statusUser != null) {
                    userMe = helper.getLoggedInUser();
                    if (userMe.getBlockedUsersIds() != null
                            && !userMe.getBlockedUsersIds().contains(statusUser.getId())) {
                        if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
                            myAttachmentLLY.setVisibility(View.GONE);
                            addAttachment.animate().setDuration(400).rotationBy(-45).start();
                        } else {
                            myAttachmentLLY.setVisibility(View.VISIBLE);
                            addAttachment.animate().setDuration(400).rotationBy(45).start();
                            emojIcon.dismiss();
                        }
                    } else {
                        FragmentManager manager = getSupportFragmentManager();
                        Fragment frag = manager.findFragmentByTag("DELETE_TAG");
                        if (frag != null) {
                            manager.beginTransaction().remove(frag).commit();
                        }

                        Helper.unBlockAlert(getIntent().getExtras().getString(USER_NAME), userMe,
                                StatusStoriesActivity.this,
                                helper, statusUser.getId(), manager);
                    }
                } else {
                    if (myAttachmentLLY.getVisibility() == View.VISIBLE) {
                        myAttachmentLLY.setVisibility(View.GONE);
                        addAttachment.animate().setDuration(400).rotationBy(-45).start();
                    } else {
                        myAttachmentLLY.setVisibility(View.VISIBLE);
                        addAttachment.animate().setDuration(400).rotationBy(45).start();
                        emojIcon.dismiss();
                    }
                }
                break;
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

    void openAudioPicker() {
        if (permissionsAvailable(permissionsStorage)) {
            audioPicker = new AudioPicker(this);
            audioPicker.setAudioPickerCallback(this);
            audioPicker.pickAudio();
        } else {
            ActivityCompat.requestPermissions(this, permissionsStorage, 25);
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
                        sendContainer.setVisibility(View.GONE);

                        //    hideKeyboard();
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
                        } catch (IOException ignored) { }
                        finally {
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
            } catch (IOException ignored) {
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException ignored) {
            }
        }
        return ous.toByteArray();
    }

    @Override
    public void onAudiosChosen(List<ChosenAudio> list) {
        if (list != null && !list.isEmpty())
            newFileUploadTask(Uri.parse(list.get(0).getOriginalPath()).getPath(), AttachmentTypes.AUDIO, null);
    }

    @Override
    public void onFilesChosen(List<ChosenFile> list) {
        if (list != null && !list.isEmpty())
            newFileUploadTask(Uri.parse(list.get(0).getOriginalPath()).getPath(), AttachmentTypes.DOCUMENT, null);
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
    public void onVideosChosen(List<ChosenVideo> list) {
        if (list != null && !list.isEmpty())
            uploadThumbnail(Uri.parse(list.get(0).getOriginalPath()).getPath());
    }

    @Override
    public void onError(String s) {

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

    /**
     * Demonstrates 3 different ways of showing the progress:
     * <ul>
     * <li>Update a full fledged progress bar</li>
     * <li>Update a text view to display size/percentage</li>
     * <li>Update the placeholder via Drawable.level</li>
     * </ul>
     * This last one is tricky: the placeholder that Glide sets can be used as a progress drawable
     * without any extra Views in the view hierarchy if it supports levels via <code>usesLevel="true"</code>
     * or <code>level-list</code>.
     *
     * @param <Z> automatically match any real Glide target so it can be used flexibly without reimplementing.
     */
    @SuppressLint("SetTextI18n") // text set only for debugging
    private static class MyProgressTarget<Z> extends ProgressTarget<String, Z> {
        private final TextView text;
        private final ProgressBar progress;

        public MyProgressTarget(Target<Z> target, ProgressBar progress, TextView text) {
            super(target);
            this.progress = progress;
            this.text = text;
        }

        @Override
        public float getGranualityPercentage() {
            return 0.1f; // this matches the format string for #text below
        }

        @Override
        protected void onConnecting() {
            progress.setIndeterminate(true);
            progress.setVisibility(View.VISIBLE);

            if (isTextEnabled) {
                text.setVisibility(View.VISIBLE);
                text.setText("connecting");
            } else {
                text.setVisibility(View.INVISIBLE);
            }
            storyStatusView.pause();
        }

        @Override
        protected void onDownloading(long bytesRead, long expectedLength) {
            progress.setIndeterminate(false);
            progress.setProgress((int) (100 * bytesRead / expectedLength));

            if (isTextEnabled) {
                text.setVisibility(View.VISIBLE);
                text.setText(String.format(Locale.ROOT, "downloading %.2f/%.2f MB %.1f%%",
                        bytesRead / 1e6, expectedLength / 1e6, 100f * bytesRead / expectedLength));
            } else {
                text.setVisibility(View.INVISIBLE);
            }


            storyStatusView.pause();

        }

        @Override
        protected void onDownloaded() {
            progress.setIndeterminate(true);
            if (isTextEnabled) {
                text.setVisibility(View.VISIBLE);
                text.setText("decoding and transforming");
            } else {
                text.setVisibility(View.INVISIBLE);
            }


            storyStatusView.pause();
        }

        @Override
        protected void onDelivered() {
            progress.setVisibility(View.INVISIBLE);
            text.setVisibility(View.INVISIBLE);
            storyStatusView.resume();
        }
    }

    private String getChatChild() {
        chatChild = userMe.getId() + "-" + userOrGroupId;
        BaseApplication.getChatRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(chatChild)) {
                    chatChild = userOrGroupId + "-" + userMe.getId();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return chatChild;
    }

    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
