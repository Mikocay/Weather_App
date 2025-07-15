package com.mikocay.weatherapp;

import android.os.Bundle;
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
    private RecyclerView recyclerView;
    private NewsAdapter newsAdapter;
    private ProgressBar progressBar;
    private Spinner countrySpinner;
    private List<News> newsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        initViews();
        setupRecyclerView();
        setupCountrySpinner();
        loadNews("us"); // Load US news by default
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
        // Tạo danh sách các quốc gia
        String[] countries = {"us", "gb", "ca", "au", "de", "fr", "it", "jp", "kr", "vn"};
        String[] countryNames = {"United States", "United Kingdom", "Canada", "Australia",
                "Germany", "France", "Italy", "Japan", "South Korea", "Vietnam"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, countryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(adapter);

        countrySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                loadNews(countries[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadNews(String country) {
        progressBar.setVisibility(android.view.View.VISIBLE);

        NewsService.getNewsByCountry(country, new NewsService.NewsCallback() {
            @Override
            public void onSuccess(List<News> newsResponse) {
                progressBar.setVisibility(android.view.View.GONE);
                newsList.clear();
                newsList.addAll(newsResponse);
                newsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(NewsActivity.this, "Error loading news: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBackButton() {
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
}