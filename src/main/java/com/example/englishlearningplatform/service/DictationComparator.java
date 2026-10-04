package com.example.englishlearningplatform.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.englishlearningplatform.dto.dictation.DictationWordResult;
import com.example.englishlearningplatform.dto.dictation.WordStatus;

final class DictationComparator {

    static final int MAX_WORDS = 1000;

    record ComparisonResult(List<DictationWordResult> words, double accuracy) {
    }

    private DictationComparator() {
    }

    static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replace('-', ' ')
                .replaceAll("[^\\p{L}\\p{N}'\\s]", "");

        normalized = normalized.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        String[] tokens = normalized.split("\\s+");

        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            String cleanedToken = stripOuterQuotes(token);
            if (!cleanedToken.isEmpty()) {
                result.add(cleanedToken);
            }
        }

        return result;
    }

    private static String stripOuterQuotes(String token) {
        int start = 0;
        int end = token.length();

        while (start < end && token.charAt(start) == '\'') {
            start++;
        }
        while (end > start && token.charAt(end - 1) == '\'') {
            end--;
        }

        return token.substring(start, end);
    }

    private static int[][] buildLcsTable(List<String> ref, List<String> user) {

        int n = ref.size();
        int m = user.size();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (ref.get(i).equals(user.get(j))) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        return dp;
    }

    static ComparisonResult compare(String transcript, String userInput) {
        List<String> ref = tokenize(transcript);
        List<String> user = tokenize(userInput);

        if (user.isEmpty()) {
            throw new IllegalArgumentException("Input must contain at least one word");
        }
        if (ref.isEmpty()) {
            throw new IllegalArgumentException("Transcript must contain at least one word");
        }
        if (ref.size() > MAX_WORDS || user.size() > MAX_WORDS) {
            throw new IllegalArgumentException("Text must not exceed " + MAX_WORDS + " words");
        }

        int[][] dp = buildLcsTable(ref, user);
        int i = 0;
        int j = 0;
        int n = ref.size();
        int m = user.size();

        List<DictationWordResult> result = new ArrayList<>();
        List<String> refGap = new ArrayList<>();
        List<String> userGap = new ArrayList<>();

        while (i < n && j < m) {
            if (ref.get(i).equals(user.get(j))) {
                flushGap(refGap, userGap, result);
                result.add(new DictationWordResult(user.get(j), WordStatus.CORRECT, null));
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                refGap.add(ref.get(i));
                i++;
            } else {
                userGap.add(user.get(j));
                j++;
            }
        }

        while (i < n) {
            refGap.add(ref.get(i));
            i++;
        }
        while (j < m) {
            userGap.add(user.get(j));
            j++;
        }
        flushGap(refGap, userGap, result);

        long correctCount = result.stream()
                .filter(w -> w.status() == WordStatus.CORRECT)
                .count();

        double accuracy = Math.round((100.0 * correctCount / Math.max(n, m)) * 10.0) / 10.0;

        return new ComparisonResult(result, accuracy);
    }

    private static void flushGap(List<String> refGap, List<String> userGap,
            List<DictationWordResult> out) {
        int k = Math.min(refGap.size(), userGap.size());

        for (int idx = 0; idx < k; idx++) {
            out.add(new DictationWordResult(userGap.get(idx), WordStatus.WRONG, refGap.get(idx)));
        }

        for (int idx = k; idx < refGap.size(); idx++) {
            out.add(new DictationWordResult(refGap.get(idx), WordStatus.MISSING, null));
        }

        for (int idx = k; idx < userGap.size(); idx++) {
            out.add(new DictationWordResult(userGap.get(idx), WordStatus.EXTRA, null));
        }

        refGap.clear();
        userGap.clear();
    }
}