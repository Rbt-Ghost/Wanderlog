package com.example.wanderlog;

import com.google.firebase.auth.FirebaseUser;
import java.util.Locale;

/** Single-letter stand-in for a profile picture. */
final class UserInitial {
    private UserInitial() {}

    static String of(FirebaseUser user) {
        if (user == null) return "";
        String source = user.getDisplayName();
        if (source == null || source.trim().isEmpty()) source = user.getEmail();
        if (source == null || source.trim().isEmpty()) return "";
        return source.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
