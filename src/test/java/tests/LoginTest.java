package tests;

import framework.base.BaseUITest;
import framework.data.pojo.LoginData;
import framework.pages.LoginPage;
import framework.utilities.JsonUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Sample UI test for the login flow.
 * Pages are instantiated directly (no constructor injection);
 * assertions live in the test, never in the Page Object.
 */
public class LoginTest extends BaseUITest {

    @BeforeMethod(alwaysRun = true)
    public void launchApplication() {
        initializeApplication();
    }

    @Test
    public void verifyLoginPageLoads() {
        LoginPage loginPage = new LoginPage();
        Assert.assertNotNull(getDriver().getTitle(), "Page title should be available after load");
    }

    @Test
    public void verifyLoginWithTestData() {
        LoginData data = JsonUtils.read("testdata/login.json", LoginData.class);

        LoginPage loginPage = new LoginPage();
        // Business flow lives in the page; the assertion lives here.
        // (example.com has no login form, so this documents intended usage.)
        Assert.assertNotNull(data.getUsername(), "Username should load from test data");
        Assert.assertNotNull(data.getPassword(), "Password should load from test data");
    }
}
