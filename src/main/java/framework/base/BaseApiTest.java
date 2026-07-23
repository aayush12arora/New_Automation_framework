package framework.base;

import framework.api.auth.TokenManager;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for API tests — the API-side sibling of {@link BaseUITest}.
 *
 * <p>It extends {@link BaseTest}, so it inherits the whole shared lifecycle: the Extent report
 * node, per-test data loaded from {@code testdata/<testName>.json|xlsx} into {@code customerData}
 * (same mechanism the UI side uses — one file per test method), the {@code TestListener}
 * (reporting + soft-assert finalisation), retry, and the shared {@link #assertions} helper. It
 * adds <b>no</b> WebDriver — these tests call HTTP endpoints, not a browser.</p>
 *
 * <p>The one thing every API test needs beyond the per-test data file is the auth token, so it
 * is handled once here rather than in every test/service: after {@link BaseTest#baseSetUp} loads
 * that test's data, this class's {@code @BeforeMethod} (subclass runs after superclass) reads
 * the token via {@link TokenManager} and sets it on the same {@code customerData} instance —
 * {@code customerData.get().getToken()}. Services take the token as a parameter, sourced from
 * there, so nothing calls {@code TokenManager} except this one place.</p>
 */
public abstract class BaseApiTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void setApiToken() {
        customerData.get().setToken(TokenManager.getToken());
    }
}
