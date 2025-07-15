package com.mikocay.weatherapp;

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
import com.mikocay.weatherapp.adapter.NewsAdapter;
import com.mikocay.weatherapp.model.News;
import com.mikocay.weatherapp.network.NewsService;
import java.util.ArrayList;
import java.util.List;

public class NewsActivity extends AppCompatActivity {
    private static final String TAG = "NewsActivity";
    private RecyclerView recyclerView;
    private NewsAdapter newsAdapter;
    private ProgressBar progressBar;
    private Spinner countrySpinner;
    private List<News> newsList = new ArrayList<>();
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        initViews();
        setupRecyclerView();
        setupCountrySpinner();
        setupBackButton();

        // Delay initial load để đảm bảo UI đã setup xong
        recyclerView.post(() -> loadVietnamNews());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.news_recycler_view);
        progressBar = findViewById(R.id.news_progress_bar);
        countrySpinner = findViewById(R.id.country_spinner);
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

    private void setupBackButton() {
        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
}