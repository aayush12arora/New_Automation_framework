package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.CategoryInfo;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.base.TorsavoBaseTest;
import framework.pages.ProductPage;
import framework.pages.ShopPage;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/** Product listing: grid, cards, category filter, search, sort and filter reset. */
public class ShopTest extends TorsavoBaseTest {

    private ShopPage openShop() {
        open("/products");
        return new ShopPage().waitForResults();
    }

    /** The category of a published product, as listed in the visible categories. */
    private CategoryInfo categoryWithProducts() {
        ProductInfo product = TorsavoApi.products().get(0);
        return TorsavoApi.categories().stream()
                .filter(c -> c.name().equalsIgnoreCase(product.category()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Category not visible: " + product.category()));
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Shop lists every published product")
    public void shopListsProducts() {
        ShopPage shop = openShop();
        int expected = Math.min(TorsavoApi.products().size(), 20);
        assertions.assertTrue(shop.productCount() > 0, "Shop should list products");
        assertions.softAssertEquals(shop.productCount(), expected, "First page should show all products from the API (up to 20)");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Every product card shows a name and a price")
    public void productCardsShowNameAndPrice() {
        ShopPage shop = openShop();
        List<String> names = shop.productNames();
        List<Double> prices = shop.productPrices();
        assertions.assertEquals(names.size(), shop.productCount(), "Every card should have a name");
        names.forEach(n -> assertions.softAssertTrue(!n.isBlank(), "Card name should not be blank"));
        prices.forEach(p -> assertions.softAssertTrue(p > 0, "Card price should be positive, was " + p));
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A product card opens the matching product page")
    public void productCardOpensProductPage() {
        ShopPage shop = openShop();
        String firstName = shop.productNames().get(0);
        shop.openProduct(0);
        waitForUrl("/product/");
        assertions.assertEquals(new ProductPage().waitForLoad().name(), firstName, "Product page should match the clicked card");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Choosing a collection shows only products from it")
    public void categoryFilterShowsOnlyThatCategory() {
        CategoryInfo category = categoryWithProducts();
        ShopPage shop = openShop();
        shop.selectCategory(category.name());
        assertions.assertTrue(shop.productCount() > 0, "Category should have products");
        shop.productCategories().forEach(c ->
                assertions.softAssertEquals(c, category.name(), "Every card should belong to " + category.name()));
        assertions.softAssertContains(getDriver().getCurrentUrl(), "category=" + category.slug(), "URL should carry the category slug");
    }

    @Test(groups = {REGRESSION}, description = "/products?category=<slug> pre-selects that collection")
    public void categoryDeepLinkPreselectsCategory() {
        CategoryInfo category = categoryWithProducts();
        open("/products?category=" + category.slug());
        ShopPage shop = new ShopPage().waitForResults();
        framework.utilities.WaitUtils.waitFor(d -> shop.productCategories().stream().allMatch(c -> c.equals(category.name())));
        assertions.assertTrue(shop.productCount() > 0, "Deep-linked category should list products");
        assertions.softAssertContains(shop.pageHeading(), category.name(), "Page heading should name the category");
    }

    @Test(groups = {REGRESSION}, description = "An unknown or hidden category redirects to all products")
    public void unknownCategoryRedirectsToAllProducts() {
        open("/products?category=no-such-category-qa");
        ShopPage shop = new ShopPage().waitForResults();
        assertions.assertTrue(!getDriver().getCurrentUrl().contains("category="), "Category param should be dropped");
        assertions.assertTrue(shop.productCount() > 0, "All products should be listed instead of an empty page");
    }

    @Test(groups = {REGRESSION}, description = "Sidebar search narrows the grid to matching products")
    public void sidebarSearchFindsMatchingProduct() {
        ProductInfo product = TorsavoApi.inStockProduct();
        ShopPage shop = openShop();
        shop.searchInSidebar(product.name());
        List<String> names = shop.productNames();
        assertions.assertTrue(names.contains(product.name()), "Search should find '" + product.name() + "' — got " + names);
        names.forEach(n -> assertions.softAssertTrue(n.toLowerCase().contains(product.name().toLowerCase().split(" ")[0]),
                "Unrelated product in results: " + n));
    }

    @Test(groups = {REGRESSION}, description = "A search with no matches shows the empty state")
    public void searchWithoutMatchesShowsEmptyState() {
        ShopPage shop = openShop();
        shop.searchInSidebar("zzqx-no-such-piece");
        assertions.assertTrue(shop.isEmptyStateShown(), "Empty state should be shown");
        assertions.assertEquals(shop.emptyStateHeading(), "No pieces found", "Empty-state heading");
    }

    @Test(groups = {REGRESSION}, description = "'Clear Filters' in the empty state brings all products back")
    public void clearFiltersRestoresFullCatalogue() {
        ShopPage shop = openShop();
        int all = shop.productCount();
        shop.searchInSidebar("zzqx-no-such-piece");
        shop.clearFiltersFromEmptyState();
        assertions.assertEquals(shop.productCount(), all, "All products should be listed again");
    }

    @Test(groups = {REGRESSION}, description = "Sort 'Price: Low to High' orders cards by ascending price")
    public void sortPriceLowToHigh() {
        ShopPage shop = openShop();
        shop.sortBy("Price: Low to High");
        List<Double> prices = shop.productPrices();
        List<Double> sorted = new ArrayList<>(prices);
        sorted.sort(Comparator.naturalOrder());
        assertions.assertEquals(prices, sorted, "Prices should be ascending");
    }

    @Test(groups = {REGRESSION}, description = "Sort 'Price: High to Low' orders cards by descending price")
    public void sortPriceHighToLow() {
        ShopPage shop = openShop();
        shop.sortBy("Price: High to Low");
        List<Double> prices = shop.productPrices();
        List<Double> sorted = new ArrayList<>(prices);
        sorted.sort(Comparator.reverseOrder());
        assertions.assertEquals(prices, sorted, "Prices should be descending");
    }

    @Test(groups = {REGRESSION}, description = "Active filters are counted and 'Clear all' resets them")
    public void activeFilterCountAndClearAll() {
        CategoryInfo category = categoryWithProducts();
        ShopPage shop = openShop();
        int all = shop.productCount();
        shop.selectCategory(category.name());
        assertions.assertContains(shop.activeFilterLabel(), "1 filter active", "One active filter should be reported");
        shop.clearAllFromSidebar();
        assertions.assertTrue(!shop.isActiveFilterLabelShown(), "Filter badge should disappear after 'Clear all'");
        assertions.assertEquals(shop.productCount(), all, "All products should be listed again");
    }
}
