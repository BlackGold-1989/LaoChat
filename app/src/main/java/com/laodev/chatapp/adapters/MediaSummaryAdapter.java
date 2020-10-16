package com.laodev.chatapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.laodev.chatapp.R;
import com.laodev.chatapp.activities.ImageViewerActivity;
import com.laodev.chatapp.models.AttachmentTypes;
import com.laodev.chatapp.models.Message;
import com.laodev.chatapp.utils.FileUtils;
import com.laodev.chatapp.utils.Helper;
import com.laodev.chatapp.utils.MyFileProvider;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by a_man on 6/28/2017.
 */

public class MediaSummaryAdapter extends RecyclerView.Adapter<MediaSummaryAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<Message> dataList;

    private int imageViewDimens;
    private Drawable audioIcon;
    private Drawable documentIcon;
    private String myId;

    public MediaSummaryAdapter(Context context, ArrayList<Message> attachments, boolean grid, String myId) {
        this.context = context;
        this.dataList = attachments;
        this.myId = myId;
        imageViewDimens = context.getResources().getDimensionPixelSize(grid ? R.dimen.media_summary_grid : R.dimen.media_summary_strip);
        createDrawables();
    }

    private void createDrawables() {
        audioIcon = ContextCompat.getDrawable(context, R.drawable.ic_audiotrack_24dp);
        audioIcon = DrawableCompat.wrap(audioIcon);
        DrawableCompat.setTint(audioIcon, Color.WHITE);
        DrawableCompat.setTintMode(audioIcon, PorterDuff.Mode.SRC_ATOP);

        Drawable videoIcon = ContextCompat.getDrawable(context, R.drawable.ic_video_24dp);
        videoIcon = DrawableCompat.wrap(videoIcon);
        DrawableCompat.setTint(videoIcon, Color.WHITE);
        DrawableCompat.setTintMode(videoIcon, PorterDuff.Mode.SRC_ATOP);

        documentIcon = ContextCompat.getDrawable(context, R.drawable.ic_insert_64dp);
        documentIcon = DrawableCompat.wrap(documentIcon);
        DrawableCompat.setTint(documentIcon, Color.WHITE);
        DrawableCompat.setTintMode(documentIcon, PorterDuff.Mode.SRC_ATOP);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_summary, parent, false);
        view.getLayoutParams().height = imageViewDimens;
        view.getLayoutParams().width = imageViewDimens;
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.setData(dataList.get(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView textView;

        private File file = null;

        MyViewHolder(View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.mediaImage);
            this.textView = itemView.findViewById(R.id.mediaSummary);
            itemView.setOnClickListener(v -> {
                if (file != null) if (file.exists()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = MyFileProvider.getUriForFile(context,
                            context.getString(R.string.authority),
                            file);
                    intent.setDataAndType(uri, Helper.getMimeType(context, uri)); //storage path is path of your vcf file and vFile is name of that file.
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                } else if (dataList.get(getAdapterPosition()).getAttachmentType() == AttachmentTypes.IMAGE)
                    context.startActivity(ImageViewerActivity.newInstance(context, dataList.get(getAdapterPosition()).getAttachment().getUrl()));
                else
                    Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_LONG).show();
            });
        }

        @SuppressLint("SetTextI18n")
        public void setData(Message msg) {
            file = new File(Environment.getExternalStorageDirectory() + "/"
                    +
                    context.getString(R.string.app_name) + "/" + AttachmentTypes.getTypeName(msg.getAttachmentType()) + (myId.equals(msg.getSenderId()) ? "/.sent/" : "")
                    , msg.getAttachment().getName());
            switch (msg.getAttachmentType()) {
                case AttachmentTypes.IMAGE:
                    textView.setVisibility(View.GONE);
                    imageView.setBackgroundColor(Color.TRANSPARENT);
                    Picasso.get()
                            .load(msg.getAttachment().getUrl())
                            .tag(this)
                            .placeholder(R.drawable.ic_logo_)
                            .into(imageView);
                    break;
                case AttachmentTypes.AUDIO:
                    imageView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreen));
                    imageView.setImageDrawable(audioIcon);
                    if (file.exists()) {
                        Uri uri = Uri.fromFile(file);
                        try {
                            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                            mmr.setDataSource(context, uri);
                            String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            int millis = Integer.parseInt(durationStr);
                            textView.setVisibility(View.VISIBLE);
                            textView.setText(TimeUnit.MILLISECONDS.toMinutes(millis) + ":" + TimeUnit.MILLISECONDS.toSeconds(millis));
                            mmr.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case AttachmentTypes.VIDEO:
                    textView.setVisibility(View.VISIBLE);
                    imageView.setBackgroundColor(Color.TRANSPARENT);
                    textView.setText(msg.getAttachment().getName());
                    Picasso.get()
                            .load(msg.getAttachment().getData())
                            .tag(this)
                            .placeholder(R.drawable.ic_logo_)
                            .into(imageView);
                    break;
                case AttachmentTypes.DOCUMENT:
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(msg.getAttachment().getName() + "." + FileUtils.getExtension(msg.getAttachment().getName()));
                    imageView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreen));
                    imageView.setImageDrawable(documentIcon);
                    break;
                case AttachmentTypes.CONTACT:
                    break;
                case AttachmentTypes.LOCATION:
                    break;
                case AttachmentTypes.NONE_TEXT:
                    break;
                case AttachmentTypes.NONE_TYPING:
                    break;
                case AttachmentTypes.RECORDING:
                    break;
            }
        }
    }
}
