package framework.base;

import framework.utilities.LoggerUtil;
import framework.utilities.StepLogger;
import framework.utilities.WaitUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;

/**
 * Parent of every Page Object.
 * Provides reusable Selenium wrappers. Each wrapper records a step (log + Extent
 * report + screenshot) via {@link StepLogger}. Page Objects extend this class and
 * never receive a {@link org.openqa.selenium.WebDriver} through their constructor —
 * the driver is always obtained from {@link DriverManager}.
 */
public abstract class BasePage {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    /** @return the driver bound to the current thread. */
    protected org.openqa.selenium.WebDriver driver() {
        return DriverManager.getDriver();
    }

    protected void click(By locator) {
        WaitUtils.waitForClickable(locator).click();
        StepLogger.step(log, "Clicked: " + locator);
    }

    protected void type(By locator, String text) {
        WebElement element = WaitUtils.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
        StepLogger.step(log, "Typed '%s' into: %s".formatted(text, locator));
    }

    protected void clear(By locator) {
        WaitUtils.waitForVisible(locator).clear();
        StepLogger.step(log, "Cleared: " + locator);
    }

    protected String getText(By locator) {
        String text = WaitUtils.waitForVisible(locator).getText();
        StepLogger.step(log, "Got text '%s' from: %s".formatted(text, locator));
        return text;
    }

    protected boolean isDisplayed(By locator) {
        boolean displayed;
        try {
            displayed = WaitUtils.waitForVisible(locator).isDisplayed();
        } catch (RuntimeException e) {
            displayed = false;
        }
        StepLogger.step(log, "Is displayed [%s]: %s".formatted(displayed, locator));
        return displayed;
    }

    protected WebElement waitForVisible(By locator) {
        WebElement element = WaitUtils.waitForVisible(locator);
        StepLogger.step(log, "Waited for visible: " + locator);
        return element;
    }

    protected WebElement waitForClickable(By locator) {
        WebElement element = WaitUtils.waitForClickable(locator);
        StepLogger.step(log, "Waited for clickable: " + locator);
        return element;
    }

    protected void scrollIntoView(By locator) {
        WebElement element = WaitUtils.waitForVisible(locator);
        ((JavascriptExecutor) driver()).executeScript("arguments[0].scrollIntoView(true);", element);
        StepLogger.step(log, "Scrolled into view: " + locator);
    }

    protected void jsClick(By locator) {
        WebElement element = WaitUtils.waitForVisible(locator);
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
        StepLogger.step(log, "JS clicked: " + locator);
    }

    protected void selectByVisibleText(By locator, String visibleText) {
        new Select(WaitUtils.waitForVisible(locator)).selectByVisibleText(visibleText);
        StepLogger.step(log, "Selected '%s' from: %s".formatted(visibleText, locator));
    }

    protected void acceptAlert() {
        alert().accept();
        StepLogger.step(log, "Accepted alert");
    }

    protected void dismissAlert() {
        alert().dismiss();
        StepLogger.step(log, "Dismissed alert");
    }

    protected void switchToFrame(By locator) {
        driver().switchTo().frame(WaitUtils.waitForVisible(locator));
        StepLogger.step(log, "Switched to frame: " + locator);
    }

    protected void switchToDefault() {
        driver().switchTo().defaultContent();
        StepLogger.step(log, "Switched to default content");
    }

    private Alert alert() {
        return driver().switchTo().alert();
    }
}
