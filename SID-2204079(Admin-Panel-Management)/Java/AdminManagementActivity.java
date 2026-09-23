package com.example.smartduetadmissionsystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.List;

public class AdminManagementActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;

    private EditText etAdminEmail;
    private MaterialButton btnGrantAdmin;
    private View progressBar;

    private RecyclerView recyclerAdmins;

    private final List<AdminModel> adminList = new ArrayList<>();
    private AdminManagementAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_management);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();

        initViews();
        setupRecyclerView();
        setupActions();
        loadAdmins();
    }

    private void initViews() {

        etAdminEmail = findViewById(R.id.etAdminEmail);
        btnGrantAdmin = findViewById(R.id.btnGrantAdmin);
        progressBar = findViewById(R.id.progressBar);
        recyclerAdmins = findViewById(R.id.recyclerAdmins);
    }

    private void setupRecyclerView() {

        adapter = new AdminManagementAdapter(
                this,
                adminList,
                this::confirmDisableAdmin
        );

        recyclerAdmins.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerAdmins.setAdapter(adapter);
    }

    private void setupActions() {

        View btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnGrantAdmin.setOnClickListener(
                v -> grantAdminAccess()
        );
    }

    // =========================================================
    // GRANT ADMIN ACCESS
    // =========================================================

    private void grantAdminAccess() {

        FirebaseUser currentUser =
                auth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Admin login required.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String email =
                etAdminEmail.getText()
                        .toString()
                        .trim()
                        .toLowerCase();

        if (TextUtils.isEmpty(email)) {

            etAdminEmail.setError(
                    "Enter admin Gmail"
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

        setLoading(true);

        /*
         * IMPORTANT:
         *
         * The Cloud Function checks:
         * 1. Caller is authenticated
         * 2. Caller is active admin
         * 3. Target email belongs to an existing
         *    Firebase Authentication account
         */

        java.util.Map<String, Object> data =
                new java.util.HashMap<>();

        data.put("email", email);

        functions
                .getHttpsCallable("grantAdminAccess")
                .call(data)
                .addOnSuccessListener(result -> {

                    setLoading(false);

                    etAdminEmail.setText("");

                    Toast.makeText(
                            this,
                            "Admin access granted successfully.",
                            Toast.LENGTH_LONG
                    ).show();

                    loadAdmins();
                })
                .addOnFailureListener(e -> {

                    setLoading(false);

                    String message =
                            e.getMessage();

                    if (message == null
                            || message.trim().isEmpty()) {

                        message =
                                "Unable to grant admin access.";
                    }

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // LOAD ADMINS
    // =========================================================

    private void loadAdmins() {

        db.collection("admins")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    adminList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        AdminModel model =
                                new AdminModel();

                        model.uid = doc.getId();
                        model.name =
                                safe(doc.getString("name"));

                        model.email =
                                safe(doc.getString("email"));

                        model.role =
                                safe(doc.getString("role"));

                        Boolean active =
                                doc.getBoolean("active");

                        model.active =
                                Boolean.TRUE.equals(active);

                        adminList.add(model);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to load admins.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================================================
    // DISABLE ADMIN
    // =========================================================

    private void confirmDisableAdmin(
            AdminModel admin
    ) {

        FirebaseUser currentUser =
                auth.getCurrentUser();

        if (currentUser != null
                && currentUser.getUid()
                .equals(admin.uid)) {

            Toast.makeText(
                    this,
                    "You cannot disable your own admin access.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Disable Admin")
                .setMessage(
                        "Disable admin access for\n"
                                + admin.email
                                + "?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Disable",
                        (dialog, which) ->
                                disableAdmin(admin)
                )
                .show();
    }

    private void disableAdmin(
            AdminModel admin
    ) {

        db.collection("admins")
                .document(admin.uid)
                .update(
                        "active",
                        false
                )
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Admin access disabled.",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadAdmins();
                })
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                "Unable to disable admin.",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void setLoading(boolean loading) {

        btnGrantAdmin.setEnabled(!loading);

        if (progressBar != null) {
            progressBar.setVisibility(
                    loading
                            ? View.VISIBLE
                            : View.GONE
            );
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // =========================================================
    // MODEL
    // =========================================================

    public static class AdminModel {

        String uid;
        String name;
        String email;
        String role;
        boolean active;
    }
}