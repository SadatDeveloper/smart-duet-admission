package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyllabusManagementActivity extends AppCompatActivity {

    private static final String OFFICIAL_SYLLABUS_URL =
            "https://www.duet.ac.bd/storage/notice/2026/Apr/2026-04-19_1776578396_243.pdf";
    private static final int PICK_SYLLABUS_PDF = 6201;

    private final ExecutorService pdfExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean pdfDialogOpen = false;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private DocumentSnapshot currentDocument;
    private String officialSyllabusUrl = OFFICIAL_SYLLABUS_URL;

    private TextView tvBack, tvStatus, tvLastUpdated;
    private TextView tvPreviewTitle, tvPreviewSession;
    private TextView tvPreviewQuestionType, tvPreviewTotalMarks, tvPreviewExamDuration;
    private TextView tvPreviewMathTopics, tvPreviewPhysicsTopics,
            tvPreviewChemistryTopics, tvPreviewEnglishTopics, tvPreviewDiplomaTopics;
    private TextView tvOfficialUrlPreview;
    private com.google.android.material.button.MaterialButton btnViewOfficialSyllabus;

    private com.google.android.material.button.MaterialButton btnEdit;
    private com.google.android.material.button.MaterialButton btnPublish;
    private com.google.android.material.button.MaterialButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syllabus_management);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupActions();
        verifyAdminAndListen();
    }

    private void bindViews() {
        tvBack = findViewById(R.id.tvBackSyllabusManagement);
        tvStatus = findViewById(R.id.tvSyllabusStatus);
        tvLastUpdated = findViewById(R.id.tvSyllabusLastUpdated);

        tvPreviewTitle = findViewById(R.id.tvPreviewTitle);
        tvPreviewSession = findViewById(R.id.tvPreviewSession);
        tvPreviewQuestionType = findViewById(R.id.tvPreviewQuestionType);
        tvPreviewTotalMarks = findViewById(R.id.tvPreviewTotalMarks);
        tvPreviewExamDuration = findViewById(R.id.tvPreviewExamDuration);

        tvPreviewMathTopics = findViewById(R.id.tvPreviewMathTopics);
        tvPreviewPhysicsTopics = findViewById(R.id.tvPreviewPhysicsTopics);
        tvPreviewChemistryTopics = findViewById(R.id.tvPreviewChemistryTopics);
        tvPreviewEnglishTopics = findViewById(R.id.tvPreviewEnglishTopics);
        tvPreviewDiplomaTopics = findViewById(R.id.tvPreviewDiplomaTopics);
        tvOfficialUrlPreview = findViewById(R.id.tvOfficialUrlPreview);
        btnViewOfficialSyllabus = findViewById(R.id.btnViewOfficialSyllabus);

        btnEdit = findViewById(R.id.btnEditSyllabus);
        btnPublish = findViewById(R.id.btnPublishSyllabus);
        btnDelete = findViewById(R.id.btnDeleteSyllabus);
    }

    private void setupActions() {
        tvBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnPublish.setOnClickListener(v -> togglePublish());
        btnDelete.setOnClickListener(v -> confirmDelete());

        btnViewOfficialSyllabus.setOnClickListener(v -> showOfficialSyllabusPdf());
    }

    private void verifyAdminAndListen() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please login as admin.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db.collection("admins")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(admin -> {
                    boolean active = Boolean.TRUE.equals(admin.getBoolean("active"));
                    String role = safe(admin.getString("role"));

                    if (!active || !"admin".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Admin access denied.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    listenSyllabus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Unable to verify admin.", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void listenSyllabus() {
        if (listener != null) listener.remove();

        listener = db.collection("syllabus")
                .document("current")
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Toast.makeText(
                                this,
                                "Unable to load syllabus.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    currentDocument = doc;

                    if (doc == null || !doc.exists()) {
                        showEmptyState();
                        return;
                    }

                    boolean published = Boolean.TRUE.equals(doc.getBoolean("published"));

                    tvStatus.setText(published ? "PUBLISHED" : "DRAFT");
                    tvStatus.setTextColor(
                            getColor(published ? android.R.color.holo_green_dark
                                    : android.R.color.holo_blue_dark)
                    );
                    tvLastUpdated.setText(
                            published
                                    ? "Published • visible to students"
                                    : "Saved as draft • not visible to students"
                    );
                    btnPublish.setText(
                            published ? "Unpublish Syllabus" : "Publish Syllabus"
                    );

                    bindPreview(doc);
                });
    }

    private void showEmptyState() {
        tvStatus.setText("NOT CREATED");
        tvLastUpdated.setText("No syllabus has been created yet.");
        btnPublish.setText("Publish Syllabus");
        officialSyllabusUrl = OFFICIAL_SYLLABUS_URL;

        setPreview(tvPreviewTitle, "Admission Test Syllabus");
        setPreview(tvPreviewSession, "Academic Session 2026");
        setPreview(tvPreviewQuestionType, "Multiple Choice Questions");
        setPreview(tvPreviewTotalMarks, "As per official admission circular");
        setPreview(tvPreviewExamDuration, "As per official admission circular");
        setPreview(tvPreviewMathTopics, "No content published yet.");
        setPreview(tvPreviewPhysicsTopics, "No content published yet.");
        setPreview(tvPreviewChemistryTopics, "No content published yet.");
        setPreview(tvPreviewEnglishTopics, "No content published yet.");
        setPreview(tvPreviewDiplomaTopics, "No content published yet.");
        setPreview(tvOfficialUrlPreview, "DUET Official Admission Syllabus - 2026\n" + OFFICIAL_SYLLABUS_URL);
    }

    private void bindPreview(DocumentSnapshot doc) {
        setPreview(tvPreviewTitle, getValue(doc, "title", "Admission Test Syllabus"));
        setPreview(tvPreviewSession, getValue(doc, "session", "Academic Session 2026"));
        setPreview(tvPreviewQuestionType, getValue(doc, "questionType", "Multiple Choice Questions"));
        setPreview(tvPreviewTotalMarks, getValue(doc, "totalMarks", "As per official admission circular"));
        setPreview(tvPreviewExamDuration, getValue(doc, "examDuration", "As per official admission circular"));

        setPreview(tvPreviewMathTopics, getValue(doc, "mathematics", "Not provided"));
        setPreview(tvPreviewPhysicsTopics, getValue(doc, "physics", "Not provided"));
        setPreview(tvPreviewChemistryTopics, getValue(doc, "chemistry", "Not provided"));
        setPreview(tvPreviewEnglishTopics, getValue(doc, "english", "Not provided"));
        setPreview(tvPreviewDiplomaTopics, getValue(doc, "diplomaTechnology", "Not provided"));
        officialSyllabusUrl = getValue(doc, "officialUrl", OFFICIAL_SYLLABUS_URL);
        setPreview(tvOfficialUrlPreview, "DUET Official Admission Syllabus - 2026\n" + officialSyllabusUrl);
    }

    private void showOfficialSyllabusPdf() {
        if (TextUtils.isEmpty(officialSyllabusUrl)) {
            Toast.makeText(this, "Official syllabus PDF link is not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pdfDialogOpen) {
            return;
        }

        pdfDialogOpen = true;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(14), dp(10), dp(8), dp(10));

        TextView title = new TextView(this);
        title.setText("Official DUET Syllabus");
        title.setTextColor(Color.parseColor("#173F6B"));
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(0, dp(46), 1f);
        topBar.addView(title, titleParams);

        TextView close = new TextView(this);
        close.setText("×");
        close.setGravity(Gravity.CENTER);
        close.setTextSize(30);
        close.setTextColor(Color.parseColor("#667085"));
        close.setPadding(dp(8), 0, dp(8), 0);
        close.setOnClickListener(v -> {
            if (pdfRenderer != null) {
                try { pdfRenderer.close(); } catch (Exception ignored) {}
                pdfRenderer = null;
            }
            if (pdfFileDescriptor != null) {
                try { pdfFileDescriptor.close(); } catch (Exception ignored) {}
                pdfFileDescriptor = null;
            }
            pdfDialogOpen = false;
        });
        topBar.addView(close,
                new LinearLayout.LayoutParams(dp(50), dp(46)));

        root.addView(topBar);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(10), dp(6), dp(10), dp(8));

        TextView zoomOut = zoomButton("−");
        TextView zoomLabel = zoomButton("100%");
        TextView zoomIn = zoomButton("+");

        controls.addView(zoomOut);
        controls.addView(zoomLabel);
        controls.addView(zoomIn);

        root.addView(controls);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout pages = new LinearLayout(this);
        pages.setOrientation(LinearLayout.VERTICAL);
        pages.setPadding(dp(8), dp(8), dp(8), dp(24));

        scrollView.addView(pages);
        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        ProgressBar progress = new ProgressBar(this);
        progress.setVisibility(View.VISIBLE);

        LinearLayout progressWrap = new LinearLayout(this);
        progressWrap.setGravity(Gravity.CENTER);
        progressWrap.setPadding(0, dp(10), 0, dp(10));
        progressWrap.addView(progress);

        root.addView(progressWrap);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .create();

        dialog.setOnDismissListener(d -> {
            pdfDialogOpen = false;
            if (pdfRenderer != null) {
                try { pdfRenderer.close(); } catch (Exception ignored) {}
                pdfRenderer = null;
            }
            if (pdfFileDescriptor != null) {
                try { pdfFileDescriptor.close(); } catch (Exception ignored) {}
                pdfFileDescriptor = null;
            }
        });

        final float[] scale = {1.0f};

        zoomOut.setOnClickListener(v -> {
            scale[0] = Math.max(0.5f, scale[0] - 0.1f);
            zoomLabel.setText(((int) (scale[0] * 100)) + "%");
            renderCurrentPages(pdfRenderer, pages, scale[0]);
        });

        zoomIn.setOnClickListener(v -> {
            scale[0] = Math.min(2.0f, scale[0] + 0.1f);
            zoomLabel.setText(((int) (scale[0] * 100)) + "%");
            renderCurrentPages(pdfRenderer, pages, scale[0]);
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent
            );
            int width = (int) (getResources()
                    .getDisplayMetrics().widthPixels * 0.96f);
            int height = (int) (getResources()
                    .getDisplayMetrics().heightPixels * 0.92f);
            dialog.getWindow().setLayout(width, height);
        }

        downloadAndRenderOfficialPdf(
                dialog,
                pages,
                progress
        );
    }

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pdfFileDescriptor;

    private void downloadAndRenderOfficialPdf(
            AlertDialog dialog,
            LinearLayout pages,
            ProgressBar progress
    ) {
        pdfExecutor.execute(() -> {
            File file = null;

            try {
                URL url = new URL(officialSyllabusUrl);
                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                int responseCode = connection.getResponseCode();

                if (responseCode < 200 || responseCode >= 300) {
                    throw new IllegalStateException(
                            "HTTP " + responseCode
                    );
                }

                file = new File(
                        getCacheDir(),
                        "duet_official_syllabus_2026.pdf"
                );

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output =
                             new FileOutputStream(file)) {

                    byte[] buffer = new byte[8192];
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                } finally {
                    connection.disconnect();
                }

                File finalFile = file;

                runOnUiThread(() -> {
                    try {
                        pdfFileDescriptor =
                                ParcelFileDescriptor.open(
                                        finalFile,
                                        ParcelFileDescriptor.MODE_READ_ONLY
                                );

                        pdfRenderer =
                                new PdfRenderer(pdfFileDescriptor);

                        renderCurrentPages(
                                pdfRenderer,
                                pages,
                                1.0f
                        );

                        progress.setVisibility(View.GONE);

                    } catch (Exception e) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(
                                this,
                                "PDF could not be opened.",
                                Toast.LENGTH_LONG
                        ).show();
                        dialog.dismiss();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            "Unable to load official syllabus PDF.",
                            Toast.LENGTH_LONG
                    ).show();
                    dialog.dismiss();
                });
            }
        });
    }

    private void renderCurrentPages(
            PdfRenderer renderer,
            LinearLayout pages,
            float scale
    ) {
        if (renderer == null) {
            return;
        }

        pages.removeAllViews();

        int pageCount = renderer.getPageCount();

        for (int i = 0; i < pageCount; i++) {
            PdfRenderer.Page page = renderer.openPage(i);

            int baseWidth =
                    getResources().getDisplayMetrics().widthPixels - dp(42);

            float ratio =
                    (float) page.getHeight()
                            / (float) page.getWidth();

            int width = Math.max(
                    dp(280),
                    (int) (baseWidth * scale)
            );

            int height = (int) (width * ratio);

            Bitmap bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
            );

            bitmap.eraseColor(Color.WHITE);

            page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            );

            page.close();

            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setAdjustViewBounds(true);
            image.setBackgroundColor(Color.WHITE);
            image.setPadding(dp(2), dp(2), dp(2), dp(10));

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.gravity = Gravity.CENTER_HORIZONTAL;
            pages.addView(image, params);
        }
    }

    private TextView zoomButton(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.parseColor("#173F6B"));
        view.setTextSize(18);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setBackgroundColor(Color.parseColor("#EEF5FF"));
        view.setPadding(dp(16), 0, dp(16), 0);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dp(66),
                        dp(42)
                );
        params.setMargins(dp(4), 0, dp(4), 0);

        view.setLayoutParams(params);
        return view;
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void showEditDialog() {
        selectedSyllabusPdfUri = null;
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_syllabus_form, null);

        EditText etTitle = view.findViewById(R.id.etSyllabusTitle);
        EditText etSession = view.findViewById(R.id.etSyllabusSession);
        EditText etQuestionType = view.findViewById(R.id.etQuestionType);
        EditText etTotalMarks = view.findViewById(R.id.etTotalMarks);
        EditText etExamDuration = view.findViewById(R.id.etExamDuration);
        EditText etMath = view.findViewById(R.id.etMathTopics);
        EditText etPhysics = view.findViewById(R.id.etPhysicsTopics);
        EditText etChemistry = view.findViewById(R.id.etChemistryTopics);
        EditText etEnglish = view.findViewById(R.id.etEnglishTopics);
        EditText etDiploma = view.findViewById(R.id.etDiplomaTopics);
        EditText etUrl = view.findViewById(R.id.etOfficialUrl);
        com.google.android.material.button.MaterialButton btnUploadPdf =
                view.findViewById(R.id.btnUploadSyllabusPdf);
        TextView tvPdfStatus = view.findViewById(R.id.tvPdfUploadStatus);

        if (currentDocument != null && currentDocument.exists()) {
            copyValue(etTitle, "title", "Admission Test Syllabus");
            copyValue(etSession, "session", "Academic Session 2026");
            copyValue(etQuestionType, "questionType", "Multiple Choice Questions");
            copyValue(etTotalMarks, "totalMarks", "");
            copyValue(etExamDuration, "examDuration", "");
            copyValue(etMath, "mathematics", "");
            copyValue(etPhysics, "physics", "");
            copyValue(etChemistry, "chemistry", "");
            copyValue(etEnglish, "english", "");
            copyValue(etDiploma, "diplomaTechnology", "");
            etUrl.setText(getValue(currentDocument, "officialUrl", OFFICIAL_SYLLABUS_URL));
        } else {
            etTitle.setText("Admission Test Syllabus");
            etSession.setText("Academic Session 2026");
            etQuestionType.setText("Multiple Choice Questions");
            etUrl.setText(OFFICIAL_SYLLABUS_URL);
        }

        if (btnUploadPdf != null) {
            btnUploadPdf.setOnClickListener(v -> {
                chooseSyllabusPdf();
                if (tvPdfStatus != null) {
                    tvPdfStatus.setText("Select the PDF from your device.");
                }
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Admission Syllabus")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save Draft", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (text(etTitle).isEmpty()) {
                        etTitle.setError("Title is required");
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    if (selectedSyllabusPdfUri != null) {
                        if (btnUploadPdf != null) btnUploadPdf.setEnabled(false);
                        if (tvPdfStatus != null) tvPdfStatus.setText("Uploading PDF...");

                        new CloudinaryUploader(this).upload(
                                selectedSyllabusPdfUri,
                                false,
                                new CloudinaryUploader.UploadCallback() {
                                    @Override
                                    public void onSuccess(String secureUrl) {
                                        etUrl.setText(secureUrl);
                                        selectedSyllabusPdfUri = null;
                                        if (btnUploadPdf != null) btnUploadPdf.setEnabled(true);
                                        if (tvPdfStatus != null) tvPdfStatus.setText("PDF uploaded successfully.");

                                        saveSyllabusData(
                                                dialog,
                                                etTitle,
                                                etSession,
                                                etQuestionType,
                                                etTotalMarks,
                                                etExamDuration,
                                                etMath,
                                                etPhysics,
                                                etChemistry,
                                                etEnglish,
                                                etDiploma,
                                                etUrl
                                        );
                                    }

                                    @Override
                                    public void onError(String message) {
                                        if (btnUploadPdf != null) btnUploadPdf.setEnabled(true);
                                        if (tvPdfStatus != null) tvPdfStatus.setText(message);
                                        Toast.makeText(
                                                SyllabusManagementActivity.this,
                                                "PDF upload failed: " + message,
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );
                        return;
                    }

                    saveSyllabusData(
                            dialog,
                            etTitle,
                            etSession,
                            etQuestionType,
                            etTotalMarks,
                            etExamDuration,
                            etMath,
                            etPhysics,
                            etChemistry,
                            etEnglish,
                            etDiploma,
                            etUrl
                    );
                }));

        dialog.show();
    }

    private void saveSyllabusData(
            AlertDialog dialog,
            EditText etTitle,
            EditText etSession,
            EditText etQuestionType,
            EditText etTotalMarks,
            EditText etExamDuration,
            EditText etMath,
            EditText etPhysics,
            EditText etChemistry,
            EditText etEnglish,
            EditText etDiploma,
            EditText etUrl
    ) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("title", text(etTitle));
        data.put("session", text(etSession));
        data.put("questionType", text(etQuestionType));
        data.put("totalMarks", text(etTotalMarks));
        data.put("examDuration", text(etExamDuration));
        data.put("mathematics", text(etMath));
        data.put("physics", text(etPhysics));
        data.put("chemistry", text(etChemistry));
        data.put("english", text(etEnglish));
        data.put("diplomaTechnology", text(etDiploma));
        data.put("officialUrl", text(etUrl));
        data.put("officialPdfUrl", text(etUrl));
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedBy", user.getUid());

        if (currentDocument == null || !currentDocument.exists()) {
            data.put("published", false);
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("createdBy", user.getUid());
        }

        db.collection("syllabus")
                .document("current")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    try {
                        AdminActivityLogger.log(
                                "SYLLABUS_SAVED",
                                "current",
                                "Saved admission syllabus draft."
                        );
                    } catch (Exception ignored) { }
                    dialog.dismiss();
                    Toast.makeText(this, "Syllabus saved as draft.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Save failed: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void chooseSyllabusPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_SYLLABUS_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SYLLABUS_PDF && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                getContentResolver().takePersistableUriPermission(
                        data.getData(),
                        data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) { }
            selectedSyllabusPdfUri = data.getData();
            Toast.makeText(this, "PDF selected. Open Edit Syllabus to upload/save it.", Toast.LENGTH_LONG).show();
        }
    }

    private Uri selectedSyllabusPdfUri;

    private void togglePublish() {
        if (currentDocument == null || !currentDocument.exists()) {
            Toast.makeText(
                    this,
                    "Create and save the syllabus first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        boolean publish = !Boolean.TRUE.equals(
                currentDocument.getBoolean("published")
        );

        Map<String, Object> data = new HashMap<>();
        data.put("published", publish);
        data.put("updatedAt", FieldValue.serverTimestamp());

        if (publish) {
            data.put("publishedAt", FieldValue.serverTimestamp());
        } else {
            data.put("unpublishedAt", FieldValue.serverTimestamp());
        }

        db.collection("syllabus")
                .document("current")
                .update(data)
                .addOnSuccessListener(unused -> {
                    try {
                        AdminActivityLogger.log(
                                publish ? "SYLLABUS_PUBLISHED" : "SYLLABUS_UNPUBLISHED",
                                "current",
                                publish
                                        ? "Published admission syllabus to students."
                                        : "Unpublished admission syllabus."
                        );
                    } catch (Exception ignored) {
                    }

                    Toast.makeText(
                            this,
                            publish
                                    ? "Syllabus published to students."
                                    : "Syllabus unpublished.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Update failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void confirmDelete() {
        if (currentDocument == null || !currentDocument.exists()) {
            Toast.makeText(this, "No syllabus to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Syllabus?")
                .setMessage("This will permanently remove the current syllabus.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) ->
                        db.collection("syllabus")
                                .document("current")
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    try {
                                        AdminActivityLogger.log(
                                                "SYLLABUS_DELETED",
                                                "current",
                                                "Deleted admission syllabus."
                                        );
                                    } catch (Exception ignored) {
                                    }

                                    Toast.makeText(
                                            this,
                                            "Syllabus deleted.",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                this,
                                                "Delete failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show()
                                )
                )
                .show();
    }

    private void copyValue(EditText target, String key, String fallback) {
        target.setText(
                currentDocument == null
                        ? fallback
                        : getValue(currentDocument, key, fallback)
        );
    }

    private static String getValue(DocumentSnapshot doc, String key, String fallback) {
        Object value = doc.get(key);
        if (value == null) return fallback;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? fallback : s;
    }

    private static void setPreview(TextView view, String value) {
        if (view != null) view.setText(value);
    }

    private static String text(EditText editText) {
        return editText.getText().toString().trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    protected void onDestroy() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
        pdfExecutor.shutdownNow();
        super.onDestroy();
    }
}
