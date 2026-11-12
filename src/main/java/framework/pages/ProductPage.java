package framework.pages;

import framework.base.BasePage;
import framework.pages.components.HeaderComponent;
import framework.utilities.StepLogger;
import framework.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * Product detail page ({@code /product/<slug>}): gallery, price, colour + size pickers,
 * Add to Cart / Buy Now, wishlist, info tabs, reviews and recommendations.
 *
 * <p>The page is CSS-module styled, so classes look like {@code Product_sizeBtn__a1B2c}.
 * Locators match on the stable source name ({@code [class*='sizeBtn']}).</p>
 */
public class ProductPage extends BasePage {

    private final By productName = By.cssSelector("h1[class*='name']");
    private final By categoryLabel = By.cssSelector("p[class*='categoryLabel']");
    private final By price = By.cssSelector("[class*='priceBlock'] > span[class*='price']");
    private final By taxNote = By.cssSelector("p[class*='taxNote']");
    private final By sizeButtons = By.cssSelector("button[class*='sizeBtn']");
    private final By colorButtons = By.cssSelector("button[class*='colorBtn']");
    private final By selectedOptionLabels = By.cssSelector("span[class*='optionSelected']");
    private final By addToCartButton = By.cssSelector("[class*='actionRow'] button[class*='addToCart']");
    private final By buyNowButton = By.cssSelector("[class*='actionRow'] button[class*='buyNow']");
    private final By inStock = By.cssSelector("span[class*='inStock']");
    private final By outOfStock = By.cssSelector("span[class*='outOfStock']");
    private final By breadcrumbLinks = By.cssSelector("[class*='breadcrumb'] a");
    private final By sizeGuideLink = By.xpath("//label[contains(@class,'optionLabel')]//a[normalize-space(.)='Size Guide']");
    private final By tabs = By.cssSelector("[class*='tabHeader'] button");
    private final By activeTab = By.cssSelector("[class*='tabHeader'] button[class*='tabActive']");
    private final By tabContent = By.cssSelector("[class*='tabContent']");
    private final By wishlistButton = By.xpath("//div[contains(@class,'secondaryActions')]/button[1]");
    private final By reviewsSection = By.id("reviews");
    private final By reviewLoginPrompt = By.cssSelector("[class*='reviewLoginPrompt']");
    private final By reviewForm = By.cssSelector("form[class*='reviewForm']");
    private final By reviewTextarea = By.cssSelector("textarea[class*='reviewTextarea']");
    private final By submitReview = By.cssSelector("button[class*='submitReviewBtn']");
    private final By notFoundHeading = By.xpath("//h2[normalize-space(.)='Product not found']");
    private final By addedToCartToast = By.xpath("//p[normalize-space(.)='Added to Cart']");
    private final By viewCartInToast = By.xpath("//button[normalize-space(.)='View Cart']");

    public final HeaderComponent header = new HeaderComponent();

    private By toast(String text) {
        return By.xpath("//div[@role='status' and contains(normalize-space(.),\"" + text + "\")]");
    }

    private By sizeButton(String size) {
        return By.xpath("//button[contains(@class,'sizeBtn') and (normalize-space(.)='" + size
                + "' or starts-with(normalize-space(.),'" + size + " '))]");
    }

    private By colorButton(String color) {
        return By.xpath("//button[contains(@class,'colorBtn') and @title='" + color + "']");
    }

    // ------------------------------------------------------------------ Loading

    /** Waits for the product (or the "not found" state) to finish loading client-side. */
    public ProductPage waitForLoad() {
        WaitUtils.waitFor(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(productName),
                ExpectedConditions.visibilityOfElementLocated(notFoundHeading)));
        StepLogger.step(log, "Product page loaded");
        return this;
    }

    // ------------------------------------------------------------------ Details

    public String name() {
        return textContent(productName);
    }

    public String category() {
        return textContent(categoryLabel);
    }

    public double price() {
        return ShopPage.parsePrice(getText(price));
    }

    public boolean isTaxNoteShown() {
        return isDisplayed(taxNote);
    }

    public boolean isNotFoundShown() {
        return isDisplayed(notFoundHeading);
    }

    public boolean isInStockShown() {
        return isDisplayed(inStock, 5);
    }

    public boolean isOutOfStockShown() {
        return isDisplayed(outOfStock, 5);
    }

    // ------------------------------------------------------------------ Options

    public List<String> sizes() {
        return findAll(sizeButtons).stream().map(BasePage::textContent).toList();
    }

    public List<String> colors() {
        return findAll(colorButtons).stream().map(e -> e.getDomAttribute("title")).toList();
    }

    public void selectSize(String size) {
        click(sizeButton(size));
    }

    public void selectSizeAt(int index) {
        selectOption(sizeButtons, index, "sizeActive", "size");
    }

    public void selectColor(String color) {
        click(colorButton(color));
    }

    public void selectColorAt(int index) {
        selectOption(colorButtons, index, "colorActive", "colour");
    }

    /**
     * Clicks the n-th option button until it shows as active. The page re-renders when the
     * signed-in session finishes restoring (the product is re-fetched), which can detach the
     * button or swallow a click, so the button is re-found on every attempt.
     */
    private void selectOption(By buttons, int index, String activeClass, String label) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement button = waitForAll(buttons).get(index);
                scrollToCenter(button);
                js("arguments[0].click();", button);
                WaitUtils.waitFor(d -> {
                    List<WebElement> current = d.findElements(buttons);
                    if (current.size() <= index) {
                        return false; // list is mid re-render
                    }
                    String cls = current.get(index).getDomAttribute("class");
                    return cls != null && cls.contains(activeClass);
                });
                StepLogger.step(log, "Selected %s #%d".formatted(label, index));
                return;
            } catch (StaleElementReferenceException | TimeoutException | IndexOutOfBoundsException e) {
                log.debug("Selecting {} #{} attempt {} did not stick: {}", label, index, attempt, e.getClass().getSimpleName());
            }
        }
        throw new IllegalStateException("Could not select " + label + " #" + index + " after 3 attempts");
    }

    /** Picks the first size and first colour when the product offers them. */
    public void selectFirstOptions() {
        if (!findAll(colorButtons).isEmpty()) {
            selectColorAt(0);
        }
        if (!findAll(sizeButtons).isEmpty()) {
            selectSizeAt(0);
        }
    }

    public boolean isSizeSelected(String size) {
        WebElement button = waitForVisible(sizeButton(size));
        return button.getDomAttribute("class").contains("sizeActive");
    }

    public List<String> selectedOptionLabels() {
        return findAll(selectedOptionLabels).stream().map(BasePage::textContent).toList();
    }

    // ------------------------------------------------------------------ Actions

    public void clickAddToCart() {
        click(addToCartButton);
    }

    public void clickBuyNow() {
        click(buyNowButton);
    }

    /** Selects the first size/colour and adds to cart, waiting for the confirmation toast. */
    public void addToCartWithFirstOptions() {
        selectFirstOptions();
        clickAddToCart();
        waitForVisible(addedToCartToast);
    }

    public boolean isAddToCartEnabled() {
        return waitForVisible(addToCartButton).isEnabled();
    }

    public String addToCartButtonText() {
        return textContent(addToCartButton);
    }

    public boolean isBuyNowShown() {
        return isDisplayed(buyNowButton, 3);
    }

    public boolean isAddedToCartToastShown() {
        return isDisplayed(addedToCartToast);
    }

    public void clickViewCartInToast() {
        click(viewCartInToast);
    }

    public boolean isToastShown(String text) {
        return isDisplayed(toast(text));
    }

    public void toggleWishlist() {
        click(wishlistButton);
    }

    public String wishlistButtonText() {
        return textContent(wishlistButton);
    }

    // ------------------------------------------------------------------ Navigation / info

    public List<String> breadcrumbTexts() {
        return waitForAll(breadcrumbLinks).stream().map(BasePage::textContent).toList();
    }

    public void clickBreadcrumb(String text) {
        click(By.xpath("//*[contains(@class,'breadcrumb')]//a[normalize-space(.)='" + text + "']"));
    }

    public String sizeGuideHref() {
        return getAttribute(sizeGuideLink, "href");
    }

    public List<String> tabNames() {
        return waitForAll(tabs).stream().map(BasePage::textContent).toList();
    }

    public void openTab(String name) {
        click(By.xpath("//*[contains(@class,'tabHeader')]//button[normalize-space(.)='" + name + "']"));
    }

    public String activeTabName() {
        return textContent(activeTab);
    }

    public boolean isTabContentShown() {
        return isDisplayed(tabContent);
    }

    // ------------------------------------------------------------------ Reviews

    public void scrollToReviews() {
        scrollIntoView(reviewsSection);
    }

    public boolean isReviewLoginPromptShown() {
        scrollToReviews();
        return isDisplayed(reviewLoginPrompt);
    }

    public boolean isReviewFormShown() {
        scrollToReviews();
        return isDisplayed(reviewForm, 3);
    }

    public void submitReview(String text) {
        scrollToReviews();
        if (text != null) {
            type(reviewTextarea, text);
        }
        click(submitReview);
    }
}
