package com.laodev.chatapp.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.laodev.chatapp.R;
import com.laodev.chatapp.interfaces.OnUserGroupItemClick;
import com.laodev.chatapp.models.Group;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by a_man on 31-12-2017.
 */

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.MyViewHolder> implements Filterable {

    private OnUserGroupItemClick itemClickListener;
    private ArrayList<Group> dataList, dataListFiltered;
    private Filter filter;

    public GroupsAdapter(@NonNull Context context, @Nullable ArrayList<Group> groups) {
        if (context instanceof OnUserGroupItemClick) {
            this.itemClickListener = (OnUserGroupItemClick) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnUserGroupItemClick");
        }

        this.dataList = groups;
        this.dataListFiltered = groups;
        this.filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    dataListFiltered = dataList;
                } else {
                    ArrayList<Group> filteredList = new ArrayList<>();
                    for (Group row : dataList) {
                        if (row.getName().toLowerCase().startsWith(charString.toLowerCase())) {
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
                dataListFiltered = (ArrayList<Group>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_user, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.setData(dataListFiltered.get(position));
    }

    @Override
    public int getItemCount() {
        return dataListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return this.filter;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        ImageView userImage;

        MyViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);

            itemView.setOnClickListener(view -> {
                int pos = getAdapterPosition();
                itemClickListener.OnGroupClick(dataListFiltered.get(pos), pos, userImage);
            });
        }

        public void setData(Group group) {
            Picasso.get()
                    .load(group.getImage())
                    .tag(this)
                    .placeholder(R.drawable.ic_logo_)
                    .into(userImage);

            userName.setText(group.getName());
        }
    }
}
