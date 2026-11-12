package framework.pages;

import framework.base.BasePage;
import framework.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * Checkout ({@code /checkout}, signed-in only): shipping form, delivery check, coupon,
 * GST breakup and "Place Order &amp; Pay" (which opens Razorpay).
 */
public class CheckoutPage extends BasePage {

    private final By heading = By.xpath("//h1[normalize-space(.)='Checkout']");
    private final By shippingSection = By.xpath("//h2[normalize-space(.)='Shipping Information']");
    private final By emptyCartMessage = By.xpath("//p[normalize-space(.)='Your cart is empty.']");
    private final By phone = By.name("shippingPhone");
    private final By address = By.name("shippingAddress");
    private final By city = By.name("shippingCity");
    private final By state = By.name("shippingState");
    private final By zip = By.name("shippingZip");
    private final By country = By.name("shippingCountry");
    private final By summaryLines = By.xpath("//h2[normalize-space(.)='Order Summary']/following-sibling::div[contains(@class,'summaryRow')]/span[1]");
    private final By couponInput = By.cssSelector("input[aria-label='Discount code']");
    private final By couponApply = By.xpath("//input[@aria-label='Discount code']/following-sibling::button");
    /** Scoped to the coupon box: Next.js's route announcer also uses role="alert". */
    private final By couponError = By.xpath("//input[@aria-label='Discount code']/ancestor::div[2]//div[@role='alert']");
    private final By taxableAmount = By.xpath("//span[normalize-space(.)='Taxable Amount']");
    private final By cgstLabel = By.xpath("//span[starts-with(normalize-space(.),'CGST')]");
    private final By sgstLabel = By.xpath("//span[starts-with(normalize-space(.),'SGST')]");
    private final By igstLabel = By.xpath("//span[starts-with(normalize-space(.),'IGST')]");
    private final By totalValue = By.xpath("//div[contains(@class,'total')]//div[contains(@class,'summaryRow')][span[normalize-space(.)='Total']]/span[2]");
    private final By placeOrderButton = By.xpath("//form//button[@type='submit'][contains(normalize-space(.),'Place Order') or normalize-space(.)='Processing...']");
    private final By deliveryAvailable = By.xpath("//*[contains(normalize-space(text()),'Delivery available')]");
    private final By deliveryUnavailable = By.xpath("//*[contains(normalize-space(text()),'not available to this pincode')]");
    private final By razorpayFrame = By.cssSelector("iframe.razorpay-checkout-frame");
    private final By paymentFailedHeading = By.xpath("//h2[normalize-space(.)='Payment Failed']");

    public CheckoutPage waitForLoad() {
        WaitUtils.waitFor(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(shippingSection),
                ExpectedConditions.visibilityOfElementLocated(emptyCartMessage)));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(heading) && isDisplayed(shippingSection);
    }

    public boolean isEmptyCartMessageShown() {
        return isDisplayed(emptyCartMessage);
    }

    public List<String> summaryLineTexts() {
        return waitForAll(summaryLines).stream().map(BasePage::textContent).toList();
    }

    // ------------------------------------------------------------------ Shipping form

    public void fillShipping(String phoneNo, String street, String cityName, String stateName, String pincode) {
        type(phone, phoneNo);
        type(address, street);
        type(city, cityName);
        selectState(stateName);
        type(zip, pincode);
    }

    public void selectState(String stateName) {
        selectByVisibleText(state, stateName);
    }

    public List<String> stateOptions() {
        return new Select(waitForVisible(state)).getOptions().stream().map(WebElement::getText).toList();
    }

    public String countryValue() {
        return getProperty(country, "value");
    }

    public String phoneValidationMessage() {
        return validationMessage(phone);
    }

    public void enterPincode(String pincode) {
        type(zip, pincode);
    }

    /** True if a serviceability verdict (available or not) appears for the pincode. */
    public boolean deliveryVerdictShown() {
        try {
            WaitUtils.waitFor(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(deliveryAvailable),
                    ExpectedConditions.visibilityOfElementLocated(deliveryUnavailable)));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ Coupon

    public void applyCoupon(String code) {
        type(couponInput, code);
        click(couponApply);
    }

    public boolean isApplyCouponEnabled() {
        return waitForVisible(couponApply).isEnabled();
    }

    public String couponError() {
        return getText(couponError);
    }

    // ------------------------------------------------------------------ Totals

    public boolean isTaxableAmountShown() {
        return isDisplayed(taxableAmount);
    }

    public boolean isCgstSgstShown() {
        return isDisplayed(cgstLabel, 5) && isDisplayed(sgstLabel, 5);
    }

    public boolean isIgstShown() {
        return isDisplayed(igstLabel, 5);
    }

    public double total() {
        return ShopPage.parsePrice(getText(totalValue));
    }

    // ------------------------------------------------------------------ Place order

    public void placeOrder() {
        click(placeOrderButton);
    }

    /**
     * Waits for the outcome of "Place Order &amp; Pay": either Razorpay's window becomes visible
     * (checkout.js preloads a hidden iframe, so presence alone proves nothing) or the
     * "Payment Failed" state appears. Returns true only when Razorpay is shown.
     */
    public boolean isRazorpayCheckoutOpened() {
        try {
            WaitUtils.waitFor(d -> {
                boolean frameShown = d.findElements(razorpayFrame).stream().anyMatch(e -> e.isDisplayed());
                return frameShown || !d.findElements(paymentFailedHeading).isEmpty();
            });
        } catch (RuntimeException e) {
            return false;
        }
        return findAll(razorpayFrame).stream().anyMatch(e -> e.isDisplayed());
    }

    /** Reason shown under "Payment Failed" ("" when not failed). */
    public String paymentFailureReason() {
        By reason = By.xpath("//h2[normalize-space(.)='Payment Failed']/following-sibling::p[1]");
        return isPresentNow(reason) ? textContent(reason) : "";
    }

    public boolean isPaymentFailedShown() {
        return isDisplayed(paymentFailedHeading, 3);
    }
}
