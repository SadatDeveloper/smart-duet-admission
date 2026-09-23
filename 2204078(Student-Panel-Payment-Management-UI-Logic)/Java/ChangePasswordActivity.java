package com.example.smartduetadmissionsystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private BottomNavigationView bottomNavigation;

    private TextInputEditText etCurrentPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;

    private MaterialButton btnChangePassword;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupHeader();
        setupProfile();
        setupBottomNavigation();
        setupDrawer();

        btnChangePassword.setOnClickListener(v -> changePassword());
    }


    private void initializeViews() {

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        etCurrentPassword =
                findViewById(R.id.etCurrentPassword);

        etNewPassword =
                findViewById(R.id.etNewPassword);

        etConfirmPassword =
                findViewById(R.id.etConfirmPassword);

        btnChangePassword =
                findViewById(R.id.btnChangePassword);
    }


    private void setupHeader() {

        btnMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );
    }


    private void setupProfile() {

        btnProfile.setOnClickListener(v -> {

            startActivity(
                    new android.content.Intent(
                            ChangePasswordActivity.this,
                            ProfileActivity.class
                    )
            );
        });
    }


    private void setupBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {

                startActivity(
                        new android.content.Intent(
                                ChangePasswordActivity.this,
                                MainActivity.class
                        )
                );

                return true;

            } else if (id == R.id.nav_notice) {

                startActivity(
                        new android.content.Intent(
                                ChangePasswordActivity.this,
                                NoticeActivity.class
                        )
                );

                return true;

            } else if (id == R.id.nav_profile) {

                startActivity(
                        new android.content.Intent(
                                ChangePasswordActivity.this,
                                ProfileActivity.class
                        )
                );

                return true;
            }

            return false;
        });
    }


    private void setupDrawer() {

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            drawerLayout.closeDrawer(navigationView);

            if (id == R.id.nav_dashboard) {
                openActivity(MainActivity.class);

            } else if (id == R.id.nav_eligibility) {
                openActivity(EligibilityActivity.class);

            } else if (id == R.id.nav_notice) {
                openActivity(NoticeActivity.class);

            } else if (id == R.id.nav_department) {
                openActivity(DepartmentsActivity.class);

            } else if (id == R.id.nav_apply) {
                openActivity(AdmissionApplicationActivity.class);

            } else if (id == R.id.nav_tracker) {
                openActivity(ApplicationTrackerActivity.class);

            } else if (id == R.id.nav_admit_card) {
                openActivity(AdmitCardActivity.class);

            } else if (id == R.id.nav_result) {
                openActivity(ResultActivity.class);

            } else if (id == R.id.nav_syllabus) {
                openActivity(SyllabusActivity.class);
            }

            return true;
        });
    }


    private void openActivity(Class<?> activityClass) {

        startActivity(
                new android.content.Intent(
                        ChangePasswordActivity.this,
                        activityClass
                )
        );
    }


    private void changePassword() {

        String currentPassword =
                etCurrentPassword.getText()
                        .toString()
                        .trim();

        String newPassword =
                etNewPassword.getText()
                        .toString()
                        .trim();

        String confirmPassword =
                etConfirmPassword.getText()
                        .toString()
                        .trim();


        if (TextUtils.isEmpty(currentPassword)) {

            etCurrentPassword.setError(
                    "Enter your current password"
            );

            etCurrentPassword.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(newPassword)) {

            etNewPassword.setError(
                    "Enter your new password"
            );

            etNewPassword.requestFocus();

            return;
        }


        if (newPassword.length() < 6) {

            etNewPassword.setError(
                    "Password must be at least 6 characters"
            );

            etNewPassword.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(confirmPassword)) {

            etConfirmPassword.setError(
                    "Confirm your new password"
            );

            etConfirmPassword.requestFocus();

            return;
        }


        if (!newPassword.equals(confirmPassword)) {

            etConfirmPassword.setError(
                    "Passwords do not match"
            );

            etConfirmPassword.requestFocus();

            return;
        }


        if (currentPassword.equals(newPassword)) {

            etNewPassword.setError(
                    "New password must be different"
            );

            etNewPassword.requestFocus();

            return;
        }


        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null ||
                user.getEmail() == null) {

            Toast.makeText(
                    this,
                    "User session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        btnChangePassword.setEnabled(false);

        btnChangePassword.setText("Updating...");


        /*
         * Re-authenticate first because Firebase requires
         * recent authentication for sensitive operations.
         */

        user.reauthenticate(
                EmailAuthProvider.getCredential(
                        user.getEmail(),
                        currentPassword
                )
        ).addOnSuccessListener(unused -> {

            user.updatePassword(newPassword)
                    .addOnSuccessListener(unused2 -> {

                        btnChangePassword.setEnabled(true);

                        btnChangePassword.setText(
                                "Change Password"
                        );

                        Toast.makeText(
                                ChangePasswordActivity.this,
                                "Password changed successfully",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    })
                    .addOnFailureListener(e -> {

                        btnChangePassword.setEnabled(true);

                        btnChangePassword.setText(
                                "Change Password"
                        );

                        Toast.makeText(
                                ChangePasswordActivity.this,
                                "Failed to change password: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });

        }).addOnFailureListener(e -> {

            btnChangePassword.setEnabled(true);

            btnChangePassword.setText(
                    "Change Password"
            );

            Toast.makeText(
                    ChangePasswordActivity.this,
                    "Current password is incorrect",
                    Toast.LENGTH_LONG
            ).show();
        });
    }


    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(navigationView)) {

            drawerLayout.closeDrawer(navigationView);

        } else {

            super.onBackPressed();
        }
    }
}