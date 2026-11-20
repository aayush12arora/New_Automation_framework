package tests;

import com.fasterxml.jackson.databind.JsonNode;
import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.api.torsavo.TorsavoApi.HttpResult;
import framework.api.torsavo.TorsavoApi.OffersInfo;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.base.TorsavoBaseTest;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/**
 * API contract for the promotions release: {@code GET /offers}, bundle pricing
 * ({@code POST /offers/quote}), the payment mode on {@code POST /shipping/quote} and the
 * idle-at-checkout code ({@code POST /coupons/idle-offer}). Skipped against a backend that
 * predates the release.
 */
public class OffersApiTest extends TorsavoBaseTest {

    private static final String PINCODE = "250404";
    private static final String UNKNOWN_PRODUCT = "00000000-0000-4000-8000-000000000000";

    @Test(groups = {SMOKE, REGRESSION}, description = "GET /offers is public and reports features, payment options and bundles")
    public void offersEndpointReportsFeaturesAndPayment() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        JsonNode raw = offers.raw();
        for (String flag : List.of("loginCouponPopup", "idleCheckoutOffer", "bundles")) {
            assertions.softAssertTrue(raw.path("features").path(flag).isBoolean(), "features." + flag + " should be a boolean");
        }
        assertions.softAssertTrue(raw.path("payment").path("cod").isBoolean(), "payment.cod should be a boolean");
        assertions.softAssertTrue(offers.codAdvance() >= 0, "payment.codAdvance should not be negative");
        assertions.softAssertTrue(offers.prepaidDiscountPercent() >= 0 && offers.prepaidDiscountPercent() < 100,
                "payment.prepaidDiscountPercent should be 0-99 — got " + offers.prepaidDiscountPercent());
        assertions.assertTrue(raw.path("bundles").isArray(), "bundles should be an array");
        assertions.softAssertEquals(raw.path("idleOffer").isNull() || raw.path("idleOffer").isMissingNode(),
                !offers.idleCheckoutOffer(), "idleOffer details are present exactly when the idle offer is on");
        if (!offers.bundles()) {
            assertions.softAssertEquals(raw.path("bundles").size(), 0, "No bundles are listed while FEATURE_BUNDLES is off");
        }
    }

    @Test(groups = {REGRESSION}, description = "GET /offers exposes no keys or secrets")
    public void offersEndpointLeaksNoSecrets() {
        TorsavoApi.offersOrSkip();
        HttpResult res = TorsavoApi.get("/offers", null);
        assertions.assertEquals(res.status(), 200, "GET /offers status");
        String body = res.body().toLowerCase();
        for (String secret : List.of("razorpay", "secret", "token", "password")) {
            assertions.softAssertTrue(!body.contains(secret), "/offers must not expose '" + secret + "'");
        }
    }

    @Test(groups = {REGRESSION}, description = "Each listed bundle has an id, title, positive price and positive item quantities")
    public void bundleOffersAreWellFormed() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        if (!offers.bundles() || offers.raw().path("bundles").isEmpty()) {
            throw new SkipException("FEATURE_BUNDLES is off or BUNDLE_OFFERS is empty");
        }
        for (JsonNode b : offers.raw().path("bundles")) {
            String id = b.path("id").asText();
            assertions.softAssertTrue(!id.isBlank(), "Bundle id should be set");
            assertions.softAssertTrue(!b.path("title").asText().isBlank(), "Bundle " + id + " should have a title");
            assertions.softAssertTrue(b.path("price").asDouble() > 0, "Bundle " + id + " price should be positive");
            assertions.softAssertTrue(b.path("items").size() > 0, "Bundle " + id + " should list items");
            for (JsonNode item : b.path("items")) {
                assertions.softAssertTrue(item.path("quantity").asInt() > 0, "Bundle " + id + " item quantity should be positive");
                assertions.softAssertTrue(!item.path("slug").isNull(),
                        "Bundle " + id + " category '" + item.path("category").asText() + "' should exist in the catalogue");
            }
        }
    }

    @Test(groups = {REGRESSION}, description = "Only bundles cheaper than their dearest pieces bought separately are advertised")
    public void listedBundlesAlwaysSaveMoney() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        if (!offers.bundles() || offers.raw().path("bundles").isEmpty()) {
            throw new SkipException("FEATURE_BUNDLES is off or BUNDLE_OFFERS is empty");
        }
        List<ProductInfo> catalogue = TorsavoApi.products();
        for (JsonNode b : offers.raw().path("bundles")) {
            double dearest = 0;
            for (JsonNode item : b.path("items")) {
                String category = item.path("category").asText();
                double priciest = catalogue.stream().filter(p -> p.category().equalsIgnoreCase(category))
                        .mapToDouble(ProductInfo::price).max().orElse(0);
                dearest += priciest * item.path("quantity").asInt();
            }
            assertions.softAssertTrue(b.path("price").asDouble() < dearest,
                    "Bundle '" + b.path("title").asText() + "' (₹" + b.path("price").asDouble()
                            + ") should cost less than its dearest pieces (₹" + dearest + "), or be hidden");
        }
    }

    @Test(groups = {REGRESSION}, description = "Bundle quote for an empty cart is zero")
    public void bundleQuoteForEmptyCartIsZero() {
        TorsavoApi.offersOrSkip();
        HttpResult res = TorsavoApi.postJson("/offers/quote", Map.of("items", List.of()), null);
        assertions.assertEquals(res.status(), 200, "POST /offers/quote status — " + res.body());
        JsonNode quote = TorsavoApi.json(res, "/offers/quote");
        assertions.softAssertEquals(quote.path("discount").asDouble(), 0.0, "Empty cart discount");
        assertions.softAssertEquals(quote.path("bundles").size(), 0, "Empty cart applies no bundles");
    }

    @Test(groups = {REGRESSION}, description = "Bundle quote rejects a malformed cart with 400")
    public void bundleQuoteRejectsInvalidItems() {
        TorsavoApi.offersOrSkip();
        HttpResult notUuid = TorsavoApi.postJson("/offers/quote",
                Map.of("items", List.of(Map.of("productId", "not-a-uuid", "quantity", 1))), null);
        assertions.softAssertEquals(notUuid.status(), 400, "Non-UUID productId");
        HttpResult zeroQty = TorsavoApi.postJson("/offers/quote",
                Map.of("items", List.of(Map.of("productId", UNKNOWN_PRODUCT, "quantity", 0))), null);
        assertions.softAssertEquals(zeroQty.status(), 400, "Quantity 0");
        HttpResult tooMany = TorsavoApi.postJson("/offers/quote",
                Map.of("items", List.of(Map.of("productId", UNKNOWN_PRODUCT, "quantity", 51))), null);
        assertions.softAssertEquals(tooMany.status(), 400, "Quantity above 50");
    }

    @Test(groups = {REGRESSION}, description = "A single piece never unlocks a multi-piece bundle")
    public void singlePieceGetsNoBundleDiscount() {
        TorsavoApi.offersOrSkip();
        ProductInfo product = TorsavoApi.inStockProduct();
        HttpResult res = TorsavoApi.postJson("/offers/quote",
                Map.of("items", List.of(Map.of("productId", product.id(), "quantity", 1))), null);
        assertions.assertEquals(res.status(), 200, "POST /offers/quote status — " + res.body());
        JsonNode quote = TorsavoApi.json(res, "/offers/quote");
        boolean singlePieceBundle = false;
        for (JsonNode b : TorsavoApi.offersOrSkip().raw().path("bundles")) {
            int pieces = 0;
            for (JsonNode i : b.path("items")) {
                pieces += i.path("quantity").asInt();
            }
            singlePieceBundle |= pieces == 1;
        }
        if (!singlePieceBundle) {
            assertions.assertEquals(quote.path("discount").asDouble(), 0.0, "One piece should get no bundle discount");
        }
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A cart that completes a bundle is charged the bundle price")
    public void completeBundleIsPricedAtBundlePrice() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        if (!offers.bundles() || offers.raw().path("bundles").isEmpty()) {
            throw new SkipException("FEATURE_BUNDLES is off or BUNDLE_OFFERS is empty");
        }
        JsonNode bundle = offers.raw().path("bundles").get(0);
        List<ProductInfo> catalogue = TorsavoApi.products();
        List<Map<String, Object>> items = new ArrayList<>();
        double original = 0;
        for (JsonNode need : bundle.path("items")) {
            String category = need.path("category").asText();
            int qty = need.path("quantity").asInt();
            // Dearest pieces in stock first (bundles are only listed when they beat the dearest
            // pieces); "any 2 jeans" may be filled from different products
            List<ProductInfo> inCategory = catalogue.stream()
                    .filter(x -> x.category().equalsIgnoreCase(category) && x.stock() > 0)
                    .sorted((a, b) -> Double.compare(b.price(), a.price()))
                    .toList();
            int left = qty;
            for (ProductInfo p : inCategory) {
                if (left == 0) {
                    break;
                }
                int take = Math.min(left, p.stock());
                items.add(Map.of("productId", p.id(), "quantity", take));
                original += p.price() * take;
                left -= take;
            }
            if (left > 0) {
                throw new SkipException("Only " + (qty - left) + " of " + qty + " '" + category + "' pieces in stock");
            }
        }
        HttpResult res = TorsavoApi.postJson("/offers/quote", Map.of("items", items), null);
        assertions.assertEquals(res.status(), 200, "POST /offers/quote status — " + res.body());
        JsonNode quote = TorsavoApi.json(res, "/offers/quote");
        double bundlePrice = bundle.path("price").asDouble();
        if (original <= bundlePrice) {
            // Listed bundles beat the dearest pieces in the catalogue; the dearest may be out of stock
            throw new SkipException("The in-stock pieces cost less than the bundle (₹" + original + " ≤ ₹" + bundlePrice + ")");
        }
        assertions.assertTrue(quote.path("bundles").size() > 0, "Bundle '" + bundle.path("title").asText() + "' should apply — " + quote);
        double savings = 0;
        for (JsonNode applied : quote.path("bundles")) {
            savings += applied.path("savings").asDouble();
            assertions.softAssertEquals(applied.path("originalPrice").asDouble() - applied.path("price").asDouble(),
                    applied.path("savings").asDouble(), "savings = originalPrice - price for " + applied.path("id").asText());
        }
        assertions.softAssertEquals(quote.path("discount").asDouble(), savings, "Quote discount equals the sum of bundle savings");
        assertions.softAssertTrue(quote.path("discount").asDouble() >= original - bundlePrice,
                "Saving should be at least what bundle '" + bundle.path("id").asText() + "' alone gives");
    }

    @Test(groups = {REGRESSION}, description = "Shipping quote accepts PREPAID and COD and rejects other payment modes")
    public void shippingQuoteValidatesPaymentMode() {
        TorsavoApi.offersOrSkip();
        ProductInfo product = TorsavoApi.inStockProduct();
        List<Map<String, Object>> items = List.of(Map.of("productId", product.id(), "quantity", 1));
        for (String mode : List.of("PREPAID", "COD")) {
            HttpResult res = TorsavoApi.postJson("/shipping/quote", Map.of("pincode", PINCODE, "items", items, "paymentMode", mode), null);
            assertions.softAssertEquals(res.status(), 200, mode + " quote status — " + res.body());
            if (res.status() == 200) {
                assertions.softAssertTrue(TorsavoApi.json(res, "/shipping/quote").path("serviceable").isBoolean(),
                        mode + " quote should say whether the pincode is serviceable");
            }
        }
        HttpResult bad = TorsavoApi.postJson("/shipping/quote", Map.of("pincode", PINCODE, "items", items, "paymentMode", "CARD"), null);
        assertions.assertEquals(bad.status(), 400, "Unknown payment mode should be rejected");
    }

    @Test(groups = {REGRESSION}, description = "A serviceable shipping quote names the courier and delivery estimate")
    public void serviceableQuoteNamesCourier() {
        TorsavoApi.offersOrSkip();
        ProductInfo product = TorsavoApi.inStockProduct();
        HttpResult res = TorsavoApi.postJson("/shipping/quote", Map.of("pincode", PINCODE,
                "items", List.of(Map.of("productId", product.id(), "quantity", 1))), null);
        assertions.assertEquals(res.status(), 200, "Quote status — " + res.body());
        JsonNode quote = TorsavoApi.json(res, "/shipping/quote");
        if (!quote.path("serviceable").asBoolean() || quote.path("fallback").asBoolean()) {
            throw new SkipException("Pincode " + PINCODE + " not serviceable or Shiprocket unreachable (flat-fee fallback)");
        }
        assertions.softAssertTrue(!quote.path("courierName").asText().isBlank(), "courierName should be set");
        assertions.softAssertTrue(quote.path("surface").isBoolean(), "surface (road vs air) should be reported");
        assertions.softAssertTrue(quote.path("cost").asDouble() >= 0, "cost should not be negative");
    }

    @Test(groups = {REGRESSION}, description = "Two pieces ship free whatever the payment mode")
    public void twoPiecesShipFreeForCodToo() {
        TorsavoApi.offersOrSkip();
        ProductInfo product = TorsavoApi.products().stream().filter(p -> p.stock() >= 2).findFirst()
                .orElseThrow(() -> new SkipException("No product with 2+ in stock"));
        for (String mode : List.of("PREPAID", "COD")) {
            HttpResult res = TorsavoApi.postJson("/shipping/quote", Map.of("pincode", PINCODE, "paymentMode", mode,
                    "items", List.of(Map.of("productId", product.id(), "quantity", 2))), null);
            assertions.assertEquals(res.status(), 200, mode + " quote status — " + res.body());
            JsonNode quote = TorsavoApi.json(res, "/shipping/quote");
            if (quote.path("serviceable").asBoolean()) {
                assertions.softAssertTrue(quote.path("free").asBoolean(), mode + ": 2 pieces should be free shipping");
                assertions.softAssertEquals(quote.path("cost").asDouble(), 0.0, mode + ": 2 pieces cost nothing to ship");
            }
        }
    }

    @Test(groups = {REGRESSION}, description = "Idle-at-checkout code needs sign-in, and is 404 while the feature is off")
    public void idleOfferRequiresAuthAndFeature() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        HttpResult guest = TorsavoApi.postJson("/coupons/idle-offer", Map.of(), null);
        assertions.softAssertEquals(guest.status(), 401, "Guests should be refused");
        if (!offers.idleCheckoutOffer()) {
            requireTestDataCreation();
            AuthSession customer = TorsavoApi.customerSession();
            HttpResult res = TorsavoApi.postJson("/coupons/idle-offer", Map.of(), customer.accessToken());
            assertions.assertEquals(res.status(), 404, "Idle offer should be unavailable while FEATURE_IDLE_CHECKOUT_OFFER is off");
        }
    }

    @Test(groups = {REGRESSION}, description = "Idle-at-checkout: no code for an empty cart; one stable single-use code otherwise")
    public void idleOfferIssuesOneCodePerCustomer() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        if (!offers.idleCheckoutOffer()) {
            throw new SkipException("FEATURE_IDLE_CHECKOUT_OFFER is off");
        }
        requireTestDataCreation();
        AuthSession customer = TorsavoApi.registerNewCustomer();
        HttpResult empty = TorsavoApi.postJson("/coupons/idle-offer", Map.of(), customer.accessToken());
        assertions.assertEquals(empty.status(), 200, "Empty-cart idle offer status — " + empty.body());
        assertions.softAssertTrue(TorsavoApi.json(empty, "idle-offer").path("offer").isNull(), "Empty cart gets no code");

        ProductInfo product = TorsavoApi.inStockProduct();
        List<String> sizes = TorsavoApi.effectiveSizes(product);
        TorsavoApi.addToServerCart(customer, product.id(), 1, sizes.isEmpty() ? null : sizes.get(0));

        JsonNode first = TorsavoApi.json(TorsavoApi.postJson("/coupons/idle-offer", Map.of(), customer.accessToken()), "idle-offer").path("offer");
        assertions.assertTrue(first.path("code").asText().startsWith("QUICK-"), "Idle code should start with QUICK- — got " + first);
        assertions.softAssertEquals(first.path("percent").asDouble(),
                offers.raw().path("idleOffer").path("percent").asDouble(), "Idle code percent matches /offers");
        JsonNode again = TorsavoApi.json(TorsavoApi.postJson("/coupons/idle-offer", Map.of(), customer.accessToken()), "idle-offer").path("offer");
        assertions.softAssertEquals(again.path("code").asText(), first.path("code").asText(),
                "Asking again while the code is live returns the same code");
    }
}
