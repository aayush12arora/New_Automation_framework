package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.base.TorsavoBaseTest;
import framework.constants.FrameworkConstants;
import framework.pages.LoginPage;
import framework.pages.RegisterPage;
import framework.pages.components.HeaderComponent;
import framework.utilities.PropertyManager;
import org.testng.annotations.Test;

import java.util.UUID;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/** New-customer registration. */
public class RegisterTest extends TorsavoBaseTest {

    private String uniqueEmail() {
        return "torsavo.qa.ui." + UUID.randomUUID().toString().substring(0, 12) + "@"
                + PropertyManager.getOrDefault(FrameworkConstants.TEST_EMAIL_DOMAIN, "example.com");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Registration page shows name, e-mail and password fields")
    public void registrationPageShowsForm() {
        open("/register");
        assertions.assertTrue(new RegisterPage().isLoaded(), "Registration form should be visible");
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "A new customer can register and is signed in straight away")
    public void newCustomerCanRegisterAndIsSignedIn() {
        requireTestDataCreation();
        open("/register");
        new RegisterPage().register("QA New Customer", uniqueEmail(), "Qa#Pass1234");
        waitForPath("/");
        assertions.assertTrue(new HeaderComponent().isSignedIn(), "New customer should be signed in after registering");
    }

    @Test(groups = {REGRESSION}, description = "Registering an existing e-mail is rejected")
    public void duplicateEmailIsRejected() {
        AuthSession existing = TorsavoApi.customerSession();
        open("/register");
        RegisterPage page = new RegisterPage();
        page.register("Duplicate", existing.email(), "Qa#Pass1234");
        assertions.assertContains(page.errorMessage(), "already exists", "Duplicate account should be refused");
        assertions.assertContains(currentPath(), "/register", "User should stay on the registration page");
    }

    @Test(groups = {REGRESSION}, description = "A password shorter than 6 characters is rejected")
    public void shortPasswordIsRejected() {
        open("/register");
        RegisterPage page = new RegisterPage();
        page.register("Short Pass", uniqueEmail(), "12345");
        assertions.assertTrue(page.isErrorShown(), "An error should be shown for a too-short password");
        assertions.assertContains(currentPath(), "/register", "Account must not be created");
    }

    @Test(groups = {REGRESSION}, description = "Empty registration form is blocked by required fields")
    public void requiredFieldsAreEnforced() {
        open("/register");
        RegisterPage page = new RegisterPage();
        page.clickSubmit();
        assertions.assertTrue(!page.nameValidationMessage().isBlank(), "Name should be required");
        assertions.assertContains(currentPath(), "/register", "Form must not submit");
    }

    @Test(groups = {REGRESSION}, description = "Sign-in and registration pages link to each other")
    public void loginAndRegisterPagesLinkToEachOther() {
        open("/login");
        new LoginPage().clickCreateAccount();
        waitForPath("/register");
        RegisterPage register = new RegisterPage();
        assertions.assertTrue(register.isLoaded(), "Registration page should open from the sign-in modal");
        register.clickSignIn();
        waitForPath("/login");
        assertions.assertTrue(new LoginPage().isLoaded(), "Sign-in page should open from the registration page");
    }
}
