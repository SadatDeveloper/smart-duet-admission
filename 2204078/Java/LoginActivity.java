package com.example.smartduetadmissionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    private MaterialButton btnLogin;
    private MaterialButton btnCreateAccount;

    private TextView tvForgotPassword;
    private TextView tvAdminLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SessionManager sessionManager;


    @Override
    protected void onStart() {
        super.onStart();

        /*
         * Admin session
         * Admin must remain inside Admin area
         * until Admin Logout.
         */
        if (sessionManager != null
                && sessionManager.isAdminSession()
                && mAuth.getCurrentUser() != null) {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            AdminDashboardActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();

            return;
        }


        /*
         * Student session
         */
        if (sessionManager != null
                && sessionManager.isStudentSession()
                && mAuth.getCurrentUser() != null) {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            MainActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);


        mAuth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();

        sessionManager =
                new SessionManager(this);


        // =========================
        // FIND VIEWS
        // =========================

        etEmail =
                findViewById(R.id.etEmail);

        etPassword =
                findViewById(R.id.etPassword);

        btnLogin =
                findViewById(R.id.btnLogin);

        btnCreateAccount =
                findViewById(R.id.btnCreateAccount);

        tvForgotPassword =
                findViewById(R.id.tvForgotPassword);

        tvAdminLogin =
                findViewById(R.id.tvAdminLogin);


        // =========================
        // STUDENT LOGIN
        // =========================

        btnLogin.setOnClickListener(
                v -> loginUser()
        );


        // =========================
        // CREATE ACCOUNT
        // =========================

        btnCreateAccount.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            RegisterActivity.class
                    );

            startActivity(intent);
        });


        // =========================
        // FORGOT PASSWORD
        // =========================

        tvForgotPassword.setOnClickListener(
                v -> resetPassword()
        );


        // =========================
        // ADMIN LOGIN
        // =========================

        tvAdminLogin.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            AdminLoginActivity.class
                    );

            startActivity(intent);
        });
    }


    // =========================================================
    // STUDENT LOGIN
    // =========================================================

    private void loginUser() {

        // =====================================================
        // ADMIN SESSION BLOCK
        // =====================================================

        if (sessionManager.isAdminSession()) {

            Toast.makeText(
                    this,
                    "Admin is currently logged in. Please logout from Admin first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        String email =
                etEmail.getText()
                        .toString()
                        .trim();

        String password =
                etPassword.getText()
                        .toString();


        // =====================================================
        // VALIDATION
        // =====================================================

        if (TextUtils.isEmpty(email)) {

            etEmail.setError(
                    "Enter your email address"
            );

            etEmail.requestFocus();

            return;
        }


        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError(
                    "Enter a valid email address"
            );

            etEmail.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(password)) {

            etPassword.setError(
                    "Enter your password"
            );

            etPassword.requestFocus();

            return;
        }


        if (password.length() < 6) {

            etPassword.setError(
                    "Password must contain at least 6 characters"
            );

            etPassword.requestFocus();

            return;
        }


        // =====================================================
        // LOGIN
        // =====================================================

        btnLogin.setEnabled(false);

        btnLogin.setText(
                "LOGGING IN..."
        );


        mAuth.signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        btnLogin.setEnabled(true);

                        btnLogin.setText(
                                "LOGIN"
                        );

                        String errorMessage =
                                "Login failed. Please check your email and password.";


                        if (task.getException() != null) {

                            errorMessage =
                                    task.getException()
                                            .getMessage();
                        }


                        Toast.makeText(
                                LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }


                    FirebaseUser user =
                            mAuth.getCurrentUser();


                    if (user == null) {

                        resetLoginButton();

                        return;
                    }


                    String uid =
                            user.getUid();


                    // =================================================
                    // CHECK WHETHER THIS ACCOUNT IS ADMIN
                    // =================================================

                    db.collection("admins")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(document -> {

                                if (document.exists()) {

                                    /*
                                     * This account belongs to Admin.
                                     * It must NOT enter Student area.
                                     */

                                    mAuth.signOut();

                                    resetLoginButton();

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "This is an Admin account. Please use Admin Login.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // =================================================
                                // STUDENT LOGIN SUCCESS
                                // =================================================

                                sessionManager
                                        .setAdminSession(false);

                                sessionManager
                                        .setStudentSession(true);


                                Toast.makeText(
                                        LoginActivity.this,
                                        "Login successful!",
                                        Toast.LENGTH_SHORT
                                ).show();


                                Intent intent =
                                        new Intent(
                                                LoginActivity.this,
                                                MainActivity.class
                                        );

                                intent.setFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );

                                startActivity(intent);

                                finish();

                            })
                            .addOnFailureListener(e -> {

                                /*
                                 * Do not allow Student login when
                                 * Admin identity cannot be verified.
                                 */

                                mAuth.signOut();

                                resetLoginButton();

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Unable to verify account. Please try again.",
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                });
    }


    // =========================================================
    // RESET LOGIN BUTTON
    // =========================================================

    private void resetLoginButton() {

        btnLogin.setEnabled(true);

        btnLogin.setText(
                "LOGIN"
        );
    }


    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    private void resetPassword() {

        String email =
                etEmail.getText()
                        .toString()
                        .trim();


        if (TextUtils.isEmpty(email)) {

            etEmail.setError(
                    "Enter your registered email first"
            );

            etEmail.requestFocus();

            return;
        }


        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError(
                    "Enter a valid email address"
            );

            etEmail.requestFocus();

            return;
        }


        tvForgotPassword.setEnabled(false);

        tvForgotPassword.setText(
                "SENDING..."
        );


        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    tvForgotPassword.setEnabled(true);

                    tvForgotPassword.setText(
                            "Forgot Password?"
                    );


                    if (task.isSuccessful()) {

                        Toast.makeText(
                                LoginActivity.this,
                                "Password reset email sent. Check your inbox.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        String errorMessage =
                                "Failed to send reset email.";


                        if (task.getException() != null) {

                            errorMessage =
                                    task.getException()
                                            .getMessage();
                        }


                        Toast.makeText(
                                LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}