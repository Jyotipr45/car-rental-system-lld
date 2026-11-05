package com.jyoti.carrental.web;

import com.jyoti.carrental.exception.AuthenticationException;
import com.jyoti.carrental.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * One incoming HTTP request, with typed getters that turn bad input into a clean
 * {@code 400 Bad Request} (via {@link InvalidRequestException}) instead of a crash.
 *
 * <p>This is the hand-written equivalent of Spring MVC's argument binding:
 * {@code @PathVariable}, {@code @RequestParam}, {@code @RequestHeader} and {@code @RequestBody}.</p>
 *
 * <p><b>Who is calling?</b> The {@code X-User-Id} header stands in for authentication.
 * A real system would read the user id from a verified JWT instead; see the testing guide.</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class ApiRequest {

    /** Header that identifies the acting user (a stand-in for a JWT subject). */
    public static final String USER_HEADER = "X-User-Id";

    private final String method;
    private final String path;
    private final Map<String, String> pathParams;
    private final Map<String, String> query;
    private final Map<String, String> headers;
    private final String rawBody;
    private Map<String, Object> body;

    public ApiRequest(String method, String path, Map<String, String> pathParams, String rawQuery,
                      Map<String, String> headers, String rawBody) {
        this.method = method;
        this.path = path;
        this.pathParams = pathParams;
        this.query = parseQuery(rawQuery);
        this.headers = headers;
        this.rawBody = rawBody;
    }

    public String method() { return method; }
    public String path() { return path; }

    // ---------------------------------------------------------------- caller identity

    /** The acting user from {@code X-User-Id}; missing or invalid → 401. */
    public Long userId() {
        String value = headers.get(USER_HEADER.toLowerCase(Locale.ROOT));
        if (value == null || value.isBlank()) {
            throw new AuthenticationException("Missing " + USER_HEADER + " header (the id of the user making the call)");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new AuthenticationException(USER_HEADER + " must be a number, got '" + value + "'");
        }
    }

    // ---------------------------------------------------------------- path & query

    public Long pathLong(String name) {
        String value = pathParams.get(name);
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Path parameter '" + name + "' must be a number, got '" + value + "'");
        }
    }

    public Long queryLong(String name, boolean required) {
        String v = queryValue(name, required);
        if (v == null) return null;
        try {
            return Long.valueOf(v);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Query parameter '" + name + "' must be a number");
        }
    }

    public Integer queryInt(String name) {
        Long v = queryLong(name, false);
        return v == null ? null : v.intValue();
    }

    public LocalDate queryDate(String name, boolean required) {
        String v = queryValue(name, required);
        return v == null ? null : toDate(v, name);
    }

    public <E extends Enum<E>> E queryEnum(String name, Class<E> type) {
        String v = queryValue(name, false);
        return v == null ? null : toEnum(v, type, name);
    }

    private String queryValue(String name, boolean required) {
        String v = query.get(name);
        if ((v == null || v.isBlank()) && required) {
            throw new InvalidRequestException("Query parameter '" + name + "' is required");
        }
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    // ---------------------------------------------------------------- JSON body

    /** Parsed lazily, so a GET never fails on its (empty) body. */
    public Map<String, Object> body() {
        if (body == null) {
            body = Json.parseObject(rawBody);
        }
        return body;
    }

    public String string(String field) {
        Object v = body().get(field);
        return v == null ? null : String.valueOf(v);
    }

    public BigDecimal decimal(String field) {
        Object v = body().get(field);
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("'" + field + "' must be a number");
        }
    }

    public Long longValue(String field) {
        BigDecimal v = decimal(field);
        return v == null ? null : v.longValue();
    }

    /** Missing numbers become 0 so the DTO's own validation reports "must be greater than zero". */
    public int intValue(String field) {
        BigDecimal v = decimal(field);
        return v == null ? 0 : v.intValue();
    }

    public LocalDate date(String field) {
        String v = string(field);
        return v == null ? null : toDate(v, field);
    }

    public LocalDateTime dateTime(String field) {
        String v = string(field);
        if (v == null) return null;
        try {
            return LocalDateTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("'" + field + "' must be an ISO date-time like 2026-11-09T11:00:00, got '" + v + "'");
        }
    }

    public <E extends Enum<E>> E enumValue(String field, Class<E> type) {
        String v = string(field);
        return v == null ? null : toEnum(v, type, field);
    }

    // ---------------------------------------------------------------- helpers

    private static LocalDate toDate(String v, String name) {
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("'" + name + "' must be a date like 2026-11-10, got '" + v + "'");
        }
    }

    private static <E extends Enum<E>> E toEnum(String v, Class<E> type, String name) {
        try {
            return Enum.valueOf(type, v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("'" + name + "' must be one of " + Arrays.toString(type.getEnumConstants())
                    + ", got '" + v + "'");
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            map.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return map;
    }
}
