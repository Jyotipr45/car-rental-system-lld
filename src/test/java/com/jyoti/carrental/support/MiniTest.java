package com.jyoti.carrental.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * A dependency-free test runner (about 60 lines) so the project builds and tests with
 * nothing but a JDK. Any public no-arg method whose name starts with {@code test} is run.
 *
 * <p>When you move to Spring Boot, these tests port 1:1 to JUnit 5: {@code @Test} on each
 * method, {@code Assertions.assertEquals}/{@code assertThrows} instead of the helpers below.</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class MiniTest {

    private MiniTest() {
    }

    /** Runs every test method on a fresh instance; exits with code 1 if any fail. */
    public static void run(Class<?> testClass) throws Exception {
        int passed = 0;
        int failed = 0;
        for (Method m : testClass.getDeclaredMethods()) {
            if (!m.getName().startsWith("test") || m.getParameterCount() != 0) {
                continue;
            }
            Object instance = testClass.getDeclaredConstructor().newInstance(); // isolation per test
            try {
                m.invoke(instance);
                passed++;
                System.out.println("  PASS  " + m.getName());
            } catch (InvocationTargetException e) {
                failed++;
                System.out.println("  FAIL  " + m.getName() + " -> " + e.getCause());
            }
        }
        System.out.println();
        System.out.println("  " + testClass.getSimpleName() + ": " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** Asserts that {@code action} throws {@code expected} (or a subclass) and returns it. */
    public static <T extends Throwable> T assertThrows(Class<T> expected, ThrowingRunnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                return expected.cast(t);
            }
            throw new AssertionError("expected " + expected.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + expected.getSimpleName() + " but nothing was thrown");
    }
}
