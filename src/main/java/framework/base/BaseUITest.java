package framework.base;

import framework.assertions.UIAssertions;
import framework.constants.FrameworkConstants;
import framework.utilities.PropertyManager;
import framework.utilities.StepLogger;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for UI tests. Adds UI-specific setup on top of {@link BaseTest}:
 * it launches the application before every test and exposes the shared
 * {@link UIAssertions} so tests verify through it instead of calling raw {@code Assert.*}.
 *
 * <p><b>Launch happens automatically</b> — test classes do not write their own
 * {@code @BeforeMethod}. They may optionally call {@code super(url)} from their
 * constructor to launch a custom URL; otherwise the URL from {@code framework.properties}
 * is used. The constructor only <em>configures</em> the URL: the actual navigation runs
 * in {@link #launchApplication()} once the driver exists (the driver is created per test
 * in {@link BaseTest#setUp}, so it cannot be used from a constructor).</p>
 */
public abstract class BaseUITest extends BaseTest {

    /** UI assertion helper (hard + soft) available to every UI test. */
    protected final UIAssertions assertions = new UIAssertions();

    /** Custom launch URL supplied via {@code super(url)}; {@code null} uses the properties URL. */
    private final String launchUrl;

    protected BaseUITest() {
        this(null);
    }

    protected BaseUITest(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    /**
     * Launches the application before every test. Runs after {@link BaseTest#setUp}
     * (TestNG runs base-class {@code @BeforeMethod} first), so the driver already exists.
     */
    @BeforeMethod(alwaysRun = true)
    public void launchApplication() {
        String url = (launchUrl != null) ? launchUrl : PropertyManager.get(FrameworkConstants.URL);
        getDriver().get(url);
        StepLogger.step(log, "Launched application, navigated to: " + url);
    }

    /** Navigates to an arbitrary URL. */
    protected void navigateTo(String url) {
        getDriver().get(url);
        StepLogger.step(log, "Navigated to: " + url);
    }
}
