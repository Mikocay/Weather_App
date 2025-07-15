package com.mikocay.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mikocay.weatherapp.adapter.NewsAdapter;
import com.mikocay.weatherapp.model.News;
import com.mikocay.weatherapp.network.NewsService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsActivity extends AppCompatActivity {
    private static final String TAG = "NewsActivity";
    private RecyclerView recyclerView;
    private NewsAdapter newsAdapter;
    private ProgressBar progressBar;
    private Spinner countrySpinner;
    private List<News> newsList = new ArrayList<>();
    private boolean isLoading = false;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        initViews();
        setupRecyclerView();
        setupCountrySpinner();
        setupBottomNavigation();
        setupBackButton();

        // Delay initial load để đảm bảo UI đã setup xong
        recyclerView.post(() -> loadVietnamNews());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.news_recycler_view);
        progressBar = findViewById(R.id.news_progress_bar);
        countrySpinner = findViewById(R.id.country_spinner);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(this, newsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(newsAdapter);
    }

    private void setupCountrySpinner() {
        String[] countries = {"Vietnam", "United States", "United Kingdom", "Japan", "South Korea"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(adapter);

        countrySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (isLoading) {
                    Log.d(TAG, "Already loading, skipping request");
                    return;
                }

                String selected = countries[position];
                Log.d(TAG, "Selected country: " + selected);

                if (selected.equals("Vietnam")) {
                    loadVietnamNews();
                } else {
                    loadNewsByQuery(selected, null);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_news);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_weather) {
                Intent intent = new Intent(NewsActivity.this, HomeActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_news) {
                return true;
            }
            return false;
        });
    }

    private void setupBackButton() {
        // Nếu có back button trong layout
        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(NewsActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadVietnamNews() {
        if (isLoading) return;

        Log.d(TAG, "Loading Vietnam news...");
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        NewsService.getVietnamNews(new NewsService.NewsCallback() {
            @Override
            public void onSuccess(List<News> newsResponse) {
                Log.d(TAG, "Successfully loaded " + newsResponse.size() + " news items");
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                newsList.clear();

                // Sắp xếp tin tức theo ngày mới nhất đến cũ nhất
                sortNewsByDate(newsResponse);

                newsList.addAll(newsResponse);
                newsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading Vietnam news: " + error);
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NewsActivity.this, "Error loading news: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadNewsByQuery(String query, String language) {
        if (isLoading) return;

        Log.d(TAG, "Loading news by query: " + query);
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        NewsService.getNewsByQuery(query, language, new NewsService.NewsCallback() {
            @Override
            public void onSuccess(List<News> newsResponse) {
                Log.d(TAG, "Successfully loaded " + newsResponse.size() + " news items");
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                newsList.clear();

                // Sắp xếp tin tức theo ngày mới nhất đến cũ nhất
                sortNewsByDate(newsResponse);

                newsList.addAll(newsResponse);
                newsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading news by query: " + error);
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NewsActivity.this, "Error loading news: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sắp xếp danh sách tin tức theo ngày mới nhất đến cũ nhất
     */
    private void sortNewsByDate(List<News> newsResponse) {
        Collections.sort(newsResponse, (news1, news2) -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);

                Date date1 = dateFormat.parse(news1.getPublishedAt());
                Date date2 = dateFormat.parse(news2.getPublishedAt());

                // Sắp xếp giảm dần (mới nhất trước)
                return date2.compareTo(date1);

            } catch (Exception e) {
                Log.e(TAG, "Error parsing date for sorting: " + e.getMessage());
                // Nếu không parse được date, tin tức nào có lỗi sẽ được đặt cuối
                if (news1.getPublishedAt() == null || news1.getPublishedAt().isEmpty()) {
                    return 1;
                }
                if (news2.getPublishedAt() == null || news2.getPublishedAt().isEmpty()) {
                    return -1;
                }
                // Nếu cả hai đều có lỗi, so sánh theo string
                return news2.getPublishedAt().compareTo(news1.getPublishedAt());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_news);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(NewsActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}