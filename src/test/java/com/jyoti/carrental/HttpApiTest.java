package com.jyoti.carrental;

import com.jyoti.carrental.config.CarRentalSystem;
import com.jyoti.carrental.logging.LoggingConfig;
import com.jyoti.carrental.support.MiniTest;
import com.jyoti.carrental.web.DemoDataSeeder;
import com.jyoti.carrental.web.HttpApiServer;
import com.jyoti.carrental.web.Json;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.jyoti.carrental.support.MiniTest.assertEquals;
import static com.jyoti.carrental.support.MiniTest.assertTrue;

/**
 * End-to-end tests over real HTTP: starts {@link HttpApiServer} on a free port and calls it
 * with the JDK {@link HttpClient}, exactly as Postman would.
 *
 * @author Jyoti Prakash Parida
 */
public class HttpApiTest {

    static {
        LoggingConfig.init(Level.OFF, Level.OFF, null);
    }

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final LocalDate TODAY = LocalDate.now();

    private final HttpApiServer server;
    private final DemoDataSeeder.SeedData seed;
    private final String base;

    public HttpApiTest() throws Exception {
        CarRentalSystem system = CarRentalSystem.create(Clock.systemDefaultZone(), false);
        seed = DemoDataSeeder.seed(system);
        server = new HttpApiServer(system, 0); // 0 = any free port
        server.start();
        base = "http://localhost:" + server.port();
    }

    public static void main(String[] args) throws Exception {
        MiniTest.run(HttpApiTest.class);
    }

    // ================================================================ tests

    public void testHealthSwaggerAndSpecAreServed() throws Exception {
        try {
            assertEquals(200, get("/health").statusCode());
            HttpResponse<String> ui = get("/swagger-ui");
            assertEquals(200, ui.statusCode());
            assertTrue(ui.body().contains("swagger-ui-dist@5.33.3"), "Swagger UI page");
            HttpResponse<String> spec = get("/openapi.yaml");
            assertTrue(spec.body().startsWith("openapi: 3.1.0"), "OpenAPI 3.1 spec");
            assertTrue(spec.body().contains("url: http://localhost:" + server.port()), "spec points at the real port");
        } finally {
            server.stop();
        }
    }

    public void testEveryRouteInTheServerIsDocumentedInTheSpec() throws Exception {
        try {
            String spec = get("/openapi.yaml").body();
            for (var route : server.router().routes()) {
                assertTrue(spec.contains("  " + route.template() + ":"), "missing in openapi.yaml: " + route.template());
                assertTrue(spec.contains("    " + route.method().toLowerCase() + ":"), "method " + route.method());
            }
        } finally {
            server.stop();
        }
    }

    public void testBookPayPickupReturnOverHttp() throws Exception {
        try {
            HttpResponse<String> created = post("/api/v1/bookings", seed.riyaId(),
                    "{\"vehicleId\":" + seed.cretaId() + ",\"startDate\":\"" + TODAY + "\",\"endDate\":\"" + TODAY.plusDays(2) + "\"}");
            assertEquals(201, created.statusCode());
            Map<String, Object> data = data(created);
            assertEquals("PENDING_PAYMENT", data.get("status"));
            assertEquals(new BigDecimal("9600.00"), data.get("totalAmount"));
            long id = ((BigDecimal) data.get("id")).longValue();
            assertTrue(created.headers().firstValue("X-Trace-Id").isPresent(), "X-Trace-Id header");

            assertEquals(201, post("/api/v1/bookings/" + id + "/payments", seed.riyaId(), "{\"method\":\"UPI\"}").statusCode());
            assertEquals(200, post("/api/v1/bookings/" + id + "/pickup", seed.adminId(), "").statusCode());
            HttpResponse<String> returned = post("/api/v1/bookings/" + id + "/return", seed.adminId(),
                    "{\"returnTime\":\"" + TODAY.plusDays(4) + "T11:00:00\",\"lateFeeMethod\":\"CARD\"}");
            assertEquals(200, returned.statusCode());
            assertEquals(new BigDecimal("9600.00"), data(returned).get("lateFee"));
            assertEquals("COMPLETED", data(returned).get("status"));
        } finally {
            server.stop();
        }
    }

    public void testErrorsAreProperHttpStatusesWithJsonBodies() throws Exception {
        try {
            String body = "{\"vehicleId\":" + seed.nexonId() + ",\"startDate\":\"" + TODAY.plusDays(5) + "\",\"endDate\":\"" + TODAY.plusDays(6) + "\"}";
            assertEquals(201, post("/api/v1/bookings", seed.arjunId(), body).statusCode());
            assertEquals(409, post("/api/v1/bookings", seed.riyaId(), body).statusCode());            // overlap
            assertEquals(401, post("/api/v1/bookings", null, body).statusCode());                     // no X-User-Id
            assertEquals(403, post("/api/v1/branches", seed.riyaId(), "{\"name\":\"X\",\"address\":\"Y\",\"city\":\"Z\"}").statusCode());
            assertEquals(400, post("/api/v1/auth/login", null, "{\"email\":").statusCode());          // malformed JSON
            assertEquals(400, post("/api/v1/bookings/1/payments", seed.riyaId(), "{\"method\":\"GOLD\"}").statusCode());
            assertEquals(404, get("/api/v1/bookings/999").statusCode());
            assertEquals(404, get("/api/v1/does-not-exist").statusCode());
            HttpResponse<String> wrongMethod = send(HttpRequest.newBuilder(URI.create(base + "/api/v1/branches")).DELETE().build());
            assertEquals(405, wrongMethod.statusCode());
            assertTrue(wrongMethod.headers().firstValue("Allow").orElse("").contains("POST"), "Allow header");

            Map<String, Object> err = Json.parseObject(post("/api/v1/bookings", seed.riyaId(), body).body());
            assertEquals(Boolean.FALSE, err.get("success"));
            assertTrue(String.valueOf(err.get("message")).contains("already booked"), "message");
        } finally {
            server.stop();
        }
    }

    public void testCorsPreflightForBrowserTools() throws Exception {
        try {
            HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(base + "/api/v1/bookings"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build());
            assertEquals(204, r.statusCode());
            assertEquals("*", r.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
            assertTrue(r.headers().firstValue("Access-Control-Allow-Headers").orElse("").contains("X-User-Id"), "allows X-User-Id");
        } finally {
            server.stop();
        }
    }

    public void testJsonRoundTrip() {
        Object parsed = Json.parse("{\"a\":[1,2.50,\"x\\\"y\",true,null],\"b\":{\"c\":\"\\u0041\"}}");
        assertEquals("{\"a\":[1,2.50,\"x\\\"y\",true,null],\"b\":{\"c\":\"A\"}}", Json.write(parsed));
        assertEquals(List.of(), Json.parse("[]"));
        server.stop();
    }

    // ================================================================ helpers

    private HttpResponse<String> get(String path) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(base + path)).GET().build());
    }

    private HttpResponse<String> post(String path, Long userId, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (userId != null) {
            b.header("X-User-Id", String.valueOf(userId));
        }
        return send(b.build());
    }

    private static HttpResponse<String> send(HttpRequest request) throws Exception {
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(HttpResponse<String> r) {
        return (Map<String, Object>) Json.parseObject(r.body()).get("data");
    }
}
