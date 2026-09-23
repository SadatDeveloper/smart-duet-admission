package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountSettingsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private BottomNavigationView bottomNavigation;

    private LinearLayout layoutChangePassword;
    private LinearLayout layoutEmail;
    private LinearLayout layoutMobile;
    private LinearLayout layoutSecurity;

    private MaterialCardView layoutDeleteAccount;

    private TextView tvEmail;
    private TextView tvMobile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupHeader();
        setupProfile();
        setupBottomNavigation();
        setupDrawer();
        setupSettings();
        loadAccountInformation();
    }


    private void initializeViews() {

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        layoutChangePassword =
                findViewById(R.id.layoutChangePassword);

        layoutEmail =
                findViewById(R.id.layoutEmail);

        layoutMobile =
                findViewById(R.id.layoutMobile);

        layoutSecurity =
                findViewById(R.id.layoutSecurity);

        layoutDeleteAccount =
                findViewById(R.id.layoutDeleteAccount);

        tvEmail =
                findViewById(R.id.tvEmail);

        tvMobile =
                findViewById(R.id.tvMobile);
    }


    private void setupHeader() {

        btnMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );
    }


    private void setupProfile() {

        btnProfile.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            AccountSettingsActivity.this,
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
                        new Intent(
                                AccountSettingsActivity.this,
                                MainActivity.class
                        )
                );

                return true;

            } else if (id == R.id.nav_notice) {

                startActivity(
                        new Intent(
                                AccountSettingsActivity.this,
                                NoticeActivity.class
                        )
                );

                return true;

            } else if (id == R.id.nav_profile) {

                startActivity(
                        new Intent(
                                AccountSettingsActivity.this,
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
                new Intent(
                        AccountSettingsActivity.this,
                        activityClass
                )
        );
    }


    private void setupSettings() {

        // ==========================================
        // CHANGE PASSWORD
        // ==========================================

        layoutChangePassword.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            AccountSettingsActivity.this,
                            ChangePasswordActivity.class
                    )
            );
        });


        // ==========================================
        // EMAIL
        // ==========================================

        layoutEmail.setOnClickListener(v -> {

            Toast.makeText(
                    AccountSettingsActivity.this,
                    "Registered email address",
                    Toast.LENGTH_SHORT
            ).show();
        });


        // ==========================================
        // MOBILE NUMBER
        // ==========================================

        layoutMobile.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            AccountSettingsActivity.this,
                            EditProfileActivity.class
                    )
            );
        });


        // ==========================================
        // SECURITY
        // ==========================================

        layoutSecurity.setOnClickListener(v -> {

            FirebaseUser user =
                    mAuth.getCurrentUser();

            if (user != null) {

                Toast.makeText(
                        AccountSettingsActivity.this,
                        "Firebase Authentication is protecting your account",
                        Toast.LENGTH_LONG
                ).show();
            }
        });


        // ==========================================
        // DELETE ACCOUNT
        // ==========================================

        layoutDeleteAccount.setOnClickListener(v ->
                showDeleteConfirmation()
        );
    }


    // ==========================================
    // DELETE ACCOUNT CONFIRMATION
    // ==========================================

    private void showDeleteConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Delete Account?")
                .setMessage(
                        "This action will permanently delete your account. "
                                + "Your profile data will also be removed."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Continue",
                        (dialog, which) ->
                                showPasswordForDelete()
                )
                .show();
    }


    // ==========================================
    // CURRENT PASSWORD
    // ==========================================

    private void showPasswordForDelete() {

        EditText passwordInput =
                new EditText(this);

        passwordInput.setHint(
                "Enter current password"
        );

        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );


        int padding = (int)
                (24 * getResources()
                        .getDisplayMetrics()
                        .density);

        LinearLayout container =
                new LinearLayout(this);

        container.setPadding(
                padding,
                0,
                padding,
                0
        );

        container.addView(
                passwordInput,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );


        new AlertDialog.Builder(this)
                .setTitle("Confirm Account Deletion")
                .setMessage(
                        "For security, enter your current password."
                )
                .setView(container)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete Account",
                        (dialog, which) -> {

                            String password =
                                    passwordInput.getText()
                                            .toString()
                                            .trim();

                            if (TextUtils.isEmpty(password)) {

                                Toast.makeText(
                                        AccountSettingsActivity.this,
                                        "Password is required",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            deleteAccount(password);
                        }
                )
                .show();
    }


    // ==========================================
    // DELETE ACCOUNT
    // ==========================================

    private void deleteAccount(String password) {

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


        Toast.makeText(
                this,
                "Verifying account...",
                Toast.LENGTH_SHORT
        ).show();


        user.reauthenticate(
                EmailAuthProvider.getCredential(
                        user.getEmail(),
                        password
                )
        ).addOnSuccessListener(unused -> {

            String uid = user.getUid();


            // Delete Firestore profile first

            db.collection("users")
                    .document(uid)
                    .delete()
                    .addOnCompleteListener(task -> {

                        // Delete Firebase Authentication account
                        user.delete()
                                .addOnSuccessListener(unused2 -> {

                                    Toast.makeText(
                                            AccountSettingsActivity.this,
                                            "Account deleted successfully",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    Intent intent =
                                            new Intent(
                                                    AccountSettingsActivity.this,
                                                    LoginActivity.class
                                            );

                                    intent.setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);

                                    finish();
                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(
                                            AccountSettingsActivity.this,
                                            "Could not delete account: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });

                    });

        }).addOnFailureListener(e -> {

            Toast.makeText(
                    AccountSettingsActivity.this,
                    "Current password is incorrect",
                    Toast.LENGTH_LONG
            ).show();
        });
    }


    private void loadAccountInformation() {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {
            return;
        }


        if (user.getEmail() != null) {

            tvEmail.setText(
                    user.getEmail()
            );
        }


        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    String phone =
                            documentSnapshot.getString("phone");

                    if (phone != null &&
                            !phone.trim().isEmpty()) {

                        tvMobile.setText(phone);
                    }
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