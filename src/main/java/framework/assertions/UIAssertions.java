package framework.assertions;

import framework.utilities.LoggerUtil;
import framework.utilities.StepLogger;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

/**
 * Dedicated home for UI assertions so test scripts never call raw {@code Assert.*}.
 *
 * <p>Supports two modes:</p>
 * <ul>
 *   <li><b>Hard</b> assertions fail the test immediately.</li>
 *   <li><b>Soft</b> assertions are collected per thread and verified together at the
 *       end of the test. The framework finalises them automatically (see the test
 *       listener), so tests do not call {@code assertAll()} themselves.</li>
 * </ul>
 *
 * <p>Every assertion is recorded as a step (log + Extent report + screenshot).</p>
 */
public class UIAssertions {

    private static final Logger LOG = LoggerUtil.getLogger(UIAssertions.class);
    private static final ThreadLocal<SoftAssert> SOFT = ThreadLocal.withInitial(SoftAssert::new);

    // ---------------------------------------------------------------- Hard assertions

    public void assertTrue(boolean condition, String message) {
        StepLogger.step(LOG, "Assert TRUE - " + message);
        Assert.assertTrue(condition, message);
    }

    public void assertEquals(Object actual, Object expected, String message) {
        StepLogger.step(LOG, "Assert EQUALS [expected=%s, actual=%s] - %s".formatted(expected, actual, message));
        Assert.assertEquals(actual, expected, message);
    }

    public void assertNotNull(Object value, String message) {
        StepLogger.step(LOG, "Assert NOT NULL - " + message);
        Assert.assertNotNull(value, message);
    }

    public void assertContains(String actual, String expected, String message) {
        StepLogger.step(LOG, "Assert CONTAINS [expected=%s, actual=%s] - %s".formatted(expected, actual, message));
        Assert.assertTrue(actual != null && actual.contains(expected), message);
    }

    // ---------------------------------------------------------------- Soft assertions

    public void softAssertTrue(boolean condition, String message) {
        StepLogger.step(LOG, "Soft assert TRUE - " + message);
        SOFT.get().assertTrue(condition, message);
    }

    public void softAssertEquals(Object actual, Object expected, String message) {
        StepLogger.step(LOG, "Soft assert EQUALS [expected=%s, actual=%s] - %s".formatted(expected, actual, message));
        SOFT.get().assertEquals(actual, expected, message);
    }

    public void softAssertNotNull(Object value, String message) {
        StepLogger.step(LOG, "Soft assert NOT NULL - " + message);
        SOFT.get().assertNotNull(value, message);
    }

    public void softAssertContains(String actual, String expected, String message) {
        StepLogger.step(LOG, "Soft assert CONTAINS [expected=%s, actual=%s] - %s".formatted(expected, actual, message));
        SOFT.get().assertTrue(actual != null && actual.contains(expected), message);
    }

    // ---------------------------------------------------------------- Lifecycle (framework-only)

    /** Verifies all collected soft assertions for the current thread, then clears them. */
    public static void assertAll() {
        try {
            SOFT.get().assertAll();
        } finally {
            SOFT.remove();
        }
    }

    /** Discards any collected soft assertions for the current thread without verifying. */
    public static void reset() {
        SOFT.remove();
    }
}
