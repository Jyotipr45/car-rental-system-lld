package com.jyoti.carrental.web;

import com.jyoti.carrental.dto.response.ApiResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps {@code METHOD /path/{param}} to a handler, the plain-Java twin of Spring's
 * {@code @GetMapping / @PostMapping} with {@code @PathVariable}.
 *
 * <p>Each route template is compiled to a regex once at startup:
 * {@code /api/v1/bookings/{id}/payments} → {@code ^/api/v1/bookings/(?<id>[^/]+)/payments$}.</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class Router {

    /** A handler gets the parsed request and returns the controller's response envelope. */
    @FunctionalInterface
    public interface Handler extends Function<ApiRequest, ApiResponse<?>> {
    }

    public record Route(String method, String template, Pattern pattern, List<String> paramNames, Handler handler) {
    }

    public record Match(Route route, Map<String, String> pathParams) {
    }

    private static final Pattern PARAM = Pattern.compile("\\{(\\w+)}");

    private final List<Route> routes = new ArrayList<>();

    public Router add(String method, String template, Handler handler) {
        List<String> names = new ArrayList<>();
        Matcher m = PARAM.matcher(template);
        StringBuilder regex = new StringBuilder("^");
        int last = 0;
        while (m.find()) {
            regex.append(Pattern.quote(template.substring(last, m.start())));
            regex.append("(?<").append(m.group(1)).append(">[^/]+)");
            names.add(m.group(1));
            last = m.end();
        }
        regex.append(Pattern.quote(template.substring(last))).append("$");
        routes.add(new Route(method, template, Pattern.compile(regex.toString()), names, handler));
        return this;
    }

    /** Finds the route for this method and path. */
    public Optional<Match> match(String method, String path) {
        for (Route r : routes) {
            if (!r.method().equals(method)) continue;
            Matcher m = r.pattern().matcher(path);
            if (m.matches()) {
                Map<String, String> params = new LinkedHashMap<>();
                for (String name : r.paramNames()) {
                    params.put(name, m.group(name));
                }
                return Optional.of(new Match(r, params));
            }
        }
        return Optional.empty();
    }

    /** Methods allowed on this path, for 405 responses and CORS pre-flight. Empty = unknown path. */
    public TreeSet<String> allowedMethods(String path) {
        TreeSet<String> methods = new TreeSet<>();
        for (Route r : routes) {
            if (r.pattern().matcher(path).matches()) {
                methods.add(r.method());
            }
        }
        return methods;
    }

    public List<Route> routes() {
        return List.copyOf(routes);
    }
}
