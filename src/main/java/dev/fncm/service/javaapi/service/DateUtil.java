package dev.fncm.service.javaapi.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Date-parsing utilities shared across FileNet JACE operation classes.
 *
 * <p>Two formats are in use in this package:
 * <ul>
 *   <li>{@link #parseDdMmYyyy} — {@code dd.MM.yyyy} — used in uploaded JSON field data</li>
 *   <li>{@link #parseIso8601Utc} — {@code yyyy-MM-dd'T'HH:mm:ss'Z'} — used in UI request payloads</li>
 * </ul>
 *
 * <p>Both methods return {@code null} and log a warning on parse failure rather than
 * propagating an exception, keeping callers simple.
 */
public final class DateUtil {

    private static final Logger LOGGER = Logger.getLogger(DateUtil.class.getName());

    /** Format used in the building inspection JSON extract file: {@code dd.MM.yyyy}. */
    private static final String DD_MM_YYYY = "dd.MM.yyyy";

    /** ISO-8601 UTC format used in UI request payloads: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}. */
    private static final String ISO_8601_UTC = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private DateUtil() {}

    /**
     * Parses a date string in {@code dd.MM.yyyy} format (e.g. {@code "02.07.2026"}).
     *
     * @param dateStr the date string to parse
     * @return the parsed {@link Date}, or {@code null} if the string is blank or unparseable
     */
    public static Date parseDdMmYyyy(String dateStr) {
        return parse(dateStr, DD_MM_YYYY, null);
    }

    /**
     * Parses a date string in ISO-8601 UTC format (e.g. {@code "2026-07-02T00:00:00Z"}).
     *
     * @param dateStr the date string to parse
     * @return the parsed {@link Date}, or {@code null} if the string is blank or unparseable
     */
    public static Date parseIso8601Utc(String dateStr) {
        return parse(dateStr, ISO_8601_UTC, "UTC");
    }

    // ── private ───────────────────────────────────────────────────────────────

    private static Date parse(String dateStr, String pattern, String timeZoneId) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            if (timeZoneId != null) {
                sdf.setTimeZone(TimeZone.getTimeZone(timeZoneId));
            }
            return sdf.parse(dateStr);
        } catch (Exception e) {
            LOGGER.warning("Could not parse date '" + dateStr + "' with pattern '" + pattern + "': " + e.getMessage());
            return null;
        }
    }
}
