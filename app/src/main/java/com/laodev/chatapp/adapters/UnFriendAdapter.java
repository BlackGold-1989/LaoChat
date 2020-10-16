package com.laodev.chatapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.laodev.chatapp.R;
import com.laodev.chatapp.models.User;
import com.squareup.picasso.Picasso;

import java.util.List;

public class UnFriendAdapter extends RecyclerView.Adapter<UnFriendAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;
    private UnFriendAdapterListener unFriendAdapterListener;


    public UnFriendAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_item_unfriend, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.setData(userList.get(i));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setUnFriendAdapterListener(UnFriendAdapterListener unFriendAdapterListener) {
        this.unFriendAdapterListener = unFriendAdapterListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView phone;
        private TextView status;
        private Button btn_add;
        private ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            phone = itemView.findViewById(R.id.user_phone);
            status = itemView.findViewById(R.id.user_status);
            btn_add = itemView.findViewById(R.id.btn_add);
            image = itemView.findViewById(R.id.user_image);
        }

        private void setData(final User user) {
            if (user.getImage().length() > 0) {
                Picasso.get()
                        .load(user.getImage())
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(image);
            }

            String phoneStr = user.getId();
            if (phoneStr.contains("+")) {
                phone.setText(phoneStr);
            } else {
                phone.setText(context.getString(R.string.social_account));
            }

            String statusStr = user.getStatus();
            status.setText(statusStr);

            btn_add.setOnClickListener(v -> unFriendAdapterListener.onAddFriendEvent(user));
        }

    }

    public interface UnFriendAdapterListener {
        void onAddFriendEvent(User user);
    }

}
