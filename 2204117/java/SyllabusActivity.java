package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyllabusActivity extends AppCompatActivity {

    private static final String DEFAULT_OFFICIAL_SYLLABUS_URL =
            "https://www.duet.ac.bd/storage/notice/2026/Apr/2026-04-19_1776578396_243.pdf";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration syllabusListener;
    private String officialSyllabusUrl = "";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private BottomNavigationView bottomNavigation;

    private TextView btnViewOfficialSyllabus;

    private TextView tvSyllabusTitle, tvSyllabusSession;
    private TextView tvQuestionType, tvTotalMarks, tvExamDuration;
    private TextView tvMathTopics, tvPhysicsTopics, tvChemistryTopics,
            tvEnglishTopics, tvDiplomaTopics;

    private TextView btnMathMore, btnPhysicsMore, btnChemistryMore,
            btnEnglishMore, btnDiplomaMore;

    // In-app PDF viewer
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pdfFileDescriptor;
    private File currentPdfFile;
    private final ExecutorService pdfExecutor =
            Executors.newSingleThreadExecutor();

    private LinearLayout pdfPagesContainer;
    private ProgressBar pdfProgress;
    private TextView pdfZoomLabel;

    private float pdfZoomScale = 1.0f;
    private AlertDialog pdfDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_syllabus);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();

        setupMenu();
        setupProfile();
        setupBottomNavigation();
        setupDrawer();
        setupOfficialSyllabusButton();
        setupSeeMoreButtons();
        loadPublishedSyllabus();
    }

    private void initViews() {

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        btnViewOfficialSyllabus =
                findViewById(R.id.btnViewOfficialSyllabus);

        tvSyllabusTitle = findViewById(R.id.tvSyllabusTitle);
        tvSyllabusSession = findViewById(R.id.tvSyllabusSession);
        tvQuestionType = findViewById(R.id.tvQuestionType);
        tvTotalMarks = findViewById(R.id.tvTotalMarks);
        tvExamDuration = findViewById(R.id.tvExamDuration);
        tvMathTopics = findViewById(R.id.tvMathTopics);
        tvPhysicsTopics = findViewById(R.id.tvPhysicsTopics);
        tvChemistryTopics = findViewById(R.id.tvChemistryTopics);
        tvEnglishTopics = findViewById(R.id.tvEnglishTopics);
        tvDiplomaTopics = findViewById(R.id.tvDiplomaTopics);

        btnMathMore = findViewById(R.id.btnMathMore);
        btnPhysicsMore = findViewById(R.id.btnPhysicsMore);
        btnChemistryMore = findViewById(R.id.btnChemistryMore);
        btnEnglishMore = findViewById(R.id.btnEnglishMore);
        btnDiplomaMore = findViewById(R.id.btnDiplomaMore);
    }

    private void setupMenu() {

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null && navigationView != null) {
                    drawerLayout.openDrawer(navigationView);
                }
            });
        }
    }

    private void setupProfile() {

        if (btnProfile != null) {

            btnProfile.setOnClickListener(v -> {

                Intent intent = new Intent(
                        SyllabusActivity.this,
                        ProfileActivity.class
                );

                startActivity(intent);
            });
        }
    }

    private void setupBottomNavigation() {

        if (bottomNavigation == null) {
            return;
        }

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {

                openActivity(MainActivity.class);
                return true;

            } else if (id == R.id.nav_notice) {

                openActivity(NoticeActivity.class);
                return true;

            } else if (id == R.id.nav_profile) {

                openActivity(ProfileActivity.class);
                return true;
            }

            return false;
        });

        int syllabusId =
                getResources().getIdentifier(
                        "nav_syllabus",
                        "id",
                        getPackageName()
                );

        if (syllabusId != 0) {
            bottomNavigation.setSelectedItemId(syllabusId);
        }
    }

    private void setupDrawer() {

        if (navigationView == null) {
            return;
        }

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {

                openActivity(MainActivity.class);

            } else if (id == R.id.nav_eligibility) {

                openActivity(EligibilityActivity.class);

            } else if (id == R.id.nav_notice) {

                openActivity(NoticeActivity.class);

            } else if (id == R.id.nav_department) {

                openActivity(DepartmentsActivity.class);

            } else if (id == R.id.nav_apply) {

                openActivity(AdmissionApplicationActivity.class);

            } else if (id == R.id.nav_tracker) {

                openActivity(ApplicationTrackerActivity.class);

            } else if (id == R.id.nav_admit_card) {

                openActivity(AdmitCardActivity.class);

            } else if (id == R.id.nav_result) {

                openActivity(ResultActivity.class);

            } else if (id == R.id.nav_syllabus) {

                closeDrawer();
            }

            return true;
        });
    }

    private void setupOfficialSyllabusButton() {

        if (btnViewOfficialSyllabus == null) {
            return;
        }

        btnViewOfficialSyllabus.setOnClickListener(v -> {
            if (TextUtils.isEmpty(officialSyllabusUrl)) {
                Toast.makeText(this, "Official syllabus link is not published yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            openOfficialSyllabusPdf();
        });
    }

    private void loadPublishedSyllabus() {
        if (db == null) return;
        if (syllabusListener != null) syllabusListener.remove();

        syllabusListener = db.collection("syllabus")
                .document("current")
                .addSnapshotListener((document, error) -> {
                    if (error != null || document == null || !document.exists()) {
                        bindFallbackSyllabus();
                        return;
                    }

                    if (!Boolean.TRUE.equals(document.getBoolean("published"))) {
                        bindFallbackSyllabus();
                        return;
                    }

                    setText(tvSyllabusTitle, valueOr(document, "title", "Admission Test Syllabus"));
                    setText(tvSyllabusSession, valueOr(document, "session", "Academic Session 2026"));
                    setText(tvQuestionType, valueOr(document, "questionType", "Multiple Choice Questions"));
                    setText(tvTotalMarks, valueOr(document, "totalMarks", "As per official admission circular"));
                    setText(tvExamDuration, valueOr(document, "examDuration", "As per official admission circular"));
                    setSubjectText(tvMathTopics, valueOr(document, "mathematics", "Topics will be updated from the published syllabus."), btnMathMore);
                    setSubjectText(tvPhysicsTopics, valueOr(document, "physics", "Topics will be updated from the published syllabus."), btnPhysicsMore);
                    setSubjectText(tvChemistryTopics, valueOr(document, "chemistry", "Topics will be updated from the published syllabus."), btnChemistryMore);
                    setSubjectText(tvEnglishTopics, valueOr(document, "english", "Topics will be updated from the published syllabus."), btnEnglishMore);
                    setSubjectText(tvDiplomaTopics, valueOr(document, "diplomaTechnology", "Relevant diploma engineering technology subjects."), btnDiplomaMore);

                    officialSyllabusUrl = valueOr(document, "officialUrl", DEFAULT_OFFICIAL_SYLLABUS_URL);
                });
    }

    private void bindFallbackSyllabus() {
        setText(tvSyllabusTitle, "Admission Test Syllabus");
        setText(tvSyllabusSession, "Academic Session 2026");
        setText(tvQuestionType, "Multiple Choice Questions");
        setText(tvTotalMarks, "As per official admission circular");
        setText(tvExamDuration, "As per official admission circular");
        setSubjectText(tvMathTopics, "Topics will be updated from the published syllabus.", btnMathMore);
        setSubjectText(tvPhysicsTopics, "Topics will be updated from the published syllabus.", btnPhysicsMore);
        setSubjectText(tvChemistryTopics, "Topics will be updated from the published syllabus.", btnChemistryMore);
        setSubjectText(tvEnglishTopics, "Topics will be updated from the published syllabus.", btnEnglishMore);
        setSubjectText(tvDiplomaTopics, "Questions may be based on relevant diploma engineering technology subjects.", btnDiplomaMore);
        officialSyllabusUrl = DEFAULT_OFFICIAL_SYLLABUS_URL;
    }

    private void setupSeeMoreButtons() {
        bindSeeMore(tvMathTopics, btnMathMore);
        bindSeeMore(tvPhysicsTopics, btnPhysicsMore);
        bindSeeMore(tvChemistryTopics, btnChemistryMore);
        bindSeeMore(tvEnglishTopics, btnEnglishMore);
        bindSeeMore(tvDiplomaTopics, btnDiplomaMore);
    }

    private void setSubjectText(TextView textView, String value, TextView moreButton) {
        if (textView == null) return;
        textView.setText(value);
        boolean longText = value != null && value.length() > 260;
        textView.setMaxLines(longText ? 5 : Integer.MAX_VALUE);
        textView.setEllipsize(longText ? android.text.TextUtils.TruncateAt.END : null);
        if (moreButton != null) {
            moreButton.setVisibility(longText ? View.VISIBLE : View.GONE);
            moreButton.setText("See more");
            moreButton.setTag(Boolean.FALSE);
        }
    }

    private void bindSeeMore(TextView content, TextView button) {
        if (content == null || button == null) return;
        button.setOnClickListener(v -> {
            boolean expanded = Boolean.TRUE.equals(button.getTag());
            if (expanded) {
                content.setMaxLines(5);
                content.setEllipsize(android.text.TextUtils.TruncateAt.END);
                button.setText("See more");
                button.setTag(Boolean.FALSE);
            } else {
                content.setMaxLines(Integer.MAX_VALUE);
                content.setEllipsize(null);
                button.setText("See less");
                button.setTag(Boolean.TRUE);
            }
        });
    }

    private static void setText(TextView view, String value) {
        if (view != null) view.setText(value);
    }

    private static String valueOr(DocumentSnapshot doc, String key, String fallback) {
        Object value = doc.get(key);
        if (value == null) return fallback;
        String result = String.valueOf(value).trim();
        return result.isEmpty() ? fallback : result;
    }

    // =========================================================
    // IN-APP PDF VIEWER
    // =========================================================

    private void openOfficialSyllabusPdf() {

        if (pdfDialog != null && pdfDialog.isShowing()) {
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // ---------------- PROFESSIONAL TOP BAR ----------------

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(14), dp(8), dp(10), dp(8));
        topBar.setBackgroundColor(Color.WHITE);
        topBar.setElevation(dp(5));

        // Small document accent icon
        ImageView documentIcon = new ImageView(this);
        documentIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        documentIcon.setColorFilter(Color.parseColor("#155EEF"));

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(42), dp(42));
        iconParams.setMargins(0, 0, dp(10), 0);
        documentIcon.setPadding(dp(9), dp(9), dp(9), dp(9));
        topBar.addView(documentIcon, iconParams);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Official DUET Syllabus");
        title.setTextColor(Color.parseColor("#173F6B"));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText("Official PDF • Smart DUET");
        subtitle.setTextColor(Color.parseColor("#667085"));
        subtitle.setTextSize(10.5f);
        subtitle.setSingleLine(true);

        titleBox.addView(title);
        titleBox.addView(subtitle);

        LinearLayout.LayoutParams titleBoxParams =
                new LinearLayout.LayoutParams(0, dp(48), 1f);
        topBar.addView(titleBox, titleBoxParams);

        pdfZoomLabel = createToolbarButton("100%");
        topBar.addView(pdfZoomLabel);

        TextView zoomOut = createToolbarButton("−");
        topBar.addView(zoomOut);

        TextView zoomIn = createToolbarButton("+");
        topBar.addView(zoomIn);

        // Professional download icon button
        ImageButton download = new ImageButton(this);
        download.setImageResource(android.R.drawable.stat_sys_download);
        download.setColorFilter(Color.parseColor("#155EEF"));
        download.setContentDescription("Download PDF");
        download.setBackground(createToolbarBackground(
                Color.parseColor("#EEF5FF")
        ));
        download.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout.LayoutParams downloadParams =
                new LinearLayout.LayoutParams(dp(44), dp(44));
        downloadParams.setMargins(dp(3), 0, dp(3), 0);
        topBar.addView(download, downloadParams);

        TextView close = createToolbarButton("×");
        topBar.addView(close);

        root.addView(topBar);

        // ---------------- DIVIDER ----------------

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#E5E7EB"));
        root.addView(
                divider,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                )
        );

        // ---------------- CONTENT ----------------

        LinearLayout viewerArea = new LinearLayout(this);
        viewerArea.setOrientation(LinearLayout.VERTICAL);
        viewerArea.setBackgroundColor(Color.parseColor("#EEF2F7"));

        pdfProgress = new ProgressBar(this);
        pdfProgress.setVisibility(View.VISIBLE);

        LinearLayout progressWrap = new LinearLayout(this);
        progressWrap.setGravity(Gravity.CENTER);
        progressWrap.addView(pdfProgress);

        viewerArea.addView(
                progressWrap,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(70)
                )
        );

        ScrollView verticalScroll =
                new ScrollView(this);

        HorizontalScrollViewCompat horizontalScroll =
                new HorizontalScrollViewCompat(this);

        pdfPagesContainer =
                new LinearLayout(this);
        pdfPagesContainer.setOrientation(
                LinearLayout.VERTICAL
        );
        pdfPagesContainer.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(24)
        );

        horizontalScroll.addView(pdfPagesContainer);
        verticalScroll.addView(horizontalScroll);

        viewerArea.addView(
                verticalScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        root.addView(
                viewerArea,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        // ---------------- DIALOG ----------------

        pdfDialog = new AlertDialog.Builder(this)
                .setView(root)
                .create();

        close.setOnClickListener(v -> pdfDialog.dismiss());

        zoomOut.setOnClickListener(v -> {
            setPdfZoom(pdfZoomScale - 0.25f);
            rerenderPdfPages();
        });

        zoomIn.setOnClickListener(v -> {
            setPdfZoom(pdfZoomScale + 0.25f);
            rerenderPdfPages();
        });

        pdfZoomLabel.setOnClickListener(v -> {
            setPdfZoom(1.0f);
            rerenderPdfPages();
        });

        download.setOnClickListener(v -> downloadOfficialPdf());

        pdfDialog.setOnDismissListener(dialog ->
                closePdfRenderer()
        );

        pdfDialog.show();

        Window window = pdfDialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );

            int width =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels
                                    * 0.97f
                    );

            int height =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .heightPixels
                                    * 0.92f
                    );

            window.setLayout(width, height);
        }

        downloadAndRenderPdf();
    }

    private void downloadOfficialPdf() {

        try {

            DownloadManager manager =
                    (DownloadManager)
                            getSystemService(
                                    Context.DOWNLOAD_SERVICE
                            );

            if (manager == null) {

                Toast.makeText(
                        this,
                        "Download service is not available.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            DownloadManager.Request request =
                    new DownloadManager.Request(
                            Uri.parse(
                                    officialSyllabusUrl
                            )
                    );

            request.setTitle(
                    "DUET Official Syllabus 2026"
            );

            request.setDescription(
                    "Downloading official DUET syllabus PDF"
            );

            request.setMimeType(
                    "application/pdf"
            );

            request.setNotificationVisibility(
                    DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            manager.enqueue(request);

            Toast.makeText(
                    this,
                    "Official syllabus download started.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to start PDF download.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private void setPdfZoom(float scale) {
        pdfZoomScale = Math.max(0.5f, Math.min(2.0f, scale));
        if (pdfZoomLabel != null) {
            pdfZoomLabel.setText(((int) (pdfZoomScale * 100)) + "%");
        }
    }

    private void rerenderPdfPages() {
        runOnUiThread(() -> renderPdfPages());
    }

    private void renderPdfPages() {
        if (pdfRenderer == null || pdfPagesContainer == null) return;

        pdfPagesContainer.removeAllViews();

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int baseWidth = Math.max(dp(280), screenWidth - dp(56));

        for (int pageIndex = 0; pageIndex < pdfRenderer.getPageCount(); pageIndex++) {
            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);
            try {
                float ratio = (float) page.getHeight() / (float) page.getWidth();
                int width = Math.max(dp(280), (int) (baseWidth * pdfZoomScale));
                int height = (int) (width * ratio);

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setBackgroundColor(Color.WHITE);
                imageView.setPadding(dp(2), dp(2), dp(2), dp(12));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.gravity = Gravity.CENTER_HORIZONTAL;
                pdfPagesContainer.addView(imageView, params);
            } finally {
                page.close();
            }
        }
    }

    private void renderPdf() {
        if (currentPdfFile == null) return;
        try {
            closePdfRenderer();
            pdfFileDescriptor = ParcelFileDescriptor.open(
                    currentPdfFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
            );
            pdfRenderer = new PdfRenderer(pdfFileDescriptor);
            setPdfZoom(1.0f);
            renderPdfPages();
        } catch (Exception e) {
            Toast.makeText(this, "PDF could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    private File downloadPdf(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.connect();

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            connection.disconnect();
            throw new Exception("HTTP " + code);
        }

        File file = new File(getCacheDir(), "duet_official_syllabus_2026.pdf");
        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        } finally {
            connection.disconnect();
        }
        return file;
    }

    private void downloadAndRenderPdf() {
        if (TextUtils.isEmpty(officialSyllabusUrl)) {
            Toast.makeText(this, "Official syllabus link is not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        pdfExecutor.execute(() -> {
            try {
                File file = downloadPdf(officialSyllabusUrl);
                currentPdfFile = file;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    renderPdf();
                    if (pdfProgress != null) pdfProgress.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (pdfProgress != null) pdfProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Unable to load official syllabus PDF.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void closePdfRenderer() {
        try {
            if (pdfRenderer != null) pdfRenderer.close();
        } catch (Exception ignored) { }
        pdfRenderer = null;

        try {
            if (pdfFileDescriptor != null) pdfFileDescriptor.close();
        } catch (Exception ignored) { }
        pdfFileDescriptor = null;
    }

    private TextView createToolbarButton(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.parseColor("#173F6B"));
        view.setTextSize(text.equals("×") ? 24 : 15);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setBackground(createToolbarBackground(Color.parseColor("#F7F9FC")));
        view.setPadding(dp(6), 0, dp(6), 0);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(dp(44), dp(44));

        params.setMargins(dp(3), 0, dp(3), 0);
        view.setLayoutParams(params);

        if (text.equals("100%")) {
            view.setTextSize(11);
        }

        return view;
    }

    private android.graphics.drawable.GradientDrawable createToolbarBackground(
            int color
    ) {

        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(
                dp(1),
                Color.parseColor("#E0E7F0")
        );

        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class HorizontalScrollViewCompat
            extends android.widget.HorizontalScrollView {

        public HorizontalScrollViewCompat(
                Context context
        ) {
            super(context);

            setFillViewport(true);
            setHorizontalScrollBarEnabled(false);
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private void openActivity(
            Class<?> activityClass
    ) {

        closeDrawer();

        Intent intent =
                new Intent(
                        SyllabusActivity.this,
                        activityClass
                );

        startActivity(intent);
    }

    private void closeDrawer() {

        if (drawerLayout != null &&
                navigationView != null &&
                drawerLayout.isDrawerOpen(
                        navigationView
                )) {

            drawerLayout.closeDrawer(
                    navigationView
            );
        }
    }

    @Override
    public void onBackPressed() {

        if (pdfDialog != null &&
                pdfDialog.isShowing()) {

            pdfDialog.dismiss();
            return;
        }

        if (drawerLayout != null &&
                navigationView != null &&
                drawerLayout.isDrawerOpen(
                        navigationView
                )) {

            drawerLayout.closeDrawer(
                    navigationView
            );

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (syllabusListener != null) {
            syllabusListener.remove();
            syllabusListener = null;
        }
        closePdfRenderer();

        pdfExecutor.shutdownNow();

        super.onDestroy();
    }
}
