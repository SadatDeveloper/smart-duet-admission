package com.example.smartduetadmissionsystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AdmissionApplicationActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CloudinaryUploader cloudinaryUploader;
    private FirebaseUser user;

    private int currentStep = 1;

    private LinearLayout layoutStep1, layoutStep2, layoutStep3,
            layoutStep4, layoutStep5;

    private EditText etFullName, etFatherName, etMotherName,
            etPhone, etEmail, etDateOfBirth, etNidNumber,
            etSscGpa, etDiplomaCgpa, etTransactionId;

    private Spinner spGender, spPassingYear, spDiplomaTechnology,
            spDepartment, spQuota, spPaymentMethod;

    private TextView tvEligibility;

    private TextView tvPhoto, tvSignature,
            tvNidDocument, tvQuotaDocument;

    private TextView tvSummaryApplicationId,
            tvSummaryName, tvSummaryMobile,
            tvSummaryTechnology, tvSummaryYear,
            tvSummaryDepartment, tvSummaryQuota;

    private String applicationId;
    private TextView tvReviewReady;
    private TextView tvReviewPersonal;
    private TextView tvReviewAcademic;
    private TextView tvReviewDepartment;
    private TextView tvReviewDocuments;
    private TextView tvReviewPayment;

    private CheckBox cbDeclaration;

    private Uri photoUri;
    private Uri signatureUri;
    private Uri nidDocumentUri;
    private Uri quotaDocumentUri;

    // Cloudinary upload state
    private boolean isUploading = false;

    private String photoUrl = "";
    private String signatureUrl = "";
    private String nidDocumentUrl = "";
    private String quotaDocumentUrl = "";

    private static final int PICK_PHOTO = 101;
    private static final int PICK_SIGNATURE = 102;
    private static final int PICK_NID = 103;
    private static final int PICK_QUOTA = 104;

    // File size limits: images (photo/signature) max 100 KB.
    private static final long MAX_IMAGE_SIZE = 100L * 1024L;
    private static final long MAX_DOCUMENT_SIZE = 3L * 1024L * 1024L;

    // AUTO SAVE: draft data is stored locally on this device.
    private SharedPreferences draftPrefs;
    private boolean isLoadingDraft = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admission_application);

        // AUTO SAVE: local draft storage.
        draftPrefs = getSharedPreferences("admission_application_draft", MODE_PRIVATE);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        cloudinaryUploader = new CloudinaryUploader(this);
        user = auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        initializeViews();
        tvReviewReady = findViewById(R.id.tvReviewReady);
        tvReviewPersonal = findViewById(R.id.tvReviewPersonal);
        tvReviewAcademic = findViewById(R.id.tvReviewAcademic);
        tvReviewDepartment = findViewById(R.id.tvReviewDepartment);
        tvReviewDocuments = findViewById(R.id.tvReviewDocuments);
        tvReviewPayment = findViewById(R.id.tvReviewPayment);
        setupSpinners();
        loadDraft();
        loadUserProfile();
        loadExistingApplication();
        setupListeners();

        showStep(1);
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        layoutStep3 = findViewById(R.id.layoutStep3);
        layoutStep4 = findViewById(R.id.layoutStep4);
        layoutStep5 = findViewById(R.id.layoutStep5);

        etFullName = findViewById(R.id.etFullName);
        etFatherName = findViewById(R.id.etFatherName);
        etMotherName = findViewById(R.id.etMotherName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etNidNumber = findViewById(R.id.etNidNumber);

        etSscGpa = findViewById(R.id.etSscGpa);
        etDiplomaCgpa = findViewById(R.id.etDiplomaCgpa);

        etTransactionId = findViewById(R.id.etTransactionId);

        spGender = findViewById(R.id.spGender);
        spPassingYear = findViewById(R.id.spPassingYear);
        spDiplomaTechnology = findViewById(R.id.spDiplomaTechnology);
        spDepartment = findViewById(R.id.spDepartment);
        spQuota = findViewById(R.id.spQuota);
        spPaymentMethod = findViewById(R.id.spPaymentMethod);

        tvEligibility = findViewById(R.id.tvEligibility);

        tvPhoto = findViewById(R.id.tvPhoto);
        tvSignature = findViewById(R.id.tvSignature);
        tvNidDocument = findViewById(R.id.tvNidDocument);
        tvQuotaDocument = findViewById(R.id.tvQuotaDocument);

        tvSummaryApplicationId =
                findViewById(R.id.tvSummaryApplicationId);

        tvSummaryName = findViewById(R.id.tvSummaryName);
        tvSummaryMobile = findViewById(R.id.tvSummaryMobile);
        tvSummaryTechnology = findViewById(R.id.tvSummaryTechnology);
        tvSummaryYear = findViewById(R.id.tvSummaryYear);
        tvSummaryDepartment = findViewById(R.id.tvSummaryDepartment);
        tvSummaryQuota = findViewById(R.id.tvSummaryQuota);

        cbDeclaration = findViewById(R.id.cbDeclaration);
    }

    // =========================================================
    // SPINNERS
    // =========================================================

    private void setupSpinners() {

        setSpinner(
                spGender,
                new String[]{
                        "Select Gender",
                        "Male",
                        "Female",
                        "Other"
                }
        );

        setSpinner(
                spPassingYear,
                new String[]{
                        "Select Passing Year",
                        "2026",
                        "2025"
                }
        );

        // EXACT DIPLOMA TECHNOLOGY LIST
        setSpinner(
                spDiplomaTechnology,
                new String[]{
                        "Select Diploma Technology",

                        "Civil Technology",
                        "Surveying Technology",

                        "Electrical Technology",
                        "Electronics Technology",
                        "Telecommunication Technology",

                        "Mechanical Technology",
                        "Power Technology",
                        "Automobile Technology",
                        "RAC Technology",
                        "Mechatronics Technology",
                        "Marine Technology",
                        "Shipbuilding Technology",
                        "Chemical Technology",

                        "Computer Technology",
                        "Computer Science & Technology",
                        "Data Telecommunication & Networking",

                        "Textile Technology",
                        "Jute Technology",
                        "Garments & Pattern Making Technology",

                        "Architecture Technology",
                        "Architecture & Interior Design",

                        "Food Technology",

                        "Ceramic Technology",
                        "Glass Technology",
                        "Mining & Mine Survey Technology"
                }
        );

        // Initially only placeholder
        setSpinner(
                spDepartment,
                new String[]{
                        "Select Department"
                }
        );

        setSpinner(
                spQuota,
                new String[]{
                        "Select Quota",
                        "General",
                        "Freedom Fighter's Son/Daughter",
                        "Tribal",
                        "Disabled",
                        "Other"
                }
        );

        setSpinner(
                spPaymentMethod,
                new String[]{
                        "Select Payment Method",
                        "bKash",
                        "Nagad",
                        "Rocket",
                        "Bank Payment"
                }
        );
    }

    private void setSpinner(
            Spinner spinner,
            String[] data) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        data
                );

        spinner.setAdapter(adapter);
    }

    // =========================================================
    // LOAD USER PROFILE
    // =========================================================

    private void loadUserProfile() {

        if (user.getEmail() != null) {
            etEmail.setText(user.getEmail());
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    etFullName.setText(
                            getValue(documentSnapshot, "name")
                    );

                    etFatherName.setText(
                            getValue(documentSnapshot, "fatherName")
                    );

                    etMotherName.setText(
                            getValue(documentSnapshot, "motherName")
                    );

                    etPhone.setText(
                            getValue(documentSnapshot, "phone")
                    );

                    etDateOfBirth.setText(
                            getValue(documentSnapshot, "dateOfBirth")
                    );

                    etNidNumber.setText(
                            getValue(documentSnapshot, "nidNumber")
                    );

                    String gender =
                            getValue(documentSnapshot, "gender");

                    if (!TextUtils.isEmpty(gender)) {

                        ArrayAdapter adapter =
                                (ArrayAdapter) spGender.getAdapter();

                        int position =
                                adapter.getPosition(gender);

                        if (position >= 0) {
                            spGender.setSelection(position);
                        }
                    }
                });
    }

    // =========================================================
    // LOAD EXISTING APPLICATION
    // =========================================================

    private void loadExistingApplication() {

        db.collection("applications")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    String existingId =
                            documentSnapshot.getString("applicationId");

                    if (!TextUtils.isEmpty(existingId)) {
                        applicationId = existingId;
                    }
                });
    }

    // =========================================================
    // GENERATE APPLICATION ID
    // =========================================================

    private String generateApplicationId() {

        String shortId =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8)
                        .toUpperCase(Locale.US);

        return "DUET-2026-" + shortId;
    }

    private String getValue(
            com.google.firebase.firestore.DocumentSnapshot doc,
            String field) {

        Object value = doc.get(field);

        return value == null
                ? ""
                : value.toString();
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    private void setupListeners() {

        findViewById(R.id.btnMenu)
                .setOnClickListener(v ->
                        drawerLayout.openDrawer(
                                GravityCompat.START
                        ));

        findViewById(R.id.ivProfile)
                .setOnClickListener(v ->
                        startActivity(
                                new Intent(
                                        this,
                                        ProfileActivity.class
                                )
                        ));

        etDateOfBirth.setOnClickListener(
                v -> showDatePicker()
        );

        // STEP 1
        findViewById(R.id.btnStep1Next)
                .setOnClickListener(v -> {

                    if (validateStep1()) {
                        showStep(2);
                    }
                });

        // STEP 2
        findViewById(R.id.btnStep2Previous)
                .setOnClickListener(v ->
                        showStep(1)
                );

        findViewById(R.id.btnStep2Next)
                .setOnClickListener(v -> {

                    if (validateStep2()) {
                        showStep(3);
                    }
                });

        // STEP 3
        findViewById(R.id.btnStep3Previous)
                .setOnClickListener(v ->
                        showStep(2)
                );

        findViewById(R.id.btnStep3Next)
                .setOnClickListener(v -> {

                    if (validateStep3()) {
                        showStep(4);
                    }
                });

        // STEP 4
        findViewById(R.id.btnStep4Previous)
                .setOnClickListener(v ->
                        showStep(3)
                );

        findViewById(R.id.btnStep4Next)
                .setOnClickListener(v -> {

                    if (validateStep4()) {

                        prepareReview();

                        showStep(5);
                    }
                });

        // STEP 5
        findViewById(R.id.btnStep5Previous)
                .setOnClickListener(v ->
                        showStep(4)
                );

        findViewById(R.id.btnSubmitApplication)
                .setOnClickListener(v ->
                        showFinalConfirmation()
                );

        // DOCUMENTS
        findViewById(R.id.btnPhoto)
                .setOnClickListener(v ->
                        chooseFile(
                                PICK_PHOTO,
                                false
                        ));

        findViewById(R.id.btnSignature)
                .setOnClickListener(v ->
                        chooseFile(
                                PICK_SIGNATURE,
                                false
                        ));

        findViewById(R.id.btnNidDocument)
                .setOnClickListener(v ->
                        chooseFile(
                                PICK_NID,
                                true
                        ));

        findViewById(R.id.btnQuotaDocument)
                .setOnClickListener(v ->
                        chooseFile(
                                PICK_QUOTA,
                                true
                        ));

        // =====================================================
        // DIPLOMA TECHNOLOGY -> DEPARTMENT MAPPING
        // =====================================================

        spDiplomaTechnology.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        String technology =
                                parent
                                        .getItemAtPosition(position)
                                        .toString();

                        updateEligibleDepartments(
                                technology
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        // QUOTA
        spQuota.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        String quota =
                                parent
                                        .getItemAtPosition(position)
                                        .toString();

                        boolean required =
                                !quota.equals("Select Quota")
                                        &&
                                        !quota.equals("General");

                        findViewById(
                                R.id.quotaDocumentLabel
                        ).setVisibility(
                                required
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        findViewById(
                                R.id.btnQuotaDocument
                        ).setVisibility(
                                required
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        tvQuotaDocument.setVisibility(
                                required
                                        ? View.VISIBLE
                                        : View.GONE
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        // DRAWER
        navigationView.setNavigationItemSelectedListener(
                item -> {

                    handleDrawerNavigation(item);

                    drawerLayout.closeDrawer(
                            GravityCompat.START
                    );

                    return true;
                }
        );

        // BOTTOM NAVIGATION
        bottomNavigation.setOnItemSelectedListener(
                item -> {

                    if (item.getItemId() == R.id.nav_home) {

                        startActivity(
                                new Intent(
                                        AdmissionApplicationActivity.this,
                                        MainActivity.class
                                )
                        );

                        return true;

                    } else if (
                            item.getItemId() == R.id.nav_notice) {

                        startActivity(
                                new Intent(
                                        AdmissionApplicationActivity.this,
                                        NoticeActivity.class
                                )
                        );

                        return true;

                    } else if (
                            item.getItemId() == R.id.nav_profile) {

                        startActivity(
                                new Intent(
                                        AdmissionApplicationActivity.this,
                                        ProfileActivity.class
                                )
                        );

                        return true;
                    }

                    return false;
                }
        );
    }

    // =========================================================
    // EXACT DEPARTMENT MAPPING
    // =========================================================

    private void updateEligibleDepartments(
            String technology) {

        List<String> departments =
                new ArrayList<>();

        departments.add("Select Department");

        switch (technology) {

            // =================================================
            // CIVIL
            // =================================================

            case "Civil Technology":
            case "Surveying Technology":

                departments.add(
                        "Civil Engineering"
                );

                break;


            // =================================================
            // EEE
            // =================================================

            case "Electrical Technology":
            case "Telecommunication Technology":

                departments.add(
                        "Electrical and Electronic Engineering (EEE)"
                );

                break;


            // =================================================
            // ELECTRONICS -> EEE + CSE
            // =================================================

            case "Electronics Technology":

                departments.add(
                        "Electrical and Electronic Engineering (EEE)"
                );

                departments.add(
                        "Computer Science and Engineering (CSE)"
                );

                break;


            // =================================================
            // ME + IPE + MME + ChE
            // =================================================

            case "Mechanical Technology":
            case "Power Technology":
            case "RAC Technology":

                departments.add(
                        "Mechanical Engineering (ME)"
                );

                departments.add(
                        "Industrial and Production Engineering (IPE)"
                );

                departments.add(
                        "Materials and Metallurgical Engineering (MME)"
                );

                departments.add(
                        "Chemical Engineering (ChE)"
                );

                break;


            // =================================================
            // ME + IPE + MME
            // =================================================

            case "Automobile Technology":

                departments.add(
                        "Mechanical Engineering (ME)"
                );

                departments.add(
                        "Industrial and Production Engineering (IPE)"
                );

                departments.add(
                        "Materials and Metallurgical Engineering (MME)"
                );

                break;


            // =================================================
            // ME + IPE
            // =================================================

            case "Mechatronics Technology":
            case "Marine Technology":

                departments.add(
                        "Mechanical Engineering (ME)"
                );

                departments.add(
                        "Industrial and Production Engineering (IPE)"
                );

                break;


            // =================================================
            // ME + IPE + MME
            // =================================================

            case "Shipbuilding Technology":

                departments.add(
                        "Mechanical Engineering (ME)"
                );

                departments.add(
                        "Industrial and Production Engineering (IPE)"
                );

                departments.add(
                        "Materials and Metallurgical Engineering (MME)"
                );

                break;


            // =================================================
            // ME + IPE + MME + ChE
            // =================================================

            case "Chemical Technology":

                departments.add(
                        "Mechanical Engineering (ME)"
                );

                departments.add(
                        "Industrial and Production Engineering (IPE)"
                );

                departments.add(
                        "Materials and Metallurgical Engineering (MME)"
                );

                departments.add(
                        "Chemical Engineering (ChE)"
                );

                break;


            // =================================================
            // CSE
            // =================================================

            case "Computer Technology":
            case "Computer Science & Technology":
            case "Data Telecommunication & Networking":

                departments.add(
                        "Computer Science and Engineering (CSE)"
                );

                break;


            // =================================================
            // TEXTILE
            // =================================================

            case "Textile Technology":
            case "Jute Technology":
            case "Garments & Pattern Making Technology":

                departments.add(
                        "Textile Engineering"
                );

                break;


            // =================================================
            // ARCHITECTURE
            // =================================================

            case "Architecture Technology":
            case "Architecture & Interior Design":

                departments.add(
                        "Architecture"
                );

                break;


            // =================================================
            // FOOD
            // =================================================

            case "Food Technology":

                departments.add(
                        "Food Engineering"
                );

                break;


            // =================================================
            // MME
            // =================================================

            case "Ceramic Technology":
            case "Glass Technology":
            case "Mining & Mine Survey Technology":

                departments.add(
                        "Materials and Metallurgical Engineering (MME)"
                );

                break;
        }

        // =====================================================
        // UPDATE DEPARTMENT SPINNER
        // =====================================================

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        departments
                );

        spDepartment.setAdapter(adapter);

        // =====================================================
        // ELIGIBILITY MESSAGE
        // =====================================================

        if (departments.size() > 1) {

            StringBuilder message =
                    new StringBuilder();

            message.append(
                    "✓ You are eligible for "
            );

            message.append(
                    departments.size() - 1
            );

            if (departments.size() - 1 == 1) {
                message.append(" department: ");
            } else {
                message.append(" departments: ");
            }

            for (int i = 1;
                 i < departments.size();
                 i++) {

                message.append(
                        departments.get(i)
                );

                if (i < departments.size() - 1) {
                    message.append(", ");
                }
            }

            message.append(".");

            tvEligibility.setText(
                    message.toString()
            );

            tvEligibility.setVisibility(
                    View.VISIBLE
            );

        } else {

            tvEligibility.setText(
                    "ⓘ Please select your Diploma Technology first."
            );

            tvEligibility.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // =========================================================
    // STEP 1 VALIDATION
    // =========================================================

    private boolean validateStep1() {

        if (TextUtils.isEmpty(
                etFullName.getText().toString().trim())) {

            etFullName.setError(
                    "Full Name is required"
            );

            return false;
        }

        if (TextUtils.isEmpty(
                etFatherName.getText().toString().trim())) {

            etFatherName.setError(
                    "Father's Name is required"
            );

            return false;
        }

        if (TextUtils.isEmpty(
                etMotherName.getText().toString().trim())) {

            etMotherName.setError(
                    "Mother's Name is required"
            );

            return false;
        }

        String phone =
                etPhone.getText().toString().trim();

        if (phone.length() != 11) {

            etPhone.setError(
                    "Enter valid 11 digit mobile number"
            );

            return false;
        }

        if (TextUtils.isEmpty(
                etDateOfBirth.getText().toString().trim())) {

            etDateOfBirth.setError(
                    "Date of Birth is required"
            );

            return false;
        }

        if (spGender.getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Please select gender",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (TextUtils.isEmpty(
                etNidNumber.getText().toString().trim())) {

            etNidNumber.setError(
                    "NID/Birth Registration Number is required"
            );

            return false;
        }

        return true;
    }

    // =========================================================
    // STEP 2 VALIDATION
    // =========================================================

    private boolean validateStep2() {

        String ssc =
                etSscGpa.getText().toString().trim();

        String diploma =
                etDiplomaCgpa.getText().toString().trim();

        if (TextUtils.isEmpty(ssc)) {

            etSscGpa.setError(
                    "SSC GPA is required"
            );

            return false;
        }

        if (TextUtils.isEmpty(diploma)) {

            etDiplomaCgpa.setError(
                    "Diploma CGPA is required"
            );

            return false;
        }

        try {

            double sscValue =
                    Double.parseDouble(ssc);

            double diplomaValue =
                    Double.parseDouble(diploma);

            if (sscValue < 0 ||
                    sscValue > 5) {

                etSscGpa.setError(
                        "GPA must be between 0.00 and 5.00"
                );

                return false;
            }

            if (diplomaValue < 0 ||
                    diplomaValue > 4) {

                etDiplomaCgpa.setError(
                        "CGPA must be between 0.00 and 4.00"
                );

                return false;
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Enter valid GPA/CGPA",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (spPassingYear.getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Please select passing year",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (spDiplomaTechnology
                .getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Please select diploma technology",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    // =========================================================
    // STEP 3 VALIDATION
    // =========================================================

    private boolean validateStep3() {

        if (spDepartment.getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Please select an eligible department",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    // =========================================================
    // STEP 4 VALIDATION
    // =========================================================

    private boolean validateStep4() {

        if (photoUri == null) {

            Toast.makeText(
                    this,
                    "Please select applicant photo",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (signatureUri == null) {

            Toast.makeText(
                    this,
                    "Please select signature",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (nidDocumentUri == null) {

            Toast.makeText(
                    this,
                    "Please select NID/Birth Registration document",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (spQuota.getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Please select quota",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        String quota =
                spQuota.getSelectedItem().toString();

        if (!quota.equals("General")
                && quotaDocumentUri == null) {

            Toast.makeText(
                    this,
                    "Quota supporting document is required",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    // =========================================================
    // PREPARE REVIEW
    // =========================================================

    private void prepareReview() {
        // Existing Step 5 summary is preserved.
        // The final confirmation dialog is an additional safety layer.

        if (TextUtils.isEmpty(applicationId)) {
            applicationId = generateApplicationId();
        }

        tvSummaryApplicationId.setText(
                "Application ID\n" + applicationId
        );

        tvSummaryName.setText(
                "Applicant Name\n" +
                        etFullName.getText()
                                .toString()
                                .trim()
        );

        tvSummaryMobile.setText(
                "Mobile\n" +
                        etPhone.getText()
                                .toString()
                                .trim()
        );

        tvSummaryTechnology.setText(
                "Diploma Technology\n" +
                        spDiplomaTechnology
                                .getSelectedItem()
                                .toString()
        );

        tvSummaryYear.setText(
                "Diploma Passing Year\n" +
                        spPassingYear
                                .getSelectedItem()
                                .toString()
        );

        tvSummaryDepartment.setText(
                "Selected Department\n" +
                        spDepartment
                                .getSelectedItem()
                                .toString()
        );

        tvSummaryQuota.setText(
                "Quota\n" +
                        spQuota
                                .getSelectedItem()
                                .toString()
        );
    }

    // =========================================================
    // SUBMIT APPLICATION
    // =========================================================

    // =========================================================
    // STEP 3: STRONG ELIGIBILITY & VALIDATION
    // =========================================================

    private boolean validateBeforeSubmit() {

        // ---------- Personal Information ----------
        if (!requireText(etFullName, "Full name is required")) return false;
        if (!requireText(etFatherName, "Father's name is required")) return false;
        if (!requireText(etMotherName, "Mother's name is required")) return false;

        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty() || !phone.matches("01[3-9]\\d{8}")) {
            etPhone.setError("Enter a valid Bangladesh mobile number");
            etPhone.requestFocus();
            return false;
        }

        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()
                || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return false;
        }

        if (!requireText(etDateOfBirth, "Date of birth is required")) return false;
        if (!requireText(etNidNumber, "NID / Birth Registration is required")) return false;

        if (spGender.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select gender.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // ---------- Academic Information ----------
        Double sscGpa = parseNumber(etSscGpa);
        if (sscGpa == null || sscGpa < 0.0 || sscGpa > 5.0) {
            etSscGpa.setError("SSC GPA must be between 0.00 and 5.00");
            etSscGpa.requestFocus();
            return false;
        }

        Double diplomaCgpa = parseNumber(etDiplomaCgpa);
        if (diplomaCgpa == null || diplomaCgpa < 0.0 || diplomaCgpa > 4.0) {
            etDiplomaCgpa.setError("Diploma CGPA must be between 0.00 and 4.00");
            etDiplomaCgpa.requestFocus();
            return false;
        }

        if (spPassingYear.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select passing year.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spDiplomaTechnology.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select diploma technology.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // ---------- Department Eligibility ----------
        if (spDepartment.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a department.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String technology = spDiplomaTechnology.getSelectedItem().toString();
        String department = spDepartment.getSelectedItem().toString();

        if (!isDepartmentEligible(technology, department)) {
            Toast.makeText(
                    this,
                    "Selected department is not available for " + technology + ".",
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        // ---------- Documents ----------
        if (photoUri == null) {
            Toast.makeText(this, "Please select Applicant Photo.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (signatureUri == null) {
            Toast.makeText(this, "Please select Signature.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (nidDocumentUri == null) {
            Toast.makeText(this, "Please select NID / Birth Registration document.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // ---------- Quota ----------
        String quota = spQuota.getSelectedItem() == null
                ? ""
                : spQuota.getSelectedItem().toString();

        if (quota.isEmpty() || quota.equals("Select Quota")) {
            Toast.makeText(this, "Please select quota.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!quota.equals("General") && quotaDocumentUri == null) {
            Toast.makeText(
                    this,
                    "Quota supporting document is required.",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        // ---------- Payment ----------
        if (spPaymentMethod.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select payment method.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (etTransactionId.getText().toString().trim().isEmpty()) {
            etTransactionId.setError("Transaction ID is required");
            etTransactionId.requestFocus();
            return false;
        }

        if (!cbDeclaration.isChecked()) {
            Toast.makeText(this, "Please accept the declaration.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean requireText(EditText field, String message) {
        if (field == null || field.getText().toString().trim().isEmpty()) {
            if (field != null) {
                field.setError(message);
                field.requestFocus();
            }
            return false;
        }
        return true;
    }

    private Double parseNumber(EditText field) {
        try {
            return Double.parseDouble(field.getText().toString().trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isDepartmentEligible(String technology, String department) {

        switch (technology) {

            case "Civil Technology":
            case "Surveying Technology":
                return department.equals("Civil Engineering");

            case "Electrical Technology":
            case "Telecommunication Technology":
                return department.equals(
                        "Electrical and Electronic Engineering (EEE)");

            case "Electronics Technology":
                return department.equals(
                        "Electrical and Electronic Engineering (EEE)")
                        || department.equals(
                        "Computer Science and Engineering (CSE)");

            case "Mechanical Technology":
            case "Power Technology":
            case "RAC Technology":
                return department.equals("Mechanical Engineering (ME)")
                        || department.equals(
                        "Industrial and Production Engineering (IPE)")
                        || department.equals(
                        "Materials and Metallurgical Engineering (MME)")
                        || department.equals("Chemical Engineering (ChE)");

            case "Automobile Technology":
                return department.equals("Mechanical Engineering (ME)")
                        || department.equals(
                        "Industrial and Production Engineering (IPE)")
                        || department.equals(
                        "Materials and Metallurgical Engineering (MME)");

            case "Mechatronics Technology":
            case "Marine Technology":
                return department.equals("Mechanical Engineering (ME)")
                        || department.equals(
                        "Industrial and Production Engineering (IPE)");

            case "Shipbuilding Technology":
                return department.equals("Mechanical Engineering (ME)")
                        || department.equals(
                        "Industrial and Production Engineering (IPE)")
                        || department.equals(
                        "Materials and Metallurgical Engineering (MME)");

            case "Chemical Technology":
                return department.equals("Mechanical Engineering (ME)")
                        || department.equals(
                        "Industrial and Production Engineering (IPE)")
                        || department.equals(
                        "Materials and Metallurgical Engineering (MME)")
                        || department.equals("Chemical Engineering (ChE)");

            case "Computer Technology":
            case "Computer Science and Technology":
            case "Data Telecommunication and Networking Technology":
                return department.equals(
                        "Computer Science and Engineering (CSE)");

            case "Textile Technology":
            case "Jute Technology":
            case "Garments Design and Pattern Making Technology":
                return department.equals("Textile Engineering");

            case "Architecture Technology":
            case "Architecture and Interior Design Technology":
                return department.equals("Architecture");

            case "Food Technology":
                return department.equals("Food Engineering");

            case "Ceramic Technology":
            case "Glass Technology":
            case "Mining Technology":
                return department.equals(
                        "Materials and Metallurgical Engineering (MME)");

            default:
                // Do not invent a new eligibility rule.
                // Existing spinner mapping remains the source of truth.
                return true;
        }
    }

    // =========================================================
    // STEP 4: PROFESSIONAL REVIEW & CONFIRM SUBMIT
    // =========================================================

    private void updateProfessionalReview() {
        if (tvReviewReady != null) {
            tvReviewReady.setText("READY");
        }

        if (tvReviewPersonal != null) {
            tvReviewPersonal.setText("✓ Personal Information");
        }

        if (tvReviewAcademic != null) {
            tvReviewAcademic.setText("✓ Academic Information");
        }

        if (tvReviewDepartment != null) {
            tvReviewDepartment.setText("✓ Department Selection");
        }

        if (tvReviewDocuments != null) {
            tvReviewDocuments.setText("✓ Required Documents");
        }

        if (tvReviewPayment != null) {
            tvReviewPayment.setText("✓ Payment Information");
        }
    }

    private void showFinalConfirmation() {

        // Final validation before opening the review popup.
        if (!validateBeforeSubmit()) {
            return;
        }

        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_application_review);
        dialog.setCancelable(true);

        TextView tvName = dialog.findViewById(R.id.reviewName);
        TextView tvPhone = dialog.findViewById(R.id.reviewPhone);
        TextView tvEmail = dialog.findViewById(R.id.reviewEmail);
        TextView tvTechnology = dialog.findViewById(R.id.reviewTechnology);
        TextView tvYear = dialog.findViewById(R.id.reviewYear);
        TextView tvDepartment = dialog.findViewById(R.id.reviewDepartment);
        TextView tvQuota = dialog.findViewById(R.id.reviewQuota);
        TextView tvPayment = dialog.findViewById(R.id.reviewPayment);
        TextView tvTransaction = dialog.findViewById(R.id.reviewTransaction);
        TextView tvDocuments = dialog.findViewById(R.id.reviewDocuments);

        tvName.setText(valueOrDash(etFullName.getText().toString()));
        tvPhone.setText(valueOrDash(etPhone.getText().toString()));
        tvEmail.setText(valueOrDash(etEmail.getText().toString()));
        tvTechnology.setText(selectedSpinnerValue(spDiplomaTechnology));
        tvYear.setText(selectedSpinnerValue(spPassingYear));
        tvDepartment.setText(selectedSpinnerValue(spDepartment));
        tvQuota.setText(selectedSpinnerValue(spQuota));
        tvPayment.setText(selectedSpinnerValue(spPaymentMethod));
        tvTransaction.setText(valueOrDash(etTransactionId.getText().toString()));

        String quota = selectedSpinnerValue(spQuota);
        String documentStatus =
                "✓ Applicant Photo  •  Maximum 100 KB\n" +
                        "✓ Signature  •  Maximum 100 KB\n" +
                        "✓ NID / Birth Registration  •  Maximum 3 MB";

        if (!"General".equalsIgnoreCase(quota)) {
            documentStatus += "\n✓ Quota Supporting Document  •  Maximum 3 MB";
        }
        tvDocuments.setText(documentStatus);

        android.widget.Button btnEdit = dialog.findViewById(R.id.btnReviewEdit);
        android.widget.Button btnConfirm = dialog.findViewById(R.id.btnReviewConfirm);

        // EDIT: return to the application form. Existing data remains in the fields.
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showStep(1);
        });

        // SUBMIT APPLICATION: use the existing, tested submission/upload method.
        btnConfirm.setOnClickListener(v -> {
            if (isUploading) {
                Toast.makeText(
                        AdmissionApplicationActivity.this,
                        "Upload is already in progress. Please wait.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            dialog.dismiss();
            submitApplication();
        });

        dialog.setOnShowListener(d -> {
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(
                                android.graphics.Color.TRANSPARENT
                        )
                );
                window.setGravity(android.view.Gravity.CENTER);

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int screenHeight = getResources().getDisplayMetrics().heightPixels;

                window.setLayout(
                        (int) (screenWidth * 0.94f),
                        (int) (screenHeight * 0.88f)
                );
            }
        });

        dialog.show();
    }

    private String selectedSpinnerValue(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) {
            return "Not selected";
        }
        return spinner.getSelectedItem().toString();
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Not provided";
        }
        return value.trim();
    }


    // Existing final submission method is intentionally unchanged.
    private void submitApplication() {

        // STEP 3: Strong final validation.
        // Existing submission/upload flow remains unchanged.
        if (!validateBeforeSubmit()) {
            return;
        }

        if (isUploading) {
            Toast.makeText(
                    this,
                    "Upload is already in progress. Please wait.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (spPaymentMethod.getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Please select payment method",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String transactionId =
                etTransactionId
                        .getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(transactionId)) {

            etTransactionId.setError(
                    "Transaction ID is required"
            );

            return;
        }

        if (!cbDeclaration.isChecked()) {

            Toast.makeText(
                    this,
                    "Please accept the declaration",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (photoUri == null ||
                signatureUri == null ||
                nidDocumentUri == null) {

            Toast.makeText(
                    this,
                    "Please select all required documents first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String quota =
                spQuota.getSelectedItem().toString();

        if (!quota.equals("General") &&
                quotaDocumentUri == null) {

            Toast.makeText(
                    this,
                    "Quota supporting document is required.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Make sure an Application ID exists before upload.
        if (TextUtils.isEmpty(applicationId)) {
            applicationId = generateApplicationId();
        }

        isUploading = true;
        setSubmitButtonLoading(true);

        uploadAllDocuments();
    }

    // =========================================================
    // CLOUDINARY UPLOAD
    // =========================================================

    private void uploadAllDocuments() {

        uploadCloudinaryFile(
                photoUri,
                true,
                "Applicant Photo",
                url -> {
                    photoUrl = url;

                    uploadCloudinaryFile(
                            signatureUri,
                            true,
                            "Signature",
                            signatureDownloadUrl -> {
                                signatureUrl = signatureDownloadUrl;

                                uploadCloudinaryFile(
                                        nidDocumentUri,
                                        false,
                                        "NID / Birth Registration Document",
                                        nidDownloadUrl -> {
                                            nidDocumentUrl = nidDownloadUrl;

                                            if (spQuota.getSelectedItemPosition() != 1) {
                                                uploadCloudinaryFile(
                                                        quotaDocumentUri,
                                                        false,
                                                        "Quota Supporting Document",
                                                        quotaDownloadUrl -> {
                                                            quotaDocumentUrl = quotaDownloadUrl;
                                                            saveApplicationToFirestore();
                                                        }
                                                );
                                            } else {
                                                quotaDocumentUrl = "";
                                                saveApplicationToFirestore();
                                            }
                                        }
                                );
                            }
                    );
                }
        );
    }

    private interface CloudinaryUploadSuccessCallback {
        void onSuccess(String secureUrl);
    }

    private void uploadCloudinaryFile(
            Uri fileUri,
            boolean isImage,
            String fileLabel,
            CloudinaryUploadSuccessCallback callback) {

        if (fileUri == null) {
            handleUploadFailure(fileLabel + " is missing.");
            return;
        }

        updateUploadStatus(fileLabel + " uploading...");

        cloudinaryUploader.upload(
                fileUri,
                isImage,
                new CloudinaryUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String secureUrl) {
                        updateUploadStatus(
                                fileLabel + " uploaded successfully."
                        );
                        callback.onSuccess(secureUrl);
                    }

                    @Override
                    public void onError(String message) {
                        handleUploadFailure(
                                "Failed to upload " + fileLabel + ": " + message
                        );
                    }
                }
        );
    }

    private void updateUploadStatus(String message) {

        Button button =
                findViewById(R.id.btnSubmitApplication);

        if (button != null) {
            button.setText(message);
        }
    }

    private void handleUploadFailure(String message) {

        isUploading = false;
        setSubmitButtonLoading(false);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void setSubmitButtonLoading(boolean loading) {

        Button button =
                findViewById(R.id.btnSubmitApplication);

        if (button == null) {
            return;
        }

        button.setEnabled(!loading);

        if (loading) {
            button.setText("Uploading documents...");
        } else {
            button.setText("⊙ Submit Application");
        }
    }

    // =========================================================
    // SAVE APPLICATION AFTER CLOUDINARY UPLOAD
    // =========================================================

    private void saveApplicationToFirestore() {

        String uid = user.getUid();

        DocumentReference applicationRef =
                db.collection("applications")
                        .document(uid);

        Map<String, Object> application =
                new HashMap<>();

        application.put(
                "applicationId",
                applicationId
        );

        application.put(
                "userId",
                uid
        );

        application.put(
                "fullName",
                etFullName.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "fatherName",
                etFatherName.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "motherName",
                etMotherName.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "phone",
                etPhone.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "email",
                etEmail.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "dateOfBirth",
                etDateOfBirth.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "gender",
                spGender.getSelectedItem()
                        .toString()
        );

        application.put(
                "nidNumber",
                etNidNumber.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "sscGpa",
                etSscGpa.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "diplomaCgpa",
                etDiplomaCgpa.getText()
                        .toString()
                        .trim()
        );

        application.put(
                "passingYear",
                spPassingYear.getSelectedItem()
                        .toString()
        );

        application.put(
                "diplomaTechnology",
                spDiplomaTechnology
                        .getSelectedItem()
                        .toString()
        );

        application.put(
                "department",
                spDepartment
                        .getSelectedItem()
                        .toString()
        );

        application.put(
                "quota",
                spQuota.getSelectedItem()
                        .toString()
        );

        application.put(
                "paymentMethod",
                spPaymentMethod
                        .getSelectedItem()
                        .toString()
        );

        application.put(
                "transactionId",
                etTransactionId
                        .getText()
                        .toString()
                        .trim()
        );

        // Student submission workflow:
        // New application starts as SUBMITTED and payment remains PENDING.
        // Admin approval will later change these to APPROVED and PAID.
        application.put(
                "status",
                "SUBMITTED"
        );

        application.put(
                "paymentStatus",
                "PENDING"
        );

        application.put(
                "amount",
                1200
        );

        application.put(
                "submittedAt",
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        // NEW: Cloudinary secure URLs
        application.put(
                "photoUrl",
                photoUrl
        );

        application.put(
                "signatureUrl",
                signatureUrl
        );

        application.put(
                "nidDocumentUrl",
                nidDocumentUrl
        );

        application.put(
                "quotaDocumentUrl",
                quotaDocumentUrl
        );

        // Keep old URI fields for backward compatibility
        // with previously submitted records.
        application.put(
                "photoUri",
                photoUri != null
                        ? photoUri.toString()
                        : ""
        );

        application.put(
                "signatureUri",
                signatureUri != null
                        ? signatureUri.toString()
                        : ""
        );

        application.put(
                "nidDocumentUri",
                nidDocumentUri != null
                        ? nidDocumentUri.toString()
                        : ""
        );

        application.put(
                "quotaDocumentUri",
                quotaDocumentUri != null
                        ? quotaDocumentUri.toString()
                        : ""
        );

        applicationRef
                .set(
                        application,
                        SetOptions.merge()
                )
                .addOnSuccessListener(unused -> {

                    isUploading = false;
                    setSubmitButtonLoading(false);

                    // Clear local draft only after Firestore submission succeeds.
                    clearDraft();

                    Toast.makeText(
                            AdmissionApplicationActivity.this,
                            "Application submitted successfully\nApplication ID: "
                                    + applicationId,
                            Toast.LENGTH_LONG
                    ).show();

                    Intent intent =
                            new Intent(
                                    AdmissionApplicationActivity.this,
                                    ApplicationTrackerActivity.class
                            );

                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e -> {

                    isUploading = false;
                    setSubmitButtonLoading(false);

                    Toast.makeText(
                            AdmissionApplicationActivity.this,
                            "Documents uploaded, but application could not be saved: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // SHOW STEP
    // =========================================================

    private void showStep(int step) {

        currentStep = step;

        layoutStep1.setVisibility(
                step == 1
                        ? View.VISIBLE
                        : View.GONE
        );

        layoutStep2.setVisibility(
                step == 2
                        ? View.VISIBLE
                        : View.GONE
        );

        layoutStep3.setVisibility(
                step == 3
                        ? View.VISIBLE
                        : View.GONE
        );

        layoutStep4.setVisibility(
                step == 4
                        ? View.VISIBLE
                        : View.GONE
        );

        layoutStep5.setVisibility(
                step == 5
                        ? View.VISIBLE
                        : View.GONE
        );

        updateStepIndicator(step);

        findViewById(R.id.scrollView)
                .post(() ->
                        findViewById(R.id.scrollView)
                                .scrollTo(0, 0)
                );
    }

    // =========================================================
    // STEP INDICATOR
    // =========================================================

    private void updateStepIndicator(
            int activeStep) {

        int[] numberIds = {

                R.id.step1Number,
                R.id.step2Number,
                R.id.step3Number,
                R.id.step4Number,
                R.id.step5Number
        };

        int[] textIds = {

                R.id.step1Text,
                R.id.step2Text,
                R.id.step3Text,
                R.id.step4Text,
                R.id.step5Text
        };

        for (int i = 0; i < 5; i++) {

            TextView number =
                    findViewById(
                            numberIds[i]
                    );

            TextView text =
                    findViewById(
                            textIds[i]
                    );

            int step = i + 1;

            if (step < activeStep) {

                number.setBackgroundResource(
                        R.drawable.bg_step_completed
                );

                number.setTextColor(
                        android.graphics.Color.WHITE
                );

                text.setTextColor(
                        android.graphics.Color.rgb(
                                22,
                                138,
                                91
                        )
                );

                number.setText("✓");

            } else if (step == activeStep) {

                number.setBackgroundResource(
                        R.drawable.bg_step_active
                );

                number.setTextColor(
                        android.graphics.Color.WHITE
                );

                text.setTextColor(
                        android.graphics.Color.rgb(
                                23,
                                105,
                                255
                        )
                );

                number.setText(
                        String.valueOf(step)
                );

            } else {

                number.setBackgroundResource(
                        R.drawable.bg_step_inactive
                );

                number.setTextColor(
                        android.graphics.Color.rgb(
                                100,
                                116,
                                139
                        )
                );

                text.setTextColor(
                        android.graphics.Color.rgb(
                                71,
                                85,
                                105
                        )
                );

                number.setText(
                        String.valueOf(step)
                );
            }
        }
    }

    // =========================================================
    // DATE PICKER
    // =========================================================

    private void showDatePicker() {

        Calendar calendar =
                Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view,
                         year,
                         month,
                         dayOfMonth) -> {

                            String date =
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d/%02d/%04d",
                                            dayOfMonth,
                                            month + 1,
                                            year
                                    );

                            etDateOfBirth.setText(
                                    date
                            );
                        },
                        calendar.get(
                                Calendar.YEAR
                        ),
                        calendar.get(
                                Calendar.MONTH
                        ),
                        calendar.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        dialog.show();
    }

    // =========================================================
    // FILE PICKER
    // =========================================================

    private void chooseFile(
            int requestCode,
            boolean document) {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        // Keep the existing selection behavior.
        // ACTION_OPEN_DOCUMENT allows persistent URI permission.
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        if (document) {

            intent.setType("*/*");

        } else {

            intent.setType("image/*");
        }

        startActivityForResult(
                intent,
                requestCode
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

        if (resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {

            return;
        }

        Uri uri = data.getData();

        // =====================================================
        // IMAGE SIZE LIMIT ONLY
        // Applicant Photo and Signature must be <= 100 KB.
        // No other application functionality is changed.
        // =====================================================
        if (requestCode == PICK_PHOTO
                || requestCode == PICK_SIGNATURE) {

            long fileSize = getFileSize(uri);

            if (fileSize <= 0) {
                Toast.makeText(
                        this,
                        "Unable to determine image file size.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            if (fileSize > MAX_IMAGE_SIZE) {
                Toast.makeText(
                        this,
                        "Image size must be maximum 100 KB.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
        }

        // DOCUMENT SIZE LIMIT: maximum 3 MB.
        // Existing document picker and upload flow are unchanged.
        if (requestCode == PICK_NID
                || requestCode == PICK_QUOTA) {

            long fileSize = getFileSize(uri);

            if (fileSize <= 0) {
                Toast.makeText(
                        this,
                        "Unable to determine document file size.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            if (fileSize > MAX_DOCUMENT_SIZE) {
                Toast.makeText(
                        this,
                        "PDF/document size must be maximum 3 MB.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
        }

        // Keep access to files selected from the system picker
        // so Cloudinary can read them during upload.
        try {
            final int takeFlags =
                    data.getFlags()
                            & Intent.FLAG_GRANT_READ_URI_PERMISSION;

            getContentResolver().takePersistableUriPermission(
                    uri,
                    takeFlags
            );
        } catch (Exception ignored) {
            // Some document providers do not support
            // persistable permissions. Upload still proceeds
            // while the URI remains available.
        }

        if (requestCode == PICK_PHOTO) {

            photoUri = uri;

            tvPhoto.setText(
                    getFileName(uri)
            );

        } else if (requestCode == PICK_SIGNATURE) {

            signatureUri = uri;

            tvSignature.setText(
                    getFileName(uri)
            );

        } else if (requestCode == PICK_NID) {

            nidDocumentUri = uri;

            tvNidDocument.setText(
                    getFileName(uri)
            );

        } else if (requestCode == PICK_QUOTA) {

            quotaDocumentUri = uri;

            tvQuotaDocument.setText(
                    getFileName(uri)
            );
        }
    }


    // Returns selected file size in bytes.
    // Used only for the 100 KB image validation.
    private long getFileSize(Uri uri) {

        android.database.Cursor cursor = null;

        try {
            cursor = getContentResolver().query(
                    uri,
                    new String[]{
                            android.provider.OpenableColumns.SIZE
                    },
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {

                int sizeIndex =
                        cursor.getColumnIndex(
                                android.provider.OpenableColumns.SIZE
                        );

                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }

        } catch (Exception ignored) {

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return -1;
    }

    private String getFileName(Uri uri) {

        String path = uri.getPath();

        if (path == null) {
            return "Selected file";
        }

        int index =
                path.lastIndexOf('/');

        if (index >= 0
                && index < path.length() - 1) {

            return path.substring(
                    index + 1
            );
        }

        return path;
    }

    // =========================================================
    // DRAWER NAVIGATION
    // =========================================================

    private void handleDrawerNavigation(
            MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {

            startActivity(
                    new Intent(
                            this,
                            MainActivity.class
                    )
            );

        } else if (id == R.id.nav_eligibility) {

            startActivity(
                    new Intent(
                            this,
                            EligibilityActivity.class
                    )
            );

        } else if (id == R.id.nav_notice) {

            startActivity(
                    new Intent(
                            this,
                            NoticeActivity.class
                    )
            );

        } else if (id == R.id.nav_department) {

            startActivity(
                    new Intent(
                            this,
                            DepartmentsActivity.class
                    )
            );

        } else if (id == R.id.nav_tracker) {

            startActivity(
                    new Intent(
                            this,
                            ApplicationTrackerActivity.class
                    )
            );

        } else if (id == R.id.nav_admit_card) {

            startActivity(
                    new Intent(
                            this,
                            AdmitCardActivity.class
                    )
            );

        } else if (id == R.id.nav_result) {

            startActivity(
                    new Intent(
                            this,
                            ResultActivity.class
                    )
            );

        } else if (id == R.id.nav_syllabus) {

            startActivity(
                    new Intent(
                            this,
                            SyllabusActivity.class
                    )
            );
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // ==================== AUTO SAVE ====================

    private void saveDraft() {
        if (draftPrefs == null || isLoadingDraft) return;

        SharedPreferences.Editor e = draftPrefs.edit();

        putText(e, "fullName", etFullName);
        putText(e, "fatherName", etFatherName);
        putText(e, "motherName", etMotherName);
        putText(e, "phone", etPhone);
        putText(e, "email", etEmail);
        putText(e, "dateOfBirth", etDateOfBirth);
        putText(e, "nidNumber", etNidNumber);
        putText(e, "sscGpa", etSscGpa);
        putText(e, "diplomaCgpa", etDiplomaCgpa);

        // Save spinner selections.
        if (spPassingYear != null && spPassingYear.getSelectedItem() != null) {
            e.putString("passingYear",
                    spPassingYear.getSelectedItem().toString());
        }
        if (spDiplomaTechnology != null
                && spDiplomaTechnology.getSelectedItem() != null) {
            e.putString("diplomaTechnology",
                    spDiplomaTechnology.getSelectedItem().toString());
        }
        if (spDepartment != null && spDepartment.getSelectedItem() != null) {
            e.putString("department",
                    spDepartment.getSelectedItem().toString());
        }
        if (spGender != null && spGender.getSelectedItem() != null) {
            e.putString("gender", spGender.getSelectedItem().toString());
        }
        if (spQuota != null && spQuota.getSelectedItem() != null) {
            e.putString("quota", spQuota.getSelectedItem().toString());
        }
        if (spPaymentMethod != null
                && spPaymentMethod.getSelectedItem() != null) {
            e.putString("paymentMethod",
                    spPaymentMethod.getSelectedItem().toString());
        }

        putText(e, "transactionId", etTransactionId);
        e.apply();
    }

    private void putText(SharedPreferences.Editor e, String key, EditText view) {
        if (view != null) {
            e.putString(key, view.getText().toString());
        }
    }

    private void loadDraft() {
        if (draftPrefs == null) return;

        isLoadingDraft = true;

        setTextIfSaved("fullName", etFullName);
        setTextIfSaved("fatherName", etFatherName);
        setTextIfSaved("motherName", etMotherName);
        setTextIfSaved("phone", etPhone);
        setTextIfSaved("email", etEmail);
        setTextIfSaved("dateOfBirth", etDateOfBirth);
        setTextIfSaved("nidNumber", etNidNumber);
        setTextIfSaved("sscGpa", etSscGpa);
        setTextIfSaved("diplomaCgpa", etDiplomaCgpa);
        setTextIfSaved("transactionId", etTransactionId);

        setSpinnerIfSaved("passingYear", spPassingYear);
        setSpinnerIfSaved("diplomaTechnology", spDiplomaTechnology);
        setSpinnerIfSaved("department", spDepartment);
        setSpinnerIfSaved("gender", spGender);
        setSpinnerIfSaved("quota", spQuota);
        setSpinnerIfSaved("paymentMethod", spPaymentMethod);

        isLoadingDraft = false;
    }

    private void setTextIfSaved(String key, EditText view) {
        if (view != null && draftPrefs.contains(key)) {
            view.setText(draftPrefs.getString(key, ""));
        }
    }

    private void setSpinnerIfSaved(String key, Spinner spinner) {
        if (spinner == null || !draftPrefs.contains(key)) return;

        String saved = draftPrefs.getString(key, "");
        android.widget.SpinnerAdapter adapter = spinner.getAdapter();

        if (adapter == null) return;

        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            if (item != null && saved.equals(item.toString())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void clearDraft() {
        if (draftPrefs != null) {
            draftPrefs.edit().clear().apply();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveDraft();
    }

    @Override
    protected void onDestroy() {
        if (cloudinaryUploader != null) {
            cloudinaryUploader.shutdown();
        }
        super.onDestroy();
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(
                GravityCompat.START)) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );

        } else if (currentStep > 1) {

            showStep(
                    currentStep - 1
            );

        } else {

            super.onBackPressed();
        }
    }
}
