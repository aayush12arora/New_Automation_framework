package framework.base;

import framework.constants.FrameworkConstants;
import framework.data.customer.CustomerData;
import framework.utilities.ExcelUtils;
import framework.utilities.JsonUtils;
import framework.utilities.PropertyManager;
import org.testng.annotations.BeforeMethod;

import java.util.Map;

/**
 * Base class for API tests — the API-side sibling of {@link BaseUITest}.
 *
 * <p>It extends {@link BaseTest}, so it inherits the whole shared lifecycle: the Extent report
 * node, the {@code TestListener} (reporting + soft-assert finalisation), retry, and the shared
 * {@link #assertions} helper. It adds <b>no</b> WebDriver — these tests call HTTP endpoints,
 * not a browser.</p>
 *
 * <p>API tests share one test-data file ({@code testdata/apiData.json} or {@code .xlsx}) rather
 * than one file per method, since endpoints reuse the same IDs. It is loaded into
 * {@code customerData} before each test, so tests read inputs via {@code customerData.get()}
 * exactly like the UI side — the single data source.</p>
 *
 * <p>Usage mirrors the UI: an API test extends this class, instantiates a service directly
 * (e.g. {@code new HomePageService()} — like {@code new LoginPage()}), and verifies through
 * {@code assertions}.</p>
 */
public abstract class BaseApiTest extends BaseTest {

    private static final String API_DATA_JSON = FrameworkConstants.TESTDATA_DIR + "apiData.json";
    private static final String API_DATA_XLSX = FrameworkConstants.TESTDATA_DIR + "apiData.xlsx";

    /** Loads the shared API data into {@code customerData}, overriding the per-method load. */
    @BeforeMethod(alwaysRun = true)
    public void loadApiData() {
        customerData.set(readApiData());
    }

    private CustomerData readApiData() {
        boolean excel = FrameworkConstants.DATA_SOURCE_EXCEL.equalsIgnoreCase(
                PropertyManager.get(FrameworkConstants.DATA_SOURCE));
        if (excel && JsonUtils.resourceExists(API_DATA_XLSX)) {
            log.info("Loading API test data from {}", API_DATA_XLSX);
            Map<String, String> row = ExcelUtils.readFirstRow(API_DATA_XLSX);
            return JsonUtils.convert(row, CustomerData.class);
        }
        if (JsonUtils.resourceExists(API_DATA_JSON)) {
            log.info("Loading API test data from {}", API_DATA_JSON);
            return JsonUtils.read(API_DATA_JSON, CustomerData.class);
        }
        log.info("No API test-data file found; using empty CustomerData");
        return new CustomerData();
    }
}
