package framework.pages;

import framework.base.BasePage;
import framework.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Signed-in customer pages: My Orders ({@code /account/orders}), My Profile
 * ({@code /account/profile}) and an order's detail page ({@code /order/<id>}).
 */
public class AccountPages extends BasePage {

    // My Orders
    private final By ordersHeading = By.xpath("//h1[normalize-space(.)='My Orders']");
    private final By ordersLoginPrompt = By.xpath("//p[normalize-space(.)='Please log in to view your orders.']");
    private final By noOrdersMessage = By.xpath("//p[contains(normalize-space(.),\"haven't placed any orders yet\")]");
    private final By orderLinks = By.cssSelector("a[href^='/order/']");

    // Profile
    private final By profileName = By.name("name");
    private final By profileEmail = By.name("email");
    private final By profilePhone = By.name("phone");
    private final By profileAddress = By.name("address");
    private final By saveProfile = By.xpath("//button[@type='submit'][contains(normalize-space(.),'Save Changes')]");
    private final By viewOrdersButton = By.xpath("//button[contains(normalize-space(.),'View Orders')]");

    // Order detail
    private final By orderNotFound = By.xpath("//h2[normalize-space(.)='Order not found']");

    // ------------------------------------------------------------------ My Orders

    public AccountPages waitForOrders() {
        waitForVisible(ordersHeading);
        WaitUtils.waitFor(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(ordersLoginPrompt),
                ExpectedConditions.visibilityOfElementLocated(noOrdersMessage),
                ExpectedConditions.visibilityOfElementLocated(orderLinks)));
        return this;
    }

    public boolean isOrdersLoginPromptShown() {
        return isDisplayed(ordersLoginPrompt, 5);
    }

    public boolean isNoOrdersMessageShown() {
        return isDisplayed(noOrdersMessage, 5);
    }

    // ------------------------------------------------------------------ Profile

    /** Waits until the profile form has been populated from the API. */
    public AccountPages waitForProfile() {
        WaitUtils.waitFor(d -> {
            String v = d.findElements(profileEmail).isEmpty() ? "" : d.findElement(profileEmail).getDomProperty("value");
            return v != null && !v.isBlank();
        });
        return this;
    }

    public String profileNameValue() {
        return getProperty(profileName, "value");
    }

    public String profileEmailValue() {
        return getProperty(profileEmail, "value");
    }

    public boolean isProfileEmailEditable() {
        return waitForVisible(profileEmail).isEnabled()
                && getAttribute(profileEmail, "readonly") == null;
    }

    public void setProfilePhone(String phone) {
        clearReactInput(profilePhone);
        type(profilePhone, phone);
    }

    public void setProfileAddress(String address) {
        clearReactInput(profileAddress);
        type(profileAddress, address);
    }

    public String profilePhoneValue() {
        return getProperty(profilePhone, "value");
    }

    public void saveProfile() {
        click(saveProfile);
    }

    public void clickViewOrders() {
        click(viewOrdersButton);
    }

    public boolean isToastShown(String text) {
        return isDisplayed(By.xpath("//div[@role='status' and contains(normalize-space(.),\"" + text + "\")]"));
    }

    // ------------------------------------------------------------------ Order detail

    public boolean isOrderNotFoundShown() {
        return isDisplayed(orderNotFound);
    }
}
