package com.example.smartduetadmissionsystem;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    private ScrollView scrollView;

    private TextView tvUserName;
    private TextView tvApplicationStatus;
    private TextView tvApplicationStatusBadge;
    private TextView tvEligibilityStatus;
    private TextView tvEligibilityStatusBadge;
    private TextView tvApplicationId;
    private TextView tvAdmissionResult;

    private TextView tvYourApplicationNo;
    private TextView tvYourApplicationDepartment;
    private TextView tvYourApplicationTechnology;
    private TextView tvYourApplicationQuota;

    private View cardApplicationStatus;
    private View cardAdmissionResult;
    private View btnViewResult;
    private View btnViewApplicationStatus;
    private View btnViewApplicationResult;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration applicationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClicks();
        clearDashboardValues();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);

        tvApplicationStatus = findViewById(R.id.tvApplicationStatus);
        tvApplicationStatusBadge = findViewById(R.id.tvApplicationStatusBadge);
        tvEligibilityStatus = findViewById(R.id.tvEligibilityStatus);
        tvEligibilityStatusBadge = findViewById(R.id.tvEligibilityStatusBadge);
        tvApplicationId = findViewById(R.id.tvApplicationId);
        tvAdmissionResult = findViewById(R.id.tvAdmissionResult);

        tvYourApplicationNo = findViewById(R.id.tvYourApplicationNo);
        tvYourApplicationDepartment = findViewById(R.id.tvYourApplicationDepartment);
        tvYourApplicationTechnology = findViewById(R.id.tvYourApplicationTechnology);
        tvYourApplicationQuota = findViewById(R.id.tvYourApplicationQuota);

        cardApplicationStatus = findViewById(R.id.cardApplicationStatus);
        cardAdmissionResult = findViewById(R.id.cardAdmissionResult);

        btnViewResult = findViewById(R.id.btnViewResult);
        btnViewApplicationStatus = findViewById(R.id.btnViewApplicationStatus);
        btnViewApplicationResult = findViewById(R.id.btnViewApplicationResult);

        View content = findViewById(android.R.id.content);
        if (content instanceof ScrollView) {
            scrollView = (ScrollView) content;
        } else if (cardApplicationStatus != null) {
            scrollView = findScrollView(cardApplicationStatus);
        }
    }

    private ScrollView findScrollView(View view) {
        android.view.ViewParent parent = view.getParent();
        while (parent instanceof View) {
            if (parent instanceof ScrollView) {
                return (ScrollView) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void setupClicks() {
        if (btnViewApplicationStatus != null) {
            btnViewApplicationStatus.setOnClickListener(
                    v -> scrollTo(cardApplicationStatus)
            );
        }

        if (btnViewApplicationResult != null) {
            btnViewApplicationResult.setOnClickListener(
                    v -> scrollTo(cardAdmissionResult)
            );
        }

        if (btnViewResult != null) {
            btnViewResult.setOnClickListener(
                    v -> scrollTo(cardAdmissionResult)
            );
        }
    }

    private void scrollTo(@Nullable View target) {
        if (target == null) return;

        target.post(() -> {
            if (scrollView != null) {
                scrollView.smoothScrollTo(
                        0,
                        Math.max(0, target.getTop() - 16)
                );
            } else {
                target.requestRectangleOnScreen(
                        new Rect(
                                0, 0,
                                target.getWidth(),
                                target.getHeight()
                        ),
                        true
                );
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadStudentApplication();
    }

    private void loadStudentApplication() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            clearDashboardValues();
            return;
        }

        if (user.getDisplayName() != null
                && !user.getDisplayName().trim().isEmpty()) {
            setText(tvUserName, user.getDisplayName().trim());
        } else {
            setText(tvUserName, "Student");
        }

        final String uid = user.getUid();

        /*
         * EXACT PRIMARY SOURCE USED BY APPLICATION TRACKER:
         *
         * Firestore
         * applications/{FirebaseAuth.currentUser.uid}
         *
         * This listener is realtime, so Admin changes to status/result
         * immediately reach the student's dashboard.
         */
        attachListener(uid);

        /*
         * If an older project version stored the application under another
         * document ID, find it without changing the primary data model.
         */
        db.collection("applications")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        findLegacyApplication(uid, user.getEmail());
                    }
                });
    }

    private void findLegacyApplication(
            String uid,
            @Nullable String email
    ) {
        db.collection("applicationId")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        bindApplication(document);
                    } else {
                        findByOwnerField(uid, email);
                    }
                })
                .addOnFailureListener(e ->
                        findByOwnerField(uid, email)
                );
    }

    private void findByOwnerField(
            String uid,
            @Nullable String email
    ) {
        db.collection("applications")
                .whereEqualTo("userId", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        attachListener(snapshot.getDocuments().get(0).getId());
                    } else {
                        findByStudentUid(uid, email);
                    }
                })
                .addOnFailureListener(e ->
                        findByStudentUid(uid, email)
                );
    }

    private void findByStudentUid(
            String uid,
            @Nullable String email
    ) {
        db.collection("applications")
                .whereEqualTo("studentUid", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        attachListener(snapshot.getDocuments().get(0).getId());
                    } else if (email != null
                            && !email.trim().isEmpty()) {
                        findByEmail(email);
                    } else {
                        clearDashboardValues();
                    }
                })
                .addOnFailureListener(e -> {
                    if (email != null
                            && !email.trim().isEmpty()) {
                        findByEmail(email);
                    } else {
                        clearDashboardValues();
                    }
                });
    }

    private void findByEmail(String email) {
        db.collection("applications")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        attachListener(
                                snapshot.getDocuments().get(0).getId()
                        );
                    } else {
                        clearDashboardValues();
                    }
                })
                .addOnFailureListener(e ->
                        clearDashboardValues()
                );
    }

    private void attachListener(String documentId) {
        if (applicationListener != null) {
            applicationListener.remove();
            applicationListener = null;
        }

        applicationListener = db.collection("applications")
                .document(documentId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (document != null && document.exists()) {
                        bindApplication(document);
                    } else {
                        clearDashboardValues();
                    }
                });
    }

    private void bindApplication(DocumentSnapshot d) {

        // ---------------- APPLICATION STATUS ----------------
        String rawStatus = firstValue(
                d,
                "status",
                "applicationStatus",
                "application_status"
        );

        String status = resolveApplicationStatus(rawStatus);

        if (status.isEmpty()) {
            status = "Submitted";
        }

        setText(tvApplicationStatus, status);
        setText(tvApplicationStatusBadge, status);

        // ---------------- ELIGIBILITY ----------------
        String eligible = firstValue(
                d,
                "eligible",
                "isEligible",
                "eligibility"
        );

        setText(
                tvEligibilityStatus,
                resolveEligibility(eligible)
        );

        setText(
                tvEligibilityStatusBadge,
                resolveEligibility(eligible)
        );

        // ---------------- APPLICATION ID ----------------
        String applicationId = firstValue(
                d,
                "applicationId",
                "applicationID"
        );

        if (applicationId.isEmpty()) {
            applicationId = firstValue(
                    d,
                    "applicantNo",
                    "applicationNo",
                    "applicationNumber"
            );
        }

        if (applicationId.isEmpty()) {
            applicationId = d.getId();
        }

        setText(tvApplicationId, applicationId);

        // ---------------- ADMISSION RESULT ----------------
        String result = firstValue(
                d,
                "selected",
                "selectionStatus",
                "admissionResult",
                "resultStatus",
                "result"
        );

        if (result.isEmpty()) {
            Boolean selected = d.getBoolean("isSelected");
            if (selected != null) {
                result = selected
                        ? "Selected"
                        : "Not Selected";
            }
        }

        /*
         * resultAvailable/resultPublished are the same Firebase controls
         * used by the existing Admin dashboard/tracker logic.
         */
        boolean resultAvailable =
                Boolean.TRUE.equals(
                        d.getBoolean("resultAvailable")
                );

        boolean resultPublished =
                Boolean.TRUE.equals(
                        d.getBoolean("resultPublished")
                );

        if (resultAvailable || resultPublished) {
            setText(
                    tvAdmissionResult,
                    result.isEmpty()
                            ? "Result Available"
                            : prettyResult(result)
            );
        } else {
            setText(
                    tvAdmissionResult,
                    result.isEmpty()
                            ? "Not published"
                            : prettyResult(result)
            );
        }

        // ---------------- YOUR APPLICATION ----------------
        setText(
                tvYourApplicationNo,
                firstValueOr(
                        d,
                        applicationId,
                        "applicantNo",
                        "applicationNo",
                        "applicationNumber",
                        "applicationId"
                )
        );

        setText(
                tvYourApplicationDepartment,
                firstValueOr(
                        d,
                        "Not available",
                        "department",
                        "departmentName",
                        "appliedDepartment"
                )
        );

        setText(
                tvYourApplicationTechnology,
                firstValueOr(
                        d,
                        "Not available",
                        "diplomaTechnology",
                        "technology",
                        "diploma_technology",
                        "diplomaTechnologyName"
                )
        );

        setText(
                tvYourApplicationQuota,
                firstValueOr(
                        d,
                        "Not available",
                        "quota",
                        "quotaName",
                        "appliedQuota"
                )
        );
    }

    private String resolveApplicationStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }

        String s = raw.trim().toLowerCase(Locale.ROOT);

        if (s.equals("submitted")
                || s.equals("submit")
                || s.equals("pending")) {
            return "Submitted";
        }

        if (s.equals("under review")
                || s.equals("review")
                || s.equals("processing")
                || s.equals("in process")) {
            return "Under Review";
        }

        if (s.equals("approved")
                || s.equals("approve")
                || s.equals("accepted")
                || s.equals("accept")) {
            return "Approve";
        }

        if (s.equals("rejected")
                || s.equals("reject")
                || s.equals("declined")
                || s.equals("decline")
                || s.equals("cancelled")
                || s.equals("canceled")) {
            return "Rejected";
        }

        // Old payment values are not application statuses.
        if (s.equals("paid")
                || s.equals("verified")
                || s.equals("payment verified")
                || s.equals("payment_verified")
                || s.equals("success")
                || s.equals("successful")
                || s.equals("unpaid")
                || s.equals("rejected payment")
                || s.equals("payment rejected")
                || s.equals("payment_rejected")) {
            return "Submitted";
        }

        return "Submitted";
    }

    private String resolveEligibility(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Not available";
        }

        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replace("_", " ")
                .replace("-", " ");

        if (s.equals("true")
                || s.equals("yes")
                || s.equals("eligible")) {
            return "Pass";
        }

        if (s.equals("false")
                || s.equals("no")
                || s.equals("not eligible")) {
            return "Not Eligible";
        }

        return raw.trim();
    }

    private String prettyResult(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Not published";
        }

        String s = raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", " ")
                .replace("-", " ");

        if (s.equals("selected")
                || s.equals("select")) {
            return "Selected";
        }

        if (s.equals("not selected")
                || s.equals("notselected")
                || s.equals("rejected")
                || s.equals("reject")) {
            return "Not Selected";
        }

        if (s.equals("published")) {
            return "Published";
        }

        return raw.trim();
    }

    private String firstValue(
            DocumentSnapshot d,
            String... fields
    ) {
        for (String field : fields) {
            Object value = d.get(field);

            if (value == null) {
                continue;
            }

            String text = String.valueOf(value).trim();

            if (!text.isEmpty()) {
                return text;
            }
        }

        return "";
    }

    private String firstValueOr(
            DocumentSnapshot d,
            String fallback,
            String... fields
    ) {
        String value = firstValue(d, fields);
        return value.isEmpty() ? fallback : value;
    }

    private void clearDashboardValues() {
        setText(tvApplicationStatus, "Approve");
        setText(tvApplicationStatusBadge, "Approve");

        setText(tvEligibilityStatus, "Pass");
        setText(tvEligibilityStatusBadge, "Pass");

        setText(tvApplicationId, "Not assigned");
        setText(tvAdmissionResult, "Not published");

        setText(tvYourApplicationNo, "Not available");
        setText(tvYourApplicationDepartment, "Not available");
        setText(tvYourApplicationTechnology, "Not available");
        setText(tvYourApplicationQuota, "Not available");
    }

    private void setText(
            @Nullable TextView view,
            String value
    ) {
        if (view != null) {
            view.setText(
                    value == null ? "" : value
            );
        }
    }

    @Override
    protected void onStop() {
        if (applicationListener != null) {
            applicationListener.remove();
            applicationListener = null;
        }

        super.onStop();
    }
}
