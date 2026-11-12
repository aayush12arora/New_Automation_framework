package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.HttpResult;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.base.TorsavoBaseTest;
import framework.pages.ContentPages;
import framework.pages.HomePage;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/** Footer navigation, 404 handling and the crawler files (robots.txt, sitemap.xml). */
public class FooterAndSeoTest extends TorsavoBaseTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "Footer links to contact, shipping, returns, size guide and legal pages")
    public void footerHasCustomerCareLinks() {
        List<String> links = new HomePage().footer.linkTexts();
        for (String expected : List.of("Contact Us", "Shipping & Delivery", "Returns & Exchanges", "Size Guide",
                "Privacy Policy", "Terms of Use")) {
            assertions.softAssertTrue(links.contains(expected), "Footer should link to '" + expected + "' — got " + links);
        }
        assertions.assertTrue(!links.isEmpty(), "Footer links should render");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "An unknown route returns the 404 page")
    public void unknownRouteShows404() {
        HttpResult res = TorsavoApi.getUrl(TorsavoApi.siteUrl() + "/no-such-page-qa-404");
        assertions.assertEquals(res.status(), 404, "HTTP status for an unknown route");
        open("/no-such-page-qa-404");
        assertions.softAssertContains(new ContentPages().bodyText(), "404", "Browser should show the 404 page");
    }

    @Test(groups = {REGRESSION}, description = "Footer 'Size Guide' link opens the size guide")
    public void footerSizeGuideLinkNavigates() {
        new HomePage().footer.clickLink("Size Guide");
        waitForPath("/size-guide");
        assertions.assertTrue(!new ContentPages().sizeTabNames().isEmpty(), "Size guide should show chart tabs");
    }

    @Test(groups = {REGRESSION}, description = "robots.txt is served and points to the sitemap")
    public void robotsTxtIsServed() {
        HttpResult res = TorsavoApi.getUrl(TorsavoApi.siteUrl() + "/robots.txt");
        assertions.assertEquals(res.status(), 200, "robots.txt status");
        assertions.assertContains(res.body(), "User-agent", "robots.txt should have crawler rules");
        assertions.softAssertContains(res.body(), "Sitemap:", "robots.txt should reference the sitemap");
    }

    @Test(groups = {REGRESSION}, description = "sitemap.xml is served and lists product pages")
    public void sitemapListsProductPages() {
        HttpResult res = TorsavoApi.getUrl(TorsavoApi.siteUrl() + "/sitemap.xml");
        assertions.assertEquals(res.status(), 200, "sitemap.xml status");
        assertions.assertContains(res.body(), "<urlset", "Sitemap should be a urlset");
        ProductInfo product = TorsavoApi.inStockProduct();
        assertions.softAssertContains(res.body(), "/product/" + product.slug(), "Sitemap should include in-stock products");
    }
}
