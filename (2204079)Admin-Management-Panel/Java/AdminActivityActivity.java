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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminActivityActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private BottomNavigationView bottomNavigation;

    private TextView tvAdminName;
    private TextView tvAdminEmail;
    private TextView tvAdminRole;
    private TextView tvLastLogin;

    private LinearLayout activityContainer;
    private TextView tvLoading;
    private TextView tvActivityCount;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private ListenerRegistration activityListener;
    private boolean listenerActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_activity_log);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupHeader();
        setupDrawer();
        setupFooter();
        loadAdminInfo();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null || !sessionManager.isAdminSession()) {
            goToLogin();
            return;
        }

        startRealtimeActivityListener();
    }

    @Override
    protected void onStop() {
        stopRealtimeActivityListener();
        super.onStop();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminEmail = findViewById(R.id.tvAdminEmail);
        tvAdminRole = findViewById(R.id.tvAdminRole);
        tvLastLogin = findViewById(R.id.tvLastLogin);

        activityContainer = findViewById(R.id.activityContainer);
        tvLoading = findViewById(R.id.tvActivityLoading);
        tvActivityCount = findViewById(R.id.tvActivityCount);
    }

    private void setupHeader() {
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v ->
                    drawerLayout.openDrawer(navigationView)
            );
        }

        View profile = findViewById(R.id.btnProfile);

        if (profile != null) {
            profile.setOnClickListener(v ->
                    Toast.makeText(
                            AdminActivityActivity.this,
                            "Admin Profile",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }

    private void setupDrawer() {
        if (navigationView == null || drawerLayout == null) return;

        navigationView.setNavigationItemSelectedListener(item -> {
            String title = item.getTitle() == null
                    ? ""
                    : item.getTitle().toString()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (containsAny(title, "dashboard", "home")) {
                openActivity(AdminDashboardActivity.class);
                return true;
            }

            if (containsAny(title, "application", "applications")) {
                openActivity(ApplicationManagementActivity.class);
                return true;
            }

            if (containsAny(title, "payment", "payments")) {
                openActivity(PaymentVerificationActivity.class);
                return true;
            }

            if (containsAny(title, "admit", "admit card", "admit cards")) {
                openActivity(AdmitCardManagementActivity.class);
                return true;
            }

            if (containsAny(title, "result", "results")) {
                openActivity(ResultManagementActivity.class);
                return true;
            }

            if (containsAny(title, "notice", "notices")) {
                openActivity(NoticeManagementActivity.class);
                return true;
            }

            if (containsAny(title, "department", "departments")) {
                openActivity(DepartmentManagementActivity.class);
                return true;
            }

            if (containsAny(title, "activity", "activity log", "logs")) {
                drawerLayout.closeDrawer(navigationView);
                return true;
            }

            if (containsAny(title, "logout", "log out", "sign out")) {
                logoutAdmin();
                return true;
            }

            drawerLayout.closeDrawer(navigationView);
            return true;
        });
    }

    private void setupFooter() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            String title = item.getTitle() == null
                    ? ""
                    : item.getTitle().toString()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (containsAny(title, "dashboard", "home")) {
                openActivity(AdminDashboardActivity.class);
            } else if (containsAny(title, "application", "applications")) {
                openActivity(ApplicationManagementActivity.class);
            } else if (containsAny(title, "payment", "payments")) {
                openActivity(PaymentVerificationActivity.class);
            } else if (containsAny(title, "admit", "admit card", "admit cards")) {
                openActivity(AdmitCardManagementActivity.class);
            } else if (containsAny(title, "result", "results")) {
                openActivity(ResultManagementActivity.class);
            } else if (containsAny(title, "activity", "logs")) {
                // Already on Activity Log.
            }

            return true;
        });
    }

    private void loadAdminInfo() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        String email = user.getEmail();
        tvAdminEmail.setText(
                email == null || email.trim().isEmpty()
                        ? "Not available"
                        : email
        );

        if (user.getMetadata() != null
                && user.getMetadata().getLastSignInTimestamp() > 0) {

            tvLastLogin.setText(
                    "Last Login: " +
                            DateFormat.format(
                                    "dd MMM yyyy, hh:mm a",
                                    user.getMetadata().getLastSignInTimestamp()
                            )
            );
        } else {
            tvLastLogin.setText("Last Login: Not available");
        }

        db.collection("admins")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        logoutAndGoLogin(
                                "Admin account verification failed."
                        );
                        return;
                    }

                    Boolean active = document.getBoolean("active");
                    String role = document.getString("role");

                    if (!Boolean.TRUE.equals(active)
                            || role == null
                            || !role.equalsIgnoreCase("admin")) {

                        logoutAndGoLogin(
                                "Admin access is no longer available."
                        );
                        return;
                    }

                    String name = document.getString("name");

                    tvAdminName.setText(
                            name == null || name.trim().isEmpty()
                                    ? "Admin"
                                    : name
                    );

                    tvAdminRole.setText("Administrator");
                })
                .addOnFailureListener(e ->
                        logoutAndGoLogin(
                                "Unable to verify Admin account."
                        )
                );
    }

    private void startRealtimeActivityListener() {
        if (listenerActive) return;

        listenerActive = true;
        tvLoading.setVisibility(View.VISIBLE);

        /*
         * Read the activity collection directly and sort locally.
         * This keeps newly created logs visible even while a
         * serverTimestamp is still resolving.
         */
        activityListener = db.collection("admin_activity_logs")
                .addSnapshotListener((snapshot, error) -> {

                    tvLoading.setVisibility(View.GONE);

                    if (error != null || snapshot == null) {
                        tvActivityCount.setText("Activity Log");
                        showMessage("Unable to load activity log.");
                        return;
                    }

                    if (snapshot.isEmpty()) {
                        tvActivityCount.setText("Activity Log • 0");
                        showMessage("No admin activity yet.");
                        return;
                    }

                    List<DocumentSnapshot> docs =
                            new ArrayList<>(snapshot.getDocuments());

                    Collections.sort(
                            docs,
                            new Comparator<DocumentSnapshot>() {
                                @Override
                                public int compare(
                                        DocumentSnapshot a,
                                        DocumentSnapshot b) {

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
                            }
                    );

                    tvActivityCount.setText(
                            "Activity Log • " + docs.size()
                    );

                    activityContainer.removeAllViews();

                    for (DocumentSnapshot document : docs) {
                        addActivityItem(document);
                    }
                });
    }

    private void stopRealtimeActivityListener() {
        if (activityListener != null) {
            activityListener.remove();
            activityListener = null;
        }

        listenerActive = false;
    }

    private void addActivityItem(DocumentSnapshot document) {
        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setStrokeColor(Color.parseColor("#E2E8F0"));
        card.setStrokeWidth(1);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));

        String action = getString(document, "action");
        if (action.isEmpty()) action = "Admin Activity";

        content.addView(
                createText(action, "#173F6B", 15, true)
        );

        String adminName = getString(document, "adminName");
        if (!adminName.isEmpty()) {
            content.addView(
                    createText(
                            "Admin: " + adminName,
                            "#475569",
                            12,
                            false
                    )
            );
        }

        String adminEmail = getString(document, "adminEmail");
        if (!adminEmail.isEmpty()) {
            content.addView(
                    createText(
                            "Email: " + adminEmail,
                            "#64748B",
                            11,
                            false
                    )
            );
        }

        String applicationId =
                getString(document, "applicationId");

        if (!applicationId.isEmpty()) {
            content.addView(
                    createText(
                            "Application: " + applicationId,
                            "#475569",
                            12,
                            false
                    )
            );
        }

        String description =
                getString(document, "description");

        if (!description.isEmpty()) {
            TextView descriptionView =
                    createText(
                            description,
                            "#64748B",
                            12,
                            false
                    );

            descriptionView.setPadding(
                    0, dp(5), 0, 0
            );

            content.addView(descriptionView);
        }

        Date timestamp = getDate(document, "timestamp");

        TextView timeView;

        if (timestamp != null) {
            timeView = createText(
                    DateFormat.format(
                            "dd MMM yyyy, hh:mm a",
                            timestamp.getTime()
                    ).toString(),
                    "#94A3B8",
                    10,
                    false
            );
        } else {
            Long clientTimestamp =
                    getLong(document, "clientTimestamp");

            if (clientTimestamp != null) {
                timeView = createText(
                        DateFormat.format(
                                "dd MMM yyyy, hh:mm a",
                                clientTimestamp
                        ).toString(),
                        "#94A3B8",
                        10,
                        false
                );
            } else {
                timeView = createText(
                        "Saving timestamp…",
                        "#94A3B8",
                        10,
                        false
                );
            }
        }

        timeView.setPadding(0, dp(7), 0, 0);
        content.addView(timeView);

        card.addView(content);
        activityContainer.addView(card);
    }

    private void showMessage(String message) {
        activityContainer.removeAllViews();

        TextView empty =
                createText(message, "#64748B", 13, false);

        empty.setGravity(Gravity.CENTER);
        empty.setPadding(
                dp(10), dp(45), dp(10), dp(45)
        );

        activityContainer.addView(empty);
    }

    private TextView createText(
            String text,
            String color,
            float size,
            boolean bold) {

        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.parseColor(color));
        view.setTextSize(size);

        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }

        view.setPadding(0, dp(2), 0, dp(2));

        return view;
    }

    private Date getDate(
            DocumentSnapshot document,
            String field) {

        Object value = document.get(field);

        if (value instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) value).toDate();
        }

        if (value instanceof java.util.Date) {
            return (Date) value;
        }

        if (value instanceof Long) {
            return new Date((Long) value);
        }

        return null;
    }

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

    private String getString(
            DocumentSnapshot document,
            String field) {

        Object value = document.get(field);

        return value == null
                ? ""
                : String.valueOf(value).trim();
    }

    private boolean containsAny(
            String value,
            String... keywords) {

        if (value == null) return false;

        String lower =
                value.toLowerCase(Locale.ROOT);

        for (String keyword : keywords) {
            if (lower.contains(
                    keyword.toLowerCase(Locale.ROOT)
            )) {
                return true;
            }
        }

        return false;
    }

    private void openActivity(Class<?> target) {
        try {
            startActivity(
                    new Intent(
                            AdminActivityActivity.this,
                            target
                    )
            );

            if (drawerLayout != null
                    && navigationView != null) {
                drawerLayout.closeDrawer(navigationView);
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "This section is not available yet.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void logoutAdmin() {
        sessionManager.clearAdminSession();
        sessionManager.clearStudentSession();
        mAuth.signOut();
        goToLogin();
    }

    private void logoutAndGoLogin(String message) {
        sessionManager.clearAdminSession();
        sessionManager.clearStudentSession();
        mAuth.signOut();

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        goToLogin();
    }

    private void goToLogin() {
        Intent intent =
                new Intent(
                        this,
                        AdminLoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}
