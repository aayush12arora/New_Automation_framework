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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/**
 * Server-side pricing of the payment mode on {@code POST /orders}: the prepaid discount, the COD
 * advance / cash-due split, the A/B group stamped on the order, and that Razorpay is asked for
 * only the advance on a COD order. Every test creates (then cancels) a real unpaid order, so all
 * need {@code createTestData=true}.
 */
public class PaymentModeOrderApiTest extends TorsavoBaseTest {

    private static final double CENT = 0.011;

    /** One in-stock piece shipped inside UP; paymentMode omitted when null. */
    private Map<String, Object> orderBody(ProductInfo product, String paymentMode) {
        List<String> sizes = TorsavoApi.effectiveSizes(product);
        Map<String, Object> item = new HashMap<>();
        item.put("productId", product.id());
        item.put("quantity", 1);
        if (!sizes.isEmpty()) {
            item.put("size", sizes.get(0));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("shippingAddress", "12, QA Test Street");
        body.put("shippingCity", "Meerut");
        body.put("shippingState", "Uttar Pradesh");
        body.put("shippingZip", "250404");
        body.put("shippingCountry", "India");
        body.put("shippingPhone", "9876543210");
        if (paymentMode != null) {
            body.put("paymentMode", paymentMode);
        }
        return body;
    }

    /** Creates the order, returning its JSON; fails with the API's message otherwise. */
    private JsonNode createOrder(AuthSession customer, ProductInfo product, String paymentMode) {
        HttpResult res = TorsavoApi.postJson("/orders", orderBody(product, paymentMode), customer.accessToken());
        if (res.status() != 201) {
            throw new AssertionError("Creating a " + paymentMode + " order failed (" + res.status() + "): " + res.body());
        }
        return TorsavoApi.json(res, "/orders");
    }

    private OffersInfo offersForOrders() {
        OffersInfo offers = TorsavoApi.offersOrSkip();
        requireTestDataCreation();
        return offers;
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A prepaid order gets PREPAID_DISCOUNT_PERCENT off, priced by the server")
    public void prepaidOrderGetsPrepaidDiscount() {
        OffersInfo offers = offersForOrders();
        AuthSession customer = TorsavoApi.registerNewCustomer();
        ProductInfo product = TorsavoApi.inStockProduct();
        JsonNode order = createOrder(customer, product, "PREPAID");
        try {
            double afterBundles = product.price() - order.path("bundle_discount").asDouble(0);
            long expected = Math.round(afterBundles * offers.prepaidDiscountPercent() / 100);
            assertions.assertEquals(order.path("payment_mode").asText(), "PREPAID", "payment_mode");
            assertions.assertEquals(order.path("prepaid_discount").asDouble(), (double) expected,
                    offers.prepaidDiscountPercent() + "% of ₹" + afterBundles);
            assertions.softAssertEquals(order.path("cod_advance").asDouble(), 0.0, "No COD advance on a prepaid order");
            assertions.softAssertEquals(order.path("cod_amount").asDouble(), 0.0, "No cash due on a prepaid order");
            double total = product.price() - order.path("discount_amount").asDouble() + order.path("shipping_cost").asDouble();
            assertions.softAssertTrue(Math.abs(order.path("total").asDouble() - total) < CENT,
                    "total = items - discounts + shipping (" + total + ") — got " + order.path("total").asDouble());
        } finally {
            TorsavoApi.cancelOrderQuietly(customer, order.path("id").asText());
        }
    }

    @Test(groups = {REGRESSION}, description = "An order without paymentMode is treated as prepaid")
    public void paymentModeDefaultsToPrepaid() {
        OffersInfo offers = offersForOrders();
        AuthSession customer = TorsavoApi.registerNewCustomer();
        JsonNode order = createOrder(customer, TorsavoApi.inStockProduct(), null);
        try {
            assertions.assertEquals(order.path("payment_mode").asText(), "PREPAID", "Default payment_mode");
            if (offers.prepaidDiscountPercent() > 0) {
                assertions.softAssertTrue(order.path("prepaid_discount").asDouble() > 0, "Default order gets the prepaid discount");
            }
        } finally {
            TorsavoApi.cancelOrderQuietly(customer, order.path("id").asText());
        }
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A COD order pays the advance online and the rest in cash, with no prepaid discount")
    public void codOrderSplitsAdvanceAndCashDue() {
        OffersInfo offers = offersForOrders();
        if (!offers.cod()) {
            throw new SkipException("FEATURE_COD is off");
        }
        AuthSession customer = TorsavoApi.registerNewCustomer();
        ProductInfo product = TorsavoApi.inStockProduct();
        HttpResult res = TorsavoApi.postJson("/orders", orderBody(product, "COD"), customer.accessToken());
        if (res.status() == 400 && res.body().contains("Cash on delivery")) {
            throw new SkipException("COD refused for this product/pincode: " + res.body());
        }
        assertions.assertEquals(res.status(), 201, "COD order status — " + res.body());
        JsonNode order = TorsavoApi.json(res, "/orders");
        try {
            double total = order.path("total").asDouble();
            assertions.assertEquals(order.path("payment_mode").asText(), "COD", "payment_mode");
            assertions.softAssertEquals(order.path("prepaid_discount").asDouble(), 0.0, "COD orders get no prepaid discount");
            assertions.assertEquals(order.path("cod_advance").asDouble(), offers.codAdvance(), "Advance = COD_ADVANCE");
            assertions.assertTrue(Math.abs(order.path("cod_amount").asDouble() - (total - offers.codAdvance())) < CENT,
                    "Cash due = total - advance (" + (total - offers.codAdvance()) + ") — got " + order.path("cod_amount").asDouble());
            assertions.softAssertTrue(order.path("cod_amount").asDouble() > 0, "Something is left to pay on delivery");
        } finally {
            TorsavoApi.cancelOrderQuietly(customer, order.path("id").asText());
        }
    }

    @Test(groups = {REGRESSION}, description = "The same piece costs more with COD than prepaid by the prepaid discount")
    public void prepaidIsCheaperThanCod() {
        OffersInfo offers = offersForOrders();
        if (!offers.cod() || offers.prepaidDiscountPercent() <= 0) {
            throw new SkipException("Needs FEATURE_COD on and PREPAID_DISCOUNT_PERCENT > 0");
        }
        AuthSession customer = TorsavoApi.registerNewCustomer();
        ProductInfo product = TorsavoApi.products().stream().filter(p -> p.stock() >= 2).findFirst()
                .orElseThrow(() -> new SkipException("No product with 2+ in stock"));
        JsonNode prepaid = createOrder(customer, product, "PREPAID");
        JsonNode cod = null;
        try {
            HttpResult codRes = TorsavoApi.postJson("/orders", orderBody(product, "COD"), customer.accessToken());
            if (codRes.status() != 201) {
                throw new SkipException("COD order not possible here: " + codRes.body());
            }
            cod = TorsavoApi.json(codRes, "/orders");
            double itemsPrepaid = prepaid.path("total").asDouble() - prepaid.path("shipping_cost").asDouble();
            double itemsCod = cod.path("total").asDouble() - cod.path("shipping_cost").asDouble();
            assertions.assertTrue(Math.abs((itemsCod - itemsPrepaid) - prepaid.path("prepaid_discount").asDouble()) < CENT,
                    "Item total differs by exactly the prepaid discount — prepaid ₹" + itemsPrepaid + ", COD ₹" + itemsCod);
            assertions.softAssertTrue(cod.path("shipping_cost").asDouble() >= prepaid.path("shipping_cost").asDouble(),
                    "COD shipping should not be cheaper than prepaid");
        } finally {
            TorsavoApi.cancelOrderQuietly(customer, prepaid.path("id").asText());
            if (cod != null) {
                TorsavoApi.cancelOrderQuietly(customer, cod.path("id").asText());
            }
        }
    }

    @Test(groups = {REGRESSION}, description = "COD is refused while FEATURE_COD is off")
    public void codRefusedWhenDisabled() {
        OffersInfo offers = offersForOrders();
        if (offers.cod()) {
            throw new SkipException("FEATURE_COD is on");
        }
        AuthSession customer = TorsavoApi.registerNewCustomer();
        HttpResult res = TorsavoApi.postJson("/orders", orderBody(TorsavoApi.inStockProduct(), "COD"), customer.accessToken());
        assertions.assertEquals(res.status(), 400, "COD order while disabled");
        assertions.softAssertContains(res.body(), "Cash on delivery is not available", "Reason given");
    }

    @Test(groups = {REGRESSION}, description = "An unknown payment mode is rejected without creating an order")
    public void unknownPaymentModeIsRejected() {
        offersForOrders();
        AuthSession customer = TorsavoApi.registerNewCustomer();
        HttpResult res = TorsavoApi.postJson("/orders", orderBody(TorsavoApi.inStockProduct(), "CARD"), customer.accessToken());
        assertions.assertEquals(res.status(), 400, "paymentMode=CARD");
        JsonNode mine = TorsavoApi.json(TorsavoApi.get("/orders/my-orders", customer.accessToken()), "my-orders");
        JsonNode list = mine.isArray() ? mine : mine.path("data");
        assertions.softAssertEquals(list.size(), 0, "No order should have been created");
    }

    @Test(groups = {REGRESSION}, description = "The order records the customer's A/B group")
    public void orderRecordsExperimentGroup() {
        offersForOrders();
        TorsavoApi.requireExperimentsApi();
        AuthSession customer = TorsavoApi.registerNewCustomer();
        String group = TorsavoApi.codPopupVariant(customer);
        JsonNode order = createOrder(customer, TorsavoApi.inStockProduct(), "PREPAID");
        try {
            assertions.assertEquals(order.path("experiments").path("cod_popup").asText(), group,
                    "orders.experiments.cod_popup should match GET /experiments");
        } finally {
            TorsavoApi.cancelOrderQuietly(customer, order.path("id").asText());
        }
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Razorpay is asked for only the advance on a COD order, the full total on prepaid")
    public void razorpayChargesAdvanceForCodAndTotalForPrepaid() {
        OffersInfo offers = offersForOrders();
        AuthSession customer = TorsavoApi.registerNewCustomer();
        ProductInfo product = TorsavoApi.inStockProduct();
        JsonNode prepaid = createOrder(customer, product, "PREPAID");
        JsonNode cod = null;
        try {
            HttpResult pay = TorsavoApi.postJson("/payments/create-order", Map.of("orderId", prepaid.path("id").asText()), customer.accessToken());
            if (pay.status() >= 500) {
                throw new SkipException("Razorpay not configured on this backend: " + pay.body());
            }
            assertions.assertEquals(pay.status(), 200, "Prepaid create-order — " + pay.body());
            assertions.assertEquals(TorsavoApi.json(pay, "create-order").path("amount").asLong(),
                    Math.round(prepaid.path("total").asDouble() * 100), "Prepaid charge (paise) = order total");

            if (offers.cod()) {
                HttpResult codRes = TorsavoApi.postJson("/orders", orderBody(product, "COD"), customer.accessToken());
                if (codRes.status() == 201) {
                    cod = TorsavoApi.json(codRes, "/orders");
                    HttpResult codPay = TorsavoApi.postJson("/payments/create-order", Map.of("orderId", cod.path("id").asText()), customer.accessToken());
                    assertions.assertEquals(codPay.status(), 200, "COD create-order — " + codPay.body());
                    assertions.assertEquals(TorsavoApi.json(codPay, "create-order").path("amount").asLong(),
                            Math.round(offers.codAdvance() * 100), "COD charge (paise) = advance only");
                }
            }
        } finally {
            TorsavoApi.cancelOrderQuietly(customer, prepaid.path("id").asText());
            if (cod != null) {
                TorsavoApi.cancelOrderQuietly(customer, cod.path("id").asText());
            }
        }
    }
}
