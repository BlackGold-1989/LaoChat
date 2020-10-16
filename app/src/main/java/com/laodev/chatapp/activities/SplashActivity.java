package com.laodev.chatapp.activities;

import android.content.Intent;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.daasuu.ahp.AnimateHorizontalProgressBar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.laodev.chatapp.BaseApplication;
import com.laodev.chatapp.BuildConfig;
import com.laodev.chatapp.R;
import com.laodev.chatapp.models.AppInfo;
import com.laodev.chatapp.utils.Helper;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AnimateHorizontalProgressBar progressBar = findViewById(R.id.animate_progress_bar);
        progressBar.setMax(1500);
        progressBar.setProgressWithAnim(1500);

        BaseApplication.getAppRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppInfo appInfo = snapshot.getValue(AppInfo.class);
                if (appInfo.version.equals(BuildConfig.VERSION_NAME) && appInfo.version_code.equals(String.valueOf(BuildConfig.VERSION_CODE))) {
                    final Helper helper = new Helper(SplashActivity.this);
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(SplashActivity.this, helper.getLoggedInUser() != null ? MainActivity.class : SignInActivity.class));
                        finish();
                    }, 1500);
                } else {
                    Toast.makeText(SplashActivity.this, R.string.app_old_version, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SplashActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

}
