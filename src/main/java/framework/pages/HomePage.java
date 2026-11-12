package framework.pages;

import framework.base.BasePage;
import framework.pages.components.FooterComponent;
import framework.pages.components.HeaderComponent;
import framework.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/** Storefront landing page: hero, featured products, about carousel, footer. */
public class HomePage extends BasePage {

    private final By heroHeading = By.cssSelector("main h1");
    private final By featuredSection = By.cssSelector(".featured-section-wrapper");
    private final By productLinks = By.cssSelector("main a[href^='/product/']");
    private final By exploreFullCollection = By.xpath("//a[normalize-space(.)='Explore Full Collection']");
    private final By metaDescription = By.cssSelector("meta[name='description']");

    public final HeaderComponent header = new HeaderComponent();
    public final FooterComponent footer = new FooterComponent();

    public String heroHeading() {
        return getText(heroHeading);
    }

    /** The featured section fades in when scrolled into view, so scroll to it via the DOM first. */
    private void scrollToFeatured() {
        scrollToCenter(WaitUtils.waitForPresent(featuredSection));
    }

    public boolean isFeaturedSectionShown() {
        scrollToFeatured();
        return isDisplayed(featuredSection);
    }

    /** Distinct product links rendered on the page (featured carousel + grid). */
    public List<String> productHrefs() {
        scrollToFeatured();
        WaitUtils.waitForPresent(productLinks);
        return findAll(productLinks).stream()
                .map(e -> e.getDomAttribute("href"))
                .distinct()
                .toList();
    }

    public String openFirstProduct() {
        scrollToFeatured();
        WebElement link = waitForAll(productLinks).stream().filter(WebElement::isDisplayed).findFirst()
                .orElseThrow(() -> new IllegalStateException("No visible product link on the home page"));
        String href = link.getDomAttribute("href");
        scrollToCenter(link);
        js("arguments[0].click();", link);
        return href;
    }

    public void clickExploreFullCollection() {
        scrollToFeatured();
        scrollToCenter(WaitUtils.waitForPresent(exploreFullCollection));
        click(exploreFullCollection);
    }

    public String metaDescription() {
        return getAttribute(metaDescription, "content");
    }
}
