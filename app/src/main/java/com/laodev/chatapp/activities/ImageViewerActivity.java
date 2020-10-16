package com.laodev.chatapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;

import com.github.chrisbanes.photoview.PhotoView;
import com.laodev.chatapp.R;
import com.squareup.picasso.Picasso;

/**
 * Created by mayank on 10/5/17.
 */

public class ImageViewerActivity extends AppCompatActivity {

    private static final String IMAGE_URL = ImageViewerActivity.class.getPackage().getName() + ".image_url";

    public static Intent newInstance(Context context, String imageUrl) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.putExtra(IMAGE_URL, imageUrl);
        return intent;
    }

    PhotoView photoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        photoView = findViewById(R.id.photo_view);

        String imageUrl = getIntent().getStringExtra(IMAGE_URL);
        if (!TextUtils.isEmpty(imageUrl))
            Picasso.get()
                    .load(imageUrl)
                    .tag(this)
                    .placeholder(R.drawable.ic_logo_)
                    .into(photoView);
    }
}
