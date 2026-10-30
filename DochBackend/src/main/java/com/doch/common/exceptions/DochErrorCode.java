package com.doch.common.exceptions;


public enum DochErrorCode {
    AUTH001("AUTH001", "Invalid credentials"),
    AUTH002("AUTH002", "User not found"),
    AUTH003("AUTH003", "User already exists"),
    AUTH004("AUTH004", "Token expired"),
    AUTH005("AUTH005", "Token invalid"),
    AUTH006("AUTH006", "User not authorized"),
    AUTH007("AUTH007", "Account Locked"),
    AUTH008("AUTH008", "Password reset required"),
    AUTH009("AUTH009", "Email not verified"),
    AUTH0010("AUTH010", "Email already exists"),
    AUTH0011("AUTH011", "Phone number already exists"),
    AUTH0012("AUTH012", "Email already verified"),
    AUTH0013("AUTH013", "Account not active yet. If you were invited, open the invitation email to set your password (or request a new code on the setup page); otherwise contact DOCH support."),
    OTP001("OTP001", "Invalid OTP"),
    OTP002("OTP002", "OTP expired"),
    OTP003("OTP003", "OTP already used"),
    OTP004("OTP004", "OTP not found"),
    OTP005("OTP005", "OTP not sent"),
    OTP006("OTP006", "OTP already sent");

    private final String value;
    private final String message;
    DochErrorCode(String value, String message) {
        this.value = value;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static DochErrorCode fromValue(String value) {
        for (DochErrorCode errorCode : DochErrorCode.values()) {
            if (errorCode.value.equals(value)) {
                return errorCode;
            }
        }
        return null;
    }
}
