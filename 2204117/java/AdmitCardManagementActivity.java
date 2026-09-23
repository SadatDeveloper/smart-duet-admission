package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class AdmitCardManagementActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CloudinaryUploader cloudinaryUploader;

    private Uri authorizedSignatureUri;
    private static final int PICK_AUTHORIZED_SIGNATURE = 901;

    private ListenerRegistration applicationsListener;

    private RecyclerView recyclerAdmitCards;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private TextView tvPaidCount;
    private TextView tvGeneratedCount;
    private TextView tvPublishedCount;

    private EditText etSearch;

    private AdmitAdapter adapter;

    private final List<AdmitModel> allItems = new ArrayList<>();
    private final List<AdmitModel> filteredItems = new ArrayList<>();


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admit_card_management);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        cloudinaryUploader = new CloudinaryUploader(this);

        initViews();
        setupRecyclerView();
        setupSearch();

        checkAdminAndStartListener();
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initViews() {

        recyclerAdmitCards =
                findViewById(R.id.recyclerAdmitCards);

        progressBar =
                findViewById(R.id.progressBar);

        tvEmpty =
                findViewById(R.id.tvEmpty);

        tvPaidCount =
                findViewById(R.id.tvPaidCount);

        tvGeneratedCount =
                findViewById(R.id.tvGeneratedCount);

        tvPublishedCount =
                findViewById(R.id.tvPublishedCount);

        etSearch =
                findViewById(R.id.etSearch);

        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());
    }


    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private void setupRecyclerView() {

        recyclerAdmitCards.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new AdmitAdapter(filteredItems);

        recyclerAdmitCards.setAdapter(adapter);
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
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        applySearch();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }


    // =========================================================
    // ADMIN CHECK
    // =========================================================

    private void checkAdminAndStartListener() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Admin login required.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

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

                    progressBar.setVisibility(View.GONE);

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
    // REALTIME APPLICATION LISTENER
    // =========================================================

    private void startRealtimeListener() {

        removeRealtimeListener();

        applicationsListener =
                db.collection("applications")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    progressBar.setVisibility(
                                            View.GONE
                                    );

                                    if (error != null) {

                                        Toast.makeText(
                                                AdmitCardManagementActivity.this,
                                                "Unable to load applications:\n"
                                                        + error.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    if (snapshot == null) {
                                        return;
                                    }

                                    allItems.clear();

                                    for (
                                            DocumentSnapshot document :
                                            snapshot.getDocuments()
                                    ) {

                                        String paymentStatus =
                                                getStringValue(
                                                        document,
                                                        "paymentStatus"
                                                );

                                        String applicationStatus =
                                                getStringValue(
                                                        document,
                                                        "status"
                                                );

                                        // Only paid/verified applications
                                        // can receive admit card.
                                        if (!isPaid(paymentStatus)) {
                                            continue;
                                        }

                                        // Rejected applications cannot
                                        // receive admit card.
                                        if (applicationStatus
                                                .trim()
                                                .equalsIgnoreCase("REJECTED")) {
                                            continue;
                                        }

                                        AdmitModel item =
                                                createModel(document);

                                        allItems.add(item);
                                    }

                                    updateStatistics();
                                    applySearch();
                                }
                        );
    }


    // =========================================================
    // CREATE MODEL
    // =========================================================

    private AdmitModel createModel(
            DocumentSnapshot document) {

        AdmitModel item = new AdmitModel();

        item.documentId = document.getId();

        item.applicationId =
                getStringValue(
                        document,
                        "applicationId"
                );

        if (item.applicationId.isEmpty()) {
            item.applicationId = document.getId();
        }

        item.name =
                getStringValue(
                        document,
                        "fullName"
                );

        if (item.name.isEmpty()) {
            item.name =
                    getStringValue(
                            document,
                            "name"
                    );
        }

        item.email =
                getStringValue(
                        document,
                        "email"
                );

        item.phone =
                getStringValue(
                        document,
                        "phone"
                );

        item.fatherName =
                getStringValue(
                        document,
                        "fatherName"
                );

        item.department =
                getStringValue(
                        document,
                        "department"
                );

        item.identityNumber =
                getIdentityNumber(document);

        item.photoUrl =
                getStringValue(
                        document,
                        "photoUrl"
                );

        item.signatureUrl =
                getStringValue(
                        document,
                        "signatureUrl"
                );

        item.authorizedSignatureUrl =
                getStringValue(
                        document,
                        "authorizedSignatureUrl"
                );

        item.admitCardNo =
                getStringValue(
                        document,
                        "admitCardNo"
                );

        item.examDate =
                getStringValue(
                        document,
                        "examDate"
                );

        item.examTime =
                getStringValue(
                        document,
                        "examTime"
                );

        item.examCenter =
                getStringValue(
                        document,
                        "examCenter"
                );

        item.examDepartment =
                getStringValue(
                        document,
                        "examDepartment"
                );

        if (item.examDepartment.isEmpty()) {
            item.examDepartment = item.department;
        }

        item.seatNumber =
                getStringValue(
                        document,
                        "seatNumber"
                );

        item.instructions =
                getStringValue(
                        document,
                        "admitInstructions"
                );

        item.generated =
                Boolean.TRUE.equals(
                        document.getBoolean(
                                "admitCardGenerated"
                        )
                )
                        || !item.admitCardNo.isEmpty();

        item.published =
                Boolean.TRUE.equals(
                        document.getBoolean(
                                "admitCardPublished"
                        )
                );

        return item;
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void applySearch() {

        String query =
                etSearch.getText()
                        .toString()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        filteredItems.clear();

        for (AdmitModel item : allItems) {

            boolean matches =
                    query.isEmpty()

                            || contains(
                            item.applicationId,
                            query
                    )

                            || contains(
                            item.name,
                            query
                    )

                            || contains(
                            item.phone,
                            query
                    )

                            || contains(
                            item.email,
                            query
                    )

                            || contains(
                            item.admitCardNo,
                            query
                    )

                            || contains(
                            item.seatNumber,
                            query
                    );

            if (matches) {
                filteredItems.add(item);
            }
        }

        adapter.notifyDataSetChanged();

        updateEmptyState();
    }


    private boolean contains(
            String value,
            String keyword) {

        if (value == null || keyword == null) {
            return false;
        }

        return value
                .toLowerCase(Locale.ROOT)
                .contains(
                        keyword.toLowerCase(Locale.ROOT)
                );
    }


    // =========================================================
    // EMPTY STATE
    // =========================================================

    private void updateEmptyState() {

        if (filteredItems.isEmpty()) {

            tvEmpty.setVisibility(View.VISIBLE);
            recyclerAdmitCards.setVisibility(View.GONE);

            if (allItems.isEmpty()) {

                tvEmpty.setText(
                        "No paid applications available"
                );

            } else {

                tvEmpty.setText(
                        "No applications match your search"
                );
            }

        } else {

            tvEmpty.setVisibility(View.GONE);
            recyclerAdmitCards.setVisibility(View.VISIBLE);
        }
    }


    // =========================================================
    // STATISTICS
    // =========================================================

    private void updateStatistics() {

        int paid = allItems.size();

        int generated = 0;
        int published = 0;

        for (AdmitModel item : allItems) {

            if (item.generated) {
                generated++;
            }

            if (item.published) {
                published++;
            }
        }

        tvPaidCount.setText(
                String.valueOf(paid)
        );

        tvGeneratedCount.setText(
                String.valueOf(generated)
        );

        tvPublishedCount.setText(
                String.valueOf(published)
        );
    }


    // =========================================================
    // GENERATE / EDIT / REGENERATE DIALOG
    // =========================================================

    private void showGenerateDialog(
            AdmitModel item,
            boolean regenerate) {

        // Keep each applicant's upload selection isolated to this dialog.
        authorizedSignatureUri = null;

        View view =
                LayoutInflater.from(this)
                        .inflate(
                                R.layout.dialog_admit_card_form,
                                null
                        );

        EditText etAdmitNo = view.findViewById(R.id.etAdmitCardNo);
        EditText etExamDate = view.findViewById(R.id.etExamDate);
        EditText etExamTime = view.findViewById(R.id.etExamTime);
        EditText etCenter = view.findViewById(R.id.etExamCenter);
        EditText etDepartment = view.findViewById(R.id.etExamDepartment);
        EditText etSeat = view.findViewById(R.id.etSeatNumber);
        EditText etInstructions = view.findViewById(R.id.etInstructions);
        EditText etIdentityNumber = view.findViewById(R.id.etIdentityNumber);

        TextView tvSignatureStatus =
                view.findViewById(R.id.tvAuthorizedSignatureStatus);
        View btnUploadSignature =
                view.findViewById(R.id.btnUploadAuthorizedSignature);
        View btnGenerate =
                view.findViewById(R.id.btnGenerateAdmitCard);
        View btnCancel =
                view.findViewById(R.id.btnCancelAdmitCard);

        // Existing data for Edit / Generate Again
        etAdmitNo.setText(item.admitCardNo);
        etExamDate.setText(item.examDate);
        etExamTime.setText(item.examTime);
        etCenter.setText(item.examCenter);

        if (etIdentityNumber != null) {
            etIdentityNumber.setText(item.identityNumber);
        }

        etDepartment.setText(
                item.examDepartment.isEmpty()
                        ? item.department
                        : item.examDepartment
        );

        etSeat.setText(item.seatNumber);
        etInstructions.setText(item.instructions);

        String positiveText;
        if (regenerate) {
            positiveText = "GENERATE AGAIN";
        } else if (item.generated) {
            positiveText = "SAVE";
        } else {
            positiveText = "GENERATE";
        }

        // Existing signature is retained for Edit / Regenerate until a new one is selected.
        if (item.generated
                && item.authorizedSignatureUrl != null
                && !item.authorizedSignatureUrl.trim().isEmpty()) {
            tvSignatureStatus.setText("Current authorized signature is available");
        } else {
            tvSignatureStatus.setText("Required for a new admit card");
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(view)
                        .create();

        ((TextView) btnGenerate).setText(positiveText);

        btnUploadSignature.setOnClickListener(
                v -> {
                    chooseAuthorizedSignature();
                    tvSignatureStatus.setText("New signature selected — ready to upload");
                }
        );

        btnCancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        btnGenerate.setOnClickListener(
                v -> {
                    String admitNo =
                            etAdmitNo.getText().toString().trim();

                    String date =
                            etExamDate.getText().toString().trim();

                    String time =
                            etExamTime.getText().toString().trim();

                    String center =
                            etCenter.getText().toString().trim();

                    String department =
                            etDepartment.getText().toString().trim();

                    String seat =
                            etSeat.getText().toString().trim();

                    String instructions =
                            etInstructions.getText().toString().trim();

                    if (admitNo.isEmpty()) {
                        etAdmitNo.setError("Required");
                        etAdmitNo.requestFocus();
                        return;
                    }

                    if (date.isEmpty()) {
                        etExamDate.setError("Required");
                        etExamDate.requestFocus();
                        return;
                    }

                    if (time.isEmpty()) {
                        etExamTime.setError("Required");
                        etExamTime.requestFocus();
                        return;
                    }

                    if (center.isEmpty()) {
                        etCenter.setError("Required");
                        etCenter.requestFocus();
                        return;
                    }

                    if (seat.isEmpty()) {
                        etSeat.setError("Required");
                        etSeat.requestFocus();
                        return;
                    }

                    if (!item.generated
                            && authorizedSignatureUri == null) {
                        Toast.makeText(
                                this,
                                "Please upload Authorized Signature first.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (regenerate
                            && authorizedSignatureUri == null
                            && (item.authorizedSignatureUrl == null
                            || item.authorizedSignatureUrl.isEmpty())) {
                        Toast.makeText(
                                this,
                                "Please upload Authorized Signature first.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    btnGenerate.setEnabled(false);

                    if (authorizedSignatureUri != null) {
                        Toast.makeText(
                                this,
                                "Uploading Authorized Signature...",
                                Toast.LENGTH_SHORT
                        ).show();

                        uploadAuthorizedSignature(
                                item,
                                admitNo,
                                date,
                                time,
                                center,
                                department,
                                seat,
                                instructions,
                                regenerate,
                                dialog,
                                btnGenerate
                        );
                    } else {
                        saveAdmitCard(
                                item,
                                admitNo,
                                date,
                                time,
                                center,
                                department,
                                seat,
                                instructions,
                                regenerate,
                                item.authorizedSignatureUrl
                        );

                        dialog.dismiss();
                    }
                }
        );

        dialog.show();

        // Keep the custom action row compact and consistent across screen sizes.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }


    // =========================================================
    // SAVE ADMIT CARD
    // =========================================================

    private void saveAdmitCard(
            AdmitModel item,
            String admitNo,
            String date,
            String time,
            String center,
            String department,
            String seat,
            String instructions,
            boolean regenerate,
            String authorizedSignatureUrl) {

        FirebaseUser admin =
                auth.getCurrentUser();

        if (admin == null) {
            return;
        }


        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "admitCardNo",
                admitNo
        );

        updates.put(
                "examDate",
                date
        );

        updates.put(
                "examTime",
                time
        );

        updates.put(
                "examCenter",
                center
        );

        updates.put(
                "examDepartment",
                department
        );

        updates.put(
                "seatNumber",
                seat
        );

        updates.put(
                "admitInstructions",
                instructions
        );

        if (authorizedSignatureUrl != null
                && !authorizedSignatureUrl.trim().isEmpty()) {

            updates.put(
                    "authorizedSignatureUrl",
                    authorizedSignatureUrl.trim()
            );
        }

        updates.put(
                "admitCardGenerated",
                true
        );


        /*
         * Any Edit / Generate Again makes the
         * previous published version invalid.
         */
        updates.put(
                "admitCardPublished",
                false
        );

        updates.put(
                "admitCardGeneratedAt",
                FieldValue.serverTimestamp()
        );

        updates.put(
                "admitCardGeneratedBy",
                admin.getUid()
        );

        updates.put(
                "admitCardGeneratedByEmail",
                admin.getEmail() == null
                        ? ""
                        : admin.getEmail()
        );

        updates.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );


        db.collection("applications")
                .document(item.documentId)
                .update(updates)
                .addOnSuccessListener(
                        unused -> {

                            String action;
                            String description;

                            if (regenerate) {

                                action =
                                        "ADMIT_CARD_REGENERATED";

                                description =
                                        "Admit card generated again for "
                                                + safeName(item.name)
                                                + ".";

                            } else if (item.generated) {

                                action =
                                        "ADMIT_CARD_UPDATED";

                                description =
                                        "Admit card information updated for "
                                                + safeName(item.name)
                                                + ".";

                            } else {

                                action =
                                        "ADMIT_CARD_GENERATED";

                                description =
                                        "Admit card generated for "
                                                + safeName(item.name)
                                                + ".";
                            }


                            // Central Activity Log
                            AdminActivityLogger.log(
                                    action,
                                    item.applicationId,
                                    description
                            );


                            Toast.makeText(
                                    this,
                                    "Admit card saved successfully.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Unable to save admit card:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    // =========================================================
    // AUTHORIZED SIGNATURE - CLOUDINARY
    // =========================================================

    private void chooseAuthorizedSignature() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_AUTHORIZED_SIGNATURE
        );
    }


    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != PICK_AUTHORIZED_SIGNATURE
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {

            return;
        }

        authorizedSignatureUri = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                    authorizedSignatureUri,
                    data.getFlags()
                            & Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }

        Toast.makeText(
                this,
                "Authorized Signature selected.",
                Toast.LENGTH_SHORT
        ).show();
    }


    private void uploadAuthorizedSignature(
            AdmitModel item,
            String admitNo,
            String date,
            String time,
            String center,
            String department,
            String seat,
            String instructions,
            boolean regenerate,
            AlertDialog dialog,
            View btnGenerate) {

        if (authorizedSignatureUri == null) {
            return;
        }

        cloudinaryUploader.upload(
                authorizedSignatureUri,
                true,
                new CloudinaryUploader.UploadCallback() {

                    @Override
                    public void onSuccess(String secureUrl) {

                        runOnUiThread(() -> {

                            saveAdmitCard(
                                    item,
                                    admitNo,
                                    date,
                                    time,
                                    center,
                                    department,
                                    seat,
                                    instructions,
                                    regenerate,
                                    secureUrl
                            );

                            authorizedSignatureUri = null;
                            dialog.dismiss();
                        });
                    }

                    @Override
                    public void onError(String message) {

                        runOnUiThread(() -> {

                            btnGenerate.setEnabled(true);

                            Toast.makeText(
                                    AdmitCardManagementActivity.this,
                                    "Authorized Signature upload failed:\n"
                                            + message,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }


    // =========================================================
    // PUBLISH
    // =========================================================

    private void publishAdmitCard(
            AdmitModel item) {

        if (!item.generated) {

            Toast.makeText(
                    this,
                    "Generate the admit card first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (item.admitCardNo.isEmpty()
                || item.examDate.isEmpty()
                || item.examTime.isEmpty()
                || item.examCenter.isEmpty()
                || item.seatNumber.isEmpty()) {

            Toast.makeText(
                    this,
                    "Complete all required admit card information first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        new AlertDialog.Builder(this)
                .setTitle(
                        "Publish Admit Card"
                )
                .setMessage(
                        "Publish admit card for "
                                + safeName(item.name)
                                + "?\n\n"
                                + "The student will be able to view it."
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "PUBLISH",
                        (dialog, which) -> {

                            FirebaseUser admin =
                                    auth.getCurrentUser();

                            if (admin == null) {
                                return;
                            }


                            Map<String, Object> updates =
                                    new HashMap<>();

                            updates.put(
                                    "admitCardPublished",
                                    true
                            );

                            updates.put(
                                    "admitCardPublishedAt",
                                    FieldValue.serverTimestamp()
                            );

                            updates.put(
                                    "admitCardPublishedBy",
                                    admin.getUid()
                            );

                            updates.put(
                                    "admitCardPublishedByEmail",
                                    admin.getEmail() == null
                                            ? ""
                                            : admin.getEmail()
                            );

                            updates.put(
                                    "updatedAt",
                                    FieldValue.serverTimestamp()
                            );


                            db.collection("applications")
                                    .document(item.documentId)
                                    .update(updates)
                                    .addOnSuccessListener(
                                            unused -> {

                                                AdminActivityLogger.log(
                                                        "ADMIT_CARD_PUBLISHED",
                                                        item.applicationId,
                                                        "Admit card published for "
                                                                + safeName(item.name)
                                                                + "."
                                                );


                                                Toast.makeText(
                                                        this,
                                                        "Admit card published successfully.",
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            }
                                    )
                                    .addOnFailureListener(
                                            e -> {

                                                Toast.makeText(
                                                        this,
                                                        "Unable to publish admit card:\n"
                                                                + e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                    );
                        }
                )
                .show();
    }


    // =========================================================
    // PAYMENT STATUS
    // =========================================================

    private boolean isPaid(
            String status) {

        if (status == null) {
            return false;
        }

        String normalized =
                status.trim()
                        .toUpperCase(Locale.ROOT);

        return normalized.equals("PAID")
                || normalized.equals("VERIFIED")
                || normalized.equals("PAYMENT_VERIFIED")
                || normalized.equals("PAYMENT VERIFIED")
                || normalized.equals("CONFIRMED")
                || normalized.equals("SUCCESS")
                || normalized.equals("SUCCESSFUL");
    }


    // =========================================================
    // FIRESTORE STRING HELPER
    // =========================================================

    private String getStringValue(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        return value == null
                ? ""
                : String.valueOf(value).trim();
    }


    // =========================================================
    // SAFE NAME
    // =========================================================

    private String getIdentityNumber(
            DocumentSnapshot document) {

        String value =
                getStringValue(document, "identityNumber");

        if (!value.isEmpty()) {
            return value;
        }

        value = getStringValue(document, "identity_number");
        if (!value.isEmpty()) {
            return value;
        }

        value = getStringValue(document, "identityNo");
        if (!value.isEmpty()) {
            return value;
        }

        value = getStringValue(document, "identity");
        if (!value.isEmpty()) {
            return value;
        }

        Object personalInfo = document.get("personalInfo");
        if (personalInfo instanceof Map) {
            Object nested =
                    ((Map<?, ?>) personalInfo).get("identityNumber");
            if (nested != null) {
                return String.valueOf(nested).trim();
            }
        }

        return "";
    }

    private String safeName(String name) {

        if (name == null || name.trim().isEmpty()) {
            return "student";
        }

        return name.trim();
    }


    // =========================================================
    // REMOVE REALTIME LISTENER
    // =========================================================

    private void removeRealtimeListener() {

        if (applicationsListener != null) {

            applicationsListener.remove();

            applicationsListener = null;
        }
    }


    @Override
    protected void onDestroy() {

        removeRealtimeListener();

        super.onDestroy();
    }


    // =========================================================
    // MODEL
    // =========================================================

    private static class AdmitModel {

        String documentId = "";

        String applicationId = "";

        String name = "";

        String email = "";

        String phone = "";

        String fatherName = "";

        String department = "";

        String identityNumber = "";

        String photoUrl = "";

        String signatureUrl = "";
        String authorizedSignatureUrl = "";

        String admitCardNo = "";

        String examDate = "";

        String examTime = "";

        String examCenter = "";

        String examDepartment = "";

        String seatNumber = "";

        String instructions = "";

        boolean generated = false;

        boolean published = false;
    }


    // =========================================================
    // ADAPTER
    // =========================================================

    private class AdmitAdapter
            extends RecyclerView.Adapter<
            AdmitAdapter.Holder> {

        private final List<AdmitModel> list;


        AdmitAdapter(
                List<AdmitModel> list) {

            this.list = list;
        }


        @NonNull
        @Override
        public Holder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            View view =
                    LayoutInflater.from(
                            parent.getContext()
                    ).inflate(
                            R.layout.item_admit_card_management,
                            parent,
                            false
                    );

            return new Holder(view);
        }


        @Override
        public void onBindViewHolder(
                @NonNull Holder holder,
                int position) {

            AdmitModel item =
                    list.get(position);


            holder.tvApplicationId.setText(
                    item.applicationId
            );

            holder.tvStudentName.setText(
                    item.name.isEmpty()
                            ? "Student"
                            : item.name
            );

            holder.tvPhone.setText(
                    item.phone.isEmpty()
                            ? "Phone not available"
                            : item.phone
            );

            holder.tvPayment.setText(
                    "Payment: PAID"
            );


            String status;

            if (item.published) {

                status = "PUBLISHED";

            } else if (item.generated) {

                status = "GENERATED";

            } else {

                status = "NOT GENERATED";
            }


            holder.tvAdmitStatus.setText(status);


            GradientDrawable statusBackground =
                    new GradientDrawable();

            statusBackground.setCornerRadius(100);

            statusBackground.setColor(
                    Color.parseColor("#F1F5F9")
            );

            holder.tvAdmitStatus.setBackground(
                    statusBackground
            );


            if (item.published) {

                holder.tvAdmitStatus.setTextColor(
                        Color.parseColor("#7B1FA2")
                );

            } else if (item.generated) {

                holder.tvAdmitStatus.setTextColor(
                        Color.parseColor("#1769FF")
                );

            } else {

                holder.tvAdmitStatus.setTextColor(
                        Color.parseColor("#D97706")
                );
            }


            // Generate / Generate Again
            holder.btnGenerate.setText(
                    item.generated
                            ? "GENERATE AGAIN"
                            : "GENERATE"
            );


            // Edit only after generated
            holder.btnEdit.setVisibility(
                    item.generated
                            ? View.VISIBLE
                            : View.GONE
            );


            // Publish only after generated
            // and before published
            holder.btnPublish.setVisibility(
                    item.generated
                            && !item.published
                            ? View.VISIBLE
                            : View.GONE
            );


            /*
             * IMPORTANT:
             * Generate Again and Edit are now
             * separate actions.
             */

            holder.btnGenerate.setOnClickListener(
                    v -> showGenerateDialog(
                            item,
                            item.generated
                    )
            );


            holder.btnEdit.setOnClickListener(
                    v -> showGenerateDialog(
                            item,
                            false
                    )
            );


            holder.btnPublish.setOnClickListener(
                    v -> publishAdmitCard(item)
            );
        }


        @Override
        public int getItemCount() {
            return list.size();
        }


        // =====================================================
        // VIEW HOLDER
        // =====================================================

        class Holder
                extends RecyclerView.ViewHolder {

            TextView tvApplicationId;
            TextView tvStudentName;
            TextView tvPhone;
            TextView tvPayment;
            TextView tvAdmitStatus;

            Button btnGenerate;
            Button btnEdit;
            Button btnPublish;


            Holder(
                    @NonNull View itemView) {

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

                tvPayment =
                        itemView.findViewById(
                                R.id.tvPayment
                        );

                tvAdmitStatus =
                        itemView.findViewById(
                                R.id.tvAdmitStatus
                        );

                btnGenerate =
                        itemView.findViewById(
                                R.id.btnGenerate
                        );

                btnEdit =
                        itemView.findViewById(
                                R.id.btnEdit
                        );

                btnPublish =
                        itemView.findViewById(
                                R.id.btnPublish
                        );
            }
        }
    }
}
