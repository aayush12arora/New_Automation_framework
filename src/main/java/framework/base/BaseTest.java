package framework.base;

import framework.assertions.UIAssertions;
import framework.constants.FrameworkConstants;
import framework.data.customer.CustomerData;
import framework.reporting.ExtentReportManager;
import framework.reporting.TestListener;
import framework.utilities.ExcelUtils;
import framework.utilities.JsonUtils;
import framework.utilities.LoggerUtil;
import framework.utilities.PropertyManager;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Common root for <b>all</b> tests — UI ({@link BaseUITest}) and API ({@link BaseApiTest}).
 * Owns only the driver-independent lifecycle: creating the per-test Extent report node,
 * loading shared test data, and clearing both afterwards. It deliberately does <b>not</b>
 * create a WebDriver — that belongs to {@link BaseUITest}, so API tests inherit this class
 * without launching a browser.
 *
 * <p>The {@link TestListener} is registered here so reporting/soft-assert handling applies to
 * every test (UI and API) regardless of how it is launched. The retry analyzer is attached by
 * {@code framework.retry.RetryTransformer} via {@code META-INF/services} (ServiceLoader), not
 * {@code @Listeners} — see that class for why. Inherits {@link #getDriver()} from
 * {@link DriverContext} (used by UI tests; API tests simply never call it).</p>
 *
 * <p><b>Parallel execution:</b> under {@code parallel="methods"} TestNG shares one instance of
 * a test class across threads, so per-test state ({@link #customerData}) is held in a
 * {@link ThreadLocal}. Read it with {@code customerData.get()}.</p>
 */
@Listeners(TestListener.class)
public abstract class BaseTest extends DriverContext {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    /** Assertion helper (hard + soft), shared by UI and API tests. */
    protected final UIAssertions assertions = new UIAssertions();

    /** Test data for the current thread's test, loaded before it started. Use {@code .get()}. */
    protected static final ThreadLocal<CustomerData> customerData = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void baseSetUp(Method method) {
        ExtentReportManager.createTest(getClass().getSimpleName() + "." + method.getName());
        customerData.set(loadTestData(method.getName()));
    }

    @AfterMethod(alwaysRun = true)
    public void baseTearDown() {
        ExtentReportManager.remove();
        customerData.remove();
    }

    /**
     * Loads the test's data into {@link CustomerData} from JSON or Excel, chosen by the
     * {@code dataSource} property. The data file is named after the test and its attributes
     * (JSON keys / Excel headers) match the {@link CustomerData} fields. Returns an empty
     * object when no data file exists, so {@link #customerData} always resolves to non-null.
     */
    private CustomerData loadTestData(String testName) {
        String source = PropertyManager.get(FrameworkConstants.DATA_SOURCE).toLowerCase();
        return switch (source) {
            case FrameworkConstants.DATA_SOURCE_JSON -> loadFromJson(testName);
            case FrameworkConstants.DATA_SOURCE_EXCEL -> loadFromExcel(testName);
            default -> throw new IllegalArgumentException("Unsupported dataSource: " + source);
        };
    }

    private CustomerData loadFromJson(String testName) {
        String resource = FrameworkConstants.TESTDATA_DIR + testName + ".json";
        if (JsonUtils.resourceExists(resource)) {
            log.info("Loading test data (JSON) from {}", resource);
            return JsonUtils.read(resource, CustomerData.class);
        }
        log.info("No test data file '{}' found; using empty CustomerData", resource);
        return new CustomerData();
    }

    private CustomerData loadFromExcel(String testName) {
        String resource = FrameworkConstants.TESTDATA_DIR + testName + ".xlsx";
        if (JsonUtils.resourceExists(resource)) {
            log.info("Loading test data (Excel) from {}", resource);
            Map<String, String> row = ExcelUtils.readFirstRow(resource);
            return JsonUtils.convert(row, CustomerData.class);
        }
        log.info("No test data file '{}' found; using empty CustomerData", resource);
        return new CustomerData();
    }
}
