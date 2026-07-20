package framework.base;

import framework.reporting.ExtentReportManager;
import framework.utilities.LoggerUtil;
import framework.utilities.StepLogger;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

/**
 * Root test class. Owns only framework initialisation:
 * report node + driver creation before each test and teardown after each test.
 * UI- or API-specific setup belongs in subclasses.
 */
public abstract class BaseTest {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        // Create the report node first so every subsequent step is captured.
        ExtentReportManager.createTest(getClass().getSimpleName() + "." + method.getName());
        StepLogger.step(log, "=== Test setup: initialising driver ===");
        DriverManager.setDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        StepLogger.step(log, "=== Test teardown: quitting driver ===");
        DriverManager.quitDriver();
        ExtentReportManager.remove();
    }

    /** @return the driver bound to the current thread. */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
