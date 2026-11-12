package framework.pages.components;

import framework.base.BasePage;
import framework.utilities.StepLogger;
import framework.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;

/**
 * The storefront's top navigation bar (LandingNavbar): logo, product search with live
 * suggestions, desktop nav links, cart icon + badge, and the login / profile menu.
 */
public class HeaderComponent extends BasePage {

    private final By logo = By.cssSelector(".landing-nav-main a[href='/']");
    private final By searchInput = By.cssSelector("input.b-search-input");
    private final By searchSuggestion = By.cssSelector("[data-search-item]");
    private final By viewAllResults = By.cssSelector("button.b-view-all");
    private final By cartIcon = By.cssSelector("a.landing-icon-btn[aria-label='Cart']");
    private final By cartBadge = By.cssSelector(".landing-cart-badge");
    private final By loginIcon = By.cssSelector("a[aria-label='Login']");
    private final By profileButton = By.cssSelector("button[aria-label='Profile']");
    private final By profileMenu = By.cssSelector(".landing-dropdown-menu");
    private final By navLinks = By.cssSelector(".desktop-nav-links a.landing-nav-link");

    private By navLink(String text) {
        return By.xpath("//div[contains(@class,'desktop-nav-links')]//a[normalize-space(.)='" + text + "']");
    }

    private By profileMenuItem(String text) {
        return By.xpath("//div[contains(@class,'landing-dropdown-menu')]//*[normalize-space(.)='" + text + "']");
    }

    // ------------------------------------------------------------------ Navigation

    public void clickLogo() {
        click(logo);
    }

    public void clickNavLink(String text) {
        click(navLink(text));
    }

    public List<String> navLinkTexts() {
        return waitForAll(navLinks).stream().map(BasePage::textContent).toList();
    }

    public void openCart() {
        click(cartIcon);
    }

    // ------------------------------------------------------------------ Search

    public void search(String query) {
        type(searchInput, query);
    }

    public void submitSearch() {
        pressEnter(searchInput);
    }

    public boolean areSuggestionsShown() {
        return isDisplayed(searchSuggestion);
    }

    /** Short-timeout variant, for asserting that no suggestions appear. */
    public boolean areSuggestionsShown(int seconds) {
        return isDisplayed(searchSuggestion, seconds);
    }

    public List<String> suggestionTexts() {
        return waitForAll(searchSuggestion).stream().map(BasePage::textContent).toList();
    }

    public void openFirstSuggestion() {
        click(searchSuggestion);
    }

    public void clickViewAllResults() {
        click(viewAllResults);
    }


    // ------------------------------------------------------------------ Cart badge

    /** Current cart badge count; 0 when no badge is shown. */
    public int cartCount() {
        List<WebElement> badges = findAll(cartBadge);
        if (badges.isEmpty() || badges.get(0).getText().isBlank()) {
            return 0;
        }
        return Integer.parseInt(badges.get(0).getText().trim());
    }

    /** Waits until the cart badge shows {@code expected}. */
    public void waitForCartCount(int expected) {
        WaitUtils.waitFor(d -> cartCount() == expected);
        StepLogger.step(log, "Cart badge shows " + expected);
    }

    // ------------------------------------------------------------------ Account

    public boolean isLoginIconShown() {
        return isDisplayed(loginIcon);
    }

    public void clickLoginIcon() {
        click(loginIcon);
    }

    public boolean isSignedIn() {
        return isDisplayed(profileButton);
    }

    public boolean isSignedIn(int seconds) {
        return isDisplayed(profileButton, seconds);
    }

    /** Opens the profile dropdown (it is shown on hover / focus). */
    public void openProfileMenu() {
        // Toasts sit in the top-right corner over the profile icon; let them clear first.
        WaitUtils.waitForInvisible(By.cssSelector("[role='status']"));
        WebElement button = waitForVisible(profileButton);
        new Actions(getDriver()).moveToElement(button).perform();
        WaitUtils.waitForVisible(profileMenu);
        StepLogger.step(log, "Opened profile menu");
    }

    public void chooseProfileMenuItem(String text) {
        openProfileMenu();
        click(profileMenuItem(text));
    }

    public void logout() {
        chooseProfileMenuItem("Logout");
    }
}
