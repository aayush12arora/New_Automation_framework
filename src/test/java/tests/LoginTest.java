package tests;

import framework.base.BaseUITest;
import framework.pages.LoginPage;
import org.testng.annotations.Test;

/**
 * Sample UI test for the login flow.
 *
 * <p>Conventions demonstrated:</p>
 * <ul>
 *   <li>The application is launched automatically before each test — no
 *       {@code @BeforeMethod} here. The launch URL is set via {@code super(...)}
 *       (omit the constructor to use the URL from {@code framework.properties}).</li>
 *   <li>Test data comes from {@code customerData.get()} (loaded by BaseTest from
 *       {@code testdata/verifyLogin.<json|xlsx>} — the file is named after the test).</li>
 *   <li>No file parsing here and no raw {@code Assert.*} — verification goes through
 *       {@code assertions} (soft asserts are finalised automatically by the framework).</li>
 *   <li>Pages are instantiated directly: {@code new LoginPage()}.</li>
 *   <li>Categorised via {@code @Test(groups = ...)} — run a category with
 *       {@code mvn test -Dgroups=smoke} (see README "Test categories / groups").</li>
 * </ul>
 */
public class LoginTest extends BaseUITest {

    public LoginTest() {
        super("https://example.com");
    }

    @Test(groups = {"smoke", "regression"})
    public void verifyLogin() {
        LoginPage loginPage = new LoginPage();

        // Test data is already available via customerData.get() (initialised in BaseTest).
        assertions.softAssertNotNull(customerData.get().getUsername(), "Username should load from test data");
        assertions.softAssertNotNull(customerData.get().getPassword(), "Password should load from test data");

        // Against a real login page this is the business flow, passing the whole
        // resolved CustomerData object rather than pulling out individual fields:
        // loginPage.login(customerData.get());
        assertions.softAssertContains(getDriver().getTitle(), "Example", "Page title should contain 'Example'");
    }
}
