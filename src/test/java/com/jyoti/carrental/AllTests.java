package com.jyoti.carrental;

import com.jyoti.carrental.support.MiniTest;

/**
 * Runs every test suite. Exits with code 1 if anything fails.
 *
 * @author Jyoti Prakash Parida
 */
public final class AllTests {

    private AllTests() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== CarRentalSystemTest ==");
        MiniTest.run(CarRentalSystemTest.class);
        System.out.println();
        System.out.println("== LoggingAndAuditTest ==");
        MiniTest.run(LoggingAndAuditTest.class);
        System.out.println();
        System.out.println("== HttpApiTest ==");
        MiniTest.run(HttpApiTest.class);
    }
}
