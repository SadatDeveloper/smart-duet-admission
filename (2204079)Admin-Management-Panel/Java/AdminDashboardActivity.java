package com.example.smartduetadmissionsystem;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private BottomNavigationView bottomNavigation;


    // =========================================================
    // ADMIN INFO
    // =========================================================

    private TextView tvAdminName;
    private TextView tvAdminEmail;
    private TextView tvAdminRole;
    private TextView tvLastLogin;


    // =========================================================
    // OVERVIEW
    // =========================================================

    private TextView tvTotalStudents;
    private TextView tvTotalApplications;
    private TextView tvPendingApplications;
    private TextView tvUnderReviewApplications;
    private TextView tvApprovedApplications;
    private TextView tvRejectedApplications;
    private TextView tvPendingPayments;
    private TextView tvVerifiedPayments;
    private TextView tvPublishedAdmitCards;
    private TextView tvPublishedResults;
    private TextView tvActiveNotices;


    // =========================================================
    // ACTION REQUIRED
    // =========================================================

    private TextView tvActionPending;
    private TextView tvActionPayments;
    private TextView tvActionReview;
    private TextView tvActionAdmitCards;
    private TextView tvActionResults;


    // =========================================================
    // ACTIVITY
    // =========================================================

    private LinearLayout activityContainer;
    private TextView tvActivityLoading;


    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SessionManager sessionManager;

    private ListenerRegistration usersListener;
    private ListenerRegistration applicationsListener;
    private ListenerRegistration noticesListener;
    private ListenerRegistration activityListener;
    private boolean dashboardListenersActive = false;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sessionManager = new SessionManager(this);

        initViews();

        setupHeader();
        setupDrawer();
        setupFooter();

        // =========================================================
        // ADMIN MANAGEMENT (ADDED - existing functionality unchanged)
        // =========================================================
        setupAdminManagementButton();

        loadAdminInfo();
    }


    // =========================================================
    // ON START
    // =========================================================

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() == null
                || !sessionManager.isAdminSession()) {

            goToLogin();
        }
    }


    // =========================================================
    // INIT VIEWS
    // =========================================================

    private void initViews() {

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        bottomNavigation = findViewById(R.id.bottomNavigation);


        // Admin info

        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminEmail = findViewById(R.id.tvAdminEmail);
        tvAdminRole = findViewById(R.id.tvAdminRole);
        tvLastLogin = findViewById(R.id.tvLastLogin);


        // Overview

        tvTotalStudents =
                findViewById(R.id.tvTotalStudents);

        tvTotalApplications =
                findViewById(R.id.tvTotalApplications);

        tvPendingApplications =
                findViewById(R.id.tvPendingApplications);

        tvUnderReviewApplications =
                findViewById(R.id.tvUnderReviewApplications);

        tvApprovedApplications =
                findViewById(R.id.tvApprovedApplications);

        tvRejectedApplications =
                findViewById(R.id.tvRejectedApplications);

        tvPendingPayments =
                findViewById(R.id.tvPendingPayments);

        tvVerifiedPayments =
                findViewById(R.id.tvVerifiedPayments);

        tvPublishedAdmitCards =
                findViewById(R.id.tvPublishedAdmitCards);

        tvPublishedResults =
                findViewById(R.id.tvPublishedResults);

        tvActiveNotices =
                findViewById(R.id.tvActiveNotices);


        // Action required

        tvActionPending =
                findViewById(R.id.tvActionPending);
        tvActionPending.setClickable(true);

        tvActionPayments =
                findViewById(R.id.tvActionPayments);
        tvActionPayments.setClickable(true);

        tvActionReview =
                findViewById(R.id.tvActionReview);
        tvActionReview.setClickable(true);

        tvActionAdmitCards =
                findViewById(R.id.tvActionAdmitCards);
        tvActionAdmitCards.setClickable(true);

        tvActionResults =
                findViewById(R.id.tvActionResults);
        tvActionResults.setClickable(true);

        setupActionClicks();


        // Activity

        activityContainer =
                findViewById(R.id.activityContainer);

        tvActivityLoading =
                findViewById(R.id.tvActivityLoading);
    }


    // =========================================================
    // ACTION REQUIRED NAVIGATION
    // =========================================================

    private void setupActionClicks() {

        tvActionPending.setOnClickListener(v ->
                openActivity(ApplicationManagementActivity.class)
        );

        tvActionPayments.setOnClickListener(v ->
                openActivity(PaymentVerificationActivity.class)
        );

        tvActionReview.setOnClickListener(v ->
                openActivity(ApplicationManagementActivity.class)
        );

        tvActionAdmitCards.setOnClickListener(v ->
                openActivity(AdmitCardManagementActivity.class)
        );

        tvActionResults.setOnClickListener(v ->
                openActivity(ResultManagementActivity.class)
        );
    }


    // =========================================================
    // HEADER
    // =========================================================

    private void setupHeader() {

        btnMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );


        View profile =
                findViewById(R.id.btnProfile);

        if (profile != null) {

            profile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(
                            AdminDashboardActivity.this,
                            AdminProfileActivity.class
                    );
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(
                            AdminDashboardActivity.this,
                            "Admin Profile is not available.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }
    }


    // =========================================================
    // ADMIN INFORMATION
    // =========================================================

    private void loadAdminInfo() {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }


        String uid = user.getUid();


        // Email

        String email = user.getEmail();

        if (email != null
                && !email.trim().isEmpty()) {

            tvAdminEmail.setText(email);

        } else {

            tvAdminEmail.setText("Not available");
        }


        // Last login

        if (user.getMetadata() != null) {

            long lastLogin =
                    user.getMetadata()
                            .getLastSignInTimestamp();

            if (lastLogin > 0) {

                tvLastLogin.setText(
                        "Last Login: "
                                + DateFormat.format(
                                "dd MMM yyyy, hh:mm a",
                                lastLogin
                        )
                );

            } else {

                tvLastLogin.setText(
                        "Last Login: Not available"
                );
            }

        } else {

            tvLastLogin.setText(
                    "Last Login: Not available"
            );
        }


        // Admin document

        db.collection("admins")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        logoutAndGoLogin(
                                "Admin account verification failed."
                        );

                        return;
                    }


                    Boolean active =
                            document.getBoolean("active");

                    String role =
                            document.getString("role");


                    boolean isActive =
                            Boolean.TRUE.equals(active);

                    boolean isAdmin =
                            role != null
                                    && role.equalsIgnoreCase("admin");


                    if (!isActive || !isAdmin) {

                        logoutAndGoLogin(
                                "Admin access is no longer available."
                        );

                        return;
                    }


                    String name =
                            document.getString("name");


                    if (name != null
                            && !name.trim().isEmpty()) {

                        tvAdminName.setText(name);

                    } else {

                        tvAdminName.setText("Admin");
                    }


                    if (role != null
                            && !role.trim().isEmpty()) {

                        if (role.equalsIgnoreCase("admin")) {

                            tvAdminRole.setText(
                                    "Administrator"
                            );

                        } else {

                            tvAdminRole.setText(role);
                        }

                    } else {

                        tvAdminRole.setText(
                                "Administrator"
                        );
                    }

                })
                .addOnFailureListener(e ->
                        logoutAndGoLogin(
                                "Unable to verify Admin account."
                        )
                );
    }


    // =========================================================
    // DASHBOARD STATISTICS - REALTIME FIRESTORE
    // =========================================================

    private void loadDashboardStatistics() {

        if (dashboardListenersActive) {
            return;
        }

        dashboardListenersActive = true;
        resetDashboardToLoading();

        usersListener = db.collection("users")
                .addSnapshotListener((userSnapshot, userError) -> {

                    if (userError != null || userSnapshot == null) {
                        tvTotalStudents.setText("--");
                        return;
                    }

                    tvTotalStudents.setText(
                            String.valueOf(userSnapshot.size())
                    );
                });

        applicationsListener = db.collection("applications")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null || snapshot == null) {
                        showDashboardDataError();
                        return;
                    }

                    int total = 0;
                    int pending = 0;
                    int underReview = 0;
                    int approved = 0;
                    int rejected = 0;

                    int pendingPayments = 0;
                    int verifiedPayments = 0;

                    int publishedAdmitCards = 0;
                    int publishedResults = 0;

                    int reviewRequired = 0;
                    int admitCardsRequired = 0;
                    int resultsRequired = 0;

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        total++;

                        String status = normalize(
                                document.getString("status")
                        );

                        if (isPendingStatus(status)) {
                            pending++;
                        }

                        if (isUnderReviewStatus(status)) {
                            underReview++;
                        }

                        if (isApprovedStatus(status)) {
                            approved++;
                        }

                        if (isRejectedStatus(status)) {
                            rejected++;
                        }

                        String paymentStatus = normalize(
                                document.getString("paymentStatus")
                        );

                        if (isPendingPayment(paymentStatus)) {
                            pendingPayments++;
                        }

                        if (isVerifiedPayment(paymentStatus)) {
                            verifiedPayments++;
                        }

                        if (isReviewRequired(status, paymentStatus)) {
                            reviewRequired++;
                        }

                        boolean admitPublished = getBoolean(
                                document,
                                "admitCardPublished"
                        );

                        if (admitPublished) {
                            publishedAdmitCards++;
                        } else if (isApprovedStatus(status)) {
                            admitCardsRequired++;
                        }

                        boolean resultAvailable = getBoolean(
                                document,
                                "resultAvailable"
                        );

                        boolean resultPublished = getBoolean(
                                document,
                                "resultPublished"
                        );

                        if (resultAvailable || resultPublished) {
                            publishedResults++;
                        } else if (
                                hasResultData(document)
                                        && isEligibleForResult(document, status)
                        ) {
                            resultsRequired++;
                        }
                    }

                    tvTotalApplications.setText(String.valueOf(total));
                    tvPendingApplications.setText(String.valueOf(pending));
                    tvUnderReviewApplications.setText(String.valueOf(underReview));
                    tvApprovedApplications.setText(String.valueOf(approved));
                    tvRejectedApplications.setText(String.valueOf(rejected));

                    tvPendingPayments.setText(String.valueOf(pendingPayments));
                    tvVerifiedPayments.setText(String.valueOf(verifiedPayments));

                    tvPublishedAdmitCards.setText(
                            String.valueOf(publishedAdmitCards)
                    );

                    tvPublishedResults.setText(
                            String.valueOf(publishedResults)
                    );

                    tvActionPending.setText(String.valueOf(pending));
                    tvActionPayments.setText(String.valueOf(pendingPayments));
                    tvActionReview.setText(String.valueOf(reviewRequired));
                    tvActionAdmitCards.setText(String.valueOf(admitCardsRequired));
                    tvActionResults.setText(String.valueOf(resultsRequired));
                });

        noticesListener = db.collection("notices")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null || snapshot == null) {
                        tvActiveNotices.setText("--");
                        return;
                    }

                    int activeCount = 0;

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        if (isNoticeActive(document)) {
                            activeCount++;
                        }
                    }

                    tvActiveNotices.setText(
                            String.valueOf(activeCount)
                    );
                });

        /*
         * REALTIME ADMIN ACTIVITY
         *
         * The existing logger uses Firestore serverTimestamp().
         * While that value is resolving, timestamp can temporarily be null.
         * Therefore this listener reads the activity collection and sorts
         * locally, so a newly created activity appears immediately.
         */
        activityListener = db.collection("admin_activity_logs")
                .addSnapshotListener((snapshot, error) -> {

                    activityContainer.removeAllViews();
                    tvActivityLoading.setVisibility(View.GONE);

                    if (error != null || snapshot == null) {
                        TextView errorText = createActivityText(
                                "Unable to load recent activity.",
                                "#64748B",
                                13,
                                false
                        );
                        errorText.setGravity(Gravity.CENTER);
                        errorText.setPadding(0, 18, 0, 18);
                        activityContainer.addView(errorText);
                        return;
                    }

                    if (snapshot.isEmpty()) {
                        TextView empty = createActivityText(
                                "No admin activity yet.",
                                "#64748B",
                                13,
                                false
                        );
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(0, 18, 0, 18);
                        activityContainer.addView(empty);
                        return;
                    }

                    java.util.List<DocumentSnapshot> activityDocs =
                            new java.util.ArrayList<>(snapshot.getDocuments());

                    java.util.Collections.sort(
                            activityDocs,
                            (a, b) -> {
                                Date da = getDate(a, "timestamp");
                                Date dbDate = getDate(b, "timestamp");

                                if (da == null && dbDate == null) {
                                    Long ca = getLong(a, "clientTimestamp");
                                    Long cb = getLong(b, "clientTimestamp");

                                    if (ca != null && cb != null) {
                                        return Long.compare(cb, ca);
                                    }

                                    return b.getId().compareTo(a.getId());
                                }

                                if (da == null) return -1;
                                if (dbDate == null) return 1;

                                return Long.compare(
                                        dbDate.getTime(),
                                        da.getTime()
                                );
                            }
                    );

                    int count = Math.min(5, activityDocs.size());

                    for (int i = 0; i < count; i++) {
                        addActivityItem(activityDocs.get(i));
                    }
                });
    }


    // =========================================================
    // STOP REALTIME LISTENERS
    // =========================================================

    private void stopDashboardListeners() {

        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }

        if (applicationsListener != null) {
            applicationsListener.remove();
            applicationsListener = null;
        }

        if (noticesListener != null) {
            noticesListener.remove();
            noticesListener = null;
        }

        if (activityListener != null) {
            activityListener.remove();
            activityListener = null;
        }

        dashboardListenersActive = false;
    }


    // =========================================================
    // NOTICE STATUS
    // =========================================================

    private boolean isNoticeActive(
            DocumentSnapshot document) {

        if (document.contains("active")) {

            Boolean active =
                    document.getBoolean("active");

            if (active != null) {
                return active;
            }
        }


        if (document.contains("isActive")) {

            Boolean active =
                    document.getBoolean("isActive");

            if (active != null) {
                return active;
            }
        }


        if (document.contains("published")) {

            Boolean published =
                    document.getBoolean("published");

            if (published != null) {
                return published;
            }
        }


        String status =
                normalize(
                        document.getString("status")
                );


        if (status.equals("active")
                || status.equals("published")
                || status.equals("publish")) {

            return true;
        }


        if (!document.contains("active")
                && !document.contains("isActive")
                && !document.contains("published")
                && !document.contains("status")) {

            return true;
        }


        return false;
    }


    // =========================================================
    // PENDING STATUS
    // =========================================================

    private boolean isPendingStatus(
            String status) {

        return status.equals("pending")
                || status.equals("submitted")
                || status.equals("application_submitted")
                || status.equals("application submitted")
                || status.equals("awaiting_review")
                || status.equals("awaiting review");
    }


    // =========================================================
    // UNDER REVIEW
    // =========================================================

    private boolean isUnderReviewStatus(
            String status) {

        return status.equals("under_review")
                || status.equals("under review")
                || status.equals("review")
                || status.equals("processing")
                || status.equals("in_review")
                || status.equals("in review");
    }


    // =========================================================
    // APPROVED
    // =========================================================

    private boolean isApprovedStatus(
            String status) {

        return status.equals("approved")
                || status.equals("approve")
                || status.equals("accepted")
                || status.equals("accept");
    }


    // =========================================================
    // REJECTED
    // =========================================================

    private boolean isRejectedStatus(
            String status) {

        return status.equals("rejected")
                || status.equals("reject")
                || status.equals("declined")
                || status.equals("decline");
    }


    // =========================================================
    // PENDING PAYMENT
    // =========================================================

    private boolean isPendingPayment(
            String paymentStatus) {

        return paymentStatus.equals("pending")
                || paymentStatus.equals("payment_pending")
                || paymentStatus.equals("payment pending")
                || paymentStatus.equals("awaiting")
                || paymentStatus.equals("awaiting_verification")
                || paymentStatus.equals("awaiting verification")
                || paymentStatus.equals("unverified");
    }


    // =========================================================
    // VERIFIED PAYMENT
    // =========================================================

    private boolean isVerifiedPayment(
            String paymentStatus) {

        return paymentStatus.equals("verified")
                || paymentStatus.equals("payment_verified")
                || paymentStatus.equals("payment verified")
                || paymentStatus.equals("paid")
                || paymentStatus.equals("success")
                || paymentStatus.equals("successful")
                || paymentStatus.equals("confirmed");
    }


    // =========================================================
    // REVIEW REQUIRED
    // =========================================================

    private boolean isReviewRequired(
            String status,
            String paymentStatus) {

        if (isApprovedStatus(status)
                || isRejectedStatus(status)) {

            return false;
        }


        if (isUnderReviewStatus(status)) {

            return true;
        }


        if (isVerifiedPayment(paymentStatus)
                && (
                isPendingStatus(status)
                        || status.isEmpty()
        )) {

            return true;
        }


        return false;
    }


    // =========================================================
    // RESULT DATA
    // =========================================================

    private boolean hasResultData(
            DocumentSnapshot document) {

        String[] fields = {
                "result",
                "resultStatus",
                "marks",
                "totalMarks",
                "meritPosition",
                "resultPublished",
                "resultAvailable"
        };


        for (String field : fields) {

            if (document.contains(field)
                    && document.get(field) != null) {

                return true;
            }
        }


        return false;
    }


    // =========================================================
    // RESULT ELIGIBILITY
    // =========================================================

    private boolean isEligibleForResult(
            DocumentSnapshot document,
            String status) {

        if (isApprovedStatus(status)) {

            return true;
        }


        String resultStatus =
                normalize(
                        document.getString(
                                "resultStatus"
                        )
                );


        return resultStatus.equals("ready")
                || resultStatus.equals("pending")
                || resultStatus.equals("published")
                || getBoolean(document, "resultAvailable")
                || getBoolean(document, "resultPublished");
    }


    // =========================================================
    // GET BOOLEAN
    // =========================================================

    private boolean getBoolean(
            DocumentSnapshot document,
            String field) {

        Boolean value =
                document.getBoolean(field);

        return Boolean.TRUE.equals(value);
    }


    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalize(
            String value) {

        if (value == null) {

            return "";
        }


        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "_");
    }


    // =========================================================
    // LOADING
    // =========================================================

    private void resetDashboardToLoading() {

        tvTotalStudents.setText("...");

        tvTotalApplications.setText("...");

        tvPendingApplications.setText("...");

        tvUnderReviewApplications.setText("...");

        tvApprovedApplications.setText("...");

        tvRejectedApplications.setText("...");

        tvPendingPayments.setText("...");

        tvVerifiedPayments.setText("...");

        tvPublishedAdmitCards.setText("...");

        tvPublishedResults.setText("...");

        tvActiveNotices.setText("...");


        tvActionPending.setText("...");

        tvActionPayments.setText("...");

        tvActionReview.setText("...");

        tvActionAdmitCards.setText("...");

        tvActionResults.setText("...");
    }


    // =========================================================
    // DATA ERROR
    // =========================================================

    private void showDashboardDataError() {

        tvTotalApplications.setText("--");

        tvPendingApplications.setText("--");

        tvUnderReviewApplications.setText("--");

        tvApprovedApplications.setText("--");

        tvRejectedApplications.setText("--");

        tvPendingPayments.setText("--");

        tvVerifiedPayments.setText("--");

        tvPublishedAdmitCards.setText("--");

        tvPublishedResults.setText("--");


        tvActionPending.setText("--");

        tvActionPayments.setText("--");

        tvActionReview.setText("--");

        tvActionAdmitCards.setText("--");

        tvActionResults.setText("--");


        Toast.makeText(
                this,
                "Unable to load application statistics.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // ACTIVITY ITEM
    // =========================================================

    private void addActivityItem(
            DocumentSnapshot document) {

        LinearLayout item =
                new LinearLayout(this);

        item.setOrientation(
                LinearLayout.VERTICAL
        );

        item.setPadding(
                4,
                10,
                4,
                10
        );


        String action =
                document.getString("action");


        if (action == null
                || action.trim().isEmpty()) {

            action = "Admin Activity";
        }


        TextView actionText =
                createActivityText(
                        action,
                        "#173F6B",
                        14,
                        true
                );

        item.addView(actionText);


        String adminName =
                document.getString("adminName");


        if (adminName != null
                && !adminName.trim().isEmpty()) {

            TextView admin =
                    createActivityText(
                            "Admin: " + adminName,
                            "#475569",
                            11,
                            false
                    );

            admin.setPadding(
                    0,
                    3,
                    0,
                    0
            );

            item.addView(admin);
        }


        String applicationId =
                document.getString(
                        "applicationId"
                );


        if (applicationId != null
                && !applicationId.trim().isEmpty()) {

            TextView application =
                    createActivityText(
                            "Application: "
                                    + applicationId,
                            "#475569",
                            12,
                            false
                    );

            application.setPadding(
                    0,
                    3,
                    0,
                    0
            );

            item.addView(application);
        }


        String description =
                document.getString(
                        "description"
                );


        if (description != null
                && !description.trim().isEmpty()) {

            TextView details =
                    createActivityText(
                            description,
                            "#64748B",
                            12,
                            false
                    );

            details.setPadding(
                    0,
                    3,
                    0,
                    0
            );

            item.addView(details);
        }


        Date timestamp =
                getDate(
                        document,
                        "timestamp"
                );


        if (timestamp != null) {

            String time =
                    DateFormat.format(
                            "dd MMM yyyy, hh:mm a",
                            timestamp.getTime()
                    ).toString();


            TextView timeText =
                    createActivityText(
                            time,
                            "#94A3B8",
                            10,
                            false
                    );

            timeText.setPadding(
                    0,
                    4,
                    0,
                    0
            );

            item.addView(timeText);
        }


        View divider =
                new View(this);

        divider.setBackgroundColor(
                Color.parseColor("#E2E8F0")
        );


        LinearLayout.LayoutParams
                dividerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );


        dividerParams.topMargin = 6;


        item.addView(
                divider,
                dividerParams
        );


        activityContainer.addView(item);
    }


    // =========================================================
    // LONG VALUE HELPER
    // =========================================================

    private Long getLong(
            DocumentSnapshot document,
            String field) {

        Object value = document.get(field);

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return null;
    }


    // =========================================================
    // DATE
    // =========================================================

    private Date getDate(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);


        if (value instanceof Timestamp) {

            return ((Timestamp) value).toDate();
        }


        if (value instanceof Date) {

            return (Date) value;
        }

        if (value instanceof Long) {

            return new Date((Long) value);
        }

        if (value instanceof Number) {

            return new Date(((Number) value).longValue());
        }

        return null;
    }


    // =========================================================
    // ACTIVITY TEXT
    // =========================================================

    private TextView createActivityText(
            String text,
            String color,
            float size,
            boolean bold) {

        TextView textView =
                new TextView(this);

        textView.setText(text);

        textView.setTextColor(
                Color.parseColor(color)
        );

        textView.setTextSize(size);


        if (bold) {

            textView.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }


        return textView;
    }


    // =========================================================
    // ADMIN MANAGEMENT BUTTON (ADDED)
    // =========================================================

    private void setupAdminManagementButton() {

        /*
         * Optional dashboard button support.
         *
         * If activity_admin_dashboard.xml contains:
         *     @+id/btnAdminManagement
         * this listener will automatically connect it.
         *
         * getIdentifier() is intentionally used so the existing
         * dashboard keeps compiling even before that ID is added.
         */
        int buttonId = getResources().getIdentifier(
                "btnAdminManagement",
                "id",
                getPackageName()
        );

        if (buttonId != 0) {

            View adminManagementButton =
                    findViewById(buttonId);

            if (adminManagementButton != null) {

                adminManagementButton.setOnClickListener(v ->
                        openActivity(
                                AdminManagementActivity.class
                        )
                );
            }
        }
    }


    // =========================================================
    // DRAWER
    // =========================================================

    private void setupDrawer() {

        navigationView.setNavigationItemSelectedListener(
                item -> {

                    String title = "";

                    if (item.getTitle() != null) {

                        title = item.getTitle()
                                .toString()
                                .trim()
                                .toLowerCase(
                                        Locale.ROOT
                                );
                    }


                    // =================================================
                    // DASHBOARD
                    // =================================================

                    if (containsAny(
                            title,
                            "dashboard",
                            "home"
                    )) {

                        drawerLayout.closeDrawer(
                                navigationView
                        );

                        loadAdminInfo();
                        loadDashboardStatistics();

                        return true;
                    }


                    // =================================================
                    // APPLICATIONS
                    // =================================================

                    if (containsAny(
                            title,
                            "application",
                            "applications"
                    )) {

                        openActivity(
                                ApplicationManagementActivity.class
                        );

                        return true;
                    }


                    // =================================================
                    // PAYMENTS
                    // =================================================

                    if (containsAny(
                            title,
                            "payment",
                            "payments"
                    )) {

                        openActivity(
                                PaymentVerificationActivity.class
                        );

                        return true;
                    }


                    // =================================================
                    // ADMIT CARD
                    // =================================================

                    if (containsAny(
                            title,
                            "admit",
                            "admit card",
                            "admit cards"
                    )) {

                        openActivity(
                                AdmitCardManagementActivity.class
                        );

                        return true;
                    }


                    // =================================================
                    // RESULT
                    // =================================================

                    if (containsAny(
                            title,
                            "result",
                            "results"
                    )) {

                        openActivity(
                                ResultManagementActivity.class
                        );

                        return true;
                    }


                    // =================================================
                    // NOTICE
                    // =================================================

                    if (containsAny(
                            title,
                            "notice",
                            "notices"
                    )) {

                        openActivity(
                                NoticeManagementActivity.class
                        );

                        return true;
                    }


                    // =================================================
                    // SYLLABUS
                    // =================================================

                    if (item.getItemId() == R.id.admin_syllabus
                            || containsAny(
                            title,
                            "syllabus",
                            "syllabuses"
                    )) {

                        openActivity(
                                SyllabusManagementActivity.class
                        );

                        return true;
                    }


                    // =================================================
                    // ACTIVITY LOG
                    // =================================================

                    if (containsAny(
                            title,
                            "activity",
                            "activity log",
                            "logs"
                    )) {

                        openActivity(
                                AdminActivityActivity.class
                        );

                        return true;
                    }


                    // =================================================
                    // ADMIN MANAGEMENT (ADDED)
                    // =================================================

                    if (containsAny(
                            title,
                            "admin management",
                            "manage admin",
                            "admin access"
                    )) {

                        openActivity(
                                AdminManagementActivity.class
                        );

                        return true;
                    }


                    // =================================================
                    // LOGOUT
                    // =================================================

                    if (containsAny(
                            title,
                            "logout",
                            "log out",
                            "sign out"
                    )) {

                        logoutAdmin();

                        return true;
                    }


                    drawerLayout.closeDrawer(
                            navigationView
                    );

                    return true;
                }
        );
    }


    // =========================================================
    // TITLE MATCH
    // =========================================================

    private boolean containsAny(
            String value,
            String... keywords) {

        if (value == null) {
            return false;
        }


        String lower =
                value.toLowerCase(
                        Locale.ROOT
                );


        for (String keyword : keywords) {

            if (lower.contains(
                    keyword.toLowerCase(
                            Locale.ROOT
                    )
            )) {

                return true;
            }
        }


        return false;
    }


    // =========================================================
    // OPEN ACTIVITY
    // =========================================================

    private void openActivity(
            Class<?> targetActivity) {

        try {

            Intent intent =
                    new Intent(
                            AdminDashboardActivity.this,
                            targetActivity
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "This section is not available yet.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // FOOTER
    // =========================================================

    private void setupFooter() {

        bottomNavigation.setOnItemSelectedListener(
                item -> {

                    String title = "";

                    if (item.getTitle() != null) {

                        title = item.getTitle()
                                .toString()
                                .trim()
                                .toLowerCase(
                                        Locale.ROOT
                                );
                    }


                    if (containsAny(
                            title,
                            "dashboard",
                            "home"
                    )) {

                        loadAdminInfo();
                        loadDashboardStatistics();
                    }


                    return true;
                }
        );
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    private void logoutAdmin() {

        sessionManager.clearAdminSession();

        sessionManager.clearStudentSession();

        mAuth.signOut();


        Toast.makeText(
                this,
                "Admin logged out successfully",
                Toast.LENGTH_SHORT
        ).show();


        goToLogin();
    }


    // =========================================================
    // LOGOUT + LOGIN
    // =========================================================

    private void logoutAndGoLogin(
            String message) {

        sessionManager.clearAdminSession();

        mAuth.signOut();


        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();


        goToLogin();
    }


    // =========================================================
    // GO TO ADMIN LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent =
                new Intent(
                        AdminDashboardActivity.this,
                        AdminLoginActivity.class
                );


        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(intent);

        finish();
    }


    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (mAuth != null
                && mAuth.getCurrentUser() != null
                && sessionManager != null
                && sessionManager.isAdminSession()) {

            loadAdminInfo();
            loadDashboardStatistics();
        }
    }


    @Override
    protected void onStop() {

        stopDashboardListeners();

        super.onStop();
    }


    // =========================================================
    // BACK
    // =========================================================

    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(
                navigationView
        )) {

            drawerLayout.closeDrawer(
                    navigationView
            );

        } else {

            super.onBackPressed();
        }
    }
}