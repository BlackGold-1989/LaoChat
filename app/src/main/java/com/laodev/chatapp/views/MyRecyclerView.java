package com.laodev.chatapp.views;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.laodev.chatapp.R;
import com.squareup.picasso.Picasso;


/**
 * Created by a_man on 10-Jul-17.
 */

public class MyRecyclerView extends RecyclerView {

    private View emptyView;
    private ImageView emptyImage;
    private TextView emptyText;
    private View[] views;

    final private AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }
    };

    public MyRecyclerView(Context context) {
        super(context);
    }

    public MyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void checkIfEmpty() {
        if (emptyView != null && getAdapter() != null) {
            final boolean emptyViewVisible =
                    getAdapter().getItemCount() == 0;
            emptyView.setVisibility(emptyViewVisible ? VISIBLE : GONE);
            setVisibility(emptyViewVisible ? GONE : VISIBLE);
            if (views != null) {
                for (View v : views)
                    v.setVisibility(emptyViewVisible ? GONE : VISIBLE);
            }
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }

        checkIfEmpty();
    }

    public View getEmptyView() {
        return emptyView;
    }

    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
        checkIfEmpty();
    }

    public void setEmptyTextView(TextView textView) {
        this.emptyText = textView;
    }

    public void setEmptyImageView(ImageView imageView) {
        this.emptyImage = imageView;
    }

    public void setEmptyText(String text) {
        if (emptyText != null) emptyText.setText(text);
    }

    public void setEmptyImage(int resource) {
        if (emptyImage != null) emptyImage.setImageResource(resource);
    }

    public void setEmptyImage(String url) {
        if (emptyImage != null)/* Glide.with(context).load(url).into(emptyImage);*/
            Picasso.get()
                    .load(url)
                    .tag(this)
                    .placeholder(R.drawable.ic_avatar)
                    .into(emptyImage);

    }

    public void hideEmptyView() {
        if (emptyImage != null) emptyView.setVisibility(GONE);
    }

    public void setAdditionalHelperView(View[] views) {
        this.views = views;
    }
}
