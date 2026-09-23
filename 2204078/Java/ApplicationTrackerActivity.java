package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
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

public class ApplicationTrackerActivity extends AppCompatActivity {

    // =========================================================
    // HEADER / DRAWER / FOOTER
    // =========================================================

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private BottomNavigationView bottomNavigation;


    // =========================================================
    // APPLICATION LAYOUT
    // =========================================================

    private LinearLayout layoutApplication;
    private LinearLayout layoutNoApplication;


    // =========================================================
    // APPLICATION STATUS
    // =========================================================

    private TextView tvApplicationStatus;
    private TextView tvStatusBadge;
    private TextView tvApplicationId;


    // =========================================================
    // STATUS MESSAGE
    // =========================================================

    private TextView tvStatusMessage;


    // =========================================================
    // APPLICATION DETAILS
    // =========================================================

    private TextView tvApplicantName;
    private TextView tvAdmissionYear;
    private TextView tvDiplomaTechnology;
    private TextView tvSelectedDepartment;
    private TextView tvFaculty;
    private TextView tvSscGpa;
    private TextView tvDiplomaCgpa;
    private TextView tvPassingYear;
    private TextView tvQuota;
    private TextView tvSubmittedAt;


    // =========================================================
    // PAYMENT
    // =========================================================

    private TextView tvPaymentMethod;
    private TextView tvPaymentAmount;
    private TextView tvTransactionId;
    private TextView tvPaymentStatus;
    private TextView tvPaymentCardStatus;
    private TextView tvPaidAt;


    // =========================================================
    // TIMELINE
    // =========================================================

    private TextView tvStepCreated;
    private TextView tvStepSubmitted;
    private TextView tvStepPayment;
    private TextView tvStepReview;
    private TextView tvStepDecision;
    private TextView tvStepResult;

    private View lineCreated;
    private View lineSubmitted;
    private View linePayment;
    private View lineReview;
    private View lineDecision;


    // =========================================================
    // ADMIT CARD
    // =========================================================

    private TextView tvAdmitCardStatus;
    private TextView tvAdmitCardMessage;
    private MaterialButton btnViewAdmitCard;


    // =========================================================
    // RESULT
    // =========================================================

    private TextView tvResultStatus;
    private TextView tvResultMessage;
    private MaterialButton btnViewResult;


    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ListenerRegistration applicationListener;
    private ListenerRegistration legacyApplicationListener;


    // =========================================================
    // COLORS
    // =========================================================

    private static final int GREEN =
            Color.rgb(12, 145, 84);

    private static final int RED =
            Color.rgb(220, 38, 38);

    private static final int PENDING =
            Color.rgb(180, 120, 0);

    private static final int MUTED =
            Color.rgb(100, 116, 139);


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


        setContentView(
                R.layout.activity_application_tracker
        );


        mAuth =
                FirebaseAuth.getInstance();


        db =
                FirebaseFirestore.getInstance();


        initializeViews();


        setupHeader();


        setupDrawer();


        setupBottomNavigation();


        setupButtons();


        loadApplication();
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        drawerLayout =
                findViewById(
                        R.id.drawerLayout
                );


        navigationView =
                findViewById(
                        R.id.navigationView
                );


        btnMenu =
                findViewById(
                        R.id.btnMenu
                );


        btnProfile =
                findViewById(
                        R.id.btnProfile
                );


        bottomNavigation =
                findViewById(
                        R.id.bottomNavigation
                );


        layoutApplication =
                findViewById(
                        R.id.layoutApplication
                );


        layoutNoApplication =
                findViewById(
                        R.id.layoutNoApplication
                );


        tvApplicationStatus =
                findViewById(
                        R.id.tvApplicationStatus
                );


        tvStatusBadge =
                findViewById(
                        R.id.tvStatusBadge
                );


        tvApplicationId =
                findViewById(
                        R.id.tvApplicationId
                );


        /*
         * IMPORTANT
         *
         * We intentionally do NOT use:
         *
         * R.id.tvStatusMessage
         *
         * because your current generated R file was showing
         * an error for that resource.
         *
         * The message is therefore handled safely below.
         */

        tvStatusMessage =
                findStatusMessageSafely();


        // =====================================================
        // APPLICATION DETAILS
        // =====================================================

        tvApplicantName =
                findViewById(
                        R.id.tvApplicantName
                );


        tvAdmissionYear =
                findViewById(
                        R.id.tvAdmissionYear
                );


        tvDiplomaTechnology =
                findViewById(
                        R.id.tvDiplomaTechnology
                );


        tvSelectedDepartment =
                findViewById(
                        R.id.tvSelectedDepartment
                );


        tvFaculty =
                findViewById(
                        R.id.tvFaculty
                );


        tvSscGpa =
                findViewById(
                        R.id.tvSscGpa
                );


        tvDiplomaCgpa =
                findViewById(
                        R.id.tvDiplomaCgpa
                );


        tvPassingYear =
                findViewById(
                        R.id.tvPassingYear
                );


        tvQuota =
                findViewById(
                        R.id.tvQuota
                );


        tvSubmittedAt =
                findViewById(
                        R.id.tvSubmittedAt
                );


        // =====================================================
        // PAYMENT
        // =====================================================

        tvPaymentMethod =
                findViewById(
                        R.id.tvPaymentMethod
                );


        tvPaymentAmount =
                findViewById(
                        R.id.tvPaymentAmount
                );


        tvTransactionId =
                findViewById(
                        R.id.tvTransactionId
                );


        tvPaymentStatus =
                findViewById(
                        R.id.tvPaymentStatus
                );


        tvPaymentCardStatus =
                findViewById(
                        R.id.tvPaymentCardStatus
                );


        tvPaidAt =
                findViewById(
                        R.id.tvPaidAt
                );


        // =====================================================
        // TIMELINE
        // =====================================================

        tvStepCreated =
                findViewById(
                        R.id.tvStepCreated
                );


        tvStepSubmitted =
                findViewById(
                        R.id.tvStepSubmitted
                );


        tvStepPayment =
                findViewById(
                        R.id.tvStepPayment
                );


        tvStepReview =
                findViewById(
                        R.id.tvStepReview
                );


        tvStepDecision =
                findViewById(
                        R.id.tvStepDecision
                );


        tvStepResult =
                findViewById(
                        R.id.tvStepResult
                );


        lineCreated =
                findViewById(
                        R.id.lineCreated
                );


        lineSubmitted =
                findViewById(
                        R.id.lineSubmitted
                );


        linePayment =
                findViewById(
                        R.id.linePayment
                );


        lineReview =
                findViewById(
                        R.id.lineReview
                );


        lineDecision =
                findViewById(
                        R.id.lineDecision
                );


        // =====================================================
        // ADMIT CARD
        // =====================================================

        tvAdmitCardStatus =
                findViewById(
                        R.id.tvAdmitCardStatus
                );


        tvAdmitCardMessage =
                findViewById(
                        R.id.tvAdmitCardMessage
                );


        btnViewAdmitCard =
                findViewById(
                        R.id.btnViewAdmitCard
                );


        // =====================================================
        // RESULT
        // =====================================================

        tvResultStatus =
                findViewById(
                        R.id.tvResultStatus
                );


        tvResultMessage =
                findViewById(
                        R.id.tvResultMessage
                );


        btnViewResult =
                findViewById(
                        R.id.btnViewResult
                );
    }


    // =========================================================
    // SAFE STATUS MESSAGE
    // =========================================================

    private TextView findStatusMessageSafely() {

        int id =
                getResources()
                        .getIdentifier(
                                "tvStatusMessage",
                                "id",
                                getPackageName()
                        );


        if (id != 0) {

            View view =
                    findViewById(id);


            if (view instanceof TextView) {

                return (TextView) view;
            }
        }


        return null;
    }


    // =========================================================
    // HEADER
    // =========================================================

    private void setupHeader() {

        if (btnMenu != null) {

            btnMenu.setOnClickListener(
                    v -> {

                        if (drawerLayout != null) {

                            drawerLayout.openDrawer(
                                    GravityCompat.START
                            );
                        }
                    }
            );
        }


        if (btnProfile != null) {

            btnProfile.setOnClickListener(
                    v -> startActivity(
                            new Intent(
                                    ApplicationTrackerActivity.this,
                                    ProfileActivity.class
                            )
                    )
            );
        }
    }


    // =========================================================
    // DRAWER
    // =========================================================

    private void setupDrawer() {

        if (navigationView == null) {
            return;
        }


        navigationView.setNavigationItemSelectedListener(
                item -> {

                    int id =
                            item.getItemId();


                    if (id == R.id.nav_dashboard) {

                        openActivity(
                                MainActivity.class
                        );

                    } else if (
                            id == R.id.nav_eligibility
                    ) {

                        openActivity(
                                EligibilityActivity.class
                        );

                    } else if (
                            id == R.id.nav_notice
                    ) {

                        openActivity(
                                NoticeActivity.class
                        );

                    } else if (
                            id == R.id.nav_department
                    ) {

                        openActivity(
                                DepartmentsActivity.class
                        );

                    } else if (
                            id == R.id.nav_apply
                    ) {

                        openActivity(
                                AdmissionApplicationActivity.class
                        );

                    } else if (
                            id == R.id.nav_tracker
                    ) {

                        // Current page

                    } else if (
                            id == R.id.nav_admit_card
                    ) {

                        openActivity(
                                AdmitCardActivity.class
                        );

                    } else if (
                            id == R.id.nav_result
                    ) {

                        openActivity(
                                ResultActivity.class
                        );

                    } else if (
                            id == R.id.nav_syllabus
                    ) {

                        openActivity(
                                SyllabusActivity.class
                        );
                    }


                    if (drawerLayout != null) {

                        drawerLayout.closeDrawer(
                                GravityCompat.START
                        );
                    }


                    return true;
                }
        );
    }


    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {

        if (bottomNavigation == null) {
            return;
        }


        bottomNavigation.setOnItemSelectedListener(
                item -> {

                    int id =
                            item.getItemId();


                    if (id == R.id.nav_home) {

                        openActivity(
                                MainActivity.class
                        );

                        return true;
                    }


                    if (id == R.id.nav_notice) {

                        openActivity(
                                NoticeActivity.class
                        );

                        return true;
                    }


                    if (id == R.id.nav_profile) {

                        openActivity(
                                ProfileActivity.class
                        );

                        return true;
                    }


                    return false;
                }
        );
    }


    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        if (btnViewAdmitCard != null) {

            btnViewAdmitCard.setOnClickListener(
                    v -> startActivity(
                            new Intent(
                                    ApplicationTrackerActivity.this,
                                    AdmitCardActivity.class
                            )
                    )
            );
        }


        if (btnViewResult != null) {

            btnViewResult.setOnClickListener(
                    v -> startActivity(
                            new Intent(
                                    ApplicationTrackerActivity.this,
                                    ResultActivity.class
                            )
                    )
            );
        }
    }


    // =========================================================
    // OPEN ACTIVITY
    // =========================================================

    private void openActivity(
            Class<?> activityClass
    ) {

        startActivity(
                new Intent(
                        ApplicationTrackerActivity.this,
                        activityClass
                )
        );
    }


    // =========================================================
    // LOAD APPLICATION
    // =========================================================

    private void loadApplication() {

        FirebaseUser user =
                mAuth.getCurrentUser();


        if (user == null) {

            showNoApplication();

            return;
        }


        String uid =
                user.getUid();


        layoutApplication.setVisibility(
                View.GONE
        );


        layoutNoApplication.setVisibility(
                View.GONE
        );


        clearListeners();


        // =====================================================
        // PRIMARY COLLECTION
        // =====================================================

        applicationListener =
                db.collection(
                                "applications"
                        )
                        .document(uid)
                        .addSnapshotListener(
                                (
                                        document,
                                        error
                                ) -> {


                                    if (error != null) {

                                        loadLegacyApplication(
                                                uid
                                        );

                                        return;
                                    }


                                    if (
                                            document != null
                                                    && document.exists()
                                    ) {

                                        showApplication(
                                                document
                                        );

                                    } else {

                                        loadLegacyApplication(
                                                uid
                                        );
                                    }
                                }
                        );
    }


    // =========================================================
    // LEGACY COLLECTION
    // =========================================================

    private void loadLegacyApplication(
            String uid
    ) {

        if (legacyApplicationListener != null) {

            legacyApplicationListener.remove();

            legacyApplicationListener = null;
        }


        legacyApplicationListener =
                db.collection(
                                "applicationId"
                        )
                        .document(uid)
                        .addSnapshotListener(
                                (
                                        document,
                                        error
                                ) -> {


                                    if (error != null) {

                                        showLoadError(
                                                error
                                        );

                                        return;
                                    }


                                    if (
                                            document != null
                                                    && document.exists()
                                    ) {

                                        showApplication(
                                                document
                                        );

                                    } else {

                                        showNoApplication();
                                    }
                                }
                        );
    }


    // =========================================================
    // CLEAR LISTENERS
    // =========================================================

    private void clearListeners() {

        if (applicationListener != null) {

            applicationListener.remove();

            applicationListener = null;
        }


        if (legacyApplicationListener != null) {

            legacyApplicationListener.remove();

            legacyApplicationListener = null;
        }
    }


    // =========================================================
    // LOAD ERROR
    // =========================================================

    private void showLoadError(
            Exception e
    ) {

        showNoApplication();


        Toast.makeText(
                this,
                "Failed to load application.",
                Toast.LENGTH_LONG
        ).show();
    }


    // =========================================================
    // NO APPLICATION
    // =========================================================

    private void showNoApplication() {

        if (layoutApplication != null) {

            layoutApplication.setVisibility(
                    View.GONE
            );
        }


        if (layoutNoApplication != null) {

            layoutNoApplication.setVisibility(
                    View.VISIBLE
            );
        }
    }


    // =========================================================
    // SHOW APPLICATION
    // =========================================================

    private void showApplication(
            @NonNull DocumentSnapshot d
    ) {

        String applicationId =
                value(
                        d,
                        "applicationId",
                        "N/A"
                );


        String name =
                value(
                        d,
                        "name",
                        value(
                                d,
                                "fullName",
                                "Not Available"
                        )
                );


        String technology =
                value(
                        d,
                        "diplomaTechnology",
                        value(
                                d,
                                "technology",
                                "Not Available"
                        )
                );


        String department =
                value(
                        d,
                        "department",
                        "Not Selected"
                );


        String faculty =
                value(
                        d,
                        "faculty",
                        getFacultyForDepartment(
                                department
                        )
                );


        String sscGpa =
                value(
                        d,
                        "sscGpa",
                        "N/A"
                );


        String diplomaCgpa =
                value(
                        d,
                        "diplomaCgpa",
                        "N/A"
                );


        String passingYear =
                value(
                        d,
                        "passingYear",
                        "N/A"
                );


        String quota =
                value(
                        d,
                        "quota",
                        "General"
                );


        // =====================================================
        // APPLICATION STATUS
        // =====================================================

        String rawApplicationStatus =
                value(
                        d,
                        "status",
                        ""
                );


        String applicationStatus =
                resolveApplicationStatus(
                        rawApplicationStatus
                );


        // =====================================================
        // PAYMENT STATUS
        // =====================================================

        String rawPaymentStatus =
                value(
                        d,
                        "paymentStatus",
                        ""
                );


        boolean applicationSubmitted =
                isApplicationSubmitted(
                        rawApplicationStatus
                );

        String paymentStatus =
                resolvePaymentStatus(
                        rawPaymentStatus,
                        applicationSubmitted
                );


        String paymentMethod =
                value(
                        d,
                        "paymentMethod",
                        "N/A"
                );


        String transactionId =
                value(
                        d,
                        "transactionId",
                        "N/A"
                );


        // =====================================================
        // APPLICATION INFORMATION
        // =====================================================

        setText(
                tvApplicationId,
                applicationId
        );


        setText(
                tvApplicantName,
                name
        );


        setText(
                tvAdmissionYear,
                resolveAdmissionYear(d)
        );


        setText(
                tvDiplomaTechnology,
                technology
        );


        setText(
                tvSelectedDepartment,
                department
        );


        setText(
                tvFaculty,
                faculty
        );


        setText(
                tvSscGpa,
                sscGpa
        );


        setText(
                tvDiplomaCgpa,
                diplomaCgpa
        );


        setText(
                tvPassingYear,
                passingYear
        );


        setText(
                tvQuota,
                quota
        );


        setText(
                tvSubmittedAt,
                formatDate(
                        d.get("submittedAt"),
                        value(
                                d,
                                "applicationDate",
                                "N/A"
                        )
                )
        );


        // =====================================================
        // PAYMENT INFORMATION
        // =====================================================

        setText(
                tvPaymentMethod,
                paymentMethod
        );


        setText(
                tvPaymentAmount,
                formatAmount(
                        d.get("amount"),
                        d.get("paymentAmount")
                )
        );


        setText(
                tvTransactionId,
                transactionId
        );


        setText(
                tvPaymentStatus,
                paymentStatus
        );


        setText(
                tvPaymentCardStatus,
                paymentStatus
        );


        setText(
                tvPaidAt,
                formatDate(
                        d.get("paidAt"),
                        formatDate(
                                d.get("paymentAt"),
                                "N/A"
                        )
                )
        );


        // =====================================================
        // STATUS UI
        // =====================================================

        applyStatus(
                applicationStatus,
                paymentStatus
        );


        // =====================================================
        // ADMIT CARD + RESULT
        // =====================================================

        updateAdmissionUpdates(
                d
        );


        layoutNoApplication.setVisibility(
                View.GONE
        );


        layoutApplication.setVisibility(
                View.VISIBLE
        );
    }


    // =========================================================
    // CHECK APPLICATION SUBMISSION
    // =========================================================

    private boolean isApplicationSubmitted(
            String rawStatus
    ) {

        if (
                rawStatus == null
                        || rawStatus.trim().isEmpty()
        ) {

            return false;
        }

        String status =
                rawStatus
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return status.equals("submitted")
                || status.equals("submit")
                || status.equals("pending")
                || status.equals("under review")
                || status.equals("review")
                || status.equals("processing")
                || status.equals("in process")
                || status.equals("approved")
                || status.equals("approve")
                || status.equals("accepted")
                || status.equals("accept")
                || status.equals("rejected")
                || status.equals("reject")
                || status.equals("declined")
                || status.equals("decline")
                || status.equals("cancelled")
                || status.equals("canceled");
    }


    // =========================================================
    // FINAL APPLICATION STATUS LOGIC
    // =========================================================

    private String resolveApplicationStatus(
            String rawStatus
    ) {

        if (
                rawStatus == null
                        || rawStatus.trim().isEmpty()
        ) {

            return "";
        }


        String status =
                rawStatus
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        // -----------------------------------------------------
        // OLD PAYMENT VALUES ARE NOT APPLICATION STATUS
        // -----------------------------------------------------

        if (
                status.equals("paid")
                        || status.equals("verified")
                        || status.equals("payment verified")
                        || status.equals("payment_verified")
                        || status.equals("success")
                        || status.equals("successful")
                        || status.equals("unpaid")
                        || status.equals("rejected payment")
                        || status.equals("payment rejected")
                        || status.equals("payment_rejected")
        ) {

            return "Submitted";
        }


        // -----------------------------------------------------
        // SUBMITTED
        // -----------------------------------------------------

        if (
                status.equals("submitted")
                        || status.equals("submit")
                        || status.equals("pending")
        ) {

            return "Submitted";
        }


        // -----------------------------------------------------
        // UNDER REVIEW
        // -----------------------------------------------------

        if (
                status.equals("under review")
                        || status.equals("review")
                        || status.equals("processing")
                        || status.equals("in process")
        ) {

            return "Under Review";
        }


        // -----------------------------------------------------
        // APPROVED
        // -----------------------------------------------------

        if (
                status.equals("approved")
                        || status.equals("approve")
                        || status.equals("accepted")
                        || status.equals("accept")
        ) {

            return "Approved";
        }


        // -----------------------------------------------------
        // REJECTED
        // -----------------------------------------------------

        if (
                status.equals("rejected")
                        || status.equals("reject")
                        || status.equals("declined")
                        || status.equals("decline")
                        || status.equals("cancelled")
                        || status.equals("canceled")
        ) {

            return "Rejected";
        }


        return "Submitted";
    }


    // =========================================================
    // FINAL PAYMENT STATUS LOGIC
    // =========================================================

    private String resolvePaymentStatus(
            String rawPaymentStatus,
            boolean applicationSubmitted
    ) {

        /*
         * If the student has not submitted an application,
         * Payment Status must remain blank.
         */

        if (!applicationSubmitted) {
            return "";
        }

        /*
         * After application submission, payment remains
         * Pending until Admin changes it to Paid or Unpaid.
         */

        if (
                rawPaymentStatus == null
                        || rawPaymentStatus.trim().isEmpty()
        ) {

            return "Pending";
        }


        String payment =
                rawPaymentStatus
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        // -----------------------------------------------------
        // ADMIN VERIFIED
        // -----------------------------------------------------

        if (
                payment.equals("paid")
                        || payment.equals("verified")
                        || payment.equals("payment verified")
                        || payment.equals("payment_verified")
                        || payment.equals("confirmed")
                        || payment.equals("success")
                        || payment.equals("successful")
        ) {

            return "Paid";
        }


        // -----------------------------------------------------
        // ADMIN REJECTED
        // -----------------------------------------------------

        if (
                payment.equals("unpaid")
                        || payment.equals("rejected")
                        || payment.equals("reject")
                        || payment.equals("payment rejected")
                        || payment.equals("payment_rejected")
                        || payment.equals("failed")
                        || payment.equals("declined")
        ) {

            return "Unpaid";
        }


        // -----------------------------------------------------
        // UNKNOWN / DEFAULT
        // -----------------------------------------------------

        return "Pending";
    }


    // =========================================================
    // APPLY STATUS UI
    // =========================================================

    private void applyStatus(
            String status,
            String paymentStatus
    ) {

        if (
                status == null
                        || status.trim().isEmpty()
        ) {

            setText(
                    tvApplicationStatus,
                    ""
            );

            setText(
                    tvStatusBadge,
                    ""
            );

            if (tvStatusMessage != null) {
                tvStatusMessage.setText("");
            }

            setStep(
                    tvStepCreated,
                    true
            );

            setStep(
                    tvStepSubmitted,
                    false
            );

            setStep(
                    tvStepPayment,
                    false
            );

            setStep(
                    tvStepReview,
                    false
            );

            setStep(
                    tvStepDecision,
                    false
            );

            setStep(
                    tvStepResult,
                    false
            );

            setLine(lineCreated, false);
            setLine(lineSubmitted, false);
            setLine(linePayment, false);
            setLine(lineReview, false);
            setLine(lineDecision, false);

            return;
        }


        String normalized =
                status
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        // =====================================================
        // APPLICATION STATUS
        // =====================================================

        setText(
                tvApplicationStatus,
                status
        );


        setText(
                tvStatusBadge,
                "● " + status
        );


        // =====================================================
        // APPLICATION STATUS COLOR
        // =====================================================

        if (
                normalized.contains("reject")
                        || normalized.contains("declin")
                        || normalized.contains("cancel")
        ) {

            setColor(
                    tvApplicationStatus,
                    RED
            );


            setColor(
                    tvStatusBadge,
                    RED
            );

        } else if (
                normalized.contains("approv")
                        || normalized.contains("accept")
        ) {

            setColor(
                    tvApplicationStatus,
                    GREEN
            );


            setColor(
                    tvStatusBadge,
                    GREEN
            );

        } else {

            // Submitted / Under Review

            setColor(
                    tvApplicationStatus,
                    PENDING
            );


            setColor(
                    tvStatusBadge,
                    PENDING
            );
        }


        // =====================================================
        // STATUS MESSAGE
        // =====================================================

        if (tvStatusMessage != null) {

            if (
                    normalized.contains("reject")
                            || normalized.contains("declin")
                            || normalized.contains("cancel")
            ) {

                tvStatusMessage.setText(
                        "Your application was not approved."
                );

            } else if (
                    normalized.contains("approv")
                            || normalized.contains("accept")
            ) {

                tvStatusMessage.setText(
                        "Your application has been approved."
                );

            } else if (
                    normalized.contains("review")
                            || normalized.contains("process")
            ) {

                tvStatusMessage.setText(
                        "Your application is currently under review."
                );

            } else {

                tvStatusMessage.setText(
                        "Your application has been submitted successfully."
                );
            }
        }


        // =====================================================
        // TIMELINE
        // =====================================================

        boolean paymentPaid =
                "Paid".equalsIgnoreCase(
                        paymentStatus
                );


        boolean paymentUnpaid =
                "Unpaid".equalsIgnoreCase(
                        paymentStatus
                );


        boolean applicationRejected =
                normalized.contains(
                        "reject"
                )
                        || normalized.contains(
                        "declin"
                )
                        || normalized.contains(
                        "cancel"
                );


        boolean applicationApproved =
                normalized.contains(
                        "approv"
                )
                        || normalized.contains(
                        "accept"
                );


        boolean reviewDone =
                paymentPaid
                        || paymentUnpaid
                        || normalized.contains(
                        "review"
                )
                        || normalized.contains(
                        "process"
                )
                        || applicationApproved
                        || applicationRejected;


        boolean decisionDone =
                applicationApproved
                        || applicationRejected;


        setStep(
                tvStepCreated,
                true
        );


        setStep(
                tvStepSubmitted,
                true
        );


        setStep(
                tvStepPayment,
                paymentPaid || paymentUnpaid
        );


        setStep(
                tvStepReview,
                reviewDone
        );


        setStep(
                tvStepDecision,
                decisionDone
        );


        setStep(
                tvStepResult,
                false
        );


        setLine(
                lineCreated,
                true
        );


        setLine(
                lineSubmitted,
                paymentPaid || paymentUnpaid
        );


        setLine(
                linePayment,
                reviewDone
        );


        setLine(
                lineReview,
                decisionDone
        );


        setLine(
                lineDecision,
                false
        );
    }


    // =========================================================
    // ADMIT CARD + RESULT
    // =========================================================

    private void updateAdmissionUpdates(
            DocumentSnapshot d
    ) {

        boolean admitPublished =
                Boolean.TRUE.equals(
                        d.getBoolean(
                                "admitCardPublished"
                        )
                );


        boolean resultAvailable =
                Boolean.TRUE.equals(
                        d.getBoolean(
                                "resultAvailable"
                        )
                );


        // =====================================================
        // ADMIT CARD
        // =====================================================

        if (admitPublished) {

            setText(
                    tvAdmitCardStatus,
                    "Admit Card Available"
            );


            setText(
                    tvAdmitCardMessage,
                    "Your admit card has been published."
            );


            if (btnViewAdmitCard != null) {

                btnViewAdmitCard.setVisibility(
                        View.VISIBLE
                );
            }

        } else {

            setText(
                    tvAdmitCardStatus,
                    "Admit Card"
            );


            setText(
                    tvAdmitCardMessage,
                    "Admit card has not been published yet."
            );


            if (btnViewAdmitCard != null) {

                btnViewAdmitCard.setVisibility(
                        View.GONE
                );
            }
        }


        // =====================================================
        // RESULT
        // =====================================================

        if (resultAvailable) {

            setText(
                    tvResultStatus,
                    "Result Available"
            );


            setText(
                    tvResultMessage,
                    "Your admission result is available."
            );


            if (btnViewResult != null) {

                btnViewResult.setVisibility(
                        View.VISIBLE
                );
            }


            setStep(
                    tvStepResult,
                    true
            );


            setLine(
                    lineDecision,
                    true
            );

        } else {

            setText(
                    tvResultStatus,
                    "Result"
            );


            setText(
                    tvResultMessage,
                    "Admission result has not been published yet."
            );


            if (btnViewResult != null) {

                btnViewResult.setVisibility(
                        View.GONE
                );
            }
        }
    }


    // =========================================================
    // ADMISSION YEAR
    // =========================================================

    private String resolveAdmissionYear(
            DocumentSnapshot d
    ) {

        String year =
                value(
                        d,
                        "admissionYear",
                        ""
                );


        if (!year.isEmpty()) {

            return year;
        }


        String passingYear =
                value(
                        d,
                        "passingYear",
                        ""
                );


        if (!passingYear.isEmpty()) {

            try {

                return String.valueOf(
                        Integer.parseInt(
                                passingYear
                        ) + 1
                );

            } catch (Exception ignored) {
            }
        }


        return "2026";
    }


    // =========================================================
    // AMOUNT
    // =========================================================

    private String formatAmount(
            Object amount,
            Object oldAmount
    ) {

        Object object =
                amount != null
                        ? amount
                        : oldAmount;


        if (object == null) {

            return "Tk. 1,200.00";
        }


        try {

            double value =
                    Double.parseDouble(
                            String.valueOf(
                                    object
                            )
                    );


            return String.format(
                    Locale.US,
                    "Tk. %,.2f",
                    value
            );

        } catch (Exception e) {

            return "Tk. "
                    + String.valueOf(
                    object
            );
        }
    }


    // =========================================================
    // DATE
    // =========================================================

    private String formatDate(
            Object object,
            String fallback
    ) {

        if (
                object instanceof Timestamp
        ) {

            Date date =
                    ((Timestamp) object)
                            .toDate();


            return new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.US
            ).format(
                    date
            );
        }


        if (
                object instanceof Date
        ) {

            return new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.US
            ).format(
                    (Date) object
            );
        }


        if (
                object instanceof String
                        && !String.valueOf(
                        object
                ).trim().isEmpty()
        ) {

            return String.valueOf(
                    object
            );
        }


        return fallback;
    }


    // =========================================================
    // FIRESTORE STRING
    // =========================================================

    private String value(
            DocumentSnapshot d,
            String key,
            String fallback
    ) {

        String value =
                d.getString(
                        key
                );


        if (
                value == null
                        || value.trim().isEmpty()
        ) {

            return fallback;
        }


        return value.trim();
    }


    // =========================================================
    // FACULTY
    // =========================================================

    private String getFacultyForDepartment(
            String department
    ) {

        if (department == null) {

            return "Faculty of Engineering";
        }


        String d =
                department.toLowerCase(
                        Locale.ROOT
                );


        if (d.contains("civil")) {

            return "Faculty of Civil Engineering";
        }


        if (
                d.contains("electrical")
                        || d.contains("eee")
                        || d.contains("telecommunication")
        ) {

            return "Faculty of Electrical & Electronic Engineering";
        }


        if (
                d.contains("mechanical")
                        || d.contains("ipe")
                        || d.contains("mme")
                        || d.contains("chemical")
        ) {

            return "Faculty of Mechanical Engineering";
        }


        if (
                d.contains("computer")
                        || d.contains("cse")
        ) {

            return "Faculty of Computer Science & Engineering";
        }


        if (d.contains("textile")) {

            return "Faculty of Textile Engineering";
        }


        if (d.contains("architecture")) {

            return "Faculty of Architecture & Planning";
        }


        if (d.contains("food")) {

            return "Faculty of Food Engineering";
        }


        return "Faculty of Engineering";
    }


    // =========================================================
    // SAFE TEXT
    // =========================================================

    private void setText(
            TextView view,
            String text
    ) {

        if (view != null) {

            view.setText(
                    text == null
                            ? ""
                            : text
            );
        }
    }


    // =========================================================
    // SAFE COLOR
    // =========================================================

    private void setColor(
            TextView view,
            int color
    ) {

        if (view != null) {

            view.setTextColor(
                    color
            );
        }
    }


    // =========================================================
    // TIMELINE STEP
    // =========================================================

    private void setStep(
            TextView view,
            boolean done
    ) {

        if (view == null) {
            return;
        }


        view.setText(
                done
                        ? "✓"
                        : "•"
        );


        view.setTextColor(
                done
                        ? GREEN
                        : MUTED
        );


        view.setTypeface(
                Typeface.DEFAULT,
                done
                        ? Typeface.BOLD
                        : Typeface.NORMAL
        );


        view.setAlpha(
                done
                        ? 1.0f
                        : 0.55f
        );
    }


    // =========================================================
    // TIMELINE LINE
    // =========================================================

    private void setLine(
            View view,
            boolean active
    ) {

        if (view != null) {

            view.setAlpha(
                    active
                            ? 1.0f
                            : 0.30f
            );
        }
    }


    // =========================================================
    // ON RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (
                mAuth != null
                        && mAuth.getCurrentUser() != null
        ) {

            loadApplication();
        }
    }


    // =========================================================
    // ON STOP
    // =========================================================

    @Override
    protected void onStop() {

        clearListeners();


        super.onStop();
    }


    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        if (
                drawerLayout != null
                        && drawerLayout.isDrawerOpen(
                        GravityCompat.START
                )
        ) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );

            return;
        }

        super.onBackPressed();
    }
}