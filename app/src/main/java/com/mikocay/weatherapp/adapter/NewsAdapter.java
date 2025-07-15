package com.mikocay.weatherapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.mikocay.weatherapp.R;
import com.mikocay.weatherapp.model.News;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private List<News> newsList;
    private Context context;

    public NewsAdapter(Context context, List<News> newsList) {
        this.context = context;
        this.newsList = newsList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_item, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        News news = newsList.get(position);

        holder.titleTextView.setText(news.getTitle());
        holder.descriptionTextView.setText(news.getDescription());
        holder.sourceTextView.setText(news.getSource());

        // Format và hiển thị thời gian
        String formattedTime = formatTime(news.getPublishedAt());
        holder.timeTextView.setText(formattedTime);

        // Load image using Glide
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(news.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imageView);
        }

        // Set click listener to open article - sử dụng itemView thay vì cardView
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getUrl()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    private String formatTime(String publishedAt) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);
            Date date = inputFormat.parse(publishedAt);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return publishedAt;
        }
    }

    public void updateNews(List<News> newNewsList) {
        this.newsList = newNewsList;
        notifyDataSetChanged();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView sourceTextView;
        TextView timeTextView;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.news_image);
            titleTextView = itemView.findViewById(R.id.news_title);
            descriptionTextView = itemView.findViewById(R.id.news_description);
            sourceTextView = itemView.findViewById(R.id.news_source);
            timeTextView = itemView.findViewById(R.id.news_time);
        }
    }
}