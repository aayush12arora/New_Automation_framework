package framework.base;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.AuthSession;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.pages.ProductPage;
import framework.pages.components.HeaderComponent;
import framework.utilities.StepLogger;
import framework.utilities.WaitUtils;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

import java.net.URI;

/**
 * Base class for every Torsavo storefront test. Adds store-specific helpers on top of
 * {@link BaseUITest} (which already opens {@code url} from {@code framework.properties}
 * before each test):
 *
 * <ul>
 *   <li>{@link #open(String)} — navigate to a path on the configured store;</li>
 *   <li>{@link #signInAsNewCustomer()} — fast sign-in: tokens come from the API and are
 *       placed in localStorage exactly as the storefront's own login does, so tests that
 *       are not <em>about</em> logging in don't spend time on the login form;</li>
 *   <li>{@link #addInStockProductToCart()} — the common "get something into the cart" setup;</li>
 *   <li>a guard that fails fast with a clear message if the store is in launch-queue mode
 *       (every shop page redirects to {@code /launch} until launch).</li>
 * </ul>
 *
 * <p>Pointing the suite at another environment only needs {@code url} (and {@code apiUrl}
 * when the API host can't be derived) in {@code framework.properties}.</p>
 */
public abstract class TorsavoBaseTest extends BaseUITest {

    @BeforeMethod(alwaysRun = true)
    public void verifyStoreIsOpen() {
        String path = URI.create(getDriver().getCurrentUrl()).getPath();
        if ("/launch".equals(path) && !TorsavoApi.siteUrl().endsWith("/launch")) {
            throw new IllegalStateException("The store at " + TorsavoApi.siteUrl() + " is in launch-queue mode "
                    + "(NEXT_PUBLIC_PRELAUNCH=true): every shop page redirects to /launch, so storefront tests cannot run.");
        }
    }

    /** Navigates to a path on the configured store, e.g. {@code open("/cart")}. */
    protected void open(String path) {
        navigateTo(TorsavoApi.siteUrl() + path);
        WaitUtils.waitForPageLoad();
    }

    /** Path of the current page, e.g. {@code /products}. */
    protected String currentPath() {
        return URI.create(getDriver().getCurrentUrl()).getPath();
    }

    /** Waits until the URL contains {@code fragment}. */
    protected void waitForUrl(String fragment) {
        WaitUtils.waitForUrlContains(fragment);
    }

    /** Waits until the page path equals {@code path} exactly. */
    protected void waitForPath(String path) {
        WaitUtils.waitFor(d -> path.equals(URI.create(d.getCurrentUrl()).getPath()));
    }

    /** Signs a session into the browser (same localStorage keys the storefront's login writes). */
    protected void signIn(AuthSession session) {
        String current = getDriver().getCurrentUrl();
        if (!current.startsWith(TorsavoApi.siteUrl())) {
            open("/");
        }
        ((org.openqa.selenium.JavascriptExecutor) getDriver()).executeScript(
                "localStorage.setItem('refreshToken', arguments[0]); localStorage.setItem('user', arguments[1]);",
                session.refreshToken(), session.userJson());
        StepLogger.step(log, "Signed in as " + session.email() + " (session injected)");
    }

    /** Registers (or reuses) a customer, signs them in and lands on the home page signed in. */
    protected AuthSession signInAsNewCustomer() {
        AuthSession session = TorsavoApi.customerSession();
        signIn(session);
        open("/");
        if (!new HeaderComponent().isSignedIn()) {
            throw new IllegalStateException("Injected session for " + session.email() + " was not accepted by the storefront");
        }
        return session;
    }

    /** Opens the in-stock product, picks its first size/colour and adds it to the cart. */
    protected ProductInfo addInStockProductToCart() {
        ProductInfo product = TorsavoApi.inStockProduct();
        open("/product/" + product.slug());
        ProductPage page = new ProductPage().waitForLoad();
        page.addToCartWithFirstOptions();
        return product;
    }

    /** Skips the current test when the run must not create accounts/orders (createTestData=false). */
    protected void requireTestDataCreation() {
        if (!TorsavoApi.createTestDataAllowed()) {
            throw new SkipException("createTestData=false — test creates accounts/orders and was skipped");
        }
    }
}
