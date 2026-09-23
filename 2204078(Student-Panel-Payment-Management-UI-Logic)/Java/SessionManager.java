package com.example.smartduetadmissionsystem;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME =
            "SmartDUETSession";

    private static final String KEY_ADMIN_SESSION =
            "admin_session";

    private static final String KEY_STUDENT_SESSION =
            "student_session";


    private final SharedPreferences preferences;


    public SessionManager(Context context) {

        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }


    // =========================================================
    // ADMIN SESSION
    // =========================================================

    public void setAdminSession(boolean value) {

        preferences.edit()
                .putBoolean(KEY_ADMIN_SESSION, value)
                .apply();
    }


    public boolean isAdminSession() {

        return preferences.getBoolean(
                KEY_ADMIN_SESSION,
                false
        );
    }


    // =========================================================
    // STUDENT SESSION
    // =========================================================

    public void setStudentSession(boolean value) {

        preferences.edit()
                .putBoolean(KEY_STUDENT_SESSION, value)
                .apply();
    }


    public boolean isStudentSession() {

        return preferences.getBoolean(
                KEY_STUDENT_SESSION,
                false
        );
    }


    // =========================================================
    // CLEAR ADMIN
    // =========================================================

    public void clearAdminSession() {

        preferences.edit()
                .putBoolean(KEY_ADMIN_SESSION, false)
                .apply();
    }


    // =========================================================
    // CLEAR STUDENT
    // =========================================================

    public void clearStudentSession() {

        preferences.edit()
                .putBoolean(KEY_STUDENT_SESSION, false)
                .apply();
    }


    // =========================================================
    // CLEAR EVERYTHING
    // =========================================================

    public void clearAllSessions() {

        preferences.edit()
                .clear()
                .apply();
    }
}