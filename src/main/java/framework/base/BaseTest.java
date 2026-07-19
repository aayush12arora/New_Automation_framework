package framework.base;

import framework.utilities.LoggerUtil;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Root test class. Owns only framework initialisation:
 * driver creation before each test and driver teardown after each test.
 * UI- or API-specific setup belongs in subclasses.
 */
public abstract class BaseTest {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        log.info("=== Test setup: initialising driver ===");
        DriverManager.setDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        log.info("=== Test teardown: quitting driver ===");
        DriverManager.quitDriver();
    }

    /** @return the driver bound to the current thread. */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
