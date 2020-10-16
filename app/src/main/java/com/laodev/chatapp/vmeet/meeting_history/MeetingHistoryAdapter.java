package com.laodev.chatapp.vmeet.meeting_history;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.daimajia.swipe.implments.SwipeItemRecyclerMangerImpl;
import com.daimajia.swipe.util.Attributes;
import com.laodev.chatapp.R;
import com.laodev.chatapp.vmeet.bean.MeetingHistory;
import com.laodev.chatapp.vmeet.utils.AppConstants;
import com.laodev.chatapp.vmeet.utils.SharedObjects;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MeetingHistoryAdapter extends RecyclerSwipeAdapter<MeetingHistoryAdapter.ViewHolder> {

    ArrayList<MeetingHistory> list;
    Context context;
    OnItemClickListener onItemClickListener;
    SwipeItemRecyclerMangerImpl mItemManger;

    public MeetingHistoryAdapter(ArrayList<MeetingHistory> list, Context context) {
        this.list = list;
        this.context = context;
        mItemManger = new SwipeItemRecyclerMangerImpl(this);
        setMode(Attributes.Mode.Single);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.itemview_meeting_history, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        final MeetingHistory bean = list.get(position);

        if (!TextUtils.isEmpty(bean.getMeeting_id())) {
            holder.txtName.setText(bean.getMeeting_id());
        }else{
            holder.txtName.setText("");
        }

        if (!TextUtils.isEmpty(bean.getStartTime())) {

            String date = SharedObjects.convertDateFormat(bean.getStartTime()
                    , AppConstants.DateFormats.DATETIME_FORMAT_24,AppConstants.DateFormats.DATE_FORMAT_DD_MMM_YYYY);

            String time = SharedObjects.convertDateFormat(bean.getStartTime()
                    ,AppConstants.DateFormats.DATETIME_FORMAT_24, AppConstants.DateFormats.TIME_FORMAT_12);

            holder.txtDate.setText(date + ", " + time);

            if (date.equalsIgnoreCase(SharedObjects.getTodaysDate(AppConstants.DateFormats.DATE_FORMAT_DD_MMM_YYYY))){
                holder.btnJoin.setVisibility(View.VISIBLE);
            }else{
                holder.btnJoin.setVisibility(View.GONE);
            }

        }else{
            holder.txtDate.setText("");
        }

        if (!TextUtils.isEmpty(bean.getStartTime()) && !TextUtils.isEmpty(bean.getEndTime())) {

            //HH converts hour in 24 hours format (0-23), day calculation
            SimpleDateFormat format = new SimpleDateFormat(AppConstants.DateFormats.DATETIME_FORMAT_24);

            Date d1 = null;
            Date d2 = null;

            try {
                d1 = format.parse(bean.getStartTime());
                d2 = format.parse(bean.getEndTime());

                //in milliseconds
                long diff = d2.getTime() - d1.getTime();

                long diffSeconds = diff / 1000 % 60;
                long diffMinutes = diff / (60 * 1000) % 60;
                long diffHours = diff / (60 * 60 * 1000) % 24;
                long diffDays = diff / (24 * 60 * 60 * 1000);

                if (diffHours > 0){
                    holder.txtDuration.setText(SharedObjects.pad(Integer.parseInt(String.valueOf(diffHours))) + ":"
                            + SharedObjects.pad(Integer.parseInt(String.valueOf(diffMinutes))) + ":"
                            + SharedObjects.pad(Integer.parseInt(String.valueOf(diffSeconds))));
                }else if (diffMinutes > 0){
                    holder.txtDuration.setText(SharedObjects.pad(Integer.parseInt(String.valueOf(diffMinutes)))
                            + ":" + SharedObjects.pad(Integer.parseInt(String.valueOf(diffSeconds))));
                }else if (diffSeconds > 0){
                    holder.txtDuration.setText(SharedObjects.pad(Integer.parseInt(String.valueOf(diffSeconds))) + " sec(s)");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            holder.txtDuration.setText("-");
        }

        holder.llDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    mItemManger.closeItem(position);
                    onItemClickListener.onDeleteClickListener(position, list.get(position));
                }
            }
        });

        holder.btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    mItemManger.closeItem(position);
                    onItemClickListener.onJoinClickListener(position, list.get(position));
                }
            }
        });

        holder.llMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClickListener(position, list.get(position));
                }
            }
        });

        holder.swipe.setShowMode(SwipeLayout.ShowMode.PullOut);
        holder.swipe.addSwipeListener(new SwipeLayout.SwipeListener() {
            @Override
            public void onStartOpen(SwipeLayout layout) {
            }

            @Override
            public void onOpen(SwipeLayout layout) {
            }

            @Override
            public void onStartClose(SwipeLayout layout) {
            }

            @Override
            public void onClose(SwipeLayout layout) {
            }

            @Override
            public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {
            }

            @Override
            public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {
            }
        });

        mItemManger.bindView(holder.itemView, position);
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public interface OnItemClickListener {
        void onItemClickListener(int position, MeetingHistory bean);
        void onDeleteClickListener(int position, MeetingHistory bean);
        void onJoinClickListener(int position, MeetingHistory bean);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView txtName;
        private Button btnJoin;
        private TextView txtDuration;
        private TextView txtDate;

        private LinearLayout llMain;
        private SwipeLayout swipe;
        private LinearLayout llDelete ;

        public ViewHolder(View itemView) {
            super(itemView);

            txtName = itemView.findViewById(R.id.txtName);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            txtDate = itemView.findViewById(R.id.txtDate);
            llMain = itemView.findViewById(R.id.llMain);
            swipe = itemView.findViewById(R.id.swipe);
            llDelete = itemView.findViewById(R.id.llDelete);
        }
    }
}



