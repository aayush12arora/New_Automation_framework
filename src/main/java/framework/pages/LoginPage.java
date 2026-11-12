package framework.pages;

import framework.base.BasePage;
import framework.data.customer.CustomerData;
import org.openqa.selenium.By;

/**
 * Torsavo sign-in modal ({@code /login}). The page renders the landing page behind a
 * glass overlay; the form itself has no ids, so fields are located by type inside the form.
 * Holds locators and business methods only — no assertions.
 */
public class LoginPage extends BasePage {

    /** The home page (with its own newsletter form) renders behind the modal, so scope to the sign-in form. */
    private static final String FORM = "//form[.//input[@type='password']]";

    private final By emailField = By.xpath(FORM + "//input[@type='email']");
    private final By passwordField = By.xpath(FORM + "//input[@type='password']");
    private final By signInButton = By.xpath(FORM + "//button[@type='submit']");
    private final By errorMessage = By.xpath(FORM + "//div[contains(@style,'rgb(220, 38, 38)') or contains(@style,'#dc2626')]");
    private final By heading = By.xpath("//h2[normalize-space(.)='Welcome Back']");
    private final By googleButton = By.xpath("//button[contains(normalize-space(.),'Continue with Google')]");
    private final By createAccountLink = By.xpath("//a[normalize-space(.)='Create Account']");

    public boolean isLoaded() {
        return isDisplayed(heading) && isDisplayed(emailField);
    }

    public void enterEmail(String email) {
        type(emailField, email);
    }

    public void enterPassword(String password) {
        type(passwordField, password);
    }

    public void clickSignIn() {
        click(signInButton);
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSignIn();
    }

    public void login(CustomerData customerData) {
        login(customerData.getEmail(), customerData.getPassword());
    }

    public String errorMessage() {
        return getText(errorMessage);
    }

    public boolean isGoogleSignInShown() {
        return isDisplayed(googleButton);
    }

    public void clickCreateAccount() {
        click(createAccountLink);
    }

    public String emailValidationMessage() {
        return validationMessage(emailField);
    }

    public String passwordValidationMessage() {
        return validationMessage(passwordField);
    }
}
