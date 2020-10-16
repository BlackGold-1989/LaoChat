package com.laodev.chatapp.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.CallListActivity;
import com.laodev.chatapp.activities.MainActivity;
import com.laodev.chatapp.models.User;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CallListAdapter extends RecyclerView.Adapter<CallListAdapter.ViewHolder> implements Filterable {
    private Context context;
    private ArrayList<User> myUsers;
    private ArrayList<User> itemsFiltered;

    public CallListAdapter(Context context, ArrayList<User> myUsers) {
        this.context = context;
        this.myUsers = myUsers;
        itemsFiltered = myUsers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_list_item_log_call,
                viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        final User user = itemsFiltered.get(i);

        if (user.getImage() != null && !user.getImage().isEmpty()) {
            viewHolder.myProgressBar.setVisibility(View.VISIBLE);
            if (user.getBlockedUsersIds() != null && !user.getBlockedUsersIds().contains(MainActivity.userId))
                Picasso.get()
                        .load(user.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(viewHolder.userImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                viewHolder.myProgressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Exception e) {
                                viewHolder.myProgressBar.setVisibility(View.GONE);
                            }
                        });
            else {
                Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .tag(this)
                        .error(R.drawable.ic_avatar)
                        .placeholder(R.drawable.ic_avatar)
                        .into(viewHolder.userImage);
                viewHolder.myProgressBar.setVisibility(View.GONE);
            }
        } else {
            Picasso.get()
                    .load(R.drawable.ic_avatar)
                    .tag(this)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(viewHolder.userImage);
            viewHolder.myProgressBar.setVisibility(View.GONE);
        }
        viewHolder.userName.setText(user.getNameInPhone());
        viewHolder.status.setText(user.getStatus());

        viewHolder.audioCall.setOnClickListener(view -> ((CallListActivity) context).makeCall(false, user));

        viewHolder.videoCall.setOnClickListener(view -> ((CallListActivity) context).makeCall(true, user));
    }

    @Override
    public int getItemCount() {
        return itemsFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String query = charSequence.toString();

                ArrayList<User> filtered = new ArrayList<>();

                if (query.isEmpty()) {
                    filtered = myUsers;
                } else {
                    for (User user : myUsers) {
                        if (user.getNameInPhone().toLowerCase().contains(query.toLowerCase())) {
                            filtered.add(user);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.count = filtered.size();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults results) {
                itemsFiltered = (ArrayList<User>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView userImage;
        private ImageView audioCall;
        private ImageView videoCall;
        private TextView userName;
        private TextView status;
        private ProgressBar myProgressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            userImage = itemView.findViewById(R.id.userImage);
            userName = itemView.findViewById(R.id.userName);
            status = itemView.findViewById(R.id.status);
            audioCall = itemView.findViewById(R.id.audioCall);
            videoCall = itemView.findViewById(R.id.videoCall);
            myProgressBar = itemView.findViewById(R.id.progressBar);

        }
    }
}
