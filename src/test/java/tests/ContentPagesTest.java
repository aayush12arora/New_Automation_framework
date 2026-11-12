package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.CategoryInfo;
import framework.base.TorsavoBaseTest;
import framework.pages.ContentPages;
import framework.pages.ShopPage;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/**
 * About, Contact, Size Guide, Collections and the legal policies. Payment gateways and
 * ad platforms review the policy pages, so they are covered individually.
 */
public class ContentPagesTest extends TorsavoBaseTest {

    private ContentPages openPage(String path) {
        open(path);
        return new ContentPages();
    }

    private void assertPolicyPage(String path, String expectedHeadingWord) {
        ContentPages page = openPage(path);
        assertions.assertContains(page.heading().toLowerCase(), expectedHeadingWord.toLowerCase(), "Policy heading for " + path);
        assertions.assertTrue(page.sectionHeadings().size() >= 2, path + " should have policy sections");
        List<String> nav = page.policyNavTexts();
        assertions.softAssertTrue(nav.size() >= 4, "Other-policies navigation should list the policies — got " + nav);
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "About page loads with the brand story")
    public void aboutPageLoads() {
        ContentPages page = openPage("/about");
        assertions.assertTrue(!page.heading().isBlank(), "About page should have a heading");
        assertions.softAssertContains(page.bodyText().toLowerCase(), "our story", "About page should tell the brand story");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Contact page offers WhatsApp, e-mail and business details")
    public void contactPageShowsWhatsappAndEmail() {
        ContentPages page = openPage("/contact");
        assertions.assertEquals(page.heading(), "Talk to us", "Contact heading");
        assertions.assertContains(page.whatsappHref(), "wa.me", "WhatsApp card should link to wa.me");
        assertions.assertContains(page.emailHref(), "mailto:", "E-mail card should be a mailto link");
        assertions.softAssertContains(page.businessDetailsText(), "GSTIN", "Business details should include the GSTIN");
        assertions.softAssertTrue(page.isGrievanceOfficerShown(), "Grievance officer details are legally required");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Size guide shows a chart tab for every category")
    public void sizeGuideShowsChartForEachCategory() {
        ContentPages page = openPage("/size-guide");
        List<String> tabs = page.sizeTabNames();
        for (CategoryInfo c : TorsavoApi.categories()) {
            assertions.softAssertTrue(tabs.contains(c.name()), "Size chart tab for " + c.name() + " — got " + tabs);
        }
        assertions.assertTrue(page.sizeTableRowCount() > 0, "Active chart should have size rows");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Privacy policy page loads")
    public void privacyPolicyLoads() {
        assertPolicyPage("/privacy", "Privacy");
    }

    @Test(groups = {REGRESSION}, description = "Terms of use page loads")
    public void termsPageLoads() {
        assertPolicyPage("/terms", "Terms");
    }

    @Test(groups = {REGRESSION}, description = "Shipping policy page loads")
    public void shippingPolicyLoads() {
        assertPolicyPage("/shipping-policy", "Shipping");
    }

    @Test(groups = {REGRESSION}, description = "Returns & exchanges policy page loads")
    public void returnsPolicyLoads() {
        assertPolicyPage("/returns", "Return");
    }

    @Test(groups = {REGRESSION}, description = "Contact form requires a name and a message")
    public void contactFormRequiresNameAndMessage() {
        ContentPages page = openPage("/contact");
        page.submitContactForm();
        assertions.assertTrue(!page.contactNameValidation().isBlank(), "Name should be required");
        page.fillContactName("QA Tester");
        page.submitContactForm();
        assertions.assertTrue(!page.contactMessageValidation().isBlank(), "Message should be required");
        assertions.softAssertTrue(page.contactTopicCount() > 1, "Topic dropdown should offer several topics");
    }

    @Test(groups = {REGRESSION}, description = "Switching size-chart tabs selects that category")
    public void sizeGuideTabSwitching() {
        ContentPages page = openPage("/size-guide");
        List<String> tabs = page.sizeTabNames();
        if (tabs.size() < 2) {
            throw new SkipException("Only one size chart configured");
        }
        String target = tabs.get(tabs.size() - 1);
        page.selectSizeTab(target);
        assertions.assertEquals(page.selectedSizeTab(), target, "Clicked tab should be selected");
        assertions.softAssertContains(getDriver().getCurrentUrl(), "category=", "URL should remember the chosen chart");
    }

    @Test(groups = {REGRESSION}, description = "A collection tile opens the shop filtered to it")
    public void collectionsPageOpensFilteredShop() {
        ContentPages page = openPage("/collections");
        List<String> hrefs = page.collectionHrefs();
        assertions.assertTrue(!hrefs.isEmpty(), "Collections should be listed");
        page.openCollection(0);
        waitForUrl("/products");
        assertions.assertTrue(new ShopPage().waitForResults().productCount() >= 0, "Shop should load for the collection");
        assertions.softAssertContains(getDriver().getCurrentUrl(), "category=", "Shop URL should carry the collection");
    }
}
