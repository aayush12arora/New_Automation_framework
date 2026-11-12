package framework.pages.components;

import framework.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/** Site footer: trust bar, link columns and the bottom legal links. */
public class FooterComponent extends BasePage {

    private final By footerLinks = By.cssSelector("a.footer-link, a.footer-bottom-link");
    private final By trustTitles = By.cssSelector(".footer-trust-title");

    private By linkByText(String text) {
        return By.xpath("(//a[(contains(@class,'footer-link') or contains(@class,'footer-bottom-link'))"
                + " and normalize-space(.)='" + text + "'])[1]");
    }

    public List<String> linkTexts() {
        scrollIntoView(trustTitles);
        return findAll(footerLinks).stream()
                .map(e -> e.getDomProperty("textContent"))
                .map(String::trim)
                .toList();
    }

    public List<String> trustBadgeTitles() {
        return waitForAll(trustTitles).stream().map(BasePage::textContent).toList();
    }

    public void clickLink(String text) {
        click(linkByText(text));
    }

    public String linkHref(String text) {
        return getAttribute(linkByText(text), "href");
    }
}
