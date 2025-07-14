package com.mikocay.weatherapp.url;

import com.mikocay.weatherapp.location.LocationCord;

public class URL {
    private String link;
    private static String city_url;
    private String forecastLink; // Thêm link cho forecast


    public URL() {
        // Current weather
        link = "https://api.openweathermap.org/data/2.5/weather?lat="
                + LocationCord.lat + "&lon=" + LocationCord.lon
                + "&appid=" + LocationCord.API_KEY + "&units=metric";

        // OneCall API cho forecast
        forecastLink = "https://api.openweathermap.org/data/2.5/onecall?lat="
                + LocationCord.lat + "&lon=" + LocationCord.lon
                + "&appid=" + LocationCord.API_KEY + "&units=metric&exclude=minutely,hourly,alerts";
    }

    public String getLink() {
        return link;
    }

    public String getForecastLink() {
        return forecastLink;
    }

    public static void setCity_url(String cityName) {
        city_url = "https://api.openweathermap.org/data/2.5/weather?&q=" + cityName
                + "&appid=" + LocationCord.API_KEY + "&units=metric";
    }

    public static String getCity_url() {
        return city_url;
    }
}