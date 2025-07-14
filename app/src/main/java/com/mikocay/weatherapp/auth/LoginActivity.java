package com.mikocay.weatherapp.auth;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mikocay.weatherapp.HomeActivity;
import com.mikocay.weatherapp.databinding.ActivityLoginBinding;
import com.mikocay.weatherapp.toast.Toaster;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToHome();
        }
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.signupLink.setOnClickListener(v -> navigateToSignup());
        binding.forgotPassword.setOnClickListener(v -> navigateToForgotPassword());
    }

    private void loginUser() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.emailInput.setError("Email không được để trống");
            binding.emailInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordInput.setError("Mật khẩu không được để trống");
            binding.passwordInput.requestFocus();
            return;
        }

        showProgressBar();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    hideProgressBar();
                    if (task.isSuccessful()) {
                        Toaster.successToast(this, "Đăng nhập thành công");
                        navigateToHome();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Đăng nhập thất bại";
                        Toaster.errorToast(this, errorMessage);
                    }
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignup() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    private void navigateToForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    private void showProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.loginForm.setVisibility(View.GONE);
    }

    private void hideProgressBar() {
        binding.progressBar.setVisibility(View.GONE);
        binding.loginForm.setVisibility(View.VISIBLE);
    }
}