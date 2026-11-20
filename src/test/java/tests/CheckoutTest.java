package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.base.TorsavoBaseTest;
import framework.constants.FrameworkConstants;
import framework.pages.CartPage;
import framework.pages.CheckoutPage;
import framework.pages.LoginPage;
import framework.pages.components.HeaderComponent;
import framework.utilities.PropertyManager;
import framework.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/** Checkout for a signed-in customer: summary, shipping form, coupon, GST and payment hand-off. */
public class CheckoutTest extends TorsavoBaseTest {

    private static final String PHONE = "9876543210";
    private static final String STREET = "12, QA Test Street";
    private static final String CITY = "Meerut";
    private static final String PINCODE = "250404";

    private String homeState() {
        return PropertyManager.getOrDefault(FrameworkConstants.GST_HOME_STATE, "Uttar Pradesh");
    }

    /**
     * Signed-in customer with the in-stock product in their cart, on the checkout page.
     * Reaches checkout the way a shopper does (header cart icon, then "Proceed to Checkout"):
     * these are client-side navigations, so the restored session is already in memory.
     */
    private CheckoutPage checkoutWithItem() {
        signInAsNewCustomer();
        addInStockProductToCart();
        HeaderComponent header = new HeaderComponent();
        header.isSignedIn();
        header.openCart();
        new CartPage().waitForLoad().proceedToCheckout();
        waitForPath("/checkout");
        return new CheckoutPage().waitForLoad();
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A signed-in customer with items reaches the checkout form")
    public void signedInCustomerReachesCheckout() {
        CheckoutPage checkout = checkoutWithItem();
        assertions.assertTrue(checkout.isLoaded(), "Checkout with shipping form should be shown");
        assertions.softAssertEquals(checkout.countryValue(), "India", "Country should default to India");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Order summary lists the cart item with its options and quantity")
    public void orderSummaryListsCartItem() {
        ProductInfo product = TorsavoApi.inStockProduct();
        CheckoutPage checkout = checkoutWithItem();
        List<String> lines = checkout.summaryLineTexts();
        assertions.assertTrue(lines.stream().anyMatch(l -> l.startsWith(product.name()) && l.endsWith("x 1")),
                "Summary should list '" + product.name() + " ... x 1' — got " + lines);
        assertions.softAssertEquals(checkout.total() > 0, true, "Order total should be positive");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Placing an order without shipping details is blocked")
    public void shippingFormRequiresFields() {
        CheckoutPage checkout = checkoutWithItem();
        checkout.placeOrder();
        assertions.assertTrue(!checkout.phoneValidationMessage().isBlank(), "Phone should be required");
        assertions.assertContains(currentPath(), "/checkout", "Customer should stay on checkout");
        assertions.softAssertTrue(!checkout.isRazorpayCheckoutOpened(), "Payment must not open");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A guest's cart is kept when they sign in at checkout")
    public void guestCartIsKeptAfterSigningInAtCheckout() {
        AuthSession customer = TorsavoApi.customerSession();
        ProductInfo product = addInStockProductToCart();
        open("/checkout");
        waitForUrl("/login");
        new LoginPage().login(customer.email(), customer.password());
        waitForPath("/checkout");
        CheckoutPage checkout = new CheckoutPage().waitForLoad();
        assertions.assertTrue(checkout.isLoaded(), "Customer should be returned to checkout");
        assertions.assertTrue(checkout.summaryLineTexts().stream().anyMatch(l -> l.startsWith(product.name())),
                "Guest cart item should carry over after signing in");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Valid shipping details create the order and open Razorpay")
    public void placeOrderOpensRazorpayPayment() {
        requireTestDataCreation();
        CheckoutPage checkout = checkoutWithItem();
        checkout.fillShipping(PHONE, STREET, CITY, homeState(), PINCODE);
        // Like a shopper, wait for the pincode check (it prices shipping) before paying.
        checkout.deliveryVerdictShown();
        checkout.placeOrder();
        boolean opened = checkout.isRazorpayCheckoutOpened();
        assertions.assertTrue(opened, "Razorpay payment window should open - page says: '"
                + checkout.paymentFailureReason() + "'");
    }

    @Test(groups = {REGRESSION}, description = "Entering a pincode runs the courier delivery check")
    public void pincodeShowsDeliveryVerdict() {
        CheckoutPage checkout = checkoutWithItem();
        checkout.enterPincode(PINCODE);
        assertions.assertTrue(checkout.deliveryVerdictShown(),
                "A delivery available / not available verdict should appear for pincode " + PINCODE);
    }

    @Test(groups = {REGRESSION}, description = "Checkout shows the courier and estimate the delivery quote picked")
    public void deliveryShowsQuotedCourier() {
        ProductInfo product = TorsavoApi.inStockProduct();
        TorsavoApi.HttpResult res = TorsavoApi.postJson("/shipping/quote", java.util.Map.of("pincode", PINCODE,
                "items", List.of(java.util.Map.of("productId", product.id(), "quantity", 1))), null);
        com.fasterxml.jackson.databind.JsonNode quote = TorsavoApi.json(res, "/shipping/quote");
        if (res.status() != 200 || !quote.path("serviceable").asBoolean() || quote.path("courierName").asText().isBlank()) {
            throw new org.testng.SkipException("No courier quote for " + PINCODE + ": " + res.body());
        }
        CheckoutPage checkout = checkoutWithItem();
        checkout.enterPincode(PINCODE);
        assertions.assertTrue(checkout.deliveryVerdictShown(), "Delivery verdict for " + PINCODE);
        assertions.assertEquals(checkout.shipsViaCourier(), quote.path("courierName").asText(),
                "Courier shown should be the one the quote picked across providers");
        String estimate = checkout.deliveryEstimateText();
        if (!quote.path("etd").asText().isBlank()) {
            assertions.softAssertContains(estimate, "Estimated delivery by", "Delivery date shown when the courier gives one");
        } else if (quote.path("estimatedDays").asInt() > 0) {
            assertions.softAssertContains(estimate, "Estimated delivery in", "Days in transit shown when there is no date");
        }
        assertions.softAssertTrue(!estimate.contains("—"), "No placeholder dash when the courier gives no date — got '" + estimate + "'");
    }

    /**
     * Reloading checkout while signed in must keep the customer on checkout.
     * Known defect at time of writing: checkout redirects to /login on the first render
     * because it checks the session before AuthContext has restored it.
     */
    @Test(groups = {REGRESSION}, description = "Reloading checkout keeps a signed-in customer on checkout")
    public void checkoutSurvivesPageReload() {
        checkoutWithItem();
        getDriver().navigate().refresh();
        WaitUtils.waitFor(d -> d.getCurrentUrl().contains("/login")
                || !d.findElements(By.name("shippingPhone")).isEmpty());
        assertions.assertContains(currentPath(), "/checkout", "Signed-in customer should stay on checkout after a reload");
        assertions.assertTrue(new CheckoutPage().waitForLoad().isLoaded(), "Checkout form should be shown after a reload");
    }

    @Test(groups = {REGRESSION}, description = "State dropdown offers all 28 states and 8 union territories")
    public void stateDropdownListsAllStatesAndUts() {
        List<String> options = checkoutWithItem().stateOptions();
        assertions.assertEquals(options.size() - 1, 36, "36 states/UTs plus the placeholder");
        for (String s : List.of("Uttar Pradesh", "Maharashtra", "Delhi", "Karnataka", "Tamil Nadu")) {
            assertions.softAssertTrue(options.contains(s), "State list should include " + s);
        }
    }

    @Test(groups = {REGRESSION}, description = "An invalid discount code is rejected with a message")
    public void invalidCouponShowsError() {
        CheckoutPage checkout = checkoutWithItem();
        checkout.applyCoupon("NOTAREALCODE99");
        assertions.assertContains(checkout.couponError(), "isn't valid", "Invalid code message");
    }

    @Test(groups = {REGRESSION}, description = "Shipping inside the home state shows CGST + SGST")
    public void homeStateShowsCgstAndSgst() {
        CheckoutPage checkout = checkoutWithItem();
        checkout.selectState(homeState());
        assertions.assertTrue(checkout.isTaxableAmountShown(), "Taxable amount should be shown");
        assertions.assertTrue(checkout.isCgstSgstShown(), "Intra-state order should show CGST and SGST");
    }

    @Test(groups = {REGRESSION}, description = "Shipping to another state shows IGST")
    public void otherStateShowsIgst() {
        CheckoutPage checkout = checkoutWithItem();
        checkout.selectState(homeState().equals("Maharashtra") ? "Karnataka" : "Maharashtra");
        assertions.assertTrue(checkout.isIgstShown(), "Inter-state order should show IGST");
    }
}
