package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.base.TorsavoBaseTest;
import framework.pages.LoginPage;
import framework.pages.ProductPage;
import framework.pages.ShopPage;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/** Product detail page: details, option pickers, add to cart / buy now, stock, tabs, reviews. */
public class ProductPageTest extends TorsavoBaseTest {

    private ProductPage openInStockProduct() {
        open("/product/" + TorsavoApi.inStockProduct().slug());
        return new ProductPage().waitForLoad();
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Product page shows name, API price and the GST-inclusive note")
    public void productPageShowsNamePriceAndTaxNote() {
        ProductInfo product = TorsavoApi.inStockProduct();
        ProductPage page = openInStockProduct();
        assertions.assertEquals(page.name(), product.name(), "Product name");
        assertions.assertEquals(page.price(), product.price(), "Displayed price should match the catalogue price");
        assertions.softAssertTrue(page.isTaxNoteShown(), "'Inclusive of all taxes' should be shown");
        assertions.softAssertTrue(page.isInStockShown(), "In-stock indicator should be shown");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Add to Cart without choosing a size is blocked with a prompt")
    public void addToCartWithoutSizeIsBlocked() {
        ProductPage page = openInStockProduct();
        if (page.sizes().size() < 2) {
            throw new SkipException("Product has a single/no size — nothing to choose");
        }
        if (!page.colors().isEmpty()) {
            page.selectColorAt(0);
        }
        page.clickAddToCart();
        assertions.assertTrue(page.isToastShown("Please select a size"), "Size prompt should appear");
        assertions.assertEquals(page.header.cartCount(), 0, "Nothing should be added to the cart");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Adding with size + colour confirms and updates the cart badge")
    public void addToCartWithOptionsUpdatesCartBadge() {
        ProductPage page = openInStockProduct();
        page.addToCartWithFirstOptions();
        assertions.assertTrue(page.isAddedToCartToastShown(), "'Added to Cart' confirmation should appear");
        page.header.waitForCartCount(1);
        assertions.assertEquals(page.header.cartCount(), 1, "Cart badge should show 1");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "An unknown product URL shows 'Product not found'")
    public void invalidProductSlugShowsNotFound() {
        open("/product/no-such-product-qa-404");
        ProductPage page = new ProductPage().waitForLoad();
        assertions.assertTrue(page.isNotFoundShown(), "'Product not found' should be shown");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "An out-of-stock product cannot be added to the cart")
    public void outOfStockProductCannotBeAdded() {
        ProductInfo product = TorsavoApi.outOfStockProduct();
        open("/product/" + product.slug());
        ProductPage page = new ProductPage().waitForLoad();
        assertions.assertTrue(!page.isAddToCartEnabled(), "Add to Cart should be disabled");
        assertions.softAssertEquals(page.addToCartButtonText(), "Out of Stock", "Button label");
        assertions.softAssertTrue(page.isOutOfStockShown(), "'Currently Unavailable' should be shown");
        assertions.softAssertTrue(!page.isBuyNowShown(), "Buy Now should be hidden");
    }

    @Test(groups = {REGRESSION}, description = "Add to Cart without choosing a colour is blocked with a prompt")
    public void addToCartWithoutColourIsBlocked() {
        ProductPage page = openInStockProduct();
        if (page.colors().size() < 2) {
            throw new SkipException("Product has a single/no colour — nothing to choose");
        }
        if (!page.sizes().isEmpty()) {
            page.selectSizeAt(0);
        }
        page.clickAddToCart();
        assertions.assertTrue(page.isToastShown("Please select a colour"), "Colour prompt should appear");
    }

    @Test(groups = {REGRESSION}, description = "Choosing a size highlights it and shows it in the label")
    public void selectedSizeIsHighlighted() {
        ProductPage page = openInStockProduct();
        List<String> sizes = TorsavoApi.effectiveSizes(TorsavoApi.inStockProduct());
        if (sizes.isEmpty()) {
            throw new SkipException("Product has no sizes");
        }
        String size = sizes.get(sizes.size() / 2);
        page.selectSize(size);
        assertions.assertTrue(page.isSizeSelected(size), "Chosen size button should be active");
        assertions.softAssertTrue(page.selectedOptionLabels().contains(": " + size), "Label should read ': " + size + "'");
    }

    @Test(groups = {REGRESSION}, description = "Buy Now as a guest asks the customer to sign in, then checkout")
    public void buyNowAsGuestRequiresSignIn() {
        ProductPage page = openInStockProduct();
        page.selectFirstOptions();
        page.clickBuyNow();
        waitForUrl("/login");
        assertions.assertContains(getDriver().getCurrentUrl(), "redirect=/checkout", "Login should return to checkout");
        assertions.assertTrue(new LoginPage().isLoaded(), "Sign-in form should be shown");
    }

    @Test(groups = {REGRESSION}, description = "Breadcrumb 'Shop' link returns to the listing")
    public void breadcrumbLeadsBackToShop() {
        ProductPage page = openInStockProduct();
        assertions.softAssertEquals(page.breadcrumbTexts().subList(0, 2), List.of("Home", "Shop"), "Breadcrumb trail");
        page.clickBreadcrumb("Shop");
        waitForPath("/products");
        assertions.assertTrue(new ShopPage().waitForResults().productCount() > 0, "Shop should list products");
    }

    @Test(groups = {REGRESSION}, description = "Size Guide link points at the product's category chart")
    public void sizeGuideLinkTargetsCategoryChart() {
        ProductPage page = openInStockProduct();
        String href = page.sizeGuideHref();
        assertions.assertContains(href, "/size-guide", "Size guide link");
        assertions.softAssertContains(href, "category=", "Link should open the product's own size chart");
    }

    @Test(groups = {REGRESSION}, description = "Description tab is open by default and shows content")
    public void descriptionTabIsShownByDefault() {
        ProductPage page = openInStockProduct();
        assertions.assertTrue(page.tabNames().contains("Description"), "Description tab should exist");
        assertions.assertEquals(page.activeTabName(), "Description", "Description should be the default tab");
        assertions.softAssertTrue(page.isTabContentShown(), "Tab content should be visible");
    }

    @Test(groups = {REGRESSION}, description = "Guests are asked to sign in before writing a review")
    public void guestSeesSignInPromptForReviews() {
        ProductPage page = openInStockProduct();
        assertions.assertTrue(page.isReviewLoginPromptShown(), "Review sign-in prompt should be shown to guests");
        assertions.softAssertTrue(!page.isReviewFormShown(), "Review form should be hidden from guests");
    }

    @Test(groups = {REGRESSION}, description = "Wishlist button toggles to 'Wishlisted'")
    public void wishlistToggleChangesLabel() {
        ProductPage page = openInStockProduct();
        assertions.assertContains(page.wishlistButtonText(), "Add to Wishlist", "Initial label");
        page.toggleWishlist();
        assertions.assertContains(page.wishlistButtonText(), "Wishlisted", "Label after toggling");
    }
}
