package framework.base;

import framework.constants.FrameworkConstants;
import framework.data.customer.CustomerData;
import framework.reporting.ExtentReportManager;
import framework.reporting.TestListener;
import framework.utilities.ExcelUtils;
import framework.utilities.JsonUtils;
import framework.utilities.LoggerUtil;
import framework.utilities.PropertyManager;
import framework.utilities.StepLogger;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Root test class. Owns only framework initialisation:
 * report node, test data and driver creation before each test, teardown after.
 * UI- or API-specific setup belongs in subclasses.
 *
 * <p>The {@link TestListener} is registered here so reporting/soft-assert handling
 * applies to every test regardless of how it is launched (IDE or {@code testng.xml}).
 * Inherits {@link #getDriver()} from {@link DriverContext}.</p>
 */
@Listeners(TestListener.class)
public abstract class BaseTest extends DriverContext {

    protected final Logger log = LoggerUtil.getLogger(getClass());

    /** Test data for the current test, loaded from {@code testdata/<testName>.json}. */
    protected CustomerData customerData;

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        // Create the report node first so every subsequent step is captured.
        ExtentReportManager.createTest(getClass().getSimpleName() + "." + method.getName());
        customerData = loadTestData(method.getName());
        StepLogger.step(log, "=== Test setup: initialising driver ===");
        DriverManager.setDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        StepLogger.step(log, "=== Test teardown: quitting driver ===");
        DriverManager.quitDriver();
        ExtentReportManager.remove();
    }

    /**
     * Loads the test's data into {@link CustomerData} from JSON or Excel, chosen by
     * the {@code dataSource} property. The data file is named after the test and its
     * attributes (JSON keys / Excel headers) match the {@link CustomerData} fields.
     * Returns an empty object when no data file exists, so {@link #customerData} is
     * always non-null.
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
