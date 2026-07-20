package tests;

import framework.base.BaseUITest;
import framework.pages.LoginPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Sample UI test for the login flow.
 *
 * <p>Conventions demonstrated:</p>
 * <ul>
 *   <li>Test data comes from {@code customerData} (loaded by BaseTest from
 *       {@code testdata/verifyLogin.json} — the file is named after the test).</li>
 *   <li>No JSON parsing here and no raw {@code Assert.*} — verification goes through
 *       {@code assertions} (soft asserts are finalised automatically by the framework).</li>
 *   <li>Pages are instantiated directly: {@code new LoginPage()}.</li>
 * </ul>
 */
public class LoginTest extends BaseUITest {

    @BeforeMethod(alwaysRun = true)
    public void launchApplication() {
        initializeApplication();
    }

    @Test
    public void verifyLogin() {
        LoginPage loginPage = new LoginPage();

        // Test data is already available via customerData (initialised in BaseTest).
        assertions.softAssertNotNull(customerData.getUsername(), "Username should load from test data");
        assertions.softAssertNotNull(customerData.getPassword(), "Password should load from test data");

        // Against a real login page this is the business flow:
        // loginPage.login(customerData.getUsername(), customerData.getPassword());
        assertions.softAssertContains(getDriver().getTitle(), "Example", "Page title should contain 'Example'");
    }
}
