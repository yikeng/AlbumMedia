package com.demons.media.models.ad;

import android.view.View;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.demons.media.R;

/**
 * 广告viewolder
 */
public class AdViewHolder extends RecyclerView.ViewHolder {
    public FrameLayout adFrame;
    public AdViewHolder(View itemView) {
        super(itemView);
        adFrame = (FrameLayout) itemView.findViewById(R.id.ad_frame_easy_photos);
    }
}
