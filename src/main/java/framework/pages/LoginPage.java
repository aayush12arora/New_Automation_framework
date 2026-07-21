package framework.pages;

import framework.base.BasePage;
import framework.data.customer.CustomerData;
import org.openqa.selenium.By;

/**
 * Page Object for the login screen.
 * Holds locators and business methods only — no assertions,
 * no TestNG annotations, no validation logic.
 */
public class LoginPage extends BasePage {

    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By loginButton = By.cssSelector("button[type='submit']");
    private final By errorMessage = By.id("error");

    public void enterUsername(String username) {
        type(usernameField, username);
    }

    public void enterPassword(String password) {
        type(passwordField, password);
    }

    public void clickLogin() {
        click(loginButton);
    }

    /** Enters credentials and submits the form. */
    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }

    /** Convenience overload: logs in using credentials from a {@link CustomerData} object. */
    public void login(CustomerData customerData) {
        login(customerData.getUsername(), customerData.getPassword());
    }

    public String getErrorMessage() {
        return getText(errorMessage);
    }

    public boolean isLoginButtonDisplayed() {
        return isDisplayed(loginButton);
    }
}
