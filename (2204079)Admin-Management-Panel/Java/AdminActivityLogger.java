package com.example.smartduetadmissionsystem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminActivityLogger {

    public static void log(
            String action,
            String applicationId,
            String description
    ) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        String uid = user.getUid();
        String email = user.getEmail();

        FirebaseFirestore db =
                FirebaseFirestore.getInstance();

        Map<String, Object> logData =
                new HashMap<>();

        logData.put("adminUid", uid);
        logData.put(
                "adminEmail",
                email != null ? email : ""
        );
        logData.put(
                "action",
                action != null
                        ? action
                        : "Admin Activity"
        );
        logData.put(
                "applicationId",
                applicationId != null
                        ? applicationId
                        : ""
        );
        logData.put(
                "description",
                description != null
                        ? description
                        : ""
        );

        // Authoritative Firestore timestamp.
        logData.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        // Immediate realtime UI fallback.
        logData.put(
                "clientTimestamp",
                System.currentTimeMillis()
        );

        db.collection("admins")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    String adminName =
                            document.getString("name");

                    logData.put(
                            "adminName",
                            adminName != null
                                    && !adminName.trim().isEmpty()
                                    ? adminName
                                    : "Admin"
                    );

                    saveLog(db, logData);
                })
                .addOnFailureListener(e -> {

                    logData.put(
                            "adminName",
                            "Admin"
                    );

                    saveLog(db, logData);
                });
    }

    public static void log(
            String action,
            String description
    ) {
        log(action, "", description);
    }

    private static void saveLog(
            FirebaseFirestore db,
            Map<String, Object> logData
    ) {
        db.collection("admin_activity_logs")
                .add(logData)
                .addOnFailureListener(e -> {
                    // Activity logging is non-blocking.
                });
    }
}
