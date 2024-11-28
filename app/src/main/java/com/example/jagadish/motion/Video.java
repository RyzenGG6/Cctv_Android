package com.example.jagadish.motion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class Video extends RecyclerView.Adapter<Video.VideoViewHolder> {

    private Context context;
    private List<String> videoList;
    private OnVideoClickListener listener;

    public interface OnVideoClickListener {
        void onVideoClick(String videoPath);
    }

    public Video(Context context, List<String> videoList, OnVideoClickListener listener) {
        this.context = context;
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        String videoPath = videoList.get(position);
        File videoFile = new File(videoPath);
        holder.videoNameTextView.setText(videoFile.getName());

        Glide.with(context)
                .load(videoPath)
                .into(holder.thumbnailImageView);

        holder.itemView.setOnClickListener(v -> listener.onVideoClick(videoPath));
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        TextView videoNameTextView;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.thumbnailImageView);
            videoNameTextView = itemView.findViewById(R.id.videoNameTextView);
        }
    }
}

