package com.laodev.chatapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.laodev.chatapp.R;
import com.laodev.chatapp.models.CheckableUser;
import com.laodev.chatapp.utils.StringUtil;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ForwardUserAdapter extends RecyclerView.Adapter<ForwardUserAdapter.ViewHolder> {

    private Context context;
    private List<CheckableUser> userList;
    private String searchKey;
    private ForwardUserAdapterListener forwardUserAdapterListener;


    public ForwardUserAdapter(Context context, List<CheckableUser> userList, String searchKey) {
        this.context = context;
        this.userList = userList;
        this.searchKey = searchKey;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_item_forward_user, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.setData(userList.get(i));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setForwardUserAdapterListener(ForwardUserAdapterListener forwardUserAdapterListener) {
        this.forwardUserAdapterListener = forwardUserAdapterListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name, phone, status;
        private ImageView image;
        private CheckBox chk_select;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.user_name);
            phone = itemView.findViewById(R.id.user_phone);
            status = itemView.findViewById(R.id.user_status);
            image = itemView.findViewById(R.id.user_image);
            chk_select = itemView.findViewById(R.id.chk_select);
        }

        private void setData(final CheckableUser checkableUser) {
            Picasso.get()
                    .load(checkableUser.getUser().getImage())
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(image);
            String nameStr = checkableUser.getUser().getNameInPhone();
            if (searchKey.length() > 0 && nameStr.toLowerCase().contains(searchKey.toLowerCase())) {
                name.setText(StringUtil.getSearchText(nameStr, searchKey));
            } else {
                name.setText(nameStr);
            }

            String phoneStr = checkableUser.getUser().getId();
            if (phoneStr.contains("+")) {
                if (searchKey.length() > 0 && phoneStr.contains(searchKey)) {
                    phone.setText(StringUtil.getSearchText(phoneStr, searchKey));
                } else {
                    phone.setText(phoneStr);
                }
            } else {
                phone.setText(context.getString(R.string.social_account));
            }

            String statusStr = checkableUser.getUser().getStatus();
            if (searchKey.length() > 0 && statusStr.toLowerCase().contains(searchKey.toLowerCase())) {
                status.setText(StringUtil.getSearchText(statusStr, searchKey));
            } else {
                status.setText(statusStr);
            }

            chk_select.setChecked(checkableUser.isCheck());
            chk_select.setOnCheckedChangeListener((buttonView, isChecked) -> {
                checkableUser.setCheck(isChecked);
                if (forwardUserAdapterListener != null) {
                    forwardUserAdapterListener.onChangeCheckStatus();
                }
            });
        }

    }

    public interface ForwardUserAdapterListener {
        void onChangeCheckStatus();
    }

}
