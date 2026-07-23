package framework.base;

/**
 * Base class for API tests — the API-side sibling of {@link BaseUITest}.
 *
 * <p>It extends {@link BaseTest}, so it inherits the whole shared lifecycle: the Extent report
 * node, test data ({@code customerData}, loaded from {@code testdata/<testName>.json|xlsx}),
 * the {@code TestListener} (reporting + soft-assert finalisation), retry, and the shared
 * {@link #assertions} helper. It adds <b>no</b> WebDriver — these tests call HTTP endpoints,
 * not a browser.</p>
 *
 * <p>Usage mirrors the UI side: an API test extends this class, instantiates a client
 * directly (e.g. {@code new HomeClient()} — like {@code new LoginPage()}), reads its inputs
 * from {@code customerData.get()}, and verifies through {@code assertions}.</p>
 */
public abstract class BaseApiTest extends BaseTest {
    // Intentionally minimal: all shared behaviour is inherited from BaseTest.
    // This class exists so every API test has a clear, API-specific base to extend,
    // symmetric with BaseUITest, and a home for any future API-only setup.
}
