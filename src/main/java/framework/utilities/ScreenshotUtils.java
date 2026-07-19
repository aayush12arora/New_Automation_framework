package framework.utilities;

import framework.base.DriverManager;
import framework.constants.FrameworkConstants;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures a PNG screenshot of the current browser window
 * and writes it to the configured screenshot directory.
 */
public final class ScreenshotUtils {

    private static final Logger LOG = LoggerUtil.getLogger(ScreenshotUtils.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {
        // Prevent instantiation.
    }

    /**
     * @param name logical name used as the file prefix
     * @return absolute path of the saved screenshot, or {@code null} if capture failed
     */
    public static String capture(String name) {
        File source = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.FILE);
        String fileName = "%s_%s.png".formatted(name, LocalDateTime.now().format(TIMESTAMP));
        File destination = new File(FrameworkConstants.SCREENSHOT_DIR, fileName);
        try {
            FileUtils.copyFile(source, destination);
            LOG.info("Screenshot saved: {}", destination.getAbsolutePath());
            return destination.getAbsolutePath();
        } catch (IOException e) {
            LOG.error("Failed to save screenshot '{}'", name, e);
            return null;
        }
    }
}
