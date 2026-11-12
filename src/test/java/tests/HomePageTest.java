package tests;

import framework.base.TorsavoBaseTest;
import framework.constants.FrameworkConstants;
import framework.pages.HomePage;
import framework.pages.ShopPage;
import framework.utilities.PropertyManager;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/** Landing page: branding, hero, featured products and primary navigation. */
public class HomePageTest extends TorsavoBaseTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "Home page loads with the store name in the title")
    public void homePageLoadsWithBrandTitle() {
        HomePage home = new HomePage();
        home.header.navLinkTexts();
        assertions.assertContains(home.pageTitle(), PropertyManager.getOrDefault(FrameworkConstants.STORE_NAME, "Torsavo"),
                "Browser title should contain the store name");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Hero banner heading is rendered")
    public void heroHeadingIsDisplayed() {
        String heading = new HomePage().heroHeading();
        assertions.assertTrue(!heading.isBlank(), "Hero heading should have text");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Featured products load from the API and link to product pages")
    public void featuredProductsAreListed() {
        HomePage home = new HomePage();
        assertions.assertTrue(home.isFeaturedSectionShown(), "Featured section should be visible");
        List<String> hrefs = home.productHrefs();
        assertions.assertTrue(!hrefs.isEmpty(), "At least one featured product should be shown");
        hrefs.forEach(h -> assertions.softAssertTrue(h.startsWith("/product/"), "Product link should point to /product/: " + h));
    }

    @Test(groups = {REGRESSION}, description = "Header shows Shop, Collections, About and New Arrivals")
    public void headerShowsPrimaryNavigation() {
        List<String> links = new HomePage().header.navLinkTexts();
        for (String expected : List.of("Shop", "Collections", "About", "New Arrivals")) {
            assertions.softAssertTrue(links.contains(expected), "Header should contain '" + expected + "' — found " + links);
        }
    }

    @Test(groups = {REGRESSION}, description = "Clicking a featured product opens its product page")
    public void featuredProductOpensProductPage() {
        String href = new HomePage().openFirstProduct();
        waitForUrl(href);
        assertions.assertContains(getDriver().getCurrentUrl(), href, "Should navigate to the product page");
    }

    @Test(groups = {REGRESSION}, description = "'Explore Full Collection' opens the shop")
    public void exploreFullCollectionOpensShop() {
        new HomePage().clickExploreFullCollection();
        waitForUrl("/products");
        assertions.assertTrue(new ShopPage().waitForResults().productCount() > 0, "Shop should list products");
    }

    @Test(groups = {REGRESSION}, description = "Logo in the header returns to the home page")
    public void logoReturnsToHomeFromShop() {
        open("/products");
        new ShopPage().waitForResults().header.clickLogo();
        waitForPath("/");
        assertions.assertTrue(new HomePage().isFeaturedSectionShown(), "Home page should be shown after clicking the logo");
    }
}
