package com.example.smartduetadmissionsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvName;
    private TextView tvEmail;
    private TextView tvRole;
    private TextView tvStatus;
    private TextView tvLastLogin;

    private MaterialButton btnEditName;
    private MaterialButton btnEditEmail;
    private MaterialButton btnChangePassword;
    private MaterialButton btnAdminManagement;

    private AppCompatImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupActions();
        loadProfile();
    }

    // =========================================================
    // BIND VIEWS
    // =========================================================

    private void bindViews() {

        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvRole = findViewById(R.id.tvProfileRole);
        tvStatus = findViewById(R.id.tvProfileStatus);
        tvLastLogin = findViewById(R.id.tvProfileLastLogin);

        btnEditName = findViewById(R.id.btnEditName);
        btnEditEmail = findViewById(R.id.btnEditEmail);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnAdminManagement = findViewById(R.id.btnAdminManagement);

        btnBack = findViewById(R.id.btnBack);
    }

    // =========================================================
    // ACTIONS
    // =========================================================

    private void setupActions() {

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnEditName != null) {
            btnEditName.setOnClickListener(
                    v -> showEditNameDialog()
            );
        }

        if (btnEditEmail != null) {
            btnEditEmail.setOnClickListener(
                    v -> showEditEmailDialog()
            );
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(
                    v -> showChangePasswordDialog()
            );
        }

        // =====================================================
        // ADMIN MANAGEMENT
        // =====================================================

        if (btnAdminManagement != null) {
            btnAdminManagement.setOnClickListener(v -> {

                Intent intent = new Intent(
                        AdminProfileActivity.this,
                        AdminManagementActivity.class
                );

                startActivity(intent);
            });
        }
    }

    // =========================================================
    // LOAD PROFILE
    // =========================================================

    private void loadProfile() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login as admin.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // Firebase Authentication Email
        String email = user.getEmail();

        tvEmail.setText(
                email == null || email.trim().isEmpty()
                        ? "Not available"
                        : email
        );

        // Last Login
        long lastLogin = user.getMetadata() == null
                ? 0
                : user.getMetadata().getLastSignInTimestamp();

        tvLastLogin.setText(
                lastLogin > 0
                        ? DateFormat.format(
                        "dd MMM yyyy, hh:mm a",
                        lastLogin
                )
                        : "Not available"
        );

        // Firestore Admin Information
        db.collection("admins")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {

                        Toast.makeText(
                                this,
                                "Admin account verification failed.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                        return;
                    }

                    Boolean active = doc.getBoolean("active");

                    String role =
                            safe(doc.getString("role"));

                    String name =
                            safe(doc.getString("name"));

                    if (name.isEmpty()
                            && user.getDisplayName() != null) {

                        name = user.getDisplayName().trim();
                    }

                    if (!Boolean.TRUE.equals(active)
                            || !"admin".equalsIgnoreCase(role)) {

                        Toast.makeText(
                                this,
                                "Admin access is no longer available.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                        return;
                    }

                    tvName.setText(
                            name.isEmpty()
                                    ? "Admin"
                                    : name
                    );

                    tvRole.setText(
                            "admin".equalsIgnoreCase(role)
                                    ? "Administrator"
                                    : role
                    );

                    tvStatus.setText("Active");
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to load Admin Profile.",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                });
    }

    // =========================================================
    // EDIT NAME
    // =========================================================

    private void showEditNameDialog() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) return;

        final EditText input = new EditText(this);

        input.setSingleLine(true);
        input.setHint("Admin Name");
        input.setText(tvName.getText().toString());
        input.setSelectAllOnFocus(true);

        input.setPadding(
                dp(16),
                dp(4),
                dp(16),
                dp(4)
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Edit Profile Name")
                        .setView(input)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Save",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                String name =
                        input.getText()
                                .toString()
                                .trim();

                if (name.isEmpty()) {

                    input.setError(
                            "Name is required"
                    );

                    return;
                }

                if (name.length() < 2) {

                    input.setError(
                            "Enter a valid name"
                    );

                    return;
                }

                db.collection("admins")
                        .document(user.getUid())
                        .update(
                                "name",
                                name,
                                "updatedAt",
                                FieldValue.serverTimestamp()
                        )
                        .addOnSuccessListener(unused -> {

                            tvName.setText(name);

                            dialog.dismiss();

                            Toast.makeText(
                                    this,
                                    "Profile updated successfully.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        })
                        .addOnFailureListener(e ->

                                Toast.makeText(
                                        this,
                                        "Profile update failed: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                        );
            });
        });

        dialog.show();
    }

    // =========================================================
    // EDIT EMAIL
    // =========================================================

    private void showEditEmailDialog() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.getEmail() == null) {

            Toast.makeText(
                    this,
                    "Current email is not available.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        View view = LayoutInflater.from(this)
                .inflate(
                        R.layout.dialog_admin_edit_email,
                        null
                );

        EditText etNewEmail =
                view.findViewById(R.id.etNewEmail);

        EditText etCurrentPassword =
                view.findViewById(
                        R.id.etEmailCurrentPassword
                );

        etNewEmail.setText(user.getEmail());

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Edit Email")
                        .setView(view)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Update Email",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                String newEmail =
                        etNewEmail.getText()
                                .toString()
                                .trim();

                String currentPassword =
                        etCurrentPassword.getText()
                                .toString();

                // Email validation
                if (TextUtils.isEmpty(newEmail)) {

                    etNewEmail.setError(
                            "Enter new email"
                    );

                    etNewEmail.requestFocus();

                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(newEmail)
                        .matches()) {

                    etNewEmail.setError(
                            "Enter a valid email address"
                    );

                    etNewEmail.requestFocus();

                    return;
                }

                // Same email check
                if (newEmail.equalsIgnoreCase(
                        user.getEmail()
                )) {

                    etNewEmail.setError(
                            "This is already your current email"
                    );

                    etNewEmail.requestFocus();

                    return;
                }

                // Password required
                if (TextUtils.isEmpty(currentPassword)) {

                    etCurrentPassword.setError(
                            "Enter current password"
                    );

                    etCurrentPassword.requestFocus();

                    return;
                }

                btnEditEmail.setEnabled(false);

                // =================================================
                // RE-AUTHENTICATION
                // =================================================

                user.reauthenticate(
                                EmailAuthProvider.getCredential(
                                        user.getEmail(),
                                        currentPassword
                                )
                        )
                        .addOnSuccessListener(result -> {

                            // =================================================
                            // UPDATE FIREBASE AUTH EMAIL
                            // =================================================

                            user.updateEmail(newEmail)
                                    .addOnSuccessListener(unused -> {

                                        // =================================================
                                        // UPDATE FIRESTORE ADMIN EMAIL
                                        // =================================================

                                        db.collection("admins")
                                                .document(user.getUid())
                                                .update(
                                                        "email",
                                                        newEmail,
                                                        "updatedAt",
                                                        FieldValue.serverTimestamp()
                                                )
                                                .addOnSuccessListener(
                                                        firestoreUnused -> {

                                                            tvEmail.setText(
                                                                    newEmail
                                                            );

                                                            btnEditEmail
                                                                    .setEnabled(true);

                                                            dialog.dismiss();

                                                            Toast.makeText(
                                                                    this,
                                                                    "Email updated successfully.",
                                                                    Toast.LENGTH_LONG
                                                            ).show();
                                                        }
                                                )
                                                .addOnFailureListener(
                                                        firestoreError -> {

                                                            btnEditEmail
                                                                    .setEnabled(true);

                                                            Toast.makeText(
                                                                    this,
                                                                    "Firebase email changed, "
                                                                            + "but admin profile update failed: "
                                                                            + firestoreError.getMessage(),
                                                                    Toast.LENGTH_LONG
                                                            ).show();
                                                        }
                                                );
                                    })
                                    .addOnFailureListener(e -> {

                                        btnEditEmail.setEnabled(true);

                                        Toast.makeText(
                                                this,
                                                "Email update failed: "
                                                        + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    });
                        })
                        .addOnFailureListener(e -> {

                            btnEditEmail.setEnabled(true);

                            etCurrentPassword.setError(
                                    "Current password is incorrect"
                            );

                            etCurrentPassword.requestFocus();
                        });
            });
        });

        dialog.show();
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    private void showChangePasswordDialog() {

        View view = LayoutInflater.from(this)
                .inflate(
                        R.layout.dialog_admin_change_password,
                        null
                );

        EditText etCurrent =
                view.findViewById(
                        R.id.etCurrentPassword
                );

        EditText etNew =
                view.findViewById(
                        R.id.etNewPassword
                );

        EditText etConfirm =
                view.findViewById(
                        R.id.etConfirmPassword
                );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Change Password")
                        .setView(view)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Update Password",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                String current =
                        etCurrent.getText().toString();

                String newPassword =
                        etNew.getText().toString();

                String confirm =
                        etConfirm.getText().toString();

                if (current.isEmpty()) {

                    etCurrent.setError(
                            "Enter current password"
                    );

                    return;
                }

                if (newPassword.length() < 6) {

                    etNew.setError(
                            "Password must be at least 6 characters"
                    );

                    return;
                }

                if (!newPassword.equals(confirm)) {

                    etConfirm.setError(
                            "Passwords do not match"
                    );

                    return;
                }

                FirebaseUser user =
                        auth.getCurrentUser();

                if (user == null
                        || user.getEmail() == null) {

                    return;
                }

                btnChangePassword.setEnabled(false);

                user.reauthenticate(
                                EmailAuthProvider.getCredential(
                                        user.getEmail(),
                                        current
                                )
                        )
                        .addOnSuccessListener(result ->

                                user.updatePassword(
                                                newPassword
                                        )
                                        .addOnSuccessListener(unused -> {

                                            btnChangePassword
                                                    .setEnabled(true);

                                            dialog.dismiss();

                                            Toast.makeText(
                                                    this,
                                                    "Password updated successfully.",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        })
                                        .addOnFailureListener(e -> {

                                            btnChangePassword
                                                    .setEnabled(true);

                                            Toast.makeText(
                                                    this,
                                                    "Password update failed: "
                                                            + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        })
                        )
                        .addOnFailureListener(e -> {

                            btnChangePassword.setEnabled(true);

                            etCurrent.setError(
                                    "Current password is incorrect"
                            );

                            etCurrent.requestFocus();
                        });
            });
        });

        dialog.show();
    }

    // =========================================================
    // DP HELPER
    // =========================================================

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private static String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}