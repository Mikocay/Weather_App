package com.mikocay.weatherapp.location;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import java.util.List;
import java.util.Locale;

public class CityFinder {

    public static void setLongitudeLatitude(Location location) {
        try {
            if (location != null) {
                LocationCord.lat = String.valueOf(location.getLatitude());
                LocationCord.lon = String.valueOf(location.getLongitude());
                Log.d("location_lat", LocationCord.lat);
                Log.d("location_lon", LocationCord.lon);
            } else {
                Log.e("CityFinder", "Location is null, using default location");
                // Set default location (New York as example)
                LocationCord.lat = "40.7128";
                LocationCord.lon = "-74.0060";
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e("CityFinder", "Error setting location, using default");
            // Set default location in case of error
            LocationCord.lat = "40.7128";
            LocationCord.lon = "-74.0060";
        }
    }

    public static String getCityNameUsingNetwork(Context context, Location location) {
        String city = "";
        try {
            if (location != null) {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    city = addresses.get(0).getLocality();
                    if (city == null || city.isEmpty()) {
                        city = addresses.get(0).getAdminArea(); // Try to get state/region
                    }
                }
                Log.d("city", city);
            } else {
                Log.e("CityFinder", "Location is null, using default city");
                city = "New York"; // Default city
            }
        } catch (Exception e) {
            Log.d("city", "Error to find the city: " + e.getMessage());
            city = "New York"; // Default city on error
        }
        return city;
    }
}