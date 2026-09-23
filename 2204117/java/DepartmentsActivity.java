package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class DepartmentsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;
    private ImageButton btnMenu;
    private MaterialCardView btnProfile;
    private FrameLayout departmentWebViewContainer;
    private WebView departmentWebView;
    private View webViewProgress;
    private ImageButton btnCloseWebView;

    private TextInputEditText etSearchDepartment;
    private AutoCompleteTextView actFaculty;

    // Department Cards
    private MaterialCardView cardArchitecture;
    private MaterialCardView cardCe;
    private MaterialCardView cardChe;
    private MaterialCardView cardCse;
    private MaterialCardView cardEee;
    private MaterialCardView cardFe;
    private MaterialCardView cardIpe;
    private MaterialCardView cardMe;
    private MaterialCardView cardMme;
    private MaterialCardView cardTe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_departments);

        initViews();
        setupHeaderAndDrawer();
        setupDepartmentWebView();
        setupBottomNavigation();
        setupFacultyDropdown();
        setupSearch();
        setupFacultyFilter();
        setupViewDetailsButtons();

        filterDepartments();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);
        departmentWebViewContainer = findViewById(R.id.departmentWebViewContainer);
        departmentWebView = findViewById(R.id.departmentWebView);
        webViewProgress = findViewById(R.id.webViewProgress);
        btnCloseWebView = findViewById(R.id.btnCloseWebView);

        etSearchDepartment = findViewById(R.id.etSearchDepartment);
        actFaculty = findViewById(R.id.actFaculty);

        cardArchitecture = findViewById(R.id.cardArchitecture);
        cardCe = findViewById(R.id.cardCe);
        cardChe = findViewById(R.id.cardChe);
        cardCse = findViewById(R.id.cardCse);
        cardEee = findViewById(R.id.cardEee);
        cardFe = findViewById(R.id.cardFe);
        cardIpe = findViewById(R.id.cardIpe);
        cardMe = findViewById(R.id.cardMe);
        cardMme = findViewById(R.id.cardMme);
        cardTe = findViewById(R.id.cardTe);
    }

    private void setupHeaderAndDrawer() {

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {

                if (drawerLayout != null) {
                    drawerLayout.openDrawer(
                            GravityCompat.START
                    );
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                    startActivity(
                            new Intent(
                                    DepartmentsActivity.this,
                                    ProfileActivity.class
                            )
                    )
            );
        }

        if (navigationView != null) {

            navigationView.setNavigationItemSelectedListener(item -> {

                int id = item.getItemId();

                if (id == R.id.nav_dashboard) {

                    openActivity(
                            MainActivity.class
                    );

                } else if (id == R.id.nav_eligibility) {

                    openActivity(
                            EligibilityActivity.class
                    );

                } else if (id == R.id.nav_notice) {

                    openActivity(
                            NoticeActivity.class
                    );

                } else if (id == R.id.nav_department) {

                    closeDrawer();

                } else if (id == R.id.nav_apply) {

                    openActivity(
                            AdmissionApplicationActivity.class
                    );

                } else if (id == R.id.nav_tracker) {

                    openActivity(
                            ApplicationTrackerActivity.class
                    );

                    // =============================================
                    // FIXED: ADMIT CARD
                    // =============================================

                } else if (id == R.id.nav_admit_card) {

                    openActivity(
                            AdmitCardActivity.class
                    );

                    // =============================================
                    // FIXED: VIEW RESULT
                    // =============================================

                } else if (id == R.id.nav_result) {

                    openActivity(
                            ResultActivity.class
                    );

                    // =============================================
                    // FIXED: SYLLABUS
                    // =============================================

                } else if (id == R.id.nav_syllabus) {

                    openActivity(
                            SyllabusActivity.class
                    );

                } else {

                    closeDrawer();
                }

                return true;
            });
        }
    }

    private void setupBottomNavigation() {

        if (bottomNavigation == null) {
            return;
        }

        bottomNavigation.setSelectedItemId(
                R.id.nav_home
        );

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {

                openActivity(
                        MainActivity.class
                );

                return true;

            } else if (id == R.id.nav_notice) {

                openActivity(
                        NoticeActivity.class
                );

                return true;

            } else if (id == R.id.nav_profile) {

                openActivity(
                        ProfileActivity.class
                );

                return true;
            }

            return false;
        });
    }

    private void openActivity(
            Class<?> target
    ) {

        closeDrawer();

        Intent intent =
                new Intent(
                        this,
                        target
                );

        startActivity(intent);
    }

    private void closeDrawer() {

        if (drawerLayout != null
                && drawerLayout.isDrawerOpen(
                GravityCompat.START
        )) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );
        }
    }

    @Override
    public void onBackPressed() {

        if (drawerLayout != null
                && drawerLayout.isDrawerOpen(
                GravityCompat.START
        )) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );

            return;
        }

        if (departmentWebViewContainer != null
                && departmentWebViewContainer.getVisibility()
                == View.VISIBLE) {

            if (departmentWebView != null
                    && departmentWebView.canGoBack()) {

                departmentWebView.goBack();

            } else {

                closeDepartmentWebView();
            }

            return;
        }

        super.onBackPressed();
    }

    private void setupFacultyDropdown() {

        if (actFaculty == null) {
            return;
        }

        String[] faculties = {
                "All Faculties",
                "Faculty of Civil Engineering",
                "Faculty of Electrical & Electronic Engineering",
                "Faculty of Mechanical Engineering"
        };

        ArrayAdapter<String> facultyAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        faculties
                );

        actFaculty.setAdapter(
                facultyAdapter
        );

        actFaculty.setText(
                "All Faculties",
                false
        );
    }

    private void setupSearch() {

        if (etSearchDepartment == null) {
            return;
        }

        etSearchDepartment.addTextChangedListener(
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

                        filterDepartments();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    private void setupFacultyFilter() {

        if (actFaculty == null) {
            return;
        }

        actFaculty.setOnItemClickListener(
                (parent, view, position, id) ->
                        filterDepartments()
        );
    }

    private void filterDepartments() {

        String query = "";

        if (etSearchDepartment != null
                && etSearchDepartment.getText() != null) {

            query =
                    etSearchDepartment
                            .getText()
                            .toString()
                            .trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );
        }

        String selectedFaculty =
                "All Faculties";

        if (actFaculty != null
                && actFaculty.getText() != null) {

            String faculty =
                    actFaculty
                            .getText()
                            .toString()
                            .trim();

            if (!faculty.isEmpty()) {
                selectedFaculty = faculty;
            }
        }

        boolean showArchitecture =
                matchesDepartment(
                        "Architecture",
                        "Arch",
                        query
                )
                        && matchesFaculty(
                        "CE",
                        selectedFaculty
                );

        boolean showCe =
                matchesDepartment(
                        "Civil Engineering",
                        "CE",
                        query
                )
                        && matchesFaculty(
                        "CE",
                        selectedFaculty
                );

        boolean showChe =
                matchesDepartment(
                        "Chemical Engineering",
                        "ChE",
                        query
                )
                        && matchesFaculty(
                        "CE",
                        selectedFaculty
                );

        boolean showCse =
                matchesDepartment(
                        "Computer Science & Engineering",
                        "CSE",
                        query
                )
                        && matchesFaculty(
                        "EEE",
                        selectedFaculty
                );

        boolean showEee =
                matchesDepartment(
                        "Electrical & Electronic Engineering",
                        "EEE",
                        query
                )
                        && matchesFaculty(
                        "EEE",
                        selectedFaculty
                );

        boolean showFe =
                matchesDepartment(
                        "Food Engineering",
                        "FE",
                        query
                )
                        && matchesFaculty(
                        "ME",
                        selectedFaculty
                );

        boolean showIpe =
                matchesDepartment(
                        "Industrial & Production Engineering",
                        "IPE",
                        query
                )
                        && matchesFaculty(
                        "ME",
                        selectedFaculty
                );

        boolean showMe =
                matchesDepartment(
                        "Mechanical Engineering",
                        "ME",
                        query
                )
                        && matchesFaculty(
                        "ME",
                        selectedFaculty
                );

        boolean showMme =
                matchesDepartment(
                        "Materials & Metallurgical Engineering",
                        "MME",
                        query
                )
                        && matchesFaculty(
                        "ME",
                        selectedFaculty
                );

        boolean showTe =
                matchesDepartment(
                        "Textile Engineering",
                        "TE",
                        query
                )
                        && matchesFaculty(
                        "ME",
                        selectedFaculty
                );

        setCardVisibility(
                cardArchitecture,
                showArchitecture
        );

        setCardVisibility(
                cardCe,
                showCe
        );

        setCardVisibility(
                cardChe,
                showChe
        );

        setCardVisibility(
                cardCse,
                showCse
        );

        setCardVisibility(
                cardEee,
                showEee
        );

        setCardVisibility(
                cardFe,
                showFe
        );

        setCardVisibility(
                cardIpe,
                showIpe
        );

        setCardVisibility(
                cardMe,
                showMe
        );

        setCardVisibility(
                cardMme,
                showMme
        );

        setCardVisibility(
                cardTe,
                showTe
        );
    }

    private boolean matchesDepartment(
            String departmentName,
            String shortCode,
            String query
    ) {

        if (TextUtils.isEmpty(query)) {
            return true;
        }

        String name =
                departmentName.toLowerCase(
                        Locale.ROOT
                );

        String code =
                shortCode.toLowerCase(
                        Locale.ROOT
                );

        return name.contains(query)
                || code.contains(query);
    }

    private boolean matchesFaculty(
            String departmentFaculty,
            String selectedFaculty
    ) {

        if (TextUtils.isEmpty(selectedFaculty)
                || selectedFaculty.equals(
                "All Faculties"
        )) {

            return true;
        }

        switch (selectedFaculty) {

            case "Faculty of Civil Engineering":

                return departmentFaculty.equals(
                        "CE"
                );

            case "Faculty of Electrical & Electronic Engineering":

                return departmentFaculty.equals(
                        "EEE"
                );

            case "Faculty of Mechanical Engineering":

                return departmentFaculty.equals(
                        "ME"
                );

            default:

                return true;
        }
    }

    private void setCardVisibility(
            MaterialCardView card,
            boolean visible
    ) {

        if (card != null) {

            card.setVisibility(
                    visible
                            ? View.VISIBLE
                            : View.GONE
            );
        }
    }

    // ==========================================
    // VIEW DETAILS BUTTONS
    // ==========================================

    private void setupViewDetailsButtons() {

        setLink(
                "btnArchitectureDetails",
                "https://www.duet.ac.bd/department/arch"
        );

        setLink(
                "btnCEDetails",
                "https://www.duet.ac.bd/department/ce"
        );

        setLink(
                "btnCheDetails",
                "https://www.duet.ac.bd/department/che"
        );

        setLink(
                "btnCseDetails",
                "https://www.duet.ac.bd/department/cse"
        );

        setLink(
                "btnEeeDetails",
                "https://www.duet.ac.bd/department/eee"
        );

        setLink(
                "btnFeDetails",
                "https://www.duet.ac.bd/department/fe"
        );

        setLink(
                "btnIpeDetails",
                "https://www.duet.ac.bd/department/ipe"
        );

        setLink(
                "btnMeDetails",
                "https://www.duet.ac.bd/department/me"
        );

        setLink(
                "btnMmeDetails",
                "https://www.duet.ac.bd/department/mme"
        );

        setLink(
                "btnTeDetails",
                "https://www.duet.ac.bd/department/te"
        );
    }

    private void setupDepartmentWebView() {

        if (departmentWebView == null) {
            return;
        }

        WebSettings settings =
                departmentWebView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        departmentWebView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request
                    ) {

                        view.loadUrl(
                                request.getUrl()
                                        .toString()
                        );

                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url
                    ) {

                        view.loadUrl(url);

                        return true;
                    }
                }
        );

        departmentWebView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onProgressChanged(
                            WebView view,
                            int newProgress
                    ) {

                        if (webViewProgress != null) {

                            webViewProgress.setVisibility(
                                    newProgress < 100
                                            ? View.VISIBLE
                                            : View.GONE
                            );
                        }
                    }
                }
        );

        if (btnCloseWebView != null) {

            btnCloseWebView.setOnClickListener(
                    v -> closeDepartmentWebView()
            );
        }
    }

    private void openDepartmentWebView(
            String url
    ) {

        if (departmentWebViewContainer == null
                || departmentWebView == null) {

            return;
        }

        departmentWebViewContainer.setVisibility(
                View.VISIBLE
        );

        departmentWebViewContainer.bringToFront();

        if (webViewProgress != null) {

            webViewProgress.setVisibility(
                    View.VISIBLE
            );
        }

        departmentWebView.loadUrl(url);
    }

    private void closeDepartmentWebView() {

        if (departmentWebViewContainer != null) {

            departmentWebViewContainer.setVisibility(
                    View.GONE
            );
        }

        if (departmentWebView != null) {

            departmentWebView.stopLoading();

            departmentWebView.loadUrl(
                    "about:blank"
            );
        }
    }

    private void setLink(
            String buttonName,
            String url
    ) {

        int buttonId =
                getResources().getIdentifier(
                        buttonName,
                        "id",
                        getPackageName()
                );

        if (buttonId == 0) {
            return;
        }

        View button =
                findViewById(buttonId);

        if (button != null) {

            button.setOnClickListener(
                    v -> openDepartmentWebView(url)
            );
        }
    }
}