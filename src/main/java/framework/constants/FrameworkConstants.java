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

    // Retry property keys.
    public static final String RETRY_ENABLED = "retryEnabled";
    public static final String RETRY_COUNT = "retryCount";

    /** Data-source toggle: {@code json} or {@code excel}. */
    public static final String DATA_SOURCE = "dataSource";
    public static final String DATA_SOURCE_JSON = "json";
    public static final String DATA_SOURCE_EXCEL = "excel";

    // Database property keys.
    public static final String DB_URL = "db.url";
    public static final String DB_USERNAME = "db.username";
    public static final String DB_PASSWORD = "db.password";

    /** Directory where captured screenshots are written. */
    public static final String SCREENSHOT_DIR = "target/screenshots";

    /** Base directory for Extent reports; each run goes in a timestamped sub-folder. */
    public static final String EXTENT_REPORT_DIR = "target/extent-report";

    /** Classpath directory holding per-test data (file name = test method name). */
    public static final String TESTDATA_DIR = "testdata/";

    // qTest upload property keys. The API token is NOT read from this file —
    // it comes from the QTEST_API_TOKEN environment variable so it never gets committed.
    public static final String QTEST_ENABLED = "qtestEnabled";
    public static final String QTEST_DOMAIN = "qtestDomain";
    public static final String QTEST_PROJECT_ID = "qtestProjectId";
    public static final String QTEST_API_TOKEN_ENV_VAR = "QTEST_API_TOKEN";

    /** Local copy of each generated qTest JUnit-XML upload, kept for reference. */
    public static final String QTEST_REPORT_DIR = "target/qtest-reports";
}
