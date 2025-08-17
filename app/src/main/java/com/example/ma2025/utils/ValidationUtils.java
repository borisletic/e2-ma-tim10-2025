package com.example.ma2025.utils;

import android.util.Patterns;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= Constants.MIN_PASSWORD_LENGTH;
    }

    public static boolean isValidUsername(String username) {
        return username != null &&
                username.length() >= Constants.MIN_USERNAME_LENGTH &&
                username.length() <= Constants.MAX_USERNAME_LENGTH &&
                username.matches("^[a-zA-Z0-9_]+$"); // Samo slova, brojevi i _
    }

    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    public static String getPasswordStrengthMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Unesite lozinku";
        }
        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return "Lozinka mora imati najmanje " + Constants.MIN_PASSWORD_LENGTH + " karaktera";
        }
        if (password.length() >= 8 && containsUpperCase(password) &&
                containsLowerCase(password) && containsDigit(password)) {
            return "Jaka lozinka";
        }
        if (password.length() >= 6) {
            return "Srednja lozinka";
        }
        return "Slaba lozinka";
    }

    private static boolean containsUpperCase(String str) {
        return str.matches(".*[A-Z].*");
    }

    private static boolean containsLowerCase(String str) {
        return str.matches(".*[a-z].*");
    }

    private static boolean containsDigit(String str) {
        return str.matches(".*\\d.*");
    }

    public static String getUsernameErrorMessage(String username) {
        if (username == null || username.isEmpty()) {
            return "Unesite korisničko ime";
        }
        if (username.length() < Constants.MIN_USERNAME_LENGTH) {
            return "Korisničko ime mora imati najmanje " + Constants.MIN_USERNAME_LENGTH + " karaktera";
        }
        if (username.length() > Constants.MAX_USERNAME_LENGTH) {
            return "Korisničko ime može imati najviše " + Constants.MAX_USERNAME_LENGTH + " karaktera";
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "Korisničko ime može sadržati samo slova, brojeve i znak _";
        }
        return null;
    }
}