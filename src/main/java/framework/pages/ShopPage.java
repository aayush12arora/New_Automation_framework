package framework.pages;

import framework.base.BasePage;
import framework.pages.components.HeaderComponent;
import framework.utilities.StepLogger;
import framework.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * Product listing page ({@code /products}) — desktop layout with the filter sidebar
 * (search, collections, price range, sort) and the infinite-scroll product grid.
 *
 * <p>Filter changes re-fetch after a 300 ms debounce and swap the grid for a spinner, so
 * every filter action goes through {@link #refreshing(Runnable)}, which waits for the old
 * grid to detach and the new results (or the empty state) to render.</p>
 */
public class ShopPage extends BasePage {

    private final By productCards = By.cssSelector("a.bl-product-card-link");
    private final By cardNames = By.cssSelector("a.bl-product-card-link .bl-card-info h3");
    private final By cardPrices = By.cssSelector("a.bl-product-card-link .bl-card-info span");
    private final By cardCategories = By.cssSelector("a.bl-product-card-link .bl-card-category");
    private final By productGrid = By.cssSelector(".bl-product-grid");
    private final By emptyState = By.cssSelector(".bl-empty-state");
    private final By emptyStateHeading = By.cssSelector(".bl-empty-state h3");
    private final By emptyStateClearButton = By.xpath("//div[contains(@class,'bl-empty-state')]//button[normalize-space(.)='Clear Filters']");
    private final By sidebarSearch = By.cssSelector("aside input[placeholder='Search pieces...']");
    private final By sortDropdownButton = By.xpath("//aside//h3[normalize-space(.)='Sort By']/following-sibling::div[1]/button");
    private final By clearAllButton = By.xpath("//aside//button[contains(normalize-space(.),'Clear all')]");
    private final By activeFilterLabel = By.xpath("//aside//span[contains(normalize-space(.),'active')]");
    private final By pageHeading = By.cssSelector("main h1");

    public final HeaderComponent header = new HeaderComponent();

    private By categoryButton(String name) {
        return By.xpath("//aside//h3[normalize-space(.)='Collections']/following-sibling::div[1]/button[normalize-space(.)='" + name + "']");
    }

    private By sortOption(String label) {
        return By.xpath("//aside//button[normalize-space(.)='" + label + "']");
    }

    // ------------------------------------------------------------------ Loading

    /** Waits until the grid or the empty state has rendered. */
    public ShopPage waitForResults() {
        WaitUtils.waitFor(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(productGrid),
                ExpectedConditions.visibilityOfElementLocated(emptyState)));
        StepLogger.step(log, "Shop results rendered");
        return this;
    }

    /** Runs a filter action and waits for the result list to be replaced. */
    private void refreshing(Runnable action) {
        waitForResults();
        WebElement current = findAll(productGrid).isEmpty() ? findAll(emptyState).get(0) : findAll(productGrid).get(0);
        action.run();
        try {
            WaitUtils.waitFor(ExpectedConditions.stalenessOf(current));
        } catch (RuntimeException e) {
            log.debug("Result list was not replaced (same results); continuing");
        }
        waitForResults();
    }

    // ------------------------------------------------------------------ Results

    public int productCount() {
        return findAll(productCards).size();
    }

    public List<String> productNames() {
        return findAll(cardNames).stream().map(BasePage::textContent).toList();
    }

    public List<Double> productPrices() {
        return findAll(cardPrices).stream().map(e -> parsePrice(e.getText())).toList();
    }

    public List<String> productCategories() {
        return findAll(cardCategories).stream().map(BasePage::textContent).toList();
    }

    public List<String> productHrefs() {
        return findAll(productCards).stream().map(e -> e.getDomAttribute("href")).toList();
    }

    public void openProduct(int index) {
        WebElement card = findAll(productCards).get(index);
        scrollToCenter(card);
        js("arguments[0].click();", card);
        StepLogger.step(log, "Opened product card #" + index);
    }

    public void openProductBySlug(String slug) {
        click(By.cssSelector("a.bl-product-card-link[href='/product/" + slug + "']"));
    }

    public boolean isEmptyStateShown() {
        return isDisplayed(emptyStateHeading);
    }

    public String emptyStateHeading() {
        return textContent(emptyStateHeading);
    }

    public String pageHeading() {
        return getProperty(pageHeading, "textContent").trim();
    }

    // ------------------------------------------------------------------ Filters

    public void searchInSidebar(String query) {
        refreshing(() -> type(sidebarSearch, query));
    }

    public void selectCategory(String name) {
        refreshing(() -> click(categoryButton(name)));
    }

    public boolean isCategoryOffered(String name) {
        return isDisplayed(categoryButton(name), 5);
    }

    public void sortBy(String label) {
        refreshing(() -> {
            click(sortDropdownButton);
            click(sortOption(label));
        });
    }

    public String currentSortLabel() {
        return textContent(sortDropdownButton);
    }

    public String activeFilterLabel() {
        return textContent(activeFilterLabel);
    }

    public boolean isActiveFilterLabelShown() {
        return isDisplayed(activeFilterLabel, 3);
    }

    public void clearAllFromSidebar() {
        refreshing(() -> click(clearAllButton));
    }

    public void clearFiltersFromEmptyState() {
        refreshing(() -> click(emptyStateClearButton));
    }

    public static double parsePrice(String text) {
        String digits = text.replaceAll("[^0-9.]", "");
        return digits.isEmpty() ? 0 : Double.parseDouble(digits);
    }
}
