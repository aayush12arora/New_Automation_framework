package framework.base;

import framework.utilities.LoggerUtil;
import framework.utilities.WaitUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;

/**
 * Parent of every Page Object.
 * Provides reusable, logged Selenium wrappers. Page Objects extend this class
 * and never receive a {@link org.openqa.selenium.WebDriver} through their constructor —
 * the driver is always obtained from {@link DriverManager}.
 */
public abstract class BasePage {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    /** @return the driver bound to the current thread. */
    protected org.openqa.selenium.WebDriver driver() {
        return DriverManager.getDriver();
    }

    protected void click(By locator) {
        log.info("Click: {}", locator);
        WaitUtils.waitForClickable(locator).click();
    }

    protected void type(By locator, String text) {
        log.info("Type '{}' into: {}", text, locator);
        WebElement element = WaitUtils.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected void clear(By locator) {
        log.info("Clear: {}", locator);
        WaitUtils.waitForVisible(locator).clear();
    }

    protected String getText(By locator) {
        String text = WaitUtils.waitForVisible(locator).getText();
        log.info("Get text '{}' from: {}", text, locator);
        return text;
    }

    protected boolean isDisplayed(By locator) {
        try {
            return WaitUtils.waitForVisible(locator).isDisplayed();
        } catch (RuntimeException e) {
            log.warn("Element not displayed: {}", locator);
            return false;
        }
    }

    protected WebElement waitForVisible(By locator) {
        return WaitUtils.waitForVisible(locator);
    }

    protected WebElement waitForClickable(By locator) {
        return WaitUtils.waitForClickable(locator);
    }

    protected void scrollIntoView(By locator) {
        log.info("Scroll into view: {}", locator);
        WebElement element = WaitUtils.waitForVisible(locator);
        ((JavascriptExecutor) driver()).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    protected void jsClick(By locator) {
        log.info("JS click: {}", locator);
        WebElement element = WaitUtils.waitForVisible(locator);
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
    }

    protected void selectByVisibleText(By locator, String visibleText) {
        log.info("Select '{}' from: {}", visibleText, locator);
        new Select(WaitUtils.waitForVisible(locator)).selectByVisibleText(visibleText);
    }

    protected void acceptAlert() {
        log.info("Accept alert");
        alert().accept();
    }

    protected void dismissAlert() {
        log.info("Dismiss alert");
        alert().dismiss();
    }

    protected void switchToFrame(By locator) {
        log.info("Switch to frame: {}", locator);
        driver().switchTo().frame(WaitUtils.waitForVisible(locator));
    }

    protected void switchToDefault() {
        log.info("Switch to default content");
        driver().switchTo().defaultContent();
    }

    private Alert alert() {
        return driver().switchTo().alert();
    }
}
