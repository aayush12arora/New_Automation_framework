package framework.reporting;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import framework.utilities.LoggerUtil;
import framework.utilities.ScreenshotUtils;
import org.slf4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that finalises each test in the Extent report:
 * attaches a pass/fail screenshot at the end of the test and flushes
 * the report once the suite completes.
 *
 * <p>The report node itself is created in {@code BaseTest#setUp} so that
 * driver-setup steps are captured too.</p>
 */
public class TestListener implements ITestListener {

    private static final Logger LOG = LoggerUtil.getLogger(TestListener.class);

    @Override
    public void onTestSuccess(ITestResult result) {
        attachFinalScreenshot(result, Status.PASS, "Test PASSED: " + result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOG.error("Test FAILED: {}", result.getName(), result.getThrowable());
        if (ExtentReportManager.hasTest() && result.getThrowable() != null) {
            ExtentReportManager.getTest().log(Status.FAIL, result.getThrowable());
        }
        attachFinalScreenshot(result, Status.FAIL, "Test FAILED: " + result.getName());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (ExtentReportManager.hasTest()) {
            ExtentReportManager.getTest().log(Status.SKIP, "Test SKIPPED: " + result.getName());
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flush();
        LOG.info("Extent report flushed");
    }

    private void attachFinalScreenshot(ITestResult result, Status status, String message) {
        if (!ExtentReportManager.hasTest()) {
            return;
        }
        // Persist to disk for the record...
        ScreenshotUtils.capture(result.getName());
        // ...and embed the same shot in the report.
        String base64 = ScreenshotUtils.captureBase64();
        if (base64 != null) {
            ExtentReportManager.getTest().log(status, message,
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        } else {
            ExtentReportManager.getTest().log(status, message);
        }
    }
}
