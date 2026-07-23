package framework.base;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import framework.api.config.ApiConfig;
import framework.assertions.UIAssertions;
import framework.constants.FrameworkConstants;
import framework.data.api.ApiTestData;
import framework.reporting.ExtentReportManager;
import framework.reporting.TestListener;
import framework.utilities.ExcelUtils;
import framework.utilities.JsonUtils;
import framework.utilities.LoggerUtil;
import framework.utilities.PropertyManager;
import org.slf4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.lang.reflect.Method;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Base class for API tests — the API-side sibling of {@link BaseTest}. It reuses all the
 * shared framework infrastructure (Extent reporting via {@link TestListener}, soft-assert
 * finalisation, retry, {@link UIAssertions}, config, Excel/JSON test data) but does
 * <b>not</b> create a WebDriver, since these tests hit HTTP endpoints, not a browser.
 *
 * <p>In {@code mock} mode it starts a WireMock server for the test class and resets its stubs
 * before each test, so the suite runs green with no live environment. Mock-based test classes
 * should be annotated {@code @Test(singleThreaded = true)} so parallel methods don't clobber
 * each other's stubs on the shared server.</p>
 */
@Listeners(TestListener.class)
public abstract class BaseApiTest {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    /** Assertion helper (hard + soft), shared with the UI side. */
    protected final UIAssertions assertions = new UIAssertions();

    /** Shared API test data loaded before each test — read values, never hard-code them. */
    protected ApiTestData apiData;

    private WireMockServer wireMockServer;

    @BeforeClass(alwaysRun = true)
    public void startMockServer() {
        if (ApiConfig.isMock()) {
            wireMockServer = new WireMockServer(options().port(ApiConfig.mockPort()));
            wireMockServer.start();
            WireMock.configureFor("localhost", ApiConfig.mockPort());
            log.info("WireMock started on port {}", ApiConfig.mockPort());
        }
    }

    @AfterClass(alwaysRun = true)
    public void stopMockServer() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            log.info("WireMock stopped");
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void apiSetUp(Method method) {
        ExtentReportManager.createTest(getClass().getSimpleName() + "." + method.getName());
        apiData = loadApiData();
        if (ApiConfig.isMock() && wireMockServer != null) {
            WireMock.reset();
        }
    }

    @AfterMethod(alwaysRun = true)
    public void apiTearDown() {
        ExtentReportManager.remove();
    }

    /** @return {@code true} if running against WireMock rather than the live server. */
    protected boolean isMock() {
        return ApiConfig.isMock();
    }

    /**
     * Loads shared API data from JSON or Excel (chosen by the {@code dataSource} property),
     * returning an empty object when no file exists so {@link #apiData} is never null.
     */
    private ApiTestData loadApiData() {
        boolean excel = FrameworkConstants.DATA_SOURCE_EXCEL.equalsIgnoreCase(
                PropertyManager.get(FrameworkConstants.DATA_SOURCE));
        String resource = FrameworkConstants.API_TESTDATA + (excel ? ".xlsx" : ".json");
        if (!JsonUtils.resourceExists(resource)) {
            log.info("No API test-data file '{}' found; using empty ApiTestData", resource);
            return new ApiTestData();
        }
        log.info("Loading API test data from {}", resource);
        if (excel) {
            Map<String, String> row = ExcelUtils.readFirstRow(resource);
            return JsonUtils.convert(row, ApiTestData.class);
        }
        return JsonUtils.read(resource, ApiTestData.class);
    }
}
