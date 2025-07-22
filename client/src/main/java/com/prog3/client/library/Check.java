package com.prog3.client.library;

public class Check {
    public static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._]+@mail\\.com$";
        return email.matches(emailRegex);
    }
}
