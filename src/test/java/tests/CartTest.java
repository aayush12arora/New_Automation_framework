package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.base.TorsavoBaseTest;
import framework.constants.FrameworkConstants;
import framework.pages.CartPage;
import framework.pages.LoginPage;
import framework.pages.ProductPage;
import framework.pages.ShopPage;
import framework.utilities.PropertyManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/**
 * Shopping cart (guest cart lives in localStorage; tests run as guests unless noted).
 * Every test starts from a fresh browser, so carts never leak between tests.
 */
public class CartTest extends TorsavoBaseTest {

    private CartPage openCart() {
        open("/cart");
        return new CartPage().waitForLoad();
    }

    private int freeShippingMinItems() {
        return Integer.parseInt(PropertyManager.getOrDefault(FrameworkConstants.FREE_SHIPPING_MIN_ITEMS, "2"));
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "An empty cart shows the empty state with a way back to the shop")
    public void emptyCartShowsEmptyState() {
        CartPage cart = openCart();
        assertions.assertTrue(cart.isEmpty(), "'Your cart is empty' should be shown");
        cart.clickBrowseCollections();
        waitForPath("/products");
        assertions.assertTrue(new ShopPage().waitForResults().productCount() > 0, "Shop should open from the empty cart");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "An added product appears in the cart with its size and colour")
    public void addedProductAppearsInCartWithChosenOptions() {
        ProductInfo product = addInStockProductToCart();
        CartPage cart = openCart();
        assertions.assertEquals(cart.itemNames(), List.of(product.name()), "Cart should hold exactly the added product");
        String line = cart.lineTexts().get(0);
        if (!product.colorOptions().isEmpty()) {
            assertions.softAssertContains(line, product.colorOptions().get(0), "Line should show the chosen colour");
        }
        List<String> sizes = TorsavoApi.effectiveSizes(product);
        if (!sizes.isEmpty()) {
            assertions.softAssertContains(line, sizes.get(0), "Line should show the chosen size");
        }
        assertions.softAssertEquals(cart.lineTotal(0), product.price(), "Line total for one piece");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Increasing quantity updates the line total and badge")
    public void increasingQuantityUpdatesLineTotal() {
        ProductInfo product = addInStockProductToCart();
        CartPage cart = openCart();
        cart.increaseQuantity(0);
        assertions.assertEquals(cart.quantityOfLine(0), 2, "Quantity should be 2");
        assertions.assertEquals(cart.lineTotal(0), product.price() * 2, "Line total should double");
        cart.header.waitForCartCount(2);
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Removing the only line empties the cart")
    public void removingLastItemEmptiesCart() {
        addInStockProductToCart();
        CartPage cart = openCart();
        cart.removeLine(0);
        assertions.assertTrue(cart.isEmpty(), "Cart should be empty after removing the item");
        assertions.assertEquals(cart.header.cartCount(), 0, "Badge should clear");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Guests must sign in to check out and come back to checkout")
    public void guestCheckoutRequiresSignIn() {
        addInStockProductToCart();
        openCart().proceedToCheckout();
        waitForUrl("/login");
        assertions.assertContains(getDriver().getCurrentUrl(), "redirect=/checkout", "Login should return to checkout");
        assertions.assertTrue(new LoginPage().isLoaded(), "Sign-in form should be shown");
    }

    @Test(groups = {REGRESSION}, description = "Decreasing quantity updates the line and the badge")
    public void decreasingQuantityUpdatesBadge() {
        addInStockProductToCart();
        CartPage cart = openCart();
        cart.increaseQuantity(0);
        cart.increaseQuantity(0);
        cart.decreaseQuantity(0);
        assertions.assertEquals(cart.quantityOfLine(0), 2, "Quantity should be 2 after +, +, -");
        cart.header.waitForCartCount(2);
    }

    @Test(groups = {REGRESSION}, description = "Same product in two sizes makes two separate lines")
    public void differentSizesAreSeparateLines() {
        ProductInfo product = TorsavoApi.inStockProduct();
        open("/product/" + product.slug());
        ProductPage page = new ProductPage().waitForLoad();
        if (page.sizes().size() < 2) {
            throw new SkipException("Product needs at least two sizes");
        }
        if (!page.colors().isEmpty()) {
            page.selectColorAt(0);
        }
        page.selectSizeAt(0);
        page.clickAddToCart();
        page.header.waitForCartCount(1);
        page.selectSizeAt(1);
        page.clickAddToCart();
        page.header.waitForCartCount(2);
        assertions.assertEquals(openCart().lineCount(), 2, "Each size should be its own cart line");
    }

    @Test(groups = {REGRESSION}, description = "A guest cart survives a page reload")
    public void guestCartSurvivesReload() {
        ProductInfo product = addInStockProductToCart();
        CartPage cart = openCart();
        getDriver().navigate().refresh();
        cart.waitForLoad();
        assertions.assertEquals(cart.itemNames(), List.of(product.name()), "Cart contents after reload");
    }

    @Test(groups = {REGRESSION}, description = "Subtotal equals the sum of all line totals")
    public void subtotalEqualsSumOfLines() {
        addInStockProductToCart();
        CartPage cart = openCart();
        cart.increaseQuantity(0);
        double sum = 0;
        for (int i = 0; i < cart.lineCount(); i++) {
            sum += cart.lineTotal(i);
        }
        assertions.assertEquals(cart.subtotal(), sum, "Subtotal should equal the sum of line totals");
        assertions.softAssertEquals(cart.total(), cart.subtotal(), "Total before shipping/coupons should equal subtotal");
    }

    @Test(groups = {REGRESSION}, description = "Below the free-shipping threshold, shipping is charged at checkout")
    public void shippingChargedBelowFreeThreshold() {
        if (freeShippingMinItems() <= 1) {
            throw new SkipException("Free shipping applies from one piece");
        }
        addInStockProductToCart();
        CartPage cart = openCart();
        assertions.assertTrue(!cart.shippingText().equalsIgnoreCase("Free"), "Shipping should not be free for one piece");
        assertions.assertTrue(cart.isFreeShippingHintShown(), "Cart should say how many more pieces unlock free shipping");
    }

    @Test(groups = {REGRESSION}, description = "Reaching the free-shipping threshold makes shipping free")
    public void shippingFreeAtThreshold() {
        addInStockProductToCart();
        CartPage cart = openCart();
        for (int i = 1; i < freeShippingMinItems(); i++) {
            cart.increaseQuantity(0);
        }
        assertions.assertEquals(cart.shippingText(), "Free", "Shipping should be free at " + freeShippingMinItems() + " pieces");
    }
}
