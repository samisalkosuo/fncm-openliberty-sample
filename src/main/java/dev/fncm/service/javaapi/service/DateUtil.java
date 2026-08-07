package dev.fncm.service.javaapi.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
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
 *
 * <p>{@link DateTimeFormatter} instances are immutable and thread-safe; there is no shared
 * mutable state in this class.
 */
public final class DateUtil {

    private static final Logger LOGGER = Logger.getLogger(DateUtil.class.getName());

    /** Thread-safe formatter for {@code dd.MM.yyyy} (building inspection JSON extract). */
    private static final DateTimeFormatter FMT_DD_MM_YYYY =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DateUtil() {}

    /**
     * Parses a date string in {@code dd.MM.yyyy} format (e.g. {@code "02.07.2026"}).
     *
     * @param dateStr the date string to parse
     * @return the parsed {@link Date} at midnight UTC, or {@code null} if the string is blank or unparseable
     */
    public static Date parseDdMmYyyy(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(dateStr, FMT_DD_MM_YYYY);
            return Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (DateTimeParseException e) {
            LOGGER.warning("Could not parse date '" + dateStr + "' with pattern 'dd.MM.yyyy': " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses a date string in ISO-8601 UTC format (e.g. {@code "2026-07-02T00:00:00Z"}).
     *
     * @param dateStr the date string to parse
     * @return the parsed {@link Date}, or {@code null} if the string is blank or unparseable
     */
    public static Date parseIso8601Utc(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Date.from(zdt.toInstant());
        } catch (DateTimeParseException e) {
            LOGGER.warning("Could not parse date '" + dateStr + "' as ISO-8601: " + e.getMessage());
            return null;
        }
    }
}
