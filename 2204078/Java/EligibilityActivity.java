package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.textfield.TextInputEditText;

public class EligibilityActivity extends AppCompatActivity {

    private TextInputEditText etSscGpa;
    private TextInputEditText etDiplomaGpa;

    private AutoCompleteTextView actDiplomaYear;
    private AutoCompleteTextView actTechnology;

    private MaterialButton btnCheckEligibility;
    private MaterialButton btnApplyAdmission;

    private MaterialCardView resultCard;

    private TextView tvResult;
    private TextView tvResultMessage;
    private ImageButton btnMenu;
    private ImageButton btnProfile;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_eligibility);

        // -----------------------------
        // Connect Views
        // -----------------------------

        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);

        etSscGpa = findViewById(R.id.etSscGpa);
        etDiplomaGpa = findViewById(R.id.etDiplomaGpa);

        actDiplomaYear = findViewById(R.id.actDiplomaYear);
        actTechnology = findViewById(R.id.actTechnology);

        btnCheckEligibility =
                findViewById(R.id.btnCheckEligibility);

        btnApplyAdmission =
                findViewById(R.id.btnApplyAdmission);

        resultCard =
                findViewById(R.id.resultCard);

        tvResult =
                findViewById(R.id.tvResult);

        tvResultMessage =
                findViewById(R.id.tvResultMessage);

        // -----------------------------
        // Setup Dropdowns
        // -----------------------------

        setupDropdowns();

        // -----------------------------
        // Fixed Dashboard-style header
        // -----------------------------

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Same dark side drawer used on the Dashboard.
        btnMenu.setOnClickListener(v -> {

            if (drawerLayout != null) {
                drawerLayout.openDrawer(
                        android.view.Gravity.START
                );
            }
        });

        btnProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    EligibilityActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });

        setupNavigationDrawer();
        setupBottomNavigation();

        // Keep the existing eligibility functionality working.
        btnCheckEligibility.setOnClickListener(
                v -> checkEligibility()
        );

        btnApplyAdmission.setOnClickListener(v -> {

            startActivity(new Intent(
                    EligibilityActivity.this,
                    AdmissionApplicationActivity.class
            ));
        });
    }

    // =========================================================
    // FIXED BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {

                Intent intent = new Intent(
                        EligibilityActivity.this,
                        MainActivity.class
                );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                startActivity(intent);

                return true;

            } else if (id == R.id.nav_notice) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        NoticeActivity.class
                ));

                return true;

            } else if (id == R.id.nav_profile) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        ProfileActivity.class
                ));

                return true;
            }

            return false;
        });
    }

    // =========================================================
    // DASHBOARD SIDE DRAWER
    // =========================================================

    private void setupNavigationDrawer() {

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {

                Intent intent = new Intent(
                        EligibilityActivity.this,
                        MainActivity.class
                );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                startActivity(intent);

                finish();

            } else if (id == R.id.nav_eligibility) {

                // Already on this page.

            } else if (id == R.id.nav_notice) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        NoticeActivity.class
                ));

            } else if (id == R.id.nav_department) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        DepartmentsActivity.class
                ));

            } else if (id == R.id.nav_apply) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        AdmissionApplicationActivity.class
                ));

            } else if (id == R.id.nav_tracker) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        ApplicationTrackerActivity.class
                ));

                // =====================================================
                // ADMIT CARD
                // =====================================================

            } else if (id == R.id.nav_admit_card) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        AdmitCardActivity.class
                ));

                // =====================================================
                // VIEW RESULT
                // =====================================================

            } else if (id == R.id.nav_result) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        ResultActivity.class
                ));

                // =====================================================
                // SYLLABUS
                // =====================================================

            } else if (id == R.id.nav_syllabus) {

                startActivity(new Intent(
                        EligibilityActivity.this,
                        SyllabusActivity.class
                ));
            }

            // Close drawer after selection
            drawerLayout.closeDrawer(
                    android.view.Gravity.START
            );

            return true;
        });
    }

    // =========================================================
    // DROPDOWNS
    // =========================================================

    private void setupDropdowns() {

        String[] years = {
                "2026",
                "2025",
                "2024",
                "2023",
                "2022",
                "2021",
                "2020",
                "2019",
                "2018",
                "2017",
                "2016"
        };

        ArrayAdapter<String> yearAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        years
                );

        actDiplomaYear.setAdapter(yearAdapter);

        String[] technologies = {
                "Computer Technology",
                "Civil Technology",
                "Electrical Technology",
                "Mechanical Technology",
                "Electronics Technology",
                "Architecture Technology",
                "Automobile Technology",
                "Chemical Technology",
                "Environmental Technology",
                "Power Technology",
                "Telecommunication Technology"
        };

        ArrayAdapter<String> technologyAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        technologies
                );

        actTechnology.setAdapter(technologyAdapter);
    }

    // =========================================================
    // CHECK ELIGIBILITY
    // =========================================================

    private void checkEligibility() {

        String sscText =
                etSscGpa.getText()
                        .toString()
                        .trim();

        String diplomaText =
                etDiplomaGpa.getText()
                        .toString()
                        .trim();

        String diplomaYear =
                actDiplomaYear.getText()
                        .toString()
                        .trim();

        String technology =
                actTechnology.getText()
                        .toString()
                        .trim();

        // =====================================================
        // VALIDATION
        // =====================================================

        if (TextUtils.isEmpty(sscText)) {

            etSscGpa.setError(
                    "Enter SSC GPA"
            );

            etSscGpa.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(diplomaText)) {

            etDiplomaGpa.setError(
                    "Enter Diploma CGPA"
            );

            etDiplomaGpa.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(diplomaYear)) {

            actDiplomaYear.setError(
                    "Select Diploma passing year"
            );

            actDiplomaYear.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(technology)) {

            actTechnology.setError(
                    "Select diploma technology"
            );

            actTechnology.requestFocus();

            return;
        }

        // =====================================================
        // PARSE GPA
        // =====================================================

        double ssc;
        double diploma;
        int diplomaPassingYear;

        try {

            ssc = Double.parseDouble(sscText);

            diploma = Double.parseDouble(diplomaText);

            diplomaPassingYear =
                    Integer.parseInt(diplomaYear);

        } catch (NumberFormatException e) {

            etSscGpa.setError(
                    "Enter valid academic information"
            );

            etSscGpa.requestFocus();

            return;
        }

        // =====================================================
        // GPA RANGE
        // =====================================================

        if (ssc < 0 || ssc > 5) {

            etSscGpa.setError(
                    "GPA must be between 0 and 5"
            );

            etSscGpa.requestFocus();

            return;
        }

        if (diploma < 0 || diploma > 4) {

            etDiplomaGpa.setError(
                    "CGPA must be between 0 and 4"
            );

            etDiplomaGpa.requestFocus();

            return;
        }

        // =====================================================
        // MINIMUM REQUIREMENTS
        // =====================================================

        boolean sscEligible =
                ssc >= 3.00;

        boolean diplomaEligible =
                diploma >= 3.00;

        // =====================================================
        // PASSING YEAR
        // =====================================================

        boolean passingYearEligible =
                diplomaPassingYear == 2025
                        || diplomaPassingYear == 2024;

        // =====================================================
        // FINAL RESULT
        // =====================================================

        boolean eligible =
                sscEligible
                        && diplomaEligible
                        && passingYearEligible;

        // Show result card
        resultCard.setVisibility(View.VISIBLE);

        // =====================================================
        // ELIGIBLE
        // =====================================================

        if (eligible) {

            tvResult.setText(
                    "You Are Eligible"
            );

            tvResult.setTextColor(
                    getColor(android.R.color.holo_green_dark)
            );

            tvResultMessage.setText(
                    "You meet the basic eligibility requirements "
                            + "for DUET admission based on your SSC GPA, "
                            + "Diploma CGPA, and Diploma passing year."
            );

            // Show Apply button
            btnApplyAdmission.setVisibility(
                    View.VISIBLE
            );

        }

        // =====================================================
        // NOT ELIGIBLE
        // =====================================================

        else {

            btnApplyAdmission.setVisibility(
                    View.GONE
            );

            tvResult.setText(
                    "Not Eligible"
            );

            tvResult.setTextColor(
                    getColor(android.R.color.holo_red_dark)
            );

            String message;

            if (!sscEligible) {

                message =
                        "SSC / Equivalent GPA must be at least "
                                + "3.00 out of 5.00.";

            } else if (!diplomaEligible) {

                message =
                        "Diploma CGPA must be at least "
                                + "3.00 out of 4.00.";

            } else if (!passingYearEligible) {

                message =
                        "For the 2026 admission cycle, only the latest "
                                + "two Diploma passing years, 2025 and 2024, "
                                + "are considered.";

            } else {

                message =
                        "You do not currently meet the eligibility criteria.";
            }

            tvResultMessage.setText(
                    message
            );
        }
    }
}