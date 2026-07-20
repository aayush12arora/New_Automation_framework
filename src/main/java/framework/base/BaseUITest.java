package framework.base;

import framework.assertions.UIAssertions;
import framework.constants.FrameworkConstants;
import framework.utilities.PropertyManager;
import framework.utilities.StepLogger;

/**
 * Base class for UI tests. Adds UI-specific setup on top of {@link BaseTest}:
 * launching the application and exposing the shared {@link UIAssertions} so tests
 * verify through it instead of calling raw {@code Assert.*}.
 */
public abstract class BaseUITest extends BaseTest {

    /** UI assertion helper (hard + soft) available to every UI test. */
    protected final UIAssertions assertions = new UIAssertions();

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
