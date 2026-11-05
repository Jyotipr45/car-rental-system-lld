package com.jyoti.carrental.web;

import com.jyoti.carrental.config.CarRentalSystem;
import com.jyoti.carrental.controller.AuthController;
import com.jyoti.carrental.controller.BookingController;
import com.jyoti.carrental.controller.BranchController;
import com.jyoti.carrental.controller.VehicleController;
import com.jyoti.carrental.dto.request.AddBikeRequest;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.CreateBookingRequest;
import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.dto.request.LoginRequest;
import com.jyoti.carrental.dto.request.PaymentRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.dto.request.VehicleSearchRequest;
import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.exception.CarRentalException;
import com.jyoti.carrental.exception.InvalidRequestException;
import com.jyoti.carrental.logging.TraceContext;
import com.jyoti.carrental.model.enums.CarType;
import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.TransmissionType;
import com.jyoti.carrental.model.enums.UserRole;
import com.jyoti.carrental.model.enums.VehicleCategory;
import com.jyoti.carrental.model.enums.VehicleStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exposes the controllers as a real REST API over HTTP, using the JDK's built-in
 * {@code com.sun.net.httpserver} (no Tomcat, no Spring), so you can test with Postman,
 * Requestly, Swagger UI or curl.
 *
 * <p>This class is only the <b>HTTP adapter</b>: it turns HTTP into calls on the existing
 * controllers and turns {@link ApiResponse} back into HTTP. No business rule lives here.
 * That is the point of a layered design: the transport can change (console demo → HTTP →
 * Spring MVC) without touching services.</p>
 *
 * <pre>
 * GET  /                 → redirects to Swagger UI
 * GET  /swagger-ui       → interactive API docs (Swagger UI page)
 * GET  /openapi.yaml     → OpenAPI 3.1 contract
 * GET  /health           → {"status":"UP"}
 * ...  /api/v1/**        → the 17 REST endpoints (see openapi.yaml)
 * </pre>
 *
 * <p>Binds to 127.0.0.1 only (your machine), and sends permissive CORS headers so browser
 * tools such as Requestly's web app can call it during local testing.</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class HttpApiServer {

    private static final Logger LOG = Logger.getLogger(HttpApiServer.class.getName());
    private static final int MAX_BODY_BYTES = 1024 * 1024;
    private static final String CORS_HEADERS = "Content-Type, " + ApiRequest.USER_HEADER;

    private final HttpServer server;
    private final ExecutorService executor;
    private final Router router = new Router();

    /**
     * @param system the wired application
     * @param port   TCP port; 0 picks a free port (used by tests)
     */
    public HttpApiServer(CarRentalSystem system, int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        AtomicInteger n = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(16, r -> {
            Thread t = new Thread(r, "http-" + n.incrementAndGet());
            t.setDaemon(false);
            return t;
        });
        server.setExecutor(executor);
        server.createContext("/", this::dispatch);
        registerRoutes(system);
    }

    public void start() {
        server.start();
        LOG.info(() -> "HTTP API listening on http://localhost:" + port() + " (" + router.routes().size() + " routes)");
    }

    public void stop() {
        server.stop(0);
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.info("HTTP API stopped");
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public Router router() {
        return router;
    }

    // =================================================================== routes

    /** Every endpoint delegates to an existing controller method; this table is the whole API. */
    private void registerRoutes(CarRentalSystem system) {
        AuthController auth = system.authController();
        BranchController branches = system.branchController();
        VehicleController vehicles = system.vehicleController();
        BookingController bookings = system.bookingController();

        // ---- auth
        router.add("POST", "/api/v1/auth/register", r -> auth.register(() -> new RegisterUserRequest(
                r.string("name"), r.string("email"), r.string("phone"), r.string("password"),
                r.string("drivingLicenseNo"), r.enumValue("role", UserRole.class))));
        router.add("POST", "/api/v1/auth/login", r -> auth.login(() -> new LoginRequest(
                r.string("email"), r.string("password"))));

        // ---- branches
        router.add("POST", "/api/v1/branches", r -> branches.create(r.userId(), () -> new CreateBranchRequest(
                r.string("name"), r.string("address"), r.string("city"))));
        router.add("GET", "/api/v1/branches", r -> branches.list());
        router.add("GET", "/api/v1/branches/{id}/vehicles", r -> vehicles.listByBranch(r.pathLong("id")));

        // ---- vehicles
        router.add("POST", "/api/v1/vehicles/cars", r -> vehicles.addCar(r.userId(), () -> new AddCarRequest(
                r.string("registrationNo"), r.string("brand"), r.string("model"),
                r.enumValue("fuelType", FuelType.class), r.decimal("pricePerDay"), r.longValue("branchId"),
                r.enumValue("carType", CarType.class), r.enumValue("transmission", TransmissionType.class),
                r.intValue("seats"))));
        router.add("POST", "/api/v1/vehicles/bikes", r -> vehicles.addBike(r.userId(), () -> new AddBikeRequest(
                r.string("registrationNo"), r.string("brand"), r.string("model"),
                r.enumValue("fuelType", FuelType.class), r.decimal("pricePerDay"), r.longValue("branchId"),
                r.intValue("engineCc"))));
        router.add("PATCH", "/api/v1/vehicles/{id}/status", r -> vehicles.changeStatus(
                r.userId(), r.pathLong("id"), requireField(r.enumValue("status", VehicleStatus.class), "status")));
        router.add("GET", "/api/v1/vehicles/search", r -> vehicles.search(() -> new VehicleSearchRequest(
                r.queryLong("branchId", true), r.queryDate("startDate", true), r.queryDate("endDate", true),
                r.queryEnum("category", VehicleCategory.class), r.queryEnum("carType", CarType.class),
                r.queryInt("minSeats"))));

        // ---- bookings
        router.add("POST", "/api/v1/bookings", r -> bookings.create(() -> new CreateBookingRequest(
                r.userId(), r.longValue("vehicleId"), r.date("startDate"), r.date("endDate"))));
        router.add("GET", "/api/v1/bookings/{id}", r -> bookings.get(r.pathLong("id")));
        router.add("POST", "/api/v1/bookings/{id}/payments", r -> bookings.pay(() -> new PaymentRequest(
                r.pathLong("id"), r.userId(), r.enumValue("method", PaymentMethod.class))));
        router.add("GET", "/api/v1/bookings/{id}/payments", r -> bookings.payments(r.pathLong("id")));
        router.add("POST", "/api/v1/bookings/{id}/cancel", r -> bookings.cancel(r.pathLong("id"), r.userId()));
        router.add("POST", "/api/v1/bookings/{id}/pickup", r -> bookings.pickup(r.userId(), r.pathLong("id")));
        router.add("POST", "/api/v1/bookings/{id}/return", r -> bookings.returnVehicle(r.userId(), r.pathLong("id"),
                requireField(r.dateTime("returnTime"), "returnTime"), r.enumValue("lateFeeMethod", PaymentMethod.class)));

        // ---- users
        router.add("GET", "/api/v1/users/{userId}/bookings", r -> bookings.history(r.pathLong("userId")));
    }

    private static <T> T requireField(T value, String field) {
        if (value == null) {
            throw new InvalidRequestException("'" + field + "' is required");
        }
        return value;
    }

    // =================================================================== dispatch

    private void dispatch(HttpExchange ex) throws IOException {
        TraceContext.start(); // one trace id per HTTP request, reused by GlobalExceptionHandler
        try {
            String method = ex.getRequestMethod().toUpperCase(Locale.ROOT);
            String path = ex.getRequestURI().getPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            if (method.equals("OPTIONS")) {           // CORS pre-flight from browser tools
                sendEmpty(ex, 204, Set.copyOf(router.allowedMethods(path)));
                return;
            }
            if (serveStatic(ex, method, path)) {
                return;
            }

            Optional<Router.Match> match = router.match(method, path);
            if (match.isEmpty()) {
                Set<String> allowed = router.allowedMethods(path);
                if (allowed.isEmpty()) {
                    sendJson(ex, ApiResponse.error(404, "No endpoint " + method + " " + path + ". See /swagger-ui"));
                } else {
                    ex.getResponseHeaders().add("Allow", String.join(", ", allowed));
                    sendJson(ex, ApiResponse.error(405, "Method " + method + " not allowed here. Use " + allowed));
                }
                return;
            }

            ApiRequest request = new ApiRequest(method, path, match.get().pathParams(),
                    ex.getRequestURI().getRawQuery(), lowerCaseHeaders(ex), readBody(ex));
            ApiResponse<?> response;
            try {
                response = match.get().route().handler().apply(request);
            } catch (CarRentalException e) {
                // Raised while reading the request before a controller took over (e.g. missing X-User-Id).
                LOG.warning(() -> method + " " + ex.getRequestURI().getPath() + " " + e.getHttpStatus() + " "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                response = ApiResponse.error(e.getHttpStatus(), e.getMessage());
            }
            sendJson(ex, response);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unhandled error for " + ex.getRequestMethod() + " " + ex.getRequestURI(), e);
            sendJson(ex, ApiResponse.error(500, "Internal server error"));
        } finally {
            TraceContext.clear();
            ex.close();
        }
    }

    /** Serves the docs pages. Returns true if the request was handled. */
    private boolean serveStatic(HttpExchange ex, String method, String path) throws IOException {
        if (!method.equals("GET") && !method.equals("HEAD")) {
            return false;
        }
        switch (path) {
            case "/" -> {
                ex.getResponseHeaders().add("Location", "/swagger-ui");
                sendEmpty(ex, 302, Set.of());
                return true;
            }
            case "/health" -> {
                sendJson(ex, 200, Json.write(Map.of("status", "UP")));
                return true;
            }
            case "/openapi.yaml" -> {
                sendOpenApi(ex);
                return true;
            }
            case "/swagger-ui", "/swagger-ui.html", "/docs" -> {
                sendResource(ex, "/static/swagger-ui.html", "text/html; charset=utf-8");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // =================================================================== I/O helpers

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] bytes = in.readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                throw new InvalidRequestException("Request body larger than 1 MB");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> lowerCaseHeaders(HttpExchange ex) {
        Map<String, String> headers = new LinkedHashMap<>();
        ex.getRequestHeaders().forEach((k, v) -> {
            if (!v.isEmpty()) headers.put(k.toLowerCase(Locale.ROOT), v.get(0));
        });
        return headers;
    }

    private static void sendJson(HttpExchange ex, ApiResponse<?> response) throws IOException {
        if (response.traceId() != null) {
            ex.getResponseHeaders().add("X-Trace-Id", response.traceId());
        }
        sendJson(ex, response.status(), Json.write(response));
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        send(ex, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendResource(HttpExchange ex, String resource, String contentType) throws IOException {
        try (InputStream in = HttpApiServer.class.getResourceAsStream(resource)) {
            if (in == null) {
                sendJson(ex, ApiResponse.error(500, "Missing resource " + resource
                        + " (if you compiled with javac, copy src/main/resources into the classes folder)"));
                return;
            }
            send(ex, 200, contentType, in.readAllBytes());
        }
    }

    /**
     * Serves the contract with its {@code servers} URL pointing at this server's real port,
     * so Swagger UI "Try it out" and Postman/Requestly imports work even with -Dserver.port=9090.
     */
    private void sendOpenApi(HttpExchange ex) throws IOException {
        try (InputStream in = HttpApiServer.class.getResourceAsStream("/openapi.yaml")) {
            if (in == null) {
                sendResource(ex, "/openapi.yaml", "application/yaml; charset=utf-8"); // reports the missing file
                return;
            }
            String spec = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("http://localhost:8080", "http://localhost:" + port());
            send(ex, 200, "application/yaml; charset=utf-8", spec.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void sendEmpty(HttpExchange ex, int status, Set<String> allowed) throws IOException {
        addCors(ex, allowed);
        ex.sendResponseHeaders(status, -1);
    }

    private static void send(HttpExchange ex, int status, String contentType, byte[] body) throws IOException {
        addCors(ex, Set.of());
        ex.getResponseHeaders().set("Content-Type", contentType);
        boolean head = ex.getRequestMethod().equalsIgnoreCase("HEAD");
        ex.sendResponseHeaders(status, head ? -1 : body.length);
        if (!head) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(body);
            }
        }
    }

    /** Local-development CORS: lets browser-based tools (Requestly web, Swagger UI elsewhere) call the API. */
    private static void addCors(HttpExchange ex, Set<String> allowed) {
        var h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Headers", CORS_HEADERS);
        h.set("Access-Control-Expose-Headers", "X-Trace-Id");
        h.set("Access-Control-Allow-Methods", allowed.isEmpty() ? "GET, POST, PATCH, OPTIONS" : String.join(", ", allowed) + ", OPTIONS");
    }
}
