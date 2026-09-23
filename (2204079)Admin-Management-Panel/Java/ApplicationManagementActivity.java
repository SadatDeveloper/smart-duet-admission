package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApplicationManagementActivity extends AppCompatActivity {

    // =========================================================
    // FIRESTORE
    // =========================================================

    private FirebaseFirestore db;


    // =========================================================
    // HEADER / FOOTER
    // =========================================================

    private ImageButton btnBack;

    private BottomNavigationView bottomNavigation;


    // =========================================================
    // SEARCH
    // =========================================================

    private TextInputEditText etSearch;


    // =========================================================
    // FILTER BUTTONS
    // =========================================================

    private MaterialButton btnAll;
    private MaterialButton btnPending;
    private MaterialButton btnApproved;
    private MaterialButton btnRejected;


    // =========================================================
    // APPLICATION LIST
    // =========================================================

    private TextView tvApplicationCount;
    private TextView tvLoading;

    private LinearLayout applicationContainer;


    // =========================================================
    // DATA
    // =========================================================

    private final List<DocumentSnapshot> applications =
            new ArrayList<>();


    private String currentFilter = "all";


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
                R.layout.activity_application_management
        );


        db =
                FirebaseFirestore.getInstance();


        initViews();

        setupHeader();

        setupFooter();

        setupFilters();

        setupSearch();

        loadApplications();
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initViews() {

        btnBack =
                findViewById(
                        R.id.btnBack
                );


        bottomNavigation =
                findViewById(
                        R.id.bottomNavigation
                );


        etSearch =
                findViewById(
                        R.id.etSearch
                );


        btnAll =
                findViewById(
                        R.id.btnAll
                );


        btnPending =
                findViewById(
                        R.id.btnPending
                );


        btnApproved =
                findViewById(
                        R.id.btnApproved
                );


        btnRejected =
                findViewById(
                        R.id.btnRejected
                );


        tvApplicationCount =
                findViewById(
                        R.id.tvApplicationCount
                );


        tvLoading =
                findViewById(
                        R.id.tvLoading
                );


        applicationContainer =
                findViewById(
                        R.id.applicationContainer
                );
    }


    // =========================================================
    // HEADER
    // =========================================================

    private void setupHeader() {

        btnBack.setOnClickListener(
                v -> finish()
        );
    }


    // =========================================================
    // FOOTER
    // =========================================================

    private void setupFooter() {

        bottomNavigation.setOnItemSelectedListener(
                item -> true
        );
    }


    // =========================================================
    // FILTER SETUP
    // =========================================================

    private void setupFilters() {

        btnAll.setOnClickListener(
                v -> {

                    currentFilter = "all";

                    updateFilterButtons();

                    updateList();
                }
        );


        btnPending.setOnClickListener(
                v -> {

                    currentFilter = "pending";

                    updateFilterButtons();

                    updateList();
                }
        );


        btnApproved.setOnClickListener(
                v -> {

                    currentFilter = "approved";

                    updateFilterButtons();

                    updateList();
                }
        );


        btnRejected.setOnClickListener(
                v -> {

                    currentFilter = "rejected";

                    updateFilterButtons();

                    updateList();
                }
        );


        updateFilterButtons();
    }


    // =========================================================
    // FILTER BUTTON VISUAL STATE
    // =========================================================

    private void updateFilterButtons() {

        setFilterButtonState(
                btnAll,
                currentFilter.equals("all")
        );


        setFilterButtonState(
                btnPending,
                currentFilter.equals("pending")
        );


        setFilterButtonState(
                btnApproved,
                currentFilter.equals("approved")
        );


        setFilterButtonState(
                btnRejected,
                currentFilter.equals("rejected")
        );
    }


    private void setFilterButtonState(
            MaterialButton button,
            boolean selected
    ) {

        if (selected) {

            button.setTextColor(
                    Color.WHITE
            );

            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor(
                                    "#173F6B"
                            )
                    )
            );

        } else {

            button.setTextColor(
                    Color.parseColor(
                            "#475569"
                    )
            );

            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.WHITE
                    )
            );
        }
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {

        etSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        updateList();
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }


    // =========================================================
    // LOAD APPLICATIONS
    // =========================================================

    private void loadApplications() {

        tvLoading.setVisibility(
                View.VISIBLE
        );


        applicationContainer.removeAllViews();


        db.collection(
                        "applications"
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            applications.clear();


                            applications.addAll(
                                    querySnapshot.getDocuments()
                            );


                            tvLoading.setVisibility(
                                    View.GONE
                            );


                            updateList();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            tvLoading.setVisibility(
                                    View.GONE
                            );


                            applicationContainer
                                    .removeAllViews();


                            TextView error =
                                    createText(
                                            "Unable to load applications.\n"
                                                    + "Please check Firestore permission.",
                                            "#DC2626",
                                            13,
                                            false
                                    );


                            error.setGravity(
                                    Gravity.CENTER
                            );


                            error.setPadding(
                                    20,
                                    40,
                                    20,
                                    40
                            );


                            applicationContainer.addView(
                                    error
                            );


                            Toast.makeText(
                                    this,
                                    "Application data load failed.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    // =========================================================
    // UPDATE APPLICATION LIST
    // =========================================================

    private void updateList() {

        applicationContainer.removeAllViews();


        String search =
                etSearch.getText()
                        .toString()
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        int count = 0;


        for (
                DocumentSnapshot document :
                applications
        ) {

            String status =
                    normalize(
                            document.getString(
                                    "status"
                            )
                    );


            if (!matchesFilter(status)) {

                continue;
            }


            if (
                    !matchesSearch(
                            document,
                            search
                    )
            ) {

                continue;
            }


            addApplicationCard(
                    document
            );


            count++;
        }


        tvApplicationCount.setText(
                "Applications: " + count
        );


        if (count == 0) {

            showEmpty(
                    "No matching applications found."
            );
        }
    }


    // =========================================================
    // FILTER MATCH
    // =========================================================

    private boolean matchesFilter(
            String status
    ) {

        switch (currentFilter) {

            case "pending":

                return status.equals(
                        "pending"
                )
                        || status.equals(
                        "under_review"
                )
                        || status.equals(
                        "under review"
                );


            case "approved":

                return status.equals(
                        "approved"
                );


            case "rejected":

                return status.equals(
                        "rejected"
                );


            default:

                return true;
        }
    }


    // =========================================================
    // SEARCH MATCH
    // =========================================================

    private boolean matchesSearch(
            DocumentSnapshot document,
            String search
    ) {

        if (search.isEmpty()) {

            return true;
        }


        String id =
                document.getId()
                        .toLowerCase(
                                Locale.ROOT
                        );


        String name =
                getValue(
                        document,
                        "name",
                        "studentName",
                        "fullName"
                ).toLowerCase(
                        Locale.ROOT
                );


        String email =
                getValue(
                        document,
                        "email",
                        "studentEmail"
                ).toLowerCase(
                        Locale.ROOT
                );


        String phone =
                getValue(
                        document,
                        "phone",
                        "phoneNumber",
                        "mobile"
                ).toLowerCase(
                        Locale.ROOT
                );


        String technology =
                getValue(
                        document,
                        "technology",
                        "Technology",
                        "department",
                        "departmentName"
                ).toLowerCase(
                        Locale.ROOT
                );


        return id.contains(search)
                || name.contains(search)
                || email.contains(search)
                || phone.contains(search)
                || technology.contains(search);
    }


    // =========================================================
    // XML BASED APPLICATION CARD
    // =========================================================

    private void addApplicationCard(
            DocumentSnapshot document
    ) {

        View cardView =
                getLayoutInflater().inflate(
                        R.layout.item_application,
                        applicationContainer,
                        false
                );


        // =====================================================
        // FIND CARD VIEWS
        // =====================================================

        TextView tvApplicationId =
                cardView.findViewById(
                        R.id.tvApplicationId
                );


        TextView tvStudentName =
                cardView.findViewById(
                        R.id.tvStudentName
                );


        TextView tvStudentEmail =
                cardView.findViewById(
                        R.id.tvStudentEmail
                );


        TextView tvStudentPhone =
                cardView.findViewById(
                        R.id.tvStudentPhone
                );


        TextView tvTechnology =
                cardView.findViewById(
                        R.id.tvTechnology
                );


        TextView tvPaymentStatus =
                cardView.findViewById(
                        R.id.tvPaymentStatus
                );


        TextView tvStatusBadge =
                cardView.findViewById(
                        R.id.tvStatusBadge
                );


        MaterialButton btnViewDetails =
                cardView.findViewById(
                        R.id.btnViewDetails
                );


        MaterialButton btnApprove =
                cardView.findViewById(
                        R.id.btnApprove
                );


        MaterialButton btnReject =
                cardView.findViewById(
                        R.id.btnReject
                );


        // =====================================================
        // GET DATA
        // =====================================================

        String name =
                getValue(
                        document,
                        "name",
                        "studentName",
                        "fullName"
                );


        String email =
                getValue(
                        document,
                        "email",
                        "studentEmail"
                );


        String phone =
                getValue(
                        document,
                        "phone",
                        "phoneNumber",
                        "mobile"
                );


        String technology =
                getValue(
                        document,
                        "technology",
                        "Technology",
                        "department",
                        "departmentName"
                );


        String payment =
                document.getString(
                        "paymentStatus"
                );


        String status =
                document.getString(
                        "status"
                );


        // =====================================================
        // SET DATA
        // =====================================================

        tvApplicationId.setText(
                document.getId()
        );


        tvStudentName.setText(
                value(name)
        );


        tvStudentEmail.setText(
                "✉  " + value(email)
        );


        if (phone.isEmpty()) {

            tvStudentPhone.setVisibility(
                    View.GONE
            );

        } else {

            tvStudentPhone.setVisibility(
                    View.VISIBLE
            );


            tvStudentPhone.setText(
                    "☎  " + phone
            );
        }


        tvTechnology.setText(
                value(technology)
        );


        tvPaymentStatus.setText(
                displayStatus(payment)
        );


        // =====================================================
        // APPLICATION STATUS BADGE
        // =====================================================

        applyStatusBadge(
                tvStatusBadge,
                status
        );


        // =====================================================
        // PAYMENT COLOR
        // =====================================================

        applyPaymentColor(
                tvPaymentStatus,
                payment
        );


        // =====================================================
        // VIEW DETAILS
        // =====================================================

        btnViewDetails.setOnClickListener(
                v ->
                        showDetails(
                                document
                        )
        );


        // =====================================================
        // APPROVE
        // =====================================================

        btnApprove.setOnClickListener(
                v ->
                        confirmApprove(
                                document
                        )
        );


        // =====================================================
        // REJECT
        // =====================================================

        btnReject.setOnClickListener(
                v ->
                        confirmReject(
                                document
                        )
        );


        // =====================================================
        // ADD CARD
        // =====================================================

        applicationContainer.addView(
                cardView
        );
    }


    // =========================================================
    // STATUS BADGE
    // =========================================================

    private void applyStatusBadge(
            TextView badge,
            String status
    ) {

        String normalized =
                normalize(status);


        badge.setText(
                displayStatus(status)
        );


        int textColor;

        int backgroundColor;


        if (
                normalized.equals(
                        "approved"
                )
        ) {

            textColor =
                    Color.parseColor(
                            "#15803D"
                    );

            backgroundColor =
                    Color.parseColor(
                            "#DCFCE7"
                    );


        } else if (
                normalized.equals(
                        "rejected"
                )
        ) {

            textColor =
                    Color.parseColor(
                            "#B91C1C"
                    );

            backgroundColor =
                    Color.parseColor(
                            "#FEE2E2"
                    );


        } else {

            textColor =
                    Color.parseColor(
                            "#B45309"
                    );

            backgroundColor =
                    Color.parseColor(
                            "#FEF3C7"
                    );
        }


        badge.setTextColor(
                textColor
        );


        GradientDrawable drawable =
                new GradientDrawable();


        drawable.setColor(
                backgroundColor
        );


        drawable.setCornerRadius(
                100f
        );


        badge.setBackground(
                drawable
        );
    }


    // =========================================================
    // PAYMENT COLOR
    // =========================================================

    private void applyPaymentColor(
            TextView paymentView,
            String payment
    ) {

        String normalized =
                normalize(payment);


        if (
                normalized.equals(
                        "verified"
                )
                        || normalized.equals(
                        "paid"
                )
                        || normalized.equals(
                        "success"
                )
                        || normalized.equals(
                        "successful"
                )
        ) {

            paymentView.setTextColor(
                    Color.parseColor(
                            "#16A34A"
                    )
            );


        } else if (
                normalized.equals(
                        "rejected"
                )
                        || normalized.equals(
                        "failed"
                )
        ) {

            paymentView.setTextColor(
                    Color.parseColor(
                            "#DC2626"
                    )
            );


        } else {

            paymentView.setTextColor(
                    Color.parseColor(
                            "#F59E0B"
                    )
            );
        }
    }


    // =========================================================
    // PROFESSIONAL VIEW DETAILS
    // =========================================================

    private void showDetails(
            DocumentSnapshot document
    ) {

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_application_details,
                        null
                );


        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();


        // =====================================================
        // FIND DIALOG VIEWS
        // =====================================================

        ImageButton btnCloseDetails =
                dialogView.findViewById(
                        R.id.btnCloseDetails
                );


        MaterialButton btnDetailsClose =
                dialogView.findViewById(
                        R.id.btnDetailsClose
                );


        TextView tvApplicationId =
                dialogView.findViewById(
                        R.id.tvDetailsApplicationId
                );


        TextView tvName =
                dialogView.findViewById(
                        R.id.tvDetailsName
                );


        TextView tvEmail =
                dialogView.findViewById(
                        R.id.tvDetailsEmail
                );


        TextView tvPhone =
                dialogView.findViewById(
                        R.id.tvDetailsPhone
                );


        TextView tvTechnology =
                dialogView.findViewById(
                        R.id.tvDetailsTechnology
                );


        TextView tvPayment =
                dialogView.findViewById(
                        R.id.tvDetailsPayment
                );


        TextView tvStatus =
                dialogView.findViewById(
                        R.id.tvDetailsStatus
                );


        LinearLayout rejectionContainer =
                dialogView.findViewById(
                        R.id.rejectionReasonContainer
                );


        TextView tvRejectionReason =
                dialogView.findViewById(
                        R.id.tvDetailsRejectionReason
                );


        // =====================================================
        // GET FIRESTORE DATA
        // =====================================================

        String name =
                getValue(
                        document,
                        "name",
                        "studentName",
                        "fullName"
                );


        String email =
                getValue(
                        document,
                        "email",
                        "studentEmail"
                );


        String phone =
                getValue(
                        document,
                        "phone",
                        "phoneNumber",
                        "mobile"
                );


        String technology =
                getValue(
                        document,
                        "technology",
                        "Technology",
                        "department",
                        "departmentName"
                );


        String payment =
                document.getString(
                        "paymentStatus"
                );


        String status =
                document.getString(
                        "status"
                );


        String rejectionReason =
                document.getString(
                        "rejectionReason"
                );


        // =====================================================
        // SET DATA
        // =====================================================

        tvApplicationId.setText(
                document.getId()
        );


        tvName.setText(
                value(name)
        );


        tvEmail.setText(
                value(email)
        );


        tvPhone.setText(
                value(phone)
        );


        tvTechnology.setText(
                value(technology)
        );


        tvPayment.setText(
                displayStatus(payment)
        );


        tvStatus.setText(
                displayStatus(status)
        );


        // =====================================================
        // STATUS COLOR
        // =====================================================

        tvStatus.setTextColor(
                Color.parseColor(
                        getStatusColor(status)
                )
        );


        // =====================================================
        // PAYMENT COLOR
        // =====================================================

        applyPaymentColor(
                tvPayment,
                payment
        );


        // =====================================================
        // REJECTION REASON
        // =====================================================

        if (
                rejectionReason != null
                        && !rejectionReason
                        .trim()
                        .isEmpty()
        ) {

            rejectionContainer.setVisibility(
                    View.VISIBLE
            );


            tvRejectionReason.setText(
                    rejectionReason
            );

        } else {

            rejectionContainer.setVisibility(
                    View.GONE
            );
        }


        // =====================================================
        // CLOSE BUTTON
        // =====================================================

        btnCloseDetails.setOnClickListener(
                v ->
                        dialog.dismiss()
        );


        btnDetailsClose.setOnClickListener(
                v ->
                        dialog.dismiss()
        );


        // =====================================================
        // SHOW DIALOG
        // =====================================================

        dialog.show();


        // =====================================================
        // DIALOG SIZE
        // =====================================================

        if (
                dialog.getWindow() != null
        ) {

            dialog.getWindow()
                    .setBackgroundDrawableResource(
                            android.R.color.transparent
                    );


            int screenWidth =
                    getResources()
                            .getDisplayMetrics()
                            .widthPixels;


            int dialogWidth =
                    (int) (
                            screenWidth * 0.94
                    );


            dialog.getWindow()
                    .setLayout(
                            dialogWidth,
                            WindowManager.LayoutParams.WRAP_CONTENT
                    );
        }
    }


    // =========================================================
    // APPROVE CONFIRMATION
    // =========================================================

    private void confirmApprove(
            DocumentSnapshot document
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Approve Application"
                )
                .setMessage(
                        "Have you verified this application "
                                + "and confirmed that the information "
                                + "is valid and eligible?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "APPROVE",
                        (dialog, which) ->
                                approveApplication(
                                        document
                                )
                )
                .show();
    }


    // =========================================================
    // REJECT CONFIRMATION
    // =========================================================

    private void confirmReject(
            DocumentSnapshot document
    ) {

        final TextInputEditText reason =
                new TextInputEditText(this);


        reason.setHint(
                "Enter rejection reason"
        );


        reason.setMinLines(
                3
        );


        reason.setGravity(
                Gravity.TOP
        );


        LinearLayout container =
                new LinearLayout(this);


        container.setOrientation(
                LinearLayout.VERTICAL
        );


        container.setPadding(
                30,
                5,
                30,
                5
        );


        container.addView(
                reason,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );


        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Reject Application"
                        )
                        .setMessage(
                                "Please enter the reason "
                                        + "before rejecting this application."
                        )
                        .setView(
                                container
                        )
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "REJECT",
                                null
                        )
                        .create();


        dialog.setOnShowListener(
                ignored -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                String rejectReason =
                                        reason.getText()
                                                .toString()
                                                .trim();


                                if (
                                        rejectReason.isEmpty()
                                ) {

                                    reason.setError(
                                            "Rejection reason is required"
                                    );


                                    reason.requestFocus();

                                    return;
                                }


                                dialog.dismiss();


                                rejectApplication(
                                        document,
                                        rejectReason
                                );
                            }
                    );
                }
        );


        dialog.show();
    }


    // =========================================================
    // APPROVE APPLICATION
    // =========================================================

    private void approveApplication(
            DocumentSnapshot document
    ) {

        db.collection(
                        "applications"
                )
                .document(
                        document.getId()
                )
                .update(
                        "status",
                        "APPROVED"
                )
                .addOnSuccessListener(
                        unused -> {

                            AdminActivityLogger.log(
                                    "Application Approved",
                                    document.getId(),
                                    "Application verified and approved by administrator."
                            );


                            Toast.makeText(
                                    this,
                                    "Application approved.",
                                    Toast.LENGTH_SHORT
                            ).show();


                            loadApplications();
                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "Unable to approve application.",
                                        Toast.LENGTH_LONG
                                ).show()
                );
    }


    // =========================================================
    // REJECT APPLICATION
    // =========================================================

    private void rejectApplication(
            DocumentSnapshot document,
            String reason
    ) {

        db.collection(
                        "applications"
                )
                .document(
                        document.getId()
                )
                .update(
                        "status",
                        "REJECTED",
                        "rejectionReason",
                        reason
                )
                .addOnSuccessListener(
                        unused -> {

                            AdminActivityLogger.log(
                                    "Application Rejected",
                                    document.getId(),
                                    reason
                            );


                            Toast.makeText(
                                    this,
                                    "Application rejected.",
                                    Toast.LENGTH_SHORT
                            ).show();


                            loadApplications();
                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "Unable to reject application.",
                                        Toast.LENGTH_LONG
                                ).show()
                );
    }


    // =========================================================
    // GET VALUE
    // =========================================================

    private String getValue(
            DocumentSnapshot document,
            String... fields
    ) {

        for (String field : fields) {

            Object object =
                    document.get(field);


            if (object != null) {

                String result =
                        String.valueOf(
                                object
                        ).trim();


                if (!result.isEmpty()) {

                    return result;
                }
            }
        }


        return "";
    }


    // =========================================================
    // VALUE
    // =========================================================

    private String value(
            String text
    ) {

        if (
                text == null
                        || text.trim().isEmpty()
        ) {

            return "Not available";
        }


        return text;
    }


    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalize(
            String text
    ) {

        if (text == null) {

            return "";
        }


        return text
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "-",
                        "_"
                );
    }


    // =========================================================
    // DISPLAY STATUS
    // =========================================================

    private String displayStatus(
            String status
    ) {

        if (
                status == null
                        || status.trim().isEmpty()
        ) {

            return "NOT AVAILABLE";
        }


        return status
                .replace(
                        "_",
                        " "
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }


    // =========================================================
    // STATUS COLOR
    // =========================================================

    private String getStatusColor(
            String status
    ) {

        String normalized =
                normalize(status);


        if (
                normalized.equals(
                        "approved"
                )
        ) {

            return "#16A34A";
        }


        if (
                normalized.equals(
                        "rejected"
                )
        ) {

            return "#DC2626";
        }


        return "#F59E0B";
    }


    // =========================================================
    // CREATE TEXT
    // =========================================================

    private TextView createText(
            String text,
            String color,
            float size,
            boolean bold
    ) {

        TextView textView =
                new TextView(this);


        textView.setText(
                text
        );


        textView.setTextColor(
                Color.parseColor(
                        color
                )
        );


        textView.setTextSize(
                size
        );


        if (bold) {

            textView.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }


        return textView;
    }


    // =========================================================
    // EMPTY STATE
    // =========================================================

    private void showEmpty(
            String message
    ) {

        TextView empty =
                createText(
                        message,
                        "#64748B",
                        13,
                        false
                );


        empty.setGravity(
                Gravity.CENTER
        );


        empty.setPadding(
                10,
                45,
                10,
                45
        );


        applicationContainer.addView(
                empty
        );
    }
}