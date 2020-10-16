package com.laodev.chatapp.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.laodev.chatapp.R;
import com.laodev.chatapp.models.CheckableUser;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class SelectedUserView extends LinearLayout {

    private CircleImageView img_user;
    private CheckableUser user;

    private SelectedUserViewListener selectedUserViewListener;

    public SelectedUserView(Context context) {
        super(context);

        setOrientation(LinearLayout.HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.item_select_user, this, true);

        img_user = findViewById(R.id.user_image);
        ImageView img_close = findViewById(R.id.img_close);
        img_close.setOnClickListener(v -> {
            if (user != null) {
                user.setCheck(false);
                selectedUserViewListener.onClickCloseButton();
            }
        });
    }

    public void setUser(CheckableUser user) {
        this.user = user;

        Picasso.get()
                .load(user.getUser().getImage())
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .into(img_user);
    }

    public void setSelectedUserViewListener(SelectedUserViewListener selectedUserViewListener) {
        this.selectedUserViewListener = selectedUserViewListener;
    }

    public interface SelectedUserViewListener {
        void onClickCloseButton();
    }

}
