package tests;

import com.fasterxml.jackson.databind.JsonNode;
import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.api.torsavo.TorsavoApi.HttpResult;
import framework.base.TorsavoBaseTest;
import framework.pages.AdminPage;
import org.openqa.selenium.By;
import framework.utilities.WaitUtils;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/**
 * A/B test "cod_popup" plumbing: the customer's group ({@code GET /experiments}), checkout step
 * tracking ({@code POST /experiments/events}) and the admin results page. Skipped against a
 * backend that predates the release.
 */
public class ExperimentsApiTest extends TorsavoBaseTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "A customer's A/B group is popup or control and never changes")
    public void codPopupGroupIsStablePerCustomer() {
        TorsavoApi.requireExperimentsApi();
        requireTestDataCreation();
        AuthSession customer = TorsavoApi.registerNewCustomer();
        String first = TorsavoApi.codPopupVariant(customer);
        assertions.assertTrue(List.of("popup", "control").contains(first), "Group should be popup or control — got " + first);
        AuthSession again = TorsavoApi.login(customer.email(), customer.password());
        assertions.assertEquals(TorsavoApi.codPopupVariant(again), first, "Group must stay the same across sessions");
    }

    @Test(groups = {REGRESSION}, description = "Guests have no A/B group and can't record events")
    public void experimentsRequireSignIn() {
        TorsavoApi.requireExperimentsApi();
        assertions.softAssertEquals(TorsavoApi.get("/experiments", null).status(), 401, "GET /experiments as guest");
        HttpResult event = TorsavoApi.postJson("/experiments/events",
                Map.of("experiment", "cod_popup", "event", "checkout_viewed"), null);
        assertions.softAssertEquals(event.status(), 401, "POST /experiments/events as guest");
        assertions.softAssertEquals(TorsavoApi.get("/experiments/cod-popup/results", null).status(), 401,
                "Results as guest");
    }

    @Test(groups = {REGRESSION}, description = "A signed-in customer gets their cod_popup group")
    public void signedInCustomerGetsGroup() {
        TorsavoApi.requireExperimentsApi();
        AuthSession customer = TorsavoApi.customerSession();
        HttpResult res = TorsavoApi.get("/experiments", customer.accessToken());
        assertions.assertEquals(res.status(), 200, "GET /experiments — " + res.body());
        JsonNode body = TorsavoApi.json(res, "/experiments");
        assertions.assertTrue(List.of("popup", "control").contains(body.path("codPopup").asText()),
                "codPopup should be popup or control — got " + body);
    }

    @Test(groups = {REGRESSION}, description = "Every checkout step event is accepted")
    public void validEventsAreRecorded() {
        TorsavoApi.requireExperimentsApi();
        requireTestDataCreation();
        AuthSession customer = TorsavoApi.registerNewCustomer();
        for (String event : List.of("checkout_viewed", "cod_selected", "popup_shown",
                "popup_switched_prepaid", "popup_kept_cod", "popup_timeout")) {
            HttpResult res = TorsavoApi.postJson("/experiments/events",
                    Map.of("experiment", "cod_popup", "event", event, "paymentMode", "COD"), customer.accessToken());
            assertions.softAssertEquals(res.status(), 204, "Event '" + event + "' — " + res.body());
        }
    }

    @Test(groups = {REGRESSION}, description = "Unknown experiments, events and payment modes are rejected")
    public void invalidEventsAreRejected() {
        TorsavoApi.requireExperimentsApi();
        AuthSession customer = TorsavoApi.customerSession();
        String token = customer.accessToken();
        assertions.softAssertEquals(TorsavoApi.postJson("/experiments/events",
                Map.of("experiment", "other_test", "event", "checkout_viewed"), token).status(), 400, "Unknown experiment");
        assertions.softAssertEquals(TorsavoApi.postJson("/experiments/events",
                Map.of("experiment", "cod_popup", "event", "purchased"), token).status(), 400, "Unknown event");
        assertions.softAssertEquals(TorsavoApi.postJson("/experiments/events",
                Map.of("experiment", "cod_popup", "event", "cod_selected", "paymentMode", "CARD"), token).status(), 400,
                "Unknown payment mode");
    }

    @Test(groups = {REGRESSION}, description = "Customers can't read A/B results")
    public void resultsAreAdminOnly() {
        TorsavoApi.requireExperimentsApi();
        AuthSession customer = TorsavoApi.customerSession();
        assertions.assertEquals(TorsavoApi.get("/experiments/cod-popup/results", customer.accessToken()).status(), 403,
                "Customer reading results");
    }

    @Test(groups = {REGRESSION}, description = "Admin results list the popup and control groups with consistent counts")
    public void adminResultsAreConsistent() {
        TorsavoApi.requireExperimentsApi();
        AuthSession admin = TorsavoApi.adminSession();
        HttpResult res = TorsavoApi.get("/experiments/cod-popup/results", admin.accessToken());
        assertions.assertEquals(res.status(), 200, "Results status — " + res.body());
        JsonNode results = TorsavoApi.json(res, "results");
        assertions.softAssertEquals(results.path("experiment").asText(), "cod_popup", "Experiment name");
        int percent = results.path("popupPercent").asInt(-1);
        assertions.softAssertTrue(percent >= 0 && percent <= 100, "popupPercent 0-100 — got " + percent);
        List<String> variants = new java.util.ArrayList<>();
        for (JsonNode g : results.path("groups")) {
            String v = g.path("variant").asText();
            variants.add(v);
            JsonNode c = g.path("customers");
            JsonNode o = g.path("orders");
            assertions.softAssertTrue(c.path("sawPopup").asInt() >= c.path("switchedToPrepaid").asInt(),
                    v + ": switched to prepaid ≤ saw popup");
            assertions.softAssertEquals(o.path("prepaid").asInt() + o.path("cod").asInt(), o.path("paid").asInt(),
                    v + ": prepaid + COD = paid orders");
            if ("control".equals(v)) {
                assertions.softAssertEquals(c.path("sawPopup").asInt(), 0, "Control customers never see the popup");
            }
        }
        assertions.assertEquals(variants, List.of("popup", "control"), "Groups reported");
    }

    @Test(groups = {REGRESSION}, description = "Admin A/B tests page shows the COD popup results table")
    public void adminExperimentsPageShowsResults() {
        TorsavoApi.requireExperimentsApi();
        signIn(TorsavoApi.adminSession());
        open("/admin/experiments");
        AdminPage admin = new AdminPage();
        assertions.softAssertTrue(admin.sidebarLinkTexts().contains("A/B tests"), "Sidebar should link to A/B tests");
        By header = By.xpath("//table//thead//th");
        WaitUtils.waitFor(d -> d.findElements(header).size() >= 3);
        List<String> headers = getDriver().findElements(header).stream().map(e -> e.getText().trim().toLowerCase()).toList();
        assertions.assertTrue(headers.contains("popup") && headers.contains("control"),
                "Results table should have popup and control columns — got " + headers);
        assertions.softAssertTrue(!getDriver().findElements(By.xpath("//td[starts-with(normalize-space(.),'Popup switch rate')]")).isEmpty(),
                "Popup switch rate row should be shown");
    }
}
