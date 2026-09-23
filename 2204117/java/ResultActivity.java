package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private ImageButton btnMenu;
    private MaterialCardView btnProfile;

    // Dynamic section cards
    private MaterialCardView cardApplicationInfo;
    private MaterialCardView cardResultStatus;
    private MaterialCardView cardMeritPosition;
    private MaterialCardView cardResultDetails;

    // Result header
    private TextView tvResultTitle;
    private TextView tvResultMessage;

    // Application information
    private TextView tvApplicationNumber;
    private TextView tvApplicantName;
    private TextView tvFatherName;
    private TextView tvDepartment;
    private TextView tvDiplomaTechnology;

    // Merit
    private TextView tvMeritPosition;

    // Result details
    private TextView tvResultStatus;
    private TextView tvResultPublished;
    private TextView tvMarks;
    private TextView tvTotalMarks;
    private TextView tvRemarks;

    // Optional result rows
    private View rowMarksTotal;
    private View rowRemarks;

    private BottomNavigationView bottomNavigation;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration resultListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupMenu();
        setupProfile();
        setupDrawer();
        setupBottomNavigation();

        loadStudentApplicationRealtime();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);

        cardApplicationInfo = findViewById(R.id.cardApplicationInfo);
        cardResultStatus = findViewById(R.id.cardResultStatus);
        cardMeritPosition = findViewById(R.id.cardMeritPosition);
        cardResultDetails = findViewById(R.id.cardResultDetails);

        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultMessage = findViewById(R.id.tvResultMessage);

        tvApplicationNumber = findViewById(R.id.tvApplicationNumber);
        tvApplicantName = findViewById(R.id.tvApplicantName);
        tvFatherName = findViewById(R.id.tvFatherName);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvDiplomaTechnology = findViewById(R.id.tvDiplomaTechnology);

        tvMeritPosition = findViewById(R.id.tvMeritPosition);

        tvResultStatus = findViewById(R.id.tvResultStatus);
        tvResultPublished = findViewById(R.id.tvResultPublished);
        tvMarks = findViewById(R.id.tvMarks);
        tvTotalMarks = findViewById(R.id.tvTotalMarks);
        tvRemarks = findViewById(R.id.tvRemarks);

        rowMarksTotal = findViewById(R.id.rowMarksTotal);
        rowRemarks = findViewById(R.id.rowRemarks);

        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupMenu() {
        if (btnMenu != null && drawerLayout != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
        }
    }

    private void setupProfile() {
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                    startActivity(new Intent(ResultActivity.this, ProfileActivity.class))
            );
        }
    }

    private void setupDrawer() {
        if (navigationView == null) return;

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

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
                closeDrawer();
            } else if (id == R.id.nav_syllabus) {
                openActivity(SyllabusActivity.class);
            }

            return true;
        });
    }

    private void openActivity(Class<?> activityClass) {
        closeDrawer();
        startActivity(new Intent(ResultActivity.this, activityClass));
    }

    private void closeDrawer() {
        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                openActivity(MainActivity.class);
                return true;
            }

            if (id == R.id.nav_notice) {
                openActivity(NoticeActivity.class);
                return true;
            }

            if (id == R.id.nav_profile) {
                openActivity(ProfileActivity.class);
                return true;
            }

            return false;
        });
    }

    /**
     * Loads ONLY the currently logged-in student's application.
     *
     * Firestore path:
     * applications/{studentUid}
     *
     * Result becomes visible only when:
     * resultPublished == true
     * AND
     * resultAvailable == true
     *
     * Snapshot listener makes the student screen realtime.
     */
    private void loadStudentApplicationRealtime() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showNoApplication("Please login first.");
            Toast.makeText(this, "Please login first.", Toast.LENGTH_LONG).show();
            return;
        }

        removeResultListener();

        resultListener = db.collection("applications")
                .document(user.getUid())
                .addSnapshotListener((document, error) -> {

                    if (error != null) {
                        showNoApplication("Unable to load your application right now.");
                        return;
                    }

                    if (document == null || !document.exists()) {
                        showNoApplication("Your admission application was not found.");
                        return;
                    }

                    // Application information is always loaded from Firebase.
                    showApplicationInformation(document);

                    boolean resultPublished =
                            getBoolean(document, "resultPublished");

                    boolean resultAvailable =
                            getBoolean(document, "resultAvailable");

                    // Both flags are required.
                    if (!resultPublished || !resultAvailable) {
                        showResultNotPublished();
                        return;
                    }

                    showPublishedResult(document);
                });
    }

    private void showApplicationInformation(DocumentSnapshot document) {
        cardApplicationInfo.setVisibility(View.VISIBLE);

        tvApplicationNumber.setText(
                first(document,
                        "applicationNumber",
                        "applicationNo",
                        "applicationId",
                        "applicationIdNumber")
        );

        tvApplicantName.setText(
                first(document,
                        "applicantName",
                        "name",
                        "fullName")
        );

        tvFatherName.setText(
                first(document,
                        "fatherName",
                        "fathersName",
                        "father_name")
        );

        tvDepartment.setText(
                first(document,
                        "department",
                        "departmentName",
                        "selectedDepartment")
        );

        tvDiplomaTechnology.setText(
                first(document,
                        "diplomaTechnology",
                        "technology",
                        "technologyName",
                        "diploma_technology")
        );
    }

    private void showResultNotPublished() {
        cardResultStatus.setVisibility(View.VISIBLE);
        cardMeritPosition.setVisibility(View.GONE);
        cardResultDetails.setVisibility(View.GONE);

        tvResultTitle.setText("Result Not Published");
        tvResultMessage.setText(
                "Your admission result has not been published yet. " +
                        "Please check again after the official publication."
        );

        tvResultStatus.setText("NOT PUBLISHED");
        tvResultPublished.setText("Not published");
        tvMeritPosition.setText("—");

        rowMarksTotal.setVisibility(View.GONE);
        rowRemarks.setVisibility(View.GONE);
    }

    private void showPublishedResult(DocumentSnapshot document) {
        cardResultStatus.setVisibility(View.VISIBLE);
        cardMeritPosition.setVisibility(View.VISIBLE);
        cardResultDetails.setVisibility(View.VISIBLE);

        String status = firstOrEmpty(
                document,
                "resultStatus",
                "result",
                "status"
        );

        String marks = firstOrEmpty(
                document,
                "marks",
                "obtainedMarks",
                "score"
        );

        String totalMarks = firstOrEmpty(
                document,
                "totalMarks",
                "maximumMarks",
                "fullMarks"
        );

        String merit = firstOrEmpty(
                document,
                "meritPosition",
                "merit",
                "rank",
                "position"
        );

        String remarks = firstOrEmpty(
                document,
                "remarks",
                "resultRemarks",
                "remark"
        );

        String publishedAt = formatPublishedDate(document);

        if (status.isEmpty()) {
            status = "PUBLISHED";
        }

        tvResultStatus.setText(status.toUpperCase(Locale.ROOT));

        tvResultPublished.setText(
                publishedAt.isEmpty() ? "Published" : publishedAt
        );

        tvMeritPosition.setText(
                merit.isEmpty() ? "—" : merit
        );

        // Dynamic marks section.
        if (!marks.isEmpty() || !totalMarks.isEmpty()) {
            rowMarksTotal.setVisibility(View.VISIBLE);

            tvMarks.setText(
                    marks.isEmpty() ? "—" : marks
            );

            tvTotalMarks.setText(
                    totalMarks.isEmpty() ? "—" : totalMarks
            );
        } else {
            rowMarksTotal.setVisibility(View.GONE);
        }

        // Dynamic remarks section.
        if (!remarks.isEmpty()) {
            rowRemarks.setVisibility(View.VISIBLE);
            tvRemarks.setText(remarks);
        } else {
            rowRemarks.setVisibility(View.GONE);
        }

        String normalized = status.trim().toLowerCase(Locale.ROOT);

        if (normalized.equals("pass")
                || normalized.equals("passed")
                || normalized.equals("selected")
                || normalized.equals("qualified")) {

            tvResultTitle.setText("Congratulations!");
            tvResultMessage.setText(
                    "Your official admission result has been published successfully."
            );

        } else if (normalized.equals("fail")
                || normalized.equals("failed")
                || normalized.equals("not selected")
                || normalized.equals("not_selected")) {

            tvResultTitle.setText("Admission Result");
            tvResultMessage.setText(
                    "Your official admission result has been published."
            );

        } else {

            tvResultTitle.setText("Admission Result");
            tvResultMessage.setText(
                    "Your official admission result is now available."
            );
        }
    }

    private void showNoApplication(String message) {
        cardApplicationInfo.setVisibility(View.GONE);
        cardResultStatus.setVisibility(View.VISIBLE);
        cardMeritPosition.setVisibility(View.GONE);
        cardResultDetails.setVisibility(View.GONE);

        tvResultTitle.setText("Result Unavailable");
        tvResultMessage.setText(message);
        tvResultStatus.setText("UNAVAILABLE");
        tvResultPublished.setText("—");
        tvMeritPosition.setText("—");

        rowMarksTotal.setVisibility(View.GONE);
        rowRemarks.setVisibility(View.GONE);
    }

    private boolean getBoolean(DocumentSnapshot document, String field) {
        Boolean value = document.getBoolean(field);
        return Boolean.TRUE.equals(value);
    }

    private String first(DocumentSnapshot document, String... fields) {
        String value = firstOrEmpty(document, fields);
        return value.isEmpty() ? "Not available" : value;
    }

    private String firstOrEmpty(DocumentSnapshot document, String... fields) {
        for (String field : fields) {
            Object value = document.get(field);

            if (value == null) continue;

            String text = String.valueOf(value).trim();

            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }

        return "";
    }

    private String formatPublishedDate(DocumentSnapshot document) {
        Object value = document.get("resultPublishedAt");

        if (value instanceof Timestamp) {
            Date date = ((Timestamp) value).toDate();

            return new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.ENGLISH
            ).format(date);
        }

        // Also supports a string timestamp if the project stores it as text.
        if (value != null) {
            String text = String.valueOf(value).trim();

            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }

        return "";
    }

    private void removeResultListener() {
        if (resultListener != null) {
            resultListener.remove();
            resultListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        removeResultListener();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(Gravity.START)) {

            drawerLayout.closeDrawer(Gravity.START);

        } else {
            super.onBackPressed();
        }
    }
}
