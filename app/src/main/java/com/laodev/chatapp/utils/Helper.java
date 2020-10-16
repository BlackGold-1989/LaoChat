package com.laodev.chatapp.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.models.Chat;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.models.Status;
import com.laodev.chatapp.models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;

/**
 * Created by a_man on 5/5/2017.
 */

public class Helper {
    private static final String USER = "USER";
    private static final String USER_MUTE = "USER_MUTE";
    private static final String SEND_OTP = "SEND_OTP";

    public static final String BROADCAST_USER = "com.laodev.chatapp.services.USER";
    public static final String BROADCAST_MY_CONTACTS = "com.laodev.chatapp.MY_CONTACTS";
    public static final String BROADCAST_MY_USERS = "com.laodev.chatapp.MY_USERS";
    public static final String BROADCAST_DOWNLOAD_EVENT = "com.laodev.chatapp.DOWNLOAD_EVENT";
    public static final String BROADCAST_GROUP = "com.laodev.chatapp.services.GROUP";
    public static final String BROADCAST_LOGOUT = "com.laodev.chatapp.services.LOGOUT";
    public static final String UPLOAD_AND_SEND = "com.laodev.chatapp.services.UPLOAD_N_SEND";
    public static final String REF_DATA = "data";
    public static final String REF_USER = "users";
    public static final String GROUP_CREATE = "group_create";
    public static final String GROUP_PREFIX = "group";
    public static final String GROUP_NOTIFIED = "group_notified";
    public static final String USER_NAME_CACHE = "usercachemap";
    public static final String REF_APP = "apps";
    public static final String REF_CHAT = "chats";
    public static final String REF_GROUP = "groups";
    public static final String REF_STATUS_NEW = "userstatus";
    public static final String BROADCAST_STATUS = "com.laodev.chatapp.services.STATUS";

    public static String CURRENT_CHAT_ID;
    public static boolean CHAT_CAB = false;

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;

    private SharedPreferenceHelper sharedPreferenceHelper;
    private Gson gson;
    private HashSet<String> muteUsersSet;
    private HashMap<String, User> myUsersNameInPhoneMap;

    public Helper(Context context) {
        sharedPreferenceHelper = new SharedPreferenceHelper(context);
        gson = new Gson();
    }

    public static String getDateTime(Long milliseconds) {
        return new SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()).format(new Date(milliseconds));
    }

    public static String getTime(Long milliseconds) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(milliseconds));
    }

    public static boolean isImage(Context context, String url) {
        return getMimeType(context, url).startsWith("image");
    }

    public static String getMimeType(Context context, Uri uri) {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    public static String getMimeType(Context context, String url) {
        String mimeType;
        Uri uri = Uri.parse(url);
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }

    public static String getChatChild(String userId, String myId) {
        //example: userId="9" and myId="5" -->> chat child = "5-9"
        String[] temp = {userId, myId};
        Arrays.sort(temp);
        return temp[0] + "-" + temp[1];
    }

    public User getLoggedInUser() {
        String savedUserPref = sharedPreferenceHelper.getStringPreference(USER);
        if (savedUserPref != null)
            return gson.fromJson(savedUserPref, new TypeToken<User>() {
            }.getType());
        return null;
    }

    public void setLoggedInUser(User user) {
        sharedPreferenceHelper.setStringPreference(USER, gson.toJson(user, new TypeToken<User>() {
        }.getType()));
    }

    public void logout() {
        sharedPreferenceHelper.clearPreference(SEND_OTP);
        sharedPreferenceHelper.clearPreference(USER);
    }

    public void setPhoneNumberForVerification(String phone) {
        sharedPreferenceHelper.setStringPreference(SEND_OTP, phone);
    }

    public String getPhoneNumberForVerification() {
        return sharedPreferenceHelper.getStringPreference(SEND_OTP);
    }

    public void clearPhoneNumberForVerification() {
        sharedPreferenceHelper.clearPreference(SEND_OTP);
    }

    public boolean isLoggedIn() {
        return sharedPreferenceHelper.getStringPreference(USER) != null;
    }

    public void setUserMute(String userId, boolean mute) {
        if (muteUsersSet == null) {
            String muteUsersPref = sharedPreferenceHelper.getStringPreference(USER_MUTE);
            if (muteUsersPref != null) {
                muteUsersSet = gson.fromJson(muteUsersPref, new TypeToken<HashSet<String>>() {
                }.getType());
            } else {
                muteUsersSet = new HashSet<>();
            }
        }

        if (mute)
            muteUsersSet.add(userId);
        else
            muteUsersSet.remove(userId);

        sharedPreferenceHelper.setStringPreference(USER_MUTE, gson.toJson(muteUsersSet, new TypeToken<HashSet<String>>() {
        }.getType()));
    }

    public boolean isUserMute(String userId) {
        String muteUsersPref = sharedPreferenceHelper.getStringPreference(USER_MUTE);
        if (muteUsersPref != null) {
            HashSet<String> muteUsersSet = gson.fromJson(muteUsersPref, new TypeToken<HashSet<String>>() {
            }.getType());
            return muteUsersSet.contains(userId);
        } else {
            return false;
        }
    }

    public static void loadUrl(Context context, String url) {
        Uri uri = Uri.parse(url);
// create an intent builder
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
// Begin customizing
// set toolbar colors
        intentBuilder.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));
        intentBuilder.setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));

        intentBuilder.addDefaultShareMenuItem();
        intentBuilder.enableUrlBarHiding();
// build custom tabs intent
        CustomTabsIntent customTabsIntent = intentBuilder.build();
// launch the url
        customTabsIntent.launchUrl(context, uri);
    }

    public static boolean contactMatches(String userPhone, String phoneNumber) {
        if (userPhone.length() < 8 || phoneNumber.length() < 8)
            return false;
//        String reverseUserNumber = new StringBuffer(userPhone).reverse().toString().substring(0, 7);
//        String reversePhoneNumber = new StringBuffer(phoneNumber).reverse().toString().substring(0, 7);
//        return reversePhoneNumber.equals(reverseUserNumber);
        return userPhone.substring(userPhone.length() - 7, userPhone.length()).equals(phoneNumber.substring(phoneNumber.length() - 7, phoneNumber.length()));
    }

    public static Realm getRealmInstance() {
        RealmConfiguration config = new RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build();
        Realm realm = Realm.getInstance(config);
        return realm;
    }

    public static RealmQuery<Chat> getChat(Realm rChatDb, String myId, String userId) {
        return rChatDb.where(Chat.class).equalTo("myId", myId).equalTo(userId.startsWith(GROUP_PREFIX) ? "groupId" : "userId", userId);
    }

    public static RealmQuery<Chat> getGroupChat(Realm rChatDb, String myId, String userId) {
        return rChatDb.where(Chat.class).equalTo(userId.startsWith(GROUP_PREFIX) ? "groupId" : "userId", userId);
    }

    public static RealmQuery<Status> getStatus(Realm rChatDb, String myId) {
        return rChatDb.where(Status.class).equalTo("myId", myId);
    }

    public static RealmQuery<Status> getStatus(Realm rChatDb) {
        return rChatDb.where(Status.class);
    }


    public SharedPreferenceHelper getSharedPreferenceHelper() {
        return sharedPreferenceHelper;
    }

    public HashMap<String, User> getCacheMyUsers() {
        if (this.myUsersNameInPhoneMap != null) {
            return this.myUsersNameInPhoneMap;
        } else {
            String inPrefs = sharedPreferenceHelper.getStringPreference(USER_NAME_CACHE);
            if (inPrefs != null) {
                this.myUsersNameInPhoneMap = new Gson().fromJson(inPrefs, new TypeToken<HashMap<String, User>>() {
                }.getType());
                return this.myUsersNameInPhoneMap;
            } else {
                return null;
            }
        }
    }

    public void setCacheMyUsers(ArrayList<User> myUsers) {
        if (this.myUsersNameInPhoneMap == null) {
            this.myUsersNameInPhoneMap = new HashMap<>();
        }
        this.myUsersNameInPhoneMap.clear();
        User me = getLoggedInUser();
        me.setNameInPhone("You");
        this.myUsersNameInPhoneMap.put(me.getId(), me);
        for (User user : myUsers) {
            this.myUsersNameInPhoneMap.put(user.getId(), user);
        }
        sharedPreferenceHelper.setStringPreference(USER_NAME_CACHE, new Gson().toJson(this.myUsersNameInPhoneMap, new TypeToken<HashMap<String, User>>() {
        }.getType()));
    }

    public static void openShareIntent(Context context, @Nullable View itemview, String shareText) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        if (itemview != null) {
            try {
                Uri imageUri = getImageUri(context, itemview, "postBitmap.jpeg");
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (IOException e) {
                intent.setType("text/plain");
                e.printStackTrace();
            }
        } else {
            intent.setType("text/plain");
        }
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        context.startActivity(Intent.createChooser(intent, "Share Via:"));
    }

    private static Uri getImageUri(Context context, View view, String fileName) throws IOException {
        Bitmap bitmap = loadBitmapFromView(view);
        File pictureFile = new File(context.getExternalCacheDir(), fileName);
        FileOutputStream fos = new FileOutputStream(pictureFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        fos.close();
        return Uri.parse("file://" + pictureFile.getAbsolutePath());
    }

    private static Bitmap loadBitmapFromView(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            v.setDrawingCacheEnabled(true);
            return v.getDrawingCache();
        }

        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return bitmap;
    }

    public static void openPlayStore(Context context) {
        final String appPackageName = context.getPackageName(); // getPackageName() from Context or Activity object
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public static void openSupportMail(Context context) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "apps@dreamguys.co.in", null));
//        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
//        emailIntent.putExtra(Intent.EXTRA_TEXT, "Body");
        context.startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    public static int getDisplayWidth(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public static void closeKeyboard(Context context, View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void deleteMessageFromRealm(Realm rChatDb, String msgId) {
        final Message result = rChatDb.where(Message.class).equalTo("id", msgId).findFirst();
        if (result != null) {
            //rChatDb.beginTransaction();
            rChatDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmObject.deleteFromRealm(result);
                }
            });

            //rChatDb.commitTransaction();
        }
    }

    public static void updateMessageFromRealm(final Realm rChatDb, final Message msg) {
//        final Message result = rChatDb.where(Message.class).equalTo("id", msgId).findFirst();
        if (msg != null) {
            //rChatDb.beginTransaction();
            rChatDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    rChatDb.insertOrUpdate(msg);
//                    RealmObject.deleteFromRealm(result);
                }
            });

            //rChatDb.commitTransaction();
        }
    }

    public static void deleteGroupFromRealm(Realm rChatDb, String msgId) {
        final Chat result = rChatDb.where(Chat.class).equalTo("groupId", msgId).findFirst();
        if (result != null) {
            //rChatDb.beginTransaction();
            rChatDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmObject.deleteFromRealm(result);
                }
            });

            //rChatDb.commitTransaction();
        }
    }


    public static void readMessageFromRealm(Realm rChatDb, String msgId) {
        final Message result = rChatDb.where(Message.class).equalTo("id", msgId).findFirst();
        if (result != null) {
            //rChatDb.beginTransaction();
            rChatDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    result.setReadMsg(true);
                }
            });

            //rChatDb.commitTransaction();
        }
    }

    public static String timeFormater(float time) {
        long secs = (long) (time / 1000);
        long mins = (long) ((time / 1000) / 60);
        long hrs = (long) (((time / 1000) / 60) / 60); /* Convert the seconds to String * and format to ensure it has * a leading zero when required */
        secs = secs % 60;
        String seconds = String.valueOf(secs);
        if (secs == 0) {
            seconds = "00";
        }
        if (secs < 10 && secs > 0) {
            seconds = "0" + seconds;
        } /* Convert the minutes to String and format the String */
        mins = mins % 60;
        String minutes = String.valueOf(mins);
        if (mins == 0) {
            minutes = "00";
        }
        if (mins < 10 && mins > 0) {
            minutes = "0" + minutes;
        } /* Convert the hours to String and format the String */
        String hours = String.valueOf(hrs);
        if (hrs == 0) {
            hours = "00";
        }
        if (hrs < 10 && hrs > 0) {
            hours = "0" + hours;
        }

        return hours + ":" + minutes + ":" + seconds;
    }


    public static String getTimeAgo(long time, Context ctx) {
        if (time < 1000000000000L) {
            time *= 1000;
        }

        long now = System.currentTimeMillis();

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 24 * HOUR_MILLIS) {
            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            return formatter.format(calendar.getTime());
        } else if (diff < 48 * HOUR_MILLIS) {
            return "Yesterday";
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            return formatter.format(calendar.getTime());
        }
    }

    public static String getTimeAgoLastSeen(long time, Context ctx) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();

       /* if (time > now || time <= 0) {
            return null;
        }*/

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            //return diff / DAY_MILLIS + " days ago";
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            return formatter.format(calendar.getTime());
        }
    }

    public static String getFormattedDate(long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "dd/MM/yyyy";
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return "Today, " + DateFormat.format(timeFormatString, smsTime);
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return "Yesterday, " + DateFormat.format(timeFormatString, smsTime);
        } else {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        }
    }

    public static String getChatFormattedDate(long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "dd/MM/yyyy";
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return "" + DateFormat.format(timeFormatString, smsTime);
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return "Yesterday";
        } else {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        }
    }

    public static RealmList<String> getRandomElement(RealmList<String> list,
                                                     int totalItems) {
        Random rand = new Random();

        // create a temporary list for storing
        // selected element
        RealmList<String> newList = new RealmList<>();
        for (int i = 0; i < totalItems; i++) {

            // take a raundom index between 0 to size
            // of given List
            int randomIndex = rand.nextInt(list.size());

            // add element in temporary list
            if (!newList.contains(list.get(randomIndex)))
                newList.add(list.get(randomIndex));

            // Remove selected element from orginal list
//            list.remove(randomIndex);
        }
        return newList;
    }

    public static void unBlockAlert(String name, User userMe, Context context, Helper helper,
                                    String userId, FragmentManager manager) {
        String UNBLOCK_TAG = "UNBLOCK_TAG";

        ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment
                .newInstance("UnBlock", String.format("Are you sure want to unblock %s",
                        name), view -> {
                            if (userMe.getBlockedUsersIds().contains(userId)) {
                                userMe.getBlockedUsersIds().remove(userId);
                            }

                            BaseApplication.getUserRef().child(userMe.getId()).child("blockedUsersIds")
                                    .setValue(userMe.getBlockedUsersIds())
                                    .addOnSuccessListener(aVoid -> {
                                        helper.setLoggedInUser(userMe);
                                        Toast.makeText(context, "Unblocked", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(context, "Unable to unblock user", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        },
                        view -> {

                        });
        confirmationDialogFragment.show(manager, UNBLOCK_TAG);
    }
}
