package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.api.torsavo.TorsavoApi.OffersInfo;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.base.TorsavoBaseTest;
import framework.constants.FrameworkConstants;
import framework.pages.CartPage;
import framework.pages.CheckoutPage;
import framework.pages.OffersPage;
import framework.pages.ContentPages;
import framework.pages.components.HeaderComponent;
import framework.utilities.PropertyManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/**
 * Checkout payment method (pay online with the prepaid discount, or cash on delivery with an
 * advance), the cod_popup A/B test and the bundle offers page. Every expectation is read from
 * {@code GET /offers}, so the tests follow the store's env; they skip against a backend that
 * predates the release.
 */
public class PaymentMethodTest extends TorsavoBaseTest {

    private static final String PINCODE = "250404";

    private OffersInfo offersWithCod() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        if (!offers.cod()) {
            throw new SkipException("FEATURE_COD is off");
        }
        return offers;
    }

    /** {@code customer} (or a new one) with the in-stock product in the cart, on checkout. */
    private CheckoutPage checkoutAs(AuthSession customer) {
        signIn(customer);
        open("/");
        new HeaderComponent().isSignedIn();
        addInStockProductToCart();
        HeaderComponent header = new HeaderComponent();
        header.openCart();
        new CartPage().waitForLoad().proceedToCheckout();
        waitForPath("/checkout");
        return new CheckoutPage().waitForLoad().waitForPaymentOptions();
    }

    private CheckoutPage checkoutWithItem() {
        return checkoutAs(TorsavoApi.customerSession());
    }

    private String homeState() {
        return PropertyManager.getOrDefault(FrameworkConstants.GST_HOME_STATE, "Uttar Pradesh");
    }

    // ------------------------------------------------------------------ Payment method

    @Test(groups = {SMOKE, REGRESSION}, description = "Checkout offers Pay online, plus Cash on delivery when COD is on")
    public void checkoutListsPaymentMethods() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        CheckoutPage checkout = checkoutWithItem();
        List<String> expected = offers.cod() ? List.of("Pay online", "Cash on delivery") : List.of("Pay online");
        assertions.assertEquals(checkout.paymentOptionTitles(), expected, "Payment options");
        assertions.softAssertTrue(checkout.isPrepaidSelected(), "Pay online should be selected by default");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Paying online shows the prepaid discount in the summary")
    public void prepaidDiscountIsShownByDefault() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        if (offers.prepaidDiscountPercent() <= 0) {
            throw new SkipException("PREPAID_DISCOUNT_PERCENT is 0");
        }
        ProductInfo product = TorsavoApi.inStockProduct();
        CheckoutPage checkout = checkoutWithItem();
        String pct = formatPercent(offers.prepaidDiscountPercent());
        assertions.assertTrue(checkout.isPrepaidDiscountShown(), "Prepaid discount row should be shown");
        assertions.softAssertEquals(checkout.prepaidDiscountLabel(), "Prepaid discount (" + pct + "%)", "Row label");
        // One piece, no coupon; bundles never apply to a single piece of a multi-piece bundle
        long expected = Math.round(product.price() * offers.prepaidDiscountPercent() / 100);
        assertions.assertEquals(checkout.prepaidDiscount(), (double) expected,
                pct + "% of ₹" + product.price());
        assertions.softAssertContains(checkout.prepaidOptionNote(), "Save " + pct + "%", "Pay online option advertises the saving");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Choosing COD drops the prepaid discount and splits advance / cash due")
    public void codShowsAdvanceAndCashDue() {
        OffersInfo offers = offersWithCod();
        CheckoutPage checkout = checkoutWithItem();
        checkout.fillShipping("9876543210", "12, QA Test Street", "Meerut", homeState(), PINCODE);
        checkout.deliveryVerdictShown();
        double prepaidTotal = checkout.total();
        checkout.selectCodAndKeepIt();

        assertions.assertTrue(checkout.isPrepaidDiscountAbsent(), "Prepaid discount row should disappear with COD");
        double codTotal = checkout.total();
        double advance = Math.min(offers.codAdvance(), codTotal);
        assertions.softAssertTrue(codTotal >= prepaidTotal, "COD total (₹" + codTotal + ") ≥ prepaid total (₹" + prepaidTotal + ")");
        assertions.assertEquals(checkout.codAdvance(), advance, "Pay now (advance)");
        String due = checkout.codDueText();
        if (!"—".equals(due.trim())) {
            assertions.softAssertEquals(Prices.parse(due), codTotal - advance, "Cash due = total - advance");
        }
        assertions.softAssertContains(checkout.placeOrderLabel(), "Advance", "Button should say the advance is paid now");
        assertions.softAssertContains(checkout.placeOrderLabel(), "₹" + Prices.rupees(advance), "Button shows the advance");
    }

    @Test(groups = {REGRESSION}, description = "Switching back to Pay online restores the discount and hides the COD split")
    public void switchingBackToPrepaidRestoresDiscount() {
        OffersInfo offers = offersWithCod();
        CheckoutPage checkout = checkoutWithItem();
        checkout.selectCodAndKeepIt();
        checkout.selectPrepaid();
        assertions.assertTrue(checkout.isCodBreakdownAbsent(), "Advance / cash due rows should disappear");
        assertions.softAssertEquals(checkout.placeOrderLabel(), "Place Order & Pay", "Button label for prepaid");
        if (offers.prepaidDiscountPercent() > 0) {
            assertions.assertTrue(checkout.isPrepaidDiscountShown(), "Prepaid discount should come back");
        }
    }

    @Test(groups = {REGRESSION}, description = "With COD selected, the inline 'Pay online & save' nudge switches to prepaid")
    public void inlineNudgeSwitchesToPrepaid() {
        OffersInfo offers = offersWithCod();
        if (offers.prepaidDiscountPercent() <= 0) {
            throw new SkipException("PREPAID_DISCOUNT_PERCENT is 0, so there is no nudge");
        }
        CheckoutPage checkout = checkoutWithItem();
        checkout.selectCodAndKeepIt();
        assertions.assertTrue(checkout.isInlineCodNudgeShown(), "Nudge should be shown under the payment options");
        checkout.clickInlineCodNudge();
        assertions.assertTrue(checkout.isPrepaidSelected(), "Pay online should be selected after the nudge");
        assertions.softAssertTrue(checkout.isPrepaidDiscountShown(), "Prepaid discount should be applied again");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A COD order opens Razorpay for the advance")
    public void codOrderOpensRazorpayForAdvance() {
        offersWithCod();
        requireTestDataCreation();
        CheckoutPage checkout = checkoutWithItem();
        checkout.fillShipping("9876543210", "12, QA Test Street", "Meerut", homeState(), PINCODE);
        checkout.deliveryVerdictShown();
        checkout.selectCodAndKeepIt();
        checkout.placeOrder();
        assertions.assertTrue(checkout.isRazorpayCheckoutOpened(),
                "Razorpay should open for the COD advance - page says: '" + checkout.paymentFailureReason() + "'");
    }

    // ------------------------------------------------------------------ A/B test: cod_popup

    @Test(groups = {SMOKE, REGRESSION}, description = "Popup group: picking COD shows the 'pay online and save' popup")
    public void popupGroupSeesCodPopup() {
        popupPreconditions();
        CheckoutPage checkout = checkoutAs(TorsavoApi.customerInCodPopupGroup("popup", 12));
        checkout.waitForExperimentGroupLoaded();
        checkout.selectCod();
        assertions.assertTrue(checkout.isCodPopupShown(5), "Popup group should get the COD popup");
        assertions.softAssertContains(checkout.codPopupTitle(), "Pay online and save", "Popup title");
        checkout.keepCodInPopup();
        assertions.assertTrue(checkout.isCodSelected(), "'Keep COD' keeps cash on delivery selected");
        // Shown once per visit
        checkout.selectPrepaid();
        checkout.selectCod();
        assertions.softAssertTrue(!checkout.isCodPopupShown(2), "Popup should not show a second time in one visit");
    }

    @Test(groups = {REGRESSION}, description = "Popup group: 'Pay online & save' in the popup switches to prepaid")
    public void popupSwitchesToPrepaid() {
        popupPreconditions();
        CheckoutPage checkout = checkoutAs(TorsavoApi.customerInCodPopupGroup("popup", 12));
        checkout.waitForExperimentGroupLoaded();
        checkout.selectCod();
        assertions.assertTrue(checkout.isCodPopupShown(5), "Popup should appear");
        checkout.switchToPrepaidInPopup();
        assertions.assertTrue(checkout.isPrepaidSelected(), "Pay online should be selected");
        assertions.softAssertTrue(checkout.isPrepaidDiscountShown(), "Prepaid discount applies again");
    }

    @Test(groups = {REGRESSION}, description = "Popup group: the popup closes by itself after 5 seconds and COD stays selected")
    public void popupTimesOutKeepingCod() {
        popupPreconditions();
        CheckoutPage checkout = checkoutAs(TorsavoApi.customerInCodPopupGroup("popup", 12));
        checkout.waitForExperimentGroupLoaded();
        checkout.selectCod();
        assertions.assertTrue(checkout.isCodPopupShown(5), "Popup should appear");
        assertions.assertTrue(checkout.codPopupClosesWithin(9), "Popup should close by itself after its 5-second countdown");
        assertions.softAssertTrue(checkout.isCodSelected(), "COD stays selected after the timeout");
    }

    @Test(groups = {REGRESSION}, description = "Control group: picking COD shows no popup")
    public void controlGroupSeesNoPopup() {
        popupPreconditions();
        CheckoutPage checkout = checkoutAs(TorsavoApi.customerInCodPopupGroup("control", 12));
        checkout.waitForExperimentGroupLoaded();
        checkout.selectCod();
        assertions.assertTrue(!checkout.isCodPopupShown(3), "Control group must not get the popup");
        assertions.softAssertTrue(checkout.isCodSelected(), "COD selected");
    }

    private OffersInfo popupPreconditions() {
        OffersInfo offers = offersWithCod();
        TorsavoApi.requireExperimentsApi();
        if (offers.prepaidDiscountPercent() <= 0) {
            throw new SkipException("PREPAID_DISCOUNT_PERCENT is 0: the popup has nothing to offer");
        }
        return offers;
    }

    // ------------------------------------------------------------------ Bundle offers page

    @Test(groups = {REGRESSION}, description = "/offers lists the bundles from the API, or is a 404 while bundles are off")
    public void offersPageMatchesBundleConfig() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        int status = TorsavoApi.getUrl(TorsavoApi.siteUrl() + "/offers").status();
        if (!offers.bundles()) {
            assertions.assertEquals(status, 404, "/offers while FEATURE_BUNDLES is off");
            open("/offers");
            assertions.softAssertContains(new ContentPages().bodyText(), "404", "Browser shows the 404 page");
            return;
        }
        assertions.assertEquals(status, 200, "/offers status");
        open("/offers");
        OffersPage page = new OffersPage();
        assertions.assertTrue(page.isLoaded(), "Offers page heading");
        List<String> expected = new java.util.ArrayList<>();
        offers.raw().path("bundles").forEach(b -> expected.add(b.path("title").asText()));
        if (expected.isEmpty()) {
            assertions.assertTrue(page.isEmptyMessageShown(), "Empty state when no bundles are configured");
        } else {
            assertions.assertEquals(page.bundleTitles(), expected, "Bundle cards match GET /offers");
            for (String href : page.shopLinkHrefs()) {
                assertions.softAssertTrue(href.startsWith("/products?category="), "Shop link goes to a category — " + href);
            }
        }
    }

    private static String formatPercent(double pct) {
        return pct == Math.rint(pct) ? String.valueOf((long) pct) : String.valueOf(pct);
    }

    /** Small price helpers (₹ formatting as the storefront prints it). */
    private static final class Prices {
        static double parse(String text) {
            return framework.pages.ShopPage.parsePrice(text);
        }

        static String rupees(double amount) {
            return java.text.NumberFormat.getIntegerInstance(java.util.Locale.of("en", "IN")).format(Math.round(amount));
        }
    }
}
