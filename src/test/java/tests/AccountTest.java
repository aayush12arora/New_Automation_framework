package tests;

import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.base.TorsavoBaseTest;
import framework.pages.AccountPages;
import framework.pages.components.HeaderComponent;
import org.testng.annotations.Test;

import java.util.UUID;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/** Customer account area: My Orders, My Profile, order details. */
public class AccountTest extends TorsavoBaseTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "My Orders asks guests to log in")
    public void myOrdersAsGuestAsksToLogIn() {
        open("/account/orders");
        assertions.assertTrue(new AccountPages().waitForOrders().isOrdersLoginPromptShown(),
                "'Please log in to view your orders.' should be shown");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A new customer's order history is empty")
    public void newCustomerHasNoOrders() {
        signInAsNewCustomer();
        open("/account/orders");
        assertions.assertTrue(new AccountPages().waitForOrders().isNoOrdersMessageShown(),
                "New customer should see the 'no orders yet' message");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Profile shows the signed-in customer's name and e-mail")
    public void profileShowsSignedInCustomer() {
        AuthSession session = signInAsNewCustomer();
        new HeaderComponent().chooseProfileMenuItem("My Profile");
        waitForPath("/account/profile");
        AccountPages profile = new AccountPages().waitForProfile();
        assertions.assertEquals(profile.profileEmailValue(), session.email(), "Profile e-mail");
        assertions.softAssertEquals(profile.profileNameValue(), session.name(), "Profile name");
    }

    @Test(groups = {REGRESSION}, description = "Profile sends guests to the login page")
    public void profileAsGuestRedirectsToLogin() {
        open("/account/profile");
        waitForUrl("/login");
        assertions.assertContains(currentPath(), "/login", "Guest should be redirected to login");
    }

    @Test(groups = {REGRESSION}, description = "Customer can update their phone number")
    public void profilePhoneCanBeUpdated() {
        requireTestDataCreation();
        signInAsNewCustomer();
        HeaderComponent header = new HeaderComponent();
        header.chooseProfileMenuItem("My Profile");
        waitForPath("/account/profile");
        AccountPages profile = new AccountPages().waitForProfile();
        String phone = "98" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8);
        profile.setProfilePhone(phone);
        profile.saveProfile();
        assertions.assertTrue(profile.isToastShown("Profile updated successfully"), "Success toast should be shown");
        // Leave and come back: the form is re-fetched from the API, proving the change was saved.
        profile.clickViewOrders();
        waitForPath("/account/orders");
        header.chooseProfileMenuItem("My Profile");
        waitForPath("/account/profile");
        assertions.assertEquals(new AccountPages().waitForProfile().profilePhoneValue(), phone, "Phone should persist");
    }

    @Test(groups = {REGRESSION}, description = "Header profile menu opens My Orders")
    public void headerProfileMenuOpensMyOrders() {
        signInAsNewCustomer();
        new HeaderComponent().chooseProfileMenuItem("My Orders");
        waitForPath("/account/orders");
        assertions.assertTrue(new AccountPages().waitForOrders().isNoOrdersMessageShown(), "My Orders page should load");
    }

    @Test(groups = {REGRESSION}, description = "An unknown order id shows 'Order not found'")
    public void unknownOrderIdShowsNotFound() {
        signInAsNewCustomer();
        open("/order/" + UUID.randomUUID());
        assertions.assertTrue(new AccountPages().isOrderNotFoundShown(), "'Order not found' should be shown");
    }
}
