package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.base.TorsavoBaseTest;
import framework.pages.AdminPage;
import framework.pages.LoginPage;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/**
 * Admin panel access control and dashboard. The two admin-account tests are skipped
 * unless {@code adminEmail}/{@code adminPassword} are configured.
 */
public class AdminTest extends TorsavoBaseTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "Guests cannot open the admin panel")
    public void adminPanelBlocksGuests() {
        open("/admin");
        assertions.assertTrue(new AdminPage().isAccessRestrictedShown(), "'Access Restricted' should be shown to guests");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Customers cannot open the admin panel")
    public void adminPanelBlocksCustomers() {
        signInAsNewCustomer();
        open("/admin");
        assertions.assertTrue(new AdminPage().isAccessRestrictedShown(), "'Access Restricted' should be shown to customers");
    }

    @Test(groups = {REGRESSION}, description = "Admin sign-in lands on the dashboard")
    public void adminSignInLandsOnDashboard() {
        AuthSession admin = TorsavoApi.adminSession();
        open("/login");
        new LoginPage().login(admin.email(), admin.password());
        waitForPath("/admin");
        assertions.assertEquals(new AdminPage().pageTitle(), "Dashboard", "Admin should land on the dashboard");
    }

    @Test(groups = {REGRESSION}, description = "Dashboard shows revenue, orders, products and customers")
    public void dashboardShowsKeyStats() {
        signIn(TorsavoApi.adminSession());
        open("/admin");
        AdminPage admin = new AdminPage();
        List<String> stats = admin.statLabels();
        for (String expected : List.of("Total Revenue", "Total Orders", "Products", "Customers")) {
            assertions.softAssertTrue(stats.contains(expected), "Dashboard stat '" + expected + "' — got " + stats);
        }
        assertions.assertTrue(!admin.sidebarLinkTexts().isEmpty(), "Admin navigation should render");
    }
}
