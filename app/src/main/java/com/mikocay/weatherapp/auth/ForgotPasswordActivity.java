package com.mikocay.weatherapp.auth;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mikocay.weatherapp.databinding.ActivityForgotPasswordBinding;
import com.mikocay.weatherapp.toast.Toaster;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.backButton.setOnClickListener(v -> finish());
        binding.resetButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = binding.emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.emailInput.setError("Email không được để trống");
            binding.emailInput.requestFocus();
            return;
        }

        showProgressBar();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideProgressBar();
                    if (task.isSuccessful()) {
                        Toaster.successToast(this, "Link đặt lại mật khẩu đã được gửi đến email của bạn");
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Gửi email thất bại";
                        Toaster.errorToast(this, errorMessage);
                    }
                });
    }

    private void showProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.forgotForm.setVisibility(View.GONE);
    }

    private void hideProgressBar() {
        binding.progressBar.setVisibility(View.GONE);
        binding.forgotForm.setVisibility(View.VISIBLE);
    }
}