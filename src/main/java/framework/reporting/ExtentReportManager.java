package framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import framework.constants.FrameworkConstants;

/**
 * Owns the single {@link ExtentReports} instance and the per-thread
 * {@link ExtentTest} node, so parallel tests each log into their own report entry.
 */
public final class ExtentReportManager {

    private static final ExtentReports EXTENT = create();
    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    private ExtentReportManager() {
        // Prevent instantiation.
    }

    private static ExtentReports create() {
        ExtentSparkReporter spark = new ExtentSparkReporter(FrameworkConstants.EXTENT_REPORT_PATH);
        spark.config().setDocumentTitle("Automation Report");
        spark.config().setReportName("Selenium Automation Framework");
        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);
        return extent;
    }

    /** Creates a report node for the current test and binds it to this thread. */
    public static void createTest(String name) {
        TEST.set(EXTENT.createTest(name));
    }

    public static ExtentTest getTest() {
        return TEST.get();
    }

    public static boolean hasTest() {
        return TEST.get() != null;
    }

    /** Clears the test node from the current thread. */
    public static void remove() {
        TEST.remove();
    }

    /** Writes all buffered report data to disk. */
    public static void flush() {
        EXTENT.flush();
    }
}
