package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etAdminEmail;
    private TextInputEditText etAdminPassword;

    private MaterialButton btnAdminLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SessionManager sessionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sessionManager = new SessionManager(this);

        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPassword = findViewById(R.id.etAdminPassword);

        btnAdminLogin = findViewById(R.id.btnAdminLogin);

        btnAdminLogin.setOnClickListener(v -> loginAdmin());
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (sessionManager.isAdminSession()
                && mAuth.getCurrentUser() != null) {

            openAdminDashboard();
        }
    }


    private void loginAdmin() {

        String email =
                etAdminEmail.getText()
                        .toString()
                        .trim();

        String password =
                etAdminPassword.getText()
                        .toString();


        // Email validation

        if (TextUtils.isEmpty(email)) {

            etAdminEmail.setError(
                    "Enter admin email"
            );

            etAdminEmail.requestFocus();

            return;
        }


        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etAdminEmail.setError(
                    "Enter a valid email address"
            );

            etAdminEmail.requestFocus();

            return;
        }


        // Password validation

        if (TextUtils.isEmpty(password)) {

            etAdminPassword.setError(
                    "Enter admin password"
            );

            etAdminPassword.requestFocus();

            return;
        }


        // Loading

        btnAdminLogin.setEnabled(false);

        btnAdminLogin.setText(
                "LOGGING IN..."
        );


        // Clear previous Student session

        sessionManager.clearStudentSession();


        // Firebase supports one active user

        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }


        // Firebase Authentication

        mAuth.signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        resetButton();

                        Toast.makeText(
                                AdminLoginActivity.this,
                                "Invalid admin email or password.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }


                    FirebaseUser user =
                            mAuth.getCurrentUser();


                    if (user == null) {

                        resetButton();

                        Toast.makeText(
                                AdminLoginActivity.this,
                                "Admin login failed.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }


                    String uid =
                            user.getUid();


                    // Verify Admin from Firestore

                    db.collection("admins")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(document -> {


                                // Admin document check

                                if (!document.exists()) {

                                    mAuth.signOut();

                                    sessionManager
                                            .clearAdminSession();

                                    resetButton();

                                    Toast.makeText(
                                            AdminLoginActivity.this,
                                            "This account is not registered as an Admin.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // Active status

                                Boolean active =
                                        document.getBoolean(
                                                "active"
                                        );


                                // Role

                                String role =
                                        document.getString(
                                                "role"
                                        );


                                boolean isActive =
                                        Boolean.TRUE.equals(
                                                active
                                        );


                                boolean isAdmin =
                                        role != null
                                                && role.equalsIgnoreCase(
                                                "admin"
                                        );


                                // Active check

                                if (!isActive) {

                                    mAuth.signOut();

                                    sessionManager
                                            .clearAdminSession();

                                    resetButton();

                                    Toast.makeText(
                                            AdminLoginActivity.this,
                                            "Admin account is inactive.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // Admin role check

                                if (!isAdmin) {

                                    mAuth.signOut();

                                    sessionManager
                                            .clearAdminSession();

                                    resetButton();

                                    Toast.makeText(
                                            AdminLoginActivity.this,
                                            "This account does not have Admin access.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // Login successful

                                sessionManager
                                        .setStudentSession(false);

                                sessionManager
                                        .setAdminSession(true);


                                Toast.makeText(
                                        AdminLoginActivity.this,
                                        "Admin login successful!",
                                        Toast.LENGTH_SHORT
                                ).show();


                                openAdminDashboard();

                            })
                            .addOnFailureListener(e -> {

                                mAuth.signOut();

                                sessionManager
                                        .clearAdminSession();

                                resetButton();

                                Toast.makeText(
                                        AdminLoginActivity.this,
                                        "Unable to verify Admin account.",
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                });
    }


    private void resetButton() {

        btnAdminLogin.setEnabled(true);

        btnAdminLogin.setText(
                "ADMIN LOGIN"
        );
    }


    private void openAdminDashboard() {

        Intent intent =
                new Intent(
                        AdminLoginActivity.this,
                        AdminDashboardActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}