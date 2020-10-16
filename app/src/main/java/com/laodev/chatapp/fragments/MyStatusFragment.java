package com.laodev.chatapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.iceteck.silicompressorr.SiliCompressor;
import com.kbeanie.multipicker.api.CameraImagePicker;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.MainActivity;
import com.laodev.chatapp.activities.StatusStoriesActivity;
import com.laodev.chatapp.adapters.StatusAdapter;
import com.laodev.chatapp.interfaces.HomeIneractor;
import com.laodev.chatapp.models.Attachment;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Contact;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.Status;
import com.laodev.chatapp.models.StatusImage;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.FileUtils;
import com.laodev.chatapp.utils.FirebaseUploader;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.viewHolders.BaseMessageViewHolder;
import com.laodev.chatapp.views.MyRecyclerView;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class MyStatusFragment extends Fragment implements ImagePickerCallback {

    private boolean isCacheEnabled = true;
    private boolean isImmersiveEnabled = true;
    private boolean isTextEnabled = false;
    private long storyDuration = 5000L;

    private MyRecyclerView recyclerView;
    private Button clear;
    private StatusAdapter statusAdapter;
    private SwipeRefreshLayout mySwipeRefreshLayout;

    private Realm rChatDb;
    private User userMe;

    private RealmResults<Status> resultList;
    private ArrayList<Status> chatDataList = new ArrayList<>();
    private ArrayList<Status> filterDataList = new ArrayList<>();


    private HomeIneractor homeInteractor;
    private Helper helper;

    private ImagePicker imagePicker;
    private CameraImagePicker cameraPicker;
    private String pickerPath;
    private String[] permissionsStorage = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private String[] permissionsCamera = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private String chatChild;
    private Context context;

    private ArrayList<StatusImage> statusImageArrayList = new ArrayList<>();
    private ProgressDialog dialog;
    private CircleImageView img;
    private ArrayList<Contact> myContacts;
    private RealmChangeListener<RealmResults<Status>> chatListChangeListener = new RealmChangeListener<RealmResults<Status>>() {
        @Override
        public void onChange(@NonNull RealmResults<Status> element) {
            if (element.isValid() && element.size() > 0) {
                chatDataList.clear();
                chatDataList.addAll(rChatDb.copyFromRealm(element));
                fillAdapter();
                setUserNamesAsInPhone();
            }
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        fetchMyContacts();
        try {
            homeInteractor = (HomeIneractor) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement HomeIneractor");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new Helper(getContext());
        userMe = homeInteractor.getUserMe();
        Realm.init(getContext());
        rChatDb = Helper.getRealmInstance();
        dialog = new ProgressDialog(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        view.findViewById(R.id.myStatus_RLY).setOnClickListener(view1 -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setMessage("Get image from");
            alertDialog.setPositiveButton("Camera", (dialogInterface, i) -> {
                dialogInterface.dismiss();
                openImageClick();
            });
            alertDialog.setNegativeButton("Gallery", (dialogInterface, i) -> {
                dialogInterface.dismiss();
                openImagePick();
            });
            alertDialog.create().show();
        });


        recyclerView = view.findViewById(R.id.recycler_view);
        img = view.findViewById(R.id.fab_chat);
        clear = view.findViewById(R.id.clear);
        mySwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_lay);
        mySwipeRefreshLayout.setRefreshing(false);
        recyclerView.setEmptyView(view.findViewById(R.id.emptyView));
        recyclerView.setEmptyImageView(view.findViewById(R.id.emptyImage));
        recyclerView.setEmptyTextView(view.findViewById(R.id.emptyText));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mySwipeRefreshLayout.setOnRefreshListener(() -> {
            try {
                RealmQuery<Status> query = rChatDb.where(Status.class)/*.equalTo("myId", userMe.getId())*/;//Query from chats whose owner is logged in user
                resultList = query.isNotNull("myId").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time

                chatDataList.clear();
                chatDataList.addAll(rChatDb.copyFromRealm(resultList));
                fillAdapter();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mySwipeRefreshLayout.setRefreshing(false);
            setUserNamesAsInPhone();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            RealmQuery<Status> query = rChatDb.where(Status.class)/*.equalTo("myId", userMe.getId())*/;//Query from chats whose owner is logged in user
            resultList = query.isNotNull("myId").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time
            chatDataList.clear();
            chatDataList.addAll(rChatDb.copyFromRealm(resultList));
            fillAdapter();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setUserNamesAsInPhone();

        clear.setOnClickListener(view1 -> rChatDb.executeTransaction(realm -> realm.delete(Status.class)));

        if (userMe != null && userMe.getImage() != null && !userMe.getImage().isEmpty()) {
            Picasso.get()
                    .load(userMe.getImage())
                    .tag(this)
                    .error(R.drawable.ic_avatar)
                    .placeholder(R.drawable.ic_avatar)
                    .into(img);
        }
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
    }

    private void fillAdapter() {
        filterDataList.clear();
        for (Status status : chatDataList) {
            for (Contact contact : myContacts) {
                if (status.getMyId().equals(contact.getPhoneNumber())) {
                    filterDataList.add(status);
                }
            }
        }

        for (int i = 0; i < filterDataList.size(); i++) {
            long time = diff(filterDataList.get(i).getTimeUpdated());
            if (time > 24)
                filterDataList.remove(i);
        }
        statusAdapter = new StatusAdapter(this.context, filterDataList, MyStatusFragment.this);
        recyclerView.setAdapter(statusAdapter);

        resultList.addChangeListener(chatListChangeListener);
    }

    private long diff(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;
        return diff / (60 * 60 * 1000);
    }

    private void openImagePick() {
        if (permissionsAvailable(permissionsStorage)) {
            imagePicker = new ImagePicker(this);
            imagePicker.shouldGenerateMetadata(true);
            imagePicker.shouldGenerateThumbnails(true);
            imagePicker.setImagePickerCallback(this);
            imagePicker.pickImage();
        } else {
            ActivityCompat.requestPermissions(getActivity(), permissionsStorage, 36);
        }
    }

    private void openImageClick() {
        if (permissionsAvailable(permissionsCamera)) {
            cameraPicker = new CameraImagePicker(this);
            cameraPicker.shouldGenerateMetadata(true);
            cameraPicker.shouldGenerateThumbnails(true);
            cameraPicker.setImagePickerCallback(this);
            pickerPath = cameraPicker.pickImage();
        } else {
            ActivityCompat.requestPermissions(getActivity(), permissionsCamera, 47);
        }
    }

    public void navigateStatusStories(int position) {
        statusImageArrayList.clear();
        RealmList<StatusImage> statusImagesList = chatDataList.get(position).getStatusImages();
        statusImageArrayList.addAll(statusImagesList);
        String[] resources = new String[statusImageArrayList.size()];
        for (int i = 0; i < statusImageArrayList.size(); i++) {
            resources[i] = statusImageArrayList.get(i).getAttachment().getUrl();
        }

        Intent a = new Intent(getContext(), StatusStoriesActivity.class);
        a.putExtra(StatusStoriesActivity.STATUS_RESOURCES_KEY, resources);
        a.putExtra(StatusStoriesActivity.STATUS_DURATION_KEY, storyDuration);
        a.putExtra(StatusStoriesActivity.IS_IMMERSIVE_KEY, isImmersiveEnabled);
        a.putExtra(StatusStoriesActivity.IS_CACHING_ENABLED_KEY, isCacheEnabled);
        a.putExtra(StatusStoriesActivity.IS_TEXT_PROGRESS_ENABLED_KEY, isTextEnabled);

        startActivity(a);
    }

    private void setUserNamesAsInPhone() {
        if (homeInteractor != null && chatDataList != null) {
            for (Status chat : chatDataList) {
                User user = chat.getUser();
                if (user != null) {
                    if (helper.getCacheMyUsers() != null && helper.getCacheMyUsers().containsKey(user.getId())) {
                        user.setNameInPhone(helper.getCacheMyUsers().get(user.getId()).getNameToDisplay());
                    } else {
                        for (Contact savedContact : myContacts) {
                            if (Helper.contactMatches(user.getId(), savedContact.getPhoneNumber())) {
                                if (user.getNameInPhone() == null || !user.getNameInPhone().equals(savedContact.getName())) {
                                    user.setNameInPhone(savedContact.getName());
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (statusAdapter != null)
            statusAdapter.notifyDataSetChanged();
    }

    private void fetchMyContacts() {
        myContacts = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
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
    }

    private boolean permissionsAvailable(String[] permissions) {
        boolean granted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        return granted;
    }


    @Override
    public void onImagesChosen(List<ChosenImage> list) {
        dialog.setCancelable(false);
        dialog.setMessage("Loading...");
        dialog.show();

        if (list != null && !list.isEmpty()) {
            Uri originalFileUri = Uri.parse(list.get(0).getOriginalPath());
            File tempFile = new File(getActivity().getCacheDir(), originalFileUri.getLastPathSegment());
            try {
                uploadImage(SiliCompressor.with(getContext()).compress(originalFileUri.toString(), tempFile));
            } catch (Exception ex) {
                uploadImage(originalFileUri.getPath());
            }
        }
    }


    private void uploadImage(String filePath) {
        newFileUploadTask(filePath, AttachmentTypes.IMAGE, null);
    }


    private void newFileUploadTask(String filePath,
                                   @AttachmentTypes.AttachmentType final int attachmentType, final Attachment attachment) {
        final File fileToUpload = new File(filePath);
        final String fileName = Uri.fromFile(fileToUpload).getLastPathSegment();

        Attachment preSendAttachment = attachment;//Create/Update attachment
        if (preSendAttachment == null) preSendAttachment = new Attachment();
        preSendAttachment.setName(fileName);
        preSendAttachment.setBytesCount(fileToUpload.length());
        preSendAttachment.setUrl("loading");
        checkAndCopy("/" + getString(R.string.app_name) + "/" + AttachmentTypes.getTypeName(attachmentType) + "/.sent/", fileToUpload);//Make a copy
        chatChild = userMe.getId();
        uploadAndSend(new File(filePath), attachment, attachmentType, chatChild);

    }

    private void uploadAndSend(final File fileToUpload, final Attachment attachment, final int attachmentType, final String chatChild) {
        if (!fileToUpload.exists())
            return;
        final String fileName = Uri.fromFile(fileToUpload).getLastPathSegment();
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(getString(R.string.app_name)).child(AttachmentTypes.getTypeName(attachmentType)).child(fileName);
        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //If file is already uploaded
                Attachment attachment1 = attachment;
                if (attachment1 == null) attachment1 = new Attachment();
                attachment1.setName(fileName);
                attachment1.setUrl(uri.toString());
                attachment1.setBytesCount(fileToUpload.length());
                prepareMessage(null, attachmentType, attachment1);
                sendMessage(null, attachmentType, attachment1);
            }
        }).addOnFailureListener(exception -> {
            //Elase upload and then send message
            FirebaseUploader firebaseUploader = new FirebaseUploader(new FirebaseUploader.UploadListener() {
                @Override
                public void onUploadFail(String message) {
                    Log.e("DatabaseException", message);
                }

                @Override
                public void onUploadSuccess(String downloadUrl) {
                    Attachment attachment1 = attachment;
                    if (attachment1 == null) attachment1 = new Attachment();
                    attachment1.setName(fileToUpload.getName());
                    attachment1.setUrl(downloadUrl);
                    attachment1.setBytesCount(fileToUpload.length());
                    prepareMessage(null, attachmentType, attachment1);

                }

                @Override
                public void onUploadProgress(int progress) {

                }

                @Override
                public void onUploadCancelled() {

                }
            }, storageReference);
            firebaseUploader.uploadOthers(context, fileToUpload);
        });
    }


    private void sendMessage(String messageBody, @AttachmentTypes.AttachmentType int attachmentType, Attachment attachment) {
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
        message.setId(((MainActivity) context).getDatabaseRef().child(chatChild).push().getKey());

        //Add messages in chat child
        ((MainActivity) context).getDatabaseRef().child(chatChild).child(message.getId()).setValue(message);
        if (dialog.isShowing())
            dialog.dismiss();
    }

    @Override
    public void onError(String s) {
        Toast.makeText(getContext(), "" + s, Toast.LENGTH_SHORT).show();
    }


    private void prepareMessage(final String body, final int attachmentType, final Attachment attachment) {
        rChatDb.executeTransaction(realm -> {
            Status statusQuery = rChatDb.where(Status.class).equalTo("myId", userMe.getId()).findFirst();
            if (statusQuery == null) {
                StatusImage statusImage = rChatDb.createObject(StatusImage.class);
                statusImage.setAttachmentType(attachmentType);
                Attachment attachment1 = rChatDb.createObject(Attachment.class);
                attachment1.setBytesCount(attachment.getBytesCount());
                attachment1.setData(attachment.getData());
                attachment1.setName(attachment.getName());
                attachment1.setUrl(attachment.getUrl());
                statusImage.setAttachment(attachment1);
                statusImage.setBody(body);
                statusImage.setDate(System.currentTimeMillis());
                statusImage.setSenderId(userMe.getId());
                statusImage.setSenderName(userMe.getName());
                statusImage.setSent(false);
                statusImage.setDelivered(false);
                statusImage.setId(attachment.getUrl() + attachment.getBytesCount() + attachment.getName());

                Status status = rChatDb.createObject(Status.class);
                status.getStatusImages().add(statusImage);
                status.setLastMessage(body);
                status.setMyId(userMe.getId());
                status.setTimeUpdated(System.currentTimeMillis());
                status.setUser(rChatDb.copyToRealm(userMe));
                status.setUserId(userMe.getId());
                status.setTimeUpdated(System.currentTimeMillis());
                status.setLastMessage(body);
                sendMessage(null, attachmentType, attachment);
            } else {
                Attachment attachment1 = statusQuery.getStatusImages().get(0).getAttachment();
                attachment1.setUrl(attachment.getUrl());
                attachment1.setName(attachment.getName());
                statusQuery.setTimeUpdated(System.currentTimeMillis());
                statusQuery.getStatusImages().get(0).setAttachment(attachment1);
                statusQuery.setStatusImages(statusQuery.getStatusImages());

                DatabaseReference databaseReference = ((MainActivity) context).getDatabaseRef().child(chatChild);
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                            String key = dataSnapshot1.getKey();
                            Message value = dataSnapshot1.getValue(Message.class);
                            value.setAttachmentType(attachmentType);
                            if (attachmentType != AttachmentTypes.NONE_TEXT)
                                value.setAttachment(attachment);
                            else
                                BaseMessageViewHolder.animate = true;
                            value.setDate(System.currentTimeMillis());

                            ((MainActivity) context).getDatabaseRef().child(chatChild).child(key).setValue(value);
                            if (dialog.isShowing())
                                dialog.dismiss();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.i("TAG_ERROR", databaseError.getMessage());
                    }
                });
            }
        });
    }


    private void checkAndCopy(String directory, File source) {
        //Create and copy file content
        File file = new File(Environment.getExternalStorageDirectory(), directory);
        boolean dirExists = file.exists();
        if (!dirExists)
            dirExists = file.mkdirs();
        if (dirExists) {
            try {
                file = new File(Environment.getExternalStorageDirectory() + directory, Uri.fromFile(source).getLastPathSegment());
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
            }
        }
    }

}
