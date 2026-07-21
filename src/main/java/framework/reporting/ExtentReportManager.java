package framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import framework.constants.FrameworkConstants;
import framework.utilities.LoggerUtil;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Owns the single {@link ExtentReports} instance and the per-thread
 * {@link ExtentTest} node, so parallel tests each log into their own report entry.
 * Each run writes to a timestamped folder: {@code target/extent-report/<timestamp>/index.html}.
 */
public final class ExtentReportManager {

    private static final Logger LOG = LoggerUtil.getLogger(ExtentReportManager.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final Path REPORT_PATH = buildReportPath();
    private static final ExtentReports EXTENT = create();
    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    private ExtentReportManager() {
        // Prevent instantiation.
    }

    private static Path buildReportPath() {
        return Path.of(FrameworkConstants.EXTENT_REPORT_DIR,
                LocalDateTime.now().format(TIMESTAMP), "index.html");
    }

    private static ExtentReports create() {
        ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH.toString());
        spark.config().setDocumentTitle("Automation Report");
        spark.config().setReportName("Selenium Automation Framework");
        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);
        LOG.info("Extent report will be written to {}", REPORT_PATH);
        return extent;
    }

    /** @return the path to this run's generated Extent HTML report. */
    public static Path getReportPath() {
        return REPORT_PATH;
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
