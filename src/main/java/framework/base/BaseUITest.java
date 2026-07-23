package framework.base;

import framework.constants.FrameworkConstants;
import framework.utilities.PropertyManager;
import framework.utilities.StepLogger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for UI tests. Adds the browser lifecycle on top of {@link BaseTest}: it creates
 * the WebDriver and launches the application before every test, and quits the driver after.
 *
 * <p>TestNG runs base-class {@code @BeforeMethod} first, so {@link BaseTest#baseSetUp} (report
 * node + data) runs before {@link #launchApplication()} here, and the {@code @AfterMethod}
 * order is reversed so the driver is quit before {@link BaseTest#baseTearDown}. The driver is
 * still alive when the {@link framework.reporting.TestListener} takes its final pass/fail
 * screenshot (that runs before {@code @AfterMethod}).</p>
 *
 * <p><b>Launch is automatic</b> — test classes write only {@code @Test} methods. They may pass
 * a custom launch URL via {@code super(url)}; otherwise the URL from {@code framework.properties}
 * is used. The constructor only <em>configures</em> the URL; the driver is created here, in a
 * {@code @BeforeMethod}, because it cannot exist at construction time.</p>
 */
public abstract class BaseUITest extends BaseTest {

    /** Custom launch URL supplied via {@code super(url)}; {@code null} uses the properties URL. */
    private final String launchUrl;

    protected BaseUITest() {
        this(null);
    }

    protected BaseUITest(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    @BeforeMethod(alwaysRun = true)
    public void launchApplication() {
        DriverManager.setDriver();
        String url = (launchUrl != null) ? launchUrl : PropertyManager.get(FrameworkConstants.URL);
        getDriver().get(url);
        StepLogger.step(log, "Launched application, navigated to: " + url);
    }

    @AfterMethod(alwaysRun = true)
    public void quitDriver() {
        StepLogger.step(log, "Quitting driver");
        DriverManager.quitDriver();
    }

    /** Navigates to an arbitrary URL. */
    protected void navigateTo(String url) {
        getDriver().get(url);
        StepLogger.step(log, "Navigated to: " + url);
    }
}
