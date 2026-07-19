package framework.base;

import framework.constants.FrameworkConstants;
import framework.utilities.PropertyManager;

/**
 * Base class for UI tests. Adds UI-specific setup on top of {@link BaseTest}:
 * launching the application under test by navigating to the configured URL.
 */
public abstract class BaseUITest extends BaseTest {

    /** Launches the application by navigating to the URL from framework.properties. */
    protected void initializeApplication() {
        String url = PropertyManager.get(FrameworkConstants.URL);
        log.info("Navigating to application URL: {}", url);
        getDriver().get(url);
    }

    /** Navigates to an arbitrary URL. */
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        getDriver().get(url);
    }
}
