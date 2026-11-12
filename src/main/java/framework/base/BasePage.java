package framework.base;

import framework.utilities.LoggerUtil;
import framework.utilities.StepLogger;
import framework.utilities.WaitUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;

import java.util.List;

/**
 * Parent of every Page Object.
 * Provides reusable Selenium wrappers. Each wrapper records a step (log + Extent
 * report + screenshot) via {@link StepLogger}. Page Objects extend this class and
 * never receive a {@link org.openqa.selenium.WebDriver} through their constructor —
 * the driver is inherited from {@link DriverContext} via {@link #getDriver()}.
 */
public abstract class BasePage extends DriverContext {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    /**
     * Clicks an element. Scrolls it to the centre of the viewport first (keeps it clear of
     * fixed headers) and falls back to a JavaScript click if something still overlays it.
     */
    protected void click(By locator) {
        WebElement element = WaitUtils.waitForClickable(locator);
        scrollToCenter(element);
        try {
            element.click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
            log.debug("Native click blocked on {}, using JS click", locator);
            js("arguments[0].click();", WaitUtils.waitForClickable(locator));
        }
        StepLogger.step(log, "Clicked: " + locator);
    }

    protected void type(By locator, String text) {
        WebElement element = WaitUtils.waitForVisible(locator);
        scrollToCenter(element);
        element.clear();
        element.sendKeys(text);
        StepLogger.step(log, "Typed '%s' into: %s".formatted(text, locator));
    }

    /** Clears a React-controlled input reliably (select-all + delete fires the change event). */
    protected void clearReactInput(By locator) {
        WebElement element = WaitUtils.waitForVisible(locator);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        StepLogger.step(log, "Cleared: " + locator);
    }

    protected void pressEnter(By locator) {
        WaitUtils.waitForVisible(locator).sendKeys(Keys.ENTER);
        StepLogger.step(log, "Pressed ENTER in: " + locator);
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

    /**
     * The element's DOM text ({@code textContent}), trimmed. Unlike {@link #getText(By)} it is
     * not affected by CSS {@code text-transform}, so it matches the data (the storefront
     * renders many labels in uppercase).
     */
    protected String textContent(By locator) {
        return textContent(WaitUtils.waitForVisible(locator));
    }

    protected static String textContent(WebElement element) {
        String text = element.getDomProperty("textContent");
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    protected String getAttribute(By locator, String attribute) {
        return WaitUtils.waitForPresent(locator).getDomAttribute(attribute);
    }

    protected String getProperty(By locator, String property) {
        return WaitUtils.waitForPresent(locator).getDomProperty(property);
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

    /** Like {@link #isDisplayed(By)} but with a custom (usually short) timeout. */
    protected boolean isDisplayed(By locator, int seconds) {
        try {
            return WaitUtils.waitForVisible(locator, seconds).isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Immediate check with no waiting — for asserting something is absent. */
    protected boolean isPresentNow(By locator) {
        return !getDriver().findElements(locator).isEmpty();
    }

    protected List<WebElement> findAll(By locator) {
        return getDriver().findElements(locator);
    }

    /** Waits for at least one match, then returns all matches. */
    protected List<WebElement> waitForAll(By locator) {
        WaitUtils.waitForVisible(locator);
        return getDriver().findElements(locator);
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

    protected void waitForUrlContains(String fragment) {
        WaitUtils.waitForUrlContains(fragment);
        StepLogger.step(log, "URL now contains: " + fragment);
    }

    protected void scrollIntoView(By locator) {
        WebElement element = WaitUtils.waitForVisible(locator);
        scrollToCenter(element);
        StepLogger.step(log, "Scrolled into view: " + locator);
    }

    protected void scrollToCenter(WebElement element) {
        js("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
    }

    protected void jsClick(By locator) {
        WebElement element = WaitUtils.waitForPresent(locator);
        js("arguments[0].click();", element);
        StepLogger.step(log, "JS clicked: " + locator);
    }

    protected void selectByVisibleText(By locator, String visibleText) {
        new Select(WaitUtils.waitForVisible(locator)).selectByVisibleText(visibleText);
        StepLogger.step(log, "Selected '%s' from: %s".formatted(visibleText, locator));
    }

    protected Object js(String script, Object... args) {
        return ((JavascriptExecutor) getDriver()).executeScript(script, args);
    }

    public String currentUrl() {
        return getDriver().getCurrentUrl();
    }

    public String pageTitle() {
        return getDriver().getTitle();
    }

    /** Reads a value from the page's localStorage (null when absent). */
    public String localStorageItem(String key) {
        Object value = js("return window.localStorage.getItem(arguments[0]);", key);
        return value == null ? null : value.toString();
    }

    /** HTML5 constraint-validation message of a form field ("" when the field is valid). */
    public String validationMessage(By locator) {
        Object value = js("return arguments[0].validationMessage;", WaitUtils.waitForPresent(locator));
        return value == null ? "" : value.toString();
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
        getDriver().switchTo().frame(WaitUtils.waitForVisible(locator));
        StepLogger.step(log, "Switched to frame: " + locator);
    }

    protected void switchToDefault() {
        getDriver().switchTo().defaultContent();
        StepLogger.step(log, "Switched to default content");
    }

    private Alert alert() {
        return getDriver().switchTo().alert();
    }
}
