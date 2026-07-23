package framework.base;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import framework.api.config.ApiConfig;
import framework.constants.FrameworkConstants;
import framework.data.api.ApiTestData;
import framework.utilities.ExcelUtils;
import framework.utilities.JsonUtils;
import framework.utilities.PropertyManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Base class for API tests — the API-side sibling of {@link BaseUITest}. It extends
 * {@link BaseTest}, so it inherits the shared lifecycle (Extent report node, data loading,
 * {@code TestListener}, soft-assert finalisation, retry, {@link #assertions}) but adds
 * <b>no</b> WebDriver: these tests hit HTTP endpoints, not a browser.
 *
 * <p>On top of the inherited lifecycle it loads shared API test data and, in {@code mock} mode,
 * runs a WireMock server for the test class (started once per class, stubs reset before each
 * test) so the suite runs green with no live environment. Mock-based test classes should be
 * annotated {@code @Test(singleThreaded = true)} so parallel methods don't clobber each other's
 * stubs on the shared server.</p>
 */
public abstract class BaseApiTest extends BaseTest {

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

    /** Runs after {@link BaseTest#baseSetUp} (report node already created). */
    @BeforeMethod(alwaysRun = true)
    public void apiSetUp() {
        apiData = loadApiData();
        if (ApiConfig.isMock() && wireMockServer != null) {
            WireMock.reset();
        }
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
