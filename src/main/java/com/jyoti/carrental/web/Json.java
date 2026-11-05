package com.jyoti.carrental.web;

import com.jyoti.carrental.exception.InvalidRequestException;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately small JSON writer and parser (about 200 lines) so the project keeps
 * zero external dependencies. In Spring Boot, Jackson does this job automatically.
 *
 * <p><b>Writing</b> supports: null, String, Number (BigDecimal kept exact), Boolean, Enum
 * (as its name), java.time types (ISO-8601 via toString), Map, Collection and Java
 * records (each component becomes a field).</p>
 *
 * <p><b>Parsing</b> returns {@code Map<String,Object>}, {@code List<Object>}, String,
 * BigDecimal, Boolean or null. Malformed input throws {@link InvalidRequestException}
 * so the client gets a clean 400.</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class Json {

    private Json() {
    }

    // =================================================================== write

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder(256);
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            writeString(sb, s);
        } else if (v instanceof BigDecimal bd) {
            sb.append(bd.toPlainString());
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Enum<?> e) {
            writeString(sb, e.name());
        } else if (v instanceof TemporalAccessor) {
            writeString(sb, v.toString()); // LocalDate -> "2026-11-10", LocalDateTime -> "2026-11-10T10:00"
        } else if (v instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                writeValue(sb, entry.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
        } else if (v.getClass().isRecord()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            for (RecordComponent rc : v.getClass().getRecordComponents()) {
                try {
                    fields.put(rc.getName(), rc.getAccessor().invoke(v));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot read record component " + rc.getName(), e);
                }
            }
            writeValue(sb, fields);
        } else {
            writeString(sb, v.toString());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // =================================================================== parse

    /** Parses a JSON object. An empty body is treated as {@code {}}. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new InvalidRequestException("Request body must be a JSON object");
        }
        return (Map<String, Object>) value;
    }

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.readValue();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw p.error("Unexpected trailing characters");
        }
        return value;
    }

    /** Recursive-descent parser: one method per JSON grammar rule. */
    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        Object readValue() {
            skipWhitespace();
            if (atEnd()) throw error("Unexpected end of JSON");
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) yield readNumber();
                    throw error("Unexpected character '" + c + "'");
                }
            };
        }

        Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // {
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (!peek('"')) throw error("Expected a field name in double quotes");
                String key = readString();
                skipWhitespace();
                expect(':');
                map.put(key, readValue());
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else if (peek('}')) {
                    pos++;
                    return map;
                } else {
                    throw error("Expected ',' or '}'");
                }
            }
        }

        List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            pos++; // [
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (true) {
                list.add(readValue());
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else if (peek(']')) {
                    pos++;
                    return list;
                } else {
                    throw error("Expected ',' or ']'");
                }
            }
        }

        String readString() {
            pos++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (!atEnd()) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (atEnd()) break;
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"', '\\', '/' -> sb.append(e);
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (pos + 4 > s.length()) throw error("Bad unicode escape");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw error("Bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw error("Unterminated string");
        }

        BigDecimal readNumber() {
            int start = pos;
            if (peek('-')) pos++;
            while (!atEnd() && "0123456789.eE+-".indexOf(s.charAt(pos)) >= 0) pos++;
            try {
                return new BigDecimal(s.substring(start, pos));
            } catch (NumberFormatException e) {
                throw error("Invalid number");
            }
        }

        Object literal(String word, Object value) {
            if (!s.startsWith(word, pos)) throw error("Invalid literal");
            pos += word.length();
            return value;
        }

        void expect(char c) {
            if (!peek(c)) throw error("Expected '" + c + "'");
            pos++;
        }

        boolean peek(char c) {
            return !atEnd() && s.charAt(pos) == c;
        }

        void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        InvalidRequestException error(String message) {
            return new InvalidRequestException("Malformed JSON at position " + pos + ": " + message);
        }
    }
}
