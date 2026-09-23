package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NoticeManagementActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration noticeListener;

    private RecyclerView recyclerNotices;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTotal, tvPublished, tvDraft;
    private EditText etSearch;
    private MaterialButton btnAddNotice, btnBack;

    private final List<NoticeModel> allNotices = new ArrayList<>();
    private final List<NoticeModel> filteredNotices = new ArrayList<>();
    private NoticeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_management);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupRecycler();
        setupActions();
        verifyAdminAndLoad();
    }

    private void bindViews() {
        recyclerNotices = findViewById(R.id.recyclerNotices);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTotal = findViewById(R.id.tvTotal);
        tvPublished = findViewById(R.id.tvPublished);
        tvDraft = findViewById(R.id.tvDraft);
        etSearch = findViewById(R.id.etSearch);
        btnAddNotice = findViewById(R.id.btnAddNotice);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecycler() {
        recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoticeAdapter();
        recyclerNotices.setAdapter(adapter);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());
        btnAddNotice.setOnClickListener(v -> showNoticeDialog(null));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void verifyAdminAndLoad() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please login as admin.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setLoading(true);

        db.collection("admins")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(adminDoc -> {
                    boolean active = Boolean.TRUE.equals(adminDoc.getBoolean("active"));
                    String role = safe(adminDoc.getString("role"));

                    if (!active || !"admin".equalsIgnoreCase(role)) {
                        setLoading(false);
                        Toast.makeText(this, "Admin access denied.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    listenToNotices();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not verify admin access.", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void listenToNotices() {
        if (noticeListener != null) noticeListener.remove();

        noticeListener = db.collection("notices")
                .addSnapshotListener((snapshots, error) -> {
                    setLoading(false);

                    if (error != null) {
                        Toast.makeText(this, "Failed to load notices: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    allNotices.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            allNotices.add(NoticeModel.from(doc));
                        }
                    }

                    updateStats();
                    applySearch();
                });
    }

    private void applySearch() {
        String q = safe(etSearch.getText() == null ? "" : etSearch.getText().toString())
                .trim().toLowerCase(Locale.ROOT);

        filteredNotices.clear();

        for (NoticeModel n : allNotices) {
            if (q.isEmpty()
                    || contains(n.title, q)
                    || contains(n.subject, q)
                    || contains(n.description, q)
                    || contains(n.authority, q)
                    || contains(n.category, q)
                    || contains(n.priority, q)) {
                filteredNotices.add(n);
            }
        }

        adapter.notifyDataSetChanged();

        boolean empty = filteredNotices.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerNotices.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (allNotices.isEmpty()) {
            tvEmpty.setText("No notices created yet.");
        } else if (empty) {
            tvEmpty.setText("No notices match your search.");
        }
    }

    private void updateStats() {
        int published = 0;
        int draft = 0;

        for (NoticeModel n : allNotices) {
            if (n.published) published++;
            else draft++;
        }

        tvTotal.setText(String.valueOf(allNotices.size()));
        tvPublished.setText(String.valueOf(published));
        tvDraft.setText(String.valueOf(draft));
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private void showNoticeDialog(NoticeModel existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_notice_form, null);

        EditText etTitle = view.findViewById(R.id.etNoticeTitle);
        EditText etDescription = view.findViewById(R.id.etNoticeDescription);
        EditText etSubject = view.findViewById(R.id.etNoticeSubject);
        EditText etAuthority = view.findViewById(R.id.etNoticeAuthority);
        EditText etCategory = view.findViewById(R.id.etNoticeCategory);
        EditText etPriority = view.findViewById(R.id.etNoticePriority);

        if (existing != null) {
            etTitle.setText(existing.title);
            etDescription.setText(existing.description);
            etSubject.setText(existing.subject);
            etAuthority.setText("DUET Admission Test");
            etAuthority.setEnabled(false);
            etCategory.setText(existing.category);
            etPriority.setText(existing.priority);
        } else {
            etSubject.setText("");
            etAuthority.setText("DUET Admission Test");
            etCategory.setText("General");
            etPriority.setText("Normal");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Create Notice" : "Edit Notice")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(existing == null ? "Create" : "Save", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = etTitle.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();
                    String subject = etSubject.getText().toString().trim();
                    String authority = "DUET Admission Test";
                    String category = etCategory.getText().toString().trim();
                    String priority = etPriority.getText().toString().trim();

                    if (title.isEmpty()) {
                        etTitle.setError("Title is required");
                        return;
                    }
                    if (description.isEmpty()) {
                        etDescription.setError("Description is required");
                        return;
                    }
                    if (category.isEmpty()) category = "General";
                    if (priority.isEmpty()) priority = "Normal";
                    if (authority.isEmpty()) authority = "DUET Admission Test";

                    saveNotice(existing, title, subject, description, authority, category, priority, dialog);
                }));

        dialog.show();
    }

    private void saveNotice(
            NoticeModel existing,
            String title,
            String subject,
            String description,
            String authority,
            String category,
            String priority,
            AlertDialog dialog) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("subject", subject);
        data.put("description", description);
        data.put("authority", authority);
        data.put("category", category);
        data.put("priority", priority);
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedBy", user.getUid());

        if (existing == null) {
            data.put("published", false);
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("createdBy", user.getUid());

            db.collection("notices")
                    .add(data)
                    .addOnSuccessListener(ref -> {
                        dialog.dismiss();
                        AdminActivityLogger.log(
                                "NOTICE_CREATED",
                                "",
                                "Created notice: " + title
                        );
                        Toast.makeText(this, "Notice created as draft.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            // Preserve existing publish state. If already published, refresh publishedAt only.
            if (existing.published) {
                data.put("publishedAt", FieldValue.serverTimestamp());
                data.put("published", true);
            }

            db.collection("notices")
                    .document(existing.id)
                    .update(data)
                    .addOnSuccessListener(unused -> {
                        dialog.dismiss();
                        AdminActivityLogger.log(
                                "NOTICE_UPDATED",
                                existing.id,
                                "Updated notice: " + title
                        );
                        Toast.makeText(this, "Notice updated.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void togglePublish(NoticeModel n) {
        Map<String, Object> data = new HashMap<>();
        boolean publish = !n.published;

        data.put("published", publish);
        data.put("updatedAt", FieldValue.serverTimestamp());

        if (publish) {
            data.put("publishedAt", FieldValue.serverTimestamp());
        } else {
            data.put("unpublishedAt", FieldValue.serverTimestamp());
        }

        db.collection("notices")
                .document(n.id)
                .update(data)
                .addOnSuccessListener(unused -> {
                    AdminActivityLogger.log(
                            publish ? "NOTICE_PUBLISHED" : "NOTICE_UNPUBLISHED",
                            n.id,
                            (publish ? "Published notice: " : "Unpublished notice: ") + n.title
                    );
                    Toast.makeText(
                            this,
                            publish ? "Notice published." : "Notice unpublished.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void confirmDelete(NoticeModel n) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notice?")
                .setMessage("This will permanently delete:\n\n" + n.title)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("notices")
                            .document(n.id)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                AdminActivityLogger.log(
                                        "NOTICE_DELETED",
                                        n.id,
                                        "Deleted notice: " + n.title
                                );
                                Toast.makeText(this, "Notice deleted.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Delete failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private void showNoticePreview(NoticeModel n) {
        String status = n.published ? "PUBLISHED" : "DRAFT";

        new AlertDialog.Builder(this)
                .setTitle(n.title)
                .setMessage(
                        "Subject: " + n.subject +
                                "\nAuthority: " + n.authority +
                                "\nCategory: " + n.category +
                                "\nPriority: " + n.priority +
                                "\nStatus: " + status +
                                "\n\n" + n.description +
                                "\n\nCreated: " + formatDate(n.createdAt)
                )
                .setPositiveButton("Close", null)
                .show();
    }

    private String formatDate(Date date) {
        if (date == null) return "—";
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(date);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private Date getDate(Object value) {
        if (value instanceof Timestamp) return ((Timestamp) value).toDate();
        if (value instanceof Date) return (Date) value;
        return null;
    }

    @Override
    protected void onDestroy() {
        if (noticeListener != null) noticeListener.remove();
        super.onDestroy();
    }

    private class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notice_management, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            NoticeModel n = filteredNotices.get(position);

            h.tvTitle.setText(n.title);
            h.tvDescription.setText(n.description);
            h.tvCategory.setText(n.category);
            h.tvPriority.setText(n.priority);
            h.tvStatus.setText(n.published ? "PUBLISHED" : "DRAFT");
            h.tvDate.setText(formatDate(n.createdAt));

            h.tvStatus.setBackgroundResource(
                    n.published ? R.drawable.bg_notice_published : R.drawable.bg_notice_draft
            );

            h.btnView.setOnClickListener(v -> showNoticePreview(n));
            h.btnEdit.setOnClickListener(v -> showNoticeDialog(n));
            h.btnPublish.setText(n.published ? "Unpublish" : "Publish");
            h.btnPublish.setOnClickListener(v -> togglePublish(n));
            h.btnDelete.setOnClickListener(v -> confirmDelete(n));
        }

        @Override
        public int getItemCount() {
            return filteredNotices.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDescription, tvCategory, tvPriority, tvStatus, tvDate;
            MaterialButton btnView, btnEdit, btnPublish, btnDelete;

            Holder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNoticeTitle);
                tvDescription = itemView.findViewById(R.id.tvNoticeDescription);
                tvCategory = itemView.findViewById(R.id.tvNoticeCategory);
                tvPriority = itemView.findViewById(R.id.tvNoticePriority);
                tvStatus = itemView.findViewById(R.id.tvNoticeStatus);
                tvDate = itemView.findViewById(R.id.tvNoticeDate);
                btnView = itemView.findViewById(R.id.btnViewNotice);
                btnEdit = itemView.findViewById(R.id.btnEditNotice);
                btnPublish = itemView.findViewById(R.id.btnPublishNotice);
                btnDelete = itemView.findViewById(R.id.btnDeleteNotice);
            }
        }
    }

    private static class NoticeModel {
        String id = "";
        String title = "";
        String subject = "";
        String description = "";
        String authority = "DUET Admission Test";
        String category = "General";
        String priority = "Normal";
        boolean published = false;
        Date createdAt;
        Date publishedAt;

        static NoticeModel from(DocumentSnapshot d) {
            NoticeModel n = new NoticeModel();
            n.id = d.getId();
            n.title = safe(d.getString("title"));
            n.subject = safe(d.getString("subject"));
            n.description = safe(d.getString("description"));
            n.authority = safe(d.getString("authority"));
            if (n.authority.isEmpty()) n.authority = "DUET Admission Test";
            n.category = safe(d.getString("category"));
            n.priority = safe(d.getString("priority"));
            if (n.category.isEmpty()) n.category = "General";
            if (n.priority.isEmpty()) n.priority = "Normal";
            n.published = Boolean.TRUE.equals(d.getBoolean("published"));

            Object created = d.get("createdAt");
            if (created instanceof Timestamp) n.createdAt = ((Timestamp) created).toDate();
            else if (created instanceof Date) n.createdAt = (Date) created;

            Object published = d.get("publishedAt");
            if (published instanceof Timestamp) n.publishedAt = ((Timestamp) published).toDate();
            else if (published instanceof Date) n.publishedAt = (Date) published;

            return n;
        }
    }
}