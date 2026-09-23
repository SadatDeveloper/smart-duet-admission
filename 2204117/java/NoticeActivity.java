package com.example.smartduetadmissionsystem;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoticeActivity extends AppCompatActivity {

    // ORIGINAL NOTICE UI — Card 1 is kept exactly as the existing static/PDF card.
    private TextView tvBackNotice;
    private EditText etSearchNotice;

    private MaterialCardView cardNotice1;
    private TextView btnNotice1View;

    // Dynamic notices are rendered in the RecyclerView already present in activity_notice.xml.
    private RecyclerView recyclerNotices;
    private ProgressBar progressNotices;
    private TextView tvEmptyNotices;
    private TextView tvNoticeCount;

    // Existing fixed header / drawer / footer.
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private BottomNavigationView bottomNavigation;

    // Existing in-place PDF viewer — NOT changed.
    private ScrollView noticeScrollView;
    private FrameLayout pdfViewerContainer;
    private LinearLayout pdfPagesContainer;
    private ProgressBar pdfProgress;
    private TextView pdfError;

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pdfFileDescriptor;
    private File currentPdfFile;
    private final ExecutorService pdfExecutor = Executors.newSingleThreadExecutor();

    private float pdfZoomScale = 1.0f;
    private ScaleGestureDetector pdfScaleDetector;
    private TextView pdfZoomLabel;
    private LinearLayout pdfZoomControls;

    // NEW: only Cards 2–5 are dynamic.
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration noticeListener;
    private final List<NoticeModel> dynamicNotices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupFixedHeader();
        setupNavigationDrawer();
        setupBottomNavigation();
        setupBackButton();
        setupSearch();
        setupRecyclerView();

        // Card 1 PDF setup remains unchanged.
        setupStaticCard1Pdf();

        // Cards 2–5 now come from Firestore.
        listenToPublishedNotices();
    }

    private void initViews() {
        tvBackNotice = findViewById(R.id.tvBackNotice);
        etSearchNotice = findViewById(R.id.etSearchNotice);

        cardNotice1 = findViewById(R.id.cardNotice1);
        btnNotice1View = findViewById(R.id.btnNotice1View);

        recyclerNotices = findViewById(R.id.recyclerNotices);
        progressNotices = findViewById(R.id.progressNotices);
        tvEmptyNotices = findViewById(R.id.tvEmptyNotices);
        tvNoticeCount = findViewById(R.id.tvNoticeCount);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        noticeScrollView = findViewById(R.id.noticeScrollView);
        pdfViewerContainer = findViewById(R.id.pdfViewerContainer);
        pdfPagesContainer = findViewById(R.id.pdfPagesContainer);
        pdfProgress = findViewById(R.id.pdfProgress);
        pdfError = findViewById(R.id.pdfError);

        setupPdfZoom();
    }

    // ============================================================
    // CARD 1 — ORIGINAL PDF BEHAVIOR PRESERVED
    // ============================================================

    private void setupStaticCard1Pdf() {
        setPdfLink(
                btnNotice1View,
                "https://duet.ac.bd/public/storage/notice/2026/May/2026-05-11_1778488947_230.pdf"
        );
    }

    private void setPdfLink(TextView button, String url) {
        if (button == null) return;
        button.setOnClickListener(v -> openPdfInsideNotice(url));
    }

    private void openPdfInsideNotice(String url) {
        if (pdfViewerContainer == null) {
            Toast.makeText(this, "PDF viewer is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        pdfViewerContainer.setVisibility(View.VISIBLE);
        if (noticeScrollView != null) noticeScrollView.setVisibility(View.GONE);
        if (pdfPagesContainer != null) pdfPagesContainer.removeAllViews();
        setPdfZoom(1.0f);

        if (pdfError != null) {
            pdfError.setText("");
            pdfError.setVisibility(View.GONE);
        }
        if (pdfProgress != null) pdfProgress.setVisibility(View.VISIBLE);

        pdfExecutor.execute(() -> {
            try {
                File file = downloadPdf(url);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) renderPdf(file);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showPdfError(
                        "Unable to load this PDF. Please check your internet connection and try again."
                ));
            }
        });
    }

    private File downloadPdf(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            connection.disconnect();
            throw new Exception("HTTP " + responseCode);
        }

        File file = new File(getCacheDir(), "duet_admission_notice_2026.pdf");

        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(file)) {

            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();

        } finally {
            connection.disconnect();
        }

        return file;
    }

    private void renderPdf(File file) {
        try {
            closePdfRenderer();
            currentPdfFile = file;

            pdfFileDescriptor = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
            );
            pdfRenderer = new PdfRenderer(pdfFileDescriptor);

            if (pdfPagesContainer != null) pdfPagesContainer.removeAllViews();

            int pageCount = pdfRenderer.getPageCount();
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int renderWidth = Math.min(screenWidth, 1080);

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);

                float scale = renderWidth / (float) page.getWidth();
                int width = renderWidth;
                int height = Math.round(page.getHeight() * scale);

                Bitmap bitmap = Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                );
                bitmap.eraseColor(0xFFFFFFFF);

                page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                );
                page.close();

                ImageView pageImage = new ImageView(this);
                pageImage.setImageBitmap(bitmap);
                pageImage.setAdjustViewBounds(true);
                pageImage.setScaleType(ImageView.ScaleType.FIT_XY);
                pageImage.setBackgroundColor(0xFFFFFFFF);
                pageImage.setTag(new int[]{width, height});

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(width, height);
                params.gravity = Gravity.CENTER_HORIZONTAL;
                params.bottomMargin = 8;

                if (pdfPagesContainer != null) {
                    pdfPagesContainer.addView(pageImage, params);
                }
            }

            setPdfZoom(1.0f);
            if (pdfProgress != null) pdfProgress.setVisibility(View.GONE);

        } catch (Exception e) {
            showPdfError("Unable to display this PDF.");
        }
    }

    private void showPdfError(String message) {
        if (pdfProgress != null) pdfProgress.setVisibility(View.GONE);
        if (pdfError != null) {
            pdfError.setText(message);
            pdfError.setVisibility(View.VISIBLE);
        }
    }

    private void closePdfRenderer() {
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
        } catch (Exception ignored) {}

        try {
            if (pdfFileDescriptor != null) {
                pdfFileDescriptor.close();
                pdfFileDescriptor = null;
            }
        } catch (Exception ignored) {}
    }

    // ============================================================
    // PDF ZOOM — EXISTING BEHAVIOR
    // ============================================================

    private void setupPdfZoom() {
        if (pdfViewerContainer == null) return;

        pdfScaleDetector = new ScaleGestureDetector(
                this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float factor = detector.getScaleFactor();
                        if (factor > 1.0f) {
                            setPdfZoom(pdfZoomScale + 0.03f);
                        } else if (factor < 1.0f) {
                            setPdfZoom(pdfZoomScale - 0.03f);
                        }
                        return true;
                    }
                }
        );

        pdfViewerContainer.setOnTouchListener((v, event) -> {
            if (pdfScaleDetector != null) {
                pdfScaleDetector.onTouchEvent(event);
            }
            return false;
        });

        pdfZoomControls = new LinearLayout(this);
        pdfZoomControls.setOrientation(LinearLayout.HORIZONTAL);
        pdfZoomControls.setGravity(Gravity.CENTER_VERTICAL);
        pdfZoomControls.setPadding(6, 4, 6, 4);
        pdfZoomControls.setBackgroundColor(0xEEFFFFFF);
        pdfZoomControls.setElevation(8f);

        TextView minus = createZoomButton("−");
        pdfZoomLabel = createZoomButton("100%");
        TextView plus = createZoomButton("+");
        ImageButton download = createDownloadButton();

        minus.setOnClickListener(v -> setPdfZoom(pdfZoomScale - 0.25f));
        plus.setOnClickListener(v -> setPdfZoom(pdfZoomScale + 0.25f));
        pdfZoomLabel.setOnClickListener(v -> setPdfZoom(1.0f));
        download.setOnClickListener(v -> downloadCurrentPdf());

        pdfZoomControls.addView(minus);
        pdfZoomControls.addView(pdfZoomLabel);
        pdfZoomControls.addView(plus);
        pdfZoomControls.addView(download);

        FrameLayout.LayoutParams controlParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.END | Gravity.TOP
                );
        controlParams.setMargins(0, 14, 14, 0);

        pdfViewerContainer.addView(pdfZoomControls, controlParams);
    }

    private ImageButton createDownloadButton() {
        ImageButton button = new ImageButton(this);
        button.setImageResource(android.R.drawable.stat_sys_download);
        button.setColorFilter(0xFF173F6B);
        button.setContentDescription("Download PDF");
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setPadding(10, 10, 10, 10);
        button.setBackgroundResource(android.R.drawable.btn_default);
        button.setClickable(true);
        button.setFocusable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(46, 42);
        button.setLayoutParams(params);
        return button;
    }

    private TextView createZoomButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(0xFF173F6B);
        button.setTextSize(16);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setMinWidth(46);
        button.setMinHeight(42);
        button.setPadding(10, 0, 10, 0);
        button.setBackgroundResource(android.R.drawable.btn_default);
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private void setPdfZoom(float scale) {
        pdfZoomScale = Math.max(0.75f, Math.min(scale, 2.5f));

        if (pdfZoomLabel != null) {
            pdfZoomLabel.setText(Math.round(pdfZoomScale * 100) + "%");
        }

        if (pdfPagesContainer == null) return;

        for (int i = 0; i < pdfPagesContainer.getChildCount(); i++) {
            View child = pdfPagesContainer.getChildAt(i);
            if (!(child instanceof ImageView)) continue;

            Object tag = child.getTag();
            if (!(tag instanceof int[])) continue;

            int[] baseSize = (int[]) tag;
            int newWidth = Math.max(1, Math.round(baseSize[0] * pdfZoomScale));
            int newHeight = Math.max(1, Math.round(baseSize[1] * pdfZoomScale));

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) child.getLayoutParams();
            params.width = newWidth;
            params.height = newHeight;
            params.gravity = Gravity.CENTER_HORIZONTAL;
            child.setLayoutParams(params);
        }
    }

    private void downloadCurrentPdf() {
        final String pdfUrl =
                "https://duet.ac.bd/public/storage/notice/2026/May/2026-05-11_1778488947_230.pdf";

        try {
            DownloadManager downloadManager =
                    (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            if (downloadManager == null) {
                Toast.makeText(this, "Download service is not available", Toast.LENGTH_SHORT).show();
                return;
            }

            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(pdfUrl));

            request.setTitle("DUET Admission Circular 2026");
            request.setDescription("Downloading DUET admission notice PDF");
            request.setMimeType("application/pdf");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    "DUET_Admission_Circular_2026.pdf"
            );

            downloadManager.enqueue(request);
            Toast.makeText(this, "PDF download started", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Unable to start PDF download", Toast.LENGTH_SHORT).show();
        }
    }

    // ============================================================
    // FIRESTORE — DYNAMIC NOTICES AFTER STATIC CARD 1
    // ============================================================

    private void setupRecyclerView() {
        if (recyclerNotices == null) return;

        recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotices.setHasFixedSize(false);
        recyclerNotices.setNestedScrollingEnabled(false);
        recyclerNotices.setAdapter(new NoticeAdapter(dynamicNotices, this::showDynamicNoticeDetails));
    }

    private void listenToPublishedNotices() {
        if (noticeListener != null) noticeListener.remove();

        if (auth.getCurrentUser() == null) {
            showEmpty("Please log in to view notices.");
            return;
        }

        if (progressNotices != null) progressNotices.setVisibility(View.VISIBLE);

        // No orderBy: legacy notice documents without createdAt remain compatible.
        noticeListener = db.collection("notices")
                .whereEqualTo("published", true)
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE,
                        (snapshots, error) -> {

                            if (error != null) {
                                showEmpty("Unable to load dynamic notices.");
                                Toast.makeText(
                                        this,
                                        "Notice loading failed",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            dynamicNotices.clear();

                            if (snapshots != null) {
                                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                    NoticeModel notice = NoticeModel.from(doc);
                                    if (notice != null) {
                                        dynamicNotices.add(notice);
                                    }
                                }
                            }

                            Collections.sort(
                                    dynamicNotices,
                                    (a, b) -> Long.compare(b.sortTime, a.sortTime)
                            );

                            int totalStudentNotices = dynamicNotices.size() + 1; // + static Card 1
                            if (tvNoticeCount != null) {
                                tvNoticeCount.setText(
                                        totalStudentNotices +
                                                (totalStudentNotices == 1 ? " Notice" : " Notices")
                                );
                            }

                            applyNoticeSearch(
                                    etSearchNotice == null
                                            ? ""
                                            : etSearchNotice.getText().toString()
                            );

                            if (progressNotices != null) {
                                progressNotices.setVisibility(View.GONE);
                            }
                        });
    }

    private void applyNoticeSearch(String searchText) {
        String q = searchText == null
                ? ""
                : searchText.trim().toLowerCase(Locale.ROOT);

        // Card 1 remains static and its PDF behavior is never replaced.
        if (cardNotice1 != null) {
            cardNotice1.setVisibility(View.VISIBLE);
        }

        List<NoticeModel> filtered = new ArrayList<>();

        for (NoticeModel notice : dynamicNotices) {
            if (q.isEmpty() || notice.searchText.contains(q)) {
                filtered.add(notice);
            }
        }

        if (recyclerNotices != null && recyclerNotices.getAdapter() instanceof NoticeAdapter) {
            NoticeAdapter adapter = (NoticeAdapter) recyclerNotices.getAdapter();
            adapter.replaceItems(filtered);
        }

        if (filtered.isEmpty()) {
            showEmpty(
                    q.isEmpty()
                            ? "No additional published notices available."
                            : "No dynamic notices matched your search."
            );
        } else {
            if (tvEmptyNotices != null) {
                tvEmptyNotices.setVisibility(View.GONE);
            }
            if (recyclerNotices != null) {
                recyclerNotices.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showEmpty(String message) {
        if (progressNotices != null) progressNotices.setVisibility(View.GONE);

        if (tvEmptyNotices != null) {
            tvEmptyNotices.setText(message);
            tvEmptyNotices.setVisibility(View.VISIBLE);
        }

        if (recyclerNotices != null) {
            recyclerNotices.setVisibility(View.GONE);
        }
    }

    private void showDynamicNoticeDetails(NoticeModel notice) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(notice.title)
                .setMessage(
                        "Category: " + notice.category +
                                "\nPriority: " + notice.priority +
                                "\nPublished: " + notice.formattedDate +
                                "\n\n" +
                                (TextUtils.isEmpty(notice.description)
                                        ? "No description available."
                                        : notice.description)
                )
                .setPositiveButton("Close", null)
                .show();
    }

    private static class NoticeAdapter
            extends RecyclerView.Adapter<NoticeAdapter.Holder> {

        interface OnNoticeClick {
            void onClick(NoticeModel notice);
        }

        private final List<NoticeModel> items = new ArrayList<>();
        private final OnNoticeClick listener;

        NoticeAdapter(List<NoticeModel> initialItems, OnNoticeClick listener) {
            if (initialItems != null) items.addAll(initialItems);
            this.listener = listener;
        }

        void replaceItems(List<NoticeModel> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_notice, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            NoticeModel n = items.get(position);

            holder.title.setText(n.title);
            holder.description.setText(
                    TextUtils.isEmpty(n.description)
                            ? "No description available."
                            : n.description
            );
            holder.category.setText(n.category);
            holder.priority.setText(n.priority);
            holder.date.setText(n.formattedDate);

            holder.card.setOnClickListener(v -> listener.onClick(n));
            holder.view.setOnClickListener(v -> listener.onClick(n));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView title, description, category, priority, date, view;

            Holder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.noticeCard);
                title = itemView.findViewById(R.id.tvStudentNoticeTitle);
                description = itemView.findViewById(R.id.tvStudentNoticeDescription);
                category = itemView.findViewById(R.id.tvStudentNoticeCategory);
                priority = itemView.findViewById(R.id.tvStudentNoticePriority);
                date = itemView.findViewById(R.id.tvStudentNoticeDate);
                view = itemView.findViewById(R.id.btnStudentNoticeView);
            }
        }
    }

    // ============================================================
    // HEADER / DRAWER / FOOTER — existing navigation
    // ============================================================

    private void setupFixedHeader() {
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                    startActivity(
                            new Intent(
                                    NoticeActivity.this,
                                    ProfileActivity.class
                            )
                    )
            );
        }
    }

    private void setupNavigationDrawer() {
        if (navigationView == null) return;

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                openActivity(MainActivity.class);
            } else if (id == R.id.nav_eligibility) {
                openActivity(EligibilityActivity.class);
            } else if (id == R.id.nav_notice) {
                closeDrawer();
            } else if (id == R.id.nav_department) {
                openActivity(DepartmentsActivity.class);
            } else if (id == R.id.nav_apply) {
                openActivity(AdmissionApplicationActivity.class);
            } else if (id == R.id.nav_tracker) {
                openActivity(ApplicationTrackerActivity.class);
            } else if (id == R.id.nav_syllabus) {
                openActivity(SyllabusActivity.class);
            } else {
                closeDrawer();
                return false;
            }

            return true;
        });
    }

    private void openActivity(Class<?> activityClass) {
        startActivity(new Intent(NoticeActivity.this, activityClass));
        closeDrawer();
    }

    private void closeDrawer() {
        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(
                        new Intent(
                                NoticeActivity.this,
                                MainActivity.class
                        )
                );
                return true;
            }

            if (id == R.id.nav_notice) return true;

            if (id == R.id.nav_profile) {
                startActivity(
                        new Intent(
                                NoticeActivity.this,
                                ProfileActivity.class
                        )
                );
                return true;
            }

            return false;
        });

        bottomNavigation.setSelectedItemId(R.id.nav_notice);
    }

    private void setupBackButton() {
        if (tvBackNotice != null) {
            tvBackNotice.setOnClickListener(v -> finish());
        }
    }

    // ============================================================
    // SEARCH — dynamic notices only; Card 1 remains unchanged
    // ============================================================
    private void setupSearch() {
        if (etSearchNotice == null) return;

        etSearchNotice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyNoticeSearch(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        });
    }

    // ============================================================
    // MODEL
    // ============================================================

    private static class NoticeModel {
        String id = "";
        String title = "";
        String description = "";
        String category = "General";
        String priority = "Normal";
        String formattedDate = "Recently published";
        String searchText = "";
        long sortTime = 0L;

        static NoticeModel from(DocumentSnapshot doc) {
            String title = value(doc, "title");
            if (TextUtils.isEmpty(title)) return null;

            NoticeModel n = new NoticeModel();
            n.id = doc.getId();
            n.title = title;
            n.description = value(doc, "description");
            n.category = valueOr(doc, "category", "General");
            n.priority = valueOr(doc, "priority", "Normal");
            n.searchText = (n.title + " " + n.description + " " + n.category + " " + n.priority)
                    .toLowerCase(Locale.ROOT);

            Timestamp publishedAt = doc.getTimestamp("publishedAt");
            Timestamp createdAt = doc.getTimestamp("createdAt");

            if (publishedAt != null) {
                DateHolder d = new DateHolder(publishedAt.toDate().getTime());
                n.sortTime = d.time;
                n.formattedDate = new java.text.SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.ENGLISH
                ).format(publishedAt.toDate());
            } else if (createdAt != null) {
                DateHolder d = new DateHolder(createdAt.toDate().getTime());
                n.sortTime = d.time;
                n.formattedDate = new java.text.SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.ENGLISH
                ).format(createdAt.toDate());
            }

            return n;
        }

        private static String value(DocumentSnapshot doc, String key) {
            Object value = doc.get(key);
            return value == null ? "" : String.valueOf(value).trim();
        }

        private static String valueOr(
                DocumentSnapshot doc,
                String key,
                String fallback
        ) {
            String value = value(doc, key);
            return value.isEmpty() ? fallback : value;
        }
    }

    private static class DateHolder {
        long time;
        DateHolder(long time) {
            this.time = time;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // ============================================================
    // BACK / DESTROY
    // ============================================================

    @Override
    public void onBackPressed() {
        if (pdfViewerContainer != null &&
                pdfViewerContainer.getVisibility() == View.VISIBLE) {

            closePdfRenderer();
            pdfViewerContainer.setVisibility(View.GONE);

            if (noticeScrollView != null) {
                noticeScrollView.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(GravityCompat.START)) {

            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (noticeListener != null) {
            noticeListener.remove();
            noticeListener = null;
        }

        closePdfRenderer();
        pdfExecutor.shutdownNow();

        super.onDestroy();
    }
}
