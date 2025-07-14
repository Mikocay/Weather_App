package com.mikocay.weatherapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.mikocay.weatherapp.auth.LoginActivity;
import com.mikocay.weatherapp.databinding.ActivitySplashScreenBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashScreenBinding binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //Removing status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //Setting Splash
        splashScreen();
    }

    private void splashScreen() {
        int SPLASH_TIME = 4000;
        new Handler().postDelayed(() -> {
            // Kiểm tra trạng thái đăng nhập
            FirebaseUser currentUser = mAuth.getCurrentUser();
            Intent intent;

            if (currentUser != null) {
                // Đã đăng nhập - chuyển đến HomeActivity
                intent = new Intent(getApplicationContext(), HomeActivity.class);
            } else {
                // Chưa đăng nhập - chuyển đến LoginActivity
                intent = new Intent(getApplicationContext(), LoginActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_TIME);
    }
}