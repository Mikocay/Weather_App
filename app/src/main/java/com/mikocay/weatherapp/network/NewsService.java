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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class NewsService {
    private static final String TAG = "NewsService";
    private static final String API_KEY = "8c339bb9125140c5bbad1ceea07ab8e4";
    private static final String BASE_URL = "https://newsapi.org/v2/everything";

    public interface NewsCallback {
        void onSuccess(List<News> newsList);
        void onError(String error);
    }

    public static void getVietnamNews(NewsCallback callback) {
        new GetNewsTask("Vietnam", "vi", callback).execute();
    }

    public static void getNewsByQuery(String query, String language, NewsCallback callback) {
        new GetNewsTask(query, language, callback).execute();
    }

    public static void getNewsByCountry(String country, NewsCallback callback) {
        String query = convertCountryToQuery(country);
        new GetNewsTask(query, null, callback).execute();
    }

    private static String convertCountryToQuery(String country) {
        switch (country.toLowerCase()) {
            case "vn":
            case "vietnam":
                return "Vietnam";
            case "us":
                return "United States";
            case "gb":
                return "United Kingdom";
            case "jp":
                return "Japan";
            case "kr":
                return "South Korea";
            default:
                return country;
        }
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
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                Log.d(TAG, "Starting API request for query: " + query);

                // Tạo URL với proper encoding
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(BASE_URL)
                        .append("?q=").append(URLEncoder.encode(query, "UTF-8"));

                if (language != null && !language.isEmpty()) {
                    urlBuilder.append("&language=").append(language);
                }

                urlBuilder.append("&apiKey=").append(API_KEY);

                String urlString = urlBuilder.toString();
                Log.d(TAG, "Requesting URL: " + urlString);

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();

                // Cấu hình connection
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "WeatherApp/1.0");
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(30000); // Tăng timeout lên 30s
                connection.setReadTimeout(30000);
                connection.setDoInput(true);

                Log.d(TAG, "Connecting to API...");
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);
                Log.d(TAG, "Response Message: " + connection.getResponseMessage());

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Successfully connected, reading response...");

                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    Log.d(TAG, "Response length: " + response.length());
                    Log.d(TAG, "Response preview: " + response.toString().substring(0,
                            Math.min(200, response.toString().length())));

                    return response.toString();

                } else {
                    // Đọc error response
                    Log.e(TAG, "HTTP Error: " + responseCode);
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    Log.e(TAG, "Error response: " + errorResponse.toString());
                    errorMessage = "HTTP Error: " + responseCode + " - " + errorResponse.toString();
                    return null;
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception occurred: " + e.getClass().getSimpleName(), e);
                Log.e(TAG, "Exception message: " + e.getMessage());
                errorMessage = "Network error: " + e.getMessage();
                return null;
            } finally {
                // Cleanup
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
                Log.d(TAG, "Connection cleanup completed");
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "onPostExecute called with result: " + (result != null ? "not null" : "null"));

            if (result != null) {
                try {
                    List<News> newsList = parseNewsResponse(result);
                    Log.d(TAG, "Parsed " + newsList.size() + " news items");
                    callback.onSuccess(newsList);
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error", e);
                    callback.onError("Parsing error: " + e.getMessage());
                }
            } else {
                String error = errorMessage != null ? errorMessage : "Unknown error";
                Log.e(TAG, "Calling onError with: " + error);
                callback.onError(error);
            }
        }

        private List<News> parseNewsResponse(String response) throws JSONException {
            List<News> newsList = new ArrayList<>();
            JSONObject jsonResponse = new JSONObject(response);

            if (jsonResponse.has("status") && "error".equals(jsonResponse.getString("status"))) {
                throw new JSONException("API Error: " + jsonResponse.optString("message"));
            }

            if (!jsonResponse.has("articles")) {
                throw new JSONException("No articles found in response");
            }

            JSONArray articles = jsonResponse.getJSONArray("articles");
            Log.d(TAG, "Found " + articles.length() + " articles");

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