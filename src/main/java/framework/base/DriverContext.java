package framework.base;

import org.openqa.selenium.WebDriver;

/**
 * Shared ancestor for any class that needs the current thread's {@link WebDriver}
 * (tests and page objects alike). The driver is sourced from {@link DriverManager},
 * so subclasses simply call {@link #getDriver()} instead of reaching into the manager.
 *
 * <p>Keeping this separate from {@link BaseTest} means Page Objects can inherit the
 * driver without inheriting the test lifecycle (TestNG annotations, reporting).</p>
 */
public abstract class DriverContext {

    /** @return the WebDriver bound to the current thread. */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
