package com.doch.common;

import java.security.SecureRandom;

/** Generates short, unambiguous, human-friendly identifiers (no 0/O/1/I/L). */
public final class ReferenceGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private ReferenceGenerator() {
    }

    public static String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    /** Order reference, e.g. DO-7K3M9Q2X. */
    public static String orderReference() {
        return "DO-" + randomCode(8);
    }

    /** Package tracking number, e.g. DCH-4KQ8Z7MX2P. */
    public static String trackingNumber() {
        return "DCH-" + randomCode(10);
    }

    /** Numeric delivery OTP. */
    public static String numericOtp(int digits) {
        StringBuilder sb = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}