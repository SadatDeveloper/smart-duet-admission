package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentVerificationActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration applicationsListener;

    private RecyclerView recyclerPayments;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private TextView tvPendingCount;
    private TextView tvVerifiedCount;
    private TextView tvRejectedCount;

    private TextView tabAll;
    private TextView tabPending;
    private TextView tabVerified;
    private TextView tabRejected;

    private EditText etSearch;

    private PaymentAdapter adapter;

    private final List<PaymentModel> allPayments = new ArrayList<>();
    private final List<PaymentModel> filteredPayments = new ArrayList<>();

    private String selectedFilter = "ALL";

    private boolean isFirstLoad = true;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment_verification);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        checkAdminAndStartListener();
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initViews() {

        recyclerPayments = findViewById(R.id.recyclerPayments);

        progressBar = findViewById(R.id.progressBar);

        tvEmpty = findViewById(R.id.tvEmpty);

        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvVerifiedCount = findViewById(R.id.tvVerifiedCount);
        tvRejectedCount = findViewById(R.id.tvRejectedCount);

        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabVerified = findViewById(R.id.tabVerified);
        tabRejected = findViewById(R.id.tabRejected);

        etSearch = findViewById(R.id.etSearch);

        findViewById(R.id.btnBack).setOnClickListener(
                v -> finish()
        );
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private void setupRecyclerView() {

        recyclerPayments.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new PaymentAdapter(filteredPayments);

        recyclerPayments.setAdapter(adapter);
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
                        applyFilters();
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
    // FILTER TABS
    // =========================================================

    private void setupFilters() {

        tabAll.setOnClickListener(v -> {

            selectedFilter = "ALL";

            updateFilterUI();

            applyFilters();
        });

        tabPending.setOnClickListener(v -> {

            selectedFilter = "PENDING";

            updateFilterUI();

            applyFilters();
        });

        // Existing XML ID kept.
        // This tab now means PAID.

        tabVerified.setOnClickListener(v -> {

            selectedFilter = "PAID";

            updateFilterUI();

            applyFilters();
        });

        // Existing XML ID kept.
        // This tab now means UNPAID.

        tabRejected.setOnClickListener(v -> {

            selectedFilter = "UNPAID";

            updateFilterUI();

            applyFilters();
        });

        updateFilterUI();
    }

    // =========================================================
    // FILTER UI
    // =========================================================

    private void updateFilterUI() {

        setFilterStyle(
                tabAll,
                selectedFilter.equals("ALL")
        );

        setFilterStyle(
                tabPending,
                selectedFilter.equals("PENDING")
        );

        setFilterStyle(
                tabVerified,
                selectedFilter.equals("PAID")
        );

        setFilterStyle(
                tabRejected,
                selectedFilter.equals("UNPAID")
        );
    }

    private void setFilterStyle(
            TextView view,
            boolean selected
    ) {

        GradientDrawable background =
                new GradientDrawable();

        background.setCornerRadius(100);

        if (selected) {

            background.setColor(
                    Color.parseColor("#102A43")
            );

            view.setTextColor(Color.WHITE);

        } else {

            background.setColor(
                    Color.parseColor("#E8EEF5")
            );

            view.setTextColor(
                    Color.parseColor("#486581")
            );
        }

        view.setBackground(background);
    }

    // =========================================================
    // ADMIN CHECK
    // =========================================================

    private void checkAdminAndStartListener() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            goToAdminLogin();

            return;
        }

        showLoading(true);

        db.collection("admins")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        Toast.makeText(
                                this,
                                "Admin access denied.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();

                        return;
                    }

                    Boolean active =
                            document.getBoolean("active");

                    String role =
                            document.getString("role");

                    boolean isAdmin =
                            Boolean.TRUE.equals(active)
                                    && role != null
                                    && role.equalsIgnoreCase("admin");

                    if (!isAdmin) {

                        Toast.makeText(
                                this,
                                "Admin access denied.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();

                        return;
                    }

                    startRealtimeListener();
                })
                .addOnFailureListener(e -> {

                    showLoading(false);

                    Toast.makeText(
                            this,
                            "Unable to verify admin access:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                });
    }

    // =========================================================
    // REAL-TIME LISTENER
    // =========================================================

    private void startRealtimeListener() {

        removeRealtimeListener();

        applicationsListener =
                db.collection("applications")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        showLoading(false);

                                        Toast.makeText(
                                                PaymentVerificationActivity.this,
                                                "Live payment update failed:\n"
                                                        + error.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    if (snapshot == null) {

                                        showLoading(false);

                                        return;
                                    }

                                    allPayments.clear();

                                    for (
                                            DocumentSnapshot document :
                                            snapshot.getDocuments()
                                    ) {

                                        String applicationStatus =
                                                getStringValue(
                                                        document,
                                                        "status"
                                                );

                                        String paymentStatus =
                                                getStringValue(
                                                        document,
                                                        "paymentStatus"
                                                );

                                        /*
                                         * Only submitted applications
                                         * are shown in Payment Verification.
                                         */

                                        boolean submitted =
                                                applicationStatus
                                                        .trim()
                                                        .equalsIgnoreCase(
                                                                "submitted"
                                                        );

                                        boolean hasPaymentStatus =
                                                !paymentStatus
                                                        .trim()
                                                        .isEmpty();

                                        if (!submitted
                                                && !hasPaymentStatus) {
                                            continue;
                                        }

                                        /*
                                         * If the application is submitted
                                         * and paymentStatus is empty,
                                         * treat the payment as Pending.
                                         *
                                         * For a rejected application,
                                         * paymentStatus is explicitly
                                         * stored as UNPAID by the Reject
                                         * action.
                                         */

                                        if (paymentStatus
                                                .trim()
                                                .isEmpty()) {

                                            paymentStatus =
                                                    "PENDING";
                                        }

                                        PaymentModel payment =
                                                createPaymentModel(
                                                        document,
                                                        paymentStatus
                                                );

                                        allPayments.add(payment);
                                    }

                                    updateStatistics();

                                    applyFilters();

                                    if (isFirstLoad) {

                                        isFirstLoad = false;

                                        showLoading(false);
                                    }
                                }
                        );
    }

    // =========================================================
    // CREATE PAYMENT MODEL
    // =========================================================

    private PaymentModel createPaymentModel(
            DocumentSnapshot document,
            String paymentStatus
    ) {

        PaymentModel payment =
                new PaymentModel();

        payment.documentId =
                document.getId();

        payment.applicationId =
                getStringValue(
                        document,
                        "applicationId"
                );

        if (payment.applicationId
                .trim()
                .isEmpty()) {

            payment.applicationId =
                    document.getId();
        }

        payment.fullName =
                getStringValue(
                        document,
                        "fullName"
                );

        if (payment.fullName.isEmpty()) {

            payment.fullName =
                    getStringValue(
                            document,
                            "name"
                    );
        }

        payment.email =
                getStringValue(
                        document,
                        "email"
                );

        payment.phone =
                getStringValue(
                        document,
                        "phone"
                );

        payment.paymentMethod =
                getStringValue(
                        document,
                        "paymentMethod"
                );

        payment.transactionId =
                getStringValue(
                        document,
                        "transactionId"
                );

        payment.paymentStatus =
                paymentStatus;

        payment.amount =
                getAmountValue(document);

        payment.applicationStatus =
                getStringValue(
                        document,
                        "status"
                );

        payment.paymentRejectionReason =
                getStringValue(
                        document,
                        "paymentRejectionReason"
                );

        payment.submittedAt =
                document.getTimestamp(
                        "submittedAt"
                );

        return payment;
    }

    // =========================================================
    // APPLY FILTERS
    // =========================================================

    private void applyFilters() {

        filteredPayments.clear();

        String query =
                etSearch == null
                        ? ""
                        : etSearch
                        .getText()
                        .toString()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        for (PaymentModel payment : allPayments) {

            String status =
                    normalizeStatus(
                            payment.paymentStatus
                    );

            boolean filterMatch = true;

            if (selectedFilter.equals("PENDING")) {

                filterMatch =
                        isPending(status);

            } else if (selectedFilter.equals("PAID")) {

                filterMatch =
                        isPaid(status);

            } else if (selectedFilter.equals("UNPAID")) {

                filterMatch =
                        isUnpaid(status);
            }

            boolean searchMatch =
                    query.isEmpty()
                            || contains(
                            payment.applicationId,
                            query
                    )
                            || contains(
                            payment.fullName,
                            query
                    )
                            || contains(
                            payment.email,
                            query
                    )
                            || contains(
                            payment.phone,
                            query
                    )
                            || contains(
                            payment.transactionId,
                            query
                    )
                            || contains(
                            payment.paymentMethod,
                            query
                    )
                            || contains(
                            payment.paymentStatus,
                            query
                    );

            if (filterMatch && searchMatch) {

                filteredPayments.add(payment);
            }
        }

        adapter.notifyDataSetChanged();

        updateEmptyState();
    }

    // =========================================================
    // EMPTY STATE
    // =========================================================

    private void updateEmptyState() {

        if (filteredPayments.isEmpty()) {

            tvEmpty.setVisibility(View.VISIBLE);

            recyclerPayments.setVisibility(View.GONE);

            if (allPayments.isEmpty()) {

                tvEmpty.setText(
                        "No submitted applications available"
                );

            } else {

                tvEmpty.setText(
                        "No payments match your search or filter"
                );
            }

        } else {

            tvEmpty.setVisibility(View.GONE);

            recyclerPayments.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private void updateStatistics() {

        int pending = 0;
        int paid = 0;
        int unpaid = 0;

        for (PaymentModel payment : allPayments) {

            String status =
                    normalizeStatus(
                            payment.paymentStatus
                    );

            if (isPending(status)) {

                pending++;

            } else if (isPaid(status)) {

                paid++;

            } else if (isUnpaid(status)) {

                unpaid++;
            }
        }

        tvPendingCount.setText(
                String.valueOf(pending)
        );

        tvVerifiedCount.setText(
                String.valueOf(paid)
        );

        tvRejectedCount.setText(
                String.valueOf(unpaid)
        );
    }

    // =========================================================
    // STATUS
    // =========================================================

    private String normalizeStatus(String status) {

        if (status == null) {
            return "";
        }

        return status
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private boolean isPending(String status) {

        return status.equals("PENDING")
                || status.equals("PAYMENT_PENDING")
                || status.equals("UNVERIFIED")
                || status.equals("SUBMITTED");
    }

    private boolean isPaid(String status) {

        return status.equals("PAID")
                || status.equals("VERIFIED")
                || status.equals("PAYMENT_VERIFIED")
                || status.equals("CONFIRMED")
                || status.equals("SUCCESSFUL");
    }

    private boolean isUnpaid(String status) {

        return status.equals("UNPAID")
                || status.equals("REJECTED")
                || status.equals("PAYMENT_REJECTED");
    }

    // =========================================================
    // PAID DIALOG
    // =========================================================

    private void showPaidDialog(
            PaymentModel payment
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Mark Payment as Paid")
                .setMessage(
                        "Application ID: "
                                + payment.applicationId

                                + "\n\nStudent: "
                                + payment.fullName

                                + "\n\nPayment Method: "
                                + payment.paymentMethod

                                + "\n\nTransaction ID: "
                                + payment.transactionId

                                + "\n\nAmount: ৳ "
                                + payment.amount

                                + "\n\nConfirm this payment as PAID?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "PAID",
                        (dialog, which) ->
                                markPaymentPaid(payment)
                )
                .show();
    }

    // =========================================================
    // MARK PAYMENT PAID
    // =========================================================

    private void markPaymentPaid(
            PaymentModel payment
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "paymentStatus",
                "PAID"
        );

        updates.put(
                "paymentVerifiedAt",
                FieldValue.serverTimestamp()
        );

        updates.put(
                "paymentVerifiedBy",
                user.getUid()
        );

        updates.put(
                "paymentVerifiedByEmail",
                user.getEmail() == null
                        ? ""
                        : user.getEmail()
        );

        updates.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("applications")
                .document(payment.documentId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    logAdminActivity(
                            "PAYMENT_PAID",
                            payment.applicationId,
                            "Payment marked as PAID for "
                                    + payment.fullName
                    );

                    Toast.makeText(
                            this,
                            "Payment marked as PAID",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to update payment:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // UNPAID DIALOG
    // =========================================================

    private void showUnpaidDialog(
            PaymentModel payment
    ) {

        final EditText input =
                new EditText(this);

        input.setHint(
                "Enter reason"
        );

        input.setMinLines(3);

        input.setGravity(
                Gravity.TOP
        );

        int padding =
                (int) (
                        16 *
                                getResources()
                                        .getDisplayMetrics()
                                        .density
                );

        input.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Mark Payment as Unpaid"
                        )
                        .setMessage(
                                "Application ID: "
                                        + payment.applicationId
                                        + "\nStudent: "
                                        + payment.fullName
                        )
                        .setView(input)
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "UNPAID",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                String reason =
                        input.getText()
                                .toString()
                                .trim();

                if (reason.isEmpty()) {

                    input.setError(
                            "Reason is required"
                    );

                    return;
                }

                dialog.dismiss();

                markPaymentUnpaid(
                        payment,
                        reason
                );
            });
        });

        dialog.show();
    }

    // =========================================================
    // MARK PAYMENT UNPAID
    // =========================================================

    private void markPaymentUnpaid(
            PaymentModel payment,
            String reason
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "paymentStatus",
                "UNPAID"
        );

        updates.put(
                "paymentRejectionReason",
                reason
        );

        updates.put(
                "paymentRejectedAt",
                FieldValue.serverTimestamp()
        );

        updates.put(
                "paymentRejectedBy",
                user.getUid()
        );

        updates.put(
                "paymentRejectedByEmail",
                user.getEmail() == null
                        ? ""
                        : user.getEmail()
        );

        updates.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("applications")
                .document(payment.documentId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    logAdminActivity(
                            "PAYMENT_UNPAID",
                            payment.applicationId,
                            "Payment marked as UNPAID for "
                                    + payment.fullName
                                    + ". Reason: "
                                    + reason
                    );

                    Toast.makeText(
                            this,
                            "Payment marked as UNPAID",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to update payment:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // ADMIN ACTIVITY LOG
    // =========================================================

    private void logAdminActivity(
            String action,
            String applicationId,
            String description
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> log =
                new HashMap<>();

        log.put(
                "adminUid",
                user.getUid()
        );

        log.put(
                "adminEmail",
                user.getEmail() == null
                        ? ""
                        : user.getEmail()
        );

        log.put(
                "action",
                action
        );

        log.put(
                "applicationId",
                applicationId == null
                        ? ""
                        : applicationId
        );

        log.put(
                "description",
                description == null
                        ? ""
                        : description
        );

        log.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        db.collection("admin_activity_logs")
                .add(log);
    }

    // =========================================================
    // FIRESTORE HELPERS
    // =========================================================

    private String getStringValue(
            DocumentSnapshot document,
            String field
    ) {

        Object value =
                document.get(field);

        return value == null
                ? ""
                : String.valueOf(value);
    }

    private String getAmountValue(
            DocumentSnapshot document
    ) {

        Object value =
                document.get("amount");

        if (value == null) {
            return "0";
        }

        return String.valueOf(value);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private boolean contains(
            String value,
            String query
    ) {

        if (value == null) {
            return false;
        }

        return value
                .toLowerCase(Locale.ROOT)
                .contains(query);
    }

    // =========================================================
    // LOADING
    // =========================================================

    private void showLoading(boolean show) {

        progressBar.setVisibility(
                show
                        ? View.VISIBLE
                        : View.GONE
        );

        if (show) {

            recyclerPayments.setVisibility(
                    View.GONE
            );

            tvEmpty.setVisibility(
                    View.GONE
            );
        }
    }

    // =========================================================
    // REMOVE LISTENER
    // =========================================================

    private void removeRealtimeListener() {

        if (applicationsListener != null) {

            applicationsListener.remove();

            applicationsListener = null;
        }
    }

    // =========================================================
    // ADMIN LOGIN
    // =========================================================

    private void goToAdminLogin() {

        startActivity(
                new Intent(
                        PaymentVerificationActivity.this,
                        AdminLoginActivity.class
                )
        );

        finish();
    }

    // =========================================================
    // MODEL
    // =========================================================

    public static class PaymentModel {

        public String documentId = "";
        public String applicationId = "";
        public String fullName = "";
        public String email = "";
        public String phone = "";
        public String paymentMethod = "";
        public String transactionId = "";
        public String paymentStatus = "";
        public String amount = "";
        public String applicationStatus = "";
        public String paymentRejectionReason = "";
        public Timestamp submittedAt;
    }

    // =========================================================
    // ADAPTER
    // =========================================================

    private class PaymentAdapter
            extends RecyclerView.Adapter<
            PaymentAdapter.PaymentViewHolder> {

        private final List<PaymentModel> payments;

        PaymentAdapter(List<PaymentModel> payments) {
            this.payments = payments;
        }

        @NonNull
        @Override
        public PaymentViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_payment_verification,
                            parent,
                            false
                    );

            return new PaymentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull PaymentViewHolder holder,
                int position
        ) {
            PaymentModel payment = payments.get(position);

            holder.tvApplicationId.setText(payment.applicationId);

            holder.tvStudentName.setText(
                    payment.fullName.isEmpty()
                            ? "Student"
                            : payment.fullName
            );

            holder.tvPhone.setText(
                    payment.phone.isEmpty()
                            ? "Phone not available"
                            : payment.phone
            );

            holder.tvPaymentMethod.setText(
                    payment.paymentMethod.isEmpty()
                            ? "Payment method not available"
                            : payment.paymentMethod
            );

            holder.tvTransactionId.setText(
                    payment.transactionId.isEmpty()
                            ? "Transaction ID not available"
                            : payment.transactionId
            );

            holder.tvAmount.setText("৳ " + payment.amount);

            String paymentStatus =
                    normalizeStatus(payment.paymentStatus);

            if (paymentStatus.isEmpty()) {
                paymentStatus = "PENDING";
            }

            holder.tvPaymentStatus.setText(paymentStatus);
            styleStatus(holder.tvPaymentStatus, paymentStatus);

            /*
             * Three separate admin actions:
             *
             * MARK PAID
             *   -> paymentStatus = PAID
             *   -> application status remains unchanged
             *
             * MARK UNPAID
             *   -> paymentStatus = UNPAID
             *   -> application status remains unchanged
             *
             * REJECT
             *   -> application status = REJECTED
             *   -> paymentStatus = UNPAID
             */

            if (isPaid(paymentStatus)) {

                // A paid payment can still be changed to unpaid
                // or the whole application can be rejected.
                holder.btnVerify.setVisibility(View.VISIBLE);
                holder.btnUnpaid.setVisibility(View.VISIBLE);
                holder.btnRejectApplication.setVisibility(View.VISIBLE);

                holder.btnVerify.setText("MARK PAID");
                holder.btnUnpaid.setText("MARK UNPAID");
                holder.btnRejectApplication.setText("REJECT");

                holder.btnVerify.setEnabled(false);
                holder.btnVerify.setAlpha(0.55f);

            } else if (isUnpaid(paymentStatus)) {

                // Unpaid payment can be marked paid or the
                // application can be rejected.
                holder.btnVerify.setVisibility(View.VISIBLE);
                holder.btnUnpaid.setVisibility(View.VISIBLE);
                holder.btnRejectApplication.setVisibility(View.VISIBLE);

                holder.btnVerify.setText("MARK PAID");
                holder.btnUnpaid.setText("MARK UNPAID");
                holder.btnRejectApplication.setText("REJECT");

                holder.btnVerify.setEnabled(true);
                holder.btnVerify.setAlpha(1f);

                holder.btnUnpaid.setEnabled(false);
                holder.btnUnpaid.setAlpha(0.55f);

            } else {

                // Pending payment: all three actions available.
                holder.btnVerify.setVisibility(View.VISIBLE);
                holder.btnUnpaid.setVisibility(View.VISIBLE);
                holder.btnRejectApplication.setVisibility(View.VISIBLE);

                holder.btnVerify.setText("MARK PAID");
                holder.btnUnpaid.setText("MARK UNPAID");
                holder.btnRejectApplication.setText("REJECT");

                holder.btnVerify.setEnabled(true);
                holder.btnVerify.setAlpha(1f);

                holder.btnUnpaid.setEnabled(true);
                holder.btnUnpaid.setAlpha(1f);
            }

            holder.btnVerify.setOnClickListener(
                    v -> {
                        if (holder.btnVerify.isEnabled()) {
                            showPaidDialog(payment);
                        }
                    }
            );

            holder.btnUnpaid.setOnClickListener(
                    v -> {
                        if (holder.btnUnpaid.isEnabled()) {
                            showUnpaidDialog(payment);
                        }
                    }
            );

            holder.btnRejectApplication.setOnClickListener(
                    v -> showRejectApplicationDialog(payment)
            );

            holder.itemView.setOnClickListener(
                    v -> showPaymentDetails(payment)
            );
        }

        // =====================================================
        // STATUS STYLE
        // =====================================================

        private void styleStatus(
                TextView textView,
                String status
        ) {
            GradientDrawable background =
                    new GradientDrawable();

            background.setCornerRadius(100);

            background.setColor(
                    Color.parseColor("#F1F5F9")
            );

            textView.setBackground(background);

            if (isPaid(status)) {

                textView.setTextColor(
                        Color.parseColor("#16803A")
                );

            } else if (isUnpaid(status)) {

                textView.setTextColor(
                        Color.parseColor("#C62828")
                );

            } else {

                textView.setTextColor(
                        Color.parseColor("#D97706")
                );
            }
        }

        @Override
        public int getItemCount() {
            return payments.size();
        }

        // =====================================================
        // VIEW HOLDER
        // =====================================================

        class PaymentViewHolder
                extends RecyclerView.ViewHolder {

            TextView tvApplicationId;
            TextView tvStudentName;
            TextView tvPhone;
            TextView tvPaymentMethod;
            TextView tvTransactionId;
            TextView tvAmount;
            TextView tvPaymentStatus;

            Button btnVerify;
            Button btnUnpaid;
            Button btnRejectApplication;

            PaymentViewHolder(
                    @NonNull View itemView
            ) {
                super(itemView);

                tvApplicationId =
                        itemView.findViewById(
                                R.id.tvApplicationId
                        );

                tvStudentName =
                        itemView.findViewById(
                                R.id.tvStudentName
                        );

                tvPhone =
                        itemView.findViewById(
                                R.id.tvPhone
                        );

                tvPaymentMethod =
                        itemView.findViewById(
                                R.id.tvPaymentMethod
                        );

                tvTransactionId =
                        itemView.findViewById(
                                R.id.tvTransactionId
                        );

                tvAmount =
                        itemView.findViewById(
                                R.id.tvAmount
                        );

                tvPaymentStatus =
                        itemView.findViewById(
                                R.id.tvPaymentStatus
                        );

                btnVerify =
                        itemView.findViewById(
                                R.id.btnVerify
                        );

                btnUnpaid =
                        itemView.findViewById(
                                R.id.btnUnpaid
                        );

                btnRejectApplication =
                        itemView.findViewById(
                                R.id.btnRejectApplication
                        );
            }
        }
    }

    // =========================================================
    // REJECT APPLICATION DIALOG
    // =========================================================

    private void showRejectApplicationDialog(
            PaymentModel payment
    ) {
        final EditText input = new EditText(this);

        input.setHint("Enter rejection reason");
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);

        int padding =
                (int) (
                        16 *
                                getResources()
                                        .getDisplayMetrics()
                                        .density
                );

        input.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Reject Application")
                        .setMessage(
                                "Application ID: "
                                        + payment.applicationId
                                        + "\nStudent: "
                                        + payment.fullName
                                        + "\n\nThis will change the Application Status to REJECTED."
                                        + "\nPayment Status will be set to UNPAID."
                        )
                        .setView(input)
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "REJECT",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                String reason =
                        input.getText()
                                .toString()
                                .trim();

                if (reason.isEmpty()) {

                    input.setError(
                            "Rejection reason is required"
                    );

                    return;
                }

                dialog.dismiss();

                rejectApplication(
                        payment,
                        reason
                );
            });
        });

        dialog.show();
    }

    // =========================================================
    // REJECT APPLICATION
    // =========================================================

    private void rejectApplication(
            PaymentModel payment,
            String reason
    ) {
        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            goToAdminLogin();
            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        /*
         * IMPORTANT:
         * Reject is the ONLY action here that changes
         * the student's Application Status.
         */
        updates.put(
                "status",
                "REJECTED"
        );

        updates.put(
                "paymentStatus",
                "UNPAID"
        );

        updates.put(
                "applicationRejectionReason",
                reason
        );

        updates.put(
                "rejectedAt",
                FieldValue.serverTimestamp()
        );

        updates.put(
                "rejectedBy",
                user.getUid()
        );

        updates.put(
                "rejectedByEmail",
                user.getEmail() == null
                        ? ""
                        : user.getEmail()
        );

        updates.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("applications")
                .document(payment.documentId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    logAdminActivity(
                            "APPLICATION_REJECTED",
                            payment.applicationId,
                            "Application rejected for "
                                    + payment.fullName
                                    + ". Reason: "
                                    + reason
                    );

                    Toast.makeText(
                            this,
                            "Application rejected successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to reject application:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // PAYMENT DETAILS
    // =========================================================

    private void showPaymentDetails(
            PaymentModel payment
    ) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        int padding =
                (int) (
                        20 *
                                getResources()
                                        .getDisplayMetrics()
                                        .density
                );

        layout.setPadding(
                padding,
                5,
                padding,
                5
        );

        addDetail(
                layout,
                "Application ID",
                payment.applicationId
        );

        addDetail(
                layout,
                "Student Name",
                payment.fullName
        );

        addDetail(
                layout,
                "Email",
                payment.email
        );

        addDetail(
                layout,
                "Phone",
                payment.phone
        );

        addDetail(
                layout,
                "Payment Method",
                payment.paymentMethod
        );

        addDetail(
                layout,
                "Transaction ID",
                payment.transactionId
        );

        addDetail(
                layout,
                "Amount",
                "৳ " + payment.amount
        );

        addDetail(
                layout,
                "Application Status",
                payment.applicationStatus
        );

        addDetail(
                layout,
                "Payment Status",
                normalizeStatus(
                        payment.paymentStatus
                )
        );

        if (!payment.paymentRejectionReason
                .trim()
                .isEmpty()) {

            addDetail(
                    layout,
                    "Reason",
                    payment.paymentRejectionReason
            );
        }

        new AlertDialog.Builder(this)
                .setTitle("Payment Details")
                .setView(layout)
                .setPositiveButton(
                        "CLOSE",
                        null
                )
                .show();
    }

    private void addDetail(
            LinearLayout parent,
            String title,
            String value
    ) {

        TextView titleView =
                new TextView(this);

        titleView.setText(title);

        titleView.setTextColor(
                Color.parseColor("#718096")
        );

        titleView.setTextSize(11);

        titleView.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        TextView valueView =
                new TextView(this);

        valueView.setText(
                value == null || value.isEmpty()
                        ? "Not available"
                        : value
        );

        valueView.setTextColor(
                Color.parseColor("#102A43")
        );

        valueView.setTextSize(14);

        valueView.setPadding(
                0,
                2,
                0,
                12
        );

        parent.addView(titleView);
        parent.addView(valueView);
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        removeRealtimeListener();

        super.onDestroy();
    }
}