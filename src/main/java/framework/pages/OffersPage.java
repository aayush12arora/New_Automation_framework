package framework.pages;

import framework.base.BasePage;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Bundle offers ({@code /offers}). Rendered only while the backend has {@code FEATURE_BUNDLES=true};
 * otherwise the route is a 404.
 */
public class OffersPage extends BasePage {

    private final By heading = By.xpath("//h1[normalize-space(.)='Mix any styles. Pay one price.']");
    private final By cardTitles = By.cssSelector("article[class*='offerCard'] h2");
    private final By emptyMessage = By.xpath("//p[starts-with(normalize-space(.),'No bundle offers right now')]");
    private final By shopLinks = By.cssSelector("article[class*='offerCard'] a[class*='shopLink']");

    public boolean isLoaded() {
        return isDisplayed(heading);
    }

    public List<String> bundleTitles() {
        return isDisplayed(cardTitles, 5) ? findAll(cardTitles).stream().map(BasePage::textContent).toList() : List.of();
    }

    public boolean isEmptyMessageShown() {
        return isDisplayed(emptyMessage, 3);
    }

    /** hrefs of the "Shop" links on the bundle cards, e.g. /products?category=shirts */
    public List<String> shopLinkHrefs() {
        return findAll(shopLinks).stream().map(e -> e.getDomAttribute("href")).toList();
    }
}
