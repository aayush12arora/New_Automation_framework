package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.base.TorsavoBaseTest;
import framework.pages.CartPage;
import framework.pages.LoginPage;
import framework.pages.components.HeaderComponent;
import org.testng.annotations.Test;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/**
 * Email/password sign-in, validation, redirect-after-login and sign-out.
 * {@code unknownEmailShowsInvalidCredentials} is data-driven from
 * {@code testdata/unknownEmailShowsInvalidCredentials.json} (loaded into {@code customerData}).
 */
public class LoginTest extends TorsavoBaseTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "Sign-in modal shows email, password and Google options")
    public void loginPageShowsSignInForm() {
        open("/login");
        LoginPage login = new LoginPage();
        assertions.assertTrue(login.isLoaded(), "Sign-in form should be visible");
        assertions.softAssertTrue(login.isGoogleSignInShown(), "'Continue with Google' should be offered");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Valid credentials sign the customer in and return them home")
    public void validCredentialsSignInAndRedirectHome() {
        AuthSession user = TorsavoApi.customerSession();
        open("/login");
        new LoginPage().login(user.email(), user.password());
        waitForPath("/");
        HeaderComponent header = new HeaderComponent();
        assertions.assertTrue(header.isSignedIn(), "Profile icon should replace the login icon");
        assertions.assertNotNull(new LoginPage().localStorageItem("refreshToken"), "Refresh token should be stored");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Wrong password is rejected with 'Invalid credentials'")
    public void wrongPasswordShowsInvalidCredentials() {
        AuthSession user = TorsavoApi.customerSession();
        open("/login");
        LoginPage login = new LoginPage();
        login.login(user.email(), user.password() + "-wrong");
        assertions.assertContains(login.errorMessage(), "Invalid credentials", "Error should explain the failure");
        assertions.assertContains(currentPath(), "/login", "User should stay on the login page");
    }

    @Test(groups = {REGRESSION}, description = "Unknown e-mail is rejected (data-driven)")
    public void unknownEmailShowsInvalidCredentials() {
        open("/login");
        LoginPage login = new LoginPage();
        login.login(customerData.get());
        assertions.assertContains(login.errorMessage(), "Invalid credentials", "Unknown account should be rejected");
    }

    @Test(groups = {REGRESSION}, description = "Submitting an empty form is blocked by required-field validation")
    public void emptyFormIsBlockedByRequiredValidation() {
        open("/login");
        LoginPage login = new LoginPage();
        login.clickSignIn();
        assertions.assertTrue(!login.emailValidationMessage().isBlank(), "E-mail field should report it is required");
        assertions.assertContains(currentPath(), "/login", "Form must not submit");
    }

    @Test(groups = {REGRESSION}, description = "A malformed e-mail is rejected before submitting")
    public void malformedEmailIsRejected() {
        open("/login");
        LoginPage login = new LoginPage();
        login.login("not-an-email", "whatever123");
        assertions.assertTrue(!login.emailValidationMessage().isBlank(), "E-mail field should flag the bad format");
    }

    @Test(groups = {REGRESSION}, description = "?redirect= sends the customer back to where they came from")
    public void redirectParameterReturnsUserToCart() {
        AuthSession user = TorsavoApi.customerSession();
        open("/login?redirect=/cart");
        new LoginPage().login(user.email(), user.password());
        waitForPath("/cart");
        assertions.assertTrue(new CartPage().waitForLoad().isEmpty(), "New customer should land on their (empty) cart");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Logout ends the session and clears stored tokens")
    public void logoutEndsSession() {
        signInAsNewCustomer();
        HeaderComponent header = new HeaderComponent();
        header.logout();
        waitForUrl("/login");
        assertions.assertTrue(new LoginPage().isLoaded(), "Sign-in form should be shown after logout");
        assertions.assertTrue(new LoginPage().localStorageItem("refreshToken") == null, "Refresh token should be removed");
    }

    @Test(groups = {REGRESSION}, description = "A signed-in session survives a full page reload")
    public void sessionSurvivesPageReload() {
        signInAsNewCustomer();
        getDriver().navigate().refresh();
        assertions.assertTrue(new HeaderComponent().isSignedIn(), "Customer should still be signed in after reload");
    }
}
