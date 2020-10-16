package com.laodev.chatapp.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;

public class FirebaseUploader {
    private UploadListener uploadListener;
    private UploadTask uploadTask;
    private AsyncTask<File, Void, String> compressionTask;
    private boolean replace;
    private StorageReference uploadRef;
    private Uri fileUri;

    public FirebaseUploader(UploadListener uploadListener, StorageReference storageRef) {
        this.uploadListener = uploadListener;
        this.uploadRef = storageRef;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    public void uploadImage(Context context, File file) {
        compressAndUpload(context, "images", file);
    }

    public void uploadAudio(Context context, File file) {
        compressAndUpload(context, "audios", file);
    }

    public void uploadVideo(Context context, File file) {
        compressAndUpload(context, "videos", file);
    }

    public void uploadOthers(Context context, File file) {
        compressAndUpload(context, "others", file);
    }

    @SuppressLint("StaticFieldLeak")
    private void compressAndUpload(final Context context, final String child, final File file) {
        compressionTask = new AsyncTask<File, Void, String>() {
            @Override
            protected String doInBackground(File... files) {
                String filePathCompressed = null;
                Uri originalFileUri = Uri.fromFile(files[0]);
                File tempFile = new File(context.getCacheDir(), originalFileUri.getLastPathSegment());

                if (child.equals("images")) {
                    filePathCompressed = SiliCompressor.with(context).compress(originalFileUri.toString(), tempFile);
                }
                if (filePathCompressed == null)
                    filePathCompressed = "";
                return filePathCompressed;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                File compressed = new File(s);
                fileUri = Uri.fromFile(compressed.length() > 0 ? compressed : file);
                FirebaseStorage storage = FirebaseStorage.getInstance();
                if (uploadRef == null)
                    uploadRef = storage.getReference().child(child).child(fileUri.getLastPathSegment());

                if (replace) {
                    upload();
                } else {
                    checkIfExists();
                }
            }
        };

        compressionTask.execute(file);
    }

    private void checkIfExists() {
        uploadRef.getDownloadUrl().addOnSuccessListener(uri -> uploadListener
                .onUploadSuccess(uri.toString())).addOnFailureListener(e -> upload());
    }

    private void upload() {
        uploadTask = uploadRef.putFile(fileUri);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return uploadRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                uploadListener.onUploadSuccess(downloadUri.toString());
            } else {
                uploadListener.onUploadFail(task.getException().getMessage());
            }
        }).addOnFailureListener(e -> uploadListener.onUploadFail(e.getMessage()));
        uploadTask.addOnProgressListener(taskSnapshot -> {
            long progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            uploadListener.onUploadProgress((int) progress);
        });
    }

    public void cancelUpload() {
        if (compressionTask != null && compressionTask.getStatus() != AsyncTask.Status.FINISHED) {
            compressionTask.cancel(true);
        }
        if (uploadTask != null && uploadTask.isInProgress()) {
            uploadTask.cancel();
            uploadListener.onUploadCancelled();
        }
    }

    public interface UploadListener {
        void onUploadFail(String message);
        void onUploadSuccess(String downloadUrl);
        void onUploadProgress(int progress);
        void onUploadCancelled();
    }

}
