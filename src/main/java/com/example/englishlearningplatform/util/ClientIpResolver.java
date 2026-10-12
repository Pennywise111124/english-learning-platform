package com.example.englishlearningplatform.util;

public final class ClientIpResolver {

    static final int MAX_LENGTH = 64;
    private static final String UNKNOWN = "unknown";

    private ClientIpResolver() {
    }

    public static String resolve(String remoteAddr, String forwardedFor, boolean trustedProxy) {
        String fallback = remoteAddr == null ? UNKNOWN : remoteAddr;

        if (!trustedProxy || forwardedFor == null || forwardedFor.isBlank()) {
            return limit(fallback);
        }

        int comma = forwardedFor.indexOf(',');
        String first = (comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
        if (first.isEmpty()) {
            return limit(fallback);
        }
        return limit(first);
    }

    private static String limit(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}