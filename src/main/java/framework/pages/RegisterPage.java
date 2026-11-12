package framework.pages;

import framework.base.BasePage;
import org.openqa.selenium.By;

/** Customer registration page ({@code /register}). */
public class RegisterPage extends BasePage {

    private final By heading = By.xpath("//h1[normalize-space(.)='Create Account']");
    private static final String FORM = "//form[.//input[@type='password']]";

    private final By nameField = By.xpath(FORM + "//input[@type='text']");
    private final By emailField = By.xpath(FORM + "//input[@type='email']");
    private final By passwordField = By.xpath(FORM + "//input[@type='password']");
    private final By submitButton = By.xpath(FORM + "//button[@type='submit']");
    private final By errorMessage = By.xpath(FORM + "//p[contains(@class,'error')]");
    private final By signInLink = By.xpath("//a[normalize-space(.)='Sign In']");

    public boolean isLoaded() {
        return isDisplayed(heading) && isDisplayed(nameField);
    }

    public void register(String name, String email, String password) {
        type(nameField, name);
        type(emailField, email);
        type(passwordField, password);
        click(submitButton);
    }

    public void clickSubmit() {
        click(submitButton);
    }

    public String errorMessage() {
        return getText(errorMessage);
    }

    public boolean isErrorShown() {
        return isDisplayed(errorMessage);
    }

    public String nameValidationMessage() {
        return validationMessage(nameField);
    }

    public void clickSignIn() {
        click(signInLink);
    }
}
