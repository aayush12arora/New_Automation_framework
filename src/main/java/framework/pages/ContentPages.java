package framework.pages;

import framework.base.BasePage;
import framework.pages.components.FooterComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Static/content pages sharing the ContentPage / PolicyLayout shells: About, Contact,
 * Size Guide, Collections, and the policy pages (Privacy, Terms, Shipping, Returns).
 */
public class ContentPages extends BasePage {

    private final By h1 = By.cssSelector("h1");
    private final By sectionHeadings = By.cssSelector("main h2, h2");

    // Policies
    private final By policyNavLinks = By.cssSelector("nav.pl-other a");
    private final By tocLinks = By.cssSelector(".toc-label + ul a, .toc-label ~ ul a");

    // Contact
    private final By whatsappCard = By.cssSelector("a.ct-card-featured");
    private final By emailCard = By.xpath("//a[contains(@class,'ct-card')][.//span[normalize-space(.)='Email']]");
    private final By contactName = By.id("ct-name");
    private final By contactMessage = By.id("ct-message");
    private final By contactTopic = By.id("ct-topic");
    private final By contactSubmit = By.xpath("//button[@type='submit'][normalize-space(.)='Write the email']");
    private final By grievancePanel = By.id("grievance-officer");
    private final By businessDetails = By.xpath("//h3[contains(normalize-space(.),'Business details')]/following-sibling::dl");

    // Size guide
    private final By sizeTabs = By.cssSelector("[role='tablist'] [role='tab']");
    private final By selectedSizeTab = By.cssSelector("[role='tab'][aria-selected='true']");
    private final By sizeTableRows = By.cssSelector("[role='tabpanel'] table tbody tr");

    // Collections
    private final By collectionLinks = By.cssSelector("a.cx-link");

    public final FooterComponent footer = new FooterComponent();

    public String heading() {
        return textContent(h1);
    }

    public List<String> sectionHeadings() {
        return waitForAll(sectionHeadings).stream().map(WebElement::getText).map(String::trim)
                .filter(s -> !s.isEmpty()).toList();
    }

    public String bodyText() {
        return waitForVisible(By.tagName("body")).getText();
    }

    // ------------------------------------------------------------------ Policies

    public List<String> policyNavTexts() {
        return waitForAll(policyNavLinks).stream().map(e -> e.getDomProperty("textContent").trim()).toList();
    }

    public void openPolicyFromNav(String text) {
        click(By.xpath("//nav[contains(@class,'pl-other')]//a[normalize-space(.)='" + text + "']"));
    }

    public int tocLinkCount() {
        return findAll(tocLinks).size();
    }

    // ------------------------------------------------------------------ Contact

    public String whatsappHref() {
        return getAttribute(whatsappCard, "href");
    }

    public String emailHref() {
        return getAttribute(emailCard, "href");
    }

    public void submitContactForm() {
        click(contactSubmit);
    }

    public String contactNameValidation() {
        return validationMessage(contactName);
    }

    public void fillContactName(String name) {
        type(contactName, name);
    }

    public String contactMessageValidation() {
        return validationMessage(contactMessage);
    }

    public int contactTopicCount() {
        return findAll(contactTopic).isEmpty() ? 0 : findAll(By.cssSelector("#ct-topic option")).size();
    }

    public boolean isGrievanceOfficerShown() {
        scrollIntoView(grievancePanel);
        return isDisplayed(grievancePanel);
    }

    public String businessDetailsText() {
        scrollIntoView(businessDetails);
        return getText(businessDetails);
    }

    // ------------------------------------------------------------------ Size guide

    public List<String> sizeTabNames() {
        return waitForAll(sizeTabs).stream().map(BasePage::textContent).toList();
    }

    public void selectSizeTab(String name) {
        click(By.xpath("//*[@role='tab'][normalize-space(.)='" + name + "']"));
        // The selected tab comes from the URL (shallow router.replace), so it updates a tick later.
        framework.utilities.WaitUtils.waitFor(d -> name.equals(textContent(d.findElement(selectedSizeTab))));
    }

    public String selectedSizeTab() {
        return textContent(selectedSizeTab);
    }

    public int sizeTableRowCount() {
        return waitForAll(sizeTableRows).size();
    }

    // ------------------------------------------------------------------ Collections

    public List<String> collectionHrefs() {
        return waitForAll(collectionLinks).stream().map(e -> e.getDomAttribute("href")).toList();
    }

    public void openCollection(int index) {
        WebElement link = waitForAll(collectionLinks).get(index);
        scrollToCenter(link);
        js("arguments[0].click();", link);
    }
}
