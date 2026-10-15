package org.ansible.messenger;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Распознавание и форматирование анонимных номеров (crystalpiece): префикс из appConfig
// fragment_prefixes плюс 1..8 цифр абонента, показ группами по 4 слева. Префиксы приходят с
// сервера (MessagesController), запасной — "777", хранится в глобальных настройках.
public class AnonymousNumbers {

    private static final String PREF_KEY = "fragmentPrefixes";
    private static final String DEFAULT_PREFIX = "777";
    private static final int MAX_SUBSCRIBER_LENGTH = 8;

    private static volatile List<String> prefixes;

    private static synchronized List<String> prefixes() {
        if (prefixes == null) {
            List<String> loaded = null;
            try {
                String stored = MessagesController.getGlobalMainSettings().getString(PREF_KEY, null);
                if (!TextUtils.isEmpty(stored)) {
                    loaded = new ArrayList<>(Arrays.asList(stored.split(",")));
                }
            } catch (Exception ignore) {
            }
            prefixes = loaded != null && !loaded.isEmpty()
                    ? loaded
                    : new ArrayList<>(Collections.singletonList(DEFAULT_PREFIX));
        }
        return prefixes;
    }

    public static void setPrefixes(List<String> newPrefixes) {
        if (newPrefixes == null || newPrefixes.isEmpty()) {
            return;
        }
        List<String> clean = new ArrayList<>();
        for (String prefix : newPrefixes) {
            if (!TextUtils.isEmpty(prefix)) {
                clean.add(prefix);
            }
        }
        if (clean.isEmpty()) {
            return;
        }
        prefixes = clean;
        try {
            MessagesController.getGlobalMainSettings().edit().putString(PREF_KEY, TextUtils.join(",", clean)).apply();
        } catch (Exception ignore) {
        }
    }

    public static boolean isAnonymous(String digits) {
        return matchedPrefix(digits) != null;
    }

    public static boolean isPrefix(String code) {
        return code != null && prefixes().contains(code);
    }

    public static String format(String digits) {
        String prefix = matchedPrefix(digits);
        if (prefix == null) {
            return digits == null ? "" : "+" + digits;
        }
        String rest = digits.substring(prefix.length());
        StringBuilder result = new StringBuilder("+").append(prefix);
        for (int i = 0; i < rest.length(); i++) {
            if (i % 4 == 0) {
                result.append(' ');
            }
            result.append(rest.charAt(i));
        }
        return result.toString();
    }

    private static String matchedPrefix(String digits) {
        if (TextUtils.isEmpty(digits)) {
            return null;
        }
        for (String prefix : prefixes()) {
            if (!TextUtils.isEmpty(prefix) && digits.startsWith(prefix)) {
                int subscriberLength = digits.length() - prefix.length();
                if (subscriberLength >= 1 && subscriberLength <= MAX_SUBSCRIBER_LENGTH) {
                    return prefix;
                }
            }
        }
        return null;
    }
}
