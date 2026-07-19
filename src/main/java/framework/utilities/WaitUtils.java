package framework.utilities;

import framework.base.DriverManager;
import framework.constants.FrameworkConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Explicit-wait helpers built on {@link WebDriverWait}.
 * No static sleeps — every wait is condition based.
 */
public final class WaitUtils {

    private WaitUtils() {
        // Prevent instantiation.
    }

    private static WebDriverWait newWait() {
        int explicitWait = PropertyManager.getInt(FrameworkConstants.EXPLICIT_WAIT);
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(explicitWait));
    }

    public static WebElement waitForVisible(By locator) {
        return newWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        return newWait().until(ExpectedConditions.elementToBeClickable(locator));
    }
}
