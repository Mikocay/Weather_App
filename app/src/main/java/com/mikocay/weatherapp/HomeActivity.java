package com.mikocay.weatherapp;

import static com.mikocay.weatherapp.location.CityFinder.getCityNameUsingNetwork;
import static com.mikocay.weatherapp.location.CityFinder.setLongitudeLatitude;
import static com.mikocay.weatherapp.network.InternetConnectivity.isInternetConnected;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mikocay.weatherapp.adapter.DaysAdapter;
import com.mikocay.weatherapp.databinding.ActivityHomeBinding;
import com.mikocay.weatherapp.auth.LoginActivity;
import com.mikocay.weatherapp.location.LocationCord;
import com.mikocay.weatherapp.toast.Toaster;
import com.mikocay.weatherapp.update.UpdateUI;
import com.mikocay.weatherapp.url.URL;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

public class HomeActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private final int WEATHER_FORECAST_APP_UPDATE_REQ_CODE = 101;
    private static final int PERMISSION_CODE = 1;
    private String name, updated_at, description, temperature, min_temperature, max_temperature, pressure, wind_speed, humidity;
    private int condition;
    private long update_time, sunset, sunrise;
    private String city = "";
    private final int REQUEST_CODE_EXTRA_INPUT = 101;
    private ActivityHomeBinding binding;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // binding
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // set navigation bar color
        setNavigationBarColor();

        //check for new app update
        checkUpdate();

        // set refresh color schemes
        setRefreshLayoutColor();

        // when user do search and refresh
        listeners();

        // getting data using internet connection
        getDataUsingNetwork();

        // setup bottom navigation only (removed setupNewsButton)
        setupBottomNavigation();

        // Thiết lập nút logout
        binding.layout.logoutButton.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn muốn đăng xuất khỏi tài khoản?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> logout())
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    // Removed setupNewsButton() method completely as news_button no longer exists

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_weather);

            bottomNavigationView.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_weather) {
                    return true;
                } else if (item.getItemId() == R.id.nav_news) {
                    Intent intent = new Intent(HomeActivity.this, NewsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnection();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_weather);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EXTRA_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> arrayList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                binding.layout.cityEt.setText(Objects.requireNonNull(arrayList).get(0).toUpperCase());
                searchCity(binding.layout.cityEt.getText().toString());
            }
        }
    }

    private void setNavigationBarColor() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.navBarColor));
        }
    }

    private void setUpDaysRecyclerView() {
        DaysAdapter daysAdapter = new DaysAdapter(this);
        binding.dayRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        binding.dayRv.setAdapter(daysAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void listeners() {
        binding.layout.mainLayout.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard(view);
            return false;
        });
        binding.layout.searchBarIv.setOnClickListener(view -> searchCity(binding.layout.cityEt.getText().toString()));
        binding.layout.searchBarIv.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard(view);
            return false;
        });
        binding.layout.cityEt.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                searchCity(binding.layout.cityEt.getText().toString());
                hideKeyboard(textView);
                return true;
            }
            return false;
        });
        binding.layout.cityEt.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                hideKeyboard(view);
            }
        });
        binding.mainRefreshLayout.setOnRefreshListener(() -> {
            checkConnection();
            Log.i("refresh", "Refresh Done.");
            binding.mainRefreshLayout.setRefreshing(false);
        });
        //Mic Search
        binding.layout.micSearchId.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, REQUEST_CODE_EXTRA_INPUT);
            try {
                startActivityForResult(intent, REQUEST_CODE_EXTRA_INPUT);
            } catch (Exception e) {
                Log.d("Error Voice", "Mic Error:  " + e);
            }
        });
    }

    private void setRefreshLayoutColor() {
        binding.mainRefreshLayout.setProgressBackgroundColorSchemeColor(
                getResources().getColor(R.color.textColor)
        );
        binding.mainRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.navBarColor)
        );
    }

    private void searchCity(String cityName) {
        if (cityName == null || cityName.isEmpty()) {
            Toaster.errorToast(this, "Please enter the city name");
        } else {
            setLatitudeLongitudeUsingCity(cityName);
        }
    }

    private void getDataUsingNetwork() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        //check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            client.getLastLocation().addOnSuccessListener(location -> {
                setLongitudeLatitude(location);
                city = getCityNameUsingNetwork(this, location);
                getTodayWeatherInfo(city);
            });
        }
    }

    private void setLatitudeLongitudeUsingCity(String cityName) {
        URL.setCity_url(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(HomeActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL.getCity_url(), null, response -> {
            try {
                LocationCord.lat = response.getJSONObject("coord").getString("lat");
                LocationCord.lon = response.getJSONObject("coord").getString("lon");
                getTodayWeatherInfo(cityName);
                binding.layout.cityEt.setText("");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> Toaster.errorToast(this, "Please enter the correct city name"));
        requestQueue.add(jsonObjectRequest);
    }

    @SuppressLint("DefaultLocale")
    private void getTodayWeatherInfo(String name) {
        URL url = new URL();
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url.getLink(), null, response -> {
            try {
                this.name = name;

                // Lấy timezone của thành phố
                String cityTimezone = getCityTimezone(name);

                // Sử dụng thời gian hiện tại
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE HH:mm", Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone(cityTimezone));

                Date currentTime = new Date();
                updated_at = sdf.format(currentTime);

                // Lấy thông tin thời tiết
                condition = response.getJSONArray("weather").getJSONObject(0).getInt("id");
                sunrise = response.getJSONObject("sys").getLong("sunrise");
                sunset = response.getJSONObject("sys").getLong("sunset");
                description = response.getJSONArray("weather").getJSONObject(0).getString("main");

                temperature = String.valueOf(Math.round(response.getJSONObject("main").getDouble("temp")));
                min_temperature = String.format("%.0f", response.getJSONObject("main").getDouble("temp_min"));
                max_temperature = String.format("%.0f", response.getJSONObject("main").getDouble("temp_max"));
                pressure = String.valueOf(response.getJSONObject("main").getInt("pressure"));
                wind_speed = String.format("%.1f", response.getJSONObject("wind").getDouble("speed"));
                humidity = String.valueOf(response.getJSONObject("main").getInt("humidity"));

                Log.d("TIME_DEBUG", "City: " + name);
                Log.d("TIME_DEBUG", "Timezone: " + cityTimezone);
                Log.d("TIME_DEBUG", "Current time: " + updated_at);

                updateUI();
                hideProgressBar();
                setUpDaysRecyclerView();

            } catch (JSONException e) {
                e.printStackTrace();
                Toaster.errorToast(this, "Error parsing weather data");
                hideProgressBar();
            }
        }, error -> {
            Toaster.errorToast(this, "Failed to fetch weather data");
            hideProgressBar();
        });

        requestQueue.add(jsonObjectRequest);
    }

    private String getCityTimezone(String cityName) {
        Map<String, String> cityTimezones = new HashMap<>();

        // Việt Nam
        cityTimezones.put("HANOI", "Asia/Ho_Chi_Minh");
        cityTimezones.put("HO CHI MINH", "Asia/Ho_Chi_Minh");
        cityTimezones.put("SAIGON", "Asia/Ho_Chi_Minh");
        cityTimezones.put("DA NANG", "Asia/Ho_Chi_Minh");

        // Các thành phố khác
        cityTimezones.put("NEW YORK", "America/New_York");
        cityTimezones.put("LONDON", "Europe/London");
        cityTimezones.put("TOKYO", "Asia/Tokyo");
        cityTimezones.put("PARIS", "Europe/Paris");
        cityTimezones.put("SINGAPORE", "Asia/Singapore");
        cityTimezones.put("BANGKOK", "Asia/Bangkok");
        cityTimezones.put("JAKARTA", "Asia/Jakarta");

        String upperCityName = cityName.toUpperCase();
        for (Map.Entry<String, String> entry : cityTimezones.entrySet()) {
            if (upperCityName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "GMT";
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        binding.layout.nameTv.setText(name);

        String translatedTime = translate(updated_at);
        binding.layout.updatedAtTv.setText(translatedTime);

        binding.layout.conditionIv.setImageResource(
                getResources().getIdentifier(
                        UpdateUI.getIconID(condition, System.currentTimeMillis() / 1000, sunrise, sunset),
                        "drawable",
                        getPackageName()
                ));

        binding.layout.conditionDescTv.setText(description);
        binding.layout.tempTv.setText(temperature + "°C");
        binding.layout.minTempTv.setText(min_temperature + "°C");
        binding.layout.maxTempTv.setText(max_temperature + "°C");
        binding.layout.pressureTv.setText(pressure + " mb");
        binding.layout.windTv.setText(wind_speed + " km/h");
        binding.layout.humidityTv.setText(humidity + "%");
    }

    private String translate(String dayToTranslate) {
        try {
            String[] dayToTranslateSplit = dayToTranslate.split(" ");
            if (dayToTranslateSplit.length >= 2) {
                dayToTranslateSplit[0] = UpdateUI.TranslateDay(dayToTranslateSplit[0].trim(), getApplicationContext());
                return dayToTranslateSplit[0] + " " + dayToTranslateSplit[1];
            } else {
                return dayToTranslate;
            }
        } catch (Exception e) {
            Log.e("TRANSLATE_ERROR", "Error translating time: " + e.getMessage());
            return dayToTranslate;
        }
    }

    private void hideProgressBar() {
        binding.progress.setVisibility(View.GONE);
        binding.layout.mainLayout.setVisibility(View.VISIBLE);
    }

    private void hideMainLayout() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.layout.mainLayout.setVisibility(View.GONE);
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void checkConnection() {
        if (!isInternetConnected(this)) {
            hideMainLayout();
            Toaster.errorToast(this, "Please check your internet connection");
        } else {
            hideProgressBar();
            getDataUsingNetwork();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toaster.successToast(this, "Permission Granted");
                getDataUsingNetwork();
            } else {
                Toaster.errorToast(this, "Permission Denied");
                finish();
            }
        }
    }

    private void checkUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(HomeActivity.this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, HomeActivity.this, WEATHER_FORECAST_APP_UPDATE_REQ_CODE);
                } catch (IntentSender.SendIntentException exception) {
                    Toaster.errorToast(this, "Update Failed");
                }
            }
        });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}