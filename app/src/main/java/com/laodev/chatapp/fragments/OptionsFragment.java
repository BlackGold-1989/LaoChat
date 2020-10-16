package com.laodev.chatapp.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.BuildConfig;
import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.SignInActivity;
import com.laodev.chatapp.models.Attachment;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.services.SinchService;
import com.laodev.chatapp.utils.ConfirmationDialogFragment;
import com.laodev.chatapp.utils.FirebaseUploader;
import com.laodev.chatapp.utils.Helper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kbeanie.multipicker.api.CameraImagePicker;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class OptionsFragment extends BaseFullDialogFragment implements ImagePickerCallback {

    private String[] permissionsCamera = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private User userMe;
    private ImagePicker imagePicker;
    private CameraImagePicker cameraPicker;
    private String pickerPath;
    private static String CONFIRM_TAG = "confirmtag";
    private static String PRIVACY_TAG = "privacytag";
    private static final int REQUEST_CODE_MEDIA_PERMISSION = 999;
    private static final int REQUEST_CODE_PICKER = 4321;
    private ImageView userImage;
    private Helper helper;

    private SinchService.SinchServiceInterface sinchServiceInterface;
    private ProgressBar myProgressBar;
    private Context context;
    private boolean fromFlag = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new Helper(getContext());
        userMe = helper.getLoggedInUser();

        BaseApplication.getUserRef().child(userMe.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userMe = dataSnapshot.getValue(User.class);
                helper.setLoggedInUser(userMe);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("TAG", databaseError.getMessage());
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_options, container);
        userImage = view.findViewById(R.id.userImage);
        myProgressBar = view.findViewById(R.id.progressBar);
        final EditText userName = view.findViewById(R.id.userName);
        final EditText userStatus = view.findViewById(R.id.userStatus);
        TextView tvVersion = view.findViewById(R.id.tv_version);

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            tvVersion.setText(getString(R.string.version) + " : " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        userName.setText(userMe.getNameToDisplay());
        userStatus.setText(userMe.getStatus());
        if (userMe.getImage() != null && !userMe.getImage().isEmpty())
            Picasso.get()
                    .load(userMe.getImage())
                    .tag(this)
                    .error(R.drawable.ic_avatar)
                    .placeholder(R.drawable.ic_avatar)
                    .into(userImage);
        else
            userImage.setBackgroundResource(R.drawable.ic_avatar);

        view.findViewById(R.id.done).setOnClickListener(view1 -> {
            Helper.closeKeyboard(getContext(), view1);
            updateUserNameAndStatus(userName.getText().toString().trim(), userStatus.getText().toString().trim());
        });
        userImage.setOnClickListener(view12 -> {
            fromFlag = false;
            pickProfileImage();
        });
        view.findViewById(R.id.back).setOnClickListener(view13 -> {
            Helper.closeKeyboard(getContext(), view13);
            dismiss();
        });
        view.findViewById(R.id.share).setOnClickListener(view14 -> Helper.openShareIntent(getContext(), null,
                "Download Chat app now https://play.google.com/store/apps/details?id=" +
                        BuildConfig.APPLICATION_ID));
        view.findViewById(R.id.rate).setOnClickListener(view15 -> Helper.openPlayStore(getContext()));
        view.findViewById(R.id.contact).setOnClickListener(view16 -> Helper.openSupportMail(getContext()));
        view.findViewById(R.id.invite_friends).setOnClickListener(view17 -> {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invitation_title));
                shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.invitation_text),
                        getContext().getPackageName()));
                startActivity(Intent.createChooser(shareIntent, "Share using.."));
            } catch (Exception ignored) {
            }

        });

        view.findViewById(R.id.privacy).setOnClickListener(view18 -> new PrivacyPolicyDialogFragment().show(getChildFragmentManager(), PRIVACY_TAG));
        view.findViewById(R.id.logout).setOnClickListener(view19 -> {
            ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment.newInstance(getString(R.string.logout),
                    getString(R.string.alt_logout_detail),
                    view1912 -> {
                        FirebaseAuth.getInstance().signOut();
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Helper.BROADCAST_LOGOUT));
                        sinchServiceInterface.stopClient();
                        helper.logout();
                        getActivity().finish();
                        Intent mIntent = new Intent(getContext(), SignInActivity.class);
                        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(mIntent);
                    },
                    view191 -> {
                    });
            confirmationDialogFragment.show(getChildFragmentManager(), CONFIRM_TAG);
        });

        view.findViewById(R.id.wallpaper).setOnClickListener(v -> {
            fromFlag = true;
            pickWallpaperImage();
        });
        return view;
    }

    private void updateUserNameAndStatus(String updatedName, String updatedStatus) {
        if (TextUtils.isEmpty(updatedName)) {
            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(updatedStatus)) {
            Toast.makeText(getContext(), "Status cannot be empty", Toast.LENGTH_SHORT).show();
        } else if (!userMe.getName().equals(updatedName) || !userMe.getStatus().equals(updatedStatus)) {
            userMe.setName(updatedName);
            userMe.setStatus(updatedStatus);
            BaseApplication.getUserRef().child(userMe.getId()).setValue(userMe).addOnSuccessListener(aVoid -> {
                if (helper != null)
                    helper.setLoggedInUser(userMe);
                toast("Updated!");
            });
        } else {
            Toast.makeText(getContext(), "Details already updated!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_MEDIA_PERMISSION) {
            if (mediaPermissions().isEmpty()) {
                pickProfileImage();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Picker.PICK_IMAGE_DEVICE:
                    if (imagePicker == null) {
                        imagePicker = new ImagePicker(this);
                    }
                    imagePicker.submit(data);
                    break;
                case Picker.PICK_IMAGE_CAMERA:
                    if (cameraPicker == null) {
                        cameraPicker = new CameraImagePicker(this);
                        cameraPicker.reinitialize(pickerPath);
                    }
                    cameraPicker.submit(data);
                    break;
            }
        }
    }

    private void userImageUploadTask(final File fileToUpload, @AttachmentTypes.AttachmentType final int attachmentType, final Attachment attachment) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(getString(R.string.app_name)).child("ProfileImage").child(userMe.getId());
        FirebaseUploader firebaseUploader = new FirebaseUploader(new FirebaseUploader.UploadListener() {
            @Override
            public void onUploadFail(String message) {
                myProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onUploadSuccess(String downloadUrl) {
                userMe = helper.getLoggedInUser();
                myProgressBar.setVisibility(View.GONE);
                userMe.setImage(downloadUrl);
                helper.setLoggedInUser(userMe);
                BaseApplication.getUserRef().child(userMe.getId()).setValue(userMe).addOnSuccessListener(aVoid -> {
                    if (helper != null)
                        helper.setLoggedInUser(userMe);
                    toast("Profile image updated");
                });
            }

            @Override
            public void onUploadProgress(int progress) {
                Log.e("IMAGE_PROGRESS", "" + progress);
            }

            @Override
            public void onUploadCancelled() {

            }
        }, storageReference);
        firebaseUploader.setReplace(true);
        firebaseUploader.uploadImage(getContext(), fileToUpload);
    }

    private void wallpaperUploadTask(final File fileToUpload, @AttachmentTypes.AttachmentType final int attachmentType, final Attachment attachment) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(getString(R.string.app_name)).child("Wallpaper").child(userMe.getId());
        FirebaseUploader firebaseUploader = new FirebaseUploader(new FirebaseUploader.UploadListener() {
            @Override
            public void onUploadFail(String message) {
                myProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onUploadSuccess(String downloadUrl) {
                userMe = helper.getLoggedInUser();
                myProgressBar.setVisibility(View.GONE);
                userMe.setWallpaper(downloadUrl);
                helper.setLoggedInUser(userMe);
                BaseApplication.getUserRef().child(userMe.getId()).setValue(userMe).addOnSuccessListener(aVoid -> {
                    if (helper != null)
                        helper.setLoggedInUser(userMe);
                    toast("Wallpaper set successfully");
                });
            }

            @Override
            public void onUploadProgress(int progress) {
                Log.e("IMAGE_PROGRESS", "" + progress);
            }

            @Override
            public void onUploadCancelled() {

            }
        }, storageReference);
        firebaseUploader.setReplace(true);
        firebaseUploader.uploadImage(getContext(), fileToUpload);
    }

    private void toast(String message) {
        if (getContext() != null)
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void pickWallpaperImage() {
        if (mediaPermissions().isEmpty()) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setMessage("Get image from");
            alertDialog.setPositiveButton("Camera", (dialogInterface, i) -> {
                dialogInterface.dismiss();

                cameraPicker = new CameraImagePicker(OptionsFragment.this);
                cameraPicker.shouldGenerateMetadata(true);
                cameraPicker.shouldGenerateThumbnails(true);
                cameraPicker.setImagePickerCallback(OptionsFragment.this);
                pickerPath = cameraPicker.pickImage();
            });
            alertDialog.setNegativeButton("Gallery", (dialogInterface, i) -> {
                dialogInterface.dismiss();

                imagePicker = new ImagePicker(OptionsFragment.this);
                imagePicker.shouldGenerateMetadata(true);
                imagePicker.shouldGenerateThumbnails(true);
                imagePicker.setImagePickerCallback(OptionsFragment.this);
                imagePicker.pickImage();
            });
            alertDialog.setNeutralButton("No Wallpaper", (dialogInterface, i) -> {
                userMe = helper.getLoggedInUser();
                userMe.setWallpaper("");
                helper.setLoggedInUser(userMe);
                BaseApplication.getUserRef().child(userMe.getId()).setValue(userMe)
                        .addOnSuccessListener(aVoid -> {
                            if (helper != null)
                                helper.setLoggedInUser(userMe);
                            toast("Wallpaper removed");
                        });
            });
            alertDialog.create().show();
        } else {
            requestPermissions(permissionsCamera, REQUEST_CODE_MEDIA_PERMISSION);
        }
    }

    private void pickProfileImage() {
        if (mediaPermissions().isEmpty()) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setMessage("Get image from");
            alertDialog.setPositiveButton("Camera", (dialogInterface, i) -> {
                dialogInterface.dismiss();

                cameraPicker = new CameraImagePicker(OptionsFragment.this);
                cameraPicker.shouldGenerateMetadata(true);
                cameraPicker.shouldGenerateThumbnails(true);
                cameraPicker.setImagePickerCallback(OptionsFragment.this);
                pickerPath = cameraPicker.pickImage();
            });
            alertDialog.setNegativeButton("Gallery", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                    imagePicker = new ImagePicker(OptionsFragment.this);
                    imagePicker.shouldGenerateMetadata(true);
                    imagePicker.shouldGenerateThumbnails(true);
                    imagePicker.setImagePickerCallback(OptionsFragment.this);
                    imagePicker.pickImage();
                }
            });
            alertDialog.create().show();
        } else {
            requestPermissions(permissionsCamera, REQUEST_CODE_MEDIA_PERMISSION);
        }
    }

    @Override
    public void onImagesChosen(List<ChosenImage> images) {
        File fileToUpload = new File(Uri.parse(images.get(0).getOriginalPath()).getPath());
//        Glide.with(this).load(fileToUpload).apply(new RequestOptions().placeholder(R.drawable.ic_logo_large)).into(userImage);
        if (fromFlag) {
            wallpaperUploadTask(fileToUpload, AttachmentTypes.IMAGE, null);
        } else {
            Picasso.get()
                    .load(fileToUpload)
                    .tag(this)
                    .placeholder(R.drawable.ic_avatar)
                    .into(userImage);
            myProgressBar.setVisibility(View.VISIBLE);
            userImageUploadTask(fileToUpload, AttachmentTypes.IMAGE, null);
        }
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // You have to save path in case your activity is killed.
        // In such a scenario, you will need to re-initialize the CameraImagePicker
        outState.putString("picker_path", pickerPath);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("picker_path")) {
                pickerPath = savedInstanceState.getString("picker_path");
            }
        }
    }

    private List<String> mediaPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : permissionsCamera) {
            if (ActivityCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }

    public static OptionsFragment newInstance(SinchService.SinchServiceInterface sinchServiceInterface) {
        OptionsFragment fragment = new OptionsFragment();
        fragment.sinchServiceInterface = sinchServiceInterface;
        return fragment;
    }
}
