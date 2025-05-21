package com.example.deepworkorkestrator;

import android.util.Base64;

public class PasswordHelper {
    public static String encode(String password) {
        return Base64.encodeToString(password.getBytes(), Base64.DEFAULT);
    }

    public static boolean check(String input, String encoded) {
        String decoded = new String(Base64.decode(encoded, Base64.DEFAULT));
        return input.equals(decoded);
    }
}