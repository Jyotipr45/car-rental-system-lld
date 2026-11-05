package com.jyoti.carrental;

import com.jyoti.carrental.config.CarRentalSystem;
import com.jyoti.carrental.logging.LoggingConfig;
import com.jyoti.carrental.web.DemoDataSeeder;
import com.jyoti.carrental.web.HttpApiServer;

/**
 * Starts the car rental system as a REST API on http://localhost:8080 so it can be tested
 * manually with Swagger UI, Postman, Requestly or curl.
 *
 * <pre>
 * server.bat                    (or ./server.sh)
 * -Dserver.port=9090            change the port   (env: SERVER_PORT)
 * -Dseed=false                  start with an empty system   (env: SEED)
 * -Dlog.level=DEBUG             more detail in logs/car-rental-0.log
 * </pre>
 *
 * <p>Stop it with Ctrl+C (or the red stop button in IntelliJ). Data lives in memory, so a
 * restart gives you a clean, freshly seeded system.</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class ApiServerApplication {

    private ApiServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        // Request lines (<-- POST /api/v1/bookings 201) are INFO; show them on the console by default.
        LoggingConfig.initFromEnvironment("INFO");

        CarRentalSystem system = CarRentalSystem.getInstance();
        boolean seed = !"false".equalsIgnoreCase(setting("seed", "SEED", "true"));
        DemoDataSeeder.SeedData data = seed ? DemoDataSeeder.seed(system) : null;

        int port = Integer.parseInt(setting("server.port", "SERVER_PORT", "8080"));
        HttpApiServer server = new HttpApiServer(system, port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown"));

        String base = "http://localhost:" + server.port();
        System.out.println();
        System.out.println("=".repeat(78));
        System.out.println("  Car Rental API is running");
        System.out.println("=".repeat(78));
        System.out.println("  Swagger UI   : " + base + "/swagger-ui");
        System.out.println("  OpenAPI spec : " + base + "/openapi.yaml");
        System.out.println("  Health       : " + base + "/health");
        System.out.println("  Postman      : import postman/CarRental.postman_collection.json");
        if (data != null) {
            System.out.println();
            System.out.println("  Seeded data (send the user id in the X-User-Id header):");
            System.out.println("    Admin  id=" + data.adminId() + "  admin@rental.com / admin@123");
            System.out.println("    Riya   id=" + data.riyaId() + "  riya@mail.com    / riya@123   (customer)");
            System.out.println("    Arjun  id=" + data.arjunId() + "  arjun@mail.com   / arjun@123  (customer)");
            System.out.println("    Branch id=" + data.branchId() + "  Hinjewadi Hub, Pune");
            System.out.println("    Vehicles: 1 Swift Rs.1800 | 2 Creta Rs.3200 | 3 Nexon EV Rs.2800 | 4 Classic 350 Rs.900 | 5 BMW 7 Rs.40000");
        }
        System.out.println();
        System.out.println("  Logs: logs/car-rental-0.log, logs/audit-0.log    Stop: Ctrl+C");
        System.out.println("=".repeat(78));
    }

    private static String setting(String property, String envVar, String defaultValue) {
        String v = System.getProperty(property);
        if (v == null || v.isBlank()) v = System.getenv(envVar);
        return (v == null || v.isBlank()) ? defaultValue : v.trim();
    }
}
