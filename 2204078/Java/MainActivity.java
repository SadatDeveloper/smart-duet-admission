package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private MaterialCardView btnMenu;
    private MaterialCardView btnProfile;

    private MaterialCardView cardEligibility;
    private MaterialCardView cardDepartments;
    private MaterialCardView cardNotices;
    private MaterialCardView cardMockTest;

    private MaterialCardView cardApplyAdmission;
    private MaterialCardView cardApplicationTracker;
    private MaterialCardView cardDocuments;
    private MaterialCardView cardAdmitCard;
    private MaterialCardView cardExamSchedule;

    private MaterialCardView cardDeadline;
    private MaterialCardView cardResult;
    private MaterialCardView cardFee;
    private MaterialCardView cardProgress;

    private BottomNavigationView bottomNavigation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initializeViews();
        setupMenu();
        setupProfile();
        setupDashboardCards();
        setupDrawer();
        setupBottomNavigation();
    }


    // =====================================================
    // FIND VIEW WITHOUT DIRECT R.ID REFERENCE
    // =====================================================

    private View findView(String idName) {

        int id = getResources().getIdentifier(
                idName,
                "id",
                getPackageName()
        );

        if (id == 0) {
            return null;
        }

        return findViewById(id);
    }


    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private void initializeViews() {

        drawerLayout =
                (DrawerLayout) findView("drawerLayout");

        navigationView =
                (NavigationView) findView("navigationView");

        btnMenu =
                (MaterialCardView) findView("btnMenu");

        btnProfile =
                (MaterialCardView) findView("btnProfile");


        cardEligibility =
                (MaterialCardView) findView("cardEligibility");

        cardDepartments =
                (MaterialCardView) findView("cardDepartments");

        cardNotices =
                (MaterialCardView) findView("cardNotices");

        cardMockTest =
                (MaterialCardView) findView("cardMockTest");


        cardApplyAdmission =
                (MaterialCardView) findView("cardApplyAdmission");

        cardApplicationTracker =
                (MaterialCardView) findView("cardApplicationTracker");

        cardDocuments =
                (MaterialCardView) findView("cardDocuments");

        cardAdmitCard =
                (MaterialCardView) findView("cardAdmitCard");

        cardExamSchedule =
                (MaterialCardView) findView("cardExamSchedule");


        cardDeadline =
                (MaterialCardView) findView("cardDeadline");

        cardResult =
                (MaterialCardView) findView("cardResult");

        cardFee =
                (MaterialCardView) findView("cardFee");

        cardProgress =
                (MaterialCardView) findView("cardProgress");


        bottomNavigation =
                (BottomNavigationView) findView(
                        "bottomNavigation"
                );
    }


    // =====================================================
    // MENU BUTTON
    // =====================================================

    private void setupMenu() {

        if (btnMenu == null) {
            return;
        }

        btnMenu.setOnClickListener(v -> {

            if (drawerLayout != null) {

                drawerLayout.openDrawer(
                        GravityCompat.START
                );
            }
        });
    }


    // =====================================================
    // PROFILE BUTTON
    // =====================================================

    private void setupProfile() {

        if (btnProfile == null) {
            return;
        }

        btnProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });
    }


    // =====================================================
    // DASHBOARD CARDS
    // =====================================================

    private void setupDashboardCards() {

        if (cardEligibility != null) {

            cardEligibility.setOnClickListener(v ->
                    openActivity(
                            EligibilityActivity.class
                    )
            );
        }


        if (cardDepartments != null) {

            cardDepartments.setOnClickListener(v ->
                    openActivity(
                            DepartmentsActivity.class
                    )
            );
        }


        if (cardNotices != null) {

            cardNotices.setOnClickListener(v ->
                    openActivity(
                            NoticeActivity.class
                    )
            );
        }


        if (cardMockTest != null) {

            cardMockTest.setOnClickListener(v ->
                    openActivity(
                            SyllabusActivity.class
                    )
            );
        }


        if (cardApplyAdmission != null) {

            cardApplyAdmission.setOnClickListener(v ->
                    openActivity(
                            AdmissionApplicationActivity.class
                    )
            );
        }


        if (cardApplicationTracker != null) {

            cardApplicationTracker.setOnClickListener(v ->
                    openActivity(
                            ApplicationTrackerActivity.class
                    )
            );
        }


        // =====================================================
        // ADMIT CARD
        // =====================================================

        if (cardAdmitCard != null) {

            cardAdmitCard.setOnClickListener(v ->
                    openActivity(
                            AdmitCardActivity.class
                    )
            );
        }


        // =====================================================
        // VIEW RESULT
        // =====================================================

        if (cardResult != null) {

            cardResult.setOnClickListener(v ->
                    openActivity(
                            ResultActivity.class
                    )
            );
        }
    }


    // =====================================================
    // DRAWER
    // =====================================================

    private void setupDrawer() {

        if (navigationView == null) {
            return;
        }

        navigationView.setNavigationItemSelectedListener(
                item -> {

                    String itemName =
                            getResources()
                                    .getResourceEntryName(
                                            item.getItemId()
                                    );


                    switch (itemName) {

                        case "nav_dashboard":

                            closeDrawer();

                            return true;


                        case "nav_eligibility":

                            openActivity(
                                    EligibilityActivity.class
                            );

                            return true;


                        case "nav_notice":

                            openActivity(
                                    NoticeActivity.class
                            );

                            return true;


                        case "nav_department":

                            openActivity(
                                    DepartmentsActivity.class
                            );

                            return true;


                        case "nav_apply":

                            openActivity(
                                    AdmissionApplicationActivity.class
                            );

                            return true;


                        case "nav_tracker":

                            openActivity(
                                    ApplicationTrackerActivity.class
                            );

                            return true;


                        // =====================================================
                        // ADMIT CARD
                        // =====================================================

                        case "nav_admit_card":

                            openActivity(
                                    AdmitCardActivity.class
                            );

                            return true;


                        // =====================================================
                        // VIEW RESULT
                        // =====================================================

                        case "nav_result":

                            openActivity(
                                    ResultActivity.class
                            );

                            return true;


                        case "nav_syllabus":

                            openActivity(
                                    SyllabusActivity.class
                            );

                            return true;


                        default:

                            return false;
                    }
                }
        );
    }


    // =====================================================
    // OPEN ACTIVITY
    // =====================================================

    private void openActivity(
            Class<?> activityClass
    ) {

        // First close drawer
        closeDrawer();

        // Then open requested Activity
        Intent intent = new Intent(
                MainActivity.this,
                activityClass
        );

        startActivity(intent);
    }


    // =====================================================
    // CLOSE DRAWER
    // =====================================================

    private void closeDrawer() {

        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(
                        GravityCompat.START
                )) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );
        }
    }


    // =====================================================
    // BOTTOM NAVIGATION
    // =====================================================

    private void setupBottomNavigation() {

        if (bottomNavigation == null) {
            return;
        }

        bottomNavigation.setOnItemSelectedListener(
                item -> {

                    String itemName =
                            getResources()
                                    .getResourceEntryName(
                                            item.getItemId()
                                    );


                    switch (itemName) {

                        case "nav_home":

                            return true;


                        case "nav_notice":

                            startActivity(
                                    new Intent(
                                            MainActivity.this,
                                            NoticeActivity.class
                                    )
                            );

                            return true;


                        case "nav_profile":

                            startActivity(
                                    new Intent(
                                            MainActivity.this,
                                            ProfileActivity.class
                                    )
                            );

                            return true;


                        default:

                            return false;
                    }
                }
        );


        // Select Home if it exists
        int homeId = getResources().getIdentifier(
                "nav_home",
                "id",
                getPackageName()
        );

        if (homeId != 0) {

            bottomNavigation.setSelectedItemId(
                    homeId
            );
        }
    }


    // =====================================================
    // BACK BUTTON
    // =====================================================

    @Override
    public void onBackPressed() {

        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(
                        GravityCompat.START
                )) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );

        } else {

            super.onBackPressed();
        }
    }
}