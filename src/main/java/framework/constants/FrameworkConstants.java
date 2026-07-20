package framework.constants;

/**
 * Central place for framework-wide constant values.
 * Keeps magic strings out of the rest of the codebase.
 */
public final class FrameworkConstants {

    private FrameworkConstants() {
        // Prevent instantiation.
    }

    /** Classpath location of the main framework configuration file. */
    public static final String PROPERTIES_FILE = "framework.properties";

    // Property keys.
    public static final String BROWSER = "browser";
    public static final String HEADLESS = "headless";
    public static final String URL = "url";
    public static final String IMPLICIT_WAIT = "implicitWait";
    public static final String EXPLICIT_WAIT = "explicitWait";
    public static final String SCREENSHOT_EACH_STEP = "screenshotOnEachStep";

    /** Directory where captured screenshots are written. */
    public static final String SCREENSHOT_DIR = "target/screenshots";

    /** Extent HTML report output location. */
    public static final String EXTENT_REPORT_PATH = "target/extent-report/index.html";
}
