package framework.pages;

import framework.base.BasePage;
import framework.pages.components.HeaderComponent;
import framework.utilities.StepLogger;
import framework.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/** Shopping cart ({@code /cart}): line items, quantity steppers, order summary. */
public class CartPage extends BasePage {

    private final By heading = By.xpath("//h1[normalize-space(.)='Shopping Cart']");
    private final By emptyTitle = By.xpath("//h2[normalize-space(.)='Your cart is empty']");
    private final By browseCollections = By.xpath("//a[normalize-space(.)='Browse Collections']");
    private final By itemNames = By.cssSelector("a[class*='itemName']");
    private final By removeButtons = By.cssSelector("button[aria-label='Remove item']");
    private final By qtyValues = By.cssSelector("span[class*='qtyValue']");
    private final By itemTotals = By.cssSelector("p[class*='itemTotal']");
    private final By itemCountLabel = By.cssSelector("span[class*='itemCount']");
    private final By subtotalValue = By.xpath("//div[contains(@class,'summaryRow')][span[starts-with(normalize-space(.),'Subtotal')]]/span[2]");
    private final By shippingValue = By.xpath("//div[contains(@class,'summaryRow')][span[normalize-space(.)='Shipping']]/span[2]");
    private final By totalValue = By.xpath("//div[contains(@class,'summaryRow') and contains(@class,'total')]/span[2]");
    private final By freeShippingHint = By.xpath("//p[contains(normalize-space(.),'for free shipping')]");
    private final By checkoutButton = By.cssSelector("a[class*='checkoutBtn']");
    private final By continueShopping = By.cssSelector("a[class*='continueShopping']");

    public final HeaderComponent header = new HeaderComponent();

    /** Waits until the cart has rendered either items or the empty state. */
    public CartPage waitForLoad() {
        waitForVisible(heading);
        WaitUtils.waitFor(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(itemNames),
                ExpectedConditions.visibilityOfElementLocated(emptyTitle)));
        return this;
    }

    public boolean isEmpty() {
        return isDisplayed(emptyTitle);
    }

    public void clickBrowseCollections() {
        click(browseCollections);
    }

    public int lineCount() {
        return findAll(itemNames).size();
    }

    public List<String> itemNames() {
        return findAll(itemNames).stream().map(BasePage::textContent).toList();
    }

    /** Full text of each line (name, size/colour, prices) — used to check chosen options. */
    public List<String> lineTexts() {
        return findAll(By.xpath("//a[contains(@class,'itemName')]/ancestor::div[contains(@class,'itemInfo')]"))
                .stream().map(BasePage::textContent).toList();
    }

    public int quantityOfLine(int index) {
        return Integer.parseInt(findAll(qtyValues).get(index).getText().trim());
    }

    public double lineTotal(int index) {
        return ShopPage.parsePrice(findAll(itemTotals).get(index).getText());
    }

    private void clickQtyButton(int index, String sign) {
        By button = By.xpath("(//div[contains(@class,'qtyGroup')])[" + (index + 1) + "]/button[normalize-space(.)='" + sign + "']");
        int before = quantityOfLine(index);
        click(button);
        int expected = "+".equals(sign) ? before + 1 : before - 1;
        if (expected > 0) {
            WaitUtils.waitFor(d -> quantityOfLine(index) == expected);
        }
    }

    public void increaseQuantity(int index) {
        clickQtyButton(index, "+");
    }

    public void decreaseQuantity(int index) {
        clickQtyButton(index, "-");
    }

    public void removeLine(int index) {
        int before = lineCount();
        WebElement button = findAll(removeButtons).get(index);
        scrollToCenter(button);
        button.click();
        WaitUtils.waitFor(d -> findAll(itemNames).size() < before);
        StepLogger.step(log, "Removed cart line #" + index);
    }

    public String itemCountLabel() {
        return getText(itemCountLabel);
    }

    public double subtotal() {
        return ShopPage.parsePrice(getText(subtotalValue));
    }

    public double total() {
        return ShopPage.parsePrice(getText(totalValue));
    }

    public String shippingText() {
        return textContent(shippingValue);
    }

    public boolean isFreeShippingHintShown() {
        return isDisplayed(freeShippingHint, 3);
    }

    public void proceedToCheckout() {
        click(checkoutButton);
    }

    public void continueShopping() {
        click(continueShopping);
    }
}
