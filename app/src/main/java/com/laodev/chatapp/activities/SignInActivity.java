package com.laodev.chatapp.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.R;
import com.laodev.chatapp.models.Country;
import com.laodev.chatapp.models.User;
import com.laodev.chatapp.utils.BannerUtil;
import com.laodev.chatapp.utils.Constants;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.utils.KeyboardUtil;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {

    private View content;
    private ProgressDialog progressDialog;
    private SearchableSpinner spinnerCountryCodes;
    private EditText etPhone;
    private EditText otpCode;
    private TextView verificationMessage, retryTimer, myOtpexpiresTXT;
    private CountDownTimer countDownTimer;
    private TextView myCountryCodeTXT;

    private FirebaseAuth mAuth;
    private String phoneNumberInPrefs = null;
    private String mVerificationId;
    private boolean authInProgress;

    private KeyboardUtil keyboardUtil;
    private Helper helper;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new Helper(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        callbackManager = CallbackManager.Factory.create();

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.VIBRATE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.READ_PHONE_STATE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        report.areAllPermissionsGranted();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();

        User user = helper.getLoggedInUser();
        if (user != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            phoneNumberInPrefs = helper.getPhoneNumberForVerification();
            keyboardUtil = KeyboardUtil.getInstance(this);
            progressDialog = new ProgressDialog(this);

            setContentView(TextUtils.isEmpty(phoneNumberInPrefs) ? R.layout.activity_sign_in_1 : R.layout.activity_sign_in_2);
            content = findViewById(R.id.content);
            mAuth = FirebaseAuth.getInstance();
            LinearLayout signInButton = findViewById(R.id.sign_in_button);
            LoginButton signInButtonFB = findViewById(R.id.sign_in_button_fb);
            signInButtonFB.setReadPermissions(Arrays.asList("email", "public_profile"));
            if (TextUtils.isEmpty(phoneNumberInPrefs)) {
                spinnerCountryCodes = findViewById(R.id.countryCode);
                etPhone = findViewById(R.id.phoneNumber);
                myCountryCodeTXT = findViewById(R.id.layout_registration_country_code_TXT);
                setupCountryCodes();
                signInButton.setVisibility(View.VISIBLE);
                signInButton.setOnClickListener(v -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, 100);
                });
                signInButtonFB.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });
            } else {
                retryTimer = findViewById(R.id.resend);
                myOtpexpiresTXT = findViewById(R.id.otp_expires_txt);
                verificationMessage = findViewById(R.id.verificationMessage);
                otpCode = findViewById(R.id.otp);
                findViewById(R.id.back).setOnClickListener(view -> back());
                findViewById(R.id.changeNumber).setOnClickListener(view -> {
                    helper.clearPhoneNumberForVerification();
                    recreate();
                });
                initiateAuth(phoneNumberInPrefs);
            }
            findViewById(R.id.submit).setOnClickListener(view -> {
                if (TextUtils.isEmpty(phoneNumberInPrefs)) {
                    submit();
                } else {
                    String otp = otpCode.getText().toString();
                    if (!TextUtils.isEmpty(otp) && !TextUtils.isEmpty(mVerificationId))
                        signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(mVerificationId, otp));
                }
            });
        }
    }

    private void showProgress(int i) {
        String title = (i == 1) ? getString(R.string.sending_code) : getString(R.string.verifying_otp);
        String message = (i == 1) ? (getString(R.string.one_tiem_password) + phoneNumberInPrefs) : getString(R.string.verifying_otp);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void initiateAuth(String phone) {
        showProgress(1);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phone, 60, TimeUnit.SECONDS, this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                        super.onCodeAutoRetrievalTimeOut(s);
                    }

                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        progressDialog.dismiss();
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        authInProgress = false;
                        progressDialog.dismiss();
                        countDownTimer.cancel();
                        if ((e.getMessage() != null) && e.getMessage().contains("E.164")) {
                            verificationMessage.setText(getString(R.string.otp_incorrect));
                        } else {
                            String error = ((e.getMessage() != null) ? ("\n" + e.getMessage()) : "");
                            verificationMessage.setText(String.format(getString(R.string.went_wrong), error));
                        }

                        retryTimer.setVisibility(View.VISIBLE);
                        retryTimer.setText(getString(R.string.resend_code));
                        myOtpexpiresTXT.setText(R.string.no_recevied_code);
                        retryTimer.setOnClickListener(view -> initiateAuth(phoneNumberInPrefs));
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verificationId, forceResendingToken);
                        authInProgress = true;
                        progressDialog.dismiss();
                        mVerificationId = verificationId;
                        myOtpexpiresTXT.setText(getString(R.string.can_resend_otp));
                        verificationMessage.setText(String.format(getString(R.string.verify_title), phoneNumberInPrefs));
                    }
                });
        startCountdown();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        showProgress(2);
        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressDialog.setMessage(getString(R.string.logging_in));
                login();
            }
        }).addOnFailureListener(e -> {
            if (e.getMessage() != null && e.getMessage().contains("invalid")) {
                Toast.makeText(SignInActivity.this, R.string.invalid_otp, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(SignInActivity.this, e.getMessage() != null ? "\n" + e.getMessage() : "", Toast.LENGTH_LONG).show();
            }
            progressDialog.dismiss();
            authInProgress = false;
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = mAuth.getCurrentUser();
                        BaseApplication.getUserRef().child(fbUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                progressDialog.dismiss();
                                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                    try {
                                        User user = dataSnapshot.getValue(User.class);
                                        if (User.validate(user)) {
                                            helper.setLoggedInUser(user);
                                            done();
                                        } else {
                                            createUser(new User(fbUser.getUid(), fbUser.getDisplayName(), getString(R.string.app_name), fbUser.getPhotoUrl().toString()));
                                        }
                                    } catch (Exception ex) {
                                        createUser(new User(fbUser.getUid(), fbUser.getDisplayName(), getString(R.string.app_name), fbUser.getPhotoUrl().toString()));
                                    }
                                } else {
                                    createUser(new User(fbUser.getUid(), fbUser.getDisplayName(), getString(R.string.app_name), fbUser.getPhotoUrl().toString()));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                BannerUtil.onShowErrorAlertEvent(content, databaseError.getMessage(), Constants.DELAY_BANNER);
                            }
                        });
                    }
                });
    }


    private void login() {
        authInProgress = true;
        BaseApplication.getUserRef().child(phoneNumberInPrefs).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    try {
                        User user = dataSnapshot.getValue(User.class);
                        if (User.validate(user)) {
                            helper.setLoggedInUser(user);
                            done();
                        } else {
                            createUser(new User(phoneNumberInPrefs, phoneNumberInPrefs, getString(R.string.app_name), ""));
                        }
                    } catch (Exception ex) {
                        createUser(new User(phoneNumberInPrefs, phoneNumberInPrefs, getString(R.string.app_name), ""));
                    }
                } else {
                    createUser(new User(phoneNumberInPrefs, phoneNumberInPrefs, getString(R.string.app_name), ""));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                BannerUtil.onShowErrorAlertEvent(content, databaseError.getMessage(), Constants.DELAY_BANNER);
            }
        });
    }

    private void createUser(final User newUser) {
        BaseApplication.getUserRef().child(newUser.getId()).setValue(newUser)
                .addOnSuccessListener(aVoid -> {
                    helper.setLoggedInUser(newUser);
                    done();
                })
                .addOnFailureListener(e -> BannerUtil.onShowErrorAlertEvent(content, R.string.wrong_create_user, Constants.DELAY_BANNER));
    }

    private void back() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cancel_verification);
        builder.setMessage(R.string.cancel_verification_content);
        builder.setNegativeButton(android.R.string.yes, (dialogInterface, i) -> {
            helper.clearPhoneNumberForVerification();
            recreate();
            dialogInterface.dismiss();
        });
        builder.setPositiveButton(android.R.string.no, (dialogInterface, i) -> dialogInterface.dismiss());
        if (progressDialog.isShowing() || authInProgress) {
            builder.create().show();
        } else {
            helper.clearPhoneNumberForVerification();
            recreate();
        }
    }

    private void startCountdown() {
        retryTimer.setOnClickListener(null);
        myOtpexpiresTXT.setText(getString(R.string.can_resend_otp));
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long l) {
                if (retryTimer != null) {
                    retryTimer.setText(String.valueOf(l / 1000));
                }
            }

            @Override
            public void onFinish() {
                if (retryTimer != null) {
                    retryTimer.setText(getString(R.string.resend));
                    myOtpexpiresTXT.setText(getString(R.string.no_recevied_code));
                    retryTimer.setOnClickListener(view -> initiateAuth(phoneNumberInPrefs));
                }
            }
        }.start();
    }

    private void setupCountryCodes() {
        ArrayList<Country> countries = getCountries();
        if (countries != null) {
            ArrayList<Country> aCountries1;
            Country country = new Country("", getString(R.string.select_country), "");
            aCountries1 = getCountries();
            aCountries1.add(0, country);

            final ArrayAdapter<Country> adapter = new ArrayAdapter<>(SignInActivity.this,
                    android.R.layout.simple_spinner_item, aCountries1);

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCountryCodes.setAdapter(adapter);

            spinnerCountryCodes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        myCountryCodeTXT.setText("");
                    } else {
                        final String aDialCode = ((Country) spinnerCountryCodes.getSelectedItem()).getDialCode();
                        myCountryCodeTXT.setText(aDialCode);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    BannerUtil.onShowWaringAlertEvent(content, R.string.no_select_country, Constants.DELAY_BANNER);
                }
            });
        }
    }

    private ArrayList<Country> getCountries() {
        ArrayList<Country> toReturn = new ArrayList<>();
        try {
            JSONArray countrArray = new JSONArray(readEncodedJsonString(this));
            toReturn = new ArrayList<>();
            for (int i = 0; i < countrArray.length(); i++) {
                JSONObject jsonObject = countrArray.getJSONObject(i);
                String countryName = jsonObject.getString("name");
                String countryDialCode = jsonObject.getString("dial_code");
                String countryCode = jsonObject.getString("code");
                Country country = new Country(countryCode, countryName, countryDialCode);
                toReturn.add(country);
            }
            Collections.sort(toReturn, (lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));
        } catch (JSONException e) {
            BannerUtil.onShowErrorAlertEvent(content, e.getMessage(), Constants.DELAY_BANNER);
        }
        return toReturn;
    }

    private String readEncodedJsonString(Context context) {
        String base64 = context.getResources().getString(R.string.countries_code);
        byte[] data = Base64.decode(base64, Base64.DEFAULT);
        return new String(data, StandardCharsets.UTF_8);
    }

    //Go to main activity
    private void done() {
        startActivity(new Intent(this, PrivacyPolicyActivity.class));
        finish();
    }

    public void submit() {
        try {
            if (spinnerCountryCodes.getSelectedItem() == null || (myCountryCodeTXT.getText().toString().trim().equalsIgnoreCase(""))) {
                BannerUtil.onShowSuccessAlertEvent(content, R.string.select_country_code, Constants.DELAY_BANNER);
                return;
            }
            if (TextUtils.isEmpty(etPhone.getText().toString())) {
                BannerUtil.onShowWaringAlertEvent(content, R.string.enter_phone_number, Constants.DELAY_BANNER);
                return;
            }
            final String phoneNumber = ((Country) spinnerCountryCodes.getSelectedItem()).getDialCode() + etPhone.getText().toString().replaceAll("\\s+", "");

            if (isValidPhoneNumber(etPhone.getText().toString().replaceAll("\\s+", ""))) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(phoneNumber);
                builder.setMessage(R.string.one_tiem_password_content);
                builder.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    helper.setPhoneNumberForVerification(phoneNumber);
                    recreate();
                    dialogInterface.dismiss();
                });
                builder.setNegativeButton(R.string.edit, (dialogInterface, i) -> {
                    etPhone.requestFocus();
                    keyboardUtil.openKeyboard();
                    dialogInterface.dismiss();
                });
                builder.create().show();

            } else {
                BannerUtil.onShowErrorAlertEvent(content, R.string.invalid_phone_content, Constants.DELAY_BANNER);
            }
        } catch (Exception e) {
            BannerUtil.onShowErrorAlertEvent(content, e.getMessage(), Constants.DELAY_BANNER);
        }
    }

    private boolean isValidPhoneNumber(CharSequence phoneNumber) {
        if (!TextUtils.isEmpty(phoneNumber)) {
            return Patterns.PHONE.matcher(phoneNumber).matches();
        }
        return false;
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String id = account.getId() != null? account.getId() : "";
                String accessToken = account.getIdToken();

                AuthCredential credential = GoogleAuthProvider.getCredential(accessToken, null);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            if (!task.isSuccessful()) {
                                BannerUtil.onShowErrorAlertEvent(content, R.string.auth_failed, Constants.DELAY_BANNER);
                            } else {
                                BaseApplication.getUserRef().child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        progressDialog.dismiss();
                                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                            try {
                                                User user = dataSnapshot.getValue(User.class);
                                                if (User.validate(user)) {
                                                    helper.setLoggedInUser(user);
                                                    done();
                                                } else {
                                                    createUser(new User(id, account.getDisplayName(), getString(R.string.app_name), account.getPhotoUrl().toString()));
                                                }
                                            } catch (Exception ex) {
                                                createUser(new User(id, account.getDisplayName(), getString(R.string.app_name), account.getPhotoUrl().toString()));
                                            }
                                        } else {
                                            createUser(new User(id, account.getDisplayName(), getString(R.string.app_name), account.getPhotoUrl().toString()));
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        BannerUtil.onShowErrorAlertEvent(content, databaseError.getMessage(), Constants.DELAY_BANNER);
                                    }
                                });
                            }
                        });
            } else {
                BannerUtil.onShowErrorAlertEvent(content, R.string.no_get_info, Constants.DELAY_BANNER);
            }
        } catch (ApiException e) {
            BannerUtil.onShowErrorAlertEvent(content, e.getMessage(), Constants.DELAY_BANNER);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

}
