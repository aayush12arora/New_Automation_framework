package framework.reporting;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import framework.assertions.UIAssertions;
import framework.qtest.QTestUploader;
import framework.utilities.LoggerUtil;
import framework.utilities.ScreenshotUtils;
import org.slf4j.Logger;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that:
 * <ul>
 *   <li>finalises collected soft assertions right after each test method, turning
 *       any soft failure into a proper test failure — so tests never call
 *       {@code assertAll()} themselves;</li>
 *   <li>attaches a pass/fail screenshot at the end of each test;</li>
 *   <li>flushes the Extent report once the suite completes;</li>
 *   <li>uploads Surefire's JUnit XML reports to qTest once the suite completes
 *       (when {@code qtestEnabled=true}), so a plain {@code mvn test} run is
 *       enough — no separate upload command.</li>
 * </ul>
 *
 * <p>The report node itself is created in {@code BaseTest#setUp} so that
 * driver-setup steps are captured too.</p>
 */
public class TestListener implements ITestListener, IInvokedMethodListener {

    private static final Logger LOG = LoggerUtil.getLogger(TestListener.class);

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!method.isTestMethod()) {
            return;
        }
        // If the test already failed, just discard collected soft assertions.
        if (testResult.getStatus() != ITestResult.SUCCESS) {
            UIAssertions.reset();
            return;
        }
        // Otherwise verify them; a soft failure becomes a real test failure.
        try {
            UIAssertions.assertAll();
        } catch (AssertionError error) {
            testResult.setStatus(ITestResult.FAILURE);
            testResult.setThrowable(error);
        }
    }

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
        QTestUploader.uploadIfEnabled(context);
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
        // Flush after each test so the report exists even if the suite is interrupted.
        ExtentReportManager.flush();
    }
}
