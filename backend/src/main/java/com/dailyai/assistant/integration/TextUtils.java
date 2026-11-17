package com.dailyai.assistant.integration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtils {

    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT_STYLE = Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]+);");
    private static final Pattern WHITESPACE = Pattern.compile("[ \\t\\x0B\\f\\r]+");

    private TextUtils() {
    }

    /** Converts feed HTML into plain text. Feeds are untrusted, so all markup is removed. */
    public static String cleanHtml(String input) {
        if (input == null) {
            return null;
        }
        String text = SCRIPT_STYLE.matcher(input).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
        text = replaceNumericEntities(text);
        text = WHITESPACE.matcher(text).replaceAll(" ");
        text = text.replace("\u00a0", " ");
        return text.trim();
    }

    public static String truncate(String input, int max) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max).trim();
    }

    private static String replaceNumericEntities(String text) {
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            try {
                int code = Integer.parseInt(matcher.group(2), matcher.group(1).equalsIgnoreCase("x") ? 16 : 10);
                matcher.appendReplacement(result, Matcher.quoteReplacement(new String(Character.toChars(code))));
            } catch (RuntimeException ex) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
