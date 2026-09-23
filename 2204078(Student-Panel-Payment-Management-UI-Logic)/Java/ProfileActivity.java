package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private BottomNavigationView bottomNavigation;

    private TextView tvProfileName;
    private TextView tvProfileEmail;

    private TextView tvFullName;
    private TextView tvFatherName;
    private TextView tvMotherName;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvDateOfBirth;
    private TextView tvGender;
    private TextView tvNidNumber;

    private MaterialButton btnEditProfile;
    private MaterialCardView btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupHeader();
        setupProfileButton();
        setupBottomNavigation();
        setupDrawer();
        setupEditProfile();
        setupLogout();
    }


    private void initializeViews() {

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);

        tvFullName = findViewById(R.id.tvFullName);
        tvFatherName = findViewById(R.id.tvFatherName);
        tvMotherName = findViewById(R.id.tvMotherName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
        tvGender = findViewById(R.id.tvGender);
        tvNidNumber = findViewById(R.id.tvNidNumber);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
    }


    private void setupHeader() {

        btnMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );
    }


    private void setupProfileButton() {

        btnProfile.setOnClickListener(v -> {
            // Already on Profile
        });
    }


    private void setupBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {

                startActivity(
                        new Intent(
                                ProfileActivity.this,
                                MainActivity.class
                        )
                );

                return true;

            } else if (id == R.id.nav_notice) {

                startActivity(
                        new Intent(
                                ProfileActivity.this,
                                NoticeActivity.class
                        )
                );

                return true;

            } else if (id == R.id.nav_profile) {

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

        drawerLayout.closeDrawer(navigationView);

        startActivity(
                new Intent(
                        ProfileActivity.this,
                        activityClass
                )
        );
    }


    private void setupEditProfile() {

        btnEditProfile.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            ProfileActivity.this,
                            EditProfileActivity.class
                    )
            );
        });
    }


    private void setupLogout() {

        btnLogout.setOnClickListener(v -> {

            mAuth.signOut();

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            LoginActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);

            finish();
        });
    }


    @Override
    protected void onResume() {

        super.onResume();

        loadUserProfile();
    }


    private void loadUserProfile() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            LoginActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);

            finish();

            return;
        }


        String firebaseName = user.getDisplayName();
        String firebaseEmail = user.getEmail();

        if (firebaseName == null ||
                firebaseName.trim().isEmpty()) {

            firebaseName = "DUET Applicant";
        }

        if (firebaseEmail == null ||
                firebaseEmail.trim().isEmpty()) {

            firebaseEmail = "Not available";
        }


        tvProfileName.setText(firebaseName);
        tvProfileEmail.setText(firebaseEmail);
        tvFullName.setText(firebaseName);
        tvEmail.setText(firebaseEmail);


        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        setValue(tvFatherName, null);
                        setValue(tvMotherName, null);
                        setValue(tvPhone, null);
                        setValue(tvDateOfBirth, null);
                        setValue(tvGender, null);
                        setValue(tvNidNumber, null);

                        return;
                    }


                    // Full Name

                    String name =
                            documentSnapshot.getString("name");

                    if (name != null &&
                            !name.trim().isEmpty()) {

                        tvProfileName.setText(name);
                        tvFullName.setText(name);
                    }


                    // Father's Name

                    setValue(
                            tvFatherName,
                            documentSnapshot.getString("fatherName")
                    );


                    // Mother's Name

                    setValue(
                            tvMotherName,
                            documentSnapshot.getString("motherName")
                    );


                    // Email

                    String email =
                            documentSnapshot.getString("email");

                    if (email != null &&
                            !email.trim().isEmpty()) {

                        tvProfileEmail.setText(email);
                        tvEmail.setText(email);
                    }


                    // Mobile

                    setValue(
                            tvPhone,
                            documentSnapshot.getString("phone")
                    );


                    // Date of Birth

                    setValue(
                            tvDateOfBirth,
                            documentSnapshot.getString("dateOfBirth")
                    );


                    // Gender

                    setValue(
                            tvGender,
                            documentSnapshot.getString("gender")
                    );


                    // NID / Birth Registration

                    setValue(
                            tvNidNumber,
                            documentSnapshot.getString("nidNumber")
                    );

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Failed to load profile",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }


    private void setValue(
            TextView textView,
            String value
    ) {

        if (value != null &&
                !value.trim().isEmpty()) {

            textView.setText(value);

        } else {

            textView.setText("Not provided");
        }
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