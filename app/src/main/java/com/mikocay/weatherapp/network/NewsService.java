package com.mikocay.weatherapp.network;

import android.os.AsyncTask;
import android.util.Log;
import com.mikocay.weatherapp.model.News;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NewsService {
    private static final String TAG = "NewsService";
    private static final String API_KEY = "8c339bb9125140c5bbad1ceea07ab8e4"; // Thay bằng API key từ newsapi.org
    private static final String BASE_URL = "https://newsapi.org/v2/everything";

    public interface NewsCallback {
        void onSuccess(List<News> newsList);
        void onError(String error);
    }

    // Method mới để tìm kiếm tin tức về Vietnam bằng tiếng Việt
    public static void getVietnamNews(NewsCallback callback) {
        new GetNewsTask("Vietnam", "vi", callback).execute();
    }

    // Method tổng quát để tìm kiếm tin tức
    public static void getNewsByQuery(String query, String language, NewsCallback callback) {
        new GetNewsTask(query, language, callback).execute();
    }

    // Giữ lại method cũ để tương thích ngược (nếu cần)
    public static void getNewsByCountry(String country, NewsCallback callback) {
        new GetNewsTask(country, null, callback).execute();
    }

    private static class GetNewsTask extends AsyncTask<Void, Void, String> {
        private String query;
        private String language;
        private NewsCallback callback;
        private String errorMessage;

        public GetNewsTask(String query, String language, NewsCallback callback) {
            this.query = query;
            this.language = language;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(BASE_URL)
                        .append("?q=").append(query)
                        .append("&apiKey=").append(API_KEY)
                        .append("&pageSize=20");

                // Thêm language parameter nếu có
                if (language != null && !language.isEmpty()) {
                    urlBuilder.append("&language=").append(language);
                }

                String urlString = urlBuilder.toString();
                Log.d(TAG, "Requesting URL: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "HTTP Error: " + responseCode;
                    return null;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();
                return response.toString();

            } catch (IOException e) {
                Log.e(TAG, "Network error", e);
                errorMessage = "Network error: " + e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    List<News> newsList = parseNewsResponse(result);
                    callback.onSuccess(newsList);
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error", e);
                    callback.onError("Parsing error: " + e.getMessage());
                }
            } else {
                callback.onError(errorMessage != null ? errorMessage : "Unknown error");
            }
        }

        private List<News> parseNewsResponse(String response) throws JSONException {
            List<News> newsList = new ArrayList<>();
            JSONObject jsonResponse = new JSONObject(response);

            if (jsonResponse.has("status") && "error".equals(jsonResponse.getString("status"))) {
                throw new JSONException("API Error: " + jsonResponse.optString("message"));
            }

            JSONArray articles = jsonResponse.getJSONArray("articles");

            for (int i = 0; i < articles.length(); i++) {
                JSONObject article = articles.getJSONObject(i);
                News news = new News();

                news.setTitle(article.optString("title", "No title"));
                news.setDescription(article.optString("description", "No description"));
                news.setUrl(article.optString("url", ""));
                news.setImageUrl(article.optString("urlToImage", ""));
                news.setPublishedAt(article.optString("publishedAt", ""));
                news.setAuthor(article.optString("author", "Unknown"));

                JSONObject source = article.optJSONObject("source");
                if (source != null) {
                    news.setSource(source.optString("name", "Unknown source"));
                }

                newsList.add(news);
            }

            return newsList;
        }
    }
}