package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
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

public class ResultManagementActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration applicationsRegistration;

    private RecyclerView recyclerResults;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTotalResults, tvPublishedResults,
            tvDraftResults, tvPassedResults, tvFailedResults;
    private EditText etSearch;
    private View btnBack;

    private final List<ResultItem> sourceItems = new ArrayList<>();
    private ResultAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_management);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupRecycler();
        setupSearch();
        btnBack.setOnClickListener(v -> finish());

        verifyAdmin();
    }

    private void bindViews() {
        recyclerResults = findViewById(R.id.recycler_results);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        tvTotalResults = findViewById(R.id.tv_total_results);
        tvPublishedResults = findViewById(R.id.tv_published_results);
        tvDraftResults = findViewById(R.id.tv_draft_results);
        tvPassedResults = findViewById(R.id.tv_passed_results);
        tvFailedResults = findViewById(R.id.tv_failed_results);
        etSearch = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupRecycler() {
        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultAdapter();
        recyclerResults.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void verifyAdmin() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        db.collection("admins").document(user.getUid()).get()
                .addOnSuccessListener(admin -> {
                    if (!admin.exists()) {
                        denyAdmin();
                        return;
                    }

                    Boolean active = admin.getBoolean("active");
                    String role = admin.getString("role");

                    if (!Boolean.TRUE.equals(active)
                            || role == null
                            || !"admin".equalsIgnoreCase(role.trim())) {
                        denyAdmin();
                        return;
                    }

                    listenApplications();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Unable to verify administrator.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void denyAdmin() {
        auth.signOut();
        Toast.makeText(this,
                "Administrator access required.",
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void listenApplications() {
        progressBar.setVisibility(View.VISIBLE);

        applicationsRegistration = db.collection("applications")
                .addSnapshotListener((snapshot, error) -> {
                    progressBar.setVisibility(View.GONE);

                    if (error != null || snapshot == null) {
                        sourceItems.clear();
                        adapter.setItems(new ArrayList<>());
                        showEmpty("Unable to load application results.");
                        updateStatistics();
                        return;
                    }

                    sourceItems.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        ResultItem item = ResultItem.from(document);

                        /*
                         * Result candidates:
                         * 1) Approved + verified/paid + published admit card
                         * 2) Any application that already contains result data
                         *
                         * This keeps the screen dynamic with the existing
                         * application-based Firestore architecture.
                         */
                        boolean eligible = isEligibleForResultEntry(item);
                        if (eligible) {
                            sourceItems.add(item);
                        }
                    }

                    updateStatistics();
                    applyFilter(etSearch.getText() == null
                            ? "" : etSearch.getText().toString());
                });
    }

    private boolean isEligibleForResultEntry(ResultItem item) {
        if (item.hasResultData) {
            return true;
        }

        return isApproved(item.applicationStatus)
                && isVerifiedPayment(item.paymentStatus)
                && item.admitCardPublished;
    }

    private boolean isApproved(String value) {
        String s = normalize(value);
        return s.equals("approved")
                || s.equals("approve")
                || s.equals("accepted")
                || s.equals("accept");
    }

    private boolean isVerifiedPayment(String value) {
        String s = normalize(value);
        return s.equals("paid")
                || s.equals("verified")
                || s.equals("payment_verified")
                || s.equals("payment verified")
                || s.equals("success")
                || s.equals("successful")
                || s.equals("confirmed");
    }

    private void applyFilter(String query) {
        String q = normalize(query);
        List<ResultItem> filtered = new ArrayList<>();

        for (ResultItem item : sourceItems) {
            if (q.isEmpty()
                    || contains(item.applicantName, q)
                    || contains(item.applicationNo, q)
                    || contains(item.admitCardNo, q)
                    || contains(item.department, q)
                    || contains(item.identityNumber, q)
                    || contains(item.resultStatus, q)) {
                filtered.add(item);
            }
        }

        adapter.setItems(filtered);

        boolean empty = filtered.isEmpty();
        recyclerResults.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

        if (empty) {
            tvEmpty.setText(sourceItems.isEmpty()
                    ? "No eligible applicants found."
                    : "No matching result records found.");
        }
    }

    private boolean contains(String value, String query) {
        return normalize(value).contains(query);
    }

    private void updateStatistics() {
        int published = 0;
        int draft = 0;
        int passed = 0;
        int failed = 0;

        for (ResultItem item : sourceItems) {
            if (item.hasResultData) {
                if (item.published) {
                    published++;
                } else {
                    draft++;
                }

                if ("pass".equalsIgnoreCase(item.resultStatus)) {
                    passed++;
                } else if ("fail".equalsIgnoreCase(item.resultStatus)) {
                    failed++;
                }
            }
        }

        tvTotalResults.setText(String.valueOf(sourceItems.size()));
        tvPublishedResults.setText(String.valueOf(published));
        tvDraftResults.setText(String.valueOf(draft));
        tvPassedResults.setText("Passed: " + passed);
        tvFailedResults.setText("Failed: " + failed);
    }

    private void showResultEditor(ResultItem item) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_result_form, null, false);

        TextView tvApplicant = view.findViewById(R.id.tv_dialog_applicant);
        TextView tvApplication = view.findViewById(R.id.tv_dialog_application);
        EditText etMarks = view.findViewById(R.id.et_marks);
        EditText etTotalMarks = view.findViewById(R.id.et_total_marks);
        EditText etMeritPosition = view.findViewById(R.id.et_merit_position);
        EditText etResultStatus = view.findViewById(R.id.et_result_status);
        EditText etRemarks = view.findViewById(R.id.et_remarks);
        View btnSave = view.findViewById(R.id.btn_save_result);
        View btnCancel = view.findViewById(R.id.btn_cancel_result);

        tvApplicant.setText(item.applicantName);
        tvApplication.setText(item.applicationNo);

        if (!item.marks.isEmpty()) etMarks.setText(item.marks);
        if (!item.totalMarks.isEmpty()) etTotalMarks.setText(item.totalMarks);
        if (!item.meritPosition.isEmpty()) etMeritPosition.setText(item.meritPosition);
        if (!item.resultStatus.isEmpty()) etResultStatus.setText(item.resultStatus);
        if (!item.remarks.isEmpty()) etRemarks.setText(item.remarks);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String marks = etMarks.getText().toString().trim();
            String totalMarks = etTotalMarks.getText().toString().trim();
            String merit = etMeritPosition.getText().toString().trim();
            String status = etResultStatus.getText().toString().trim().toUpperCase(Locale.ROOT);
            String remarks = etRemarks.getText().toString().trim();

            if (marks.isEmpty() || totalMarks.isEmpty()
                    || merit.isEmpty() || status.isEmpty()) {
                Toast.makeText(this,
                        "Please complete all required fields.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!status.equals("PASS") && !status.equals("FAIL")) {
                Toast.makeText(this,
                        "Result Status must be PASS or FAIL.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("result", status);
            updates.put("resultStatus", status);
            updates.put("marks", marks);
            updates.put("totalMarks", totalMarks);
            updates.put("meritPosition", merit);
            updates.put("remarks", remarks);

            // Editing always returns the result to Draft.
            // Admin must explicitly publish the updated result.
            updates.put("resultPublished", false);
            updates.put("resultAvailable", false);
            updates.put("resultUpdatedAt", FieldValue.serverTimestamp());

            btnSave.setEnabled(false);

            db.collection("applications")
                    .document(item.uid)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        logAdminAction(
                                "RESULT_SAVED",
                                item,
                                "Admission result saved as draft."
                        );

                        Toast.makeText(this,
                                "Result saved as Draft.",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this,
                                "Failed to save result.",
                                Toast.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    private void publishOrUnpublish(ResultItem item) {
        if (!item.hasResultData) {
            Toast.makeText(this,
                    "Enter and save the result before publishing.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean publish = !item.published;

        new AlertDialog.Builder(this)
                .setTitle(publish ? "Publish Result" : "Unpublish Result")
                .setMessage(publish
                        ? "After publishing, this result will become visible from the student's View Result section."
                        : "This will hide the result from the student's View Result section.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton(publish ? "PUBLISH" : "UNPUBLISH",
                        (dialog, which) -> updatePublication(item, publish))
                .show();
    }

    private void updatePublication(ResultItem item, boolean publish) {
        Map<String, Object> updates = new HashMap<>();

        /*
         * Both fields are maintained intentionally because the existing
         * student/dashboard architecture uses resultAvailable while the
         * result module also keeps the explicit resultPublished flag.
         */
        updates.put("resultPublished", publish);
        updates.put("resultAvailable", publish);

        if (publish) {
            updates.put("resultPublishedAt", FieldValue.serverTimestamp());
        } else {
            updates.put("resultPublishedAt", null);
        }

        db.collection("applications")
                .document(item.uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    logAdminAction(
                            publish ? "RESULT_PUBLISHED" : "RESULT_UNPUBLISHED",
                            item,
                            publish
                                    ? "Result published and made available to student."
                                    : "Result unpublished and hidden from student."
                    );

                    Toast.makeText(this,
                            publish
                                    ? "Result published successfully."
                                    : "Result unpublished successfully.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Unable to update publication status.",
                                Toast.LENGTH_LONG).show());
    }

    private void logAdminAction(String action, ResultItem item, String details) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> log = new HashMap<>();
        log.put("action", action);
        log.put("module", "Result Management");
        log.put("adminUid", user.getUid());
        log.put("applicationUid", item.uid);
        log.put("applicationNo", item.applicationNo);
        log.put("applicantName", item.applicantName);
        log.put("details", details);
        log.put("createdAt", FieldValue.serverTimestamp());

        db.collection("admin_activity_logs").add(log);
    }

    private void showEmpty(String message) {
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerResults.setVisibility(View.GONE);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void onDestroy() {
        if (applicationsRegistration != null) {
            applicationsRegistration.remove();
            applicationsRegistration = null;
        }
        super.onDestroy();
    }

    static class ResultItem {
        String uid = "";
        String applicantName = "Not available";
        String applicationNo = "Not available";
        String admitCardNo = "Not available";
        String department = "Not available";
        String identityNumber = "";
        String applicationStatus = "";
        String paymentStatus = "";
        String marks = "";
        String totalMarks = "";
        String meritPosition = "";
        String resultStatus = "";
        String remarks = "";
        boolean admitCardPublished = false;
        boolean published = false;
        boolean hasResultData = false;

        static ResultItem from(DocumentSnapshot d) {
            ResultItem item = new ResultItem();

            item.uid = d.getId();
            item.applicantName = first(d, "applicantName", "name");
            item.applicationNo = first(d, "applicationNo", "applicationNumber");
            item.admitCardNo = first(d, "admitCardNo", "admitCardNumber");
            item.department = first(d, "department", "departmentName");
            item.identityNumber = firstOptional(d,
                    "identityNumber", "identity_number",
                    "identityNo", "identity");

            item.applicationStatus = firstOptional(d, "status");
            item.paymentStatus = firstOptional(d, "paymentStatus");

            item.marks = firstOptional(d, "marks");
            item.totalMarks = firstOptional(d, "totalMarks");
            item.meritPosition = firstOptional(d, "meritPosition");
            item.resultStatus = firstOptional(d, "resultStatus", "result");
            item.remarks = firstOptional(d, "remarks", "resultRemarks");

            item.admitCardPublished =
                    Boolean.TRUE.equals(d.getBoolean("admitCardPublished"));

            item.published =
                    Boolean.TRUE.equals(d.getBoolean("resultPublished"))
                            || Boolean.TRUE.equals(d.getBoolean("resultAvailable"));

            item.hasResultData =
                    !item.marks.isEmpty()
                            || !item.totalMarks.isEmpty()
                            || !item.meritPosition.isEmpty()
                            || !item.resultStatus.isEmpty()
                            || d.contains("result");

            return item;
        }

        private static String first(DocumentSnapshot d, String... keys) {
            String value = firstOptional(d, keys);
            return value.isEmpty() ? "Not available" : value;
        }

        private static String firstOptional(DocumentSnapshot d, String... keys) {
            for (String key : keys) {
                Object value = d.get(key);
                if (value != null) {
                    String text = String.valueOf(value).trim();
                    if (!text.isEmpty()) return text;
                }
            }
            return "";
        }
    }

    private class ResultAdapter
            extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder> {

        private final List<ResultItem> items = new ArrayList<>();

        void setItems(List<ResultItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public ResultViewHolder onCreateViewHolder(
                android.view.ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_result_management, parent, false);

            return new ResultViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                ResultViewHolder holder, int position) {

            ResultItem item = items.get(position);

            holder.tvApplicantName.setText(item.applicantName);
            holder.tvApplicationNo.setText(item.applicationNo);
            holder.tvDepartment.setText(item.department);

            if (item.hasResultData) {
                holder.tvMarks.setText(
                        "Marks: " + item.marks + " / " + item.totalMarks
                                + "   •   Merit: " + item.meritPosition
                );
                holder.tvResultStatus.setText(
                        item.published ? "PUBLISHED" : "DRAFT"
                );
            } else {
                holder.tvMarks.setText("Result not entered yet");
                holder.tvResultStatus.setText("PENDING");
            }

            holder.btnEdit.setOnClickListener(v -> showResultEditor(item));

            holder.btnPublish.setEnabled(item.hasResultData);
            holder.btnPublish.setText(
                    item.published ? "UNPUBLISH" : "PUBLISH"
            );
            holder.btnPublish.setOnClickListener(
                    v -> publishOrUnpublish(item)
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ResultViewHolder extends RecyclerView.ViewHolder {
            TextView tvApplicantName, tvApplicationNo,
                    tvDepartment, tvResultStatus, tvMarks;
            MaterialButton btnEdit, btnPublish;

            ResultViewHolder(View itemView) {
                super(itemView);

                tvApplicantName =
                        itemView.findViewById(R.id.tv_applicant_name);
                tvApplicationNo =
                        itemView.findViewById(R.id.tv_application_no);
                tvDepartment =
                        itemView.findViewById(R.id.tv_department);
                tvResultStatus =
                        itemView.findViewById(R.id.tv_result_status);
                tvMarks =
                        itemView.findViewById(R.id.tv_marks);
                btnEdit =
                        itemView.findViewById(R.id.btn_edit_result);
                btnPublish =
                        itemView.findViewById(R.id.btn_publish_result);
            }
        }
    }
}
