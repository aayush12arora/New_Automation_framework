package framework.base;

import framework.constants.FrameworkConstants;
import framework.utilities.PropertyManager;
import framework.utilities.StepLogger;

/**
 * Base class for UI tests. Adds UI-specific setup on top of {@link BaseTest}:
 * launching the application under test by navigating to the configured URL.
 */
public abstract class BaseUITest extends BaseTest {

    /** Launches the application by navigating to the URL from framework.properties. */
    protected void initializeApplication() {
        String url = PropertyManager.get(FrameworkConstants.URL);
        getDriver().get(url);
        StepLogger.step(log, "Launched application, navigated to: " + url);
    }

    /** Navigates to an arbitrary URL. */
    protected void navigateTo(String url) {
        getDriver().get(url);
        StepLogger.step(log, "Navigated to: " + url);
    }
}
