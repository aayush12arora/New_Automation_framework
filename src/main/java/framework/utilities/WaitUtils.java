package framework.utilities;

import framework.base.DriverManager;
import framework.constants.FrameworkConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

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
        return newWait(explicitWait);
    }

    private static WebDriverWait newWait(int seconds) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(seconds));
    }

    public static WebElement waitForVisible(By locator) {
        return newWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForVisible(By locator, int seconds) {
        return newWait(seconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        return newWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForPresent(By locator) {
        return newWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static List<WebElement> waitForAllVisible(By locator) {
        return newWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static boolean waitForInvisible(By locator) {
        return newWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForUrlContains(String fragment) {
        return newWait().until(ExpectedConditions.urlContains(fragment));
    }

    public static boolean waitForTextIn(By locator, String text) {
        return newWait().until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /** Waits until an arbitrary condition returns a non-null / non-false value. */
    public static <T> T waitFor(Function<WebDriver, T> condition) {
        return newWait().until(condition);
    }

    /** Waits until {@code document.readyState == 'complete'}. */
    public static void waitForPageLoad() {
        newWait().until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }
}
