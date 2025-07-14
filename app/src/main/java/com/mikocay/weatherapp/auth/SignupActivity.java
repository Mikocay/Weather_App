package com.mikocay.weatherapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mikocay.weatherapp.HomeActivity;
import com.mikocay.weatherapp.databinding.ActivitySignupBinding;
import com.mikocay.weatherapp.toast.Toaster;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.signupButton.setOnClickListener(v -> signupUser());
        binding.loginLink.setOnClickListener(v -> finish());
    }

    private void signupUser() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.nameInput.setError("Tên không được để trống");
            binding.nameInput.requestFocus();
            return;
        }

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

        if (password.length() < 6) {
            binding.passwordInput.setError("Mật khẩu phải có ít nhất 6 ký tự");
            binding.passwordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordInput.setError("Mật khẩu xác nhận không khớp");
            binding.confirmPasswordInput.requestFocus();
            return;
        }

        showProgressBar();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        hideProgressBar();
                                        if (profileTask.isSuccessful()) {
                                            Toaster.successToast(this, "Đăng ký thành công");
                                            navigateToHome();
                                        } else {
                                            Toaster.errorToast(this, "Cập nhật thông tin thất bại");
                                        }
                                    });
                        }
                    } else {
                        hideProgressBar();
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Đăng ký thất bại";
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

    private void showProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.signupForm.setVisibility(View.GONE);
    }

    private void hideProgressBar() {
        binding.progressBar.setVisibility(View.GONE);
        binding.signupForm.setVisibility(View.VISIBLE);
    }
}