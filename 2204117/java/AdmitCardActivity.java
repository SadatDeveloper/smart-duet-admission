package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.graphics.pdf.PdfDocument;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

public class AdmitCardActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private BottomNavigationView bottomNavigation;
    private MaterialButton btnPrintAdmitCard;

    private ImageView ivDuetLogo;
    private ImageView ivApplicantPhoto;
    private ImageView ivApplicantSignature;
    private ImageView ivAuthorizedSignature;

    private View admitCardPaper;

    private android.widget.TextView tvApplicantName;
    private android.widget.TextView tvFatherName;
    private android.widget.TextView tvApplicationNo;
    private android.widget.TextView tvAdmitCardNo;
    private android.widget.TextView tvDepartment;
    private android.widget.TextView tvIdentityNumber;
    private android.widget.TextView tvExamDate;
    private android.widget.TextView tvExamTime;
    private android.widget.TextView tvExamCenter;
    private android.widget.TextView tvExamDepartment;
    private android.widget.TextView tvSeatNumber;
    private android.widget.TextView tvInstructions;

    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration applicationListener;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admit_card);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupHeader();
        setupDrawer();
        setupBottomNavigation();
        setupPrintButton();

        loadStudentAdmitCard();
    }

    // =========================================================
    // INIT VIEWS
    // =========================================================

    private void initViews() {

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnPrintAdmitCard = findViewById(R.id.btnPrintAdmitCard);

        admitCardPaper = findViewById(R.id.admitCardPaper);

        ivDuetLogo = findViewById(R.id.ivDuetLogo);
        if (ivDuetLogo != null) {
            ivDuetLogo.setImageResource(R.drawable.duet_logo);
        }

        ivApplicantPhoto = findViewById(R.id.ivApplicantPhoto);
        ivApplicantSignature = findViewById(R.id.ivApplicantSignature);
        ivAuthorizedSignature = findViewById(R.id.ivAuthorizedSignature);

        tvApplicantName = findViewById(R.id.tvApplicantName);
        tvFatherName = findViewById(R.id.tvFatherName);
        tvApplicationNo = findViewById(R.id.tvApplicationNo);
        tvAdmitCardNo = findViewById(R.id.tvAdmitCardNo);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvIdentityNumber = findViewById(R.id.tvIdentityNumber);

        tvExamDate = findViewById(R.id.tvExamDate);
        tvExamTime = findViewById(R.id.tvExamTime);
        tvExamCenter = findViewById(R.id.tvExamCenter);
        tvExamDepartment = findViewById(R.id.tvExamDepartment);
        tvSeatNumber = findViewById(R.id.tvSeatNumber);
        tvInstructions = findViewById(R.id.tvInstructions);
    }

    // =========================================================
    // HEADER
    // =========================================================

    private void setupHeader() {

        btnMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );

        btnProfile.setOnClickListener(v -> {

            startActivity(new Intent(
                    AdmitCardActivity.this,
                    ProfileActivity.class
            ));
        });
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {

        bottomNavigation.setOnItemSelectedListener(item -> {

            String title = item.getTitle() == null
                    ? ""
                    : item.getTitle().toString()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (title.contains("home")
                    || title.contains("dashboard")) {

                startActivity(new Intent(
                        AdmitCardActivity.this,
                        MainActivity.class
                ));

                return true;
            }

            if (title.contains("notice")) {

                startActivity(new Intent(
                        AdmitCardActivity.this,
                        NoticeActivity.class
                ));

                return true;
            }

            if (title.contains("profile")) {

                startActivity(new Intent(
                        AdmitCardActivity.this,
                        ProfileActivity.class
                ));

                return true;
            }

            return true;
        });
    }

    // =========================================================
    // DRAWER
    // =========================================================

    private void setupDrawer() {

        navigationView.setNavigationItemSelectedListener(item -> {

            String title = item.getTitle() == null
                    ? ""
                    : item.getTitle().toString()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            drawerLayout.closeDrawer(navigationView);

            if (title.contains("dashboard")
                    || title.equals("home")) {

                openStudentActivity(MainActivity.class);

            } else if (title.contains("eligibility")) {

                openStudentActivity(EligibilityActivity.class);

            } else if (title.contains("notice")) {

                openStudentActivity(NoticeActivity.class);

            } else if (title.contains("department")) {

                openStudentActivity(DepartmentsActivity.class);

            } else if (title.contains("apply")) {

                openStudentActivity(AdmissionApplicationActivity.class);

            } else if (title.contains("status")
                    || title.contains("tracker")) {

                openStudentActivity(ApplicationTrackerActivity.class);

            } else if (title.contains("admit")) {

                // Already on Admit Card.

            } else if (title.contains("result")) {

                openStudentActivity(ResultActivity.class);

            } else if (title.contains("syllabus")) {

                openStudentActivity(SyllabusActivity.class);
            }

            return true;
        });
    }

    private void openStudentActivity(Class<?> targetActivity) {

        startActivity(new Intent(
                AdmitCardActivity.this,
                targetActivity
        ));
    }

    // =========================================================
    // LOAD STUDENT ADMIT CARD
    // =========================================================

    private void loadStudentAdmitCard() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login first.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        removeApplicationListener();

        applicationListener = db.collection("applications")
                .document(user.getUid())
                .addSnapshotListener((document, error) -> {

                    if (error != null) {

                        hideAdmitCard();

                        Toast.makeText(
                                AdmitCardActivity.this,
                                "Unable to load admit card.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (document == null
                            || !document.exists()) {

                        hideAdmitCard();
                        return;
                    }

                    String paymentStatus =
                            getStringValue(document, "paymentStatus");

                    String applicationStatus =
                            getStringValue(document, "status");

                    if (!isPaid(paymentStatus)) {

                        hideAdmitCard();
                        return;
                    }

                    if (isRejected(applicationStatus)) {

                        hideAdmitCard();
                        return;
                    }

                    Boolean published =
                            document.getBoolean("admitCardPublished");

                    if (!Boolean.TRUE.equals(published)) {

                        hideAdmitCard();
                        return;
                    }

                    populateAdmitCard(document);
                });
    }

    // =========================================================
    // POPULATE ADMIT CARD
    // =========================================================

    private void populateAdmitCard(DocumentSnapshot document) {

        String applicantName =
                getStringValue(document, "fullName");

        if (applicantName.isEmpty()) {
            applicantName =
                    getStringValue(document, "name");
        }

        String fatherName =
                getStringValue(document, "fatherName");

        String applicationNo =
                getStringValue(document, "applicationId");

        if (applicationNo.isEmpty()) {
            applicationNo = document.getId();
        }

        String admitCardNo =
                getStringValue(document, "admitCardNo");

        String department =
                getStringValue(document, "department");

        String identityNumber =
                getIdentityNumber(document);

        String examDate =
                getStringValue(document, "examDate");

        String examTime =
                getStringValue(document, "examTime");

        String examCenter =
                getStringValue(document, "examCenter");

        String examDepartment =
                getStringValue(document, "examDepartment");

        if (examDepartment.isEmpty()) {
            examDepartment = department;
        }

        String seatNumber =
                getStringValue(document, "seatNumber");

        String instructions =
                getStringValue(document, "admitInstructions");

        tvApplicantName.setText(displayValue(applicantName));
        tvFatherName.setText(displayValue(fatherName));
        tvApplicationNo.setText(displayValue(applicationNo));
        tvAdmitCardNo.setText(displayValue(admitCardNo));
        tvDepartment.setText(displayValue(department));
        tvIdentityNumber.setText(displayValue(identityNumber));

        tvExamDate.setText(displayValue(examDate));
        tvExamTime.setText(displayValue(examTime));
        tvExamCenter.setText(displayValue(examCenter));
        tvExamDepartment.setText(displayValue(examDepartment));
        tvSeatNumber.setText(displayValue(seatNumber));

        tvInstructions.setText(
                instructions.isEmpty()
                        ? "No additional instructions."
                        : instructions
        );

        String photoUrl =
                getStringValue(document, "photoUrl");

        if (photoUrl.isEmpty()) {
            photoUrl =
                    getStringValue(document, "photoURL");
        }

        loadImage(photoUrl, ivApplicantPhoto);

        String signatureUrl =
                getStringValue(document, "signatureUrl");

        if (signatureUrl.isEmpty()) {
            signatureUrl =
                    getStringValue(document, "signatureURL");
        }

        loadImage(
                signatureUrl,
                ivApplicantSignature
        );

        loadAuthorizedSignature(
                document
        );

        admitCardPaper.setVisibility(View.VISIBLE);
        btnPrintAdmitCard.setVisibility(View.VISIBLE);
        btnPrintAdmitCard.setEnabled(true);
    }

    // =========================================================
    // LOAD AUTHORIZED SIGNATURE
    // Uploaded by the admin during Admit Card generation.
    // The Cloudinary URL is stored in the application document.
    // =========================================================

    private void loadAuthorizedSignature(
            DocumentSnapshot applicationDocument) {

        String authorizedSignatureUrl =
                getStringValue(
                        applicationDocument,
                        "authorizedSignatureUrl"
                );

        if (authorizedSignatureUrl.isEmpty()) {
            authorizedSignatureUrl =
                    getStringValue(
                            applicationDocument,
                            "authorizedSignatureURL"
                    );
        }

        loadImage(
                authorizedSignatureUrl,
                ivAuthorizedSignature
        );
    }


    // =========================================================
    // HIDE ADMIT CARD
    // =========================================================

    private void hideAdmitCard() {

        admitCardPaper.setVisibility(View.GONE);

        btnPrintAdmitCard.setVisibility(View.GONE);
        btnPrintAdmitCard.setEnabled(false);
    }

    // =========================================================
    // IMAGE LOADER
    // =========================================================

    private void loadImage(
            String imageUrl,
            ImageView imageView) {

        if (imageUrl == null
                || imageUrl.trim().isEmpty()) {

            imageView.setImageDrawable(null);
            return;
        }

        new Thread(() -> {

            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            InputStream inputStream = null;

            try {

                URL url = new URL(imageUrl);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoInput(true);

                connection.connect();

                inputStream =
                        connection.getInputStream();

                bitmap =
                        BitmapFactory.decodeStream(inputStream);

            } catch (Exception ignored) {

            } finally {

                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }

            Bitmap finalBitmap = bitmap;

            runOnUiThread(() -> {

                if (isFinishing()
                        || isDestroyed()) {
                    return;
                }

                if (finalBitmap != null) {

                    imageView.setImageBitmap(finalBitmap);

                } else {

                    imageView.setImageDrawable(null);
                }
            });

        }).start();
    }

    // =========================================================
    // PRINT BUTTON
    // =========================================================

    private void setupPrintButton() {

        btnPrintAdmitCard.setOnClickListener(v ->
                printAdmitCard()
        );
    }

    // =========================================================
    // PRINT ADMIT CARD
    // FIXED VERSION
    // =========================================================

    private void printAdmitCard() {
        if (admitCardPaper.getVisibility() != View.VISIBLE) {
            Toast.makeText(
                    this,
                    "Admit Card is not available.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final PrintManager printManager =
                (PrintManager) getSystemService(PRINT_SERVICE);

        if (printManager == null) {
            Toast.makeText(
                    this,
                    "Printing is not available on this device.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        /*
         * PRINT ONLY:
         * The screen Admit Card remains fully responsive. For printing, we
         * render the same View again at an a high-resolution print width instead of taking
         * the phone-width screenshot and stretching it. This prevents the
         * text from becoming oversized/distorted in the downloaded PDF.
         */
        final int oldWidth = admitCardPaper.getWidth();
        final int oldHeight = admitCardPaper.getHeight();

        if (oldWidth <= 0 || oldHeight <= 0) {
            Toast.makeText(
                    this,
                    "Unable to prepare Admit Card for printing.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        final Bitmap admitCardBitmap;
        final ViewGroup.LayoutParams originalLayoutParams =
                admitCardPaper.getLayoutParams();

        try {
            // A4 ratio: 210 / 297. Use a moderate render size to keep
            // memory usage safe on phones while retaining good PDF quality.
            final int printWidth = 1800;
            final int maxPrintHeight = 3200;

            ViewGroup.LayoutParams printLayoutParams =
                    new ViewGroup.LayoutParams(
                            printWidth,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            admitCardPaper.setLayoutParams(printLayoutParams);
            admitCardPaper.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            printWidth,
                            View.MeasureSpec.EXACTLY
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                            maxPrintHeight,
                            View.MeasureSpec.AT_MOST
                    )
            );

            int measuredPrintHeight =
                    admitCardPaper.getMeasuredHeight();

            if (measuredPrintHeight <= 0) {
                throw new IllegalStateException(
                        "Unable to measure Admit Card for A4."
                );
            }

            admitCardPaper.layout(
                    0,
                    0,
                    printWidth,
                    measuredPrintHeight
            );

            admitCardBitmap = Bitmap.createBitmap(
                    printWidth,
                    measuredPrintHeight,
                    Bitmap.Config.ARGB_8888
            );

            Canvas snapshotCanvas =
                    new Canvas(admitCardBitmap);

            // UI-thread snapshot at high-resolution print width.
            admitCardPaper.draw(snapshotCanvas);

            // Restore the exact screen layout immediately after the snapshot.
            admitCardPaper.setLayoutParams(originalLayoutParams);
            admitCardPaper.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            oldWidth,
                            View.MeasureSpec.EXACTLY
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                            oldHeight,
                            View.MeasureSpec.EXACTLY
                    )
            );
            admitCardPaper.layout(
                    0,
                    0,
                    oldWidth,
                    oldHeight
            );
            admitCardPaper.requestLayout();

        } catch (Exception e) {
            // Always restore the screen layout if print preparation fails.
            admitCardPaper.setLayoutParams(originalLayoutParams);
            admitCardPaper.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            oldWidth,
                            View.MeasureSpec.EXACTLY
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                            oldHeight,
                            View.MeasureSpec.EXACTLY
                    )
            );
            admitCardPaper.layout(
                    0,
                    0,
                    oldWidth,
                    oldHeight
            );
            admitCardPaper.requestLayout();

            Toast.makeText(
                    this,
                    "Unable to prepare Admit Card: "
                            + (e.getMessage() == null
                            ? "unknown error"
                            : e.getMessage()),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        final String jobName = "DUET Admit Card";

        PrintDocumentAdapter printAdapter =
                new PrintDocumentAdapter() {

                    private PrintedPdfDocument pdfDocument;

                    @Override
                    public void onLayout(
                            PrintAttributes oldAttributes,
                            PrintAttributes newAttributes,
                            CancellationSignal cancellationSignal,
                            LayoutResultCallback callback,
                            Bundle extras) {

                        if (cancellationSignal.isCanceled()) {
                            callback.onLayoutCancelled();
                            return;
                        }

                        try {
                            pdfDocument =
                                    new PrintedPdfDocument(
                                            AdmitCardActivity.this,
                                            newAttributes
                                    );

                            PrintDocumentInfo info =
                                    new PrintDocumentInfo.Builder(
                                            "DUET_Admit_Card.pdf"
                                    )
                                            .setContentType(
                                                    PrintDocumentInfo
                                                            .CONTENT_TYPE_DOCUMENT
                                            )
                                            .setPageCount(1)
                                            .build();

                            callback.onLayoutFinished(
                                    info,
                                    true
                            );

                        } catch (Exception e) {
                            callback.onLayoutFailed(
                                    e.getMessage() == null
                                            ? "Unable to create PDF."
                                            : e.getMessage()
                            );
                        }
                    }

                    @Override
                    public void onWrite(
                            PageRange[] pages,
                            ParcelFileDescriptor destination,
                            CancellationSignal cancellationSignal,
                            WriteResultCallback callback) {

                        PrintedPdfDocument document =
                                pdfDocument;

                        if (document == null) {
                            callback.onWriteFailed(
                                    "Print document is not ready."
                            );
                            return;
                        }

                        try {
                            if (cancellationSignal.isCanceled()) {
                                callback.onWriteCancelled();
                                return;
                            }

                            PdfDocument.Page page =
                                    document.startPage(1);

                            Canvas canvas =
                                    page.getCanvas();

                            float pageWidth =
                                    canvas.getWidth();

                            float pageHeight =
                                    canvas.getHeight();

                            float bitmapWidth =
                                    admitCardBitmap.getWidth();

                            float bitmapHeight =
                                    admitCardBitmap.getHeight();

                            /*
                             * PDF-only spacing:
                             * 25pt from the physical A4 page edge to the border,
                             * then 10pt from the border to the Admit Card content.
                             * The screen layout remains completely unchanged.
                             */
                            final float OUTER_BORDER_INSET_PT = 25.0f;
                            final float INNER_CONTENT_INSET_PT = 10.0f;

                            float contentLeft =
                                    OUTER_BORDER_INSET_PT
                                            + INNER_CONTENT_INSET_PT;
                            float contentTop =
                                    OUTER_BORDER_INSET_PT
                                            + INNER_CONTENT_INSET_PT;
                            float contentRight =
                                    pageWidth
                                            - OUTER_BORDER_INSET_PT
                                            - INNER_CONTENT_INSET_PT;
                            float contentBottom =
                                    pageHeight
                                            - OUTER_BORDER_INSET_PT
                                            - INNER_CONTENT_INSET_PT;

                            float contentWidth =
                                    contentRight - contentLeft;
                            float contentHeight =
                                    contentBottom - contentTop;

                            /*
                             * Keep the PDF proportions intact. Scale uniformly
                             * into the content area so fonts, tables, photos and
                             * signatures are not stretched or distorted.
                             */
                            float scale =
                                    Math.min(
                                            contentWidth / bitmapWidth,
                                            contentHeight / bitmapHeight
                                    );

                            float scaledWidth =
                                    bitmapWidth * scale;

                            float scaledHeight =
                                    bitmapHeight * scale;

                            float left =
                                    contentLeft
                                            + (contentWidth - scaledWidth) / 2f;

                            float top =
                                    contentTop
                                            + (contentHeight - scaledHeight) / 2f;

                            canvas.save();

                            canvas.translate(left, top);
                            canvas.scale(scale, scale);

                            canvas.drawBitmap(
                                    admitCardBitmap,
                                    0f,
                                    0f,
                                    null
                            );

                            canvas.restore();

                            // Final A4 document border: 25pt outside margin.
                            Paint borderPaint =
                                    new Paint(Paint.ANTI_ALIAS_FLAG);
                            borderPaint.setStyle(Paint.Style.STROKE);
                            borderPaint.setColor(Color.rgb(8, 61, 120));
                            borderPaint.setStrokeWidth(2.0f);

                            // Keep the entire 2pt stroke outside the content-safe area.
                            // The visible outer edge is exactly 25pt from the
                            // physical page edge, while content starts another
                            // 10pt inside that border.
                            float borderInset =
                                    OUTER_BORDER_INSET_PT + 1.0f;

                            canvas.drawRect(
                                    borderInset,
                                    borderInset,
                                    pageWidth - borderInset,
                                    pageHeight - borderInset,
                                    borderPaint
                            );

                            document.finishPage(page);

                            FileOutputStream outputStream =
                                    new FileOutputStream(
                                            destination
                                                    .getFileDescriptor()
                                    );

                            document.writeTo(outputStream);
                            outputStream.flush();

                            callback.onWriteFinished(
                                    new PageRange[]{
                                            PageRange.ALL_PAGES
                                    }
                            );

                        } catch (Exception e) {

                            callback.onWriteFailed(
                                    e.getMessage() == null
                                            ? "Printing failed."
                                            : e.getMessage()
                            );

                        } finally {

                            try {
                                document.close();
                            } catch (Exception ignored) {
                            }

                            pdfDocument = null;

                            /*
                             * IMPORTANT: Do NOT recycle admitCardBitmap here.
                             * Android print preview can invoke onWrite() more
                             * than once (preview/retry/save). Recycling it
                             * after the first write causes: "Canvas: trying
                             * to use a recycled bitmap" on the next write.
                             * Let the print adapter/GC release it naturally
                             * after the print job is completely finished.
                             */
                        }
                    }
                };

        /*
         * Keep the requested physical paper size:
         * ISO A4 = 210 x 297 mm.
         */
        PrintAttributes attributes =
                new PrintAttributes.Builder()
                        .setMediaSize(
                                PrintAttributes.MediaSize.ISO_A4
                        )
                        .setResolution(
                                new PrintAttributes.Resolution(
                                        "duet_print",
                                        "DUET Print",
                                        300,
                                        300
                                )
                        )
                        .setMinMargins(
                                PrintAttributes.Margins.NO_MARGINS
                        )
                        .build();

        try {
            printManager.print(
                    jobName,
                    printAdapter,
                    attributes
            );
        } catch (Exception e) {
            // Do not recycle admitCardBitmap here. The print framework may
            // still hold/use the adapter after print() returns.

            Toast.makeText(
                    this,
                    "Print failed: "
                            + (e.getMessage() == null
                            ? "unknown error"
                            : e.getMessage()),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // FIRESTORE HELPERS
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

    private String displayValue(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "Not available";
        }

        return value;
    }

    private boolean isPaid(String status) {

        if (status == null) {
            return false;
        }

        String normalized =
                status.trim()
                        .toUpperCase(Locale.ROOT);

        return normalized.equals("PAID")
                || normalized.equals("VERIFIED")
                || normalized.equals("PAYMENT_VERIFIED")
                || normalized.equals("CONFIRMED")
                || normalized.equals("SUCCESSFUL");
    }

    private boolean isRejected(String status) {

        if (status == null) {
            return false;
        }

        String normalized =
                status.trim()
                        .toUpperCase(Locale.ROOT);

        return normalized.equals("REJECTED")
                || normalized.equals("REJECT")
                || normalized.equals("DECLINED")
                || normalized.equals("DECLINE");
    }

    // =========================================================
    // LISTENER CLEANUP
    // =========================================================

    private void removeApplicationListener() {

        if (applicationListener != null) {

            applicationListener.remove();
            applicationListener = null;
        }
    }

    // =========================================================
    // BACK
    // =========================================================

    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(
                navigationView
        )) {

            drawerLayout.closeDrawer(
                    navigationView
            );

        } else {

            super.onBackPressed();
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        removeApplicationListener();

        super.onDestroy();
    }
}
