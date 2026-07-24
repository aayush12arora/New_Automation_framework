package framework.utilities;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import framework.constants.FrameworkConstants;
import framework.reporting.ExtentReportManager;
import org.slf4j.Logger;

/**
 * Single entry point for logging a test step.
 * Every step is written to the SLF4J log <em>and</em>, when a report node exists,
 * added to the Extent report with a screenshot attached (configurable).
 */
public final class StepLogger {

    private static final int MAX_CODE_BLOCK = 10_000;

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

    /**
     * Records a step and, when a report node exists, renders {@code payload} underneath it as a
     * dedicated JSON code block — instead of dumping it inline as a truncated plain-text string.
     * {@code payload} may be a raw JSON string or a POJO (e.g. a REST Assured request body);
     * {@code null}/blank payloads log just the message, with no empty code block.
     */
    public static void stepWithJson(Logger log, String message, Object payload) {
        String pretty = truncate(JsonUtils.prettyPrint(payload));
        log.info(pretty.isEmpty() ? message : message + " | body=" + pretty);
        if (!ExtentReportManager.hasTest()) {
            return;
        }
        ExtentReportManager.getTest().log(Status.INFO, message);
        if (!pretty.isEmpty()) {
            ExtentReportManager.getTest().log(Status.INFO, MarkupHelper.createCodeBlock(pretty, CodeLanguage.JSON));
        }
    }

    private static String truncate(String body) {
        if (body.length() <= MAX_CODE_BLOCK) {
            return body;
        }
        return body.substring(0, MAX_CODE_BLOCK) + "\n…(truncated)";
    }

    private static boolean screenshotEachStep() {
        return PropertyManager.getBoolean(FrameworkConstants.SCREENSHOT_EACH_STEP);
    }
}
