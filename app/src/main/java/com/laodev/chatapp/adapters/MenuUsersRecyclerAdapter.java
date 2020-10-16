package com.laodev.chatapp.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.MainActivity;
import com.laodev.chatapp.interfaces.OnUserGroupItemClick;
import com.laodev.chatapp.models.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by mayank on 7/5/17.
 */

public class MenuUsersRecyclerAdapter extends RecyclerView.Adapter<MenuUsersRecyclerAdapter.BaseViewHolder> implements Filterable {
    private OnUserGroupItemClick itemClickListener;
    private ArrayList<User> dataList, dataListFiltered;
    private Filter filter;
    private User userMe;

    public MenuUsersRecyclerAdapter(@NonNull Context context, @Nullable ArrayList<User> users, User userMe) {
        if (context instanceof OnUserGroupItemClick) {
            this.itemClickListener = (OnUserGroupItemClick) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnUserGroupItemClick");
        }

        this.dataList = users;
        this.dataListFiltered = users;
        this.userMe = userMe;
        this.filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    dataListFiltered = dataList;
                } else {
                    ArrayList<User> filteredList = new ArrayList<>();
                    for (User row : dataList) {
                        String toCheckWith = row.getNameInPhone() != null ? row.getNameInPhone() : row.getName();
                        if (toCheckWith.toLowerCase().startsWith(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }
                    dataListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = dataListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                dataListFiltered = (ArrayList<User>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UsersViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_user,
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        if (holder instanceof UsersViewHolder) {
            ((UsersViewHolder) holder).setData(dataListFiltered.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return dataListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return this.filter;
    }

    class BaseViewHolder extends RecyclerView.ViewHolder {
        BaseViewHolder(View itemView) {
            super(itemView);
        }
    }

    class UsersViewHolder extends BaseViewHolder {
        private ImageView userImage;
        private TextView userName, status;

        UsersViewHolder(final View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            status = itemView.findViewById(R.id.status);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                itemClickListener.OnUserClick(dataListFiltered.get(pos), pos, userImage);
            });
        }

        public void setData(User user) {
            userName.setText(TextUtils.isEmpty(user.getNameInPhone()) ? user.getName() : user.getNameInPhone());

            String profileImageUrl = user.getImage();
            if (profileImageUrl != null && !profileImageUrl.isEmpty())
                if (user.getBlockedUsersIds() != null && !user.getBlockedUsersIds().contains(MainActivity.userId))
                    Picasso.get()
                            .load(profileImageUrl)
                            .tag(this)
                            .placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.ic_avatar)
                            .into(userImage);
                else
                    Picasso.get()
                            .load(R.drawable.ic_avatar)
                            .tag(this)
                            .placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.ic_avatar)
                            .into(userImage);
            else
                Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(userImage);

            if (userMe.getBlockedUsersIds() != null && userMe.getBlockedUsersIds().contains(user.getId())) {
                if (status != null) {
                    status.setVisibility(View.VISIBLE);
                    status.setText(R.string.tap_unblock);
                }
            } else {
                if (status != null) {
                    status.setVisibility(View.GONE);
                    status.setText(user.getStatus() != null ? user.getStatus() : " ");
                }
            }
            userName.setCompoundDrawablesWithIntrinsicBounds(0, 0, user.isOnline() ? R.drawable.ring_green : 0, 0);
        }
    }
}
