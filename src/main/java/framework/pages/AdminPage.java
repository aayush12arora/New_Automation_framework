package framework.pages;

import framework.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/** Admin panel ({@code /admin}): access guard, dashboard stats and sidebar navigation. */
public class AdminPage extends BasePage {

    private final By accessRestricted = By.xpath("//h2[normalize-space(.)='Access Restricted']");
    private final By pageTitle = By.cssSelector("h1[class*='pageTitle']");
    private final By statLabels = By.cssSelector("[class*='statLabel']");
    private final By sidebarLinks = By.cssSelector("nav a[class*='navLink'], aside a[class*='navLink']");

    public boolean isAccessRestrictedShown() {
        return isDisplayed(accessRestricted);
    }

    public String pageTitle() {
        return textContent(pageTitle);
    }

    public List<String> statLabels() {
        return waitForAll(statLabels).stream().map(BasePage::textContent).toList();
    }

    public List<String> sidebarLinkTexts() {
        return waitForAll(sidebarLinks).stream().map(BasePage::textContent).toList();
    }
}
