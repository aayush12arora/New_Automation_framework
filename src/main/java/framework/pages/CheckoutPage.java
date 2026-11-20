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

    /** Courier under "Delivery available" ("Ships via X · Surface delivery"); "" when not shown. */
    public String shipsViaCourier() {
        By courier = By.xpath("//div[starts-with(normalize-space(.),'Ships via')]/strong");
        return isDisplayed(courier, 5) ? textContent(courier) : "";
    }

    /** Text of the delivery estimate under "Delivery available" (date or days in transit). */
    public String deliveryEstimateText() {
        By estimate = By.xpath("//*[contains(normalize-space(text()),'Delivery available')]/following-sibling::div[1]");
        return isDisplayed(estimate, 5) ? textContent(estimate) : "";
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

    // ------------------------------------------------------------------ Payment method (prepaid / COD)

    private static final String PAY_ONLINE = "Pay online";
    private static final String COD = "Cash on delivery";
    private final By paymentGroup = By.cssSelector("div[role='radiogroup'][aria-label='Payment method']");
    private final By paymentOptionTitles = By.xpath("//div[@role='radiogroup'][@aria-label='Payment method']//label//strong");
    private final By prepaidDiscountLabel = By.xpath("//div[contains(@class,'summaryRow')]/span[starts-with(normalize-space(.),'Prepaid discount')]");
    private final By prepaidDiscountValue = By.xpath("//div[contains(@class,'summaryRow')][span[starts-with(normalize-space(.),'Prepaid discount')]]/span[2]");
    private final By codAdvanceValue = By.xpath("//div[contains(@class,'summaryRow')][span[normalize-space(.)='Pay now (advance)']]/span[2]");
    private final By codDueValue = By.xpath("//div[contains(@class,'summaryRow')][span[normalize-space(.)='Pay in cash on delivery']]/span[2]");
    private final By codInlineNudge = By.xpath("//button[normalize-space(.)='Pay online & save']");
    private final By codPopup = By.cssSelector("div[role='dialog'][aria-labelledby='cod-nudge-title']");
    private final By codPopupTitle = By.id("cod-nudge-title");
    private final By codPopupKeepCod = By.xpath("//div[@role='dialog']//button[normalize-space(.)='Keep COD']");
    private final By codPopupSwitch = By.xpath("//div[@role='dialog']//button[starts-with(normalize-space(.),'Pay online & save')]");

    private By paymentRadio(String title) {
        return By.xpath("//div[@role='radiogroup'][@aria-label='Payment method']//label[.//strong[normalize-space(.)='"
                + title + "']]//input[@type='radio']");
    }

    /** Waits for the payment-method radios (rendered once {@code /offers} has answered). */
    public CheckoutPage waitForPaymentOptions() {
        waitForVisible(paymentGroup);
        return this;
    }

    /** Titles of the payment options shown, e.g. ["Pay online", "Cash on delivery"]. */
    public List<String> paymentOptionTitles() {
        waitForPaymentOptions();
        return findAll(paymentOptionTitles).stream().map(BasePage::textContent).toList();
    }

    public boolean isPrepaidSelected() {
        return waitForPresentRadio(PAY_ONLINE).isSelected();
    }

    public boolean isCodSelected() {
        return isPresentNow(paymentRadio(COD)) && waitForPresentRadio(COD).isSelected();
    }

    /** Note under "Pay online", e.g. "UPI, cards, net banking. Save 6% (₹60) on this order." */
    public String prepaidOptionNote() {
        return textContent(By.xpath("//div[@role='radiogroup']//label[.//strong[normalize-space(.)='" + PAY_ONLINE + "']]//p"));
    }

    private WebElement waitForPresentRadio(String title) {
        return WaitUtils.waitForPresent(paymentRadio(title));
    }

    /** Picks cash on delivery; does not touch the A/B popup if one opens. */
    public void selectCod() {
        click(paymentRadio(COD));
        WaitUtils.waitFor(d -> isCodSelected());
    }

    /** Picks cash on delivery and closes the A/B popup with "Keep COD" if this customer gets it. */
    public void selectCodAndKeepIt() {
        selectCod();
        if (isCodPopupShown(3)) {
            keepCodInPopup();
        }
    }

    public void selectPrepaid() {
        click(paymentRadio(PAY_ONLINE));
        WaitUtils.waitFor(d -> isPrepaidSelected());
    }

    public boolean isPrepaidDiscountShown() {
        return isDisplayed(prepaidDiscountLabel, 3);
    }

    public boolean isPrepaidDiscountAbsent() {
        return WaitUtils.waitFor(d -> d.findElements(prepaidDiscountLabel).isEmpty());
    }

    public String prepaidDiscountLabel() {
        return textContent(prepaidDiscountLabel);
    }

    public double prepaidDiscount() {
        return ShopPage.parsePrice(getText(prepaidDiscountValue));
    }

    public double codAdvance() {
        return ShopPage.parsePrice(getText(codAdvanceValue));
    }

    /** "Pay in cash on delivery" amount; "—" until shipping is priced. */
    public String codDueText() {
        return getText(codDueValue);
    }

    public boolean isCodBreakdownAbsent() {
        return WaitUtils.waitFor(d -> d.findElements(codAdvanceValue).isEmpty() && d.findElements(codDueValue).isEmpty());
    }

    /** Text of the "Place Order &amp; Pay…" button. */
    public String placeOrderLabel() {
        return textContent(placeOrderButton);
    }

    public boolean isInlineCodNudgeShown() {
        return isDisplayed(codInlineNudge, 3);
    }

    /** "Pay online &amp; save" under the payment options (shown while COD is selected). */
    public void clickInlineCodNudge() {
        click(codInlineNudge);
        WaitUtils.waitFor(d -> isPrepaidSelected());
    }

    // ------------------------------------------------------------------ A/B test: COD popup

    public boolean isCodPopupShown(int seconds) {
        return isDisplayed(codPopup, seconds);
    }

    public String codPopupTitle() {
        return textContent(codPopupTitle);
    }

    public void keepCodInPopup() {
        click(codPopupKeepCod);
        WaitUtils.waitForInvisible(codPopup);
    }

    public void switchToPrepaidInPopup() {
        click(codPopupSwitch);
        WaitUtils.waitForInvisible(codPopup);
    }

    /** Waits for the popup to close by itself (5-second countdown). */
    public boolean codPopupClosesWithin(int seconds) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(getDriver(), java.time.Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.invisibilityOfElementLocated(codPopup));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Waits until checkout has asked the API for this customer's A/B group. The popup decision is
     * taken when COD is clicked, so picking COD before this answer arrives would never show it.
     */
    public void waitForExperimentGroupLoaded() {
        WaitUtils.waitFor(d -> Boolean.TRUE.equals(js(
                "return performance.getEntriesByType('resource').some(e => /\\/experiments(\\?|$)/.test(e.name) && e.responseEnd > 0);")));
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
