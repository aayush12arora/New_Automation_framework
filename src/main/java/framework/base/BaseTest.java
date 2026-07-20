package framework.base;

import framework.constants.FrameworkConstants;
import framework.data.customer.CustomerData;
import framework.reporting.ExtentReportManager;
import framework.utilities.JsonUtils;
import framework.utilities.LoggerUtil;
import framework.utilities.StepLogger;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

/**
 * Root test class. Owns only framework initialisation:
 * report node, test data and driver creation before each test, teardown after.
 * UI- or API-specific setup belongs in subclasses.
 *
 * <p>Inherits {@link #getDriver()} from {@link DriverContext}.</p>
 */
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
     * Reads the test's JSON data (named after the test) via {@link JsonUtils}
     * and maps it into a {@link CustomerData} object. Returns an empty object
     * when no data file exists, so {@link #customerData} is always non-null.
     */
    private CustomerData loadTestData(String testName) {
        String resource = FrameworkConstants.TESTDATA_DIR + testName + ".json";
        if (JsonUtils.resourceExists(resource)) {
            log.info("Loading test data from {}", resource);
            return JsonUtils.read(resource, CustomerData.class);
        }
        log.info("No test data file '{}' found; using empty CustomerData", resource);
        return new CustomerData();
    }
}
