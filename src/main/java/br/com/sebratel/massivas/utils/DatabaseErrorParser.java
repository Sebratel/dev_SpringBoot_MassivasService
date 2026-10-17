package br.com.sebratel.massivas.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseErrorParser {

    private static final Pattern NULL_COLUMN_PATTERN = Pattern.compile("Column '(.+?)' cannot be null", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUPLICATE_ENTRY_PATTERN = Pattern.compile("Duplicate entry '(.+?)' for key '(.+?)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_TOO_LONG_PATTERN = Pattern.compile("Data too long for column '(.+?)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("a foreign key constraint fails.*FOREIGN KEY \\('(.+?)'\\)", Pattern.CASE_INSENSITIVE);

    public static Map<String, Object> parse(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        Map<String, String> dbErrors = new HashMap<>();

        Matcher nullMatcher = NULL_COLUMN_PATTERN.matcher(message);
        if (nullMatcher.find()) {
            dbErrors.put(nullMatcher.group(1), "cannot be null");
        }

        Matcher duplicateMatcher = DUPLICATE_ENTRY_PATTERN.matcher(message);
        if (duplicateMatcher.find()) {
            dbErrors.put(duplicateMatcher.group(2), "duplicate entry: " + duplicateMatcher.group(1));
        }

        Matcher tooLongMatcher = DATA_TOO_LONG_PATTERN.matcher(message);
        if (tooLongMatcher.find()) {
            dbErrors.put(tooLongMatcher.group(1), "data too long");
        }

        Matcher fkMatcher = FOREIGN_KEY_PATTERN.matcher(message);
        if (fkMatcher.find()) {
            dbErrors.put(fkMatcher.group(1), "foreign key constraint violation");
        }

        if (dbErrors.isEmpty()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("database", dbErrors);
        return result;
    }
}
