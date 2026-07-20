package framework.utilities;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import framework.constants.FrameworkConstants;
import framework.reporting.ExtentReportManager;
import org.slf4j.Logger;

/**
 * Single entry point for logging a test step.
 * Every step is written to the SLF4J log <em>and</em>, when a report node exists,
 * added to the Extent report with a screenshot attached (configurable).
 */
public final class StepLogger {

    private StepLogger() {
        // Prevent instantiation.
    }

    /** Records an informational step. */
    public static void step(Logger log, String message) {
        log.info(message);
        if (!ExtentReportManager.hasTest()) {
            return;
        }
        if (screenshotEachStep()) {
            String base64 = ScreenshotUtils.captureBase64();
            if (base64 != null) {
                ExtentReportManager.getTest().log(Status.INFO, message,
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
                return;
            }
        }
        ExtentReportManager.getTest().log(Status.INFO, message);
    }

    private static boolean screenshotEachStep() {
        return PropertyManager.getBoolean(FrameworkConstants.SCREENSHOT_EACH_STEP);
    }
}
