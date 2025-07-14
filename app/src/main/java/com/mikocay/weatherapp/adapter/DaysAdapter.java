package com.mikocay.weatherapp.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mikocay.weatherapp.R;
import com.mikocay.weatherapp.location.LocationCord;
import com.mikocay.weatherapp.update.UpdateUI;
import com.github.ybq.android.spinkit.SpinKitView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DayViewHolder> {
    private final Context context;
    private List<WeatherDay> weatherDays;
    private boolean dataLoaded = false;
    private long citySunrise, citySunset;

    public DaysAdapter(Context context) {
        this.context = context;
        this.weatherDays = new ArrayList<>();
        loadForecastData();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.day_item_layout, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        if (dataLoaded && position < weatherDays.size()) {
            WeatherDay day = weatherDays.get(position);
            bindWeatherData(holder, day);
        } else {
            // Hiển thị loading
            holder.progress.setVisibility(View.VISIBLE);
            holder.layout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return Math.min(weatherDays.size(), 5); // Tối đa 5 ngày
    }

    private void loadForecastData() {
        String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?lat="
                + LocationCord.lat + "&lon=" + LocationCord.lon
                + "&appid=" + LocationCord.API_KEY + "&units=metric";

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, forecastUrl, null, response -> {
            try {
                processForecastData(response);
                dataLoaded = true;
                notifyDataSetChanged();
                Log.i("FORECAST", "Forecast data loaded successfully, items: " + weatherDays.size());
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("FORECAST", "Error parsing forecast data: " + e.getMessage());
            }
        }, error -> {
            Log.e("FORECAST", "API Error: " + error.getMessage());
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void processForecastData(JSONObject response) throws JSONException {
        JSONArray list = response.getJSONArray("list");

        // Lấy thông tin city để có sunrise/sunset
        JSONObject city = response.getJSONObject("city");
        citySunrise = city.getLong("sunrise");
        citySunset = city.getLong("sunset");

        Map<String, WeatherDay> dailyData = new HashMap<>();

        // Group data by date - lấy dữ liệu trưa (12:00) hoặc gần nhất
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            long timestamp = item.getLong("dt");
            Date date = new Date(timestamp * 1000);

            // Format ngày để group
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            String dayKey = dayFormat.format(date);

            // Ưu tiên lấy dữ liệu lúc 12:00 (trưa) hoặc 15:00
            String timeStr = item.getString("dt_txt");
            boolean isPreferredTime = timeStr.contains("12:00:00") || timeStr.contains("15:00:00");

            if (isPreferredTime || !dailyData.containsKey(dayKey)) {
                WeatherDay weatherDay = new WeatherDay();
                weatherDay.date = date;
                weatherDay.dayName = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date);
                weatherDay.condition = item.getJSONArray("weather").getJSONObject(0).getInt("id");

                // Lấy nhiệt độ từ main object
                JSONObject main = item.getJSONObject("main");
                weatherDay.minTemp = String.format("%.0f", main.getDouble("temp_min"));
                weatherDay.maxTemp = String.format("%.0f", main.getDouble("temp_max"));
                weatherDay.currentTemp = String.format("%.0f", main.getDouble("temp"));
                weatherDay.pressure = String.valueOf(main.getInt("pressure"));
                weatherDay.humidity = String.valueOf(main.getInt("humidity"));

                // Wind speed
                if (item.has("wind")) {
                    weatherDay.windSpeed = String.format("%.1f", item.getJSONObject("wind").getDouble("speed"));
                } else {
                    weatherDay.windSpeed = "0";
                }

                weatherDay.timestamp = timestamp;
                weatherDay.sunrise = citySunrise;
                weatherDay.sunset = citySunset;

                // Chỉ overwrite nếu là preferred time
                if (isPreferredTime || !dailyData.containsKey(dayKey)) {
                    dailyData.put(dayKey, weatherDay);
                }
            }
        }

        // Convert to list và sort theo ngày
        weatherDays.clear();
        weatherDays.addAll(dailyData.values());
        weatherDays.sort((a, b) -> a.date.compareTo(b.date));

        // Tính toán lại min/max temp cho mỗi ngày từ tất cả records
        calculateDailyMinMax(list);
    }

    private void calculateDailyMinMax(JSONArray list) throws JSONException {
        Map<String, List<Double>> dailyTemps = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        // Collect all temperatures for each day
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            long timestamp = item.getLong("dt");
            Date date = new Date(timestamp * 1000);
            String dayKey = dayFormat.format(date);

            double temp = item.getJSONObject("main").getDouble("temp");

            if (!dailyTemps.containsKey(dayKey)) {
                dailyTemps.put(dayKey, new ArrayList<>());
            }
            dailyTemps.get(dayKey).add(temp);
        }

        // Update min/max for each day
        for (WeatherDay day : weatherDays) {
            String dayKey = dayFormat.format(day.date);
            if (dailyTemps.containsKey(dayKey)) {
                List<Double> temps = dailyTemps.get(dayKey);
                double minTemp = temps.stream().min(Double::compareTo).orElse(0.0);
                double maxTemp = temps.stream().max(Double::compareTo).orElse(0.0);

                day.minTemp = String.format("%.0f", minTemp);
                day.maxTemp = String.format("%.0f", maxTemp);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void bindWeatherData(DayViewHolder holder, WeatherDay day) {
        String translatedDay = UpdateUI.TranslateDay(day.dayName, context);
        holder.dTime.setText(translatedDay);
        holder.temp_min.setText(day.minTemp + "°C");
        holder.temp_max.setText(day.maxTemp + "°C");
        holder.pressure.setText(day.pressure + " mb");
        holder.wind.setText(day.windSpeed + " km/h");
        holder.humidity.setText(day.humidity + "%");

        holder.icon.setImageResource(
                context.getResources().getIdentifier(
                        UpdateUI.getIconID(day.condition, day.timestamp, day.sunrise, day.sunset),
                        "drawable",
                        context.getPackageName()
                ));

        // Hiển thị data và ẩn progress
        holder.progress.setVisibility(View.GONE);
        holder.layout.setVisibility(View.VISIBLE);
    }

    // Weather data class
    private static class WeatherDay {
        Date date;
        String dayName;
        int condition;
        String minTemp;
        String maxTemp;
        String currentTemp;
        String pressure;
        String windSpeed;
        String humidity;
        long timestamp;
        long sunrise;
        long sunset;
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        SpinKitView progress;
        RelativeLayout layout;
        TextView dTime, temp_min, temp_max, pressure, wind, humidity;
        ImageView icon;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            progress = itemView.findViewById(R.id.day_progress_bar);
            layout = itemView.findViewById(R.id.day_relative_layout);
            dTime = itemView.findViewById(R.id.day_time);
            temp_min = itemView.findViewById(R.id.day_min_temp);
            temp_max = itemView.findViewById(R.id.day_max_temp);
            pressure = itemView.findViewById(R.id.day_pressure);
            wind = itemView.findViewById(R.id.day_wind);
            humidity = itemView.findViewById(R.id.day_humidity);
            icon = itemView.findViewById(R.id.day_icon);
        }
    }
}