package com.example.smartduetadmissionsystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    // =============================================
    // HEADER / DRAWER / FOOTER
    // =============================================

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private BottomNavigationView bottomNavigation;


    // =============================================
    // PROFILE FORM
    // =============================================

    private TextInputEditText etEditName;
    private TextInputEditText etFatherName;
    private TextInputEditText etMotherName;
    private TextInputEditText etEditPhone;
    private TextInputEditText etEditEmail;
    private TextInputEditText etDateOfBirth;
    private TextInputEditText etNidNumber;

    private Spinner spGender;

    private MaterialButton btnSaveProfile;
    private MaterialButton btnAccountSettings;

    private TextView tvAccountEmail;
    private TextView tvAccountPhone;


    // =============================================
    // FIREBASE
    // =============================================

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();

        setupHeader();

        setupProfile();

        setupBottomNavigation();

        setupDrawer();

        setupGenderSpinner();

        setupDatePicker();

        setupAccountSettings();

        loadCurrentProfile();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }


    // =============================================
    // INITIALIZE VIEWS
    // =============================================

    private void initializeViews() {

        drawerLayout = findViewById(R.id.drawerLayout);

        navigationView = findViewById(R.id.navigationView);

        btnMenu = findViewById(R.id.btnMenu);

        btnProfile = findViewById(R.id.btnProfile);

        bottomNavigation = findViewById(R.id.bottomNavigation);


        etEditName = findViewById(R.id.etEditName);

        etFatherName = findViewById(R.id.etFatherName);

        etMotherName = findViewById(R.id.etMotherName);

        etEditPhone = findViewById(R.id.etEditPhone);

        etEditEmail = findViewById(R.id.etEditEmail);

        etDateOfBirth = findViewById(R.id.etDateOfBirth);

        etNidNumber = findViewById(R.id.etNidNumber);

        spGender = findViewById(R.id.spGender);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        btnAccountSettings = findViewById(R.id.btnAccountSettings);

        tvAccountEmail = findViewById(R.id.tvAccountEmail);

        tvAccountPhone = findViewById(R.id.tvAccountPhone);
    }


    // =============================================
    // HEADER
    // =============================================

    private void setupHeader() {

        btnMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );
    }


    // =============================================
    // PROFILE BUTTON
    // =============================================

    private void setupProfile() {

        btnProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    EditProfileActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });
    }


    // =============================================
    // BOTTOM NAVIGATION
    // =============================================

    private void setupBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {

                startActivity(
                        new Intent(
                                EditProfileActivity.this,
                                MainActivity.class
                        )
                );

                return true;

            } else if (id == R.id.nav_notice) {

                startActivity(
                        new Intent(
                                EditProfileActivity.this,
                                NoticeActivity.class
                        )
                );

                return true;

            } else if (id == R.id.nav_profile) {

                startActivity(
                        new Intent(
                                EditProfileActivity.this,
                                ProfileActivity.class
                        )
                );

                return true;
            }

            return false;
        });
    }


    // =============================================
    // DRAWER
    // =============================================

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


    // =============================================
    // OPEN ACTIVITY
    // =============================================

    private void openActivity(Class<?> activityClass) {

        drawerLayout.closeDrawer(navigationView);

        Intent intent = new Intent(
                EditProfileActivity.this,
                activityClass
        );

        startActivity(intent);
    }


    // =============================================
    // GENDER SPINNER
    // =============================================

    private void setupGenderSpinner() {

        String[] genders = {
                "Male",
                "Female",
                "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        genders
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spGender.setAdapter(adapter);
    }


    // =============================================
    // DATE PICKER
    // =============================================

    private void setupDatePicker() {

        etDateOfBirth.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            DatePickerDialog dialog =
                    new DatePickerDialog(
                            EditProfileActivity.this,
                            (view, year, month, dayOfMonth) -> {

                                String date =
                                        String.format(
                                                "%02d/%02d/%04d",
                                                dayOfMonth,
                                                month + 1,
                                                year
                                        );

                                etDateOfBirth.setText(date);
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );

            dialog.getDatePicker().setMaxDate(
                    System.currentTimeMillis()
            );

            dialog.show();
        });
    }


    // =============================================
    // ACCOUNT SETTINGS
    // =============================================

    private void setupAccountSettings() {

        btnAccountSettings.setOnClickListener(v -> {

            Intent intent = new Intent(
                    EditProfileActivity.this,
                    AccountSettingsActivity.class
            );

            startActivity(intent);
        });
    }


    // =============================================
    // LOAD CURRENT PROFILE
    // =============================================

    private void loadCurrentProfile() {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }


        // =============================================
        // EMAIL FROM FIREBASE AUTH
        // =============================================

        String firebaseEmail = user.getEmail();

        if (firebaseEmail != null &&
                !firebaseEmail.trim().isEmpty()) {

            etEditEmail.setText(firebaseEmail);

            tvAccountEmail.setText(firebaseEmail);
        }


        // =============================================
        // LOAD FIRESTORE PROFILE
        // =============================================

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        // NAME

                        String name =
                                documentSnapshot.getString("name");

                        if (!TextUtils.isEmpty(name)) {

                            etEditName.setText(name);

                        } else if (
                                user.getDisplayName() != null &&
                                        !user.getDisplayName().trim().isEmpty()
                        ) {

                            etEditName.setText(
                                    user.getDisplayName()
                            );
                        }


                        // FATHER NAME

                        String fatherName =
                                documentSnapshot.getString("fatherName");

                        if (!TextUtils.isEmpty(fatherName)) {

                            etFatherName.setText(fatherName);
                        }


                        // MOTHER NAME

                        String motherName =
                                documentSnapshot.getString("motherName");

                        if (!TextUtils.isEmpty(motherName)) {

                            etMotherName.setText(motherName);
                        }


                        // PHONE

                        String phone =
                                documentSnapshot.getString("phone");

                        if (!TextUtils.isEmpty(phone)) {

                            etEditPhone.setText(phone);

                            tvAccountPhone.setText(phone);
                        } else {

                            tvAccountPhone.setText(
                                    "Not provided"
                            );
                        }


                        // DATE OF BIRTH

                        String dob =
                                documentSnapshot.getString("dateOfBirth");

                        if (!TextUtils.isEmpty(dob)) {

                            etDateOfBirth.setText(dob);
                        }


                        // GENDER

                        String gender =
                                documentSnapshot.getString("gender");

                        if (!TextUtils.isEmpty(gender)) {

                            for (int i = 0;
                                 i < spGender.getCount();
                                 i++) {

                                String item =
                                        spGender
                                                .getItemAtPosition(i)
                                                .toString();

                                if (item.equalsIgnoreCase(gender)) {

                                    spGender.setSelection(i);

                                    break;
                                }
                            }
                        }


                        // NID / BIRTH REGISTRATION

                        String nid =
                                documentSnapshot.getString("nidNumber");

                        if (!TextUtils.isEmpty(nid)) {

                            etNidNumber.setText(nid);
                        }

                    } else {

                        // =============================================
                        // FALLBACK
                        // =============================================

                        if (user.getDisplayName() != null &&
                                !user.getDisplayName().trim().isEmpty()) {

                            etEditName.setText(
                                    user.getDisplayName()
                            );
                        }

                        if (user.getEmail() != null) {

                            tvAccountEmail.setText(
                                    user.getEmail()
                            );
                        }

                        tvAccountPhone.setText(
                                "Not provided"
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            EditProfileActivity.this,
                            "Failed to load profile: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // =============================================
    // SAVE PROFILE
    // =============================================

    private void saveProfile() {

        String name =
                etEditName.getText()
                        .toString()
                        .trim();

        String fatherName =
                etFatherName.getText()
                        .toString()
                        .trim();

        String motherName =
                etMotherName.getText()
                        .toString()
                        .trim();

        String phone =
                etEditPhone.getText()
                        .toString()
                        .trim();

        String dateOfBirth =
                etDateOfBirth.getText()
                        .toString()
                        .trim();

        String gender =
                spGender.getSelectedItem()
                        .toString()
                        .trim();

        String nid =
                etNidNumber.getText()
                        .toString()
                        .trim();


        // =============================================
        // VALIDATION
        // =============================================

        if (TextUtils.isEmpty(name)) {

            etEditName.setError(
                    "Enter your full name"
            );

            etEditName.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(fatherName)) {

            etFatherName.setError(
                    "Enter father's name"
            );

            etFatherName.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(motherName)) {

            etMotherName.setError(
                    "Enter mother's name"
            );

            etMotherName.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(phone)) {

            etEditPhone.setError(
                    "Enter your mobile number"
            );

            etEditPhone.requestFocus();

            return;
        }


        if (phone.length() != 11) {

            etEditPhone.setError(
                    "Enter a valid 11-digit number"
            );

            etEditPhone.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(dateOfBirth)) {

            etDateOfBirth.setError(
                    "Select your date of birth"
            );

            etDateOfBirth.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(nid)) {

            etNidNumber.setError(
                    "Enter NID / Birth Registration Number"
            );

            etNidNumber.requestFocus();

            return;
        }


        // =============================================
        // CURRENT USER
        // =============================================

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =============================================
        // BUTTON LOADING
        // =============================================

        btnSaveProfile.setEnabled(false);

        btnSaveProfile.setText(
                "Saving..."
        );


        // =============================================
        // FIRESTORE DATA
        // =============================================

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "name",
                name
        );

        updates.put(
                "fatherName",
                fatherName
        );

        updates.put(
                "motherName",
                motherName
        );

        updates.put(
                "phone",
                phone
        );

        updates.put(
                "dateOfBirth",
                dateOfBirth
        );

        updates.put(
                "gender",
                gender
        );

        updates.put(
                "nidNumber",
                nid
        );


        // =============================================
        // SAVE TO FIRESTORE
        // =============================================

        db.collection("users")
                .document(user.getUid())
                .set(
                        updates,
                        SetOptions.merge()
                )
                .addOnSuccessListener(unused -> {

                    // =============================================
                    // UPDATE FIREBASE AUTH DISPLAY NAME
                    // =============================================

                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();


                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {

                                btnSaveProfile.setEnabled(true);

                                btnSaveProfile.setText(
                                        "Save Changes"
                                );


                                if (task.isSuccessful()) {

                                    Toast.makeText(
                                            EditProfileActivity.this,
                                            "Profile updated successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                } else {

                                    Toast.makeText(
                                            EditProfileActivity.this,
                                            "Profile saved successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }


                                // Return to Profile

                                finish();

                            });

                })
                .addOnFailureListener(e -> {

                    btnSaveProfile.setEnabled(true);

                    btnSaveProfile.setText(
                            "Save Changes"
                    );


                    Toast.makeText(
                            EditProfileActivity.this,
                            "Failed to update profile: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // =============================================
    // BACK PRESSED
    // =============================================

    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(navigationView)) {

            drawerLayout.closeDrawer(navigationView);

        } else {

            super.onBackPressed();
        }
    }
}